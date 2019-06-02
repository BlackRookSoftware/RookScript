package com.blackrook.rookscript.struct;

import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;

public class ScriptValueTest
{
	public static void main(String[] args)
	{
		ScriptValue sv = ScriptValue.create(false);
		sv.set((Character)'f');
		sv.set((Float)0.78f);
		for (int i = 0; i < 5000; i++)
		{
			long nanos = System.nanoTime();
			sv.set(Type.MAP, new Triple(34, -23, 45));
			System.out.println((System.nanoTime() - nanos));
		}
		//sv.setObjectRef(PairGroup.box(0, 0, 5, 5));
		System.out.println(sv);
	}
	
	public static class Triple
	{
		@ScriptIgnore
		public int junk;

		public int x;
		public int y;
		private int z;
		
		public Triple(int x, int y, int z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@ScriptName("butt")
		public int getZ()
		{
			return z;
		}
		
		@ScriptName("butt")
		public void setZ(int z)
		{
			this.z = z;
		}
	}
}
