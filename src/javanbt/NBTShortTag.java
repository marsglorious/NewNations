package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTShortTag extends NBTBaseTag
{
	
	private short value; 
	
	public NBTShortTag(short aa)
	{
		super((byte) 2);
		value = aa;		
	}
	
	public NBTShortTag(int aa) {this((short)aa);}


	protected static NBTShortTag newInstance(DataInput input) throws IOException 
	{
		return new NBTShortTag(input.readShort()); 

	}
	
	public short getValue() { return value; }
	
	public void setValue(short v) { value = v; }
	public void setValue(int v) { value = (short) v; }
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.writeShort(value);
	}
	
	public String toString()
	{
		return "NBTShortTag: value: "+value + " \n";
	}
	
}
