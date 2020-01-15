/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.hostfunction;

import java.util.LinkedList;
import java.util.Queue;

import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;

/**
 * A host function resolver that is a combination of multiple resolvers.
 * Functions are resolved in the order that they are added to this resolver.
 * @author Matthew Tropiano
 */
public class MultiFunctionResolver implements ScriptFunctionResolver
{
	/** All added resolvers. */
	private Queue<ScriptFunctionResolver> globalResolvers;

	/**
	 * Creates a new MultiHostFunctionResolver.
	 */
	public MultiFunctionResolver()
	{
		this.globalResolvers = new LinkedList<>();
	}
	
	/**
	 * Adds a global resolver.
	 * If it has been added before to global, it is not added again.
	 * @param resolver the resolver to add.
	 * @return itself.
	 */
	public MultiFunctionResolver addResolver(ScriptFunctionResolver resolver)
	{
		if (!globalResolvers.contains(resolver))
			globalResolvers.add(resolver);
		return this;
	}
	
	/**
	 * Removes all resolvers (and all namespaced ones).
	 * @return itself.
	 */
	public MultiFunctionResolver clear()
	{
		globalResolvers.clear();
		return this;
	}
	
	@Override
	public boolean containsFunction(String name)
	{
		for (ScriptFunctionResolver r : globalResolvers)
			if (r.containsFunction(name))
				return true;
		return false;
	}

	@Override
	public ScriptFunctionType getFunction(String name)
	{
		for (ScriptFunctionResolver r : globalResolvers)
			if (r.containsFunction(name))
				return r.getFunction(name);
		return null;
	}

	@Override
	public ScriptFunctionType[] getFunctions()
	{
		LinkedList<ScriptFunctionType> list = new LinkedList<ScriptFunctionType>();
		for (ScriptFunctionResolver r : globalResolvers)
			for (ScriptFunctionType t : r.getFunctions())
				list.add(t);
		
		ScriptFunctionType[] out = new ScriptFunctionType[list.size()];
		list.toArray(out);
		return out;
	}
	
}
