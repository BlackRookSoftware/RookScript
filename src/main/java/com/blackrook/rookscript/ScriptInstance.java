/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import com.blackrook.rookscript.Script.Entry;
import com.blackrook.rookscript.exception.ScriptExecutionException;
import com.blackrook.rookscript.exception.ScriptStackException;

/**
 * A single script instance.
 * @author Matthew Tropiano
 */
public class ScriptInstance
{
	/**
	 * Enumeration of script states.
	 */
	public static enum State
	{
		/** Created, not executed yet. */
		CREATED,
		/** Not executed yet. */
		INIT,
		/** Currently running. */
		RUNNING,
		/** Waiting on some kind of action to complete. */
		WAITING,
		/** Manually suspended. */
		SUSPENDED,
		/** Ended - awaiting cleanup. */
		ENDED;
	}
	
	/**
	 * A scope resolver with no scopes.
	 */
	public static final ScriptScopeResolver NO_SCOPES = new ScriptScopeResolver()
	{
		@Override
		public ScriptVariableResolver getScope(String name)
		{
			return null;
		}
		
		@Override
		public boolean containsScope(String name)
		{
			return false;
		}
	};
	
	// ======================================================================
	// Environment
	// ======================================================================

	/** Script reference. */
	private Script script;
	/** Scope mapping. */
	private ScriptScopeResolver scopeResolver;
	/** Host interface reference. */
	private Object hostInterface;
	/** Script instance stack. */
	private ScriptInstanceStack scriptInstanceStack;
	/** The script's wait handler. */
	private ScriptWaitHandler waitHandler;

	// ======================================================================
	// State
	// ======================================================================
	
	/** Current script state. */
	private State state;
	/** Starting entry name. */
	private String entryName;
	/** Waiting type. */
	private Object waitType;
	/** Waiting parameter. */
	private Object waitParameter;
	/** Commands executed per slice. */
	private int commandsExecuted;
	
	/**
	 * Creates a new script instance, no wait handler.
	 * @param script the script that holds the code.
	 * @param scriptInstanceStack the instance stack. 
	 * @param hostInterface the host interface object for host calls.
	 */
	public ScriptInstance(Script script, ScriptInstanceStack scriptInstanceStack, Object hostInterface)
	{
		this(script, scriptInstanceStack, NO_SCOPES, null, hostInterface);
	}
	
	/**
	 * Creates a new script instance, no wait handler.
	 * @param script the script that holds the code.
	 * @param scriptInstanceStack the instance stack. 
	 * @param scopeResolver the scope resolver for this script.
	 * @param hostInterface the host interface object for host calls.
	 */
	public ScriptInstance(Script script, ScriptInstanceStack scriptInstanceStack, ScriptScopeResolver scopeResolver, Object hostInterface)
	{
		this(script, scriptInstanceStack, scopeResolver, null, hostInterface);
	}
	
	/**
	 * Creates a new script instance.
	 * @param script the script that holds the code.
	 * @param scriptInstanceStack the instance stack. 
	 * @param scopeResolver the scope resolver for this script.
	 * @param waitHandler the handler for handling a script in a waiting state (can be null).
	 * @param hostInterface the host interface object for host calls.
	 * @throws IllegalArgumentException if script or scriptInstanceStack
	 */
	public ScriptInstance(Script script, ScriptInstanceStack scriptInstanceStack, ScriptScopeResolver scopeResolver, ScriptWaitHandler waitHandler, Object hostInterface)
	{
		if (script == null)
			throw new IllegalArgumentException("script is null");
		if (scriptInstanceStack == null)
			throw new IllegalArgumentException("scriptInstanceStack is null");
		
		this.script = script;
		this.scriptInstanceStack = scriptInstanceStack;
		this.scopeResolver = scopeResolver;
		this.hostInterface = hostInterface;
		this.waitHandler = waitHandler;
		
		this.state = State.CREATED;
		this.entryName = null;
		this.waitType = null;
		this.waitParameter = null;
		this.commandsExecuted = 0;
	}
	
