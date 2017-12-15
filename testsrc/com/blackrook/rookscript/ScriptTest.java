package com.blackrook.rookscript;

import java.io.File;
import java.io.OutputStreamWriter;

import com.blackrook.commons.Common;
import com.blackrook.rookscript.Script;
import com.blackrook.rookscript.ScriptCommonFunctions;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.compiler.ScriptAssembler;
import com.blackrook.rookscript.compiler.ScriptReader;
import com.blackrook.rookscript.struct.ScriptInstanceStack;

public class ScriptTest
{
	public static void main(String[] args) throws Exception
	{
		String fileName;
		if ((fileName = Common.arrayElement(args, 0)) == null)
		{
			System.out.println("ERROR: No file name for script.");
			System.out.println("Usage: command [filename] [activationStackDepth] [valueStackDepth]");
			System.out.println("    activationStackDepth: (optional) Set activation stack depth (default: 16).");
			System.out.println("         valueStackDepth: (optional) Set value stack depth (default: 512).");
			System.out.println();
			return;
		}
		
		Script script = ScriptReader.read(new File(fileName), ScriptCommonFunctions.getResolver());
		script = ScriptAssembler.optimize(script);
		ScriptAssembler.disassemble(script, new OutputStreamWriter(System.out));

		ScriptInstanceStack stack = new ScriptInstanceStack(
			Common.parseInt(Common.arrayElement(args, 1), 16), 
			Common.parseInt(Common.arrayElement(args, 2), 512)
		);
		ScriptInstance instance = new ScriptInstance(script, stack, null, null);
		
		for (int i = 0; i < 10000; i++)
		{
			long nanos = System.nanoTime();
			instance.initialize();
			instance.update();
			System.out.println((System.nanoTime() - nanos) + "ns");
		}

	}
	
}

