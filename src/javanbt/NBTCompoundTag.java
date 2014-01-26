package javanbt;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NBTCompoundTag extends NBTBaseTag
{
	private HashMap<String, NBTBaseTag> map = new HashMap<String, NBTBaseTag>();
	
	public NBTCompoundTag() 
	{
		super((byte) 10); 
	}
		
	protected static NBTCompoundTag newInstance(DataInput input) throws IOException 
	{ 
		NBTCompoundTag newtag = new NBTCompoundTag(); 
		while(true)
		{			
			byte tagType = input.readByte();
			if(tagType == 0) break;
			
			String newName = input.readUTF();
			//System.out.print(newName);
			
			newtag.map.put(newName, NBTBaseTag.read(input, tagType));
		}
		return newtag;
	}
	
	// NOTE: these load functions will return a compound with one tag
	// this is done so you can view its name
	public static NBTCompoundTag load(DataInput input) throws IOException
	{
		byte tagType = input.readByte();
		if(tagType == 0) return null;
		
		String newName = input.readUTF();
		
		NBTCompoundTag newcomp = new NBTCompoundTag();
		newcomp.map.put(newName, NBTBaseTag.read(input, tagType));
		return newcomp;
	}
	
	public static NBTCompoundTag loadFromFile(File file) throws IOException
	{
		DataInputStream ds2 = new DataInputStream(new GZIPInputStream(new FileInputStream(file)));	
		NBTCompoundTag abc =  load(ds2);
		ds2.close();
		return abc;
	}
	

	
	public NBTBaseTag getTag(String name)
	{
		return map.get(name);
	}
	
	public int getSize()
	{
		return map.size();
	}
	
	public void setTag(String name, NBTBaseTag newtag)
	{
		if(newtag == this) return;
		map.put(name, newtag);
	}
	
	public Set<String> getTagNames()
	{
		return map.keySet();
	}
	
	// todo check this
	public NBTBaseTag getTag(int index)
	{
		return (NBTBaseTag) map.entrySet().toArray()[index];
	}
	
	protected void writeInternal(DataOutput output) throws IOException
	{		
		Set<String> keys = map.keySet();		
		for(String key : keys)
		{
			
			NBTBaseTag tag = map.get(key);
			output.write(tag.getType());
			output.writeUTF(key);
			tag.writeInternal(output);
		}
		output.write((byte)0);
	}
	
	
	public void write(DataOutput output, String name) throws IOException
	{
		output.write((byte)10);
		output.writeUTF(name);
		
		writeInternal(output);
		
	}
	
	public void saveToFile(File file, String rootTagName) throws IOException
	{
		DataOutputStream ds = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)));	
		write(ds, rootTagName);
		ds.close();
	}
	
	public String toString()
	{
		String ret = "NBTCompoundTag, Size: " + map.size() + " {\n";
		Set<String> keys = map.keySet();		
		for(String key : keys)
		{			
			ret += "Name: " + key + " ";
			NBTBaseTag tag = map.get(key);
			if(tag == null) System.out.print("Unn value found for key "+key);
			ret += tag.toString();
		}
		ret += "\n}";
		return ret;
	}
}











