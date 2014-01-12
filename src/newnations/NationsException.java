package newnations;

import org.bukkit.command.CommandSender;

public class NationsException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String error;
	public String info;
	
	public NationsException(String error, String info) 
	{
		super(error + " " + info);
		this.error = error;
		this.info = info;  
    }
	
	public void printError(CommandSender sender) {NewNationsHelper.errorText(sender, error, info);}
}
