/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.function;

import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.util.ArrayUtils;
import com.blackrook.rookscript.ScriptFunctionResolver;
import com.blackrook.rookscript.ScriptFunctionType;

/**
 * A host function resolver that is a combination of multiple resolvers.
 * Functions are resolved in the order that they are added to this resolver.
 * @author Matthew Tropiano
 */
public class MultiFunctionResolver implements ScriptFunctionResolver
{
	private Queue<ScriptFunctionResolver> resolvers;

	/**
	 * Creates a new MultiResolver using a list of resolvers.
	 * @param resolvers the list of resolvers.
	 */
	public MultiFunctionResolver(ScriptFunctionResolver ... resolvers)
	{
		this.resolvers = new Queue<>();
		for (ScriptFunctionResolver r : resolvers)
			this.resolvers.add(r);
	}
	
	/**
	 * Creates a new MultiResolver using an iterable list of resolvers.
	 * @param resolvers the list of resolvers.
	 */
	public MultiFunctionResolver(Iterable<ScriptFunctionResolver> resolvers)
	{
		this.resolvers = new Queue<>();
		for (ScriptFunctionResolver r : resolvers)
			this.resolvers.add(r);
	}
	
	@Override
	public boolean containsFunctionByName(String name)
	{
		for (ScriptFunctionResolver r : resolvers)
			if (r.containsFunctionByName(name))
				return true;
		return false;
	}

	@Override
	public ScriptFunctionType getFunctionByName(String name)
	{
		for (ScriptFunctionResolver r : resolvers)
			if (r.containsFunctionByName(name))
				return r.getFunctionByName(name);
		return null;
	}
	
	@Override
	public ScriptFunctionType[] getFunctions()
	{
		int i = 0;
		ScriptFunctionType[][] resolverSets = new ScriptFunctionType[resolvers.size()][];
		for (ScriptFunctionResolver r : resolvers)
			resolverSets[i++] = r.getFunctions();
		return ArrayUtils.joinArrays(resolverSets);
	}

}
