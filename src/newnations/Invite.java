package newnations;

import org.json.simple.JSONObject;

///
public class Invite 
{

	private static long NextId = 0;
	private long id;
	
	//for town->player invites, this is a string containing the user name of the player being invited
	//for nation->town invites this is a pointer to the nation object, this invite object is then stored inside the receving towns object 
	//for nation->nation invites, it is a pointer to the sending nation
	private Object ConnectedObject;
	

	private long expire;

	public Invite() {}
	public Invite(String targetPlayer, long ex)
	{
		ConnectedObject = targetPlayer;
		expire = ex;
		id = NextId++;
	}
	public Invite(Object o)
	{
		ConnectedObject = o;
		id = NextId++;
		long unixTime = System.currentTimeMillis() / 1000L;
		expire = unixTime+172800; 
	}
	
	public void resetExpire()
	{
		long unixTime = System.currentTimeMillis() / 1000L;
		expire = unixTime+172800; 
	}
	public static void setNextId(Object id)
	{
		NextId = (Long)id;
	}
	public static long getNextId()
	{
		return (NextId);
		
	}
	public long getID() { return id; }

	
	public long getExpire()	{return expire;	}
	public Object getConnectedObject()	{ return ConnectedObject; 	}
	public void setConnectedObject(Object a) { ConnectedObject = a; }
	
	//MAKE SURE YOU RESOLVE THE STRINGS TO OBJECTS (see newnations.java)
	public Invite load(JSONObject obj)
	{
		id = (Long)obj.get("id");
		ConnectedObject = (Object) obj.get("ConnectedObject");
		expire = (Long) obj.get("expire");
		return this;
	}
	public JSONObject save()
	{
		JSONObject invite = new JSONObject();
		invite.put("id",  id);
		if(ConnectedObject instanceof Nation) invite.put("ConnectedObject", ((Nation)ConnectedObject).getName());
		else if(ConnectedObject instanceof Town) invite.put("ConnectedObject", ((Town)ConnectedObject).getName());
		else invite.put("ConnectedObject", (String)ConnectedObject);
		
		invite.put("expire", expire);
		return invite;
	}
	
	
}
