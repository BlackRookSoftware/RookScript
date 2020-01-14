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
import com.blackrook.rookscript.ScriptValue.ErrorType;
import com.blackrook.rookscript.ScriptValue.MapType;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.resolvers.variable.AbstractVariableResolver.Entry;

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
					ScriptFunctionUsage.type("The provided value.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, 
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
					ScriptFunctionUsage.type("The provided value.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.BOOLEAN, "True if so, false if not.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.ERROR, "The error.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not an error."),
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The error type.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.ERROR, "The error.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not an error."),
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The error message.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.ERROR, "The error.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not an error."),
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The localized error message.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.ERROR, "The error.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not an error."),
					ScriptFunctionUsage.type(ScriptValue.Type.MAP, "A map of {type:STRING, message:STRING, localizedmessage:STRING}.")
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
					ScriptFunctionUsage.type("The value.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, 
						"If value is: STRING, the length in characters. LIST, the length in elements." +
						"MAP, the amount of keys. OBJECTREF, if Collection, returns size(). Others, 1."
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
					ScriptFunctionUsage.type("The value.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER,
						"Returns true if: NULL." +
						"OBJECTREF: is a Collection and isEmpty() returns true. "+
						"BOOLEAN: is false. "+
						"INTEGER or FLOAT: is 0 or NaN. "+
						"STRING: length = 0. "+
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
					ScriptFunctionUsage.type("The string (if not STRING, will be converted).")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The same string converted to uppercase.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The string (if not STRING, will be converted).")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The same string converted to lowercase.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The string (if not STRING, will be converted).")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The trimmed string.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The string (if not STRING, will be converted).")
				)
				.parameter("index", 
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The index (0-based).")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If the index is out-of-bounds."),
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The character returned.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The string (if not STRING, will be converted).")
				)
				.parameter("start", 
						ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The starting index (0-based), inclusive.")
				)
				.parameter("end", 
						ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The ending index (0-based), exclusive.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If either index is out-of-bounds or the end index is less than the start index."),
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The substring returned.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The string (if not STRING, will be converted).")
				)
				.parameter("search", 
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The string to search for (if not STRING, will be converted).")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not found."),
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The starting index.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The string (if not STRING, will be converted).")
				)
				.parameter("search", 
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The string to search for (if not STRING, will be converted).")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not found."),
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The starting index.")
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
					ScriptFunctionUsage.type("The list to copy.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The resultant list.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The new list length.")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The fill value. Copies are not made for each element.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The resultant new list.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The list.")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The value to add.")
				)
				.parameter("index", 
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "Adds it to the end."),
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The index to add the value at (shifts the others).")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.BOOLEAN, "True if added. False if not a list.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The list.")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The value to remove.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.BOOLEAN, "True if removed. False if not found or not a list.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The list.")
				)
				.parameter("index", 
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The index.")
				)
				.returns(
					ScriptFunctionUsage.type("The value removed, NULL if index is out-of-bounds or a list was not provided.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The list to sort.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The list that was sorted (not a new copy).")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The list to search.")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The value to search for.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.BOOLEAN, "True if it contains the value, false if not.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The list to search.")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The value to search for.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not found."),
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The index of the found element.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The list to search.")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The value to search for.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not found."),
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The index of the found element.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The list to prepare.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The new list, set-ified.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The set (list).")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The value to add.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.BOOLEAN, "True if value was added (and is not already in the list), false otherwise.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The set (list).")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The value to remove.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.BOOLEAN, "True if the value was removed, false otherwise.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The set (list).")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The value to look for.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.BOOLEAN, "True if the value was found, false otherwise.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The set (list).")
				)
				.parameter("value", 
					ScriptFunctionUsage.type("The value to look for.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not found."),
					ScriptFunctionUsage.type(ScriptValue.Type.INTEGER, "The index in the list that it was found.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The first set (list)."),
					ScriptFunctionUsage.type("A value to encapsulate into a set.")
				)
				.parameter("list2", 
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The second set (list)."),
					ScriptFunctionUsage.type("A value to encapsulate into a set.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The new set that is the union of both sets.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The first set (list)."),
					ScriptFunctionUsage.type("A value to encapsulate into a set.")
				)
				.parameter("list2", 
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The second set (list)."),
					ScriptFunctionUsage.type("A value to encapsulate into a set.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The new set that is the intersection of both sets.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The first set (list)."),
					ScriptFunctionUsage.type("A value to encapsulate into a set.")
				)
				.parameter("list2", 
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The second set (list)."),
					ScriptFunctionUsage.type("A value to encapsulate into a set.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The new set that is the XOr of both sets.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The first set (list)."),
					ScriptFunctionUsage.type("A value to encapsulate into a set.")
				)
				.parameter("list2", 
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The second set (list)."),
					ScriptFunctionUsage.type("A value to encapsulate into a set.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "The new set that is the difference of both sets.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.MAP, "The map.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not a map."),
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "[STRING, ...] A new list of the map's keys.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.MAP, "The map.")
				)
				.parameter("key", 
					ScriptFunctionUsage.type(ScriptValue.Type.STRING, "The key.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.NULL, "If not a map."),
					ScriptFunctionUsage.type(ScriptValue.Type.LIST, "[STRING, ...] A new list of the map's keys.")
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
					ScriptFunctionUsage.type(ScriptValue.Type.MAP, "The first map."),
					ScriptFunctionUsage.type("An empty map.")
				)
				.parameter("map2", 
					ScriptFunctionUsage.type(ScriptValue.Type.MAP, "The second map."),
					ScriptFunctionUsage.type("An empty map.")
				)
				.returns(
					ScriptFunctionUsage.type(ScriptValue.Type.MAP, "A new map.")
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
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue value);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE3 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE4 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
