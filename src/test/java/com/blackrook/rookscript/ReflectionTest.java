/*******************************************************************************
 * Copyright (c) 2017-2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.resolvers.hostfunction.ClassMemberFunctionResolver;

public class ReflectionTest
{
	public static void main(String[] args) throws Exception
	{
		ScriptValue sv = ScriptValue.create(new int[]{1,2,3,4,5,6});
		List<Integer> list = new ArrayList<Integer>();
		list.add(0);
		list.add(0);
		list.add(0);
		list.add(0);
		System.out.println(list);
		sv.listApply(Integer.class, list);
		System.out.println(list);
	}
	
}
