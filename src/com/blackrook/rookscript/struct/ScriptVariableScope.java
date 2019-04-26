/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import java.util.Comparator;

import com.blackrook.commons.ResettableIterator;
import com.blackrook.commons.comparators.CaseInsensitiveComparator;
import com.blackrook.commons.util.ArrayUtils;

/**
 * An single, scoped open variable set in which values can be set.
 * All variable names are CASE-INSENSITIVE.
 * The internals are written so that the storage uses few memory allocations/deletions.
 * None of the variables are read-only. This implementation is thread-safe.
 * @author Matthew Tropiano
 */
public class ScriptVariableScope implements ScriptVariableResolver, Iterable<ScriptVariableScope.Entry>
{
	/** Default capacity. */
	public static final int DEFAULT_CAPACITY = 4;

	private static final Comparator<Entry> ENTRY_COMPARATOR = (e1, e2) -> 
		CaseInsensitiveComparator.getInstance().compare(e1.name, e2.name);
	
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
	
	// Get or return null;
	private ScriptValue get(String name)
	{
		int i;
		if ((i = getIndex(name)) < 0)
			return null;
		else
			return entries[i].value;
	}

	// Get or return -1;
	private int getIndex(String name)
	{
		int u = entryCount, l = 0;
		int i = (u+l)/2;
		int prev = u;
		
		while (i != prev)
		{
			if (entries[i].name.equalsIgnoreCase(name))
				return i;
	
			int c = CaseInsensitiveComparator.getInstance().compare(entries[i].name, name); 
			
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

	private void removeIndex(int i)
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

	/**
	 * Clears the scope.
	 */
	public synchronized void clear()
	{
		int prevCount = this.entryCount;
		this.entryCount = 0;
		// nullify object refs (to reduce chance of memory leaks).
		for (int i = 0; i < prevCount; i++)
			entries[i].value.setNull();
	}
	
	@Override
	public synchronized boolean containsValue(String name)
	{
		return get(name) != null;
	}

	@Override
	public synchronized ScriptValue getValue(String name)
	{
		return get(name);
	}

	@Override
	public synchronized void setValue(String name, Object value)
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
	public synchronized boolean clearValue(String name)
	{
		int i;
		if ((i = getIndex(name)) < 0)
			return false;

		removeIndex(i);
		return true;
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

	@Override
	public ResettableIterator<Entry> iterator()
	{
		return new EntryIterator();
	}
	
	private class EntryIterator implements ResettableIterator<Entry>
	{
		private int cur = 0;
		private boolean removed = false;
		
		@Override
		public boolean hasNext()
		{
			return cur < entryCount;
		}
		
		@Override
		public Entry next()
		{
			removed = false;
			return entries[cur++];
		}
		
		@Override
		public void remove()
		{
			if (removed)
				return;
			
			removeIndex(cur);
			removed = true;
			cur--;
		}
		
		@Override
		public void reset()
		{
			cur = 0;
		}
	}
	
}
