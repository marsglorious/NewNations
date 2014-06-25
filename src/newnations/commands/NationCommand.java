package newnations.commands;

import newnations.AccessManager;
import newnations.Nation;
import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Town;
import newnations.User;
import newnations.invites.AllianceNationInvite;
import newnations.invites.NationTownInvite;

import org.bukkit.ChatColor;

import org.bukkit.command.CommandSender;

public class NationCommand
{
	private NewNations plugin;
	private String[] args;
	private CommandSender sender;
	private User user;
	
	public NationCommand(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException 
	{
		this.plugin = plugin;
		this.args = args;
		this.sender = sender;
		this.user = user;
		//nation
		if(args.length == 0) 
		{	
			Town town = user.getTown();
			if(town == null)
				throw new NationsException("You are a wildling, you have no town.", "You can join an existing town or found your own. See '/town help'");
			NewNationsHelper.notifyText(sender, "Type '/nation help' for more information.");
			showInfo(town, sender, plugin);
			return;
		}
		
		//nation help
		if(args[0].equalsIgnoreCase("help")) 
		{
			NewNationsHelper.helpText(sender, "i.e. /nation [rename|recode|invite|uninvite|accept|access|capital|kick|list|leave]", 
					"Nation manipulation commands. '/nation [subcommand]' for more help.");
		}	
		else if(args[0].equalsIgnoreCase("invite"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //nation invite || /nation invite help
			{
				NewNationsHelper.helpText(sender, "i.e. /nation invite <town name>", "Invite a town to your nation.");
				listInvitesSent();
			}
			else invite();
		}
		else if(args[0].equalsIgnoreCase("uninvite"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //nation uninvite || /nation uninvite help
			{
				NewNationsHelper.helpText(sender, "i.e. /nation uninvite <town name>", "Uninvite a town you have invited to your nation.");
				listInvitesSent();
			}
			else uninvite();
		}
		//a town accepts an invitation to jon a nation
		else if(args[0].equalsIgnoreCase("accept"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //nation uninvite || /nation uninvite help
			{
				NewNationsHelper.helpText(sender, "i.e. /nation accept <nation name>", "Accept a nation onvite.");
				listInvites();
			}
			else accept();
		}
		else if(args[0].equalsIgnoreCase("access"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //nation uninvite || /nation uninvite help
			{
				NewNationsHelper.helpText(sender, "i.e. /nation access <townname/towncode> <access>", "Change access to your town for residents of other towns in your nation.");
				NewNationsHelper.notifyText(sender, "\"/nation access Exampletown switch\" - adds the ability of residents of Exampletown to uses switches/open doors in your town.");
				NewNationsHelper.notifyText(sender, "\"block\" allows breaking/placing blocks, \"container\" allows opening of chests and \"none\" removes all access.");
				listNationAccess(user);
			}
			else access();
		}
		else if(args[0].equalsIgnoreCase("kick"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //nation kick || /nation kick help
			{
				NewNationsHelper.helpText(sender, "i.e. /nation kick <town name>", "Kick a town from your nation.");
				listInvites();
			}
			else kick();
		} 
		else if(args[0].equalsIgnoreCase("leave"))
		{
			if(args[1].equalsIgnoreCase("help")) //nation leave help
				NewNationsHelper.helpText(sender, "i.e. /nation leave", "Leave the nation you are part of.");
			else leave();
		}
		else if(args[0].equalsIgnoreCase("capital"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //nation capital || nation capital help
				NewNationsHelper.helpText(sender, "i.e. /nation capital <town name>", "Transfer capital status to another town in your nation.");
			else capital();
		}
		else if(args[0].equalsIgnoreCase("rename"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //nation capital || nation capital help
				NewNationsHelper.helpText(sender, "i.e. /nation rename <newnationname>", "Change the name of your town.");
			else rename();
		}
		else if(args[0].equalsIgnoreCase("recode"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //nation recode || /nation recode help
				NewNationsHelper.helpText(sender, "i.e. /nation code <newtowncode>", "Recode your nation code.");
			else recode(sender, args, plugin, user);
		}
		else if(args[0].equalsIgnoreCase("list")) //nation list
		{
			listNations();
		}
		else throw new NationsException("No such subcommand exists.", "Type /nation help for more information.");
		//TODO: nation leave makes new nation, check name
	}
	
	public void invite() throws NationsException
	{
		if(!user.leaderPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to invite other towns to the nation.");
		String townName = NewNationsHelper.connectStrings(args, 1);
		//check if nations exists
		Town invitee = plugin.getTown(townName);
		
		if(invitee == null)
			throw new NationsException("Invalid Name", "Unable to invite "+ChatColor.GREEN+townName+ChatColor.YELLOW+" that town does not exist.");
		//create invite object
		Nation thisNation = user.getTown().getNation();
		
		if(invitee.getNation() == thisNation) 
			throw new NationsException("Already in the nation", "Unable to invite "+ChatColor.GREEN+townName+ChatColor.YELLOW+" because its already in the nation.");
		
		plugin.getInviteManager().inviteTownToNation(thisNation, invitee);
		
		invitee.notifyText("Your town has been invited to join the nation of "+ChatColor.GREEN+thisNation.toString()+ChatColor.YELLOW+".", null);
		thisNation.notifyText(ChatColor.GREEN+invitee.toString()+ChatColor.YELLOW+" has been invited.", null);
	}
	
	public void accept() throws NationsException
	{
		if(!user.leaderPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to change nation.");
		String nationName = NewNationsHelper.connectStrings(args, 1);
		Nation nat = plugin.getNation(nationName);
		
		if(nat == null) 
			throw new NationsException("Invalid Name", "The nation "+ChatColor.RED+nationName+ChatColor.YELLOW+" doesn't exist.");
		
		for(NationTownInvite invite : plugin.getInviteManager().getNationInvitesForTown(user.getTown()))
		{
			if(invite.getSendingNation() == nat)
			{
				user.getTown().switchNation(nat);
				invite.invalidate();
				NewNationsHelper.notifyText(sender, "Your town is now part of the nation of "+ChatColor.GREEN+nat.getName()+ChatColor.YELLOW+".");
				return;
			}
		}
		throw new NationsException("Invalid Name", "Your town was not invited to the nation of "+ChatColor.GREEN+nat.getName()+ChatColor.YELLOW+".");
	}
	
	public void access() throws NationsException
	{
		if(!user.rankPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to change nation access to your town.");
		String accessString = args[args.length - 1];
		args[args.length - 1] = null;
		Town accessee = plugin.getTown(NewNationsHelper.connectStrings(args, 1));
		if(accessee == null)
			throw new NationsException("Invalid name", "No town with that name was found.");
		if(accessee.getNation() != user.getTown().getNation())
			throw new NationsException("Not in your nation", "You can only set the nation access for town in your nation.");
		if(accessString.equalsIgnoreCase("none"))
		{
			user.getTown().getAccessMan().resetAccess(accessee);
			user.getTown().notifyText(ChatColor.GREEN+accessee.getName()+ChatColor.YELLOW+" access has been set to none for your town.", null);
			accessee.notifyText("Your town now has no access to "+ChatColor.GREEN+user.getTown().getName()+ChatColor.YELLOW+".", null);
		}
		else if(accessString.equalsIgnoreCase("switch"))
		{
			user.getTown().getAccessMan().addSwitchAccess(accessee);
			user.getTown().notifyText(ChatColor.GREEN+accessee.getName()+ChatColor.YELLOW+" now has switch access to your town.", null);
			accessee.notifyText("Your town now has switch access to "+ChatColor.GREEN+user.getTown().getName()+ChatColor.YELLOW+".", null);
		}
		else if(accessString.equalsIgnoreCase("block"))
		{
			user.getTown().getAccessMan().addBlockAccess(accessee);
			user.getTown().notifyText(ChatColor.GREEN+accessee.getName()+ChatColor.YELLOW+" now has block access to your town.", null);
			accessee.notifyText("Your town now has block access to "+ChatColor.GREEN+user.getTown().getName()+ChatColor.YELLOW+".", null);
		}
		else if(accessString.equalsIgnoreCase("container"))
		{
			user.getTown().getAccessMan().addContainerAccess(accessee);
			user.getTown().notifyText(ChatColor.GREEN+accessee.getName()+ChatColor.YELLOW+" now has container access to your town.", null);
			accessee.notifyText("Your town now has container access to "+ChatColor.GREEN+user.getTown().getName()+ChatColor.YELLOW+".", null);
		}
		else throw new NationsException("No such access", "The access key can only be either: none, switch, block, container");
	}
	
	public void uninvite() throws NationsException
	{
		if(!user.leaderPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to remove nation invites.");
		String townName = NewNationsHelper.connectStrings(args, 1);
		Nation nat = user.getTown().getNation();
		Town uninvitee = plugin.getTown(townName);
		if(uninvitee == null) 
			throw new NationsException("Invalid Name", "The town of "+ChatColor.GREEN+townName+ChatColor.YELLOW+" doesn't exist.");
		
		for(NationTownInvite invite : plugin.getInviteManager().getTownInvitesForNation(nat))
		{
			if(invite.getReceivingTown() == uninvitee)
			{
				invite.invalidate();
				NewNationsHelper.notifyText(sender, ChatColor.GREEN+uninvitee.getName()+ChatColor.YELLOW+" uninvited.");
				return;
			}
		}
		throw new NationsException("Invalid Name", "Unable to find invite.");
	}
	
	public void kick() throws NationsException
	{
		if(!user.leaderPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to kick towns from the nation.");
		String name = NewNationsHelper.connectStrings(args, 1);
		Town town = plugin.getTown(name);
		if(town == null) 
			throw new NationsException("Unable to kick", "No town the name "+ChatColor.GRAY+name+ChatColor.YELLOW+" not found.");
		Nation nation = town.getNation();
		if(nation == null) 
			throw new NationsException("Unable to kick", "No nation with the name "+ChatColor.GRAY+name+ChatColor.YELLOW+" was found.");
		NewNationsHelper.notifyAll("The town of "+ChatColor.GRAY+town.getName()+ChatColor.YELLOW+" has been kicked from the nation of "+ChatColor.RED+nation.getName()+ChatColor.YELLOW+".");
		//needs more checks
		nation = new Nation(plugin, town);
		town.switchNation(nation);
		NewNationsHelper.notifyAll("The nation of "+ChatColor.GREEN+nation.getName()+ChatColor.YELLOW+" has been founded.");
	}
	
	public void leave() throws NationsException
	{
		if(!user.leaderPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to have the town leave the nation.");
		Town town = user.getTown();
		Nation nation = town.getNation();
		NewNationsHelper.notifyAll("The town of "+ChatColor.GRAY+town.getName()+ChatColor.YELLOW+" has left the nation of "+ChatColor.RED+nation.getName()+ChatColor.YELLOW+".");
		nation = new Nation(plugin, town);
		town.switchNation(nation);
		NewNationsHelper.notifyAll("The nation of "+ChatColor.GREEN+nation.getName()+ChatColor.YELLOW+" has been founded.");
	}
	
	public void capital() throws NationsException
	{
		if(!user.leaderPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to change the nation's capital status.");
		String name = NewNationsHelper.connectStrings(args, 1);
		Town newTown = plugin.getTown(name);
		Nation nation = user.getTown().getNation();
		if(newTown == null) 
			throw new NationsException("Town not found.", "A town with that name couldn't be located.");
		if(newTown.getNation() != nation) 
			throw new NationsException("Town not in your nation.", null);
		if(nation.getCapital() != user.getTown()) 
			throw new NationsException("Unable to transfer capital status.", "Your town is not the capital. You cannot transfer capital status if you aren't a capital.");
		nation.setCapital(newTown);
		nation.notifyText("The new capital of "+ChatColor.GREEN+nation.getName()+ChatColor.YELLOW+" is "+ChatColor.GREEN+newTown.getName()+ChatColor.YELLOW+".", null);
	}
	
	private void recode(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.leaderPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to recode the nation.");
		if(args[1].length() != 3)
			throw new NationsException("Invalid Code", "Codes must be 3 letters long.");
		if(plugin.getTown(args[1]) != null || plugin.getNation(args[1]) != null || plugin.getAlliance(args[1]) != null )
			throw new NationsException("Invalid Code", "That code is already in use.");
		//TODO should numbers be allowed?
		Nation n = user.getTown().getNation();
		n.setCode(args[1].toUpperCase());
		n.notifyText("Your nation's code is now "+ChatColor.LIGHT_PURPLE+n.getCode()+ChatColor.YELLOW+".", null);
	}

	public void showInfo(Town town, CommandSender sender, NewNations plugin)
	{
		Nation nation = town.getNation();
		sender.sendMessage(ChatColor.RED + "** [ "+nation.getDisplayName()+" "+ChatColor.RED+" ] **");
		Town cap = nation.getCapital();
		sender.sendMessage(ChatColor.GREEN+ "Member Towns:");
		for(Town t : nation.getTowns())
			sender.sendMessage(ChatColor.RED+" "+t.getDisplayName()+ChatColor.RED+((t == cap) ? ChatColor.GOLD+"  [Capital] " : "  "));
	}
	
	//list the invites that this nation receved from alliances
	public void listInvites()
	{
		NewNationsHelper.notifyText(sender, "Incoming Alliance Invites: ");
		Town tow = user.getTown();
		boolean flag = false;
		for(AllianceNationInvite invite : plugin.getInviteManager().getAllianceInvitesForNation(tow.getNation()))
		{
			sender.sendMessage(ChatColor.RED + "Alliance: "+ invite.getSendingAllaince().getDisplayName() );
			flag = true;
		}
	
		if(!flag) sender.sendMessage(ChatColor.RED+"Your town has not been invited to any alliances.");
	}
	public void listInvitesSent()
	{
		NewNationsHelper.notifyText(sender, "Outgoing invites: ");
		Nation thisNation = user.getTown().getNation();
		boolean flag = false;
		
		for(NationTownInvite invite : plugin.getInviteManager().getTownInvitesForNation(thisNation))
		{
			sender.sendMessage(ChatColor.RED+"Town: "+invite.getReceivingTown().getDisplayName());
		}
		if(!flag) sender.sendMessage(ChatColor.RED + "Your nation has not invited any Tonws to join your nation.");
	}
	public void rename() throws NationsException
	{
		Nation n = user.getTown().getNation();
		String newName = NewNationsHelper.connectStrings(args, 1);
		n.rename(newName);
		n.notifyText("Your nation has been renamed to "+ChatColor.GREEN+newName+ChatColor.YELLOW+".", null);
	}
	
	public void listNations()
	{
		sender.sendMessage(ChatColor.GOLD+"Nation List:");
		for(Nation n : plugin.nations)
		{
			sender.sendMessage(ChatColor.GREEN+"Nations:" + n.getDisplayName());
			for(Town t : n.getTowns())
				sender.sendMessage(ChatColor.GREEN+"  Town:" + t.getDisplayName() + "  **");
		}
	}
	
	public void listNationAccess(User user)
	{
		Town town = user.getTown();
		AccessManager accessMan = town.getAccessMan();
		sender.sendMessage(ChatColor.GREEN+"Nation Access List:");
		for(Town t : accessMan.getTownList())
		{
			if(t == null) {sender.sendMessage("Null detected. Contact plugin developer."); continue;}
			sender.sendMessage(ChatColor.GREEN+t.getName()+ChatColor.YELLOW+" - "+ChatColor.GOLD+
					(accessMan.switchAccess(t) ? "SWITCH:" : "")+(accessMan.blockAccess(t) ? "BLOCK:" : "")+(accessMan.containerAccess(t) ? "CONTAINER" : ""));
		}
	}

}
