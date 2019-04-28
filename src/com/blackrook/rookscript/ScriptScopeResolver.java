package com.blackrook.rookscript;

/**
 * An interface for structures that map string keys to
 * {@link ScriptVariableResolver}s. As a strict policy, all scope names are CASE-INSENSITIVE.
 * @author Matthew Tropiano
 */
public interface ScriptScopeResolver
{
	/**
	 * Gets the corresponding scope for a scope name.
	 * Changing the returned value, if any, changes the value in-place.
	 * @param name the scope name.
	 * @return the corresponding scope, or <code>null</code> if no corresponding scope.
	 */
    public ScriptVariableResolver getScope(String name);
    
    /**
     * Checks if this contains a scope by its scope name.
	 * @param name the scope name.
	 * @return true if so, false if not.
     */
    public boolean containsScope(String name);
    
    /**
     * Checks if an existing scope is read-only.
     * A non-existing scope returns <code>false</code>.
	 * @param name the scope name.
	 * @return true if so, false if not.
     */
    public boolean isReadOnly(String name);

}
