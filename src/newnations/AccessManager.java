package newnations;

import java.util.HashMap;
import java.util.Set;

import org.json.simple.JSONObject;

public class AccessManager 
{

	private HashMap<Town, Byte> nationAccess = new HashMap<Town, Byte>();
	
	// this is used because when we are laoding the accessManagers the towns have not yet fully loaded yet,
	// so what we will do is store the string of the town name and after the load completes we will resolve to the objects
	
	private HashMap<String, Byte> nationAccessString ;
	
	public AccessManager(){}
	
	// load from JSON don't forget to call the reconnectStrings() method after all the towns are loaded
	public AccessManager(JSONObject nationAccessList)
	{
		nationAccessString = new HashMap<String, Byte>();
		for(Object s : nationAccessList.keySet()) 
		{
			long tempAByte = (Long) nationAccessList.get(s);
			byte accessByte = (byte) tempAByte;
			String townName = (String) s;
			
			nationAccessString.put(townName, accessByte);
		}
	}
	
	// if you loaded from JSON we need to now convert the string nation names into the proper objects
	public void reconnectStrings(NewNations plugin)
	{
		if(nationAccessString == null) return;
		for(String townName : nationAccessString.keySet())
		{
			Town thisTown = plugin.getTown(townName);
			if(thisTown != null ) nationAccess.put(thisTown, nationAccessString.get(townName));
		}
		
		//Don't need these anymore
		nationAccessString = null;		
	}
	public boolean switchAccess(Town town)
	{
		Byte access = nationAccess.get(town);
		if(access == null) return false;
		return (access & 0x01) != 0 || (access & 0x02) != 0;
	}	
	public boolean blockAccess(Town town)
	{
		Byte access = nationAccess.get(town);
		if(access == null)
		{
			System.out.print(town.getName()+" is not in the access list");
			return false; 
		}
		System.out.print(" access:" + access + " " + (access & 0x02) + " for town "+ town.getName());
		return (access & 0x02) != 0  ;
	}
	public boolean containerAccess(Town town)
	{
		Byte access = nationAccess.get(town);
		if(access == null) return false;
		return (access & 0x04) != 0;
	}
	
	private byte getExistingAccess(Town town)
	{
		Byte existingaccess = nationAccess.get(town);
		if(existingaccess == null) return 0;
		else return (byte)existingaccess;
	}
	
	public JSONObject save()
	{
		JSONObject nationAccessList = new JSONObject();
		for(Town t : nationAccess.keySet())
		{
			long aa = 0;
			Byte bb = nationAccess.get(t);
			if(bb == null) System.out.print("Null value detected for "+t.getName());
			else	aa = bb;
			nationAccessList.put(t.getName(), aa);			
		}
		return nationAccessList;
	}
	public Set<Town> getTownList()
	{
		return nationAccess.keySet();
	}
	public void addSwitchAccess(Town town)   {nationAccess.put(town, (byte)(getExistingAccess(town) | 0x01));}
	public void addBlockAccess(Town town)  {nationAccess.put(town, (byte)(getExistingAccess(town) | 0x02));}
	public void addContainerAccess(Town town) {nationAccess.put(town, (byte)(getExistingAccess(town) | 0x04));}
	public void removeSwitchAccess(Town town) {nationAccess.put(town, (byte)(getExistingAccess(town) & ~0x01));}
	public void removesetBlockAccess(Town town) {nationAccess.put(town, (byte)(getExistingAccess(town) & ~0x02));}
	public void removeContainerAccess(Town town) {nationAccess.put(town, (byte)(getExistingAccess(town) & ~0x04));}
	public void resetAccess(Town town) {nationAccess.remove(town);}
	
}
