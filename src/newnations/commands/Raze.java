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
import java.util.ArrayList;

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
		if(town.getPlots().size() == 1)
			throw new NationsException("You cannot raze your last plot.", "You must disband the town. If you wish to move, see '/town relocate help'.");
		Plot chkPlot = town.getPlots().get(0) == plot ? town.getPlots().get(1) : town.getPlots().get(0);
		
		if(!contiguous(plot))
			throw new NationsException("You cannot raze the plot there.", "Town plot must be contiguous and cannot be split into two parts.");
		plot.sellPlot(true, false);

		//Inform everyone in the town of the raze.
		town.notifyText("Plot razed.", null);
	}

	private int contiguityCount(Plot p, ArrayList<Plot> excluded) 
	{
		if(p == null) return 0;
		else if(excluded.contains(p)) return 0;
		excluded.add(p);

		Town t = p.getTown(); 
		int cnt = 1;
		cnt += contiguityCount(t.getPlot((int)p.getX(), (int)p.getZ() + 1), excluded);
		cnt += contiguityCount(t.getPlot((int)p.getX() + 1, (int)p.getZ()), excluded);
		cnt += contiguityCount(t.getPlot((int)p.getX(), (int)p.getZ() - 1), excluded);
		cnt += contiguityCount(t.getPlot((int)p.getX() - 1, (int)p.getZ()), excluded);
		return cnt;
	}

	private boolean contiguous(Plot p)
	{
		if(p == null) return false;
		Town t = p.getTown(); 
		ArrayList<Plot> excluded = new ArrayList<Plot>();
		excluded.add(p);
		if(t.getPlot((int)p.getX(), (int)p.getZ() + 1) != null)
			return (contiguityCount(t.getPlot((int)p.getX(), (int)p.getZ() + 1), excluded) + 1) == t.getPlots().size();
		if(t.getPlot((int)p.getX() + 1, (int)p.getZ()) != null)
			return (contiguityCount(t.getPlot((int)p.getX() + 1, (int)p.getZ()), excluded) + 1) == t.getPlots().size();
		if(t.getPlot((int)p.getX(), (int)p.getZ() - 1) != null)
			return (contiguityCount(t.getPlot((int)p.getX(), (int)p.getZ() - 1), excluded) + 1) == t.getPlots().size();
		if(t.getPlot((int)p.getX() - 1, (int)p.getZ()) != null)
			return (contiguityCount(t.getPlot((int)p.getX() - 1, (int)p.getZ()), excluded) + 1) == t.getPlots().size();
		return false;
	}
}


