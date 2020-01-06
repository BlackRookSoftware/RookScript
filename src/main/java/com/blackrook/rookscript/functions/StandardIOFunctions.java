/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

/**
 * Script common functions for standard input/output.
 * @author Matthew Tropiano
 */
public enum StandardIOFunctions implements ScriptFunctionType
{
	/**
	 * Prints something to STDOUT.
	 * Returns void.
	 * ARG: Value to print.
	 */
	PRINT(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			scriptInstance.getEnvironment().print(arg.asString());
			scriptInstance.pushStackValue(null);
			return true;
		}
	},

	/**
	 * Prints something to STDERR.
	 * Returns void.
	 * ARG: Value to print.
	 */
	PRINTERR(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			scriptInstance.getEnvironment().printErr(arg.asString());
			scriptInstance.pushStackValue(null);
			return true;
		}
	},

	/**
	 * Prints something to STDOUT, appending a newline.
	 * Returns void.
	 * ARG: Value to print.
	 */
	PRINTLN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			scriptInstance.getEnvironment().print(arg.asString());
			scriptInstance.getEnvironment().print('\n');
			scriptInstance.pushStackValue(null);
			return true;
		}
	},

	/**
	 * Prints something to STDERR, appending a newline.
	 * Returns void.
	 * ARG: Value to print.
	 */
	PRINTLNERR(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			scriptInstance.getEnvironment().printErr(arg.asString());
			scriptInstance.getEnvironment().printErr('\n');
			scriptInstance.pushStackValue(null);
			return true;
		}
	},

	;
	
	private final int parameterCount;
	private StandardIOFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(StandardIOFunctions.values());
	}

	@Override
	public int getParameterCount()
	{
		return parameterCount;
	}

	@Override
	public Usage getUsage()
	{
		return null;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance);

	// wraps a single value into a list.
	protected ScriptValue wrapList(ScriptValue sv)
	{
		ScriptValue out = ScriptValue.createEmptyList();
		out.listAdd(sv);
		return out;
	}
	
}
