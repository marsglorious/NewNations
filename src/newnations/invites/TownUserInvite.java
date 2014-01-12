package newnations.invites;

import org.json.simple.JSONObject;


import newnations.NewNations;
import newnations.Town;

public class TownUserInvite 
{

	private Town sendingTown;
	private String receivingUserName;
	private long expiretime;
	private NewNations plugin;
	private boolean isValid = false;
	
	public TownUserInvite(NewNations plugin, Town sender, String recever, long validForSeconds)
	{
		sendingTown = sender;
		receivingUserName = recever;
		this.expiretime = validForSeconds + System.currentTimeMillis() / 1000L;
		isValid = true;
	}
	
	public TownUserInvite(NewNations plugin, Town sender, String recever)
	{
		this(plugin,sender, recever, 172800 );
	}
	
	public TownUserInvite(NewNations plugin, JSONObject obj)
	{
		this.plugin = plugin;
		String townName = (String) obj.get("townName");
		String username = (String) obj.get("userName");
		long expire = (Long) obj.get("expire");
		
		expiretime = expire;
		if(expiretime < System.currentTimeMillis() / 1000L) return;
	
		sendingTown = plugin.getTown(townName);
		if(sendingTown == null) return;
		
		
		receivingUserName = username;
		if(receivingUserName == null || receivingUserName.isEmpty()) return;
		
		isValid = true;
	}
	
	public JSONObject save()
	{
		JSONObject thisInviteJson = new JSONObject();
		thisInviteJson.put("townName", sendingTown.getName());
		thisInviteJson.put("userName", receivingUserName);
		thisInviteJson.put("expire", expiretime);
		return thisInviteJson;
	}
	
	public boolean update()
	{
		if(expiretime < System.currentTimeMillis() / 1000L)
		{
			isValid = false;
		}
		return isValid;
	}
	
	public long resetExpire()
	{
		expiretime = 172800 + System.currentTimeMillis() / 1000L;
		return expiretime;
	}
	
	public boolean isValid() {return isValid;}
	public void invalidate() {isValid = false;}
	public String getReceivingUserName() {return receivingUserName;}
	public Town getSendingTown() {return sendingTown; }
}
