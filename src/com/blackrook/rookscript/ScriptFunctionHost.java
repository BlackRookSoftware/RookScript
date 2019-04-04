/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

/**
 * The main interface for all resolvable host functions.
 * This is what the scripts use to resolve global and namespace organized host functions.
 * @author Matthew Tropiano
 */
public interface ScriptFunctionHost
{
	/**
	 * Attempts to find if this resolver has a function by its calling name and namespace.
	 * <p>Names must be resolved case-insensitively!
	 * <p>If this returns <code>true</code> for a function name and namespace, {@link #getFunctionByName(String, String)} 
	 * must NOT return null if called with that name and namespace!
	 * @param name the name to find.
	 * @param namespace the function namespace to use (or null for the global namespace).
	 * @return a corresponding function or null for no such function.
	 */
	public boolean containsFunctionByName(String name, String namespace);

	/**
	 * Attempts to resolve a pertinent function by its calling name and namespace.
	 * <p>Names must be resolved case-insensitively!
	 * <p>If this returns a non-null for a function name and namespace, {@link #containsFunctionByName(String, String)} 
	 * must NOT return <code>false</code> if called with that name and namespace!
	 * <p>The same function must be returned if this is called again with the same name!
	 * @param name the name to resolve.
	 * @param namespace the function namespace to use (or null for the global namespace).
	 * @return a corresponding function or null for no such function.
	 * @see #containsFunctionByName(String, String)
	 */
	public ScriptFunctionType getFunctionByName(String name, String namespace);
	
	/**
	 * Gets the full list of all script functions in a namespace.
	 * @param namespace the function namespace to use (or null for the global namespace).
	 * @return a list of all functions.
	 */
	public ScriptFunctionType[] getFunctions(String namespace);
	
}
