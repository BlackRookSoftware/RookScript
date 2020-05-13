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
 * RookScript list/set functions.
 * @author Matthew Tropiano
 */
public enum ListFunctions implements ScriptFunctionType
{	
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
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				if (value.isList())
				{
					returnValue.setEmptyList(value.length());
					for (int i = 0; i < value.length(); i++)
					{
						value.listGetByIndex(i, temp);
						returnValue.listSetByIndex(i, temp);
					}
				}
				else
				{
					returnValue.setEmptyList(4);
					returnValue.listSetByIndex(0, value);
				}
				return true;
			}
			finally
			{
				value.setNull();
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
					"Gets the index of the last occurrence of a value in a list. " +
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
	
	/** @since 1.4.3 */
	SUBLIST(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new list from a larger list using a range of contiguous list indices."
				)
				.parameter("list", 
					type(Type.LIST, "The list to use.")
				)
				.parameter("start", 
					type(Type.INTEGER, "The starting index (0-based), inclusive.")
				)
				.parameter("end", 
					type(Type.NULL, "Use list length."),
					type(Type.INTEGER, "The ending index (0-based), exclusive. If negative, stop at that many elements from the end.")
				)
				.returns(
					type(Type.NULL, "If either index is out-of-bounds or the end index is less than the start index."),
					type(Type.LIST, "The substring returned.")
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
				Integer endIndex = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				int startIndex = temp.asInt();
				scriptInstance.popStackValue(list);
				
				if (!list.isList())
				{
					returnValue.setNull();
					return true;
				}
				
				int length = list.length();
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
				{
					returnValue.setEmptyList(Math.max(endIndex - startIndex, 1));
					for (int i = startIndex; i < endIndex; i++)
					{
						list.listGetByIndex(i, temp);
						returnValue.listAdd(temp);
					}
				}
				return true;
			}
			finally
			{
				temp.setNull();
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
					"Removes a value from a list, expected to be set up like a set (sorted, discrete). " +
					"This is more performant than a list - search is binary search."
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
	
	;
	
	private final int parameterCount;
	private Usage usage;
	private ListFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(ListFunctions.values());
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
	private static final ThreadLocal<ScriptValue> CACHEVALUE3 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE4 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
