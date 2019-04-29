/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.io.File;
import java.io.OutputStreamWriter;

import com.blackrook.commons.math.Pair;
import com.blackrook.commons.util.ArrayUtils;
import com.blackrook.commons.util.ValueUtils;
import com.blackrook.rookscript.Script;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.compiler.ScriptReader;
import com.blackrook.rookscript.functions.MathFunctions;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.StandardIOFunctions;
import com.blackrook.rookscript.resolvers.function.MultiFunctionResolver;
import com.blackrook.rookscript.resolvers.scope.DefaultScopeResolver;
import com.blackrook.rookscript.resolvers.variable.ObjectVariableResolver;

public class ScriptTest
{
	public static void main(String[] args) throws Exception
	{
		String fileName;
		if ((fileName = ArrayUtils.arrayElement(args, 0)) == null)
		{
			System.out.println("ERROR: No file name for script.");
			System.out.println("Usage: command [filename] [activationStackDepth] [valueStackDepth]");
			System.out.println("    activationStackDepth: (optional) Set activation stack depth (default: 16).");
			System.out.println("         valueStackDepth: (optional) Set value stack depth (default: 512).");
			System.out.println();
			return;
		}
		
		Script script = ScriptReader.read(new File(fileName), new MultiFunctionResolver(
			CommonFunctions.getResolver(), 
			StandardIOFunctions.getResolver(),
			MathFunctions.getResolver()
		));
		ScriptAssembler.disassemble(script, new OutputStreamWriter(System.out));

		ScriptInstanceStack stack = new ScriptInstanceStack(
			ValueUtils.parseInt(ArrayUtils.arrayElement(args, 1), 16), 
			ValueUtils.parseInt(ArrayUtils.arrayElement(args, 2), 512)
		);
		
		DefaultScopeResolver dsr = new DefaultScopeResolver();
		dsr.addScope("script", new ObjectVariableResolver<Pair>(new Pair()));
		
		ScriptInstance instance = new ScriptInstance(script, stack, dsr, null);
		instance.call("main");
		System.out.println("Script returns: "+instance.popStackValue());
	}
	
}

