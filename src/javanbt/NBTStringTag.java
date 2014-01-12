package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTStringTag extends NBTBaseTag
{
	
	private String value; 
	
	public NBTStringTag(String aa)
	{
		super((byte) 8);
		value = aa;		
	}

	protected static NBTStringTag newInstance(DataInput input) throws IOException 
	{
		return new NBTStringTag(input.readUTF()); 

	}
	
	public String getValue() { return value; }
	
	public void setValue(String v) { value = v; }
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.writeUTF(value);
	}
	
	public String toString()
	{
		return "NBTStringTag: value: "+value + " \n";
	}
	
}