/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.hostfunction;

import java.util.HashMap;
import java.util.Map;

import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.ScriptHostFunctionResolver;

/**
 * A host function resolver that is a combination of multiple resolvers.
 * Functions are resolved in the order that they are added to this resolver.
 * @author Matthew Tropiano
 */
public class MultiHostFunctionResolver implements ScriptHostFunctionResolver
{
	/** Resolvers in the global namespace. */
	private MultiFunctionResolver globalResolver;
	/** Resolvers in the named namespaces. */
	private Map<String, ScriptFunctionResolver> namedResolvers;

	/**
	 * Creates a new MultiHostFunctionResolver.
	 */
	public MultiHostFunctionResolver()
	{
		this.globalResolver = new MultiFunctionResolver();
		this.namedResolvers = new HashMap<>();
	}
	
	/**
	 * Adds a global resolver.
	 * If it has been added before to global, it is not added again.
	 * @param resolver the resolver to add.
	 * @return itself.
	 */
	public MultiHostFunctionResolver addResolver(ScriptFunctionResolver resolver)
	{
		globalResolver.addResolver(resolver);
		return this;
	}
	
	/**
	 * Adds a resolver in a namespace.
	 * This will replace a namespace if it already had a resolver assigned.
	 * @param namespace the function namespace.
	 * @param resolver the resolver to add.
	 * @return itself.
	 */
	public MultiHostFunctionResolver addNamedResolver(String namespace, ScriptFunctionResolver resolver)
	{
		namedResolvers.put(namespace.toLowerCase(), resolver);
		return this;
	}
	
	/**
	 * Removes all resolvers (and all namespaced ones).
	 * @return itself.
	 */
	public MultiHostFunctionResolver clear()
	{
		globalResolver.clear();
		namedResolvers.clear();
		return this;
	}
	
	@Override
	public boolean containsNamespacedFunction(String namespace, String name)
	{
		ScriptFunctionResolver resolver;
		if (namespace == null)
			return globalResolver.containsFunction(name);
		else if ((resolver = namedResolvers.get(namespace.toLowerCase())) != null)
			return resolver.containsFunction(name);
		else
			return false;
	}

	@Override
	public ScriptFunctionType getNamespacedFunction(String namespace, String name)
	{
		ScriptFunctionResolver resolver;
		if (namespace == null)
			return globalResolver.getFunction(name);
		else if ((resolver = namedResolvers.get(namespace.toLowerCase())) != null)
			return resolver.getFunction(name);
		else
			return null;
	}
	
}
