package newnations;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import newnations.commands.*;
import newnations.invites.InviteManager;
import newnations.invites.TownUserInvite;
import newnations.listeners.NationsBlockListener;
import newnations.listeners.NationsScheduler;
import newnations.listeners.NationsUserListener;

public class NewNations extends JavaPlugin
{
	public static boolean loadOK = false;
	
	public ArrayList<Nation> nations = new ArrayList<Nation>();
	public ArrayList<Alliance> alliances = new ArrayList<Alliance>();
	public ArrayList<Siege> sieges = new ArrayList<Siege>();

	//these are used for caches, they are not saved to disk
	public HashMap<Player, Town> locationCache = new HashMap<Player, Town>();
	public HashMap<PlotLocation, Plot> plotCache =  new HashMap<PlotLocation, Plot>(); //the string key is created using hashChunk() below
	public HashMap<Player, User> userCache = new HashMap<Player, User>(); //resolve user objects to player objects
	public HashSet<Player> silencePlotMessage = new HashSet<Player>();
	
	//Storage Files
	private String dir = "plugins" + File.separator + "NewNations" + File.separator;
	private File nationsFile = new File(dir + "nations.json");
	private File alliancesFile = new File(dir + "alliances.json");
	private File invitesFile = new File(dir + "invites.json");
	private File siegesFile = new File(dir + "sieges.json");
	
	private InviteManager inviteMan;
	
	public ArrayList<Siege> getSieges() {return sieges;}
	
