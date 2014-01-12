package newnations;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Town
{
	private String name;
	private String code;
	private Nation nation;
	private String worldname;
	private boolean destruction = false;
	private boolean hasRestore = false;
	private int coffers = 0, restoreFee = 0;
	private long immuneExpire = 0;
	private ArrayList<Siege> sieges = new ArrayList<Siege>();
	private ArrayList<User> users = new ArrayList<User>();
	private ArrayList<Plot> plots = new ArrayList<Plot>();

	private NewNations plugin;
	private AccessManager accessMan;
	
	public static int plotExpansionFeeFunction(int numberOfPlots) {return numberOfPlots * (Econ.EMERALD_VALUE / 5) + Econ.EMERALD_VALUE;}
	
	public Town(NewNations plugin, String townName)
	{
		this.name = townName;
		this.plugin = plugin;
		accessMan = new AccessManager();
	}
	
	public Town(NewNations plug, JSONObject obj, Nation nation)
	{		
		this.plugin = plug;
		load(obj, nation);
	}

	
	
	public Town load(JSONObject obj, Nation nation)
	{
		name = (String) obj.get("name");
		code = (String) obj.get("code");
		long aa = (Long) obj.get("coffers");
		coffers = (int) aa;
		long bb = (Long) obj.get("restoreFee");
		restoreFee = (int) bb;
		worldname = (String) obj.get("worldName");
		hasRestore = (Boolean) obj.get("hasRestore");
		destruction =  (Boolean) obj.get("destruction");
		if(obj.containsKey("immuneExpire")) immuneExpire = (Long) obj.get("immuneExpire");
		if(this.nation != null) plugin.getServer().getLogger().info("WARNING: De-sync detected on loading the nation "+name);
		this.nation = nation;
		JSONArray usersList = (JSONArray) obj.get("users");
		JSONArray plotsList = (JSONArray) obj.get("plots");

		JSONObject nationAccessList = (JSONObject) obj.get("nationAccess");
		for(int i = 0; i < usersList.size(); i++) users.add(new User(plugin, (JSONObject)usersList.get(i), this));
		for(int i = 0; i < plotsList.size(); i++) plots.add(new Plot(plugin, (JSONObject)plotsList.get(i), this));
		accessMan = new AccessManager(nationAccessList);
		return this;
	}
	
	

	public JSONObject save()
	{
		JSONObject town = new JSONObject();
		town.put("name", name);
		town.put("code", code);
		town.put("coffers", coffers);
		town.put("restoreFee", restoreFee);
		town.put("worldName", worldname);
		town.put("hasRestore", hasRestore);
		town.put("destruction", destruction);
		town.put("immuneExpire", immuneExpire);
		JSONArray usersList = new JSONArray();
		JSONArray plotsList = new JSONArray();
		for(User user : users) usersList.add(user.save());
		for(Plot plot : plots) plotsList.add(plot.save());
		town.put("users", usersList);
		town.put("plots", plotsList);
		town.put("nationAccess", accessMan.save());
		return town;
	}
	
	public String getDisplayName()
	{
		return ChatColor.GREEN + name + " " + ChatColor.DARK_GREEN + code;
	}
	
	
	public AccessManager getAccessMan()
	{
		return accessMan;
	}
	
	
	public Plot getPlot(int chunkx, int chunkz, String worldname)
	{
		if(!this.worldname.equalsIgnoreCase(worldname)) return null;
		return getPlot(chunkx, chunkz);
	}
	
	public Plot getPlot(int chunkx, int chunkz)
	{
		for(Plot plot : plots)
			if(plot.getX() == chunkx && plot.getZ() == chunkz ) return plot;
		return null;
	}
	
	
	public boolean checkAdjacent(int chunkx, int chunkz, String worldn)
	{
		for(Plot p : this.plots)
		{
			if(!this.worldname.equalsIgnoreCase(worldn)) continue;
			long x1 = p.getX();
			long z1 = p.getZ();			
			if(x1 == chunkx && (z1 + 1 == chunkz || z1 - 1 == chunkz)) return true; 
			if(z1 == chunkz && (x1 + 1 == chunkx || x1 - 1 == chunkx)) return true; 
		}
		return false;
	}
	
	public void addPlot(Plot p) {plots.add(p);}
	public Plot createPlot(PlotLocation plotLoc)
	{
		//check if its already owned by someone
		if(plugin.getPlot(plotLoc) != null) return null;
		Plot plot = new Plot(plugin, plotLoc, this);
		plugin.addCachedPlot(plotLoc, plot);
		plots.add(plot);
		return plot;
	}
	
	//This doesn't factor in nearby towns.
	public int getNextPlotCost() {return plotExpansionFeeFunction(plots.size());}	
	public int getCurrentPlotCost() {return plotExpansionFeeFunction(plots.size()-1);}
	
	
	
	public void addUser(User user) throws NationsException
	{
		if(users.size() == 0) user.setRank(User.rankType.get("leader"));
		else user.setRank(User.rankType.get("standard"));
		user.setTown(this);
		if(!users.contains(user) && users.add(user)) return;
		throw new NationsException("User already in town.", null);
	}

	/**
     * Checks if a given plot is able to be sold
     * the noChecks is used when you wish to bypass any restrictions
     * this feature is mostly intended for admins
	 * @throws NationsException 
     */
	public void canSellPlot(Plot p, boolean noChecks) throws NationsException 
	{
		if(plots.contains(p) && noChecks) return;
		if(plots.contains(p) && plots.size() > 1)
		{
			//TODO: check that selling this plot will not create any gaps or islands
			// throw new NationsException("Cannot Sell Plot", "Because reasons");
		}
		else throw new NationsException("Unable to sell plot.", "The plot is not a part of the town "+ChatColor.GREEN+name+ChatColor.YELLOW+".");
	} 
	
	public boolean removeUser(User user) 
	{
		user.setTown(null);
		user.setRank(0);
		Player p = Bukkit.getPlayerExact(user.getName());
		if(p != null) plugin.removeCachedUser(p);
		return users.remove(user);
	}
	public boolean removePlot(Plot plot) {return plots.remove(plot);}
	
	public void notifyText(String message, User excludedUser)
	{
		for(User user : users) 
		{
			if(excludedUser == user) continue;
			Player p = Bukkit.getServer().getPlayerExact(user.getName());
			if(p != null) NewNationsHelper.notifyText(p, message);
		}
	}
	public void kick(User kickee) throws NationsException
	{
		if(!users.contains(kickee)) 
			throw new NationsException("User not in your town.", null);
		removeUser(kickee);
	}
	public void leave(User user) throws NationsException
	{
		boolean removeEmptyTowns = true;
		if(plugin.getConfig().contains("removeEmptyTowns")) 
			removeEmptyTowns = plugin.getConfig().getBoolean("removeEmptyTowns");
		boolean lastLeader = true;
		for(User u : users)
		{
			if(u != user && u.leaderPriv())
			{
				lastLeader = false;
				break;
			}
		}
		if(users.size() == 1 && removeEmptyTowns) //If you are the last resident
			throw new NationsException("You cannot leave. You are the last resident.", "Type "+ChatColor.GREEN+"/town disband"+ChatColor.YELLOW+" to remove the town.");
		if(lastLeader && removeEmptyTowns)
			throw new NationsException("You cannot leave. You are the last leader.", "Set another residents rank to leader first. See \"/town rank help\"");
		removeUser(user);
	}
	
	/**
     * This should be called after all Towns have been loaded
     * 
     */
	public void checkAll()
	{
		if(code == null || code.isEmpty()) code = plugin.createCode(name); 
		//TODO: check if name is unique.

		accessMan.reconnectStrings(plugin);
	}
	
	public void switchNation(Nation newNation) throws NationsException
	{
		// if this town is not alone in the nation
		if(nation.getTowns().size() > 1)
		{
			if(nation.getCapital() == this) 
				throw new NationsException("You cannot leave your nation", "Your town is currently the capital of the nation.");
		}
		newNation.addTown(this);
		nation.removeTown(this);
		nation = newNation;
		for(Siege s : sieges) s.update();
		plugin.onEdit();
	}

	public String toString() {return getDisplayName();}
	
	public int getTownNetWorth()
	{
		int cost = 0;
		int j =  plots.size() - 1;
		for(Plot p : plots)
		{
			cost += plugin.getCostToNearbyTowns(p.getPlotLocation(), this);
			cost += Town.plotExpansionFeeFunction(j);
			j--;
		}
		//Should the net worth include the expand fee for the first plot
		return cost - Town.plotExpansionFeeFunction(0);
	}
	
	/**
	 * Destroy Town, the nation object will be notified
	 * Deletes a town (and its plots) from existence. If it is the last town in the nation it will delete the nation
	 * @throws NationsException 
     */
	public void destroy() throws NationsException
	{
		NewNationsHelper.notifyAll("The town of "+ChatColor.GREEN+name+ChatColor.DARK_AQUA+" has been disbanded.");
		for(Plot plot : plots) 
		{
			// this differs from plo.sell(), townDisbanding() will not do any checks
			// the town will be paid the value of the plots
			plot.townDisbanding();
		}
		coffers += Econ.TOWN_SETUP_FEE;
		plugin.getInviteManager().removeInviteContaining(this);
		nation.removeTown(this);
		
		for(int i = 0; i < users.size(); i++) removeUser(users.get(i));
		plugin.notifyPlotChange();
	}
	
	/**
	 * Called when a town wishes to move to a new location
	 * it will take into a account the value of the plots and pay the difference
     * @return the plot at the new town
     */
	public void relocateTown(PlotLocation newPlotLocation, World world) throws NationsException
	{
		//Check the town only has one plot.
		if(plots.size() != 1) throw new NationsException("You cannot relocate.", "You must sell all but one plot before you can relocate.");
		
		Plot oldPlot = plots.get(0);
		
		if(plugin.getPlot(newPlotLocation) != null)
			throw new NationsException("Unable to relocate.", "Someone already owns this plot.");
		
		int oldPlotCost = plugin.getCostToNearbyTowns(oldPlot.getPlotLocation(), this);	
		int newPlotCost = plugin.getCostToNearbyTowns(newPlotLocation, this);
		
		if(newPlotCost + getCoffers() < oldPlotCost)
			throw new NationsException("Unable to relocate.", "The old plot is worth "+ChatColor.GREEN+"$"+oldPlotCost+ChatColor.YELLOW+" the new plot is worth "+ChatColor.GREEN+newPlotCost+ChatColor.YELLOW+".");
		
		//sell the old one
		removePlot(oldPlot);
		plugin.removeCachedPlot(oldPlot);
		deposit(oldPlotCost);
		
		//buy the new one`
		withdraw(newPlotCost);
		Plot newPlot = new Plot(plugin, newPlotLocation, this);

		plugin.addCachedPlot(newPlotLocation, newPlot);
		addPlot(newPlot);
		plugin.notifyPlotChange();
		newPlot.placeTorches();
	}
	
	/**
	 * Called when a town wishes to move to a new location
	 * it will take into a account the value of the plots and pay the difference
     * @return the plot at the new town
     */
	public void relocateTown(Location loc) throws NationsException //Puts torches down at the edges of plots. How cools is that? Free torches.
	{	
		relocateTown(new PlotLocation(loc), loc.getWorld());	
	}
	
	public ArrayList<User> getUsers() {return users;}
	public ArrayList<Plot> getPlots() {return plots;}

	public String getCode() {return code;}
	public void setCode(String c) throws NationsException 
	{
		if(c.length() != 3) 
			throw new NationsException("Invalid Code.", "Codes must be 3 letters long.");
		if(plugin.isNameUnique(c) == false)
			throw new NationsException("Invalid Code.", "That code is already in use.");
		code = c;
	}
	public String getWorldName() {return worldname;}
	public void setWorldName(String w) {worldname = w;}
	public Nation getNation() {return nation;}
	public boolean isDestructionOn() {return destruction;}
	public void setNation(Nation nation) {this.nation = nation;}
	public void setDestruction(boolean set) {this.destruction = set;}
	public boolean hasRestore() {return hasRestore;}
	public void setHasRestore(boolean hasRestore) {this.hasRestore = hasRestore;} 
	public ArrayList<Siege> getSieges() {return sieges;}
	public void removeSiege(Siege s) {sieges.remove(s);}
	public int getRestoreFee() {return (int) restoreFee;}
	public void setRestoreFee(int restoreFee) {this.restoreFee = restoreFee;}
	public int getCoffers() {return (int) coffers;}
	public void setCoffers(int amount) {coffers = amount;}
	public long getImmuneExpire() {return (immuneExpire / 6000) - (System.currentTimeMillis() / 6000);}
	public void setImmuneExpire(long immuneExpire) {this.immuneExpire = immuneExpire;}
	public boolean isImmuned() {return System.currentTimeMillis() > immuneExpire && immuneExpire > 0;}
	
	public int withdraw(int amount) 
	{
		notifyText(""+ChatColor.RED+"$"+amount+ChatColor.YELLOW+" withdrawn from the town coffers.", null);
		return coffers -= amount;
	}
	
	public void withdraw(Player p, double amount) throws NationsException
	{
		if(coffers == 0) throw new NationsException("The town coffers are empty.", null);
		if(amount > coffers)
		{
			NewNationsHelper.notifyText(p, "The town lacks "+ChatColor.GREEN+"$"+amount+ChatColor.YELLOW+", withdrawing "+ChatColor.BLUE+"$"+coffers+ChatColor.YELLOW+" instead.");
			coffers -= Econ.deposit(p, coffers);
		}
		else coffers -= Econ.deposit(p, amount);
		NewNationsHelper.notifyText(p, "New Town Coffers: "+ChatColor.BLUE+"$"+coffers+ChatColor.YELLOW+".");
	}
	
	public double deposit(double amount) 
	{
		notifyText(""+ChatColor.GREEN+"$"+amount+ChatColor.YELLOW+" deposited to the town coffers.", null);
		return coffers += amount;
	}
	
	
	public void deposit(Player p, double amount) throws NationsException
	{
		double a = Econ.uncheckedWithdraw(p, amount);
		coffers += a;
		NewNationsHelper.notifyText(p, "New Town Coffers: "+ChatColor.BLUE+"$"+coffers+ChatColor.YELLOW+".");
	}
	

	public String getName() {return name;}
	
	public void setName(String newName) throws NationsException 
	{
		if(newName.length() > 30)
			throw new NationsException("Town name too long.", "Town names cannot be longer than 30.");
		
		if(!plugin.isNameUnique(newName))
			throw new NationsException("Town name unavailable.", "A town already exists with that name.");
		
		this.name = newName;
	} 
}
