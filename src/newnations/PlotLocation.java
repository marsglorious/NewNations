package newnations;

import org.bukkit.Location;

public class PlotLocation 
{
	private static final int    BIG_ENOUGH_INT   = 16 * 1024;
	private static final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
	
	private String worldName;
	private long chunkx;
	private long chunkz;
	
	public PlotLocation(long chunkx, long chunkz, String worldName)
	{
		this.worldName = worldName;
		this.chunkx = chunkx;
		this.chunkz = chunkz;
	}
	
	public PlotLocation(Location l)
	{
		int x = (int) (l.getX() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
		int z = (int) (l.getZ() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT;
		worldName = l.getWorld().getName();
		
		chunkx = x >> 4;
		chunkz = z >> 4;
	}
	public String getWorldName() { return worldName; }
	
	public long getChunkX() { return chunkx; }
	public long getChunkZ() { return chunkz; }
	
	@Override
	public boolean equals(Object other1)
	{
		if(other1 instanceof PlotLocation == false) return false;
		PlotLocation other = (PlotLocation) other1;
		if(other.chunkx != chunkx) return false;
		if(other.chunkz != chunkz) return false;
		if(!worldName.equalsIgnoreCase(other.worldName)) return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		int xx  = (int) (chunkx << 16);
		xx += chunkz;
		return (int) (xx ^ worldName.hashCode());
	}
}
