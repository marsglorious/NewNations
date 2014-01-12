package newnations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import javanbt.NBTCompoundTag;
import javanbt.NBTListTag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.schematic.SchematicFormat;

public class PlotRestoreManager 
{
	// a map that stores a Nations container and uses keyCode as the key, the keycode is given by y*256+z*16+x where x/z are relative chunk cords
	HashMap<Integer, NationsContainer> brokenChests = new HashMap<Integer, NationsContainer> ();
	Chunk chunk;
	String directory;
	
	File NBTFile;
	File schematicFile;
	
	NBTCompoundTag rootNBTTag = new NBTCompoundTag();
	NBTListTag brokenChestsNBTBackup = new NBTListTag();
	
	public void archiveOldFiles()
	{
		long unixTime = System.currentTimeMillis() / 1000L;
		
		if(NBTFile.exists()) 
		{
			File newloc = new File(NBTFile.getParent()+"/old-"+unixTime+"/"+NBTFile.getName());
			newloc.getParentFile().mkdirs();
			NBTFile.renameTo(newloc);
		}
		if(schematicFile.exists()) 
		{
			File newloc = new File(schematicFile.getParent()+"/old-"+unixTime+"/"+schematicFile.getName());
			newloc.getParentFile().mkdirs();
			schematicFile.renameTo(newloc);
		}
	}
	
	public PlotRestoreManager(Chunk chunk, String directory)
	{
		this.chunk = chunk;
		this.directory = directory;
		
		rootNBTTag.setTag("BrokenBesiegerChests", brokenChestsNBTBackup);
		
		schematicFile = new File(String.format("%s/restore.schematic", directory));
		schematicFile.getParentFile().mkdirs();
		NBTFile = new File(String.format("%s/restre.nbt", directory));
		NBTFile.getParentFile().mkdirs();
	}
	
	// if the plugin gets reloaded run this, it will copy all the data from the nbt back into memory
	public void checkForSavedInstance() throws IOException, ClassNotFoundException
	{
		if(NBTFile.exists())
		{
			NBTCompoundTag loadedRoot = NBTCompoundTag.loadFromFile(NBTFile);
			rootNBTTag = (NBTCompoundTag) loadedRoot.getTag("root");
			
			brokenChestsNBTBackup  = (NBTListTag) rootNBTTag.getTag("BrokenBesiegerChests");
			
			int size = brokenChestsNBTBackup.getSize();
			for(int i = 0; i < size; i++)
			{
				NBTCompoundTag thisTag = (NBTCompoundTag) brokenChestsNBTBackup.getTag(i);
				NationsContainer thisContainer = new NationsContainer(thisTag, chunk);
				brokenChests.put(thisContainer.getKeyCode(), thisContainer);
			}
		}
	}
	
