package com.blackrook.rookscript;

import java.io.File;
import java.io.OutputStreamWriter;

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
		Script script = ScriptReader.read(new File(args[0]), ScriptCommonFunctions.getResolver());
		ScriptAssembler.disassemble(script, new OutputStreamWriter(System.out));

		ScriptInstanceStack stack = new ScriptInstanceStack(16, 512);
		ScriptInstance instance = new ScriptInstance(script, stack, null, null);
		
		for (int i = 0; i < 10000; i++)
		{
			instance.initialize("0", i);
			instance.update();
		}
	}
}

