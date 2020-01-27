/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions.common;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

/**
 * RookScript buffer functions.
 * @author Matthew Tropiano
 */
public enum MiscFunctions implements ScriptFunctionType
{	
	TYPEOF(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the type name of a value."
				)
				.parameter("value", 
					type("The provided value.")
				)
				.returns(
					type(Type.STRING, 
						"The type name. Can be \"null\", \"boolean\", \"integer\", " +
						"\"float\", \"string\", \"list\", \"map\", \"buffer\", \"error\", or an \"objectref\" string."
					)
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(arg1);
				returnValue.set(arg1.getTypeName());
				return true;
			}
			finally
			{
				arg1.setNull();
			}
		}
	},

	LENGTH(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the \"length\" of a value."
				)
				.parameter("value", 
					type("The value.")
				)
				.returns(
					type(Type.INTEGER, 
						"If value is: STRING, the length in characters. LIST, the length in elements." +
						"MAP, the amount of keys. BUFFER, the size in bytes. OBJECTREF, if Collection, returns size(). Others, 1."
					)
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(arg1);
				returnValue.set(arg1.length());
				return true;
			}
			finally
			{
				arg1.setNull();
			}
		}
	},

	EMPTY(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns if a value is \"empty\"."
				)
				.parameter("value", 
					type("The value.")
				)
				.returns(
					type(Type.INTEGER,
						"Returns true if: NULL." +
						"OBJECTREF: is a Collection and isEmpty() returns true. "+
						"BOOLEAN: is false. "+
						"INTEGER or FLOAT: is 0 or NaN. "+
						"STRING: length = 0. "+
						"BUFFER: length = 0. "+
						"LIST: length = 0. "+
						"MAP: length = 0."
					)
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(arg1);
				returnValue.set(arg1.empty());
				return true;
			}
			finally
			{
				arg1.setNull();
			}
		}
	},

	;
	
	private final int parameterCount;
	private Usage usage;
	private MiscFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(MiscFunctions.values());
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
	
	protected abstract Usage usage();

	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}