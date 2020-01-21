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
 * RookScript string functions.
 * @author Matthew Tropiano
 */
public enum StringFunctions implements ScriptFunctionType
{	
	STRUPPER(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a string in full uppercase."
				)
				.parameter("string", 
					type("The string (if not STRING, will be converted).")
				)
				.returns(
					type(Type.STRING, "The same string converted to uppercase.")
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
				returnValue.set(arg1.asString().toUpperCase());
				return true;
			}
			finally
			{
				arg1.setNull();
			}
		}
	},
	
	STRLOWER(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a string in full lowercase."
				)
				.parameter("string", 
					type(Type.STRING, "The string (if not STRING, will be converted).")
				)
				.returns(
					type(Type.STRING, "The same string converted to lowercase.")
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
				returnValue.set(arg1.asString().toLowerCase());
				return true;
			}
			finally
			{
				arg1.setNull();
			}
		}
	},
	
	STRTRIM(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a string trimmed of whitespace at both ends."
				)
				.parameter("string", 
					type(Type.STRING, "The string (if not STRING, will be converted).")
				)
				.returns(
					type(Type.STRING, "The trimmed string.")
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
				returnValue.set(arg1.asString().trim());
				return true;
			}
			finally
			{
				arg1.setNull();
			}
		}
	},
	
	STRCHAR(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a single character from a string."
				)
				.parameter("string", 
					type(Type.STRING, "The string (if not STRING, will be converted).")
				)
				.parameter("index", 
					type(Type.INTEGER, "The index (0-based).")
				)
				.returns(
					type(Type.NULL, "If the index is out-of-bounds."),
					type(Type.STRING, "The character returned.")
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
				int value = temp.asInt();
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				if (value < 0 || value >= str.length())
					returnValue.setNull();
				else
					returnValue.set(String.valueOf(str.charAt(value)));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SUBSTR(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a substring of another string."
				)
				.parameter("string", 
					type(Type.STRING, "The string (if not STRING, will be converted).")
				)
				.parameter("start", 
						type(Type.INTEGER, "The starting index (0-based), inclusive.")
				)
				.parameter("end", 
						type(Type.INTEGER, "The ending index (0-based), exclusive.")
				)
				.returns(
					type(Type.NULL, "If either index is out-of-bounds or the end index is less than the start index."),
					type(Type.STRING, "The substring returned.")
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
				int endIndex = temp.asInt();
				scriptInstance.popStackValue(temp);
				int startIndex = temp.asInt();
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				int length = str.length();
				if (startIndex < 0 || startIndex >= length)
					returnValue.setNull();
				else if (endIndex < 0 && endIndex > length)
					returnValue.setNull();
				else if (endIndex < startIndex)
					returnValue.setNull();
				else
					returnValue.set(str.substring(startIndex, endIndex));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	STRINDEX(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the starting index of a string inside another string."
				)
				.parameter("string", 
					type(Type.STRING, "The string (if not STRING, will be converted).")
				)
				.parameter("search", 
					type(Type.STRING, "The string to search for (if not STRING, will be converted).")
				)
				.returns(
					type(Type.NULL, "If not found."),
					type(Type.INTEGER, "The starting index.")
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
				String targetStr = temp.asString();
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				int out;
				if ((out = str.indexOf(targetStr)) >= 0)
					returnValue.set(out);
				else
					returnValue.setNull();
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	STRLASTINDEX(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the starting index of a string inside another string, searching from the end."
				)
				.parameter("string", 
					type(Type.STRING, "The string (if not STRING, will be converted).")
				)
				.parameter("search", 
					type(Type.STRING, "The string to search for (if not STRING, will be converted).")
				)
				.returns(
					type(Type.NULL, "If not found."),
					type(Type.INTEGER, "The starting index.")
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
				String targetStr = temp.asString();
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				
				int out;
				if ((out = str.lastIndexOf(targetStr)) >= 0)
					returnValue.set(out);
				else
					returnValue.setNull();
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
	private StringFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(StringFunctions.values());
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
