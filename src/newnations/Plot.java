package newnations;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.InventoryHolder;
import org.json.simple.JSONObject;


public class Plot
{
	//private final static int width = 16, height = 255, length = 16;
	private String restore;
	private long x, z;
	private Town town;
	private NewNations plugin;
	private PlotRestoreManager plotsaver = null;

	public Plot(NewNations plugin, PlotLocation plotLoc, Town town)
	{
		this.plugin = plugin;
		this.town = town;
		this.x = plotLoc.getChunkX();
		this.z = plotLoc.getChunkZ();
		restore = town.getWorldName()+";"+x+";"+z+".schematic";
	}
	public Plot(NewNations plugin, Town town, Location loc)
	{		
		this(plugin, new PlotLocation(loc), town);
	}
	
	
	public Plot(NewNations plugin, JSONObject obj, Town town)
	{
		x = (Long) obj.get("x");
		z = (Long) obj.get("z");
		restore = town.getWorldName()+";"+x+";"+z+".schematic";
		this.town = town;
		this.plugin = plugin;
	}
	
	public JSONObject save()
	{
		JSONObject plot = new JSONObject();
		plot.put("x", x);
		plot.put("z", z);
		return plot;
	}
	
	/**
     * @return the value of a plot if its owner were to sell it
     */
	public double getPlotWorth()
	{
		double totalcost = plugin.getCostToNearbyTowns(getPlotLocation(), town);
		totalcost += town.getCurrentPlotCost();
		return totalcost;
	}

	public PlotLocation getPlotLocation()
	{
		return new PlotLocation(x,z, town.getWorldName());
	}

	/**
     * places torches in the corners
     * takes a world reference
     */
	public void placeTorches()
	{
		int x = (int)this.x;
		int z = (int)this.z;

		World world = Bukkit.getWorld(town.getWorldName());
		if(world == null) return;
			
		Material marker = Material.TORCH;
		world.getBlockAt(x * 16, world.getHighestBlockYAt(x * 16, z * 16), z * 16).setType(marker);
		world.getBlockAt((x * 16) + 15, world.getHighestBlockYAt((x * 16) + 15, z * 16), z * 16).setType(marker);
		world.getBlockAt(x * 16, world.getHighestBlockYAt(x * 16, (z * 16) + 15), (z * 16) + 15).setType(marker);
		world.getBlockAt((x * 16) + 15, world.getHighestBlockYAt((x * 16) + 15, (z * 16) + 15), (z * 16) + 15).setType(marker);
	}
	
	
	/**
     * called when a town wishes to sell a plot
     * payTown set this to true if the own should be paid the value of this plot
     * noCheck is used when you need to bypass island checks and such, this feature is intended for admin use
     */
	public void sellPlot(boolean payTown, boolean noCheck) throws NationsException
	{
		// this will throw an exception if there's an issue
		town.canSellPlot(this, noCheck);

		double oldPlotCost = getPlotWorth();	
		
		town.removePlot(this);
		if(payTown) town.deposit(oldPlotCost);
		
		plugin.removeCachedPlot(this);
		removeTorches();
		plugin.notifyPlotChange();
	}

	/**
     * called when a town is about to be disbanded
     * this function differs from sellPlot in that no checks are done whatsoever
     * the town is paid the value of the plot
     * 
     */
	public void townDisbanding()
	{
		double oldPlotCost = getPlotWorth();
		town.deposit(oldPlotCost);
		
		plugin.removeCachedPlot(this);
		removeTorches();
	}

	
	public void removeTorches()
	{
		World world = Bukkit.getWorld(town.getWorldName());
		if(world == null) return;
		int x = (int)this.x;
		int z = (int)this.z;
		Material marker = Material.TORCH;
		Block b;
		b = world.getBlockAt(x * 16, world.getHighestBlockYAt(x * 16, z * 16), z * 16);
		if(b.getType() == marker) b.setType(Material.AIR);
		b = world.getBlockAt((x * 16) + 15, world.getHighestBlockYAt((x * 16) + 15, z * 16), z * 16);
		if(b.getType() == marker) b.setType(Material.AIR);
		b = world.getBlockAt(x * 16, world.getHighestBlockYAt(x * 16, (z * 16) + 15), (z * 16) + 15);
		if(b.getType() == marker) b.setType(Material.AIR);
		b = world.getBlockAt((x * 16) + 15, world.getHighestBlockYAt((x * 16) + 15, (z * 16) + 15), (z * 16) + 15);
		if(b.getType() == marker) b.setType(Material.AIR);
	}
	
	public Town getTown() {return town;}
	public long getX() {return x;}
	public long getZ() {return z;}
	public String getRestore() {return restore;}
	
	//Restore Methods
	
	
	public void saveChunk(Chunk argchunk)
	{
		plotsaver = new PlotRestoreManager(argchunk, getPlotSaveFolder());
		
		plotsaver.archiveOldFiles();
		
		plotsaver.SaveChunkToDisk();
		town.setHasRestore(true);
	}
	
	public void loadChunk(Chunk argchunk)
	{
		try
		{
			if(plotsaver == null)
			{
				plotsaver = new PlotRestoreManager(argchunk, getPlotSaveFolder());
				plotsaver.checkForSavedInstance();
			}
			plotsaver.RestoreChunkFromDisk();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean saveChest(Block containerblock)
	{
		try
		{
			if(plotsaver == null)
			{
				
				World wolrd = Bukkit.getWorld(town.getWorldName());
				wolrd.getChunkAt((int)x, (int)z);
				plotsaver = new PlotRestoreManager(wolrd.getChunkAt((int)x, (int)z), getPlotSaveFolder());
				plotsaver.checkForSavedInstance();
			}
			Location cloc = containerblock.getLocation();
			InventoryHolder inv = (InventoryHolder) containerblock.getState();
			boolean itemsWereSaved = plotsaver.onChestBreak(new NationsContainer(cloc, inv));
			return itemsWereSaved;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	private String getPlotSaveFolder()
	{
		return plugin.getDataFolder().getPath()+"/saves/"+town.getWorldName()+"/"+x+","+z ;
	}
	
	/*private int plotFrag(PlotLocation loc, ArrayList<PlotLocation> plots)
	{
		plots.remove(loc);
		int count = 1;
		count += plotFrag()
	}*/
}











