package newnations.invites;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import newnations.Alliance;
import newnations.Nation;
import newnations.NewNations;
import newnations.Town;

public class InviteManager 
{

	private ArrayList<TownUserInvite> townUserInvites = new ArrayList<TownUserInvite>();
	private ArrayList<NationTownInvite> nationTownInvites = new ArrayList<NationTownInvite>();
	private ArrayList<AllianceNationInvite> allianceInvites = new ArrayList<AllianceNationInvite>();
	private NewNations plugin;
	
	
	public InviteManager(NewNations plugin)
	{
		this.plugin = plugin;		
	}
	
	public InviteManager(NewNations plugin, JSONObject obj)
	{
		this.plugin = plugin;	
		
		//load the townuser invites
		JSONArray townUserArray = (JSONArray) obj.get("townUserInvites");
		for(int i = 0; i < townUserArray.size(); i++)
		{
			TownUserInvite inv = new TownUserInvite(plugin, (JSONObject)townUserArray.get(i));
			if(inv.isValid()) townUserInvites.add(inv);
		}
		
		JSONArray nationTownArray = (JSONArray) obj.get("nationTownInvites");
		for(int i = 0; i < nationTownArray.size(); i++)
		{
			NationTownInvite inv = new NationTownInvite(plugin, (JSONObject)nationTownArray.get(i));
			if(inv.isValid()) nationTownInvites.add(inv);
		}
		
		JSONArray allianceNationArray = (JSONArray) obj.get("allianceNationInvites");
		for(int i = 0; i < allianceNationArray.size(); i++)
		{
			AllianceNationInvite inv = new AllianceNationInvite(plugin, (JSONObject)allianceNationArray.get(i));
			if(inv.isValid()) allianceInvites.add(inv);
		}
	}
	
	public JSONObject save()
	{
		JSONObject inviteman = new JSONObject();
		JSONArray townusersarray = new JSONArray();
		for(TownUserInvite inv : townUserInvites) 
			if(inv.update()) townusersarray.add(inv.save());
		
		JSONArray nationtownsarray = new JSONArray();
		for(NationTownInvite inv : nationTownInvites) 
			if(inv.update()) nationtownsarray.add(inv.save());
		
		JSONArray alliancearray = new JSONArray();
		for(AllianceNationInvite inv : allianceInvites) 
			if(inv.update()) alliancearray.add(inv.save());
		
		inviteman.put("townUserInvites", townusersarray);
		inviteman.put("nationTownInvites", nationtownsarray);
		inviteman.put("allianceNationInvites", alliancearray);
		return inviteman;
	}
	
	public boolean inviteUserToTown(Town town, String username)
	{
		for(TownUserInvite oldInv : getTownInvitesForUser(username))
		{
			if(oldInv.getSendingTown() == town && oldInv.isValid())
			{
				oldInv.resetExpire();
				return true;
			}
		}
		
		TownUserInvite newInvite = new TownUserInvite(plugin, town, username);
		if(newInvite.isValid()) 
		{
			townUserInvites.add(newInvite);
			return true;
		}
		return false;
	}
	
	public boolean inviteTownToNation(Nation nation, Town town)
	{		
		for(NationTownInvite oldInv : getNationInvitesForTown(town))
		{
			if(oldInv.getSendingNation() == nation && oldInv.isValid())
			{
				oldInv.resetExpire();
				return true;
			}
		}
		
		NationTownInvite newInvite = new NationTownInvite(plugin, nation, town);
		if(newInvite.isValid()) 
		{
			nationTownInvites.add(newInvite);
			return true;
		}
		return false;
	}
	
	public boolean inviteNationToAlliance(Alliance alliance, Nation nation )
	{
		for(AllianceNationInvite oldInv : getAllianceInvitesForNation(nation))
		{
			if(oldInv.getSendingAllaince() == alliance && oldInv.isValid())
			{
				oldInv.resetExpire();
				return true;
			}
		}
		
		AllianceNationInvite newInvite = new AllianceNationInvite(plugin, alliance, nation);
		if(newInvite.isValid()) 
		{
			allianceInvites.add(newInvite);
			return true;
		}
		return false;
	}
	
