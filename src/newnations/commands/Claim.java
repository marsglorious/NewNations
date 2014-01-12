package newnations.commands;

import newnations.Econ;
import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Plot;
import newnations.PlotLocation;
import newnations.Town;
import newnations.User;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Claim {

	public Claim(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException 
	{
		if(args.length >= 1 && args[0].equalsIgnoreCase("help")) //claim help
		{
			NewNationsHelper.helpText(sender, "i.e. /claim", "Claims a plot for your town.");
			return;
		}
		if(args.length >= 1 && args[0].equalsIgnoreCase("price")) //claim help
		{
			getPlotPrices(sender, args, plugin, user);
			return;
		}
		Town town = user.getTown();
		if(town == null)
			throw new NationsException("You are aren't in a town.", "You need to be in a town to claim.");
		if(!user.plotPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to claim plots.");
		if(!town.getSieges().isEmpty())
			throw new NationsException("You cannot claim during a siege.", "You must wait for to siege to conclude to claim or raze plots.");
		
		//TODO: check nearby plots - done, it now chesks for connected plots, still needs pricing
		Plot plot;
		int x, z;
		World w;  
		String wname;
		
		//This section is for the /nadmin proxy command, it allows claiming a plot from the console, eg /claim -2 4 [worldname]
		if(args.length == 2 || args.length == 3)
		{
			try 
			{
				x = Integer.parseInt(args[0]);
				z = Integer.parseInt(args[1]);
				if(args.length == 2 )
				{
					wname = town.getWorldName();
					w = plugin.getServer().getWorld(wname);
				}
				else 
				{
					w = plugin.getServer().getWorld(args[0]);
					wname = args[2];
				}
				if(w == null)
					throw new NationsException("Unknown World.", "the world "+ wname + " could not be located.");
				NewNationsHelper.notifyText(sender, "Plot successfully purchased.");
			}
			catch(Exception e) {return;}
		}
		else 
		{
			Chunk c = ((Player)sender).getLocation().getChunk();
			x = c.getX();
			z = c.getZ();
			w = c.getWorld();
			wname = w.getName();
		}
		
		PlotLocation plotLoc = new PlotLocation(x,z,wname);
		
		int newPlotCost = plugin.getPlotFinalCost(plotLoc, town);
		if(newPlotCost > town.getCoffers()) 
			throw new NationsException("Insufficent funds.", "The plot costs "+ChatColor.GREEN+"$"+newPlotCost+ChatColor.YELLOW+" - Current coffers: "+ChatColor.BLUE+"$"+town.getCoffers()); 
		if(!town.checkAdjacent(x,z,wname))
			throw new NationsException("You cannot claim here.", "You can only claim next to an existing plot you own.");
		plot = town.createPlot(plotLoc);
		if(plot == null)
			throw new NationsException("You cannot claim here.", "You can't claim territory that is already claimed.");
		town.withdraw(newPlotCost);
		NewNationsHelper.notifyText(sender, "Cost of this plot: "+ChatColor.BLUE+"$"+getFinalCost(plugin, plotLoc, town));
		plot.placeTorches();
		town.notifyText("Plot claimed.", null); //notify the whole town of the plot claim.
		plugin.addCachedPlot(plotLoc, plot);
		plugin.notifyPlotChange();
		plugin.onEdit();
	}
	
	public void getPlotPrices(CommandSender sender, String[] args, NewNations plugin, User user)
	{
		Chunk c = ((Player)sender).getLocation().getChunk();
		int x = c.getX();
		int z = c.getZ();
		
		String wname = c.getWorld().getName();
		PlotLocation plotLoc = new PlotLocation(x,z,wname);
		
		Town town = user.getTown();
		Plot plot = plugin.getPlot(plotLoc);

		if(plot == null && user.getTown() == null)
		{
			sender.sendMessage("This plot is currently vacant");
			int plotFee = plugin.getCostToNearbyTowns(plotLoc, null);
			int townSetupFee = Econ.TOWN_SETUP_FEE;
			sender.sendMessage("Price for you to create a new town here: (X:"+x +" Z:"+z+")");
			sender.sendMessage("Plot Fee:           "+plotFee);
			sender.sendMessage("Town setup Fee:     "+townSetupFee);
			sender.sendMessage("Total:              "+(townSetupFee+plotFee));
		}
		else if(plot == null && user.getTown() != null)
		{
			sender.sendMessage("This plot (X:"+x +" Z:"+z+") is currently vacant");
			int plotFee = plugin.getCostToNearbyTowns(plotLoc, town);
			int townExpandFee = town.getNextPlotCost();
			sender.sendMessage("Price for your town to buy this plot:");
			sender.sendMessage("Plot Fee:           "+plotFee);
			sender.sendMessage("Town Expansion Fee: "+townExpandFee);
			sender.sendMessage("Total:              "+(plotFee+townExpandFee));
		}
		else if(plot.getTown() == user.getTown())
		{
			sender.sendMessage("Your town already owns this plot (X:"+x +" Z:"+z+")");
			int plotFee = plugin.getCostToNearbyTowns(plotLoc, town);
			int townExpandFee = town.getCurrentPlotCost();
			sender.sendMessage("Profit for your town to sell this plot:");
			sender.sendMessage("Plot Fee:           "+plotFee);
			sender.sendMessage("Town Expansion Fee: "+townExpandFee);
			sender.sendMessage("Total:              "+(plotFee+townExpandFee));
		}
		else
		{
			sender.sendMessage("This plot ( X:"+x +" Z:"+z+") is currently owned by "+town.getName());
			int plotFee = plugin.getCostToNearbyTowns(plotLoc, town);
			int townExpandFee = town.getNextPlotCost();
			sender.sendMessage("Price for your town to buy this plot:");
			sender.sendMessage("Plot Fee:           "+plotFee);
			sender.sendMessage("Town Expansion Fee: "+townExpandFee);
			sender.sendMessage("Total:              "+(plotFee+townExpandFee));
		}

	}
	
	public static double getFinalCost(NewNations plugin, PlotLocation plotLoc, Town town)
	{
		double totalcost = plugin.getCostToNearbyTowns(plotLoc, town);
		if(town != null) totalcost += town.getNextPlotCost();
		return totalcost;
	}
}
