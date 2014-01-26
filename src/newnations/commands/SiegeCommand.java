package newnations.commands;

import newnations.Econ;
import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Siege;
import newnations.Town;
import newnations.User;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SiegeCommand 
{
	public SiegeCommand(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException 
	{
		if(args.length == 0 || args[0].equalsIgnoreCase("help")) //siege || siege help
		{
			NewNationsHelper.helpText(sender, "i.e. /siege <townname>", "Besiege a town or come to its aid.");
			listSieges(user, (Player)sender);
			return;
		}
		
		Town town = user.getTown();
		if(town == null) 
			throw new NationsException("You cannot declare siege.", "You must be in a town to siege.");
		Town besiegedTown = plugin.getTown(NewNationsHelper.connectStrings(args, 0));
		if(besiegedTown == null) 
			throw new NationsException("That town doesn't exist.", null);
		if(besiegedTown == town) 
			throw new NationsException("You cannot besiege your own town.", null);
		if(besiegedTown.getNation() == town.getNation()) 
			throw new NationsException("You cannot besiege a town in your own nation.", null);
		
		//TODO possibly display the alliance that links them
		if(Siege.checkAllegiance(besiegedTown, town)) 
			throw new NationsException("You cannot besiege a town that is in an alliance with your town.", null);
		
		Siege siege = plugin.getSiege(besiegedTown);
		if(siege != null) //add a new town
		{
			if(siege.getDefenders().contains(town) || siege.getBesiegers().contains(town)) throw new NationsException("You are already in the siege.", null);
			if(user.getRank() == 0) 
				throw new NationsException("Insufficient privilege", "You must be above standard rank to have your town join the siege.");
			if(!(besiegedTown.getNation() == town.getNation()) && !(siege.getBesiegers().get(0).getNation() == town.getNation()))
			{ //If town is merely allied, make them pay the SIEGE_FEE.
				if(town.getCoffers() < Econ.WARCAMP_FEE)
					throw new NationsException("Insufficent funds in town coffers to siege.", "You require "+ChatColor.GREEN+"$"+Econ.WARCAMP_FEE+ChatColor.YELLOW+".");
				town.withdraw(Econ.WARCAMP_FEE);
			}
			if(!siege.addTown(town))
				throw new NationsException("You are not allied or part of any involved nation.", "You must ally with an involved nation to join a side.");
			siege.broadcast(ChatColor.RED+town.getName()+" has been added to the siege at "+ChatColor.GREEN+besiegedTown.getName());
		}
		else //create a new siege
		{
			//sender.sendMessage("Expire: "+besiegedTown.immuneExpire/60000+" Current: "+System.currentTimeMillis()/60000+" "+(besiegedTown.getImmuneExpire()/60000-System.currentTimeMillis()/60000));
			if(!user.siegePriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to declare siege.");
			if(town.getCoffers() < Econ.SIEGE_FEE) 
				throw new NationsException("Insufficent funds in town coffers to siege.", "You require "+ChatColor.GREEN+"$"+Econ.SIEGE_FEE+ChatColor.YELLOW+".");
			if(besiegedTown.isImmuned())
				throw new NationsException("That town is currently immuned to siege.", "Immunity expires in "+ChatColor.GOLD+(besiegedTown.getImmuneExpire()/60000-System.currentTimeMillis()/60000)+ChatColor.YELLOW+" minute(s).");
			town.withdraw(Econ.SIEGE_FEE);
			siege = new Siege(besiegedTown, town, plugin);
			if(siege != null && plugin.getConfig().contains("besiegeLighting") && plugin.getConfig().getBoolean("besiegeLighting")) 
				((Player)sender).getWorld().strikeLightningEffect(((Player)sender).getLocation()); //Lighting effect on command.
		}
	}
	
	private void listSieges(User u, Player p)
	{
		for(Siege s : u.getSieges())
		{
			p.sendMessage(ChatColor.RED+"** [ "+ChatColor.GOLD+s.getDefenders().get(0).getName()+ChatColor.RED+" ] **");
			p.sendMessage(ChatColor.GREEN+"Defender Deathtoll: "+ChatColor.GOLD+s.getDefenderDeathtoll());
			p.sendMessage(ChatColor.GREEN+"Besieger Deathtoll: "+ChatColor.GOLD+s.getBesiegerDeathtoll());
			p.sendMessage(ChatColor.GREEN+"Towns: ");
			for(Town t : s.getDefenders())
				p.sendMessage(ChatColor.GREEN+t.getName());
			for(Town t : s.getBesiegers())
				p.sendMessage(ChatColor.RED+t.getName());
			p.sendMessage(ChatColor.GREEN+"Users: ");
			for(User user : s.getUsers().keySet())
				p.sendMessage(ChatColor.GREEN+user.getName()+": "+ChatColor.GOLD+s.getUsers().get(user));
			p.sendMessage(ChatColor.GREEN+"Defender Lifepool: "+ChatColor.GOLD+s.getDefenderLife());
			p.sendMessage(ChatColor.GREEN+"Besieger Lifepool: "+ChatColor.GOLD+s.getBesiegerLife());
			p.sendMessage(ChatColor.RED+"+---------------------------+");
		}
	}
}
