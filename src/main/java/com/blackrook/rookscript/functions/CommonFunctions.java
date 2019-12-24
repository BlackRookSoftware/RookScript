/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import static com.blackrook.rookscript.struct.ScriptThreadLocal.getCache;

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
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(scriptInstance.popStackValue().getTypeName());
			return true;
		}
	},

	/**
	 * Returns if the provided value is an error type. True if so, false if not.
	 * ARG1: The value. 
	 */
	ISERROR(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(scriptInstance.popStackValue().isError());
			return true;
		}
	},
	
	/**
	 * Returns the error type.
	 * If not an error, this returns false.
	 * ARG1: The value. 
	 */
	ERRORTYPE(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue sv = scriptInstance.popStackValue();
			if (!sv.isError())
			{
				scriptInstance.pushStackValue(false);
				return true;
			}
			
			scriptInstance.pushStackValue(sv.asObjectType(ErrorType.class).getType());
			return true;
		}
	},

	/**
	 * Returns the error message.
	 * If not an error, this returns false.
	 * ARG1: The value. 
	 */
	ERRORMSG(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue sv = scriptInstance.popStackValue();
			if (!sv.isError())
			{
				scriptInstance.pushStackValue(false);
				return true;
			}
			
			scriptInstance.pushStackValue(sv.asObjectType(ErrorType.class).getMessage());
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue sv = scriptInstance.popStackValue();
			if (!sv.isError())
			{
				scriptInstance.pushStackValue(false);
				return true;
			}
			
			scriptInstance.pushStackValue(sv.asObjectType(ErrorType.class).getLocalizedMessage());
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue sv = scriptInstance.popStackValue();
			if (!sv.isError())
			{
				scriptInstance.pushStackValue(false);
				return true;
			}
			
			ScriptValue out = ScriptValue.createEmptyMap();
			out.mapExtract(sv.asObjectType(ErrorType.class));
			scriptInstance.pushStackValue(out);
			return true;
		}
	},

	/**
	 * Returns the "length" of a value.
	 * ARG1: The value. 
	 * 
	 * Strings: string length.
	 * Lists: list length.
	 * Others: 1
	 */
	LENGTH(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			if (value.isString())
				scriptInstance.pushStackValue(value.asString().length());
			else
				scriptInstance.pushStackValue(value.length());
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(value.empty());
			return true;
		}
	},

	/**
	 * Returns a string in full uppercase.
	 * ARG1: The value (converted to string, first). 
	 */
	STRUPPER(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(scriptInstance.popStackValue().asString().toUpperCase());
			return true;
		}
	},
	
	/**
	 * Returns a string in full lowercase.
	 * ARG1: The value (converted to string, first). 
	 */
	STRLOWER(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(scriptInstance.popStackValue().asString().toLowerCase());
			return true;
		}
	},
	
	/**
	 * Returns a string trimmed of whitespace at both ends.
	 * ARG1: The value (converted to string, first). 
	 */
	STRTRIM(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(scriptInstance.popStackValue().asString().trim());
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			int value = scriptInstance.popStackValue().asInt();
			String str = scriptInstance.popStackValue().asString();
			if (value < 0 || value >= str.length())
				scriptInstance.pushStackValue(null);
			else
				scriptInstance.pushStackValue(String.valueOf(str.charAt(value)));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			int endIndex = scriptInstance.popStackValue().asInt();
			int startIndex = scriptInstance.popStackValue().asInt();
			String str = scriptInstance.popStackValue().asString();
			int length = str.length();
			if (startIndex < 0 || startIndex >= length)
				scriptInstance.pushStackValue(null);
			else if (endIndex < 0 && endIndex > length)
				scriptInstance.pushStackValue(null);
			else if (endIndex < startIndex)
				scriptInstance.pushStackValue(null);
			else
				scriptInstance.pushStackValue(str.substring(startIndex, endIndex));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			String targetStr = scriptInstance.popStackValue().asString();
			String str = scriptInstance.popStackValue().asString();
			scriptInstance.pushStackValue(str.indexOf(targetStr));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			String targetStr = scriptInstance.popStackValue().asString();
			String str = scriptInstance.popStackValue().asString();
			scriptInstance.pushStackValue(str.lastIndexOf(targetStr));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			if (value.isList())
				scriptInstance.pushStackValue(value.copy());
			else
				scriptInstance.pushStackValue(new Object[]{value.copy()});
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.listAdd(item));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			int index = scriptInstance.popStackValue().asInt();
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.listAddAt(index, item));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.listRemove(item));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			int index = scriptInstance.popStackValue().asInt();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.listRemoveAt(index));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue list = scriptInstance.popStackValue();
			list.sort();
			scriptInstance.pushStackValue(list);
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.listContains(item));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.listGetIndexOf(item));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue out = ScriptValue.createEmptyList();
			if (value.isList())
			{
				for (int i = 0; i < value.length(); i++)
					out.setAdd(value.listGetByIndex(i));
			}
			else
			{
				out.setAdd(value);
			}
			
			scriptInstance.pushStackValue(out);
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue set = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(set.setAdd(value));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue set = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(set.setRemove(value));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue set = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(set.setContains(value));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue set = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(set.setSearch(value));
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue set2 = scriptInstance.popStackValue();
			ScriptValue set1 = scriptInstance.popStackValue();
			ScriptValue out = ScriptValue.createEmptyList();
			
			ScriptValue union1;
			ScriptValue union2;
			
			if (set1.isList())
				union1 = set1;
			else
				union1 = wrapList(set1);
			
			if (set2.isList())
				union2 = set2;
			else
				union2 = wrapList(set2);

			for (int i = 0; i < union1.length(); i++)
				out.setAdd(union1.listGetByIndex(i));
			for (int i = 0; i < union2.length(); i++)
				out.setAdd(union2.listGetByIndex(i));
			
			scriptInstance.pushStackValue(out);
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue set2 = scriptInstance.popStackValue();
			ScriptValue set1 = scriptInstance.popStackValue();
			ScriptValue out = ScriptValue.createEmptyList();
			
			ScriptValue intersect1;
			ScriptValue intersect2;
			
			if (set1.isList())
				intersect1 = set1;
			else
				intersect1 = wrapList(set1);
			
			if (set2.isList())
				intersect2 = set2;
			else
				intersect2 = wrapList(set2);

			ScriptValue smallest = intersect1.length() < intersect2.length() ? intersect1 : intersect2;
			ScriptValue largest = smallest == intersect1 ? intersect2 : intersect1;
			
			for (int i = 0; i < smallest.length(); i++)
			{
				ScriptValue sv = smallest.listGetByIndex(i);
				if (largest.setContains(sv))
					out.setAdd(sv);
			}
			
			scriptInstance.pushStackValue(out);
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue set2 = scriptInstance.popStackValue();
			ScriptValue set1 = scriptInstance.popStackValue();

			ScriptValue xor1;
			ScriptValue xor2;
			
			if (set1.isList())
				xor1 = set1;
			else
				xor1 = wrapList(set1);
			
			if (set2.isList())
				xor2 = set2;
			else
				xor2 = wrapList(set2);

			ScriptValue out = xor1.copy();
			for (int i = 0; i < xor2.length(); i++)
			{
				ScriptValue sv = xor2.listGetByIndex(i);
				if (out.setContains(sv))
					out.setRemove(sv);
				else
					out.setAdd(sv);
			}
			
			scriptInstance.pushStackValue(out);
			return true;
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
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue set2 = scriptInstance.popStackValue();
			ScriptValue set1 = scriptInstance.popStackValue();

			ScriptValue diff1;
			ScriptValue diff2;
			
			if (set1.isList())
				diff1 = set1;
			else
				diff1 = wrapList(set1);
			
			if (set2.isList())
				diff2 = set2;
			else
				diff2 = wrapList(set2);

			ScriptValue out = diff1.copy();
			for (int i = 0; i < diff2.length(); i++)
				out.setRemove(diff2.listGetByIndex(i));
			
			scriptInstance.pushStackValue(out);
			return true;
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
		public boolean execute(ScriptInstance scriptInstance) 
		{
			ScriptValue sv = scriptInstance.popStackValue();
			if (!sv.isMap())
			{
				scriptInstance.pushStackValue(false);
				return true;
			}
			
			ScriptValue out = ScriptValue.createEmptyList();
			for (Entry e : sv.asObjectType(MapType.class))
				out.setAdd(e.getName());
			scriptInstance.pushStackValue(out);
			return true;
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
		public boolean execute(ScriptInstance scriptInstance) 
		{
			ScriptValue keyValue = scriptInstance.popStackValue();
			ScriptValue sv = scriptInstance.popStackValue();
			if (!sv.isMap())
			{
				scriptInstance.pushStackValue(null);
				return true;
			}
			
			ScriptValue out = getCache().temp;
			sv.mapGet(keyValue.asString(), out);
			scriptInstance.pushStackValue(out);
			return true;
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
		public boolean execute(ScriptInstance scriptInstance) 
		{
			ScriptValue map2 = scriptInstance.popStackValue();
			ScriptValue map1 = scriptInstance.popStackValue();
			ScriptValue out = ScriptValue.createEmptyMap();
			if (!map1.isMap())
			{
				scriptInstance.pushStackValue(out);
				return true;
			}
			
			for (Entry e : map1.asObjectType(MapType.class))
				out.mapSet(e.getName(), e.getValue());
			
			if (!map2.isMap())
			{
				scriptInstance.pushStackValue(out);
				return true;
			}

			for (Entry e : map2.asObjectType(MapType.class))
				out.mapSet(e.getName(), e.getValue());
			
			scriptInstance.pushStackValue(out);
			return true;
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
	public static final ScriptFunctionResolver getResolver()
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
	public abstract boolean execute(ScriptInstance scriptInstance);

	// wraps a single value into a list.
	protected ScriptValue wrapList(ScriptValue sv)
	{
		ScriptValue out = ScriptValue.createEmptyList();
		out.listAdd(sv);
		return out;
	}
	
}
