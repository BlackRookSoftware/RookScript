/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.rookscript.struct.ScriptInstanceStack;

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
	
	/** New instance Stack depth. */
	private int activationDepth;
	/** New instance Stack depth. */
	private int stackDepth;

	/** Queue of available stacks. */
	private final Queue<ScriptInstanceStack> availableStacks;

	/**
	 * Creates a new instance factory, default depths.
	 * @see #DEFAULT_ACTIVATION_DEPTH
	 * @see #DEFAULT_STACK_DEPTH
	 */
	public ScriptInstanceFactory()
	{
		this(DEFAULT_ACTIVATION_DEPTH, DEFAULT_STACK_DEPTH);
	}
	
	/**
	 * Creates a new instance factory.
	 * @param activationDepth the activation stack depth for new instances.
	 * @param stackDepth the value stack depth for new instances.
	 */
	public ScriptInstanceFactory(int activationDepth, int stackDepth)
	{
		this.activationDepth = activationDepth;
		this.stackDepth = stackDepth;
		
		this.availableStacks = new Queue<>();
	}
	
	// Creates or fetches an existing stack.
	private ScriptInstanceStack acquireStack()
	{
		if (!availableStacks.isEmpty())
		{
			synchronized (availableStacks)
			{
				if (!availableStacks.isEmpty())
					return availableStacks.dequeue();
			}
		}
		
		return new ScriptInstanceStack(activationDepth, stackDepth);
	}
	
	/**
	 * Creates a new instance.
	 * @param script the script to instantiate.
	 * @param waitHandler the waiting handler to use. 
	 * @param hostInterface the host interface object.
	 * @return a new instance.
	 */
	public ScriptInstance create(Script script, ScriptWaitHandler waitHandler, H hostInterface)
	{
		return new ScriptInstance(script, acquireStack(), waitHandler, hostInterface);
	}
	
	/**
	 * Destroys an instance and releases pooled objects.
	 * @param instance the script instance.
	 */
	public void destroy(ScriptInstance instance)
	{
		ScriptInstanceStack stack = instance.getScriptInstanceStack();
		stack.reset();
		synchronized (availableStacks)
		{
			availableStacks.enqueue(stack);
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
