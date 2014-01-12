package javanbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;

public class NBTListTag extends NBTBaseTag 
{
	
	private ArrayList<NBTBaseTag> list = new  ArrayList<NBTBaseTag> ();
	private byte contains = 0;
	
	public NBTListTag()
	{
		super((byte) 9);
	}	
	
	protected static NBTListTag newInstance(DataInput input) throws IOException
	{ 
		NBTListTag newlist = new NBTListTag();
		byte containsType = input.readByte();
		newlist.contains = containsType;
		int count = input.readInt();
		
		for(int i = 0; i < count; i++)
		{
			NBTBaseTag newtag = NBTBaseTag.read(input, containsType);
			if(newtag != null) newlist.list.add( newtag);
		}
		return newlist;
	}
	
	public NBTBaseTag getTag(int index)
	{
		return list.get(index);
	}
	
	public boolean setTag(int index, NBTBaseTag newtag)
	{
		if(newtag == this) return false;
		if(newtag.getType() != contains && contains != 0) return false;
		list.add(index, newtag);
		return true;
	}
	
	public boolean addTag(NBTBaseTag newtag)
	{
		if(contains == 0) 
			contains = newtag.getType();		
		else if(newtag.getType() != contains )
			return false;
		
		list.add( newtag);
		return true;
	}
	
	public void removeTag(int index)
	{
		list.remove(index);
		if(list.size() == 0) contains = 0; 
	}
	
	public byte getContains()
	{
		return contains;
	}
	
	public int getSize()
	{
		return list.size();
	}
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		output.write(contains);
		output.writeInt(list.size());
		for(NBTBaseTag tag : list)
		{
			tag.writeInternal(output);
		}		
	}
	
	public String toString()
	{
		String ret = "NBTListTag, ContainsType: " + contains + " Size: " + list.size() + " {\n";
		for(NBTBaseTag tag : list)
		{
			ret += tag.toString();
		}
		ret += "\n}";
		return ret;
	}
	
}
