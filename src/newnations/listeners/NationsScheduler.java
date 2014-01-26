package newnations.listeners;

import java.util.ArrayList;

import newnations.NewNations;
import newnations.Siege;

public class NationsScheduler 
{
	public NationsScheduler(final NewNations plugin)
	{
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
	    {
			public void run()
			{
				plugin.cullInvites();
	        }
	    }
		, 0L, 5 * 1200); //1200 = minute (variable)
		
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
	    {
			public void run()
			{
				ArrayList<Siege> list = plugin.getSieges();
				for( int i = list.size() -1; i >= 0 ; i--)
				{
					Siege s = list.get(i);
					s.validateOnlineUsers();
				}
	        }
	    }
		, 0L, 300); 
	}
}
