/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
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
import com.blackrook.rookscript.resolvers.ScriptHostFunctionResolver;
import com.blackrook.rookscript.struct.Utils;

/**
 * A host function resolver that is a combination of multiple resolvers.
 * Functions are resolved in the order that they are added to this resolver.
 * @author Matthew Tropiano
 */
public class MultiFunctionResolver implements ScriptHostFunctionResolver
{
	private Queue<ScriptFunctionResolver> resolvers;

	/**
	 * Creates a new MultiResolver using a list of resolvers.
	 * @param resolvers the list of resolvers.
	 */
	public MultiFunctionResolver(ScriptFunctionResolver ... resolvers)
	{
		this.resolvers = new LinkedList<>();
		for (ScriptFunctionResolver r : resolvers)
			this.resolvers.add(r);
	}
	
	/**
	 * Creates a new MultiResolver using an iterable list of resolvers.
	 * @param resolvers the list of resolvers.
	 */
	public MultiFunctionResolver(Iterable<ScriptFunctionResolver> resolvers)
	{
		this.resolvers = new LinkedList<>();
		for (ScriptFunctionResolver r : resolvers)
			this.resolvers.add(r);
	}
	
	@Override
	public boolean containsNamespacedFunction(String namespace, String name)
	{
		for (ScriptFunctionResolver r : resolvers)
			if (r.containsFunction(name))
				return true;
		return false;
	}

	@Override
	public ScriptFunctionType getNamespacedFunction(String namespace, String name)
	{
		for (ScriptFunctionResolver r : resolvers)
			if (r.containsFunction(name))
				return r.getFunction(name);
		return null;
	}
	
	@Override
	public ScriptFunctionType[] getFunctions()
	{
		int i = 0;
		ScriptFunctionType[][] resolverSets = new ScriptFunctionType[resolvers.size()][];
		for (ScriptFunctionResolver r : resolvers)
			resolverSets[i++] = r.getFunctions();
		return Utils.joinArrays(resolverSets);
	}

}
