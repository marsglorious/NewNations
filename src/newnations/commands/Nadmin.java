package newnations.commands;

import newnations.Nation;
import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Plot;
import newnations.PlotLocation;
import newnations.Town;
import newnations.User;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

 
public class Nadmin {

	public Nadmin(CommandSender sender, String cmdLabel, String[] args, NewNations plugin) throws NationsException
	{
			
		if(args.length >= 2 && args[0].equalsIgnoreCase("proxy"))
		{
			this.proxy(sender ,cmdLabel ,args, plugin);
		}
		if(args.length >= 2 && args[0].equalsIgnoreCase("worldrename"))
		{
			this.WorldRename(sender ,cmdLabel ,args, plugin);
		}
		
		//test function remove this at some point
		if(args.length == 1 && args[0].equalsIgnoreCase("test1"))
		{
			sender.sendMessage("Nearby towns, excluding your own");
			//Player p = (Player)sender;
			//ArrayList<Double> a = plugin.getCostToNearbyTowns(p.getLocation().getChunk().getX(), p.getLocation().getChunk().getZ(), p.getLocation().getWorld().getName(), plugin.getUser(p).getTown());

		}
		if(args.length == 1 && args[0].equalsIgnoreCase("load"))
		{
			plugin.loadAll();
		}
		if(args.length == 1 && args[0].equalsIgnoreCase("save"))
		{
			plugin.saveAll();
		}
		if(args.length >= 1 && args[0].equalsIgnoreCase("pay"))
		{
			if(args.length != 3) NewNationsHelper.helpText(sender, "i.e. /nadmin pay <town name> <amount>", "give funds to a town.");
			else payTown(sender, cmdLabel, args, plugin);
		}
		if(args.length >= 1 && args[0].equalsIgnoreCase("setbalance"))
		{
			if(args.length != 3) NewNationsHelper.helpText(sender, "i.e. /nadmin setbalance <town name> <amount>", "set a towns funds.");
			else setBalance(sender, cmdLabel, args, plugin);
		}
		
		if(args.length >= 1 && args[0].equalsIgnoreCase("forcebuy"))
		{
			if(args.length != 2) NewNationsHelper.helpText(sender, "i.e. /nadmin forcebuy <town name> ", "Gives the current plot to <town> no funds are used.");
			else forceBuy(sender, cmdLabel, args, plugin);
		}
		if(args.length >= 1 && args[0].equalsIgnoreCase("forcesell"))
		{
	
			if(args.length != 2) NewNationsHelper.helpText(sender, "i.e. /nadmin forcesell confirm ", "Removes a plot from its town, no compensation is provided");
			else forceSell(sender, cmdLabel, args, plugin);
		}
		if(args.length > 0 && args[0].equalsIgnoreCase("setrank"))
		{
			if(args.length != 3 ) NewNationsHelper.helpText(sender, "i.e. /nadmin rank <user> <rank>", "give funds to a town.");
			else
			{
				User us = plugin.getUser(args[1]);
				if(us == null)
				{
					NewNationsHelper.helpText(sender, "Cant Find user", "The user \""+args[1]+"\" cannot be found."); //TODO use exceptions
					return;
				}
				Integer rank = User.rankType.get(args[2].toLowerCase());
				us.setRank(rank);
				NewNationsHelper.helpText(sender, "done", "user has been changed.");
			}
		}
		
		if(args.length == 1 && args[0].equalsIgnoreCase("test3"))
		{
			
			
		}//
		if(args.length == 1 && args[0].equalsIgnoreCase("test33"))
		{
			PlotLocation a = new PlotLocation(2,3, "poo");
			PlotLocation b = new PlotLocation(2,3, "aa");
			PlotLocation c = new PlotLocation(3,3, "poo");
			
			System.out.print("A hashcode " + a.hashCode() + " a == b " + a.equals(b) + " a == c "+ a.equals(c));
			System.out.print("B hashcode " + b.hashCode() + " b == a " + b.equals(a) + " b == c "+ b.equals(c));
			System.out.print("C hashcode " + c.hashCode() + " c == b " + c.equals(b) + " c == a "+ c.equals(a));
			
		}//
		if(args.length == 1 && args[0].equalsIgnoreCase("test4"))
		{
			Player p = (Player)sender;
			int x = p.getLocation().getChunk().getX();
			int z = p.getLocation().getChunk().getZ();
			String wname = p.getLocation().getWorld().getName();
			
			PlotLocation plotLoc = new PlotLocation(x,z,wname);
			
			TESTFUNC(x,z,wname,plugin, sender);
			Town town = plugin.getUser(p.getName()).getTown();
			sender.sendMessage("Cost of this plot: "+Claim.getFinalCost(plugin, plotLoc, town) + ", towncost: "+ town.getNextPlotCost());
		}
		
	}
	
