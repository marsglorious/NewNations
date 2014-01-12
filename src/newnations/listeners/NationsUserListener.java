package newnations.listeners;

import java.util.ArrayList;
import newnations.Nation;
import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Plot;
import newnations.PlotLocation;
import newnations.Siege;
import newnations.Town;
import newnations.User;
import newnations.invites.NationTownInvite;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.*;

public class NationsUserListener implements Listener
{

	private NewNations plugin;
	
	//http://www.java-gaming.org/index.php?topic=24194.0
	private static final int    BIG_ENOUGH_INT   = 16 * 1024;
	private static final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
	
	public NationsUserListener(NewNations plugin)
	{
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		final Player player = event.getPlayer();
		final User user = plugin.getUser(player);
		if(user == null) return;
		plugin.removeCachedUser(player);
		plugin.silencePlotMessage.remove(player);
		
		//Check if players are online, if not turn destruction off.
		Town town = user.getTown();
		//Check Sieges
		final ArrayList<Siege> sieges = user.getSieges();
		if(sieges.isEmpty()) 
		{
			if(town != null && residentsOnline(town, user) == 0) town.setDestruction(false);
			return;
		}
		
	}
			
	@EventHandler
	public synchronized void onPlayerJoin(PlayerJoinEvent event) 
	{
		Player player = event.getPlayer();
		Plot plot = plugin.getPlot(player.getLocation());
		if(plot != null)
		{
			showPlotDesc(player, plot.getTown());
			plugin.locationCache.put(player, plot.getTown());
		}
		else plugin.locationCache.put(player, null);
		
		//is this user invited to any towns, if so tell them
		plugin.checkUserInvite(player);
		
		//fetch the player object, make one if there isnt any
		User user = plugin.getUser(player.getName());
		if(user != null) 
		{
			
			for(NationTownInvite invite : plugin.getInviteManager().getNationInvitesForTown(user.getTown()))
				NewNationsHelper.notifyText(player, "The nation "+ChatColor.GREEN+invite.getSendingNation().getDisplayName()+ChatColor.YELLOW+" has invited your town.");
			
			//has our town been invited to anything, TODO only officers need this
			plugin.addCachedUser(player,user);
			
			//Update user siege
			int chunkX = ((int) (player.getLocation().getX() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4; 
			int chunkZ = ((int) (player.getLocation().getZ() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4;
			checkSiege(user, player, chunkX, chunkZ);
			plugin.addCachedUser(player,user);
		}
		else
		{
			//this will automatically add them to the cache
			user = new User(plugin, player); 
		}
		
	}
	
	private void playerCheckLocation(Player player, Location to, Location from)
	{
		//Chunk if user is passing a chunk boundary, if not: return.
		int tox = ((int) (to.getX() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4; 
		int toz = ((int) (to.getZ() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4; 
		int frx = ((int) (from.getX() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4; 
		int frz = ((int) (from.getZ() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4; 
		if(tox == frx && toz == frz) return;

		//Check if user has entered a siege
		User user = plugin.getUser(player);
		checkSiege(user, player, tox, toz);
				
		//Check Player Town
		Town lastLocation = plugin.locationCache.get(player);
		PlotLocation plotLoc = new PlotLocation(tox, toz, to.getWorld().getName());
		Plot plot = plugin.getPlot(plotLoc);
		Town plotTown = null;
		if(plot != null) plotTown = plot.getTown();
		if(lastLocation == plotTown) return;
				
		//New location
		plugin.locationCache.put(player, plotTown);
		
		if(plugin.silencePlotMessage.contains(player) != true)
		showPlotDesc(player, plotTown);
	}
	
	@EventHandler
	public synchronized void onPlayerMove(PlayerMoveEvent event) 
	{
		playerCheckLocation(event.getPlayer(), event.getTo(), event.getFrom());
	}
	
	@EventHandler
	public synchronized void onPlayerTeleport(PlayerTeleportEvent event)
	{
		playerCheckLocation(event.getPlayer(), event.getTo(), event.getFrom());
	}
	
	
	@EventHandler
	public synchronized void onPlayerDeath(PlayerDeathEvent event)
	{
		User user = plugin.getUser(event.getEntity());
		if(user == null) return;
		for(int i = 0; i < user.getSieges().size(); i++) user.getSieges().get(i).recordDeath(user);
	}
	
	@EventHandler
	public synchronized void onPlayerRespawn(PlayerRespawnEvent event)
	{
		User user = plugin.getUser(event.getPlayer());
		if(user == null) return;
		Town town = user.getTown();
		if(town == null) return;
		ArrayList<Siege> sieges = user.getSieges();
		if(sieges.isEmpty()) return;
		Location l = sieges.get(0).getWarcamp(town); //TODO: do this better
		if(l == null) return;
		event.setRespawnLocation(l);
	}
	
	
	public synchronized void checkSiege(User u, Player p, long chunkX, long chunkZ)
	{
		if(u == null) return;
		ArrayList<Siege> userSieges = u.getSieges();
		ArrayList<Siege> plotSieges = plugin.getSieges(chunkX, chunkZ);
		
		//If a user is in a siege but not in range, remove him/her.
		for(int i = 0; i < userSieges.size(); i++)
		{
			if(!plotSieges.contains(userSieges.get(i)))
			{
				String townName = ChatColor.RED + userSieges.get(i).getDefenders().get(0).getName();
				NewNationsHelper.notifyText(p, "You have left the field of battle in the siege against "+townName+ChatColor.YELLOW+".");
				Siege siege = userSieges.get(i);
				siege.removeUser(u);
				siege.checkVictory();
			}
		}
		//If the user is in range of a siege but isn't in it, add him/her.
		for(Siege s : plotSieges) 
		{
			if(!userSieges.contains(s))
			{
				String townName = ChatColor.RED + s.getDefenders().get(0).getName();
				try {if(s.addUser(u)) NewNationsHelper.notifyText(p, "You have been added to the siege on "+townName+ChatColor.YELLOW+".");}
				catch(NationsException e) {e.printError(p);}
			}	
		}
	}
	
	public static synchronized void showPlotDesc(Player player, Town town)
	{
		String message = ChatColor.AQUA + "*** ";
		if(town == null) message = message + ChatColor.GRAY + "The Wild";
		//TODO: Add town/nation/allied/neutral/hostile color coding.
		else message = message+ChatColor.GREEN+town.getName()+ChatColor.BLUE+" "+town.getCode();
		player.sendMessage(message);
	}
	
	public synchronized int residentsOnline(Town town, User excludedUser)
	{
		int count = 0;
		for(User u : town.getUsers())
		{
			Player p = plugin.getServer().getPlayerExact(u.getName());
			if((p == null || p.isOnline()) && u != excludedUser) count++;
		}
		return count;
	}
}
