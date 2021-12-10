/*******************************************************************************
 * Copyright (c) 2017-2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers;

/**
 * An interface for structures that map string keys to
 * {@link ScriptVariableResolver}s. As a strict policy, all scope names are CASE-INSENSITIVE.
 * @author Matthew Tropiano
 */
public interface ScriptScopeResolver
{
	/**
	 * A scope resolver with no scopes.
	 * @since 1.8.0
	 */
	static final ScriptScopeResolver EMPTY = new ScriptScopeResolver()
	{
		@Override
		public ScriptVariableResolver getScope(String name)
		{
			return null;
		}
		
		@Override
		public boolean containsScope(String name)
		{
			return false;
		}
	};
	
	/**
	 * Gets the corresponding scope for a scope name.
	 * @param name the scope name.
	 * @return the corresponding scope, or <code>null</code> if no corresponding scope.
	 */
    ScriptVariableResolver getScope(String name);
    
    /**
     * Checks if this contains a scope by its scope name.
	 * @param name the scope name.
	 * @return true if so, false if not.
     */
    boolean containsScope(String name);
    
}
