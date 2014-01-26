package newnations;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class NewNationsHelper 
{
	
		// the exponential function that calculates the cost based on the size of nearby towns (excluding your own)
		public static double plotCostFunction(int sizeOfTown, double distance) 
		{
			return (sizeOfTown*2 + 10) * 1000 * Math.pow(4, distance * -0.15);
		}
		
		public static String connectStrings(String[] strings, int start)
		{
			String string = strings[start];
			for(int i = start + 1; i < strings.length; i++) 
				if(strings[i] != null) 
					string += " " + strings[i];
			return string;
		}
		
		/**
		 * @return: true if this user name only contains valid characters
	     */
		public static boolean isValidUsername(String s)
		{
			int len = s.length();
			if(len > 16) return false;
			
			for(int i = 0; i < len; i++)
			{
				char  c = s.charAt(i) ;
				if(c <= '9' && c >= '0') continue;
				if(c <= 'z' && c >= 'a') continue;
				if(c <= 'Z' && c >= 'A') continue;
				if(c == '_') continue;
				return false;
			}
			return true;
		}
		
		//Messaging Methods
		public static void messageAll(String message) {Bukkit.getServer().broadcastMessage(ChatColor.DARK_RED + "["+NewNations.class.getSimpleName()+"]: " + ChatColor.YELLOW + message);}
		public static void notifyAll(String message) {Bukkit.getServer().broadcastMessage(ChatColor.DARK_RED + "["+NewNations.class.getSimpleName()+"]: " + ChatColor.DARK_AQUA + message);}
		public static void notifyText(CommandSender sender, String message) {sender.sendMessage(ChatColor.YELLOW + message);}
		public static void helpText(CommandSender sender, String example, String info)
		{
			if(example != null && info != null) sender.sendMessage(ChatColor.GREEN + example + "\n" + ChatColor.YELLOW + info);
			else if(example != null) 			sender.sendMessage(ChatColor.GREEN + example);
			else if(info != null)				sender.sendMessage(ChatColor.YELLOW + info);
		}
		public static void errorText(CommandSender sender, String error, String info)
		{
			if(error != null && info != null) sender.sendMessage(ChatColor.RED + error + "\n" + ChatColor.YELLOW + info);
			else if(error != null) sender.sendMessage(ChatColor.RED + error);
			else if(info != null) sender.sendMessage(ChatColor.YELLOW + info);
		}
}
