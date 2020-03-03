/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers;

import com.blackrook.rookscript.lang.ScriptFunctionType;

/**
 * Resolver encapsulation for finding a relevant host function by name and namespace.
 * @author Matthew Tropiano
 */
public interface ScriptHostFunctionResolver
{

	/**
	 * Attempts to find if this resolver has a function by its calling name and namespace.
	 * <p>A <code>null</code> namespace is valid, since it is a host call without declaring a namespace.
	 * <p>Names must be resolved case-insensitively!
	 * <p>If this returns <code>true</code> for a function name, {@link #getNamespacedFunction(String, String)} must NOT return null if called with that name!
	 * @param namespace the function namespace (null is valid!).
	 * @param name the name to find.
	 * @return a corresponding function or null for no such function.
	 * @see #getNamespacedFunction(String, String)
	 */
	boolean containsNamespacedFunction(String namespace, String name);

	/**
	 * Attempts to resolve a pertinent function by its calling name.
	 * <p>A <code>null</code> namespace is valid, since it is a host call without declaring a namespace.
	 * <p>Names must be resolved case-insensitively!
	 * <p>If this returns a non-null for a function name, {@link #containsNamespacedFunction(String, String)} must NOT return <code>false</code> if called with that name!
	 * <p>The same function must be returned if this is called again with the same name!
	 * @param namespace the function namespace (null is valid!).
	 * @param name the name to resolve.
	 * @return a corresponding function or null for no such function.
	 * @see #containsNamespacedFunction(String, String)
	 */
	ScriptFunctionType getNamespacedFunction(String namespace, String name);
	
}
