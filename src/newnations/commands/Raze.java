package newnations.commands;

import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Plot;
import newnations.Town;
import newnations.User;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Raze {

	public Raze(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException 
	{
		if(args.length > 0 && args[0].equalsIgnoreCase("help")) //raze help
		{
			NewNationsHelper.helpText(sender, "i.e. /raze", "Raze a plot for your town.");
			return;
		}
		
		Town town = user.getTown();
		if(town == null)
			throw new NationsException("You are not in a town.", "You have to be in a town to raze.");
		if(!user.plotPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to raze plots.");
		if(!town.getSieges().isEmpty())
			throw new NationsException( "You cannot raze during a siege.", "You must wait for to siege to conclude to claim or raze plots.");
		
		Plot plot = plugin.getPlot(((Player)sender).getLocation());		
		if(plot == null || plot.getTown() != town)
			throw new NationsException("Your town does not own a plot there.", "You can only raze plots you own.");
		plot.sellPlot(true, false);

		//Inform everyone in the town of the raze.
		town.notifyText("Plot razed.", null);
	}

}
