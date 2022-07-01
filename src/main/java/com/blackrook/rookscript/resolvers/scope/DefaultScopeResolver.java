/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.scope;

import java.util.Map;
import java.util.TreeMap;

import com.blackrook.rookscript.resolvers.ScriptScopeResolver;
import com.blackrook.rookscript.resolvers.ScriptVariableResolver;

/**
 * An single, scoped open variable set in which values can be set.
 * All variable names are CASE-INSENSITIVE.
 * The internals are written so that the storage uses few memory allocations/deletions.
 * None of the variables are read-only. This implementation is thread-safe.
 * @author Matthew Tropiano
 */
public class DefaultScopeResolver implements ScriptScopeResolver
{
	/** The scope map. */
	private Map<String, ScriptVariableResolver> scopeMap;
	
	/**
	 * Creates a new default scope resolver with no added scopes.
	 */
	public DefaultScopeResolver()
	{
		this.scopeMap = new TreeMap<String, ScriptVariableResolver>(String.CASE_INSENSITIVE_ORDER);
	}
	
	/**
	 * Adds a scope to this scope resolver by name.
	 * @param name the name of the scope.
	 * @param resolver the resolver to resolve by that name.
	 */
	public synchronized void addScope(String name, ScriptVariableResolver resolver)
	{
		scopeMap.put(name, resolver);
	}
	
	/**
	 * Clears this scope resolver of all added scope mappings.
	 */
	public synchronized void clear()
	{
		scopeMap.clear();
	}
	
	@Override
	public synchronized ScriptVariableResolver getScope(String name)
	{
		return scopeMap.get(name);
	}

	@Override
	public synchronized boolean containsScope(String name)
	{
		return scopeMap.containsKey(name);
	}

}
