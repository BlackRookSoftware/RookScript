package com.blackrook.rookscript.struct;

import com.blackrook.commons.Sizable;

/**
 * An interface for structures that map string keys to
 * {@link ScriptValue}s. As a strict policy, all variable names are CASE-INSENSITIVE.
 * @author Matthew Tropiano
 */
public interface ScriptVariableResolver extends Sizable
{
	/**
	 * Gets the corresponding value for a variable name.
	 * Changing the returned value, if any, changes the value in-place.
	 * @param name the variable name.
	 * @return the corresponding value, or <code>null</code> if no corresponding value.
	 */
    public ScriptValue getValue(String name);
    
    /**
     * Sets a key-value pair.
     * This should fail if the provided name corresponds to a read-only variable. 
	 * @param name the variable name.
	 * @param value the corresponding value.
	 * @throws IllegalArgumentException if the provided name refers to a value that is read-only.
	 * @see #isReadOnly(String)
     */
    public void setValue(String name, Object value);
    
    /**
     * Removes a value by variable name.
     * This should fail if the provided name corresponds to a read-only variable. 
	 * @param name the variable name.
	 * @return true if the value existed and was removed, false otherwise.
	 * @throws IllegalArgumentException if the provided name refers to a value that is read-only.
     */
    public boolean clearValue(String name);
    
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

}
