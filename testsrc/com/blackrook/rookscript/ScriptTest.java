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

import com.blackrook.commons.util.ArrayUtils;
import com.blackrook.commons.util.ValueUtils;
import com.blackrook.rookscript.Script;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.compiler.ScriptReader;
import com.blackrook.rookscript.functions.MathFunctions;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.StandardIOFunctions;
import com.blackrook.rookscript.resolver.MultiResolver;
import com.blackrook.rookscript.scope.ScriptVariableScope;
import com.blackrook.rookscript.struct.ScriptInstanceStack;

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
		
		Script script = ScriptReader.read(new File(fileName), new MultiResolver(
			CommonFunctions.getResolver(), 
			StandardIOFunctions.getResolver(),
			MathFunctions.getResolver()
		));
		ScriptAssembler.disassemble(script, new OutputStreamWriter(System.out));

		ScriptInstanceStack stack = new ScriptInstanceStack(
			ValueUtils.parseInt(ArrayUtils.arrayElement(args, 1), 16), 
			ValueUtils.parseInt(ArrayUtils.arrayElement(args, 2), 512)
		);
		
		final ScriptVariableScope scriptScope = new ScriptVariableScope();
		ScriptScopeResolver ssr = new ScriptScopeResolver()
		{
			@Override
			public boolean isReadOnly(String name)
			{
				return false;
			}
			
			@Override
			public ScriptVariableResolver getScope(String name)
			{
				if (name.equalsIgnoreCase("script"))
					return scriptScope;
				return null;
			}
			
			@Override
			public boolean containsScope(String name)
			{
				return name.equalsIgnoreCase("script");
			}
		};
		
		ScriptInstance instance = new ScriptInstance(script, stack, ssr, null);
		instance.call("main");
		System.out.println("Script returns: "+instance.popStackValue());
	}
	
}

