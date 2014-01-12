package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTFloatTag extends NBTBaseTag
{
	
	private float value; 
	
	public NBTFloatTag(float aa)
	{
		super((byte) 5);
		value = aa;		
	}

	protected static NBTFloatTag newInstance(DataInput input) throws IOException 
	{
		return new NBTFloatTag(input.readFloat()); 

	}
	
	public float getValue() { return value; }
	
	public void setValue(float v) { value = v; }
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.writeFloat(value);
	}
	
	public String toString()
	{
		return "NBTFloatTag: value: "+value + " \n";
	}
	
}
