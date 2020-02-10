/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

/**
 * Script common functions for standard input/output.
 * @author Matthew Tropiano
 */
public enum PrintFunctions implements ScriptFunctionType
{
	PRINT(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Prints something to standard out."
				)
				.parameter("message", 
					type(Type.STRING, "Value to print.")
				)
				.returns(
					type(Type.NULL, "Returns nothing.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				scriptInstance.getEnvironment().print(temp.asString());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	PRINTLN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Prints something to standard out, appending a newline."
				)
				.parameter("message", 
					type(Type.STRING, "Value to print.")
				)
				.returns(
					type(Type.NULL, "Returns nothing.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				scriptInstance.getEnvironment().print(temp.asString());
				scriptInstance.getEnvironment().print('\n');
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	PRINTERR(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Prints something to standard error."
				)
				.parameter("message", 
					type(Type.STRING, "Value to print.")
				)
				.returns(
					type(Type.NULL, "Returns nothing.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				scriptInstance.getEnvironment().printErr(temp.asString());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	PRINTERRLN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Prints something to standard error, appending a newline."
				)
				.parameter("message", 
					type(Type.STRING, "Value to print.")
				)
				.returns(
					type(Type.NULL, "Returns nothing.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				scriptInstance.getEnvironment().printErr(temp.asString());
				scriptInstance.getEnvironment().printErr('\n');
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	;
	
	private final int parameterCount;
	private Usage usage;
	private PrintFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(PrintFunctions.values());
	}

	@Override
	public int getParameterCount()
	{
		return parameterCount;
	}

	@Override
	public Usage getUsage()
	{
		if (usage == null)
			usage = usage();
		return usage;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
