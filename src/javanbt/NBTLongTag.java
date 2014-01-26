package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTLongTag extends NBTBaseTag
{
	
	private long value; 
	
	public NBTLongTag(long aa)
	{
		super((byte) 4);
		value = aa;		
	}
	
	protected static NBTLongTag newInstance(DataInput input) throws IOException 
	{ 
		return new NBTLongTag(input.readLong()); 
	}
	
	
	public long getValue() { return value; }
	
	public void setValue(long v) { value = v; }
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.writeLong(value);
	}
	
	public String toString()
	{
		return "NBTLongTag: value: "+value + " \n";
	}
	
}
