package newnations;
import java.util.ArrayList;
import java.util.Collections;

import org.bukkit.ChatColor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Nation
{
	private String name;
	private String code;
	private ArrayList<Town> towns = new ArrayList<Town>();
	private NewNations plugin;
	
	/**
	 * Create a new nation for town <town>
	 * <town> will then be set as the capital
     * @return the new Nation object
	 * @throws NationsException 
     */
	public Nation(NewNations plugin, Town town) throws NationsException
	{
		this.plugin = plugin;
		rename("City-state of "+town.getName());
		addTown(town);
		code = plugin.createCode(name);
		plugin.addNation(this);
	}
	
	public Nation(NewNations plug, JSONObject obj)
	{
		plugin = plug;
		load(obj);
	}
	
	public Nation load(JSONObject obj )
	{
		try {name = (String) obj.get("name");}
		catch(Exception e) {}
		try {code = (String) obj.get("code");}
		catch(Exception e) {}
		
		JSONArray townsArray = (JSONArray) obj.get("towns");

		for(int i = 0; i < townsArray.size(); i++) 
		{
			Town town = new Town(plugin, (JSONObject)townsArray.get(i), this);
			if(town.getUsers().isEmpty() && plugin.getConfig().contains("removeEmptyTowns") && plugin.getConfig().getBoolean("removeEmptyTowns"))
			{	
				NewNationsHelper.notifyAll("The town of "+ChatColor.RED+town.getName()+ChatColor.YELLOW+" contains no residents and is being disbanded.");
				plugin.getServer().getLogger().info(town.getName()+" is owed: "+town.getTownNetWorth());
			}
			else if(town.getNation() == this) towns.add(town);
		}
		
		return this;
	}
	
	public void rename(String newName) throws NationsException
	{
		//TODO: check that town names don't match also
		if(plugin.isNameUnique(newName) == false) 
			throw new NationsException("Nation and town couldn't be created.", "Nation or town that name already exists.");
		name = newName;
	}

	public JSONObject save()
	{
		JSONObject nation = new JSONObject();
		nation.put("name", name);
		nation.put("code", code);
		JSONArray townsList = new JSONArray();
		JSONArray townInvitesList = new JSONArray();
		JSONArray alliancesList = new JSONArray();
		for(Town town : this.towns) townsList.add(town.save());
		nation.put("towns", townsList);
		nation.put("townInvites", townInvitesList);
		nation.put("alliances", alliancesList); //is this required?
		return nation;
	}
	
	public Town getTown(User user)
	{
		for(Town town : towns) if(town.getUsers().contains(user)) return town;
		return null;
	}

	public Town getTown(Plot plot)
	{
		for(Town town : towns) if(town.getPlots().contains(plot)) return town;
		return null;
	}
	
	public boolean addTown(Town town) 
	{
		if(towns.contains(town)) return false;
		towns.add(town);
		return true;
	}
	
		
	public void setCapital(Town t) throws NationsException
	{
		if(towns.contains(t)) Collections.swap(towns, 0, towns.indexOf(t));
		else throw new NationsException("Cannot find town.", "The town "+ChatColor.GRAY+t.getName()+ChatColor.YELLOW+"is not part of nation.");
	}
	public Town getCapital() {return towns.get(0);}
	
	public void removeTown(Town town) throws NationsException 
	{
		towns.remove(town);
		for(Town t : towns)
			t.getAccessMan().resetAccess(town);
		if(towns.size() < 1) destroy();
	}
	
	public String getDisplayName() {return ChatColor.GOLD+name+" "+ChatColor.LIGHT_PURPLE+code;}
	/**
	 * Destroy Nation
	 * Deletes a nation from existence. and removes it from any alliances
	 * the nation must be empty for this to work
	 * @throws NationsException 
     */
	public void destroy() throws NationsException
	{
		if(towns.size() > 0) 
			throw new NationsException("Unable to disband the nation", "The nation "+ChatColor.GRAY+name+ChatColor.YELLOW+" still has towns in it.");
		plugin.removeNation(this);
		for(Alliance a : getAlliances())
			a.removeNation(this);
		plugin.getInviteManager().removeInviteContaining(this);
		NewNationsHelper.notifyAll("The nation of "+ChatColor.GREEN+name+ChatColor.DARK_AQUA+" has faded into history.");
	}
	
	public Alliance getAlliance()
	{
		for(Alliance alliance : plugin.alliances) 
			if(alliance.getNations().contains(this)) return alliance;
		return null;
	}
	public ArrayList<Alliance> getAlliances()
	{
		ArrayList<Alliance> alliances2 = new ArrayList<Alliance>();
		for(Alliance all: plugin.alliances)
			if(all.getNations().contains(this)) alliances2.add(all);  
		return alliances2;
	}
	
	//called when the nation is loaded to perform any checks
	public void checkAll(NewNations plugin)
	{
		if(code == null) code = plugin.createCode(name);
		for(Town t : towns) t.checkAll();
	}
	
	public void notifyText(String message, Town excludedTown)
	{
		for(Town town : towns) 
			if(town != excludedTown) town.notifyText(message, null);	
	}
	

	public boolean isFightingWith(Nation otherNation)
	{
		for(Siege s : plugin.getSieges())
		{
			ArrayList<Nation> defendintNations = s.getDefendingNations();
			ArrayList<Nation> beseigingNations = s.getBesiegingNations();
			if(defendintNations.contains(this) && beseigingNations.contains(otherNation)) return true;
			if(beseigingNations.contains(this) && defendintNations.contains(otherNation)) return true;
		}
		return false;
	}
	
	public String toString() {return getDisplayName();}
	public ArrayList<Town> getTowns() {return towns;}

	public String getName() {return name;}
	public String getCode() { return code;}
	public void setCode(String c) { code = c;}
}
	
