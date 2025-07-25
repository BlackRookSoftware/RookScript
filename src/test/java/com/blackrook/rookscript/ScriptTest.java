/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
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
import com.blackrook.rookscript.functions.RegexFunctions;
import com.blackrook.rookscript.functions.ZipFunctions;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.DateFunctions;
import com.blackrook.rookscript.functions.DigestFunctions;
import com.blackrook.rookscript.functions.FileSystemFunctions;
import com.blackrook.rookscript.functions.IOFunctions;
import com.blackrook.rookscript.functions.PrintFunctions;
import com.blackrook.rookscript.functions.RandomFunctions;
import com.blackrook.rookscript.functions.common.BufferFunctions;
import com.blackrook.rookscript.functions.io.DataIOFunctions;
import com.blackrook.rookscript.functions.io.FileIOFunctions;
import com.blackrook.rookscript.functions.io.StreamingIOFunctions;
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
	
	private static void doStress(ScriptInstance instance, final long time)
	{
		ScriptValue out = ScriptValue.create(null);
		long total = 0L;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		
		long endTime = System.currentTimeMillis() + time;
		
		long times = 0L;
		while (System.currentTimeMillis() < endTime)
		{
			long nanos = System.nanoTime();
			instance.call("main");
			nanos = System.nanoTime() - nanos;
			total += nanos;
			min = Math.min(min, nanos);
			max = Math.max(max, nanos);
			instance.popStackValue(out);
			times++;
		}
		System.out.printf("%d iterations over %f secs: Min/Avg/Max: %d / %d / %d ns\n", times, (time / 1000.0), min, (total / times), max);
	}
	
	private static void doIterations(ScriptInstance instance, final int times)
	{
		ScriptValue out = ScriptValue.create(null);
		long total = 0L;
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		
		int x = times;
		while (x-- > 0)
		{
			long nanos = System.nanoTime();
			instance.call("main");
			nanos = System.nanoTime() - nanos;
			total += nanos;
			min = Math.min(min, nanos);
			max = Math.max(max, nanos);
			instance.popStackValue(out);
			System.out.println("Script returns: " + out + " " + nanos + " ns");
		}
		System.out.printf("Min/Avg/Max: %d / %d / %d ns\n", min, (total / times), max);
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
		
		ScriptInstance instance = ScriptInstance.createBuilder()
			.withSource(fileName, Utils.openResource(fileName))
			.withEnvironment(ScriptEnvironment.createStandardEnvironment())
			.withFunctionResolver(CommonFunctions.createResolver())
				.andFunctionResolver(PrintFunctions.createResolver())
				.andFunctionResolver(MathFunctions.createResolver())
				.andFunctionResolver(RandomFunctions.createResolver())
				.andFunctionResolver(RegexFunctions.createResolver())
				.andFunctionResolver(FileSystemFunctions.createResolver())
				.andFunctionResolver(DateFunctions.createResolver())
				.andFunctionResolver(IOFunctions.createResolver())
				.andFunctionResolver(ZipFunctions.createResolver())
				.andFunctionResolver(DigestFunctions.createResolver())
			.withScriptStack(16, 512)
			.withScope("script", new DefaultVariableResolver())
			.createInstance();
		
		//doDisassemble(instance);
		doIterations(instance, 1);
	}
	
}

