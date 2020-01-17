/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import java.nio.ByteOrder;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.ScriptValue.BufferType;
import com.blackrook.rookscript.ScriptValue.ErrorType;
import com.blackrook.rookscript.ScriptValue.MapType;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.resolvers.variable.AbstractVariableResolver.Entry;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

/**
 * Script common functions that work for all scripts.
 * @author Matthew Tropiano
 */
public enum CommonFunctions implements ScriptFunctionType
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
						"\"float\", \"string\", \"list\", \"map\", \"error\", or an \"objectref\" string."
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

	ISERROR(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if the provided value is an error type."
				)
				.parameter("value", 
					type("The provided value.")
				)
				.returns(
					type(Type.BOOLEAN, "True if so, false if not.")
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
				returnValue.set(arg1.isError());
				return true;
			}
			finally
			{
				arg1.setNull();
			}
		}
	},
	
	ERROR(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates an error type."
				)
				.parameter("type", 
					type(Type.STRING, "The error type.")
				)
				.parameter("message", 
					type(Type.STRING, "The error message.")
				)
				.parameter("messageLocalized", 
					type(Type.NULL, "Use the error message."),
					type(Type.STRING, "The error localized message.")
				)
				.returns(
					type(Type.ERROR, "The created error.")
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
				String messageLocalized = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				String message = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				String type = temp.isNull() ? null : temp.asString();
				
				if (messageLocalized == null)
					returnValue.setError(type, message);
				else
					returnValue.setError(type, message, messageLocalized);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	ERRORTYPE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the error type. If not an error, this returns null."
				)
				.parameter("error", 
					type(Type.ERROR, "The error.")
				)
				.returns(
					type(Type.NULL, "If not an error."),
					type(Type.STRING, "The error type.")
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
				if (!arg1.isError())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					returnValue.set(arg1.asObjectType(ErrorType.class).getType());
					return true;
				}
			}
			finally
			{
				arg1.setNull();
			}
		}
	},

	ERRORMSG(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the error message. If not an error, this returns null."
				)
				.parameter("error", 
					type(Type.ERROR, "The error.")
				)
				.returns(
					type(Type.NULL, "If not an error."),
					type(Type.STRING, "The error message.")
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
				if (!arg1.isError())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					returnValue.set(arg1.asObjectType(ErrorType.class).getMessage());
					return true;
				}
			}
			finally
			{
				arg1.setNull();
			}
		}
	},

	ERRORLOCALMSG(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the localized error message. If not an error, this returns null."
				)
				.parameter("error", 
					type(Type.ERROR, "The error.")
				)
				.returns(
					type(Type.NULL, "If not an error."),
					type(Type.STRING, "The localized error message.")
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
				if (!arg1.isError())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					returnValue.set(arg1.asObjectType(ErrorType.class).getLocalizedMessage());
					return true;
				}
			}
			finally
			{
				arg1.setNull();
			}
		}
	},

	ERRORMAP(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns an error type as a map. If not an error, this returns null."
				)
				.parameter("error", 
					type(Type.ERROR, "The error.")
				)
				.returns(
					type(Type.NULL, "If not an error."),
					type(Type.MAP, "A map of {type:STRING, message:STRING, localizedmessage:STRING}.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(arg1);
				if (!arg1.isError())
				{
					returnValue.set(false);
					return true;
				}
				else
				{
					ErrorType error = arg1.asObjectType(ErrorType.class);
					temp.setEmptyMap();
					temp.mapSet("type", error.getType());
					temp.mapSet("message", error.getMessage());
					temp.mapSet("localizedmessage", error.getLocalizedMessage());
					returnValue.set(temp);
					return true;
				}
			}
			finally
			{
				arg1.setNull();
				temp.setNull();
			}
		}
	},

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
	
	/**
	 * Creates a new list.
	 * Copies an existing list, or encapsulates a value as a list. 
	 * ARG1: The value to copy (and re-encapsulate in a list). 
	 */
	LIST(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new list by copying an existing list into a new " +
					"reference, or encapsulating a non-list value as a list."
				)
				.parameter("list", 
					type("The list to copy.")
				)
				.returns(
					type(Type.LIST, "The resultant list.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue newlist = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value);
				if (value.isList())
				{
					newlist.setEmptyList(value.length());
					for (int i = 0; i < value.length(); i++)
					{
						value.listGetByIndex(i, temp);
						newlist.listSetByIndex(i, temp);
					}
					returnValue.set(newlist);
				}
				else
				{
					newlist.setEmptyList(4);
					newlist.listSetByIndex(0, value);
					returnValue.set(newlist);
				}
				return true;
			}
			finally
			{
				value.setNull();
				newlist.setNull();
				temp.setNull();
			}
		}
	}, 
	
	LISTNEW(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new list of a specific length, optionally with all values initialized to a specified value."
				)
				.parameter("length", 
					type(Type.INTEGER, "The new list length.")
				)
				.parameter("value", 
					type("The fill value. Copies are not made for each element.")
				)
				.returns(
					type(Type.LIST, "The resultant new list.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue value = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				scriptInstance.popStackValue(temp);
				int length = temp.asInt();
				
				temp.setEmptyList(length, length);
				if (!value.isNull()) for (int i = 0; i < length; i++)
					temp.listSetByIndex(i, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
				value.setNull();
			}
		}
	}, 
	
	LISTADD(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Adds a value to a list. If the \"list\" argument is not a list or not added, this returns false, else true."
				)
				.parameter("list", 
					type(Type.LIST, "The list.")
				)
				.parameter("value", 
					type("The value to add.")
				)
				.parameter("index", 
					type(Type.NULL, "Adds it to the end."),
					type(Type.INTEGER, "The index to add the value at (shifts the others).")
				)
				.returns(
					type(Type.BOOLEAN, "True if added. False if not a list.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue item = CACHEVALUE1.get();
			ScriptValue list = CACHEVALUE2.get();
			ScriptValue index = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(index);
				scriptInstance.popStackValue(item);
				scriptInstance.popStackValue(list);
				if (index.isNull())
					returnValue.set(list.listAdd(item));
				else
					returnValue.set(list.listAddAt(index.asInt(), item));
				return true;
			}
			finally
			{
				item.setNull();
				list.setNull();
				index.setNull();
			}
		}
	},
	
	LISTREMOVE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Removes the first value from a list that matches the item. " +
					"Finds list/map-typed items by reference and objects by equals()."
				)
				.parameter("list", 
					type(Type.LIST, "The list.")
				)
				.parameter("value", 
					type("The value to remove.")
				)
				.returns(
					type(Type.BOOLEAN, "True if removed. False if not found or not a list.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue item = CACHEVALUE1.get();
			ScriptValue list = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(item);
				scriptInstance.popStackValue(list);
				returnValue.set(list.listRemove(item));
				return true;
			}
			finally
			{
				item.setNull();
				list.setNull();
			}
		}
	},
	
	LISTREMOVEAT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Removes the first value from a list at a specific index."
				)
				.parameter("list", 
					type(Type.LIST, "The list.")
				)
				.parameter("index", 
					type(Type.INTEGER, "The index.")
				)
				.returns(
					type("The value removed, NULL if index is out-of-bounds or a list was not provided.")
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
				int index = temp.asInt();
				scriptInstance.popStackValue(list);
				list.listRemoveAt(index, temp);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
				list.setNull();
			}
		}
	},
	
	LISTSORT(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sorts a list in place."
				)
				.parameter("list", 
					type(Type.LIST, "The list to sort.")
				)
				.returns(
					type(Type.LIST, "The list that was sorted (not a new copy).")
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
				temp.sort();
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	LISTCONTAINS(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a list contains a specific value, sequential search. Finds " +
					"list/map-typed items by reference and objects by equals()."
				)
				.parameter("list", 
					type(Type.LIST, "The list to search.")
				)
				.parameter("value", 
					type("The value to search for.")
				)
				.returns(
					type(Type.BOOLEAN, "True if it contains the value, false if not.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue item = CACHEVALUE1.get();
			ScriptValue list = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(item);
				scriptInstance.popStackValue(list);
				returnValue.set(list.listContains(item));
				return true;
			}
			finally
			{
				item.setNull();
				list.setNull();
			}
		}
	},
	
	LISTINDEX(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the index of the first occurrence of a value in a list. " +
					"Finds list/map-typed items by reference and objects by equals()."
				)
				.parameter("list", 
					type(Type.LIST, "The list to search.")
				)
				.parameter("value", 
					type("The value to search for.")
				)
				.returns(
					type(Type.NULL, "If not found."),
					type(Type.INTEGER, "The index of the found element.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue item = CACHEVALUE1.get();
			ScriptValue list = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(item);
				scriptInstance.popStackValue(list);
				
				int out;
				if ((out = list.listGetIndexOf(item)) >= 0)
					returnValue.set(out);
				else
					returnValue.setNull();
				return true;
			}
			finally
			{
				item.setNull();
				list.setNull();
			}
		}
	},
	
	LISTLASTINDEX(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the index of the first occurrence of a value in a list. " +
					"Finds list/map-typed items by reference and objects by equals()."
				)
				.parameter("list", 
					type(Type.LIST, "The list to search.")
				)
				.parameter("value", 
					type("The value to search for.")
				)
				.returns(
					type(Type.NULL, "If not found."),
					type(Type.INTEGER, "The index of the found element.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue item = CACHEVALUE1.get();
			ScriptValue list = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(item);
				scriptInstance.popStackValue(list);
				
				int out;
				if ((out = list.listGetLastIndexOf(item)) >= 0)
					returnValue.set(out);
				else
					returnValue.setNull();
				return true;
			}
			finally
			{
				item.setNull();
				list.setNull();
			}
		}
	},
	
	SET(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new list from another list, such that the contents of the " +
					"list are discrete and sorted, its contents now suitable for set operations."
				)
				.parameter("list", 
					type(Type.LIST, "The list to prepare.")
				)
				.returns(
					type(Type.LIST, "The new list, set-ified.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue newset = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value);
				newset.setEmptyList();
				if (value.isList())
				{
					for (int i = 0; i < value.length(); i++)
					{
						value.listGetByIndex(i, temp);
						newset.setAdd(temp);
					}
				}
				else
				{
					newset.setAdd(value);
				}
				
				returnValue.set(newset);
				return true;
			}
			finally
			{
				value.setNull();
				newset.setNull();
			}
		}
	},
	
	SETADD(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Adds a value to a list, expected to be set up like a set (sorted, discrete)."
				)
				.parameter("list", 
					type(Type.LIST, "The set (list).")
				)
				.parameter("value", 
					type("The value to add.")
				)
				.returns(
					type(Type.BOOLEAN, "True if value was added (and is not already in the list), false otherwise.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue set = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				scriptInstance.popStackValue(set);
				returnValue.set(set.setAdd(value));
				return true;
			}
			finally
			{
				value.setNull();
				set.setNull();
			}
		}
	},
	
	SETREMOVE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Removes a value from a list, expected to be set up like a set (sorted, discrete)."
				)
				.parameter("list", 
					type(Type.LIST, "The set (list).")
				)
				.parameter("value", 
					type("The value to remove.")
				)
				.returns(
					type(Type.BOOLEAN, "True if the value was removed, false otherwise.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue set = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				scriptInstance.popStackValue(set);
				returnValue.set(set.setRemove(value));
				return true;
			}
			finally
			{
				value.setNull();
				set.setNull();
			}
		}
	},
	
	SETCONTAINS(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a value exists in a list, expected to be set up like a set (sorted, discrete). " +
					"This is more performant than a list - search is binary search."
				)
				.parameter("list", 
					type(Type.LIST, "The set (list).")
				)
				.parameter("value", 
					type("The value to look for.")
				)
				.returns(
					type(Type.BOOLEAN, "True if the value was found, false otherwise.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue set = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				scriptInstance.popStackValue(set);
				returnValue.set(set.setContains(value));
				return true;
			}
			finally
			{
				value.setNull();
				set.setNull();
			}
		}
	},
	
	SETSEARCH(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the index of a value in a list, expected to be set up like a set (sorted, discrete). " +
					"This is more performant than a list - search is binary search."
				)
				.parameter("list", 
					type(Type.LIST, "The set (list).")
				)
				.parameter("value", 
					type("The value to look for.")
				)
				.returns(
					type(Type.NULL, "If not found."),
					type(Type.INTEGER, "The index in the list that it was found.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue set = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				scriptInstance.popStackValue(set);
				returnValue.set(set.setSearch(value));
				return true;
			}
			finally
			{
				value.setNull();
				set.setNull();
			}
		}
	},
	
	SETUNION(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the union of two sets, returning a new set with values in both."
				)
				.parameter("list1", 
					type(Type.LIST, "The first set (list)."),
					type("A value to encapsulate into a set.")
				)
				.parameter("list2", 
					type(Type.LIST, "The second set (list)."),
					type("A value to encapsulate into a set.")
				)
				.returns(
					type(Type.LIST, "The new set that is the union of both sets.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue set2 = CACHEVALUE1.get();
			ScriptValue set1 = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			ScriptValue temp = CACHEVALUE4.get();
			try
			{
				scriptInstance.popStackValue(set2);
				scriptInstance.popStackValue(set1);
				out.setEmptyList();
				
				if (!set1.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(set1);
					set1.set(temp);
				}
				
				if (!set2.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(set2);
					set2.set(temp);
				}

				for (int i = 0; i < set1.length(); i++)
				{
					set1.listGetByIndex(i, temp);
					out.setAdd(temp);
				}
				for (int i = 0; i < set2.length(); i++)
				{
					set2.listGetByIndex(i, temp);
					out.setAdd(temp);
				}
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				set2.setNull();
				set1.setNull();
				out.setNull();
				temp.setNull();
			}
		}
	},
	
	SETINTERSECT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the intersection of two sets, returning a new set with values in both."
				)
				.parameter("list1", 
					type(Type.LIST, "The first set (list)."),
					type("A value to encapsulate into a set.")
				)
				.parameter("list2", 
					type(Type.LIST, "The second set (list)."),
					type("A value to encapsulate into a set.")
				)
				.returns(
					type(Type.LIST, "The new set that is the intersection of both sets.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue set2 = CACHEVALUE1.get();
			ScriptValue set1 = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			ScriptValue temp = CACHEVALUE4.get();
			try
			{
				scriptInstance.popStackValue(set2);
				scriptInstance.popStackValue(set1);
				out.setEmptyList();
				
				if (!set1.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(set1);
					set1.set(temp);
				}
				
				if (!set2.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(set2);
					set2.set(temp);
				}

				ScriptValue smallest = set1.length() < set2.length() ? set1 : set2;
				ScriptValue largest = smallest == set1 ? set2 : set1;
				
				for (int i = 0; i < smallest.length(); i++)
				{
					smallest.listGetByIndex(i, temp);
					if (largest.setContains(temp))
						out.setAdd(temp);
				}
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				set2.setNull();
				set1.setNull();
				out.setNull();
				temp.setNull();
			}
		}
	},
	
	SETXOR(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the exclusive-or of two sets, returning the union of both sets minus the intersection."
				)
				.parameter("list1", 
					type(Type.LIST, "The first set (list)."),
					type("A value to encapsulate into a set.")
				)
				.parameter("list2", 
					type(Type.LIST, "The second set (list)."),
					type("A value to encapsulate into a set.")
				)
				.returns(
					type(Type.LIST, "The new set that is the XOr of both sets.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue set2 = CACHEVALUE1.get();
			ScriptValue set1 = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			ScriptValue temp = CACHEVALUE4.get();
			try
			{
				scriptInstance.popStackValue(set2);
				scriptInstance.popStackValue(set1);
				out.setEmptyList();

				if (!set1.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(set1);
					set1.set(temp);
				}
				
				if (!set2.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(set2);
					set2.set(temp);
				}

				for (int i = 0; i < set1.length(); i++)
				{
					set1.listGetByIndex(i, temp);
					out.listAdd(temp);
				}
				
				for (int i = 0; i < set2.length(); i++)
				{
					set2.listGetByIndex(i, temp);
					if (out.setContains(temp))
						out.setRemove(temp);
					else
						out.setAdd(temp);
				}
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				set2.setNull();
				set1.setNull();
				out.setNull();
				temp.setNull();
			}
		}
	},
	
	SETDIFF(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a new set that is the first set minus the values in the second set."
				)
				.parameter("list1", 
					type(Type.LIST, "The first set (list)."),
					type("A value to encapsulate into a set.")
				)
				.parameter("list2", 
					type(Type.LIST, "The second set (list)."),
					type("A value to encapsulate into a set.")
				)
				.returns(
					type(Type.LIST, "The new set that is the difference of both sets.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue set2 = CACHEVALUE1.get();
			ScriptValue set1 = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			ScriptValue temp = CACHEVALUE4.get();
			try
			{
				scriptInstance.popStackValue(set2);
				scriptInstance.popStackValue(set1);
				out.setEmptyList();

				if (!set1.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(set1);
					set1.set(temp);
				}
				
				if (!set2.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(set2);
					set2.set(temp);
				}

				for (int i = 0; i < set1.length(); i++)
				{
					set1.listGetByIndex(i, temp);
					out.listAdd(temp);
				}
				
				for (int i = 0; i < set2.length(); i++)
				{
					set2.listGetByIndex(i, temp);
					out.setRemove(temp);
				}
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				set2.setNull();
				set1.setNull();
				out.setNull();
				temp.setNull();
			}
		}
	},
	
	MAPKEYS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a list of all of the keys in a map. The returned list " +
					"is suitable for set operations (sorted, discrete)."
				)
				.parameter("map", 
					type(Type.MAP, "The map.")
				)
				.returns(
					type(Type.NULL, "If not a map."),
					type(Type.LIST, "[STRING, ...]", "A new list of the map's keys.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue map = CACHEVALUE1.get();
			ScriptValue out = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(map);
				if (!map.isMap())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					out.setEmptyList();
					for (Entry e : map.asObjectType(MapType.class))
						out.setAdd(e.getName());
					returnValue.set(out);
					return true;
				}
			}
			finally
			{
				map.setNull();
				out.setNull();
			}
		}
	},
	
	MAPVALUE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a value that corresponds to a key in the map."
				)
				.parameter("map", 
					type(Type.MAP, "The map.")
				)
				.parameter("key", 
					type(Type.STRING, "The key.")
				)
				.returns(
					type(Type.NULL, "If not a map."),
					type(Type.LIST, "[STRING, ...]", "A new list of the map's keys.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue keyValue = CACHEVALUE1.get();
			ScriptValue map = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			try 
			{
				scriptInstance.popStackValue(keyValue);
				scriptInstance.popStackValue(map);

				if (!map.isMap())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					map.mapGet(keyValue.asString(), out);
					returnValue.set(out);
					return true;
				}
			}
			finally
			{
				keyValue.setNull();
				map.setNull();
				out.setNull();
			}
		}
	},
	
	MAPMERGE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a new map that is the result of taking the first map and adding all " + 
					"of the keys of the second, replacing the keys that exist in the first. " + 
					"The copies are shallow - references are preserved."
				)
				.parameter("map1", 
					type(Type.MAP, "The first map."),
					type("An empty map.")
				)
				.parameter("map2", 
					type(Type.MAP, "The second map."),
					type("An empty map.")
				)
				.returns(
					type(Type.MAP, "A new map.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue map2 = CACHEVALUE1.get();
			ScriptValue map1 = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(map2);
				scriptInstance.popStackValue(map1);
				out.setEmptyMap();
				
				if (!map1.isMap())
				{
					returnValue.set(out);
					return true;
				}
				
				for (Entry e : map1.asObjectType(MapType.class))
					out.mapSet(e.getName(), e.getValue());
				
				if (!map2.isMap())
				{
					returnValue.set(out);
					return true;
				}

				for (Entry e : map2.asObjectType(MapType.class))
					out.mapSet(e.getName(), e.getValue());
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				map2.setNull();
				map1.setNull();
				out.setNull();
			}
		}
	},
	
	BUFNEW(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new, blank buffer."
				)
				.parameter("size", 
					type(Type.INTEGER, "The size of the buffer.")
				)
				.parameter("endian", 
					type(Type.NULL, "Use native endian."),
					type(Type.BOOLEAN, "True = big, false = little.")
				)
				.returns(
					type(Type.BUFFER, "A new allocated buffer of [size] bytes."),
					type(Type.ERROR, "BadParameter", "If size is < 0."),
					type(Type.ERROR, "OutOfMemory", "If not allocated.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				ByteOrder order = null;
				scriptInstance.popStackValue(temp);
				if (temp.isNull())
					order = ByteOrder.nativeOrder();
				else
					order = temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
				scriptInstance.popStackValue(temp);
				int size = temp.asInt();
				
				if (size < 0)
				{
					returnValue.setError("BadParameter", "Size is < 0.");
					return true;
				}

				try {
					returnValue.setEmptyBuffer(size, order);
				} catch (Exception e) {
					returnValue.setError(e);
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFSETSIZE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Resizes a buffer (without creating a new one). The values of the data are kept, " +
					"except for what would be discarded on a buffer shrink, and the data in an expanded part is set to 0."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to resize.")
				)
				.parameter("size", 
					type(Type.INTEGER, "The new size of the buffer.")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "Size is < 0."),
					type(Type.ERROR, "OutOfMemory", "If not allocated.")
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
				int size = temp.asInt();
				
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}

				BufferType buffer = temp.asObjectType(BufferType.class);
				
				if (size < 0)
				{
					returnValue.setError("BadParameter", "Size is < 0.");
					return true;
				}

				buffer.setSize(size);
				
				returnValue.set(temp);				
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFWRAP(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Wraps a list as a buffer."
				)
				.parameter("list", 
					type(Type.LIST, "[INTEGER, ...]", "The list of values to interpret as (clamped) byte values.")
				)
				.parameter("endian", 
					type(Type.NULL, "Use native endian."),
					type(Type.BOOLEAN, "True = big, false = little.")
				)
				.returns(
					type(Type.BUFFER, "A new allocated buffer of [length(list)] bytes."),
					type(Type.ERROR, "OutOfMemory", "If not allocated.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue list = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			try
			{
				ByteOrder order = null;
				scriptInstance.popStackValue(temp);
				if (temp.isNull())
					order = ByteOrder.nativeOrder();
				else
					order = temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;

				scriptInstance.popStackValue(list);
				if (!list.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(list);
					list.set(temp);
				}
				
				try {
					out.setEmptyBuffer(list.length(), order);
				} catch (Exception e) {
					returnValue.setError(e);
					return true;
				}
				
				BufferType buf = out.asObjectType(BufferType.class);
				for (int i = 0; i < list.length(); i++)
				{
					list.listGetByIndex(i, temp);
					buf.putByte(i, temp.asByte());
				}

				returnValue.set(out);
				return true;
			}
			finally
			{
				list.setNull();
				temp.setNull();
				out.setNull();
			}
		}
	},
	
	BUFUNWRAP(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Unwraps a buffer into a list."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to unwrap.")
				)
				.returns(
					type(Type.LIST, "A new list where each element is a value from 0 to 255, representing all of the values in the buffer in order."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer.")
				)
			;
		}

		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue out = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buffer = temp.asObjectType(BufferType.class);
				int len = buffer.size();
				out.setEmptyList(len);
				for (int i = 0; i < len; i++)
				{
					temp.set(buffer.getByte(i));
					out.listAdd(temp);
				}
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				out.setNull();
				temp.setNull();
			}
		}
		
	},
	
	BUFCOPY(5)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Set bulk bytes from another buffer. Does not advance any cursor positions."
				)
				.parameter("destbuf", 
					type(Type.BUFFER, "The destination buffer to use.")
				)
				.parameter("destindex", 
					type(Type.INTEGER, "The index into the destination buffer.")
				)
				.parameter("srcbuf", 
					type(Type.BUFFER, "The source buffer to use."),
					type(Type.LIST, "[INTEGER, ...]", "The list of integers to use as bytes.")
				)
				.parameter("srcindex", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "The starting index into the source buffer.")
				)
				.parameter("length", 
					type(Type.NULL, "Use length(srcbuf) - srcindex."),
					type(Type.INTEGER, "The amount of bytes to read/set.")
				)
				.returns(
					type(Type.BUFFER, "destbuf."),
					type(Type.ERROR, "BadParameter", "If either destbuf or srcbuf are not buffers."),
					type(Type.ERROR, "OutOfBounds", "If index or index + length is out of bounds.")
				)
			;
		}
	
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue srcbuf = CACHEVALUE2.get();
			ScriptValue destbuf = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(temp);
				Integer length = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				int srcindex = temp.isNull() ? 0 : temp.asInt();
				scriptInstance.popStackValue(srcbuf);
				scriptInstance.popStackValue(temp);
				int dstindex = temp.isNull() ? 0 : temp.asInt();
				scriptInstance.popStackValue(destbuf);
				
				if (!destbuf.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer (destination).");
					return true;
				}
				if (!srcbuf.isBuffer())
				{
					returnValue.setError("BadParameter", "Third parameter is not a buffer (source).");
					return true;
				}
				
				BufferType destination = destbuf.asObjectType(BufferType.class);
				BufferType source = srcbuf.asObjectType(BufferType.class);
				if (length == null)
					length = source.size() - srcindex;
				
				destination.readBytes(dstindex, source, srcindex, length);
				returnValue.set(destbuf);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	BUFSETPOS(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets the current cursor position for a buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("position", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "The new current buffer cursor position (0-based).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If the position is out of bounds.")
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
				int position = temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				if (position < 0 || position > buf.size())
				{
					returnValue.setError("OutOfBounds", "Position is out of bounds. size: " + buf.size() +", pos: " + position);
					return true;
				}
				
				buf.setPosition(position);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETPOS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the current cursor position for a buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.returns(
					type(Type.INTEGER, "The buffer's current cursor position (0-based)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer.")
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
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				returnValue.set(temp.asObjectType(BufferType.class).getPosition());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFSLICE(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new buffer that is a subset of bytes from another buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The source buffer to use.")
				)
				.parameter("length", 
					type(Type.NULL, "Use length(buffer) - index."),
					type(Type.INTEGER, "The amount of bytes to copy to the new buffer.")
				)
				.parameter("index", 
					type(Type.NULL, "Use current position as the starting index."),
					type(Type.INTEGER, "The starting index.")
				)
				.returns(
					type(Type.BUFFER, "New allocated buffer of [length] bytes, copied from source (same byte order)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If index or index + length is out of bounds.")
				)
			;
		}
	
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue out = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer length = temp.isNull() ? null : temp.asInt();
	
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
	
				BufferType buf = temp.asObjectType(BufferType.class);
				if (index == null)
					index = buf.getPosition();
				if (length == null)
					length = buf.size() - index;
				
				if (index < 0)
				{
					returnValue.setError("OutOfBounds", "Index < 0");
					return true;
				}
				else if (index + length > buf.size())
				{
					returnValue.setError("OutOfBounds", "Index + length = " + (index + length));
					return true;
				}
				
				temp.setEmptyBuffer(length, buf.getByteOrder());
				temp.asObjectType(BufferType.class).readBytes(0, buf, index, length);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				out.setNull();
				temp.setNull();
			}
		}
	}, 
	
	BUFFILL(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Fills a buffer with a single value. Does not advance any cursor positions."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The destination buffer to use.")
				)
				.parameter("data", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "The byte value to fill.")
				)
				.parameter("amount", 
					type(Type.NULL, "Use length(buffer) - index."),
					type(Type.INTEGER, "The amount of bytes to set.")
				)
				.parameter("index", 
					type(Type.NULL, "Use buffer's current cursor position (cursor position will be advanced)."),
					type(Type.INTEGER, "The starting index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer amount = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				byte data = temp.isNull() ? 0 : temp.asByte();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
	
				BufferType buf = temp.asObjectType(BufferType.class);
				if (amount == null)
					amount = buf.size() - (index == null ? buf.getPosition() : index);
	
				try {
					buf.putBytes(index, data, amount);
					returnValue.set(temp);
					return true;
				} catch (ArrayIndexOutOfBoundsException e) {
					returnValue.setError("OutOfBounds", e.getMessage());
					return true;
				}
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	BUFPUTBYTE(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets a byte in a buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The byte value to set (0 - 255)")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced). "),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced). ")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index] is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				byte value = temp.isNull() ? 0 : temp.asByte();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 0 >= buf.size())
				{
					returnValue.setError("OutOfBounds", "Index " + i + " is out of bounds.");
					return true;
				}
	
				buf.putByte(index, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETBYTE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a byte in a buffer (returned unsigned)."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced). "),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced). ")
				)
				.returns(
					type(Type.INTEGER, "The value at the index (0 - 255)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index] is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 0 >= buf.size() || i < 0)
				{
					returnValue.setError("OutOfBounds", "Index " + i + " is out of bounds.");
					return true;
				}
	
				returnValue.set(buf.getByte(index) & 0x0ff);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	// TODO: Finish buffers.
	
	;
	
	private final int parameterCount;
	private Usage usage;
	private CommonFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(CommonFunctions.values());
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
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE3 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE4 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
