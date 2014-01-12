package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTIntArrayTag extends NBTBaseTag
{

	private int[] value; 
	
	public NBTIntArrayTag(int[] aa)
	{
		super((byte) 11);
		value = aa;		
	}
		
	protected static NBTIntArrayTag newInstance(DataInput input) throws IOException 
	{ 
		int arraysize = input.readInt();
		int[] newarray = new int[arraysize];
		
		for(int i = 0; i < arraysize; i++ )
		{
			newarray[i] = input.readInt(); 
		}
		return new NBTIntArrayTag(newarray);
	}
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.writeInt(value.length);
		for(int i = 0; i < value.length; i++ )
		{
			output.writeInt(value[i]);
		}
	}	
	
	public int[] getValue() { return value; }	
	public void setValue(int[] v) { value = v; }
	
	public String toString()
	{
		return "NBTIntArrayTag: size: "+ value.length + " \n";
	}
	
}