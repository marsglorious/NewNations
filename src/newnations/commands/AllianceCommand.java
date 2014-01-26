package newnations.commands;

import java.util.ArrayList;

import newnations.Alliance;
import newnations.Nation;
import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Town;
import newnations.User;
import newnations.invites.AllianceNationInvite;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;


public class AllianceCommand 
{
	
	private NewNations plugin;
	private String[] args;
	private CommandSender sender;
	private User user;
	
	public AllianceCommand(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException 
	{
		this.plugin = plugin;
		this.args = args;
		this.sender = sender;
		this.user = user;
		//alliance
		if(args.length == 0) 
		{	
			Town town = user.getTown();
			if(town == null)
				throw new NationsException("You are a wildling, you have no town or nation.", "You can join an existing town or found your own.");
			NewNationsHelper.notifyText(sender, "Type /alliance help for more information.");
			listAlliances();
			listInvitesReceived();
			listInvitesSent();
			return;
		}
		
		if(args[0].equalsIgnoreCase("help")) //alliance help
		{
			NewNationsHelper.helpText(sender, "i.e. /alliance [create|rename|recode|invite|uninvite|accept|leave]", 
					"Alliance manipulation commands. '/alliance [subcommand]' for more help.");
		}
		else if(args[0].equalsIgnoreCase("create"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //alliance invite || alliance invite help
			{
				NewNationsHelper.helpText(sender, "i.e. /alliance create <alliancename>", "Invite a nation to the alliance.");
				//listInvites();
			}
			else create();
		}
		else if(args[0].equalsIgnoreCase("rename"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //alliance rename || alliance rename help
			{
				NewNationsHelper.helpText(sender, "i.e. /alliance rename <alliancename> <newname>", "Rename the alliance.");
				listAlliances();
			}
			else rename();
		}
		else if(args[0].equalsIgnoreCase("invite"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //alliance invite || alliance invite help
			{
				NewNationsHelper.helpText(sender, "i.e. /alliance invite <alliancecode> <nationname/nationcode>", "Invite a nation to the alliance. type /alliance to list your alliances.");
				listInvitesSent();
			}
			else invite();
		}
		else if(args[0].equalsIgnoreCase("accept"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //alliance invite || alliance invite help
			{
				NewNationsHelper.helpText(sender, "i.e. /alliance accept <alliancename/alliancecode> ", "Accept an invite to an alliance.");
				listInvitesReceived();
			}
			else accept();
		}
		else if(args[0].equalsIgnoreCase("uninvite"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //alliance uninvite || alliance uninvite help
			{
				NewNationsHelper.helpText(sender, "i.e. /alliance uninvite <alliancecode> <nationname/nationcode>", "Uninvite a nation invited to the alliance.");
				listInvitesSent();
			}
			else uninvite();
		}
		else if( args[0].equalsIgnoreCase("leave"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //alliance leave || alliance leave help
			{	
				NewNationsHelper.helpText(sender, "i.e. /alliance leave <alliancename/alliancecode>", "Leave an alliance.");
				listAlliances();
			}
			else leave();
		}
		else if(args[0].equalsIgnoreCase("recode"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //town rename || /town rename help
				NewNationsHelper.helpText(sender, "i.e. /alliance recode <newalliancecode>", "Recode your alliance code.");
			else recode(sender, args, plugin, user);
		}
		else NewNationsHelper.helpText(sender, "Unknown command", " Type /alliance help for more information.");
	}
	
	public void create() throws NationsException
	{
		// Throws exception if they don't have the authority
		checkIsLeaderOfNation(user);
				
		String allianceName = NewNationsHelper.connectStrings(args, 1);
		Nation nat = user.getTown().getNation();
		if(plugin.getAlliance(allianceName) != null)
			throw new NationsException("Alliance already exists", ChatColor.GRAY+allianceName+ChatColor.YELLOW+" is already taken.");
		Alliance a = new Alliance(plugin, allianceName);
		a.addNation(nat);
		NewNationsHelper.notifyText(sender, ChatColor.GREEN+a.getDisplayName()+ChatColor.YELLOW+" successfully created.");
	}
	
	private Alliance getAllianceandCheck(String name, User user) throws NationsException
	{
		ArrayList<Alliance> alliList = user.getTown().getNation().getAlliances();
		Alliance thisAlliance = plugin.getAlliance(args[1]);
		if(alliList.size() < 1) 
			throw new NationsException("Not in alliance", "Your nation is not in any alliances. See \"/alliance create help\".");
		if(thisAlliance == null)
			throw new NationsException("Alliance not found", "The alliance "+ChatColor.GRAY+args[1]+ChatColor.YELLOW+" could not be found, try using its code.");
		if(!alliList.contains(thisAlliance))
			throw new NationsException("Not in this alliance", "Your nation is not in alliance "+ChatColor.GRAY+thisAlliance.toString()+ChatColor.YELLOW+".");
		return thisAlliance;
	}
	
	public void rename() throws NationsException
	{
		checkIsLeaderOfNation(user); //throws exception if they don't have the authority	
		Alliance thisAlliance = getAllianceandCheck(args[1], user);
		thisAlliance.rename(NewNationsHelper.connectStrings(args, 2));
	}
	
	public void invite() throws NationsException
	{
		// Throws exception if they don't have the authority
		checkIsLeaderOfNation(user);
				
		Alliance thisAlliance = getAllianceandCheck(args[1], user);		
		String nationName = NewNationsHelper.connectStrings(args, 2);
		Nation invitee = plugin.getNation(nationName);
		if(invitee == user.getTown().getNation())
			throw new NationsException("Invalid Name", "You cannot invite youself to an alliance.");
		if(invitee == null)
		{
			Town town1 = plugin.getTown(nationName);
			if(town1 == null) //TODO colour codes
				throw new NationsException("Invalid Nation Name", "Nation with that name "+ChatColor.GREEN+nationName+ChatColor.YELLOW+" doesn't exist.");
			else
				throw new NationsException("Invalid Nation Name", "You have entered a town name, the town " + town1.getDisplayName() +ChatColor.YELLOW+" is a member of the nation: " + town1.getNation().getDisplayName());
		}
		if(user.getTown().getNation().isFightingWith(invitee))
			throw new NationsException("Error", "That nation is currently at war with your nation.");
		plugin.getInviteManager().inviteNationToAlliance(thisAlliance, invitee);
		thisAlliance.notifyText(ChatColor.GREEN+invitee.toString()+ChatColor.YELLOW+" has been invited to the alliance "+ChatColor.GREEN+thisAlliance.getDisplayName()+ChatColor.YELLOW+".", null);
		invitee.notifyText("Your Nation has been invited to join the Alliance: "+thisAlliance.getDisplayName(), null);
		
		//else throw new NationsException("Unable to invite", ChatColor.GREEN+nationName+ChatColor.YELLOW+" is already in or invited to the alliance "+ChatColor.GREEN+thisAlliance.getName()+ChatColor.YELLOW+".");

	}
	
	private void checkIsLeaderOfNation(User user) throws NationsException
	{
		// are they the leader of the town
		if(!user.leaderPriv()) 
			throw new NationsException("Insufficient privilege", "You lack the sufficent rank to join your nation to the alliance.");
		
		//is that town also the capital of the nation
		if(user.getTown().getNation().getCapital() != user.getTown())
			throw new NationsException("Insufficient privilege", "Only the capital of the nation may change alliance status.");
	}
	
	public void accept() throws NationsException
	{
		// Throws exception if they don't have the authority
		checkIsLeaderOfNation(user);
		
		String allianceName = NewNationsHelper.connectStrings(args, 1);
		Nation nation = user.getTown().getNation();
		Alliance alliance = plugin.getAlliance(allianceName);
		if(alliance == null) 
			throw new NationsException("Invalid Name", "The alliance "+ChatColor.GREEN+allianceName+ChatColor.YELLOW+" doesn't exist.");
		
		for(AllianceNationInvite invite : plugin.getInviteManager().getAllianceInvitesForNation(nation))
		{
			if(invite.getSendingAllaince() == alliance )
			{
				invite.invalidate();
				alliance.addNation(nation);
				NewNationsHelper.notifyText(sender, "Your nation of "+ChatColor.GREEN+nation.getName()+ChatColor.YELLOW+" is now part of "+ChatColor.GREEN+alliance.getName()+ChatColor.YELLOW+".");
				alliance.notifyText("The nation "+ nation.getDisplayName()+ChatColor.YELLOW+" has joined the alliance of " + alliance.getDisplayName()+ChatColor.YELLOW+".", null);
				return;
			}
			
		}
		
		NewNationsHelper.errorText(sender, "Invalid Name", "Your town was not invited to alliance of "+ChatColor.GREEN+allianceName+ChatColor.YELLOW+".");
	}
	
	public void uninvite() throws NationsException
	{
		// Throws exception if they don't have the authority
		checkIsLeaderOfNation(user);
		
		Alliance thisAlliance = getAllianceandCheck(args[1], user);	
		String nationName = NewNationsHelper.connectStrings(args, 2);
		Nation nat = plugin.getNation(nationName);
		if(nat == null) 
			throw new NationsException("Invalid Name", "Unable to find nation with name '"+nationName+"'"); 
			
		for(AllianceNationInvite invite : plugin.getInviteManager().getNationInvitesForAlliance(thisAlliance))
		{
			if(invite.getReceivingNation() == nat)
			{
				invite.invalidate();
				NewNationsHelper.notifyText(sender, ChatColor.RED+nat.getDisplayName()+ChatColor.YELLOW+" has been uninvited from the alliance "+  thisAlliance.getDisplayName() ); 
				return;
			}
		}
		
		throw new NationsException("Invalid Invitation", "Nation: "+nat.getDisplayName()+" is not invited to "+ thisAlliance.getDisplayName());
		
	}
	
	public void leave() throws NationsException
	{
		// Throws exception if they don't have the authority
		checkIsLeaderOfNation(user);
		
		Alliance thisAlliance = getAllianceandCheck(args[1], user);	
		Nation nat = user.getTown().getNation();
		
		thisAlliance.notifyText("Nation "+nat.getName() + " has left the Alliance: " +thisAlliance.getDisplayName(), null);
		thisAlliance.removeNation(nat);
	}
	
	private void recode(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		// Throws exception if they don't have the authority
		checkIsLeaderOfNation(user);
		
		String proposedCode = args[1];
		
		if(proposedCode.length() != 3)
			throw new NationsException("Invalid Code", "Codes must be 3 letters long.");
		if(plugin.isNameUnique(proposedCode) == false )
			throw new NationsException("Invalid Code", "That code is alredy in use");
		
		//TODO should numbers be allowed?
		user.getTown().getNation().setCode(proposedCode.toUpperCase());
		sender.sendMessage("You nation's code is now: "+ChatColor.LIGHT_PURPLE+user.getTown().getNation().getCode());
	}
	
	
	// list the alliances my nation is a member of
	public void listAlliances()
	{
		Nation nat = user.getTown().getNation();
		
		ArrayList<Alliance> alliList = nat.getAlliances();

		sender.sendMessage(ChatColor.GOLD + "Alliances:");
		boolean flag = false;
		for(Alliance a : alliList)
		{
			sender.sendMessage(ChatColor.GREEN+" "+a.getDisplayName());
			for(Nation n : a.getNations())
				sender.sendMessage("   - "+n.getDisplayName() );
			flag = true;
		}
		if(!flag) sender.sendMessage(ChatColor.GRAY+" None");
	}
	
	// list invites my nation has received from other alliances
	public void listInvitesReceived()
	{
		Nation nat = user.getTown().getNation();
		sender.sendMessage(ChatColor.GOLD+"Incoming Alliance Invites:");
		boolean flag = false;
		
		for(AllianceNationInvite invite : plugin.getInviteManager().getAllianceInvitesForNation(nat))
		{
			sender.sendMessage(ChatColor.GREEN+invite.getSendingAllaince().getDisplayName());
			flag = true;
		}
		if(!flag) sender.sendMessage(ChatColor.GRAY+" None");
	}
	
	//list the nations we have invted to any of our alliances
	public void listInvitesSent()
	{
		Nation nat = user.getTown().getNation();
		sender.sendMessage(ChatColor.GOLD + "Outgoing Alliance Invites:");
		boolean flag = false;
		
		// get the alliances we are in
		ArrayList<Alliance> ourAlliances = nat.getAlliances();
		for(Alliance a : ourAlliances)
		{			
			for(AllianceNationInvite invite : plugin.getInviteManager().getNationInvitesForAlliance(a))
			{
				sender.sendMessage("   "+a.getDisplayName()+" => "+invite.getReceivingNation().getDisplayName());
				flag = true;
			}	
		}
		if(!flag) sender.sendMessage(""+ChatColor.GREEN+" [None]");
	}	
}