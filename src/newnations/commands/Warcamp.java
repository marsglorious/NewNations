package newnations.commands;

import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Siege;
import newnations.Town;
import newnations.User;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Warcamp 
{
	private static final int    BIG_ENOUGH_INT   = 16 * 1024;
	private static final double BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT;
	public Warcamp(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException 
	{
		if(args.length > 1 && args[0].equalsIgnoreCase("help")) //siege || siege help
		{
			NewNationsHelper.helpText(sender, "i.e. /warcamp", "Set warcamp to your current location for this siege.");
			return;
		}
		Town town = user.getTown();
		if(town == null)
			throw new NationsException("You aren't in a town.", "You must be in a town to set warcamp.");
		if(user.getSieges().isEmpty())
			throw new NationsException("You cannot set a warcamp.", "You must be in a siege.");
		Location l = ((Player)sender).getLocation();
		for(Siege s : user.getSieges())
		{
			if(!user.siegePriv())
			{
				for(User u : town.getUsers())
				{
					if(plugin.getServer().getPlayer(u.getName()).isOnline() && u.getSieges().contains(s) && u.siegePriv())
						throw new NationsException("Insufficient privilege", "A soldier with siege privilege must give this order, so long as there is one.");
				}
			}
			int chunkx = ((int) (l.getX() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4; 
			int chunkz = ((int) (l.getZ() + BIG_ENOUGH_FLOOR) - BIG_ENOUGH_INT) >> 4; 
			
			if(!s.inRange(chunkx, chunkz)) 
				user.notifyUser("You have put this warcamp outside the range of the siege of "+ChatColor.RED+s.getDefenders().get(0).getName()+ChatColor.YELLOW+".");
				
			s.setWarcamp(user.getTown(), ((Player)sender).getLocation());
			town.notifyText("Your warcamp has been set for siege at "+ChatColor.RED+s.getDefenders().get(0).getName()+ChatColor.YELLOW+".", null);
		}
		//TODO: second level rank to change warcamp, unless no one above second level rank is online. 
	}
}