package newnations;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Siege
{
	//Constants
	private static final int    BIG_ENOUGH_INT   = 16 * 1024;
	private static final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
	public static int TRIBUTE_THRESHOLD = 6; //Number of defender deaths before surrender without being able to pay tribute results in looting.
	public static int IMMUNITY_PERIOD = 120; //in minutes
	public static int DEATH_LIMIT = -4; //Units of 5 Minutes
	public static long DEF_LIFE_COUNT = 6, BES_LIFE_COUNT = 5;
	public static int BATTLE_BUFFER = 10;

	private long maxX, maxZ, minX, minZ;
	private int defenderLife = 0, besiegerLife = 0, defenderDeathtoll = 0, besiegerDeathtoll = 0;
	public enum siegeStage {PRELUDE, MAIN, LOOT;}
	private siegeStage stage = siegeStage.PRELUDE;
	
	private NewNations plugin;
	private ArrayList<Town> defenders = new ArrayList<Town>();
	private ArrayList<Town> besiegers = new ArrayList<Town>(); 
	private HashMap<Town, Location> warcamps = new HashMap<Town, Location>();
	private HashMap<User, Long> users = new HashMap<User, Long>();
	private HashMap<User, Long> offlineUserTime = new HashMap<User, Long>();
	
	public Siege() {}
	public Siege(Town defender, Town besieger, final NewNations plugin) throws NationsException
	{
		if(defender == besieger) return;
		this.plugin = plugin;
		
		//Calculate Battlefield Boundary Limits
		maxX = minX = defender.getPlots().get(0).getX();
		maxZ = minZ = defender.getPlots().get(0).getZ();
		for(Plot p : defender.getPlots())
		{
			if(p.getX() > maxX) maxX = p.getX();
			if(p.getX() < minX) minX = p.getX();
			if(p.getZ() > maxZ) maxZ = p.getZ();
			if(p.getZ() < minZ) minZ = p.getZ();
		}
		maxX += BATTLE_BUFFER;
		minX -= BATTLE_BUFFER;
		maxZ += BATTLE_BUFFER;
		minZ -= BATTLE_BUFFER;
		
		//Add towns
		addTown(defender);
		addTown(besieger);
		
		//Add all player in range.
		for(Player p : Bukkit.getOnlinePlayers())
		{
			User u = plugin.getUser(p);
			if(u != null) addUser(u);
		}
		
		//Save town for restore.
		try
		{
			for(Plot p : defender.getPlots()) 
				p.saveChunk(plugin.getServer().getWorld(defender.getWorldName()).getChunkAt((int)p.getX(), (int)p.getZ()));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		plugin.addSiege(this);
		
		defenderBroadcast("You have been besieged by "+ChatColor.RED+defender.getName()+". Prepare for battle.");
		defender.getNation().notifyText("The town of "+ChatColor.GREEN+defender.getName()+" in your nation has been besieged by "+ChatColor.RED+besieger.getName()+ChatColor.YELLOW+".", defender);
		besiegerBroadcast("Your town has besieged the town of "+ChatColor.RED+defender.getName()+ChatColor.YELLOW+". Prepare for battle.");
		besieger.getNation().notifyText("The town of "+ChatColor.GREEN+besieger.getName()+" in your nation has besieged "+ChatColor.RED+besieger.getName()+ChatColor.YELLOW+".", besieger);
		NewNationsHelper.notifyAll("The town of "+ChatColor.GRAY+defender.getName()+ChatColor.DARK_AQUA+" has been besieged by "+ChatColor.GRAY+besieger.getName()+ChatColor.YELLOW+".");
		plugin.getLogger().info(besieger.getName()+" has besieged "+defender.getName()+".");
		prelude();
	}
	
	public JSONObject save()
	{
		JSONObject siege = new JSONObject();
		siege.put("stage", stage.name());
		siege.put("maxX", maxX);
		siege.put("minX", minX);
		siege.put("maxZ", maxZ);
		siege.put("minZ", minZ);
		siege.put("besiegerDeathtoll", besiegerDeathtoll);
		siege.put("defenderDeathtoll", defenderDeathtoll);
		JSONArray defendersList = new JSONArray();
		JSONArray besiegersList = new JSONArray();
		JSONObject usersList = new JSONObject();
		JSONObject warcampsList = new JSONObject();
		for(Town town : defenders) defendersList.add(town.getName());
		for(Town town : besiegers) besiegersList.add(town.getName());
		for(Town town : warcamps.keySet()) warcampsList.put(town.getName(), encodeLoc(warcamps.get(town)));
		for(User user : users.keySet()) usersList.put(user.getName(), users.get(user));
		siege.put("defenders", defendersList);
		siege.put("besiegers", besiegersList);
		siege.put("warcamps", warcampsList);
		siege.put("users", usersList);
		return siege;
	}
	
	public Siege load(JSONObject obj, NewNations plugin) throws NationsException
	{
		this.plugin = plugin;
		stage = siegeStage.valueOf((String)obj.get("stage"));
		maxX = (Long) obj.get("maxX");
		minX = (Long) obj.get("minX");
		maxZ = (Long) obj.get("maxZ");
		minZ = (Long) obj.get("minZ");
		long aa = (Long) obj.get("besiegerDeathtoll");
		besiegerDeathtoll = (int) aa;
		long bb = (Long) obj.get("defenderDeathtoll");
		defenderDeathtoll = (int) bb;
		JSONArray defendersArray = (JSONArray) obj.get("defenders");
		JSONArray besiegersArray = (JSONArray) obj.get("besiegers");
		JSONObject warcampsArray = (JSONObject) obj.get("warcamps"); 
		JSONObject usersArray = (JSONObject) obj.get("users");
		for(Object s : defendersArray) addTown(plugin.getTown((String)s));
		for(Object s : besiegersArray) 
		{
			addTown(plugin.getTown((String)s));
		}
		for(Object s : warcampsArray.keySet()) warcamps.put(plugin.getTown((String)s), decodeLoc((String)warcampsArray.get(s)));
		for(Object s : usersArray.keySet()) 
		{
			User u = plugin.getUser((String)s);
			if(u != null) users.put(u, (Long)usersArray.get(s));
		}
		for(User u : users.keySet()) addUser(u);
		if(stage == siegeStage.PRELUDE) prelude();
		else if(stage == siegeStage.LOOT) loot();
		return this;
	}
	
	public boolean addTown(Town town) throws NationsException
	{
		//Add town
		if(defenders.contains(town) || besiegers.contains(town)) {return false;}

		if(defenders.isEmpty()) defenders.add(town);
		else if(besiegers.isEmpty())  besiegers.add(town);
		else
		{
			boolean defence = checkAllegiance(town, defenders.get(0));
			boolean besiege = checkAllegiance(town, besiegers.get(0));

			if(defence && besiege) //Allied with both parties.
				throw new NationsException("You cannot join the siege.", "You are allied with both sides.");
			else if(defence) defenders.add(town);
			else if(besiege) besiegers.add(town);
			else return false; //Not allied with either party.
		}
		//Register this siege to town
		if(!town.getSieges().contains(this)) town.getSieges().add(this);
		return true;
	}
	
	public static boolean checkAllegiance(Town town, Town checkTown)
	{
		if(town.getNation() == checkTown.getNation()) return true;
		ArrayList<Alliance> checkAlliances = checkTown.getNation().getAlliances();
		ArrayList<Alliance> townAlliances = town.getNation().getAlliances();
		for(Alliance a : townAlliances) 
			if(checkAlliances.contains(a)) return true;
		return false;
	}
	
	public boolean addUser(User user) throws NationsException
	{
		if(!inRange(user)) return false;
		Town town = user.getTown();
		if(town == null) return false;
		//If user is in the nation of either the besieger or the defender, automatically add the town to the siege. 
		if(defenders.get(0).getNation() == town.getNation() || besiegers.get(0).getNation() == town.getNation()) addTown(town);
		
		boolean defending;
		if(defenders.contains(town)) defending = true;
		else if(besiegers.contains(town)) defending = false;
		else return false; //You aren't in an involved town or nation, reject.
		
		if(!users.containsKey(user)) users.put(user, defending ? DEF_LIFE_COUNT : BES_LIFE_COUNT); //Add user to siege.
		if(!user.getSieges().contains(this)) user.addSiege(this); //Register siege to user.
		
		updateLifepool();
		updateScore();
		for(User u : users.keySet()) 
			for(User secondUser : users.keySet()) u.addTeamer(secondUser, this);
		return true;
	}
	
	public void removeUser(User user)
	{
		user.removeSiege(this);
		updateLifepool();
		updateScore();
	}
	
	public void removeTown(Town town)
	{
		for(User u : town.getUsers()) removeUser(u);
		town.getSieges().remove(this);
	}
	
	public boolean recordDeath(User user)
	{
		Town town = user.getTown();
		if(defenders.contains(town)) defenderDeathtoll++;
		else besiegerDeathtoll++;
		Long lifeCount = users.get(user);
		if(lifeCount == null) return false; //User is not in the siege.
		
		if(lifeCount <= DEATH_LIMIT) return false; //User is at death limit.
		users.put(user, lifeCount - 1);
		checkVictory();
		return true;
	}
	
	public void checkVictory()
	{
		updateLifepool();
		updateScore();
		if(stage != siegeStage.MAIN) return;
		if(besiegerLife <= 0) defenderVictory();
		else if(defenderLife <= 0) besiegerVictory();
	}
	
	public void updateScore() {for(User u : users.keySet()) u.updateScoreboard();}
	
	public boolean inRange(User u)
	{
		if(u == null) return false;
		Player p = plugin.getServer().getPlayerExact(u.getName());
		if(p == null) return false;
		Location l = p.getLocation();
		int x = ((int) (l.getX() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4; 
		int z = ((int) (l.getZ() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4; 
		return inRange(x, z);
	}
	public boolean inRange(long chunkX, long chunkZ) {return chunkX > minX && chunkX < maxX && chunkZ > minZ && chunkZ < maxZ;}
	
	private void defenderVictory()
	{
		defenders.get(0).deposit(Econ.SIEGE_FEE);
		broadcast("The defenders of "+ChatColor.GRAY+defenders.get(0).getName()+ChatColor.YELLOW+" have successfully repelled the besieging force.");
		broadcast("The siege is lifted and protections are restored. The town is immune to siege for 4 hours");
		destroy();
	}
	private void besiegerVictory()
	{
		if(getDefendersCount() == 0)
		{
			surrender();
		}
		else
		{
			defenders.get(0).deposit(Econ.SIEGE_FEE);
			stage = siegeStage.LOOT;
			broadcast("The besiegers are victorious against the defenders of "+ChatColor.GRAY+defenders.get(0).getName()+ChatColor.YELLOW+".");
			loot();
		}
		
	}
	
	public int getDefendersCount()
	{
		int c = 0;
		for(User u : users.keySet())
		{
			if(defenders.contains(u.getTown()))
			{
				if(inRange(u)) c++;
			}
		}
		return c;
	}
	public void surrender() 
	{
		broadcast(ChatColor.GRAY + defenders.get(0).getName() + ChatColor.YELLOW + " have surrendered to their besiegers.");
		plugin.getLogger().info(defenders.get(0).getName()+" has surrendered to its besiegers.");

		//Refund allied towns
		for(Town t : defenders) if(t.getNation() != defenders.get(0).getNation()) t.deposit(Econ.WARCAMP_FEE);
		for(Town t : besiegers) if(t.getNation() != besiegers.get(0).getNation()) t.deposit(Econ.WARCAMP_FEE);
		
		int dt = defenderDeathtoll;
		int tribute = Econ.TRIBUTE + Econ.TRIBUTE * dt * dt * dt; //t + tx^3
		Town defender = defenders.get(0);
		if(defender.getCoffers() < tribute)
		{
			if(defenderDeathtoll > TRIBUTE_THRESHOLD) 
			{
				broadcast("Since they are unable their high tribute and have taken too many casualties, a looting phase has begun.");
				loot();
			}
			else
			{
				int excess = tribute - defender.getCoffers();
				defender.setRestoreFee(defender.getRestoreFee() + excess);
				besiegers.get(0).deposit(defender.getCoffers() + Econ.SIEGE_FEE);
				defender.withdraw(defender.getCoffers());
				broadcast("The siege is lifted and protections are returned. The town is now immune to siege for "+IMMUNITY_PERIOD+" hour(s)."); 
				destroy();
			}
		}
		else
		{
			defender.withdraw(tribute);
			besiegers.get(0).deposit(tribute);
			broadcast("The siege is lifted and protections are returned. The town is now immune to siege for "+IMMUNITY_PERIOD+" hour(s)."); 
			destroy();
		}
	}
	
	public void prelude()
	{
		int period = 1;
		broadcast("The prelude has begun against "+ChatColor.GRAY+defenders.get(0).getName()+ChatColor.YELLOW+".");
		broadcast("It will last "+period+" minute(s).");
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
	    {
			public void run()
			{
				if(stage != siegeStage.PRELUDE || plugin.getSiege(defenders.get(0)) == null) return; //If siege has progressed or is over, return.
				stage = siegeStage.MAIN;
				defenders.get(0).setDestruction(true); //Turn on destruction
				
				broadcast("The prelude has concluded for "+ChatColor.GRAY+defenders.get(0).getName()+ChatColor.YELLOW+".");
				broadcast("Protections are now down in the town.");
				if(getDefenderLife() <= 0) 
				{
					broadcast(ChatColor.GRAY+defenders.get(0).getName()+ChatColor.YELLOW+" have abandoned the defence of their lands and thus ");
					surrender();
					return;
				}
				else checkVictory();
				plugin.onEdit();
	        }
	    }
		,period * 1200); //1200 = minute (variable)
	}
	
	public void loot()
	{
		int period = 1;
		broadcast("The town is open to plunder and looting for "+period+" minute(s).");
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
	    {
			public void run()
			{
				Siege siege = plugin.getSiege(defenders.get(0));
				//if(siege == null) return;
				broadcast("Looting is now over and the siege is lifted.");
				broadcast("The lands of "+ChatColor.GRAY+defenders.get(0).getName()+ChatColor.YELLOW+" are now protected again.");
				siege.destroy();
	        }
	    }
		,period * 1200); //1200 = minute
	}
	
	public void update()
	{
		for(Town t : defenders)
		{
			if(!checkAllegiance(defenders.get(0), t) || checkAllegiance(besiegers.get(0), t)) //no longer allied with defender or now allied with besiegers
			{
				broadcast("The allegiance of "+ChatColor.RED+t.getName()+ChatColor.YELLOW+" has changed and thus they have been removed from the siege.");
				removeTown(t);
			}
		}
		for(Town t : besiegers)
		{
			if(checkAllegiance(defenders.get(0), t) || !checkAllegiance(besiegers.get(0), t)) //allied with defenders or no longer allied with defenders
			{
				broadcast("The allegiance of "+ChatColor.RED+t.getName()+ChatColor.YELLOW+" has changed and thus they have been removed from the siege.");
				removeTown(t);
			}
		}
	}
	
	//Additional Methods
	public void broadcast(String msg)
	{
		defenderBroadcast(msg);
		besiegerBroadcast(msg);
	}
	public void defenderBroadcast(String msg) {for(Town town : defenders) town.notifyText(msg, null);}
	public void besiegerBroadcast(String msg) {for(Town town : besiegers) town.notifyText(msg, null);}
	
	public void updateLifepool() 
	{
		defenderLife = 0;
		besiegerLife = 0;
		for(User u : users.keySet())
			if(u != null && u.getSieges().contains(this) && defenders.contains(u.getTown())) defenderLife += users.get(u);
		for(User u : users.keySet()) 
			if(u != null && u.getSieges().contains(this) && besiegers.contains(u.getTown())) besiegerLife += users.get(u);
	}
	
	//Warcamp Saving/Loading Methods
	private String encodeLoc(Location loc)
	{
		if(loc == null) return "";
		return loc.getWorld().getName()+";"+loc.getBlockX()+";"+loc.getBlockY()+";"+loc.getBlockZ();
	}
	private Location decodeLoc(String locTag)
	{
		if(locTag.length() == 0) return null;
		String[] ls = locTag.split(";");
		if(ls.length != 4) return null;
		World world = plugin.getServer().getWorld(ls[0]);
		double x = Double.parseDouble(ls[1]);
		double y = Double.parseDouble(ls[2]);
		double z = Double.parseDouble(ls[3]);
		return new Location(world, x, y, z);
	}
	
	public void validateOnlineUsers()
	{
		for(User u : users.keySet())
		{
			Player p = Bukkit.getPlayerExact(u.getName());
			
			if(p != null && p.isOnline()) offlineUserTime.remove(u);
			else
			{
				if(offlineUserTime.containsKey(u))
				{
					long time = offlineUserTime.get(u);
					if(time > 6)
					{
						offlineUserTime.remove(u);
						
						//if(plugin.getSiege(thsiSeige.getDefenders().get(0)) == null) continue;
						broadcast(ChatColor.RED + u.getName() + ChatColor.YELLOW + " has been removed from the siege for logging off.");
						removeUser(u);
						checkVictory();
					}
					else offlineUserTime.put(u , time + 1);						
				}
				else offlineUserTime.put(u , (long) 1);			
			}		
		}
	}
	
	public void destroy()
	{
		Town defender = defenders.get(0);
		defender.setImmuneExpire(System.currentTimeMillis() + (IMMUNITY_PERIOD * 6000)); //convert minutes to milliseconds
		defender.setDestruction(false);
		for(Town town : defenders) removeTown(town);
		for(Town town : besiegers) removeTown(town);
		plugin.removeSiege(this); 
	}
	
	public ArrayList<Nation> getDefendingNations()
	{
		ArrayList<Nation> list = new ArrayList<Nation>();
		for(Town t : defenders) 
		{
			if(list.contains(t.getNation())) list.add(t.getNation());
		}
		return list;
	}
	
	public ArrayList<Nation> getBesiegingNations()
	{
		ArrayList<Nation> list = new ArrayList<Nation>();
		for(Town t : besiegers) 
		{
			if(list.contains(t.getNation())) list.add(t.getNation());
		}
		return list;
	}
	
	public int totalDeathtoll() {return defenderDeathtoll + besiegerDeathtoll;}
	public ArrayList<Town> getDefenders() {return defenders;}
	public ArrayList<Town> getBesiegers() {return besiegers;}
	public int getDefenderLife() {return defenderLife;}
	public int getBesiegerLife() {return besiegerLife;}
	public int getDefenderDeathtoll() {return defenderDeathtoll;}
	public int getBesiegerDeathtoll() {return besiegerDeathtoll;}
	public HashMap<User, Long> getUsers() {return users;}
	public Location setWarcamp(Town town, Location l) {return warcamps.put(town, l);}
	public Location getWarcamp(Town town) 
	{
		Location w = warcamps.get(town);
		if(w == null)
		{
			for(Town t : (defenders.contains(town) ? defenders : besiegers)) 
				if(t.getNation() == town.getNation())
				{
					w = warcamps.get(t);
					break;
				}
		}
		return w;
	}
	public boolean isPrelude() {return stage == siegeStage.PRELUDE;}
	public boolean isLooting() {return stage == siegeStage.LOOT;}
	public boolean isBesiegedTown(Town town) {return defenders.get(0) == town;}
}	
