/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.io.File;

import com.blackrook.commons.util.ArrayUtils;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.functions.MathFunctions;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.StandardIOFunctions;
import com.blackrook.rookscript.resolvers.variable.DefaultVariableResolver;

public class ScriptTest
{
	public static void main(String[] args) throws Exception
	{
		String fileName;
		if ((fileName = ArrayUtils.arrayElement(args, 0)) == null)
		{
			System.out.println("ERROR: No file name for script.");
			System.out.println("Usage: command [filename]");
			System.out.println();
			return;
		}
		
		ScriptInstance instance = ScriptInstance.build()
			.withSource(new File(fileName))
			.withFunctionResolver(CommonFunctions.getResolver())
				.andFunctionResolver(StandardIOFunctions.getResolver())
				.andFunctionResolver(MathFunctions.getResolver())
			.withScriptStack(16, 512)
			.withScope("script", new DefaultVariableResolver())
			.get();
		
		int x = 5000;
		while (x-- > 0)
		{
			long nanos = System.nanoTime();
			instance.call("main");
			System.out.println("Script returns: "+instance.popStackValue()+" "+(System.nanoTime()-nanos)+" ns");
		}
		
	}
	
}