	/**
	 * Returns a new builder for creating a new script instance piece by piece.
	 * @return a new {@link ScriptBuilder} object.
	 */
	public static ScriptBuilder build()
	{
		return new ScriptBuilder();
	}
	
	/**
	 * Resets the instance.
	 */
	public void reset()
	{
		scriptInstanceStack.reset();
		state = State.CREATED;
		entryName = null;
		waitType = null;
		waitParameter = null;
		commandsExecuted = 0;
	}

	/**
	 * Gets this instance's script reference.
	 * @return the script.
	 */
	public Script getScript()
	{
		return script;
	}
	
	/**
	 * @return the current script execution state. 
	 */
	public State getState()
	{
		return state;
	}
	
	/**
	 * Gets the entry name that was used to start this script.
	 * Can be null if not started from an entry.
	 * @return the entry name that was used to start this script or null. 
	 */
	public String getEntryName()
	{
		return entryName;
	}
	
	/**
	 * Gets the host interface that this instance uses
	 * for host calls.
	 * @return the instance to use.
	 */
	public Object getHostInterface()
	{
		return hostInterface;
	}
	
	/**
	 * Returns this script's scope resolver.
	 * @return the scope resolver.
	 */
	public ScriptScopeResolver getScopeResolver()
	{
		return scopeResolver;
	}
	
	/**
	 * Returns this script's host function resolver.
	 * @return the function resolver.
	 */
	public ScriptFunctionResolver getFunctionResolver()
	{
		return script.getFunctionResolver();
	}
	
	/**
	 * Gets the instance stack on this instance.
	 * @return the instance stack.
	 */
	ScriptInstanceStack getScriptInstanceStack()
	{
		return scriptInstanceStack;
	}

	/**
	 * Initializes the script with an entry point and parameters and calls {@link #update()} to execute it.
	 * @param entryName the entry point name.
	 * @param parameters the starting parameters to push onto the stack.
	 * @throws ScriptExecutionException if the provided amount of parameters do not match the amount of parameters that the script requires, 
	 * 		or the provided entry point does not exist.
	 * @see #initialize(String, Object...)
	 * @see #update()
	 */
	public void call(String entryName, Object ... parameters)
	{
		initialize(entryName, parameters);
		update();
	}
	
	/**
	 * Initializes the script with an entry point and parameters and calls {@link #update()} to execute it,
	 * then gets the return value off the stack converted to a provided type.
	 * @param <T> the return type.
	 * @param returnType the return type to get from the script.
	 * @param entryName the entry point name.
	 * @param parameters the starting parameters to push onto the stack.
	 * @return the returned value at the end of the script, converted to a specific class type.
	 * @throws ScriptExecutionException if the provided amount of parameters do not match the amount of parameters that the script requires, 
	 * 		or the provided entry point does not exist.
	 * @see #initialize(String, Object...)
	 * @see #update()
	 * @see #popStackValue()
	 */
	public <T> T callAndReturnAs(Class<T> returnType, String entryName, Object ... parameters)
	{
		call(entryName, parameters);
		return popStackValue().createForType(returnType);
	}
	
	/**
	 * Initializes the script with parameters.
	 * @param entryName the entry point name.
	 * @param parameters the starting parameters to push onto the stack.
	 * @throws ScriptExecutionException if the provided amount of parameters do not match the amount of parameters that the script requires, 
	 * 		or the provided entry point does not exist.
	 */
	public void initialize(String entryName, Object ... parameters)
	{
		this.state = State.INIT;
		this.waitType = null;
		this.waitParameter = null;
		this.scriptInstanceStack.reset();
		
		Entry entry = script.getScriptEntry(entryName);
		if (entry == null)
			throw new ScriptExecutionException("Entry point \""+entryName+"\" does not exist.");
		
		this.entryName = entryName;	
		
		pushFrame(entry.getIndex());
		
		int entryParamCount = entry.getParameterCount();
		if (parameters.length > entryParamCount)
			throw new ScriptExecutionException("Attempt to initialize script with too many parameters. Requires "+entry.getParameterCount()+", saw "+parameters.length);
		
		int leftover = entryParamCount - parameters.length;
		for (int i = 0; i < parameters.length; i++)
			pushStackValue(parameters[i]);
		while (leftover-- > 0)
			pushStackValue(null);
	}

