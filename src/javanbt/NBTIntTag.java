package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTIntTag extends NBTBaseTag
{
	
	private int value; 
	
	public NBTIntTag(int aa)
	{
		super((byte) 3);
		value = aa;		
	}
	
	protected static NBTIntTag newInstance(DataInput input) throws IOException 
	{ 
		return new NBTIntTag(input.readInt()); 
	}
	
	
	public int getValue() { return value; }
	
	public void setValue(int v) { value = v; }
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.writeInt(value);
	}
	
	public String toString()
	{
		return "NBTIntTag: value: "+value + " \n";
	}
	
}
