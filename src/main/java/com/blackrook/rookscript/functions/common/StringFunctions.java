/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
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
					"Returns a string trimmed of whitespace (0x00 - 0x20, ASCII) at both ends."
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
					type(Type.NULL, "Use string length."),
					type(Type.INTEGER, "The ending index (0-based), exclusive. If negative, stop at that many characters from the end.")
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
				Integer endIndex = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				int startIndex = temp.asInt();
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				
				int length = str.length();
				if (endIndex == null)
					endIndex = length;
				else if (endIndex < 0)
					endIndex = length + endIndex;
				
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
		
	STRJOIN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Joins a list of strings together into one string."
				)
				.parameter("strings", 
					type(Type.LIST, "[STRING, ...]", "The list of items to convert to strings and join."),
					type("Returns this as a string.")
				)
				.parameter("joiner", 
					type(Type.NULL, "Use the empty string."),
					type(Type.STRING, "The joiner string to use between list items.")
				)
				.returns(
					type(Type.STRING, "The resultant string after the join.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue list = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				String joiner = temp.isNull() ? "" : temp.asString();
				scriptInstance.popStackValue(list);

				if (!list.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(list);
					list.set(temp);
				}
				
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < list.length(); i++)
				{
					list.listGetByIndex(i, temp);
					sb.append(temp.asString());
					if (i < list.length() - 1)
						sb.append(joiner);
				}

				returnValue.set(sb.toString());
				return true;
			}
			finally
			{
				temp.setNull();
				list.setNull();
			}
		}
	},
	
	STRSTARTSWITH(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a string starts with another substring (case sensitive)."
				)
				.parameter("string", 
					type(Type.STRING, "The string to test (converted to string if not a string).")
				)
				.parameter("prefix", 
					type(Type.STRING, "The substring to test with (converted to string if not a string).")
				)
				.parameter("offset", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "The starting offset in characters from the beginning to test from.")
				)
				.returns(
					type(Type.BOOLEAN, "True, if [string] (after the first [offset] characters) starts with [prefix], false otherwise.")
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
				int offset = temp.asInt();
				scriptInstance.popStackValue(temp);
				String substr = temp.asString();
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				
				returnValue.set(str.startsWith(substr, offset));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	STRENDSWITH(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a string ends with another substring (case sensitive)."
				)
				.parameter("string", 
					type(Type.STRING, "The string to test (converted to string if not a string).")
				)
				.parameter("suffix", 
					type(Type.STRING, "The substring to test with (converted to string if not a string).")
				)
				.returns(
					type(Type.BOOLEAN, "True, if [string] ends with [suffix], false otherwise.")
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
				String substr = temp.asString();
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				returnValue.set(str.endsWith(substr));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/** @since 1.5.0 */
	STRREPLACEALL(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Replaces all occurrances of a string in a string with another string."
				)
				.parameter("string", 
					type(Type.STRING, "The string to perform replacement on (converted to string if not a string).")
				)
				.parameter("target", 
					type(Type.STRING, "The target substring to replace (converted to string if not a string).")
				)
				.parameter("replacement", 
					type(Type.STRING, "The replacement string (converted to string if not a string).")
				)
				.returns(
					type(Type.STRING, "The resultant string, after replacement.")
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
				String replacement = temp.asString();
				scriptInstance.popStackValue(temp);
				String target = temp.asString();
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				returnValue.set(str.replace(target, replacement));
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
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
