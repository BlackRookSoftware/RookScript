/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;
import com.blackrook.rookscript.resolvers.function.ClassMemberFunctionResolver;

public class ClassFieldFunctionTest2
{
	public static void main(String[] args) throws Exception
	{
		for (ScriptFunctionType t : ClassMemberFunctionResolver.create(Triple.class, "tget_", "tset_", "t", false, false).getFunctions())
		{
			System.out.println(t);
		}
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
