package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTByteArrayTag extends NBTBaseTag
{

	private byte[] value; 
	
	public NBTByteArrayTag(byte[] aa)
	{
		super((byte) 7);
		value = aa;		
	}
	

	
	protected static NBTByteArrayTag newInstance(DataInput input) throws IOException 
	{ 
		int arraysize = input.readInt();
		byte[] newarray = new byte[arraysize];
		input.readFully(newarray, 0, arraysize);
		return new NBTByteArrayTag(newarray);
	}
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.writeInt(value.length);
		output.write(value, 0, value.length);
	}	
	
	public byte[] getValue() { return value; }	
	public void setValue(byte[] v) { value = v; }
	
	public String toString()
	{
		return "NBTByteArrayTag: size: "+ value.length + " \n";
	}
	
}