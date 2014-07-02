package newnations.commands;


import java.util.ArrayList;
import newnations.Econ;
import newnations.Nation;
import newnations.NationsException;
import newnations.NewNations;
import newnations.NewNationsHelper;
import newnations.Plot;
import newnations.PlotLocation;
import newnations.Siege;
import newnations.Town;
import newnations.User;
import newnations.invites.NationTownInvite;
import newnations.invites.TownUserInvite;
import newnations.listeners.NationsUserListener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TownCommand
{	
	public TownCommand(CommandSender sender, String[] args, NewNations plugin, User user)  throws NationsException
	{	
		//---------CommandScan----------//
		//town
		if(args.length == 0) 
		{
			if(user.getTown() == null)
				throw new NationsException( "You are a wildling, you have no town.", "You can join an existing town or found your own. See '/town help'");
			else showInfo(sender, args, plugin, user);
		}
		//town help
		else if(args[0].equalsIgnoreCase("help")) 
		{
			NewNationsHelper.helpText(sender, "i.e. /town [found|invite|uninvite|accept|spawn|rank|deposit|withdraw|destruction|restore|rename|kick|leave|disband]", 
									"Town manipulation commands. '/town [subcommand]' for more help.");
		}
		//town found <townName>
		else if(args[0].equalsIgnoreCase("found")) 
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //town found || town found help
				NewNationsHelper.helpText(sender, "i.e. /town found <town name>", "Found a town and automatically a nation.");
			else found(sender, args, plugin, user);
		}
		//town accept <townname>
		else if(args[0].equalsIgnoreCase("accept"))
		{
			if(args.length == 1) 
			{
				listUserInvites(sender, args, plugin, user); //town accept
				NewNationsHelper.notifyText(sender, "You must type the name of the town you want to join, ie. '/town accept newTown'.");
			}
			else if(args[1].equalsIgnoreCase("help")) //town accept help
				NewNationsHelper.helpText(sender, "i.e. /town accept <username>", "Accepts an invite to a town.");
			else accept(sender, args, plugin, user);
		}
		//Stop from this point if the user is a wildling.
		else if(user.getTown()  == null) throw new NationsException("You are not in a town.", "You need to be in a town to use any town commands other than help/found/accept.");
		
		//town invite <username>
		else if(args[0].equalsIgnoreCase("invite"))
		{
			if(args.length == 1) listTownInvites(sender, args, plugin, user); //town invite
			else if(args[1].equalsIgnoreCase("help")) //town invite help (stuffed if your username is "help")
				NewNationsHelper.helpText(sender, "i.e. /town invite <username>", "Invite a user to your town.");
			else invite(sender, args, plugin, user); //town invite <username>
		}
		//town uninvite <username>
		else if(args[0].equalsIgnoreCase("uninvite"))
		{
			if(args.length == 1) listTownInvites(sender, args, plugin, user); //town uninvite
			else if(args[1].equalsIgnoreCase("help")) //town uninvite help
				NewNationsHelper.helpText(sender, "i.e. /town uninvite <username>", "Remove an invite for a user from your town invites.");
			else uninvite(sender, args, plugin, user);
		}
		else if(args[0].equalsIgnoreCase("claim")) new Claim(sender, args, plugin, user); //Claim link
		else if(args[0].equalsIgnoreCase("raze")) new Raze(sender, args, plugin, user); //Raze link
		//town spawn
		else if(args[0].equalsIgnoreCase("spawn"))
		{
			if(args[1].equalsIgnoreCase("help")) //town spawn help
				NewNationsHelper.helpText(sender, "i.e. /town spawn", "Set your spawn within the town.");
			else spawn(sender, args, plugin, user);
		}
		else if(args[0].equalsIgnoreCase("rank")) //town rank help
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help"))
				NewNationsHelper.helpText(sender, "i.e. /town rank <username> <rank>", "Set the rank of a resident in your town.");
			else rank(sender, args, plugin, user);
		}
		else if(args[0].equalsIgnoreCase("deposit"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //town deposit help
				NewNationsHelper.helpText(sender, "i.e. /town deposit <amount>", "Transfer some of your money to the town coffers.");
			else deposit(sender, args, plugin, user);
		}
		else if(args[0].equalsIgnoreCase("withdraw"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //town withdraw help
				NewNationsHelper.helpText(sender, "i.e. /town withdraw <amount>", "Withdraw money from the town coffers.");
			else withdraw(sender, args, plugin, user);
		}
		else if(args[0].equalsIgnoreCase("destruction")) //town destruction
		{
			if(args.length > 1 && args[1].equalsIgnoreCase("help"))
				NewNationsHelper.helpText(sender, "i.e. /town destruction", "Toggle explosion and fire terrain damage.");
			else destruction(sender, args, plugin, user);
		}
		//town rename <townname>
		else if(args[0].equalsIgnoreCase("rename"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //town rename || /town rename help
				NewNationsHelper.helpText(sender, "i.e. /town rename <new town name>", "Rename your town.");
			else rename(sender, args, plugin, user);
		}
		else if(args[0].equalsIgnoreCase("recode"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //town rename || /town rename help
				NewNationsHelper.helpText(sender, "i.e. /town code <new town code>", "Rename your town code.");
			else recode(sender, args, plugin, user);
		}
		//town restore
		else if(args[0].equalsIgnoreCase("restore"))
		{
			if(args.length >= 2 && args[1].equalsIgnoreCase("help"))
				NewNationsHelper.helpText(sender, "i.e. /town restore", "Restore a town to its former glory.");
			else restore(sender, args, plugin, user);
		}
		//town kick <username>
		else if(args[0].equalsIgnoreCase("kick"))
		{
			if(args.length == 1 || args[1].equalsIgnoreCase("help")) //town kick || /town kick help
				NewNationsHelper.helpText(sender, "i.e. /town kick <username>", "Kick a user from the town.");
			else kick(sender, args, plugin, user);
		}
		//town leave
		else if(args[0].equalsIgnoreCase("leave"))
		{
			if(args.length > 1 && args[1].equalsIgnoreCase("help")) //town leave help
				NewNationsHelper.helpText(sender, "i.e. /town leave", "Leave the town you are currently in.");
			else leave(sender, args, plugin, user);
		}
		else if(args[0].equalsIgnoreCase("relocate"))
		{
			if(args.length > 1 && args[1].equalsIgnoreCase("help")) //town leave help
				NewNationsHelper.helpText(sender, "i.e. /town relocate", "move the location of the town.");
			else relocate(sender, args, plugin, user);
		}
		//town disband
		else if(args[0].equalsIgnoreCase("disband"))
		{
			if(args.length > 1 && args[1].equalsIgnoreCase("help")) //town disband help
			{
				NewNationsHelper.helpText(sender, "i.e. /town disband", 
						"Disband your current town and transfers coffers and plot refunds to you.\n" + 
						"If this is the last town in the nation, it will also be disbanded.");
			}
			else disband(sender, args, plugin, user);
		}
		else if(args[0].equalsIgnoreCase("silent"))
		{
			if(sender instanceof Player)
			{
				Player player = (Player) sender;
				if(plugin.silencePlotMessage.contains(sender)) 
				{					
					player.sendMessage("Plot Messages have been Enabled");
					NationsUserListener.showPlotDesc(player, plugin.locationCache.get(player));
					plugin.silencePlotMessage.remove(player) ;
				}
				else
				{
					player.sendMessage("Plot Messages have been Disabled");
					plugin.silencePlotMessage.add(player) ;
				}
			}
		}
		else if(args[0].equalsIgnoreCase("test1"))
		{
			plugin.getInviteManager().inviteUserToTown(user.getTown(), "pooface");
		}
		
		
		//town value
		else if(args[0].equalsIgnoreCase("value")) getWorth(sender, args, plugin, user); //town value
		else throw new NationsException("No such subcommand exists.", "Type /town help for more information.");
	}

	//-----Command Methods------//
	private void found(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		Town town = user.getTown();
		if(town != null) 
			throw new NationsException("You are already in a town.", "'/town leave' to leave your current town.");
		
		Player player = (Player) sender; //TODO: needs checks
		
		String townName = NewNationsHelper.connectStrings(args, 1);
		if(townName.length() > 30) throw new NationsException("Town name too long.", "Town names cannot be longer than 30.");
		//TODO: check town name characters (separate function)
		
		//TODO: check nearby plots
		Plot firstPlot = plugin.getPlot(player.getLocation());
		if(firstPlot != null) 
			throw new NationsException("You cannot found here.", "This area is already claimed.");
		
		PlotLocation plotLoc= new PlotLocation(player.getLocation());
		int cost = plugin.getPlotFinalCost(plotLoc, null);
		
		Econ.withdraw(player, cost +Econ.TOWN_SETUP_FEE); 
		
		Town newTown = new Town(plugin, townName);
		newTown.setWorldName(player.getLocation().getWorld().getName());
		newTown.setCode(plugin.createCode(townName));
		Plot plot = newTown.createPlot(new PlotLocation(player.getLocation() ));
		
		Nation nation = new Nation(plugin, newTown); //adds to database

		plot.placeTorches();
		newTown.addUser(user);
		newTown.setNation(nation);
		
		
		NewNationsHelper.notifyAll("The town of "+ChatColor.GREEN+newTown.getName()+ChatColor.DARK_AQUA+" founded in the new nation of "+ChatColor.GREEN+nation.getName()+ChatColor.DARK_AQUA+".");
	}
	
	//This function allows a town officer to invite a player
	public void invite(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{	
		if(!user.invitePriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to invite people to the town.");
		Town town = user.getTown();
		if(town == null) throw new NationsException("You are not in a town.", "You cannot invite people to a non-existent town.");
		else
		{
			String recipientName = args[1];
			if(!NewNationsHelper.isValidUsername(recipientName)) throw new NationsException("Invalid Name", "That username is not valid."); 
			
			//Don't check if they are in a town already or if they even exist
			//Deal with it when they accept the invites, expiry is currently 2 IRL days
			//The invite  is issued by the town, not the user who ran the command
			User recipient = plugin.getUser(recipientName);
			if(recipient != null && recipient.getTown() == town) NewNationsHelper.notifyText(sender, "That user is already in this town.");
			
			//town.addInvite(recipientName);			
			plugin.getInviteManager().inviteUserToTown(town, recipientName);
			
			Player recp = Bukkit.getPlayerExact(recipientName);
			if(recp != null)
				NewNationsHelper.notifyText(recp, "You have been invited to "+ChatColor.GREEN+town.getName()+ChatColor.YELLOW+". Type \"/town accept "+town.getCode()+"\" to accept.");
			plugin.onEdit();
			town.notifyText(ChatColor.GREEN+recipientName+ChatColor.YELLOW+" invited to this town.",  null);
		}
	}
	
	private void uninvite(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.invitePriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to uninvite.");
		Town town = user.getTown();
		
		if(plugin.getInviteManager().removeUserInviteFromTown(town, args[1] ))
		{
			NewNationsHelper.notifyText(sender, ChatColor.RED+args[1]+ChatColor.YELLOW+" has been uninvited.");
			plugin.onEdit();
		}
		else
			throw new NationsException("User with that name is not invited to your town.", null);	
	}
	
	private void accept(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		Town town = user.getTown();
		if(town != null) 
			throw new NationsException("You are already in a town.", "Type /town leave to leave your current town.");
		Town acceptTown = plugin.getTown(NewNationsHelper.connectStrings(args, 1));
		if(acceptTown == null) 
			throw new NationsException("Town with that name couldn't be found.", null);
		
		for(TownUserInvite inv : plugin.getInviteManager().getTownInvitesForUser(user.getName()))
		{				
			if(inv.getSendingTown() == acceptTown) 
			{
				inv.invalidate();
				acceptTown.addUser(user);
				//inform the town.
				acceptTown.notifyText(ChatColor.GREEN+user.getName()+ChatColor.YELLOW+" has joined this town.", user);
				//inform the user.
				NewNationsHelper.notifyText(sender, "You have joined the town of "+ChatColor.GREEN+acceptTown.getName()+ChatColor.YELLOW+".");
				plugin.onEdit();
				return;
			}
		}
		throw new NationsException("You aren't invited to that town.", "You need to get a privileged user in that town to invite you.");	
	}
	
	private void spawn(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		Town town = user.getTown();
		Player player = (Player) sender; //TODO needs checks
		if(town != plugin.getPlot(player.getLocation()).getTown())
			throw new NationsException("You cannot set your spawn here.", "You can only set your spawn in your own town.");
		user.setSpawn(player.getLocation());
		NewNationsHelper.notifyText(sender, "You have set your spawn to your current location.");
		plugin.onEdit();
		//TODO: potentially notify everyone in the town of this.
	}
	
	private void rank(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.rankPriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to set other resident's rank.");
		User rankee = plugin.getUser(args[1]);
		if(rankee == null) throw new NationsException("Invalid Username", "User with that name was not found.");
		if(rankee.getTown() != user.getTown()) throw new NationsException("User not in your town", "You cannot set the rank for users outside your town.");
		
		if(!user.leaderPriv() && rankee.leaderPriv()) 
			throw new NationsException("Insufficent privilege", "You must be a leader to demode leaders");
		
		if(!user.leaderPriv() && args[2].equalsIgnoreCase("leader"))
			throw new NationsException("Insufficent privilege", "You can't promote someone to leader if you aren't a leader yourself.");
		Integer rank = User.rankType.get(args[2].toLowerCase());
		if(rank == null) throw new NationsException("Rank with that name not found.", null);
		rankee.setRank(rank);
		NewNationsHelper.notifyText(sender, ChatColor.GREEN+rankee.getName()+ChatColor.YELLOW+"'s rank successfully changed to "+ChatColor.GREEN+args[2]+ChatColor.YELLOW+".");
		rankee.notifyUser("Your rank has been changed to "+ChatColor.GREEN+args[2]+ChatColor.YELLOW+".");
		plugin.onEdit();
	}
	
	private void deposit(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		user.getTown().deposit((Player)sender, Double.parseDouble(args[1]));
		plugin.onEdit();
	}
	
	private void withdraw(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.moneyPriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to withdraw from the town coffers.");
		//TODO: add cash priv
		user.getTown().withdraw((Player)sender, Double.parseDouble(args[1]));
		plugin.onEdit();
	}
	
	private void destruction(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.destructionPriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to toggle destruction.");
		Town town = user.getTown();
		town.setDestruction(!town.isDestructionOn()); //Toggle destruction
		town.notifyText("Destruction is now "+(town.isDestructionOn() ? "enabled." : "disabled."), null);
	}
	
	private void rename(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.leaderPriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to rename a town.");
		Town town = user.getTown();
		String townName = NewNationsHelper.connectStrings(args, 1);
		town.setName(townName);
		town.notifyText("The town have been renamed to "+ChatColor.GREEN+town.getName()+ChatColor.YELLOW+".", null);
		plugin.onEdit();
	}
	
	private void recode(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.leaderPriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to change the town code.");
		
		//TODO should numbers be allowed?
		user.getTown().setCode(args[1].toUpperCase());
		user.getTown().notifyText("Your town's code is now "+ChatColor.DARK_BLUE+user.getTown().getCode()+ChatColor.YELLOW+".", null);
		plugin.onEdit();
	}
	
	private void restore(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.siegePriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to restore a town.");
		Town town = user.getTown();
		if(!town.hasRestore()) throw new NationsException("Your town doesn't have a restore.", null);
		try 
		{
			//Restore plots
			for(Plot p : town.getPlots()) p.loadChunk(plugin.getServer().getWorld(town.getWorldName()).getChunkAt((int)p.getX(), (int)p.getZ()));
		}	
		catch (Exception e) {e.printStackTrace();}
		town.notifyText("Your town has been restored.", null);
		town.setHasRestore(false);
	}
	
	private void kick(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.invitePriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to kick residents from your town.");
		Town town = user.getTown();
		User kickee = plugin.getUser(args[1]); //My best variable name.
		if(kickee == null) throw new NationsException("Username not found.", "User with that name is not registered with this plugin. Perhaps permissions.");
		if(kickee == user) throw new NationsException("You can't kick yourself. Fool!", "Use /town leave");
		
		town.kick(kickee);
		town.notifyText(ChatColor.RED+kickee.getName()+ChatColor.YELLOW+" has been kicked from this town.",  null);
		kickee.notifyUser("You have been kicked from "+ChatColor.RED+town.getName()+ChatColor.YELLOW+".");
		
		//Update sieges
		ArrayList<Siege> sieges = user.getSieges();
		for(int i = 0; i < sieges.size(); i++)
		{
			sieges.get(i).removeUser(user);
			sieges.get(i).defenderBroadcast(ChatColor.RED+user.getName()+ChatColor.YELLOW+" has been removed from the siege.");	
		}
		plugin.onEdit();
	}
	private void leave(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		Town town = user.getTown();
		town.leave(user);
		NewNationsHelper.notifyText(sender, "You have left the town of "+ChatColor.GREEN+town.getName()+ChatColor.YELLOW+".");
		//Inform the town, excluding the player.
		town.notifyText(ChatColor.DARK_AQUA+user.getName()+ChatColor.YELLOW+" has left the town.", user);
		
		//Update Sieges
		for(int i = 0; i < user.getSieges().size(); i++)
		{
			Siege s = user.getSieges().get(i);
			s.removeUser(user);
			s.defenderBroadcast(ChatColor.RED+user.getName()+ChatColor.YELLOW+" has been removed from the siege.");
		}
		plugin.onEdit();
	}
	
	private void disband(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.leaderPriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to disband this town.");
		//TODO: potentially add an "accept" subcommand for safety.
		Town town = user.getTown();
		Player p = (Player) sender;
		
		//Update sieges.
		for(Siege s : town.getSieges()) 
		{
			s.removeTown(town);
			s.broadcast(ChatColor.RED+town.getName()+ChatColor.YELLOW+" has been removed from the siege.");
		}
		town.destroy();
		town.withdraw(p, town.getCoffers());
		plugin.onEdit();
	}
	
	public void relocate(CommandSender sender, String[] args, NewNations plugin, User user) throws NationsException
	{
		if(!user.leaderPriv()) throw new NationsException("Insufficient privilege", "You lack the sufficent rank to relocate the town.");
		Town town = user.getTown();
		Player p = (Player) sender;
		town.relocateTown(p.getLocation());
		town.notifyText("Your town has successfully relocated.", null);
	}
	
	
	//------------Extra Methods-------------//
	public void listUserInvites(CommandSender sender, String[] args, NewNations plugin, User user) //List incoming invites that the user has received.
	{
		boolean flag = false;
		sender.sendMessage(ChatColor.GOLD+"Town Invites:");
		
		for( TownUserInvite invite : plugin.getInviteManager().getTownInvitesForUser(user.getName()))
		{
			sender.sendMessage(ChatColor.BLUE+invite.getSendingTown().getDisplayName());
			flag = true;
		}
		
		if(!flag) sender.sendMessage(ChatColor.RED + "You have not been invited to any rowns.");
		sender.sendMessage(ChatColor.YELLOW + "Type /town [subcommand] help for more information.");
	}
	
	public void listTownInvites(CommandSender sender, String[] args, NewNations plugin, User user) //Lists outgoing invites that the town has issued.
	{
		Town town = user.getTown();
		sender.sendMessage(ChatColor.GOLD + "[User Invites List]");
		for( TownUserInvite invite : plugin.getInviteManager().getUserInvitesForTown(town))
		{
			sender.sendMessage(ChatColor.BLUE + invite.getReceivingUserName());
		}

		sender.sendMessage(ChatColor.YELLOW + "Type /town [subcommand] help for more information.");
	}
	
	public void listNationInvites(CommandSender sender, String[] args, NewNations plugin, User user) //Lists incoming invites from another nation the town has received.
	{
		Town town = user.getTown();
		
		sender.sendMessage(ChatColor.GOLD + "[ Nation Invites ]");
		
		for(NationTownInvite invite : plugin.getInviteManager().getNationInvitesForTown(user.getTown()))
		{
			sender.sendMessage(ChatColor.GREEN + invite.getSendingNation().getDisplayName());
		}
		sender.sendMessage(ChatColor.YELLOW+"Type /town [subcommand] help for more information.");
	}
	
	public void getWorth(CommandSender sender, String[] args, NewNations plugin, User user)
	{
		int n = user.getTown().getTownNetWorth();
		NewNationsHelper.notifyText(sender, "Town Net Value: "+n);
		int coff = user.getTown().getCoffers();
		NewNationsHelper.notifyText(sender, "Town Coffers: "+ coff);
		int stacks = (coff + n) / Econ.EMERALD_VALUE * 64;
		int emeralds = ((coff + n) / Econ.EMERALD_VALUE) % 64;
		NewNationsHelper.notifyText(sender, "Total: "+ChatColor.BLUE+(coff + n)+ChatColor.YELLOW+" Emerald Stacks: "+ChatColor.BLUE+stacks+" + " + emeralds);
	}

	public void showInfo(CommandSender sender, String[] args, NewNations plugin, User user) 
	{
		Town town = user.getTown();
		boolean flag = false;
		sender.sendMessage(ChatColor.RED+"** [ "+town.getDisplayName()+ChatColor.RED+" ] **");
		sender.sendMessage(ChatColor.GREEN+"Nation: "+town.getNation().getDisplayName());
		sender.sendMessage(ChatColor.GREEN+"Plots: "+ChatColor.GOLD+town.getPlots().size());
		sender.sendMessage(ChatColor.GREEN+"Coffers: "+ChatColor.BLUE+"$"+town.getCoffers());
		sender.sendMessage(ChatColor.GREEN+"Destruction: "+ChatColor.GOLD+(town.isDestructionOn() ? "Enabled" : "Disabled"));
		sender.sendMessage(ChatColor.GREEN+"Restore Fee: "+ChatColor.RED+"$"+town.getRestoreFee());
		ArrayList<TownUserInvite> invitesList = plugin.getInviteManager().getUserInvitesForTown(town);
		sender.sendMessage(ChatColor.GREEN+"User Invites: "+(invitesList.isEmpty() ? ChatColor.GRAY+"None" : ""));
		for(TownUserInvite invite : invitesList)
			sender.sendMessage(ChatColor.BLUE + invite.getReceivingUserName());
	
		sender.sendMessage(ChatColor.GREEN+"Nation Invites: ");
		for(NationTownInvite invite : plugin.getInviteManager().getNationInvitesForTown(town))
			sender.sendMessage(invite.getSendingNation().getDisplayName());
			
		sender.sendMessage(ChatColor.GREEN+"Residents:");
		for(User townUser : town.getUsers()) 
			sender.sendMessage(ChatColor.GOLD+townUser.getRankedName()); //List users in town
		sender.sendMessage(ChatColor.YELLOW+"Type /town help for more information.");
	}
}
