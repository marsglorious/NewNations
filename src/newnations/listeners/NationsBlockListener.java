package newnations.listeners;

import java.util.List;

import newnations.AccessManager;
import newnations.NewNations;
import newnations.Plot;
import newnations.Siege;
import newnations.Town;
import newnations.User;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.InventoryHolder;

public class NationsBlockListener implements Listener
{
	private NewNations plugin;
	
	
	public NationsBlockListener(NewNations plugin)
	{
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	public boolean canBreakOrPlaceBlock(Player p, Block b, boolean isBreakEvent)
	{
		Plot plot = plugin.getPlot(b.getLocation());
		if(plot == null) return true; //if this area is not claimed by anyone allow
		Town plotTown = plot.getTown();
		
		AccessManager plotAccess = plotTown.getAccessMan();
		User u = plugin.getUser(p);
		if( u != null && u.getTown() == plotTown ) return true;
		Siege siege = plugin.getSiege(plotTown);

		if(siege != null)
		{
			if(u != null && siege.getDefenders().contains(u.getTown())) return true;

			BlockState bs = b.getState();
			if(bs instanceof InventoryHolder && ! (bs instanceof Beacon))
			{
				if(u != null && plotAccess.containerAccess(u.getTown()) && plotAccess.blockAccess(u.getTown())) return true;
				if(!siege.isLooting())
				{
					//don't place chests
					if(isBreakEvent == false) return false;
					if(plot.saveChest(b))
					{					
						p.sendMessage(ChatColor.RED+"You will get nothing outside of the looting phase.");
						((InventoryHolder) bs).getInventory().clear();
						return true;
					}
					else return true;
				}
				else return true;
			}
			else //block is NOT a container
				if(u != null && plotAccess.blockAccess(u.getTown())) return true;
			
			if(!siege.isPrelude()) return true;
			p.sendMessage(ChatColor.RED+"(4) Land is protected during the prelude.");
			return false;
		}
		else //SIEGE is NOT in progress
		{
			BlockState bs = b.getState();
			if(bs instanceof InventoryHolder)
				if(u != null && plotAccess.containerAccess(u.getTown()) && plotAccess.blockAccess(u.getTown())) return true;
			else if(u != null &&  plotAccess.blockAccess(u.getTown())) return true;
		}
		p.sendMessage(ChatColor.RED+"(3) That land is protected.");
		return false;
	}
	
	@EventHandler(ignoreCancelled = true)
	public synchronized void onBlockPlace(BlockPlaceEvent event)
	{
		Player p = event.getPlayer();
		Block b = event.getBlock();
		if(canBreakOrPlaceBlock(p, b, false) == false) event.setCancelled(true);
	}
	 
	@EventHandler(ignoreCancelled = true)
	public synchronized void onBlockBreak(BlockBreakEvent event)
	{
		Player p = event.getPlayer();
		Block b = event.getBlock();
		if(canBreakOrPlaceBlock(p, b, true) == false) event.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public synchronized void onBlockInteract(PlayerInteractEvent event)
	{
		//pressure pads
		if(event.getAction() == Action.PHYSICAL) return;	
		Player p = event.getPlayer();
		Block b = event.getClickedBlock();
		if(canInteractBlock(p,b, event.getAction()) == false) event.setCancelled(true);
	}
	
	public boolean canInteractBlock(Player p, Block b, Action a)
	{
		Plot plot = plugin.getPlot(b.getLocation());
		if(plot == null) return true; //if this area is not claimed by anyone, allow.
	
		if(b == null || b.isEmpty()) return true;
		Material blockMat = b.getType();
		if(blockMat == Material.WOOD_BUTTON || blockMat == Material.FENCE) return true;
		if(blockMat == Material.WORKBENCH || blockMat == Material.ENDER_CHEST || blockMat ==  Material.ENCHANTMENT_TABLE) return true;
		
		Town plotTown = plot.getTown();
		User u = plugin.getUser(p);
		if (u != null && plotTown == u.getTown()) return true;

		BlockState bs = b.getState();
		if(bs instanceof InventoryHolder && !(bs instanceof Beacon))
		{
			String containerName = ((InventoryHolder) bs).getInventory().getName(); 
			if(containerName.equalsIgnoreCase("public"))  return true; //check public chest
			if(plotTown.getAccessMan().containerAccess(u.getTown())) return true; //check access
			if(u != null && containerName.equalsIgnoreCase("nation") && plotTown.getNation() == u.getTown().getNation()) return true; //check nation chest
			if(u != null && containerName.equalsIgnoreCase("alliance") && Siege.checkAllegiance(plotTown, u.getTown())) return true; //check alliance chest

			Siege siege = plugin.getSiege(plotTown);
			if(siege != null)
			{
				
				if(u != null && siege.getDefenders().contains(u.getTown())) return true; //if they are a defender allow
				//if the user is  not a defender AND the siege is not yet in looting phase AND they attempt to open the chest.
				if(!siege.isLooting() && a == Action.RIGHT_CLICK_BLOCK) 
				{
					p.sendMessage(ChatColor.RED+"You cannot open containers outside of looting phase.");
					return false;
				}	
				//if its not the prelude allow
				if(!siege.isPrelude()) return true;
			}
			p.sendMessage(ChatColor.RED+"(1) That land is protected.");
			return false;
		}
		else //NOT a container
		{
			// the user has switch access allow (the block event will handle blocks)
			if(u != null && plotTown.getAccessMan().switchAccess(u.getTown())) return true;
			
			//if there is a siege on that plot
			Siege siege = plugin.getSiege(plotTown);
			if(siege != null)
			{
				//if the user is a defender allow 
				if(u != null && siege.getDefenders().contains(u.getTown())) return true;
				
				//if the siege is in prelude phase and the user isn't a defender, deny access.
				if(siege.isPrelude()) 
				{
					p.sendMessage(ChatColor.RED+"(5) That land is protected during prelude.");
					return false;
				}
				
				//if the siege isn't a prelude (war or looting phase), allow.
				return true;
			}
			
			//user does not have switch (or block) access and there are no sieges.
			p.sendMessage(ChatColor.RED+"(2) That land is protected.");
			return false;
		}
	}
	
	@EventHandler
	public synchronized void onExplosion(EntityExplodeEvent event)
	{
		Plot p = plugin.getPlot(event.getLocation());
		if(p == null) 
		{
			event.blockList().clear();
			return;
		}
		
		List<Block> blocklist = event.blockList();
		for(int i = 0; i < blocklist.size(); i++)
		{
			if(blocklist.get(i).getState() instanceof InventoryHolder) 
			{
				blocklist.remove(i);
				i--;
			}
		}
		Town t = p.getTown();
		if(t.isDestructionOn()) return;
		event.blockList().clear();
	}
	
	@EventHandler
	public void onBlockIgnite(BlockIgniteEvent  event)
	{
		if(event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD || event.getCause() == BlockIgniteEvent.IgniteCause.LAVA)
		{
			Plot p = plugin.getPlot(event.getBlock().getLocation());
			if(p != null && !p.getTown().isDestructionOn()) event.setCancelled(true); 
		}
	}
	
	// On  Block Burn is called when fire is about to delete a block
	@EventHandler
	public void onBlockBurn(BlockBurnEvent event)
	{
		Plot p = plugin.getPlot(event.getBlock().getLocation());
		if(p != null && !p.getTown().isDestructionOn())	
		{ 
			//Instead of the fire deleting the block, the block will delete the fire, this solves the eternal burn issue
			event.setCancelled(true); 
			Block blockAbove = event.getBlock().getRelative(BlockFace.UP);
			if(blockAbove != null && blockAbove.getType() == Material.FIRE) blockAbove.setType(Material.AIR);
		}		
	} 
	
	
	
	// this function cancels the event, so they pistons dont even retract at all
	@EventHandler
	public void onBlockPistonRetract(BlockPistonRetractEvent event)
	{	
		if(!event.isSticky()) return;
		Plot retractedBlockPlot = plugin.getPlot(event.getRetractLocation());
		
		//if the block being pulled is outside territory	
		if(retractedBlockPlot == null || event.getRetractLocation().getBlock().isEmpty() ||  event.getRetractLocation().getBlock().isLiquid()) return;
		Plot pistonBlockPlot = plugin.getPlot(event.getBlock().getLocation());
		if(pistonBlockPlot != null)
		{
			if(pistonBlockPlot.getTown() != retractedBlockPlot.getTown())
				event.setCancelled(true);
		}
		else event.setCancelled(true);
	}
	
	@EventHandler
	public void onBlockPistonExtend(BlockPistonExtendEvent event)
	{
		Block pistonBlock = event.getBlock();
		Block lastBlock = pistonBlock.getRelative(event.getDirection(), event.getLength() + 1);
		if(lastBlock == null) return;
		Plot pushedBlockPlot = plugin.getPlot(lastBlock.getLocation());		
		if(pushedBlockPlot == null) return;
		Plot pistonBlockPlot = plugin.getPlot(pistonBlock.getLocation());
		if(pistonBlockPlot != null)
			if(pistonBlockPlot.getTown() != pushedBlockPlot.getTown()) event.setCancelled(true);
		else event.setCancelled(true);
	}	
}

