/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import com.blackrook.rookscript.exception.ScriptStackException;

/**
 * The combined stack for a script instance.
 * @author Matthew Tropiano
 */
public class ScriptInstanceStack
{
	/** Script value stack. */
	private ScriptValue[] scriptValueStack;
	/** Script value stack top. */
	private int scriptValueStackTop;
	/** Activation stack. */
	private int[] activationStack;
	/** Activation stack top index. */
	private int activationStackTop;
	/** The local scope stack. */
	private ScriptVariableScope[] scopeStack;
	/** The local scope stack top. */
	private int scopeStackTop;

	/**
	 * Creates a new instance stack.
	 * @param activationDepth the activation depth to use (function calls).
	 * @param valueStackDepth the stack depth to use (script values).
	 */
	public ScriptInstanceStack(int activationDepth, int valueStackDepth)
	{
		expandScopeStack(activationDepth);
		expandActivationStack(activationDepth);
		expandValueStack(valueStackDepth);
		scriptValueStackTop = -1;
		activationStackTop = -1;
		scopeStackTop = -1;
		reset();
	}
	
	/**
	 * Resets the stack.
	 */
	public void reset()
	{
		clearLocalScopes();
		clearStackValues();
		activationStackTop = -1;
	}
	
	// Expands the scope stack.
	private void expandScopeStack(int capacity)
	{
		ScriptVariableScope[] newStack = new ScriptVariableScope[capacity];
		if (scopeStack != null)
			System.arraycopy(scopeStack, 0, newStack, 0, scopeStack.length);
		for (int i = scriptValueStack != null ? scriptValueStack.length : 0; i < newStack.length; i++)
			newStack[i] = new ScriptVariableScope();
		scopeStack = newStack;
	}
	
	// Expands the activation stack.
	private void expandActivationStack(int capacity)
	{
		int[] newStack = new int[capacity];
		if (activationStack != null)
			System.arraycopy(activationStack, 0, newStack, 0, activationStack.length);
		for (int i = activationStack != null ? activationStack.length : 0; i < newStack.length; i++)
			newStack[i] = 0;
		activationStack = newStack;
	}
	
	// Expands the value stack.
	private void expandValueStack(int capacity)
	{
		ScriptValue[] newStack = new ScriptValue[capacity];
		if (scriptValueStack != null)
			System.arraycopy(scriptValueStack, 0, newStack, 0, scriptValueStack.length);
		for (int i = scriptValueStack != null ? scriptValueStack.length : 0; i < newStack.length; i++)
			newStack[i] = ScriptValue.create(false);
		scriptValueStack = newStack;
	}
	
	/**
	 * Clears the local scopes.
	 */
	public void clearLocalScopes()
	{
		while (scopeStackTop >= 0)
			popLocalScope();
	}

	/**
	 * Pushes a new scope into the instance.
	 * This becomes the new local scope.
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 */
	public void pushLocalScope()
	{
		if (scopeStackTop + 1 >= scopeStack.length)
			throw new ScriptStackException("scope stack overflow");
		scopeStackTop++;
	}

	/**
	 * Pops the most local context.
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public void popLocalScope()
	{
		if (scopeStackTop < 0)
			throw new ScriptStackException("scope stack underflow");
		scopeStack[scopeStackTop].clear();
		scopeStackTop--;
	}

	/**
	 * Gets a corresponding script value by name.
	 * Only looks at the topmost scope.
	 * @param name the name of the variable.
	 * @return the value or null if no variable.
	 */
	public ScriptValue getValue(String name)
	{
		return scopeStack[scopeStackTop].get(name);
	}

	/**
	 * Sets a corresponding script value by name.
	 * If the value does not exist, it is set on the topmost scope in the stack.
	 * @param name the name of the variable.
	 * @param value the value to set.
	 */
	public <T> void setValue(String name, T value)
	{
		ScriptValue sv = getValue(name);
		if (sv != null)
		{
			sv.set(value);
			return;
		}
		
		scopeStack[scopeStackTop].set(name, value);
	}

	/**
	 * @return the current command index.
	 */
	public int getCommandIndex()
	{
		return activationStack[activationStackTop];
	}

	/**
	 * Sets the current command index (jump).
	 * @param index the new index.
	 */
	public void setCommandIndex(int index)
	{
		activationStack[activationStackTop] = index;
	}

	/**
	 * Increments the current command index and returns it.
	 * @return the new current index.
	 */
	public int incrementCommandIndex()
	{
		return ++activationStack[activationStackTop];
	}

	/**
	 * Pushes the current command index onto the activation stack and sets the program counter.
	 * @param index the next script command index.
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 */
	public void pushCommandIndex(int index)
	{
		if (activationStackTop + 1 >= activationStack.length)
			throw new ScriptStackException("activation stack overflow");
		activationStackTop++;
		setCommandIndex(index);
	}

	/**
	 * Restores a command index from the activation stack.
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public void popCommandIndex()
	{
		if (activationStackTop < 0)
			throw new ScriptStackException("activation stack underflow");
		activationStackTop--;
	}

	/**
	 * Gets the command index depth.
	 * If 0, then this is 0 functions deep.
	 * @return the depth of the activation stack. 
	 */
	public int getCommandIndexDepth()
	{
		return activationStackTop;
	}

	/**
	 * Pushes a new activation frame (local scope and command index).
	 * @param nextCommandIndex the next command index.
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 * @see #pushCommandIndex(int)
	 * @see #pushLocalScope()
	 */
	public void pushFrame(int nextCommandIndex)
	{
		pushCommandIndex(nextCommandIndex);
		pushLocalScope();
	}
	
	/**
	 * Pops an activation frame (local scope and command index).
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 * @see #popCommandIndex()
	 * @see #popLocalScope()
	 */
	public void popFrame()
	{
		popCommandIndex();
		popLocalScope();
	}
	
	/**
	 * Pushes a value onto the stack.
	 * @param value the value to push.
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 */
	public <T> void pushStackValue(T value)
	{
		if (scriptValueStackTop + 1 >= scriptValueStack.length)
			throw new ScriptStackException("value stack overflow");
		scriptValueStack[++scriptValueStackTop].set(value);
	}

	/**
	 * Pops a value off the stack.
	 * @return the value at the top of the stack, or null if none left.
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public ScriptValue popStackValue()
	{
		if (scriptValueStackTop < 0)
			throw new ScriptStackException("value stack underflow");
		return scriptValueStack[scriptValueStackTop--];
	}

	/**
	 * Gets a value on the stack (reference).
	 * @param depth the depth from the top (0 is top, 1 ... N is N places down).
	 * @return the value at the top of the stack, or null if none left.
	 * @throws ScriptStackException if the top minus the depth escapes the active stack bounds. 
	 */
	public ScriptValue getStackValue(int depth)
	{
		int d = scriptValueStackTop - depth;
		if (d < 0 || d > scriptValueStackTop)
			throw new ScriptStackException("nonexistant stack position");
		return scriptValueStack[scriptValueStackTop - depth];
	}
	
	/**
	 * Script stack depth.
	 * @return the current values stack depth.
	 */
	public int getValueStackDepth()
	{
		return scriptValueStackTop;
	}

	/**
	 * Clears the value stack.
	 */
	public void clearStackValues()
	{
		int prevCount = this.scriptValueStackTop;
		this.scriptValueStackTop = -1;
		// nullify object refs (to reduce chance of memory leaks).
		for (int i = 0; i < prevCount; i++)
			scriptValueStack[i].set(0L);
	}

}
