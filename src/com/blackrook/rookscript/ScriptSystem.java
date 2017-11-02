/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import com.blackrook.commons.ResettableIterator;
import com.blackrook.commons.linkedlist.Queue;

/**
 * A script system for defining a scripting environment.
 * @param <H> the host instance object type.
 * @author Matthew Tropiano
 */
public class ScriptSystem<H extends Object>
{
	/** Instance factory. */
	private ScriptInstanceFactory<H> scriptInstanceFactory;
	
	/** Active instance queue. */
	private Queue<ScriptInstance> instanceQueue; 
	/** Active instance queue iterator. */
	private ResettableIterator<ScriptInstance> instanceQueueIterator;

	/**
	 * Creates a new script system.
	 * @param scriptInstanceFactory the factory to use for creating instance factories.
	 */
	public ScriptSystem(ScriptInstanceFactory<H> scriptInstanceFactory)
	{
		this.scriptInstanceFactory = scriptInstanceFactory;
		this.instanceQueue = new Queue<>();
		this.instanceQueueIterator = instanceQueue.iterator();
	}
	
	// TODO: Finish this.
	
	
}
