/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

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
