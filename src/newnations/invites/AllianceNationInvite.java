package newnations.invites;

import newnations.Alliance;
import newnations.Nation;
import newnations.NewNations;
import org.json.simple.JSONObject;

public class AllianceNationInvite 
{
	private Alliance sendingAlliance;
	private Nation receivingNation;
	private long expiretime;
	private NewNations plugin;
	private boolean isValid = false;
	
	public AllianceNationInvite(NewNations plugin, Alliance sender, Nation recever, long validForSeconds)
	{
		sendingAlliance = sender;
		receivingNation = recever;
		this.expiretime = validForSeconds + System.currentTimeMillis() / 1000L;
		isValid = true;
	}
	
	public AllianceNationInvite(NewNations plugin, Alliance sender, Nation receiver)
		{this(plugin,sender, receiver, 172800 );}
	
	public AllianceNationInvite(NewNations plugin, JSONObject obj)
	{
		this.plugin = plugin;
		String allianceName = (String) obj.get("allianceName");
		String nationName = (String) obj.get("nationName");
		long expire = (Long) obj.get("expire");
		
		expiretime = expire;
		if(expiretime < System.currentTimeMillis() / 1000L) return;
		
		sendingAlliance = plugin.getAlliance(allianceName);
		if(sendingAlliance == null) return;
		
		receivingNation = plugin.getNation(nationName);
		if(receivingNation == null) return;
		
		isValid = true;
	}
	
	public JSONObject save()
	{
		JSONObject thisInviteJson = new JSONObject();
		thisInviteJson.put("nationName", receivingNation.getName());
		thisInviteJson.put("allianceName", sendingAlliance.getName());
		thisInviteJson.put("expire", expiretime);
		
		return thisInviteJson;
	}
	public boolean update()
	{
		if(expiretime < System.currentTimeMillis() / 1000L) isValid = false;
		return isValid;
	}
	
	public long resetExpire()
	{
		expiretime = 172800 + System.currentTimeMillis() / 1000L;
		return expiretime;
	}
	public void invalidate() {isValid = false;}
	public boolean isValid() {return isValid;}
	public Nation getReceivingNation() {return receivingNation;}
	public Alliance getSendingAllaince() {return sendingAlliance;}
}