	/**
	 * Initializes the script at an arbitrary label.
	 * Use with caution - this is assuming manual setup of a script instance.
	 * @param labelName the script label name.
	 * @throws ScriptExecutionException if the provided label does not exist.
	 */
	public void initializeLabel(String labelName)
	{
		int index = script.getIndex(labelName);
		if (index < 0)
			throw new ScriptExecutionException("Script label \""+labelName+"\" does not exist.");
		
		initializeIndex(index);
	}
	
	/**
	 * Initializes the script at an arbitrary index.
	 * Use with caution - this is assuming manual setup of a script instance.
	 * @param index the command index.
	 * @throws ScriptExecutionException if the provided index is out of script command bounds.
	 */
	public void initializeIndex(int index)
	{
		this.state = State.INIT;
		this.waitType = null;
		this.waitParameter = null;
		this.scriptInstanceStack.reset();

		if (index < 0 || index >= script.getCommandCount())
			throw new ScriptExecutionException("Script index \""+index+"\" is out of bounds.");
		
		pushFrame(index);
	}
	
	/**
	 * Executes the script.
	 * Note that the script may stop, but not terminate.
	 */
	public void update()
	{
		switch (state)
		{
			case CREATED:
				throw new ScriptExecutionException("Script not initialized.");
			case INIT:
			case RUNNING:
			{
				// reset counter.
				commandsExecuted = 0;
				while (step())
				{
					commandsExecuted++;
					if (script.getCommandRunawayLimit() > 0 && commandsExecuted > script.getCommandRunawayLimit())
						throw new ScriptExecutionException("Script runaway triggered. Possible infinite loop. "+commandsExecuted+" commands executed.");
				}
				break;
			}
			case ENDED:
			case SUSPENDED:
				break;
			case WAITING:
			{
				if (waitHandler == null)
					break;
				if (!waitHandler.waitingScriptCanContinue(this, waitType, waitParameter))
					waitHandler.waitingScriptUpdate(this, waitType, waitParameter);
				break;
			}
			
		}
		
	}
	
	/**
	 * Makes a single command step in the script.
	 * @return false if the script should halt, true to continue.
	 */
	public boolean step()
	{
		int index = getCurrentCommandIndex();
		ScriptCommand command = script.getCommand(index);
		setCurrentCommandIndex(index + 1);
		if (command == null)
		{
			terminate();
			return false;
		}
		else
			return command.execute(this);
	}
	
	/**
	 * Sets the ENDED state.
	 * This clears a wait state, if currently waiting.
	 */
	public void terminate()
	{
		this.state = State.ENDED;
		this.waitType = null;
		this.waitParameter = null;
	}
	
	/**
	 * Sets the SUSPENDED state.
	 * This clears a wait state, if currently waiting.
	 */
	public void suspend()
	{
		this.state = State.SUSPENDED;
		this.waitType = null;
		this.waitParameter = null;
	}
	
	/**
	 * Sets the RUNNING state.
	 * This clears a wait state, if currently waiting.
	 */
	public void resume()
	{
		this.state = State.RUNNING;
		this.waitType = null;
		this.waitParameter = null;
	}
	
	/**
	 * Sets the WAITING state and waiting parameters.
	 * @param waitType the type for the wait (seen by the waiting handler).
	 * @param waitParameter the parameter for the wait (seen by the waiting handler).
	 */
	public void wait(Object waitType, Object waitParameter)
	{
		this.state = State.WAITING;
		this.waitType = waitType;
		this.waitParameter = waitParameter;
	}
	
