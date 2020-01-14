/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.variable;

import java.util.Comparator;

import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.resolvers.ScriptVariableResolver;
import com.blackrook.rookscript.struct.Utils;

/**
 * A single, scoped open variable set in which values can be set.
 * All variable names are CASE-INSENSITIVE.
 * The internals are written so that the storage uses few memory allocations/deletions.
 * None of the variables are read-only. This implementation is thread-safe.
 * @author Matthew Tropiano
 */
public class AbstractVariableResolver implements ScriptVariableResolver
{
	/** Default capacity. */
	public static final int DEFAULT_CAPACITY = 4;

	private static final Comparator<String> ENTRYNAME_COMPARATOR = (e1, e2) -> e1.compareToIgnoreCase(e2);
	private static final Comparator<Entry> ENTRY_COMPARATOR = (e1, e2) -> ENTRYNAME_COMPARATOR.compare(e1.name, e2.name);
	
	/** List of entries. */
	protected Entry[] entries;
	/** Count. */
	protected int entryCount;

	/**
	 * Creates a variable resolver with a default size.
	 * @param capacity the initial capacity.
	 */
	protected AbstractVariableResolver(int capacity)
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
	
	// Get or return null;
	protected ScriptValue get(String name)
	{
		int i;
		if ((i = getIndex(name)) < 0)
			return null;
		else
			return entries[i].value;
	}

	// Get or return -1;
	protected int getIndex(String name)
	{
		int u = entryCount, l = 0;
		int i = (u+l)/2;
		int prev = u;
		
		while (i != prev)
		{
			if (entries[i].name.equalsIgnoreCase(name))
				return i;
	
			int c = ENTRYNAME_COMPARATOR.compare(entries[i].name, name); 
			
			if (c < 0)
				l = i;
			else if (c == 0)
				return i;
			else
				u = i;
			
			prev = i;
			i = (u+l)/2;
		}
		
		return -1;
	}

	protected void removeIndex(int i)
	{
		Entry e = entries[i];
		e.name = null;
		e.value.setNull();
		entryCount--;
		
		while (i < entryCount)
		{
			entries[i] = entries[i + 1];
			i++;
		}
		
		entries[i] = e;
	}

	@Override
	public synchronized boolean containsValue(String name)
	{
		return get(name) != null;
	}

	@Override
	public synchronized boolean getValue(String name, ScriptValue out)
	{
		ScriptValue sv = get(name);
		if (sv != null)
		{
			out.set(sv);
			return true;
		}
		else
		{
			out.setNull();
			return false;
		}
	}

	@Override
	public synchronized void setValue(String name, ScriptValue value)
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
		Utils.sortFrom(entries, entryCount, ENTRY_COMPARATOR);
		entryCount++;
	}

	@Override
	public boolean isReadOnly(String name)
	{
		return false;
	}

	@Override
	public synchronized int size()
	{
		return entryCount;
	}

	@Override
	public synchronized boolean isEmpty()
	{
		return size() == 0;
	}

	@Override
	public synchronized String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		int i = 0;
		while (i < entryCount)
		{
			sb.append(entries[i]);
			if (i < entryCount - 1)
				sb.append(", ");
			i++;
		}
		sb.append('}');
		return sb.toString();
	}
	
	/**
	 * A single entry.
	 */
	public static class Entry
	{
		private String name;
		private ScriptValue value;
		
		private Entry()
		{
			this.name = null;
			this.value = ScriptValue.create(null);
		}
		
		protected void clear()
		{
			this.name = null;
			this.value.setNull();
		}
		
		public String getName()
		{
			return name;
		}
		
		public ScriptValue getValue()
		{
			return value;
		}
		
		@Override
		public String toString()
		{
			return name + ": " + value;
		}
		
	}
}
