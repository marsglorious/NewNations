package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTDoubleTag extends NBTBaseTag
{
	
	private double value; 
	
	public NBTDoubleTag(double aa)
	{
		super((byte) 6);
		value = aa;		
	}

	protected static NBTDoubleTag newInstance(DataInput input) throws IOException 
	{
		return new NBTDoubleTag(input.readDouble()); 

	}
	
	public double getValue() { return value; }
	
	public void setValue(double v) { value = v; }
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.writeDouble(value);
	}
	
	public String toString()
	{
		return "NBTDoubleTag: value: "+value + " \n";
	}
	
}
