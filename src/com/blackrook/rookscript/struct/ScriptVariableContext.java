/*******************************************************************************
 * Copyright (c) 2017-2018 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;

/**
 * An set of named scopes in which values can be set.
 * The internals are written so that the storage uses few memory allocations/deletions. 
 * @author Matthew Tropiano
 */
public class ScriptVariableContext
{
	/** The scope mapping. */
	private HashMap<String, ScriptVariableScope> scopeMap;
	
	/**
	 * Creates a context with a default size.
	 */
	public ScriptVariableContext()
	{
		this.scopeMap = new CaseInsensitiveHashMap<>();
	}
	
	/**
	 * Adds a new scope into the context.
	 * @param name the name to assign the scope.
	 */
	public void addScope(String name)
	{
		scopeMap.put(name, new ScriptVariableScope());
	}
	
	/**
	 * Removes a scope from the context.
	 * @param name the name of the scope to remove.
	 */
	public void removeScope(String name)
	{
		scopeMap.removeUsingKey(name);
	}
	
	/**
	 * Gets a variable scope by name.
	 * @param name the name assigned to the scope.
	 * @return the corresponding scope, or null if none by that name.
	 */
	public ScriptVariableScope getScope(String name)
	{
		return scopeMap.get(name);
	}

}