	//call this when a chest gets broken by a besieger outside of looting time
	// returns true if the chest was saved
	public boolean onChestBreak(NationsContainer brokenChest)
	{
		try
		{
			int keyCode = brokenChest.getKeyCode();
			
			if(brokenChests.containsKey(keyCode)) return false;
			
			//store the chest contents in main memory
			brokenChests.put(keyCode, brokenChest);
			
			//also backup to disk
			NBTCompoundTag thisChest = new NBTCompoundTag();
			brokenChest.save(thisChest);			
			brokenChestsNBTBackup.addTag(thisChest);
			
			// save
			rootNBTTag.saveToFile(NBTFile, "root");
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}
	
	ArrayList<Integer> valubleBlockIds = new ArrayList<Integer>();
	ArrayList<Material> valubleBlockMaterials = new ArrayList<Material>();
    
    
	public void RestoreChunkFromDisk()
	{
		valubleBlockIds.add(41); 
		valubleBlockIds.add(42); 
		valubleBlockIds.add(57); 
		valubleBlockIds.add(113); 
		valubleBlockIds.add(138); 
		
		valubleBlockMaterials.add(Material.IRON_BLOCK);
		valubleBlockMaterials.add(Material.DIAMOND_BLOCK);
		valubleBlockMaterials.add(Material.GOLD_BLOCK);
		valubleBlockMaterials.add(Material.BEACON);
		valubleBlockMaterials.add(Material.EMERALD_BLOCK);
		
		ArrayList<BlockState> valubleBlocsToRmove = new ArrayList<BlockState>();
		
		if(schematicFile.exists() == false) return;		
		try
		{
			EditSession editSession = new EditSession(new BukkitWorld(chunk.getWorld()), 131073);
			CuboidClipboard clipboard = SchematicFormat.MCEDIT.load(schematicFile);
						
			Vector newvec = locationToVector(chunk.getBlock(0, 0, 0).getLocation());
			
			for(int y = 0; y < 256; y ++)
			{
				for(int z = 0; z < 16; z++)
				{
					for(int x = 0; x < 16 ; x++)
					{
						BaseBlock block = clipboard.getBlock(new Vector(x,y,z));
						Block existingBlock = chunk.getBlock(x, y, z);
						
						// if the schematic contains a valuable block at this position save what is currently there
						// that way we dont restore any valuable block and instead leave whatever was there before this command
						if(valubleBlockIds.contains(block.getId())) 
						{
							System.out.print("Valuable Block Detected" + block.getId());
							
							// we will revert the valuable block back to its post was state after the restore
							valubleBlocsToRmove.add(existingBlock.getState());
						}
						
						
						// of if the location has a valuable block save it
						else if(valubleBlockMaterials.contains(existingBlock.getType()))
						{
							System.out.print("Valuable Block Detected in current world");
							valubleBlocsToRmove.add(existingBlock.getState());
						}
					}
				}
			}
			
			HashMap<Integer,NationsContainer> PostWarContents = saveExistingChests(chunk);
			
			// paste twice it helps fix any rail issues
			clipboard.paste(editSession, newvec, false, true);
			clipboard.paste(editSession, newvec, false, true);		
			
			// revert any valuable blocks back to whatever they were before this command was executed
			for(BlockState bs : valubleBlocsToRmove)
			{
				System.out.print("Valuable Block Updated" + bs);
				bs.update(true);				
			}

			restoreChests(chunk, PostWarContents, brokenChests );
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}
	// saves this chunk as a schematic in the directory
	public void SaveChunkToDisk()
	{
		saveWEChunk(chunk,schematicFile);
	}
		
	private void restoreChests(Chunk chunk, HashMap<Integer,NationsContainer> PostWarContents , HashMap<Integer,NationsContainer> BrokenBesigerChests  )
	{
		for(int y = 0; y < 256; y++)
		{
			for(int z = 0 ; z < 16; z++)
			{
				for(int x = 0; x < 16; x++)
				{
					int index = y*256 + z*16 + x;
					Block b = chunk.getBlock(x,y,z);
					
					BlockState bs = b.getState();
					
					//send a block update, helps to prevent redstone getting stuck
					bs.update();
					
					if(bs instanceof InventoryHolder)
					{
						InventoryHolder invHolder = (InventoryHolder)bs;
						invHolder.getInventory().clear();
						
						if(PostWarContents.containsKey(index))
						{
							NationsContainer postWarChest = PostWarContents.get(index);
							
							//if a besieger broke this chest and the someone replaced it
							if(BrokenBesigerChests.containsKey(index))
							{								
								NationsContainer brokenChest = BrokenBesigerChests.get(index);
								
								//restore it to just before the besieger broke it
								if(brokenChest.getInventoryType() == invHolder.getInventory().getType())
									invHolder.getInventory().setContents(brokenChest.getItemStacks());
								else
									dropItems(b.getLocation(), brokenChest.getItemStacks());
								
								//remove it from the list to avoid any duplication
								BrokenBesigerChests.remove(index);
								
								//the contents of the chest after the chest was replaced gets dropped
								dropItems(b.getLocation(), postWarChest.getItemStacks());
							}
							else
							{
								// if the chest was unharmed
								if(postWarChest.getInventoryType() == invHolder.getInventory().getType())
								{
									invHolder.getInventory().setContents(postWarChest.getItemStacks());
								}
								else
								{
									dropItems(b.getLocation(), postWarChest.getItemStacks()); 
								}
								
							}
							PostWarContents.remove(index);
						}
						// if a besieger broke a chest and no one replaced it
						else if(BrokenBesigerChests.containsKey(index))
						{
							NationsContainer brokenChest = BrokenBesigerChests.get(index);
							
							if(brokenChest.getInventoryType() == invHolder.getInventory().getType())
								invHolder.getInventory().setContents(brokenChest.getItemStacks());							
							else
								dropItems(b.getLocation(), brokenChest.getItemStacks()); 
							
							BrokenBesigerChests.remove(index);
						}						
					}					
				}
			}
		}
		
		//drop any remaining items
		Collection<NationsContainer> values = PostWarContents.values();
		for(NationsContainer postWarChest : values)
		{
			dropItems(postWarChest.getLocation(), postWarChest.getItemStacks());
		}
		
		Collection<NationsContainer> keySet2 = BrokenBesigerChests.values();
		for(NationsContainer brokenChest : keySet2)
		{
			dropItems(brokenChest.getLocation(), brokenChest.getItemStacks());
		}
		
	}
	
	

	public void dropItems(Location loc, ItemStack[] items)
	{
		World w = loc.getWorld();
		for(ItemStack item : items) if(item != null )w.dropItem(loc, item);
	}
	
	
	private HashMap<Integer,NationsContainer> saveExistingChests(Chunk chunk)
	{
		HashMap<Integer,NationsContainer> PostWarContents = new HashMap<Integer,NationsContainer>();
		
		for(int y = 0; y < 256; y++)
		{
			for(int z = 0 ; z < 16; z++)
			{
				for(int x = 0; x < 16; x++)
				{
					Block b = chunk.getBlock(x,y,z);
					BlockState bs = b.getState();
					
					if(bs instanceof InventoryHolder)
					{	
						InventoryHolder invHolder = (InventoryHolder)b.getState();
						
						int index = y*256 + z*16 + x;
						PostWarContents.put(index, new NationsContainer(b.getLocation(), invHolder) );		
						
						//Worldedit had some errors when changing a furnace to a chest
						//so we will just remove the problem block before the restore
						invHolder.getInventory().clear();
						b.setType(Material.STONE);
					}
				}
			}
		}
		return PostWarContents;
	}
	
	
	
	private void saveWEChunk(Chunk chunk, File saveFile)
	{
		WorldEditPlugin wep = (WorldEditPlugin)Bukkit.getPluginManager().getPlugin("WorldEdit");
		if (wep == null) 
		{
			Bukkit.broadcastMessage(ChatColor.RED+"Unable to find WorldEdit plugin - Unable to restore.");
			return;
		}		
		EditSession editSession = new EditSession(new BukkitWorld(chunk.getWorld()), 1000);		
		Vector startPos =  locationToVector(chunk.getBlock(0, 0, 0).getLocation());
		Vector size = new Vector(16,256,16);
		
		CuboidClipboard clipboard = new CuboidClipboard(size, startPos);
		clipboard.copy(editSession);
		try 
		{
			SchematicFormat.MCEDIT.save(clipboard, saveFile);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}		
	}
	
	private Vector locationToVector(Location loc)
	{		
		return new Vector(loc.getX(), loc.getY(), loc.getZ());
	}
}
