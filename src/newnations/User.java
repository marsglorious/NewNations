package newnations;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.json.simple.JSONObject;

public class User
{	
	public static HashMap<String, Integer> rankType = new HashMap<String, Integer>();
	
	private String name;
	private Town town;
	private Location spawn;
	
	private Scoreboard board = null;
	private Objective objective;
	private Team blues, reds;
	
	private int rank = 0;
	
	public int getRank() {return rank;}
	public void setRank(int rank) {this.rank = rank;}
	public boolean cashPriv() {return (rank & 0x01) != 0;} 
	public boolean destructionPriv() {return (rank & 0x02) != 0;}  
	public boolean rankPriv() {return (rank & 0x04) != 0;}  
	public boolean moneyPriv() {return (rank & 0x08) != 0;}  
	public boolean siegePriv() {return (rank & 0x10) != 0;}  
	public boolean invitePriv() {return (rank & 0x20) != 0;}  
	public boolean plotPriv() {return (rank & 0x40) != 0;}  
	public boolean leaderPriv() {return (rank & 0x80) != 0;}  
	
	
	//LPISMRDC
	/*
	 * (L)eader - disband/nation/alliance
	 * (P)lot - claim, raze
	 * (I)nvite - invite/deinvite/kick
	 * (S)iege - siege/surrender/warcamp
	 * (M)oney (full) withdraw/tax
	 * (R)ank - promote/demote
	 * (D)estruction - destruction
	 * (C)ash (partial withdraw) - withdraw
	 * 
	 * leader = LPISMRDC -> 0xFF
	 * officer = lPISMRDC -> 0x7F
	 * privileged = lpismRDC -> 0x07
	 * standard = lpismrdc -> 0x00
	 * 
	 * CBS
	 * (C)ontainer - Access chest, etc.
	 * (B)lock - Break, Place
	 * (S)witch - Press buttons, etc.
	 */
	
	//Siege Attributes
	private ArrayList<Siege> sieges = new ArrayList<Siege>();
	private NewNations plugin;
	
	/**
	 * Creates a new User object from a player object
	 * and adds it to the cache (if player is not null)
     * @return the new User object
     */
	public User(NewNations plugin, Player p)
	{		
		this.plugin = plugin; 
		this.name =  p.getName();
		if(p != null) plugin.addCachedUser(p, this);
	}
	
	/**
	 * Creates a new User object from a player object
	 * and adds it to the cache
     * @return the new User object
     */
	public User(NewNations plugin, String name) 
	{
		this.plugin = plugin; 
		this.name =  name;	
		Player p = Bukkit.getPlayerExact(name);
		if(p != null) plugin.addCachedUser(p, this);
	}
	
	//Replaces the load function
	public User(NewNations plugin, JSONObject obj, Town town)
	{
		this.plugin = plugin;
		name = (String) obj.get("name");
		if(obj.containsKey("rank"))
		{
			long temp = (Long) obj.get("rank");
			rank = (int) temp;
		}
		
		this.town = town;
	}
	
	public JSONObject save()
	{
		JSONObject user = new JSONObject();
		user.put("name", name);
		user.put("rank", rank);
		return user;
	}
	
	public void updateScoreboard()
	{
		if(sieges.isEmpty()) 
		{
			board = null;
			Player p = Bukkit.getPlayerExact(name);
			if(p != null) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
			return;
		}
		if(board == null) 
		{
			board = Bukkit.getScoreboardManager().getNewScoreboard();
			objective = board.registerNewObjective("sieges", "dummy");
			objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			objective.setDisplayName(ChatColor.YELLOW+"Sieges");
			blues = board.registerNewTeam("blues");
			reds = board.registerNewTeam("reds");
			blues.setPrefix(ChatColor.BLUE+"");
			reds.setPrefix(ChatColor.RED+"");
			Bukkit.getPlayerExact(name).setScoreboard(board);
		}
		//Reset objective
		objective.unregister();
		objective = board.registerNewObjective("sieges", "dummy");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setDisplayName(ChatColor.YELLOW+"Sieges");
		
		for(Siege s : sieges)
		{
			boolean d = s.getDefenders().contains(town);
			boolean n = sieges.size() > 1;
			Score defenders = objective.getScore(Bukkit.getOfflinePlayer((n ? s.getDefenders().get(0).getCode() : "")+(d ? ChatColor.BLUE : ChatColor.RED)+" Defenders"));
			Score besiegers = objective.getScore(Bukkit.getOfflinePlayer((n ? s.getDefenders().get(0).getCode() : "")+(d ? ChatColor.RED : ChatColor.BLUE)+" Besiegers"));
			if(s.getDefenderLife() == 0) defenders.setScore(1); 
			if(s.getBesiegerLife() == 0) besiegers.setScore(1);
			defenders.setScore(s.getDefenderLife());
			besiegers.setScore(s.getBesiegerLife());
		}
	}
	
	public void addTeamer(User user, Siege siege)
	{
		if(blues == null || reds == null) return;
		blues.removePlayer(Bukkit.getOfflinePlayer(user.getName()));
		reds.removePlayer(Bukkit.getOfflinePlayer(user.getName()));
		//If your allegiance is the same as the added team, make them blue, otherwise, make them red.
		if(siege.getDefenders().contains(town) == siege.getDefenders().contains(user.getTown())) blues.addPlayer(Bukkit.getOfflinePlayer(user.getName())); 
		else reds.addPlayer(Bukkit.getOfflinePlayer(user.getName()));
	}
	
	public String getName() {return name;}
	public String getRankedName() 
	{
		if(rank == rankType.get("standard")) return ChatColor.WHITE+name;
		else if(rank == rankType.get("privileged")) return ChatColor.WHITE+"* "+name;
		else if(rank == rankType.get("officer")) return ChatColor.LIGHT_PURPLE+"** "+name;
		else if(rank == rankType.get("leader")) return ChatColor.GOLD+"*#* "+name;
		else return ChatColor.AQUA+"*?* "+name;
	}
	public Town getTown() {return town;}
	public Location getSpawn() {return spawn;}
	public ArrayList<Siege> getSieges() {return sieges;}
	public void setSpawn(Location spawn) {this.spawn = spawn;}
	public void setTown(Town town) {this.town = town;}
	public void addSiege(Siege siege) {if(!sieges.contains(siege)) sieges.add(siege);}
	public void removeSiege(Siege siege) {sieges.remove(siege);}
	public void notifyUser(String message)
	{
		Player p = Bukkit.getServer().getPlayerExact(name);
		if(p != null) p.sendMessage(ChatColor.YELLOW+message);
	}
}
