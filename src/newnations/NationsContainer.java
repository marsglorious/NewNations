package newnations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javanbt.NBTByteArrayTag;
import javanbt.NBTCompoundTag;
import javanbt.NBTShortTag;
import javanbt.NBTStringTag;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class NationsContainer 
{
	public Location location;
	public ItemStack[] items;
	public InventoryType invType;
	
	public NationsContainer(Location loc, InventoryHolder invhold)
	{
		this.location = loc;
		
		invType = invhold.getInventory().getType();
		items = invhold.getInventory().getContents().clone();
	}
	
	public NationsContainer(NBTCompoundTag rootTag, Chunk chunk) throws IOException, ClassNotFoundException
	{		
		int x = ((NBTShortTag)rootTag.getTag("X")).getValue() + chunk.getX() * 16;
		int y = ((NBTShortTag)rootTag.getTag("Y")).getValue();
		int z = ((NBTShortTag)rootTag.getTag("Z")).getValue() + chunk.getZ() * 16;
		
		location = new Location(chunk.getWorld(), x,y,z);
		
		byte[] itemsRaw = ((NBTByteArrayTag)rootTag.getTag("BukkitItemStackArray")).getValue();
		ByteArrayInputStream itemsStream = new ByteArrayInputStream(itemsRaw);		
		BukkitObjectInputStream bois = new BukkitObjectInputStream(itemsStream);
		
		items = (ItemStack[])bois.readObject();		
		bois.close();
		
		invType = InventoryType.valueOf(((NBTStringTag)rootTag.getTag("InventoryType")).getValue());
	}
	
	public void save(NBTCompoundTag rootTag) throws IOException
	{
		rootTag.setTag("X", new NBTShortTag((short)(location.getBlockX() - location.getChunk().getX() * 16)));
		rootTag.setTag("Y", new NBTShortTag((short)location.getBlockY()));
		rootTag.setTag("Z", new NBTShortTag((short)(location.getBlockZ() - location.getChunk().getZ() * 16)));
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
		boos.writeObject( items );
		
		rootTag.setTag("BukkitItemStackArray", new NBTByteArrayTag(baos.toByteArray()));
		boos.close();
		
		rootTag.setTag("InventoryType", new NBTStringTag(invType.name()));
		
	}
	
	public int getKeyCode()
	{
		int x = location.getBlockX() - location.getChunk().getX() * 16;
		int y = location.getBlockY(); 
		int z = location.getBlockZ() - location.getChunk().getZ() * 16;
		return y*256 + z*16 + x;
	}
	
	public Location getLocation() {return location;}
	public InventoryType getInventoryType() {return invType;}
	public ItemStack[] getItemStacks() {return items;}
}
