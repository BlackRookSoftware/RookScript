/*******************************************************************************
 * Copyright (c) 2017-2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.functions.MathFunctions;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.PrintFunctions;
import com.blackrook.rookscript.resolvers.variable.DefaultVariableResolver;
import com.blackrook.rookscript.struct.Utils;

@SuppressWarnings("unused")
public class NamespacedScriptTest
{
	private static void doDisassemble(ScriptInstance instance) throws IOException
	{
		StringWriter sw = new StringWriter();
		ScriptAssembler.disassemble(instance.getScript(), new PrintWriter(sw));
		System.out.print(sw);
	}
	
	private static void doStress(ScriptInstance instance, int times)
	{
		final long x = times;
		long nanototal = System.nanoTime();
		ScriptValue out = ScriptValue.create(null);
		while (times-- > 0)
		{
			long nanos = System.nanoTime();
			instance.call("main");
			nanos = System.nanoTime() - nanos;
			instance.popStackValue(out);
			System.out.println("Script returns: " + out + " " + nanos + " ns");
		}
		System.out.println("Average: "+((System.nanoTime() - nanototal) / x)+" ns");
	}
	
	public static void main(String[] args) throws Exception
	{
		String res = "scripts/namespacetest.txt";
		ScriptInstance instance = ScriptInstance.createBuilder()
			.withSource(res, Utils.openResource(res))
			.withEnvironment(ScriptEnvironment.createStandardEnvironment())
			.withFunctionResolver(CommonFunctions.createResolver())
				.andFunctionResolver("io", PrintFunctions.createResolver())
				.andFunctionResolver("math", MathFunctions.createResolver())
			.withScriptStack(16, 512)
			.withScope("script", new DefaultVariableResolver())
			.createInstance();
		
		//doDisassemble(instance);
		doStress(instance, 5000);
	}
	
}

