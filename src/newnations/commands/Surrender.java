package newnations.commands;

import java.util.ArrayList;

import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Siege;
import newnations.Town;
import newnations.User;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Surrender {

	public Surrender(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException 
	{
		if(args.length != 0 && args[0].equalsIgnoreCase("help")) //surrender || surrender help
		{
			NewNationsHelper.helpText(sender, "i.e. /surrender", "Surrender to an attacking force.");
			return;
		}
		Town town = user.getTown();
		Siege siege = null;
		ArrayList<Siege> sieges = town.getSieges();
		for(Siege s : sieges) if(s.isBesiegedTown(town)) siege = s;
		if(siege == null)
			throw new NationsException("Your town is not under siege.", null);
		if(!user.siegePriv())
		{
			for(User u : town.getUsers())
			{
				if(plugin.getServer().getPlayer(u.getName()).isOnline() && u.getSieges().contains(siege) && u.siegePriv())
					throw new NationsException("Insufficient privilege", "A soldier with siege privilege must give this order, so long as there is one.");
			}
		}
		
		siege.broadcast(ChatColor.GREEN+town.getName()+ChatColor.YELLOW+" has paid tribute to its besiegers. The siege is lifted.");
		siege.surrender();
	}
}
