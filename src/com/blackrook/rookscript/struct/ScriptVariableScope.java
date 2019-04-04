/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import java.util.Comparator;

import com.blackrook.commons.Sizable;
import com.blackrook.commons.comparators.CaseInsensitiveComparator;
import com.blackrook.commons.util.ArrayUtils;

/**
 * An single, scoped open variable set in which values can be set.
 * All variable names are CASE-INSENSITIVE.
 * The internals are written so that the storage uses few memory allocations/deletions. 
 * @author Matthew Tropiano
 */
public class ScriptVariableScope implements Sizable
{
	/** Default capacity. */
	public static final int DEFAULT_CAPACITY = 4;

	private static final Comparator<Entry> ENTRY_COMPARATOR = new Comparator<Entry>()
	{
		@Override
		public int compare(Entry e1, Entry e2)
		{
			return CaseInsensitiveComparator.getInstance().compare(e1.name, e2.name);
		}
		
	};
	
	/** List of entries. */
	private Entry[] entries;
	/** Count. */
	private int entryCount;

	/**
	 * Creates a scope with a default size.
	 * @see #DEFAULT_CAPACITY
	 */
	public ScriptVariableScope()
	{
		this(DEFAULT_CAPACITY);
	}
	
	/**
	 * Creates a scope with a default size.
	 * @param capacity the initial capacity.
	 */
	public ScriptVariableScope(int capacity)
	{
		if (capacity < 1) 
			capacity = 1;
		expand(capacity);
		this.entryCount = 0;
	}
	
	private void checkExpand()
	{
		if (entryCount == entries.length - 1)
			expand(entries.length * 2);
	}

	// Expands this.
	private void expand(int capacity)
	{
		Entry[] newEntries = new Entry[capacity];
		if (entries != null)
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
		for (int i = entries != null ? entries.length : 0; i < newEntries.length; i++)
			newEntries[i] = new Entry();
		entries = newEntries;
	}
	
	/**
	 * Clears the scope.
	 */
	public void clear()
	{
		int prevCount = this.entryCount;
		this.entryCount = 0;
		// nullify object refs (to reduce chance of memory leaks).
		for (int i = 0; i < prevCount; i++)
			entries[i].value.set(0L);
	}
	
	/**
	 * Checks if a script value by name exists.
	 * @param name the name of the variable.
	 * @return the value or null if no variable.
	 */
	public boolean contains(String name)
	{
		return get(name) != null;
	}

	/**
	 * Gets a corresponding script value by name.
	 * @param name the name of the variable.
	 * @return the value or null if no variable.
	 */
	public ScriptValue get(String name)
	{
		int u = entryCount, l = 0;
		int i = (u+l)/2;
		int prev = u;
		
		while (i != prev)
		{
			if (entries[i].name.equalsIgnoreCase(name))
				return entries[i].value;

			int c = entries[i].name.compareTo(name); 
			
			if (c < 0)
				l = i;
			else if (c == 0)
				return entries[i].value;
			else
				u = i;
			
			prev = i;
			i = (u+l)/2;
		}
		
		return null;
	}

	/**
	 * Sets a corresponding script value by name.
	 * @param name the name of the variable.
	 * @param value the value to set.
	 */
	public void set(String name, Object value)
	{
		ScriptValue ev = get(name); 
		if (ev != null)
		{
			ev.set(value);
			return;
		}
		
		checkExpand();
		entries[entryCount].name = name;
		entries[entryCount].value.set(value);
		ArrayUtils.sortFrom(entries, entryCount, ENTRY_COMPARATOR);
		entryCount++;
	}

	@Override
	public int size()
	{
		return entryCount;
	}

	@Override
	public boolean isEmpty()
	{
		return size() == 0;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		int i = 0;
		while (i < entryCount)
		{
			sb.append(entries[i]);
			if (i < entryCount - 1)
				sb.append(", ");
			i++;
		}
		sb.append(']');
		return sb.toString();
	}
	
	/**
	 * A single entry.
	 */
	private static class Entry
	{
		private String name;
		private ScriptValue value;
		
		Entry()
		{
			this.name = null;
			this.value = ScriptValue.create(false);
		}
		
		@Override
		public String toString()
		{
			return name + ": " + value;
		}
		
	}
	
}
