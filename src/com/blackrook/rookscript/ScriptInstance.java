/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import com.blackrook.rookscript.Script.Entry;
import com.blackrook.rookscript.exception.ScriptExecutionException;
import com.blackrook.rookscript.exception.ScriptStackException;
import com.blackrook.rookscript.struct.ScriptInstanceStack;
import com.blackrook.rookscript.struct.ScriptValue;

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
	
	/** Script reference. */
	private Script script;
	/** Host interface reference. */
	private Object hostInterface;
	/** Script instance stack. */
	private ScriptInstanceStack scriptInstanceStack;
	/** The script's wait handler. */
	private ScriptWaitHandler waitHandler;

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
		this(script, scriptInstanceStack, null, hostInterface);
	}
	
	/**
	 * Creates a new script instance.
	 * @param script the script that holds the code.
	 * @param scriptInstanceStack the instance stack. 
	 * @param waitHandler the handler for handling a script in a waiting state (can be null).
	 * @param hostInterface the host interface object for host calls.
	 */
	public ScriptInstance(Script script, ScriptInstanceStack scriptInstanceStack, ScriptWaitHandler waitHandler, Object hostInterface)
	{
		this.script = script;
		this.scriptInstanceStack = scriptInstanceStack;
		this.hostInterface = hostInterface;
		this.waitHandler = waitHandler;
		
		this.state = State.CREATED;
		this.entryName = null;
		this.waitType = null;
		this.waitParameter = null;
		this.commandsExecuted = 0;
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
	 * @return the entry name that was used to start this script.
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
	 * Returns this script's host function resolver.
	 * @return the function resolver.
	 */
	public ScriptFunctionResolver getHostFunctionResolver()
	{
		return script.getHostFunctionResolver();
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
	 * Initializes the script at the main entry point.
	 */
	public void initialize()
	{
		this.state = State.INIT;
		this.waitType = null;
		this.waitParameter = null;
		this.scriptInstanceStack.reset();
		
		int mainIndex = script.getIndex(Script.LABEL_MAIN);
		if (mainIndex < 0)
			throw new ScriptExecutionException("Main entry point does not exist.");

		pushFrame(mainIndex);
	}
	
	/**
	 * Initializes the script with parameters.
	 * @param entryName the entry point name.
	 * @param parameters the starting parameters to push onto the stack.
	 * @throws ScriptExecutionException if the provided amount parameters do not match the amount of parameters that the script requires, 
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
	 * This also clears a wait state.
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
	 * Clears the local scopes.
	 */
	public void clearLocalScopes()
	{
		scriptInstanceStack.clearLocalScopes();
	}

	/**
	 * Pushes a new scope into the instance.
	 * This becomes the new local scope.
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 */
	public void pushLocalScope()
	{
		scriptInstanceStack.pushLocalScope();
	}

	/**
	 * Pops the most local context.
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public void popLocalScope()
	{
		scriptInstanceStack.popLocalScope();
	}

	/**
	 * Gets a corresponding script value by name.
	 * Only looks at the topmost scope.
	 * @param name the name of the variable.
	 * @return the value or null if no variable.
	 */
	public ScriptValue getValue(String name)
	{
		return scriptInstanceStack.getValue(name);
	}

	/**
	 * Sets a corresponding script value by name.
	 * If the value does not exist, it is set on the topmost scope in the stack.
	 * @param name the name of the variable.
	 * @param value the value to set.
	 */
	public <T> void setValue(String name, T value)
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
	 * Pushes the current command index onto the activation stack and sets the program counter.
	 * @param index the next script command index.
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 */
	public void pushCommandIndex(int index)
	{
		scriptInstanceStack.pushCommandIndex(index);
	}

	/**
	 * Pushes a new activation frame (local scope and command index).
	 * @param nextCommandIndex the next command index.
	 * @see #pushCommandIndex(int)
	 * @see #pushLocalScope()
	 * @throws ScriptStackException if this call would breach the stack capacity. 
	 */
	public void pushFrame(int nextCommandIndex)
	{
		scriptInstanceStack.pushFrame(nextCommandIndex);
	}
	
	/**
	 * Pops an activation frame (local scope and command index).
	 * @see #popCommandIndex()
	 * @see #popLocalScope()
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public void popFrame()
	{
		scriptInstanceStack.popFrame();
	}
	
	/**
	 * Restores a command index from the activation stack.
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public void popCommandIndex()
	{
		scriptInstanceStack.popCommandIndex();
	}

	/**
	 * Gets the command index depth.
	 * If 0, then this is 0 functions deep.
	 * @return the depth of the activation stack. 
	 */
	public int getCommandIndexDepth()
	{
		return scriptInstanceStack.getCommandIndexDepth();
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
		StringBuffer sb = new StringBuffer();
		sb.append(state);
		sb.append(" Commands:").append(' ').append(commandsExecuted);
		sb.append(" Index:").append(' ').append(scriptInstanceStack.getCommandIndex());
		sb.append(" Activation:").append(' ').append(scriptInstanceStack.getCommandIndexDepth());
		sb.append(" Stack:").append(' ').append(scriptInstanceStack.getValueStackDepth());
		return sb.toString();
	}
	
}
