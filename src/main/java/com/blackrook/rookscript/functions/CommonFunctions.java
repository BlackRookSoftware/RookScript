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
import com.blackrook.rookscript.ScriptValue.ErrorType;
import com.blackrook.rookscript.ScriptValue.MapType;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.resolvers.variable.AbstractVariableResolver.Entry;

/**
 * Script common functions that work for all scripts.
 * @author Matthew Tropiano
 */
public enum CommonFunctions implements ScriptFunctionType
{	
	/**
	 * Returns the "length" of a value.
	 * ARG1: The value. 
	 * 
	 * Strings: string length.
	 * Lists: list length.
	 * Others: 1
	 */
	TYPEOF(1)
	{
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

	/**
	 * Returns if the provided value is an error type. True if so, false if not.
	 * ARG1: The value. 
	 */
	ISERROR(1)
	{
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
	
	/**
	 * Returns the error type.
	 * If not an error, this returns null.
	 * ARG1: The value. 
	 */
	ERRORTYPE(1)
	{
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

	/**
	 * Returns the error message.
	 * If not an error, this returns null.
	 * ARG1: The value. 
	 */
	ERRORMSG(1)
	{
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

	/**
	 * Returns the localized error message.
	 * If not an error, this returns false.
	 * ARG1: The value. 
	 */
	ERRORLOCALMSG(1)
	{
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

	/**
	 * Returns an error type as a map.
	 * If not an error, this returns false.
	 * ARG1: The value. 
	 */
	ERRORMAP(1)
	{
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
					temp.mapSet("localizedMessage", error.getLocalizedMessage());
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

	/**
	 * Returns the "length" of a value.
	 * ARG1: The value. 
	 * 
	 * Strings: string length.
	 * Lists: list length.
	 * ObjectRef: length if Collection.
	 * Others: 1
	 */
	LENGTH(1)
	{
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

	/**
	 * Returns if a value is "empty".
	 * ARG1: The value. 
	 * 
	 * Object: is Sizeable and empty, or is a Collection and empty, or null.
	 * Boolean: is false.
	 * Numeric: is 0 or NaN
	 * Strings: length = 0.
	 * Lists: length = 0.
	 * Maps: length = 0.
	 */
	EMPTY(1)
	{
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

	/**
	 * Returns a string in full uppercase.
	 * ARG1: The value (converted to string, first). 
	 */
	STRUPPER(1)
	{
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
	
	/**
	 * Returns a string in full lowercase.
	 * ARG1: The value (converted to string, first). 
	 */
	STRLOWER(1)
	{
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
	
	/**
	 * Returns a string trimmed of whitespace at both ends.
	 * ARG1: The value (converted to string, first). 
	 */
	STRTRIM(1)
	{
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
	
	/**
	 * Returns a single character from a string.
	 * If ARG2 is out of bounds, this returns null.
	 * ARG1: The string value (may be converted). 
	 * ARG2: The string index. 
	 */
	STRCHAR(2)
	{
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
	
	/**
	 * Returns a substring of another string.
	 * Returns null if either index out of bounds, or end index is less than the start index.
	 * ARG1: The string (converted). 
	 * ARG2: The starting index (inclusive). 
	 * ARG3: The ending index (exclusive). 
	 */
	SUBSTR(3)
	{
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
	
	/**
	 * Returns the starting index of a string inside another string.
	 * If not found, this returns -1.
	 * ARG1: The string (converted). 
	 * ARG2: The string to search for (converted). 
	 */
	STRINDEX(2)
	{
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
				returnValue.set(str.indexOf(targetStr));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Returns the starting index of a string inside another string, searching from the end.
	 * If not found, this returns -1.
	 * ARG1: The string (converted). 
	 * ARG2: The string to search for (converted). 
	 */
	STRLASTINDEX(2)
	{
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
				returnValue.set(str.lastIndexOf(targetStr));
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
	
	/**
	 * Adds a value to a list. 
	 * If the "list" argument is not a list or not added, this returns false, else true.
	 * ARG1: The list to add the item to. 
	 * ARG2: The item to add.
	 */
	LISTADD(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue item = CACHEVALUE1.get();
			ScriptValue list = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(item);
				scriptInstance.popStackValue(list);
				returnValue.set(list.listAdd(item));
				return true;				
			}
			finally
			{
				item.setNull();
				list.setNull();
			}
		}
	},
	
	/**
	 * Adds a value to a list. 
	 * If the "list" argument is not a list or not added, this returns false, else true.
	 * ARG1: The list to add the item to. 
	 * ARG2: The item to add.
	 * ARG3: The index to add it to.
	 */
	LISTADDAT(3)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue item = CACHEVALUE1.get();
			ScriptValue list = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(item);
				int index = item.asInt();
				scriptInstance.popStackValue(item);
				scriptInstance.popStackValue(list);
				returnValue.set(list.listAddAt(index, item));
				return true;
			}
			finally
			{
				item.setNull();
				list.setNull();
			}
		}
	},
	
	/**
	 * Removes a value from a list that matches the item. 
	 * Only removes list-typed items by reference.
	 * If the "list" argument is not a list or the provided item is not removed, this returns false, else true.
	 * ARG1: The list to remove the item from. 
	 * ARG2: The item to remove.
	 */
	LISTREMOVE(2)
	{
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
	
	/**
	 * Removes a value from a list that matches the item. 
	 * Only removes list-typed items by reference.
	 * Returns the removed item, or false if no item removed due to a bad index.
	 * ARG1: The list to remove the item from. 
	 * ARG2: The index to remove.
	 */
	LISTREMOVEAT(2)
	{
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
	
	/**
	 * Sorts a list in-place.
	 * Returns the list that was sorted (NOT a new copy!).
	 * If this list contains discretely different elements,  
	 * ARG1: The list to remove the item from. 
	 */
	LISTSORT(1)
	{
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
	
	/**
	 * Checks if a list contains a specific value.
	 * Sequential search.
	 * Return true if it contains the value, false if not.
	 * ARG1: The list to look in. 
	 * ARG2: The item to look for.
	 */
	LISTCONTAINS(2)
	{
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
	
	/**
	 * Gets the index of a value in the list. 
	 * Only finds list-typed items by reference.
	 * If not found or not a list, this returns -1.
	 * ARG1: The list to look in. 
	 * ARG2: The item to look for.
	 */
	LISTINDEX(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue item = CACHEVALUE1.get();
			ScriptValue list = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(item);
				scriptInstance.popStackValue(list);
				returnValue.set(list.listGetIndexOf(item));
				return true;
			}
			finally
			{
				item.setNull();
				list.setNull();
			}
		}
	},
	
	/**
	 * Creates a new set of items from a list, such that 
	 * the contents of the list are discrete and sorted.
	 * The object returned is a list, but its contents are now suitable for set operations.
	 * ARG1: The list or value.
	 */
	SET(1)
	{
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
	
	/**
	 * Adds a value to a list, expected to be set up like a set.
	 * Returns true if value was added (and is not already in the list), false otherwise.
	 * ARG1: The set (list).
	 * ARG2: The value to add.
	 */
	SETADD(2)
	{
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
	
	/**
	 * Removes a value from a list, expected to be set up like a set.
	 * Returns true if value was removed, false otherwise.
	 * ARG1: The set (list).
	 * ARG2: The value to remove.
	 */
	SETREMOVE(2)
	{
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
	
	/**
	 * Checks if a value exists in a list, expected to be set up like a set.
	 * This is more performant than a list - search is binary search.
	 * Returns true if value was removed, false otherwise.
	 * ARG1: The set (list).
	 * ARG2: The value to look for.
	 */
	SETCONTAINS(2)
	{
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
	
	/**
	 * Gets the index of a value in a list, expected to be set up like a set.
	 * This is more performant than a list - search is binary search.
	 * If not found or not a list, this returns -1.
	 * ARG1: The set (list).
	 * ARG2: The value to look for.
	 */
	SETSEARCH(2)
	{
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
	
	/**
	 * Gets the union of two sets, returning a new set with values in both. 
	 * Returns a new set.
	 * ARG1: The first set (list).
	 * ARG2: The second set (list).
	 */
	SETUNION(2)
	{
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
	
	/**
	 * Gets the intersection of two sets, returning a new set with values in both. 
	 * Returns a new set.
	 * ARG1: The first set (list).
	 * ARG2: The second set (list).
	 */
	SETINTERSECT(2)
	{
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
	
	/**
	 * Gets the xor of two sets, returning the union of both sets minus the intersection. 
	 * Returns a new set.
	 * ARG1: The first set (list).
	 * ARG2: The second set (list).
	 */
	SETXOR(2)
	{
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
	
	/**
	 * Gets a new set that is the first set minus the values in the second set. 
	 * Returns a new set.
	 * ARG1: The first set (list).
	 * ARG2: The second set (list).
	 */
	SETDIFF(2)
	{
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
	
	/**
	 * Gets a list of all of the keys in a map.
	 * Returns a new list, or null if not a map.
	 * The returned list is suitable for set operations.
	 * ARG1: The map.
	 */
	MAPKEYS(1)
	{
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
					returnValue.set(false);
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
	
	/**
	 * Returns a value that corresponds to a key in the map.
	 * ARG1: The map.
	 * ARG2: The key.
	 */
	MAPVALUE(2)
	{
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
	
	/**
	 * Gets a map that is the result of taking the first map and adding all
	 * of the keys of the second, replacing the keys that exist in the first.
	 * The copies are shallow - references are preserved. 
	 * Returns a new map. 
	 * If the first value is not a map, this returns an empty map.
	 * If the second value is not a map, a shallow copy of the first map is returned. 
	 * ARG1: The first map.
	 * ARG2: The second map.
	 */
	MAPMERGE(2)
	{
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
	private CommonFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
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
		return null;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue value);

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE3 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE4 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
