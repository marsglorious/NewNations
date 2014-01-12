package newnations;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Alliance
{
	private String name;
	private String code;
	private ArrayList<Nation> nations = new ArrayList<Nation>();
	private NewNations plugin;
	
	public Alliance(NewNations plug, String name) throws NationsException
	{
		plugin = plug;
		
		rename(name);
		code = plugin.createCode(name);
		plugin.addAlliance(this);
	}
	
	public Alliance(NewNations plug, JSONObject obj)
	{
		plugin = plug;
		name = (String) obj.get("name");
		code = (String) obj.get("code");
		JSONArray nationsArray = (JSONArray) obj.get("nations");

		for(int i = 0; i < nationsArray.size(); i++) 
		{
			Nation n = plugin.getNation((String) nationsArray.get(i));
			if(!nations.contains(n) && n != null) nations.add(n);
		}

		plugin.addAlliance(this);
	}
	
	public String getDisplayName()
	{
		return ChatColor.BLUE + name + " " + ChatColor.AQUA + code;
	}
	
	public JSONObject save()
	{
		JSONObject alliance = new JSONObject();
		alliance.put("name", name);
		alliance.put("code", code);
		
		//save nations
		JSONArray nationList = new JSONArray();
		for(Nation nation : nations) nationList.add(nation.getName());
		alliance.put("nations", nationList);
		
		return alliance;
	}
	
	public void rename(String newName) throws NationsException
	{
		if((plugin.getAlliance(newName) != null)) 
			throw new NationsException("Alliance  couldn't be created.", "Alliance with that name already exists.");
		name = newName;
	}
	

	public void addNation(Nation n) {nations.add(n);}

	
	public void removeNation(Nation nl)
	{
		ArrayList<Nation> newArray = new ArrayList<Nation>();
		for(Nation n : nations)
			if(n != nl) newArray.add(n);
		nations = newArray;
		
		if(nations.isEmpty()) destroy();
		
	}
	
	public void destroy()
	{
		plugin.getInviteManager().removeInviteContaining(this);
		 plugin.removeAlliance(this); 
	}
	public String toString() {return getDisplayName();}
	
	public void notifyText(String message,  Nation excludedNation)
	{
		for(Nation nation : nations) 
			if(nation != excludedNation) nation.notifyText(message, null);
	}
	public String getCode() {return code;}
	public void checkAll(NewNations plugin)
	{
		if(code == null || code.isEmpty()) code = plugin.createCode(name); 
		//TODO other checks
	}
	public String getName() {return name;}
	public ArrayList<Nation> getNations() {return nations;}
}