	//get the invites sent TO a user FROM a town
	public ArrayList<TownUserInvite> getTownInvitesForUser(String username)
	{
		ArrayList<TownUserInvite> thislist = new ArrayList<TownUserInvite>();
		for(TownUserInvite inv : townUserInvites)
			if(inv.update() && inv.getReceivingUserName().equalsIgnoreCase(username)) thislist.add(inv);
		return thislist;
	}
	
	//returns the invite sent TO a town FROM a nation
	public ArrayList<NationTownInvite> getNationInvitesForTown(Town town)
	{
		ArrayList<NationTownInvite> thislist = new ArrayList<NationTownInvite>();
		for(NationTownInvite inv : nationTownInvites)
			if(inv.update() && inv.getReceivingTown() == town) thislist.add(inv);
		return thislist;
	}
	
	//returns the invites sent TO a nation FROM an alliance
	public ArrayList<AllianceNationInvite> getAllianceInvitesForNation(Nation nation)
	{
		ArrayList<AllianceNationInvite> thislist = new ArrayList<AllianceNationInvite>();
		for(AllianceNationInvite inv : allianceInvites)
			if(inv.update() && inv.getReceivingNation() == nation) thislist.add(inv);
		return thislist;
	}
	
	// 
	///
	///
	///
	//
	
	public ArrayList<TownUserInvite> getUserInvitesForTown(Town town)
	{
		ArrayList<TownUserInvite> thislist = new ArrayList<TownUserInvite>();
		for(TownUserInvite inv : townUserInvites)
		{
			if(inv.update() && inv.getSendingTown() == town) thislist.add(inv);
		}
		return thislist;
	}
	
	public ArrayList<NationTownInvite> getTownInvitesForNation(Nation nation)
	{
		ArrayList<NationTownInvite> thislist = new ArrayList<NationTownInvite>();
		for(NationTownInvite inv : nationTownInvites)
		{
			if(inv.update() && inv.getSendingNation() == nation) thislist.add(inv);
		}
		return thislist;
	}
	
	public ArrayList<AllianceNationInvite> getNationInvitesForAlliance(Alliance alliance)
	{
		ArrayList<AllianceNationInvite> thislist = new ArrayList<AllianceNationInvite>();
		for(AllianceNationInvite inv : allianceInvites)
		{
			if(inv.update() && inv.getSendingAllaince() == alliance ) thislist.add(inv);
		}
		return thislist;
	}
	
	
	///
	///
	//
	///
	
	public boolean removeUserInviteFromTown(Town town, String userToRemove)
	{
		boolean found = false;
		ArrayList<TownUserInvite> invitelist = getUserInvitesForTown(town);
		for(TownUserInvite inv : invitelist) 
		{
			if(inv.getReceivingUserName().equalsIgnoreCase(userToRemove)) 
			{
				inv.invalidate();
				found = true;
			}
		}
		return found;
	}
	
	public void removeInviteContaining(Object object)
	{
		for(int i = townUserInvites.size() -1; i >= 0; i--)
		{
			TownUserInvite inv = townUserInvites.get(i);
			if(inv.getSendingTown().equals(object) || inv.getReceivingUserName().equals(object)) townUserInvites.remove(i);
		}
		
		for(int i = nationTownInvites.size() -1; i >= 0; i--)
		{
			NationTownInvite inv = nationTownInvites.get(i);
			if(inv.getSendingNation().equals(object) || inv.getReceivingTown().equals(object)) townUserInvites.remove(i);
		}
		
		for(int i = allianceInvites.size() -1; i >= 0; i--)
		{
			AllianceNationInvite inv = allianceInvites.get(i);
			if(inv.getSendingAllaince().equals(object) || inv.getReceivingNation().equals(object)) townUserInvites.remove(i);
		}
	}

	public void cull()
	{
		//TODO: do this method
	}
}
