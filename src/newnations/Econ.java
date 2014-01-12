package newnations;

import java.util.HashMap;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

public class Econ 
{
	public static int EMERALD_VALUE = 100; 
	public static int TOWN_SETUP_FEE = 200;
	public static int SIEGE_FEE = 1000;
	public static int WARCAMP_FEE = 300;
	public static int TRIBUTE = 1000;
	
	public static Economy econ = null;
	
	public Econ(NewNations plugin)
	{
		//Config file setup
		EMERALD_VALUE = (Integer) plugin.getConfig().get("emeraldValue");
		TOWN_SETUP_FEE = (Integer) plugin.getConfig().get("townSetupFee");
		SIEGE_FEE = (Integer) plugin.getConfig().get("siegeFee");
		WARCAMP_FEE = (Integer) plugin.getConfig().get("warcampFee");
		TRIBUTE = (Integer) plugin.getConfig().get("tribute");
		
		//Setup Vault
		if(plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
		RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if(rsp != null) econ = rsp.getProvider();
	}
	
	public static double deposit(Player p, double amount)
	{
		if(econ != null)
		{
			econ.depositPlayer(p.getName(), amount);
			NewNationsHelper.notifyText(p, ""+ChatColor.GREEN+"$"+amount+ChatColor.YELLOW+" deposited to you - New Balance: "+ChatColor.BLUE+"$"+econ.getBalance(p.getName()));
			return amount;
		}
		else
		{
			//player.sendMessage("econ is null");
			int a = (int)Math.floor(amount / EMERALD_VALUE);
			if(a > 0)
			{
				HashMap<Integer, ItemStack> overflow = p.getInventory().addItem(new ItemStack(Material.EMERALD, a));
				for(ItemStack stack : overflow.values()) p.getWorld().dropItem(p.getLocation(), stack); //Drop overflow items next to the player.
			}
			NewNationsHelper.notifyText(p, ""+ChatColor.GREEN+a+ChatColor.YELLOW+" emerald(s) deposited to your inventory.");
			return a * EMERALD_VALUE;
		}
	}
	
	public static double withdraw(Player p, double amount) throws NationsException
	{
		if(econ != null)
		{
			if(!econ.has(p.getName(), amount)) 
				throw new NationsException("Insufficient funds.", "You need "+ChatColor.GREEN+"$"+amount+ChatColor.YELLOW+" - You have "+ChatColor.BLUE+"$"+econ.getBalance(p.getName())+ChatColor.YELLOW+".");
			econ.withdrawPlayer(p.getName(), amount);
			NewNationsHelper.notifyText(p, ""+ChatColor.RED+"$"+amount+ChatColor.YELLOW+" deducted from you - New Balance: "+ChatColor.BLUE+"$"+econ.getBalance(p.getName()));
			return amount;
		}
		else //Emerald currency alternative
		{
			int a = (int)Math.ceil(amount / EMERALD_VALUE);
			if(!p.getInventory().contains(Material.EMERALD, a)) throw new NationsException("Insufficient emeralds.", "You need "+ChatColor.GREEN+""+a+ChatColor.YELLOW+" emeralds.");
			
			int emeraldFeeTemp = a;
			for(int i = 0; i < p.getInventory().getSize(); i++)
			{
				ItemStack stack = p.getInventory().getItem(i);
				if(stack== null || stack.getType() != Material.EMERALD) continue;
				if(stack.getAmount() > emeraldFeeTemp) 
				{
					stack.setAmount(stack.getAmount() - emeraldFeeTemp);
					break;
				}
				else
				{
					emeraldFeeTemp -= stack.getAmount();
					p.getInventory().setItem(i, null);
					if(emeraldFeeTemp <= 0) break;
				}
			}
			NewNationsHelper.notifyText(p, ""+ChatColor.RED+a+ChatColor.YELLOW+" emerald(s) deducted from your inventory.");
			return a * EMERALD_VALUE;
		}	
	}
	
	//If the amount is greater than the balance, withdraw the balance.
	public static double uncheckedWithdraw(Player p, double amount) throws NationsException 
	{
		double balance;
		if(econ == null) balance = getEmeraldBalance(p) * EMERALD_VALUE;
		else balance = econ.getBalance(p.getName());
		if(amount > balance) 
		{
			NewNationsHelper.notifyText(p, "You lack "+ChatColor.GREEN+"$"+amount+ChatColor.YELLOW+", withdrawing "+ChatColor.BLUE+"$"+balance+ChatColor.YELLOW+" instead.");
			return withdraw(p, balance);
		}
		else return withdraw(p, amount);
	}
	
	public static double getMoneyBalance(String username) {return econ.getBalance(username);}
	public static int getEmeraldBalance(Player p)
	{
		int emeralds = 0;
		for(int i = 0; i < p.getInventory().getSize(); i++)
		{
			ItemStack stack = p.getInventory().getItem(i);
			if(stack != null && stack.getType() == Material.EMERALD) emeralds += stack.getAmount();
		}
		return emeralds;
	}
	public static Economy getEcon() {return econ;}
	public static boolean econEnabled() {return econ != null;}
}