	/**
	 * Gets a command index by label.
	 * @param labelName the label name.
	 * @return the corresponding index, or -1 for no index.
	 */
	public int getCommandIndex(String labelName)
	{
		return script.getIndex(labelName);
	}

	/**
	 * Gets the current command.
	 * @return the current command or null if current index is outside script bounds.
	 */
	public ScriptCommand getCurrentCommand()
	{
		return script.getCommand(scriptInstanceStack.getCommandIndex());
	}

	/**
	 * Gets a corresponding script value by name.
	 * Only looks at the topmost stack scope.
	 * Changing the returned value does not change the value, unless it is a reference type
	 * like a map or list.
	 * @param name the variable name.
	 * @param out the destination variable for the value.
	 * @return true if a corresponding value was fetched into out, false if not. If false, out is set to the null value.
	 */
	public boolean getValue(String name, ScriptValue out)
	{
		return scriptInstanceStack.getValue(name, out);
	}

	/**
	 * Sets a corresponding script value by name.
	 * If the value does not exist, it is set on the topmost stack scope in the stack.
	 * @param name the name of the variable.
	 * @param value the value to set.
	 */
	public void setValue(String name, ScriptValue value)
	{
		scriptInstanceStack.setValue(name, value);
	}

	/**
	 * @return the current command index.
	 */
	public int getCurrentCommandIndex()
	{
		return scriptInstanceStack.getCommandIndex();
	}

	/**
	 * Sets the current command index (jump).
	 * @param index the new index.
	 */
	public void setCurrentCommandIndex(int index)
	{
		scriptInstanceStack.setCommandIndex(index);
	}

	/**
	 * Increments the current command index and returns it.
	 * @return the new current index.
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 */
	public int incrementCurrentCommandIndex()
	{
		return scriptInstanceStack.incrementCommandIndex();
	}

	/**
	 * Pushes a new activation frame (local scope and command index).
	 * @param nextCommandIndex the next command index.
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 */
	public void pushFrame(int nextCommandIndex)
	{
		scriptInstanceStack.pushFrame(nextCommandIndex);
	}
	
	/**
	 * Pops an activation frame (local scope and command index).
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public void popFrame()
	{
		scriptInstanceStack.popFrame();
	}
	
	/**
	 * Gets the frame depth.
	 * If 0, then this is 0 functions deep - the starting entry point.
	 * @return the depth of the activation stack. 
	 */
	public int getFrameDepth()
	{
		return scriptInstanceStack.getFrameDepth();
	}

	/**
	 * Pushes a value onto the stack.
	 * @param value the value to push.
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 */
	public <T> void pushStackValue(T value)
	{
		scriptInstanceStack.pushStackValue(value);
	}

	/**
	 * Pops a value off the stack.
	 * @return the value at the top of the stack, or null if none left.
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public ScriptValue popStackValue()
	{
		return scriptInstanceStack.popStackValue();
	}

	/**
	 * Gets a value on the stack (reference).
	 * @param depth the depth from the top (0 is top, 1 ... N is N places down).
	 * @return the value at the top of the stack, or null if none left.
	 * @throws ScriptStackException if the top minus the depth escapes the active stack bounds. 
	 */
	public ScriptValue getStackValue(int depth)
	{
		return scriptInstanceStack.getStackValue(depth);
	}

	/**
	 * Clears the value stack.
	 */
	public void clearStackValues()
	{
		scriptInstanceStack.clearStackValues();
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(state);
		sb.append(" Commands:").append(' ').append(commandsExecuted);
		sb.append(" Index:").append(' ').append(scriptInstanceStack.getCommandIndex());
		sb.append(" Activation:").append(' ').append(scriptInstanceStack.getFrameDepth());
		sb.append(" Stack:").append(' ').append(scriptInstanceStack.getValueStackDepth());
		return sb.toString();
	}
	
}