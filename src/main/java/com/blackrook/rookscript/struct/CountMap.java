/*******************************************************************************
 * Copyright (c) 2017-2021 Black Rook Software
 * This program and the accompanying materials are made available under 
 * the terms of the MIT License, which accompanies this distribution.
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import java.util.HashMap;

/**
 * A special HashMap that increments or decrements a value.
 * @author Matthew Tropiano
 * @param <T> the type to count.
 */
public class CountMap<T> extends HashMap<T, Integer>
{
	private static final long serialVersionUID = -7309469694913208641L;

	/**
	 * Creates a new map with default capacity and load factor.
	 */
	public CountMap()
	{
		super();
	}

	/**
	 * Creates a new map with specific capacity and default load factor.
	 * @param initialCapacity initial table capacity.
	 */
	public CountMap(int initialCapacity)
	{
		super(initialCapacity);
	}
	
	/**
	 * Creates a new map with specific capacity and load factor.
	 * @param initialCapacity initial table capacity.
	 * @param loadFactor load factor before re-hash.
	 */
	public CountMap(int initialCapacity, float loadFactor)
	{
		super(initialCapacity, loadFactor);
	}

	/**
	 * Gets the current value corresponding to the key.
	 * If the key does not exist, this returns 0.
	 * @param key the key.
	 * @return the current total for the key.
	 */
	public int amount(T key)
	{
		return getOrDefault(key, 0);
	}
	
	/**
	 * Adds to a key's value.
	 * If the value becomes 0, it is removed.
	 * @param key the key.
	 * @param amount the amount to add. If negative, calls <code>take(key, -amount)</code>.
	 * @return the new total for the key.
	 */
	public int give(T key, int amount)
	{
		if (amount < 0)
		{
			return take(key, -amount);
		}
		else if (containsKey(key))
		{
			int out = get(key) + amount;
			put(key, out);
			return out;
		}
		else
		{
			put(key, amount);
			return amount;
		}
	}

	/**
	 * Subtracts from a key's value.
	 * You can't remove more than the value (it will be set to 0).
	 * If the value becomes 0, it is removed.
	 * @param key the key.
	 * @param amount the amount to remove. If negative, calls <code>give(key, -amount)</code>.
	 * @return the new total for the key.
	 */
	public int take(T key, int amount)
	{
		if (amount < 0)
		{
			return give(key, -amount);
		}
		else if (containsKey(key))
		{
			int out = Math.max(get(key) - amount, 0);
			if (out == 0)
				remove(key);
			else
				put(key, out);
			return out;
		}
		else
		{
			return 0;
		}
	}

}