	public void onEnable()
	{
		try 
		{		
			//Setup ranks
			User.rankType.put("standard", 0x00);
			User.rankType.put("privileged", 0x07);
			User.rankType.put("officer", 0x7F);
			User.rankType.put("leader", 0xFF);
			
			//Setup siege constants
			getConfig().addDefault("emeraldValue", 100);
			getConfig().addDefault("townSetupFee", 200);
			getConfig().addDefault("siegeFee", 1000);
			getConfig().addDefault("warcampFee", 300);
			getConfig().addDefault("tribute", 1000);
			getConfig().addDefault("besiegeLightning", true);
			getConfig().addDefault("removeEmptyTowns", true);
			getConfig().addDefault("siegeDeathLimit", 4);
			getConfig().addDefault("siegeDefenderLifeCount", 6);
			getConfig().addDefault("siegeBesiegerLifeCount", 5);
			getConfig().addDefault("siegeBuffer", 10);
			getConfig().addDefault("restoreBlockFee", 2);
			getConfig().addDefault("tributeDeathThreshold", 5);
			getConfig().addDefault("immunityPeriod", 120);
			getConfig().addDefault("siegePreludePeriod", 8);
			getConfig().addDefault("siegeLootingPeriod", 12);
			
			Siege.DEATH_LIMIT = -((Integer) getConfig().get("siegeDeathLimit"));
			Siege.DEF_LIFE_COUNT = ((Integer) getConfig().get("siegeDefenderLifeCount"));
			Siege.BES_LIFE_COUNT = ((Integer) getConfig().get("siegeBesiegerLifeCount"));
			Siege.BATTLE_BUFFER = ((Integer) getConfig().get("siegeBuffer"));
			Siege.TRIBUTE_THRESHOLD = ((Integer) getConfig().get("tributeDeathThreshold"));
			Siege.IMMUNITY_PERIOD = ((Integer) getConfig().get("immunityPeriod"));
			Siege.PRELUDE_PERIOD = (Integer) getConfig().get("siegePreludePeriod");
			Siege.LOOTING_PERIOD = (Integer) getConfig().get("siegeLootingPeriod");
			
			new NationsUserListener(this);
			new NationsBlockListener(this);
			new NationsScheduler(this);
			new Econ(this);
			
			getConfig().options().copyDefaults(true);		
			loadAll(); //Load everything up from storage.
			
			//Setup Economy
			if(Econ.econEnabled()) getLogger().info("Economy plug-in found.");
			else getLogger().info("No economy plug-in found - Switching to emerald economy.");
	
			getLogger().info(getVersion()+" Loaded.");
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	public void onDisable()
	{
		saveConfig();
		saveAll(); //Save everything to storage.
		//Unload for Java's shitty memory management system
		alliances = null;
		nations = null;
		sieges = null;
		nationsFile = null;
		alliancesFile = null;
		siegesFile = null;
		dir = null;
		locationCache = null;
		plotCache = null;
		userCache = null;
		
		getLogger().info(getVersion() + " Unloaded.");
	}
	public String getVersion() {return getDescription().getVersion();}

	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args)
	{
		try 
		{
			//Player/Console Commands
			if(cmdLabel.equalsIgnoreCase("nadmin"))
			{
				if(!sender.isOp()) throw new NationsException("Insufficient Access", "Only OPs may use the nadmin command");
				new Nadmin(sender, cmdLabel, args, this);
				return true;
			}
			else if(!(sender instanceof Player)) //If commandSender isn't a player (ie. console) stop here.
			{ 
				getLogger().log(Level.WARNING, "Sorry, console can only use the /nadmin command.");
				return false;
			}
			
			//Player-Only Commands	
			User user = getUser(((Player) sender).getName());
			if(user == null) user = new User(this, (Player)sender); //TODO: make commands deal with user == null
			if(cmdLabel.equalsIgnoreCase("nation")) 		new NationCommand(sender, args, this, user);
			else if(cmdLabel.equalsIgnoreCase("town")) 		new TownCommand(sender, args, this, user);
			else if(cmdLabel.equalsIgnoreCase("claim")) 	new Claim(sender, args, this, user);
			else if(cmdLabel.equalsIgnoreCase("raze")) 		new Raze(sender, args, this, user);
			else if(cmdLabel.equalsIgnoreCase("siege")) 	new SiegeCommand(sender, args, this, user);
			else if(cmdLabel.equalsIgnoreCase("warcamp")) 	new Warcamp(sender, args, this, user);
			else if(cmdLabel.equalsIgnoreCase("surrender")) new Surrender(sender, args, this, user);
			else if(cmdLabel.equalsIgnoreCase("alliance"))  new AllianceCommand(sender, args, this, user);
			else return false;
		}
		catch(NationsException e) {e.printError(sender);}
		return true;
	}
	
	//TODO make this generate better acronyms
	public String createCode(String name)
	{
		String out;
		do
		{
			out = "";
			for(int i = 0; i < 3; i++) out += (char)(65 + (int)(Math.random() * ((90 - 65) + 1)));
		}
		while(isNameUnique(out) == false );
		return out;
	}
	
	public boolean isNameUnique(String proposedName)
	{
		for(Nation n : nations)
		{
			if(n.getName().equalsIgnoreCase(proposedName)) return false;
			if(n.getCode().equalsIgnoreCase(proposedName)) return false;
			
			for(Town t : n.getTowns())
			{
				if(t.getName().equalsIgnoreCase(proposedName)) return false;
				if(t.getCode().equalsIgnoreCase(proposedName)) return false;
			}
		}
		for(Alliance a : alliances)
		{
			if(a.getName().equalsIgnoreCase(proposedName)) return false;
			if(a.getCode().equalsIgnoreCase(proposedName)) return false;
		}
		return true;
	}
	
	//TODO: potentially move this
	/**
	 * Plots are exponentially more expensive the closer they are to existing towns 
	 * and is proportional to the size of the nearby town
	 * @return: The cost for setting up a Plot at the given position
     */
	public int getCostToNearbyTowns(PlotLocation plotLoc, Town exclude)
	{
		double totalcost = 0;
		for(Nation n : nations)
		{
			for(Town t : n.getTowns())
			{
				if(t == exclude) continue;
				if(!t.getWorldName().equalsIgnoreCase(plotLoc.getWorldName())) continue;
				//Find the Closest plot for this town
				double closest = Double.POSITIVE_INFINITY;
				for(Plot p : t.getPlots())
				{
					//Euclidean distance (Pythagoras)
					double dist = Math.sqrt(Math.pow(p.getX() - plotLoc.getChunkX(), 2) + Math.pow(p.getZ() - plotLoc.getChunkZ(), 2));
					if(dist < closest) closest = dist;
				}
				totalcost += NewNationsHelper.plotCostFunction(t.getPlots().size(), closest);
			}
		}
		return (int)totalcost;
	}
	
	/**
	 * Copies the Plot and User objects into the HashMap caches.
     */
	public void setupCaches()
	{
		plotCache.clear();
		userCache.clear();
		for(Nation n : nations)
		{
			for(Town t : n.getTowns())
			{
				for(Plot p : t.getPlots()) plotCache.put(new PlotLocation(p.getX(), p.getZ(), t.getWorldName()), p);
				for(User u : t.getUsers())
				{
					Player p = this.getServer().getPlayerExact(u.getName());
					if(p!=null) userCache.put(p, u);
				}
			}
		}
		
		locationCache.clear();
		for(Player p : Bukkit.getServer().getOnlinePlayers())
		{
			Plot plot = getPlot(p.getLocation());
			Town plotTown = null;
			if(plot != null) plotTown = plot.getTown();
			locationCache.put(p, plotTown);
		}
	}
	
    /**
     * Add a Plot object to the Plot cache HashMap
     */
	public void addCachedPlot(PlotLocation PlotLoc, Plot p) 	
	{
		plotCache.put(PlotLoc, p);
	}
	public void addCachedUser(Player player, User user) {userCache.put(player,user);}
	public void removeCachedUser(Player p) 				{userCache.remove(p);}
	public void removeCachedPlot(Plot p) 				{while(plotCache.values().remove(p));}
	
	//Invite Management
	/**
	 * Displays the user a list of pending invites the player has received 
     */
	public void checkUserInvite(Player player)
	{
		for(TownUserInvite invite : inviteMan.getTownInvitesForUser(player.getName()))
			NewNationsHelper.notifyText(player, "The town "+ChatColor.GREEN+invite.getSendingTown().getDisplayName()+ChatColor.YELLOW+" has invited you to their town.");		
	}
	public void cullInvites() {inviteMan.cull();}
	public void addNation(Nation n) {nations.add(n);}
	public void removeNation(Nation n) {nations.remove(n);}
	public void addAlliance(Alliance a) {alliances.add(a);}
	public void removeAlliance(Alliance a) {alliances.remove(a);}
	public void addSiege(Siege s) {sieges.add(s);}
	public void removeSiege(Siege s) {sieges.remove(s);}
	
	/**
	 * Called after load()
	 * checks all data loaded correctly 
     */
	public void checkAll()
	{
		for(Nation n : nations) n.checkAll(this);
		for(Alliance a : alliances) a.checkAll(this);
	}
	
	
	//this runs whenever something gets changed, town is founded, user is added ect
	public void onEdit() {saveAll();}
	
	//Storage Methods
	public void loadAll()
	{
		try
		{
			nations.clear();
			alliances.clear();
			sieges.clear();
			JSONParser parser = new JSONParser();
			JSONObject obj;
			
			//Load nations
			if(!nationsFile.exists())
			{
				
				getLogger().info("Creating " + nationsFile.getName() + " file.");
				nationsFile.getParentFile().mkdirs(); //create all the folders it will need
				nationsFile.createNewFile();
				loadOK = true;
			}
			else
			{
				getLogger().info("Loading file: "+nationsFile.getName());
				obj = (JSONObject) parser.parse(new FileReader(nationsFile));
				JSONArray nationsArray = (JSONArray) obj.get("nations");
				for(int i = 0; i < nationsArray.size(); i++) 
				{
					Nation nation = new Nation(this, (JSONObject)nationsArray.get(i));
					if(nation.getTowns().isEmpty())
					{
						NewNationsHelper.notifyAll("Nation "+ChatColor.RED+nation.getName()+ChatColor.YELLOW+" has no towns is being removed.");
					}
					else nations.add(nation);
				}
				
			}
			
			
			//Load alliances
			if(!alliancesFile.exists())
			{
				getLogger().info("Creating " + alliancesFile.getName() + " file.");
				alliancesFile.createNewFile();
			}
			else
			{
				obj = (JSONObject) parser.parse(new FileReader(alliancesFile));
				JSONArray allaincessArray = (JSONArray) obj.get("alliances");
				for(int i = 0; i < allaincessArray.size(); i++)
					new Alliance(this, (JSONObject)allaincessArray.get(i));
			}
			
			//Load sieges
			if(!siegesFile.exists())
			{
				getLogger().info("Creating " + siegesFile.getName() + " file.");
				siegesFile.createNewFile();
			}
			else
			{
				obj = (JSONObject) parser.parse(new FileReader(siegesFile));
				JSONArray siegesArray = (JSONArray) obj.get("sieges");
				for(int i = 0; i < siegesArray.size(); i++)
					sieges.add(new Siege().load((JSONObject)siegesArray.get(i), this));
			}
			
			if(!invitesFile.exists())
			{
				getLogger().info("Creating " + invitesFile.getName() + " file.");
				inviteMan = new InviteManager(this);
				invitesFile.createNewFile();
			}
			else
			{
				obj = (JSONObject) parser.parse(new FileReader(invitesFile));
				inviteMan = new InviteManager(this, obj);
			}
			
			
			loadOK = true;
			System.out.print("loaded Data");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			getLogger().info(ChatColor.RED+"WARNING! the nations data was unable to load. To protect from data corruption saving has been disabled. Restart the server as soon as possible.");
			loadOK = false;
		}
		cullInvites();
		setupCaches();
		checkAll();
	}

	public void saveAll()
	{
		if(!loadOK) 
		{
			getLogger().info(ChatColor.RED+" Corrupted data detected, save aborted. Restart server as soon as possible.");
			return;
		}
		try
		{
			//Nations & invites Save
			JSONObject obj = new JSONObject();
			JSONArray nationsList = new JSONArray();
			for(Nation nation : nations) 
				nationsList.add(nation.save());
			obj.put("nations", nationsList);

			FileWriter file = new FileWriter(nationsFile);
			file.write(obj.toString());
			file.close();
			
			//Siege Save
			obj = new JSONObject();
			JSONArray siegesList = new JSONArray();
			for(Siege siege : sieges)
				siegesList.add(siege.save());
			obj.put("sieges", siegesList);
			file = new FileWriter(siegesFile);
			file.write(obj.toString());
			file.close();
			
			//Alliance Save
			obj = new JSONObject();
			JSONArray alliancesList = new JSONArray();
			for(Alliance alliance : alliances)
				alliancesList.add(alliance.save());
			obj.put("alliances", alliancesList);
			file = new FileWriter(alliancesFile);
			file.write(obj.toString());
			file.close();
			
			file = new FileWriter(invitesFile);
			file.write(inviteMan.save().toString());
			file.close();
			
			System.out.print("Saved Data");
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	//Get Methods
	public Alliance getAlliance(String allianceName)
	{
		for(Alliance alliance : alliances) 
		{
			String thisName = alliance.getName();
			String thiscode = alliance.getCode();
			if(thiscode != null && thiscode.equalsIgnoreCase(allianceName) || thisName != null && thisName.equalsIgnoreCase(allianceName)) 
				return alliance;
		}
		return null;
	}
	
	public Nation getNation(String nationName)
	{
		for(Nation nation : nations) 
			if(nation.getName().equalsIgnoreCase(nationName) || nation.getCode().equalsIgnoreCase(nationName))
				return nation;
		return null;
	}
	
	public Town getTown(String townName)
	{
		for(Nation nation : nations)
		{
			for(Town town : nation.getTowns()) 
				if(town.getName().equalsIgnoreCase(townName) || town.getCode().equalsIgnoreCase(townName)) return town;
		}
		return null;
	}
	public User getUser(Player p) {return userCache.get(p);}
	public User getUser(String username)
	{
		for(Nation nation : nations)
			for(Town town : nation.getTowns())
				for(User user : town.getUsers()) 
					if(username.equalsIgnoreCase(user.getName())) return user;
		return null;
	}
	public Siege getSiege(Town besiegedTown)
	{
		for(Siege s : sieges) if(s.isBesiegedTown(besiegedTown)) return s;
		return null;
	}
	
	public InviteManager getInviteManager() {return inviteMan;}
	
	/**
     * Gets a list if Sieges that are taking place at the given chunk co ords
     */
	public ArrayList<Siege> getSieges(long chunkX, long chunkZ)
	{
		ArrayList<Siege> returnSieges = new ArrayList<Siege>();
		for(Siege s : sieges) if(s.inRange(chunkX, chunkZ)) returnSieges.add(s);
		return returnSieges;
	}
	
	/**
     * @return the cost of a given plot if <town> wanted to buy it
     * includes the plot fee and the town expansion fee 
     */
	public int getPlotFinalCost(PlotLocation plotLoc, Town town)
	{
		int totalcost = getCostToNearbyTowns(plotLoc, town);
		if(town != null) totalcost += town.getNextPlotCost();
		return totalcost;
	}

	
	/**
     * @return The plot object from the HashMap cache
     */
	public Plot getPlot(Location l)
	{		
		PlotLocation newPlotLocation = new PlotLocation(l);
		return getPlot(newPlotLocation);
	}
    /**
     * @return The plot object from the HashMap cache
     */
	public Plot getPlot(PlotLocation PlotLoc) {return plotCache.get(PlotLoc);}
	
	/**
     * When a plot is bought or sold the players in that plot get out of sync with the plot location cache
     * this function will rechecks everyones location and notify them of any changes
     */
	public void notifyPlotChange()
	{
		for(Player p : Bukkit.getServer().getOnlinePlayers())
		{
			Town lastLocation = locationCache.get(p);
			Plot plot = getPlot(p.getLocation());
			Town plotTown = null;
			if(plot != null) plotTown = plot.getTown();
			if(lastLocation == plotTown) continue;
			
			locationCache.put(p, plotTown);
			NationsUserListener.showPlotDesc(p, plotTown);
		}
	}
}
