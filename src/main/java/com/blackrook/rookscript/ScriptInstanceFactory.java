/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Factory class for assembling script instances.
 * This factory pools {@link ScriptInstanceStack}s, since creating and destroying them would be costly on the GC.
 * @param <H> the host instance type.  
 * @author Matthew Tropiano
 */
public class ScriptInstanceFactory<H extends Object>
{
	public static final int DEFAULT_ACTIVATION_DEPTH = 16;
	public static final int DEFAULT_STACK_DEPTH = 512;
	
	/** The script to use for each instance. */
	private Script script;
	/** New instance activation depth. */
	private int activationDepth;
	/** New instance Stack depth. */
	private int stackDepth;
	/** Scope resolver to use with each instance. */
	private ScriptScopeResolver scopeResolver;
	/** Wait handler to use with each instance. */
	private ScriptWaitHandler waitHandler;
	/** The host interface to use for each instance. */
	private H hostInterface;

	/** Queue of available stacks. */
	private final Queue<ScriptInstanceStack> availableStacks;

	/**
	 * Creates a new instance factory, default depths.
	 * @param script the script to use for each instance.
	 * @param hostInterface the host to use for each instance.
	 * @see #DEFAULT_ACTIVATION_DEPTH
	 * @see #DEFAULT_STACK_DEPTH
	 */
	public ScriptInstanceFactory(Script script, H hostInterface)
	{
		this(script, DEFAULT_ACTIVATION_DEPTH, DEFAULT_STACK_DEPTH, ScriptInstance.NO_SCOPES, null, hostInterface);
	}

	/**
	 * Creates a new instance factory.
	 * @param script the script to use for each instance.
	 * @param activationDepth the activation stack depth for new instances.
	 * @param stackDepth the value stack depth for new instances.
	 * @param hostInterface the host to use for each instance.
	 */
	public ScriptInstanceFactory(Script script, int activationDepth, int stackDepth, H hostInterface)
	{
		this(script, activationDepth, stackDepth, ScriptInstance.NO_SCOPES, null, hostInterface);
	}
	
	/**
	 * Creates a new instance factory.
	 * @param script the script to use for each instance.
	 * @param activationDepth the activation stack depth for new instances.
	 * @param stackDepth the value stack depth for new instances.
	 * @param scopeResolver the scope resolver to use for each instance.
	 * @param hostInterface the host to use for each instance.
	 */
	public ScriptInstanceFactory(Script script, int activationDepth, int stackDepth, ScriptScopeResolver scopeResolver, H hostInterface)
	{
		this(script, activationDepth, stackDepth, scopeResolver, null, hostInterface);
	}
	
	/**
	 * Creates a new instance factory.
	 * @param script the script to use for each instance.
	 * @param activationDepth the activation stack depth for new instances.
	 * @param stackDepth the value stack depth for new instances.
	 * @param scopeResolver the scope resolver to use for each instance.
	 * @param waitHandler the wait handler to use for each instance.
	 * @param hostInterface the host to use for each instance.
	 */
	public ScriptInstanceFactory(Script script, int activationDepth, int stackDepth, ScriptScopeResolver scopeResolver, ScriptWaitHandler waitHandler, H hostInterface)
	{
		this.script = script;
		this.activationDepth = activationDepth;
		this.stackDepth = stackDepth;
		this.scopeResolver = scopeResolver;
		this.waitHandler = waitHandler;
		this.hostInterface = hostInterface;
		
		this.availableStacks = new LinkedList<>();
	}
	
	// Creates or fetches an existing stack.
	private ScriptInstanceStack acquireStack()
	{
		if (!availableStacks.isEmpty())
		{
			synchronized (availableStacks)
			{
				if (!availableStacks.isEmpty())
					return availableStacks.poll();
			}
		}
		
		return new ScriptInstanceStack(activationDepth, stackDepth);
	}
	
	/**
	 * Creates a new instance.
	 * @return a new instance with all of the associated resolvers and handlers attached to it.
	 */
	public ScriptInstance create()
	{
		return new ScriptInstance(script, acquireStack(), scopeResolver, waitHandler, hostInterface);
	}
	
	/**
	 * Creates a new instance.
	 * @param hostInterface the host interface override.
	 * @return a new instance with all of the associated resolvers and handlers attached to it.
	 */
	public ScriptInstance create(H hostInterface)
	{
		return new ScriptInstance(script, acquireStack(), scopeResolver, waitHandler, hostInterface);
	}
	
	/**
	 * Destroys an instance and releases pooled objects.
	 * @param instance the script instance.
	 */
	public void release(ScriptInstance instance)
	{
		ScriptInstanceStack stack = instance.getScriptInstanceStack();
		stack.reset();
		synchronized (availableStacks)
		{
			availableStacks.add(stack);
		}
	}
	
	/**
	 * Gets how many stacks were created but not in use.
	 * @return the amount of created stacks.
	 */
	public int getFreeStackCount()
	{
		return availableStacks.size();
	}
	
}
