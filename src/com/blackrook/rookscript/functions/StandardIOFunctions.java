/*******************************************************************************
 * Copyright (c) 2017-2018 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptFunctionResolver;
import com.blackrook.rookscript.ScriptFunctionType;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.resolver.EnumResolver;
import com.blackrook.rookscript.struct.ScriptValue;

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
	STDOUT(true, 1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			System.out.println(arg.asString());
			return true;
		}
	},

	/**
	 * Prints something to STDERR.
	 * Returns void.
	 * ARG: Value to print.
	 */
	STDERR(true, 1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			System.err.println(arg.asString());
			return true;
		}
	},

	;
	
	private final boolean isVoid;
	private final int parameterCount;
	private StandardIOFunctions(int parameterCount)
	{
		this(false, parameterCount);
	}
	
	private StandardIOFunctions(boolean isVoid, int parameterCount)
	{
		this.isVoid = isVoid;
		this.parameterCount = parameterCount;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver getResolver()
	{
		return new EnumResolver(StandardIOFunctions.values());
	}

	@Override
	public boolean isVoid()
	{
		return isVoid;
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
