/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.functions.MathFunctions;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.StandardIOFunctions;
import com.blackrook.rookscript.resolvers.variable.DefaultVariableResolver;
import com.blackrook.rookscript.struct.Utils;

@SuppressWarnings("unused")
public class ScriptTest
{
	private static void doDisassemble(ScriptInstance instance) throws IOException
	{
		StringWriter sw = new StringWriter();
		ScriptAssembler.disassemble(instance.getScript(), new PrintWriter(sw));
		System.out.print(sw);
	}
	
	private static void doStress(ScriptInstance instance, int times)
	{
		while (times-- > 0)
		{
			long nanos = System.nanoTime();
			instance.call("main");
			System.out.println("Script returns: "+instance.popStackValue()+" "+(System.nanoTime()-nanos)+" ns");
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		String fileName;
		if ((fileName = Utils.arrayElement(args, 0)) == null)
		{
			System.out.println("ERROR: No file name for script.");
			System.out.println("Usage: command [filename]");
			System.out.println();
			return;
		}
		
		ScriptInstance instance = ScriptInstance.build()
			.withSource(fileName, Utils.openResource(fileName))
			.withEnvironment(ScriptEnvironment.createStandardEnvironment())
			.withFunctionResolver(CommonFunctions.createResolver())
				.andFunctionResolver(StandardIOFunctions.createResolver())
				.andFunctionResolver(MathFunctions.createResolver())
			.withScriptStack(16, 512)
			.withScope("script", new DefaultVariableResolver())
			.get();
		
		//doDisassemble(instance);
		doStress(instance, 5000);
	}
	
}

