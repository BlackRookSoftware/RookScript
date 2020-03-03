/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.variable;

/**
 * A single, scoped open variable set in which values can be set.
 * All variable names are CASE-INSENSITIVE.
 * The internals are written so that the storage uses few memory allocations/deletions.
 * None of the variables are read-only. This implementation is thread-safe.
 * @author Matthew Tropiano
 */
public class DefaultVariableResolver extends AbstractVariableResolver
{
	/** Default capacity. */
	public static final int DEFAULT_CAPACITY = 4;

	/**
	 * Creates a variable with a default size.
	 * @see #DEFAULT_CAPACITY
	 */
	public DefaultVariableResolver()
	{
		super(DEFAULT_CAPACITY);
	}
	
	/**
	 * Creates a variable resolver with a default size.
	 * @param capacity the initial capacity.
	 */
	public DefaultVariableResolver(int capacity)
	{
		super(capacity);
	}
	
    /**
     * Removes a value by variable name.
     * This should fail if the provided name corresponds to a read-only variable. 
	 * @param name the variable name.
	 * @return true if the value existed and was removed, false otherwise.
	 * @throws IllegalArgumentException if the provided name refers to a value that is read-only.
     */
	public synchronized boolean clearValue(String name)
	{
		int i;
		if ((i = getIndex(name)) < 0)
			return false;

		removeIndex(i);
		return true;
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
			entries[i].clear();
	}
	
}
