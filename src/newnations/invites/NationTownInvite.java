package newnations.invites;

import newnations.Nation;
import newnations.NewNations;
import newnations.Town;

import org.json.simple.JSONObject;

public class NationTownInvite 
{

	private Nation sendingNation;
	private Town receivingTown;
	private long expiretime;
	private NewNations plugin;
	private boolean isValid = false;
	
	public NationTownInvite(NewNations plugin, Nation sender, Town recever, long validForSeconds)
	{
		sendingNation = sender;
		receivingTown = recever;
		this.expiretime = validForSeconds + System.currentTimeMillis() / 1000L;
		isValid = true;
	}
	
	public NationTownInvite(NewNations plugin, Nation sender, Town recever)
	{
		this(plugin,sender, recever, 172800 );
	}
	
	public long resetExpire()
	{
		expiretime = 172800 + System.currentTimeMillis() / 1000L;
		return expiretime;
	}
	
	public NationTownInvite(NewNations plugin, JSONObject obj)
	{
		this.plugin = plugin;
		String townName = (String) obj.get("townName");
		String nationName = (String) obj.get("nationName");
		long expire = (Long) obj.get("expire");
		
		expiretime = expire;
		if(expiretime < System.currentTimeMillis() / 1000L) return;
	
		receivingTown = plugin.getTown(townName);
		if(receivingTown == null) return;
		
		sendingNation = plugin.getNation(nationName);
		if(sendingNation == null) return;
		
		isValid = true;
	}
	
	public JSONObject save()
	{
		JSONObject thisInviteJson = new JSONObject();
		thisInviteJson.put("townName", receivingTown.getName());
		thisInviteJson.put("nationName", sendingNation.getName());
		thisInviteJson.put("expire", expiretime);
		
		return thisInviteJson;
	}
	
	public void invalidate() {isValid = false;}
	
	public boolean update()
	{
		if(expiretime < System.currentTimeMillis() / 1000L)
		{
			isValid = false;
		}
		return isValid;
	}
	
	public boolean isValid() {return isValid;}
	public Town getReceivingTown() {return receivingTown;}
	public Nation getSendingNation() {return sendingNation;}
}