	public void TESTFUNC(int chunkx, int chunkz, String worldname, NewNations plugin, CommandSender sender ) 
	{
		for(Nation n : plugin.nations)
		{
			for(Town t : n.getTowns())
			{
				//if(t == exclude) continue;
				if(!t.getWorldName().equalsIgnoreCase(worldname)) continue;
				
				double closest = Double.POSITIVE_INFINITY;
				for(Plot p : t.getPlots())
				{
					double dist = Math.sqrt(Math.pow(Math.abs(p.getX() - chunkx),2) + Math.pow(Math.abs(p.getZ() - chunkz),2));
					if(dist < closest) closest = dist;
					
				}
				double a = (t.getPlots().size()*2 + 10) * 1000  * Math.pow(4, closest * -0.15);
				sender.sendMessage(t.getName() + " distance: " + closest+" "+a);
			}
		}
	}
	
	
	//this command helps admins rename worlds
	public void WorldRename(CommandSender sender, String cmdLabel, String[] args, NewNations plugin)
	{
		if(args.length == 3)
		{
			for(Nation n : plugin.nations)
			{
				for(Town t : n.getTowns())
				{
					if(t.getWorldName().equalsIgnoreCase(args[1])) t.setWorldName(args[2]);
				}
			}
		}
		else sender.sendMessage("useage /nadmin worldrename [oldworldname] [newworldname]");
	}
	
	
	public void payTown(CommandSender sender, String cmdLabel, String[] args, NewNations plugin) throws NationsException
	{
		try 
		{ 
			int amount = Integer.parseInt(args[2]);
			
			Town t = plugin.getTown(args[1]);
			if(t == null) throw new NationsException( "Invalid town", "cant find town " + args[1]);
			
			t.deposit(amount);
			sender.sendMessage(t.toString() + " has been paied: "+amount);
		}
		catch(NumberFormatException  e)
		{
			throw new NationsException( "Invalid number", "");
		}
	}
	public void setBalance(CommandSender sender, String cmdLabel, String[] args, NewNations plugin) throws NationsException
	{
		try 
		{ 
			int amount = Integer.parseInt(args[2]);
			
			Town t = plugin.getTown(args[1]);
			if(t == null) throw new NationsException( "Invalid town", "cant find town " + args[1]);
			
			t.setCoffers(amount);
			sender.sendMessage("The Balance of: "+t.toString() + " has been set to: "+amount);
		}
		catch(NumberFormatException  e)
		{
			throw new NationsException( "Invalid number", "");
		}
	}
	
	public void forceSell(CommandSender sender, String cmdLabel, String[] args, NewNations plugin) throws NationsException
	{
		Player player = (Player)sender;
		
		Plot p = plugin.getPlot(player.getLocation());
		if(p == null) throw new NationsException( "Plot Vacant", "This Plot is not currently owned by anyone ");
		double compesation = p.getPlotWorth();
		p.sellPlot(false, true);
		sender.sendMessage("The current plot has been removed from: "+p.getTown().toString()+" without compensation (valued at: "+compesation +")");
	}
	
	public void forceBuy(CommandSender sender, String cmdLabel, String[] args, NewNations plugin) throws NationsException
	{
			
			Town t = plugin.getTown(args[1]);
			if(t == null) throw new NationsException( "Invalid town", "cant find town " + args[1]);
			
			Player player = (Player)sender;
			
			Plot p = plugin.getPlot(player.getLocation());
			if(p != null) throw new NationsException( "Plot Taken", "This Plot is currently owned by: "+p.getTown().toString());
			
			//TODO make sure this doesn't deduct any funds
			t.createPlot(new PlotLocation(player.getLocation()));
			sender.sendMessage("The current plot has been given to: "+t.toString()+" free of charge");
	}
	
	public void proxy(CommandSender sender, String cmdLabel, String[] args, NewNations plugin) throws NationsException
	{
		if(args.length >= 3 )
		{
			String[] args2 = new String[args.length - 3];
			for(int i = 3; i < args.length; i++)
			{
				args2[i-3] = args[i];
				//plugin.onCommand(sender, null, args[2], args2);
			}
			User user = plugin.getUser(args[1]);
			if(user == null)
			{
				//errorText(sender, "You are not registered as a user. Registering...", null);
				user = new User(plugin, args[1]);
			//	notifyText(sender, "You are now registered.");
			}
			if(args[2].equalsIgnoreCase("nation")) 		new NationCommand(sender, args2, plugin, user);
			else if(args[2].equalsIgnoreCase("town")) 		new TownCommand(sender, args2, plugin, user);
			else if(args[2].equalsIgnoreCase("claim")) 	new Claim(sender, args2, plugin, user);
			else if(args[2].equalsIgnoreCase("raze")) 		new Raze(sender, args2, plugin, user);
			else if(args[2].equalsIgnoreCase("besiege")) 	new SiegeCommand(sender, args2, plugin, user);
			else if(args[2].equalsIgnoreCase("warcamp")) 	new Warcamp(sender, args2, plugin, user);
			else if(args[2].equalsIgnoreCase("withdraw")) 	new Surrender(sender, args2, plugin, user);
			else if(args[2].equalsIgnoreCase("alliance")) 	new AllianceCommand(sender, args2, plugin, user);
			else sender.sendMessage("unknown command");
		}
		else 
		{
			//not enough args
			sender.sendMessage("Not enough args");
		}
	}

}
