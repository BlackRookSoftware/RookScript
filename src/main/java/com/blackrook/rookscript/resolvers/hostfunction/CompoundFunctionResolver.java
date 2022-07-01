/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.hostfunction;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;

/**
 * A host function resolver that is a combination of multiple resolvers.
 * Functions are resolved in the order that they are added to this resolver.
 * @author Matthew Tropiano
 */
public class CompoundFunctionResolver implements ScriptFunctionResolver
{
	/** All added resolvers. */
	private SortedMap<String, ScriptFunctionType> functionMap;

	/**
	 * Creates a new MultiHostFunctionResolver.
	 */
	public CompoundFunctionResolver()
	{
		this.functionMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	}
	
	/**
	 * Adds a script function type.
	 * If a function has the same name as another function, this function overwrites the current mapping.
	 * @param functionType the function type to add. 
	 * @return itself.
	 */
	public CompoundFunctionResolver addFunction(ScriptFunctionType functionType)
	{
		functionMap.put(functionType.name(), functionType);
		return this;
	}
	
	/**
	 * Adds all of the functions in a single resolver.
	 * @param resolver the resolver to add.
	 * @return itself.
	 * @see #addFunction(ScriptFunctionType)
	 */
	public CompoundFunctionResolver addResolver(ScriptFunctionResolver resolver)
	{
		for (ScriptFunctionType t : resolver.getFunctions())
			addFunction(t);
		return this;
	}
	
	/**
	 * Removes all resolvers (and all namespaced ones).
	 * @return itself.
	 */
	public CompoundFunctionResolver clear()
	{
		functionMap.clear();
		return this;
	}
	
	@Override
	public boolean containsFunction(String name)
	{
		return functionMap.containsKey(name);
	}

	@Override
	public ScriptFunctionType getFunction(String name)
	{
		return functionMap.get(name);
	}

	@Override
	public ScriptFunctionType[] getFunctions()
	{
		LinkedList<ScriptFunctionType> list = new LinkedList<ScriptFunctionType>();
		for (Entry<String, ScriptFunctionType> r : functionMap.entrySet())
			list.add(r.getValue());
		
		ScriptFunctionType[] out = new ScriptFunctionType[list.size()];
		list.toArray(out);
		return out;
	}
	
}
