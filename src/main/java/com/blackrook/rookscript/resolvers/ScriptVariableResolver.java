/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers;

import com.blackrook.rookscript.ScriptValue;

/**
 * An interface for structures that map string keys to
 * {@link ScriptValue}s. As a strict policy, all variable names are CASE-INSENSITIVE.
 * @author Matthew Tropiano
 */
public interface ScriptVariableResolver
{
	/**
	 * Gets the corresponding value for a variable name.
	 * Changing the returned value does not change the value, unless it is a reference type
	 * like a map or list.
	 * @param name the variable name.
	 * @param out the destination variable for the value.
	 * @return true if a corresponding value was fetched into out, false if not. If false, out is set to the null value.
	 */
    public boolean getValue(String name, ScriptValue out);
    
    /**
     * Sets a key-value pair.
     * This should fail if the provided name corresponds to a read-only variable. 
	 * @param name the variable name.
	 * @param value the corresponding value.
	 * @throws IllegalArgumentException if the provided name refers to a value that is read-only.
	 * @see #isReadOnly(String)
     */
    public void setValue(String name, ScriptValue value);
        
    /**
     * Checks if this contains a value by its variable name.
	 * @param name the variable name.
	 * @return true if so, false if not.
     */
    public boolean containsValue(String name);
    
    /**
     * Checks if an existing value is read-only.
	 * @param name the variable name.
	 * @return true if so, false if not.
     */
    public boolean isReadOnly(String name);

    /**
     * Checks if this resolver maintains no values.
     * @return true if so, false if not.
     */
	public boolean isEmpty();

	/**
	 * @return the amount of values that this maintains.
	 */
	public int size();

}
