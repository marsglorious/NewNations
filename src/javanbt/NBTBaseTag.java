package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public abstract class NBTBaseTag 
{
	private byte type;
	
	public NBTBaseTag(byte type)
	{
		this.type = type;
	}
	
	
	public byte getType() { return type;}
	

	
	public static NBTBaseTag read(DataInput input, byte tagtype) throws IOException
	{		
		if(tagtype == 10)
		{
			return NBTCompoundTag.newInstance(input);			
		}
		if(tagtype == 1)
		{
			return NBTByteTag.newInstance(input);			
		}
		if(tagtype == 2)
		{
			return NBTShortTag.newInstance(input);			
		}
		if(tagtype == 3)
		{
			return NBTIntTag.newInstance(input);			
		}
		if(tagtype == 4)
		{
			return NBTLongTag.newInstance(input);			
		}
		if(tagtype == 5)
		{
			return NBTFloatTag.newInstance(input);			
		}
		if(tagtype == 6)
		{
			return NBTDoubleTag.newInstance(input);			
		}
		if(tagtype == 8)
		{
			return NBTStringTag.newInstance(input);			
		}
		if(tagtype == 9)
		{
			return NBTListTag.newInstance(input);			
		}
		if(tagtype == 7)
		{
			return NBTByteArrayTag.newInstance(input);			
		}
		if(tagtype == 9)
		{
			return NBTListTag.newInstance(input);			
		}
		
		return null;
	}
	
	protected abstract void writeInternal(DataOutput input) throws IOException;
	
	public abstract String toString();
	
	

}
