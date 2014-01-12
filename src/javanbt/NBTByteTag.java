package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTByteTag extends NBTBaseTag
{
	
	private byte value; 
	
	public NBTByteTag(byte aa)
	{
		super((byte) 1);
		value = aa;		
	}

	protected static NBTByteTag newInstance(DataInput input) throws IOException 
	{
		return new NBTByteTag(input.readByte()); 

	}
	
	public byte getValue() { return value; }
	
	public void setValue(byte v) { value = v; }
	
	public void setBooleanValue(boolean v) { value = (byte) (v ? 1 : 0);}
	
	public boolean getBooleanValue() { return value == 0 ? false : true;}
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.write(value);
	}
	
	public String toString()
	{
		return "NBTByteTag: value: "+value + " \n";
	}
	
}
