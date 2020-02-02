/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.util.HashSet;
import java.util.Set;

import com.blackrook.rookscript.Script.Entry;
import com.blackrook.rookscript.exception.ScriptExecutionException;
import com.blackrook.rookscript.exception.ScriptStackException;
import com.blackrook.rookscript.lang.ScriptCommand;
import com.blackrook.rookscript.resolvers.ScriptHostFunctionResolver;
import com.blackrook.rookscript.resolvers.ScriptScopeResolver;
import com.blackrook.rookscript.resolvers.ScriptVariableResolver;
import com.blackrook.rookscript.struct.Utils;

/**
 * A single script instance.
 * @author Matthew Tropiano
 */
public class ScriptInstance
{
	private static final ThreadLocal<ScriptValue> CACHEVALUE = ThreadLocal.withInitial(()->ScriptValue.create(null));
	
	public static final int DEFAULT_RUNAWAY_LIMIT = 1024 * 1024;

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
	/** Script environment. */
	private ScriptEnvironment environment;
	/** Scope mapping. */
	private ScriptScopeResolver scopeResolver;
	/** Script instance stack. */
	private ScriptInstanceStack scriptInstanceStack;
	/** The script's wait handler. */
	private ScriptWaitHandler waitHandler;
	/** Pragma setting - runaway limit. */
	private int commandRunawayLimit;

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

	// ======================================================================
	// Resources
	// ======================================================================
	
	/** All registered, presumably unclosed resources. */
	private Set<AutoCloseable> closeables;
	
	/**
	 * Creates a new script instance, no wait handler, default runaway limit.
	 * @param script the script that holds the code.
	 * @param scriptInstanceStack the instance stack. 
	 * @param environment the script environment to use.
	 */
	public ScriptInstance(Script script, ScriptInstanceStack scriptInstanceStack, ScriptEnvironment environment)
	{
		this(script, scriptInstanceStack, NO_SCOPES, null, environment, DEFAULT_RUNAWAY_LIMIT);
	}
	
	/**
	 * Creates a new script instance, no wait handler, default runaway limit.
	 * @param script the script that holds the code.
	 * @param scriptInstanceStack the instance stack. 
	 * @param scopeResolver the scope resolver for this script.
	 * @param environment the script environment to use.
	 */
	public ScriptInstance(Script script, ScriptInstanceStack scriptInstanceStack, ScriptScopeResolver scopeResolver, ScriptEnvironment environment)
	{
		this(script, scriptInstanceStack, scopeResolver, null, environment, DEFAULT_RUNAWAY_LIMIT);
	}
	
	/**
	 * Creates a new script instance, no wait handler.
	 * @param script the script that holds the code.
	 * @param scriptInstanceStack the instance stack. 
	 * @param scopeResolver the scope resolver for this script.
	 * @param environment the script environment to use.
	 * @param runawayLimit the runaway script command limit. 0 or less is no limit.
	 */
	public ScriptInstance(Script script, ScriptInstanceStack scriptInstanceStack, ScriptScopeResolver scopeResolver, ScriptEnvironment environment, int runawayLimit)
	{
		this(script, scriptInstanceStack, scopeResolver, null, environment, runawayLimit);
	}
	
	/**
	 * Creates a new script instance.
	 * @param script the script that holds the code.
	 * @param scriptInstanceStack the instance stack. 
	 * @param scopeResolver the scope resolver for this script.
	 * @param waitHandler the handler for handling a script in a waiting state (can be null).
	 * @param environment the script environment to use.
	 * @param runawayLimit the runaway script command limit. 0 or less is no limit.
	 * @throws IllegalArgumentException if script or scriptInstanceStack
	 */
	public ScriptInstance(Script script, ScriptInstanceStack scriptInstanceStack, ScriptScopeResolver scopeResolver, ScriptWaitHandler waitHandler, ScriptEnvironment environment, int runawayLimit)
	{
		if (script == null)
			throw new IllegalArgumentException("script is null");
		if (scriptInstanceStack == null)
			throw new IllegalArgumentException("scriptInstanceStack is null");
		
		this.script = script;
		this.environment = environment;
		this.scriptInstanceStack = scriptInstanceStack;
		this.scopeResolver = scopeResolver;
		this.waitHandler = waitHandler;
		this.commandRunawayLimit = runawayLimit;

		reset();
	}
	
	/**
	 * Returns a new builder for creating a new script instance piece by piece.
	 * @return a new {@link ScriptInstanceBuilder} object.
	 */
	public static ScriptInstanceBuilder createBuilder()
	{
		return new ScriptInstanceBuilder();
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
		closeables = null;
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
	 * Returns this script's script environment.
	 * @return the scope resolver.
	 */
	public ScriptEnvironment getEnvironment()
	{
		return environment;
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
	public ScriptHostFunctionResolver getHostFunctionResolver()
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
	 * Sets the amount of commands that can be executed in one 
	 * update before the runaway detection is triggered.
	 * By default, this is 0, which means no detection.
	 * @param commandRunawayLimit the amount of commands.
	 */
	public void setCommandRunawayLimit(int commandRunawayLimit)
	{
		this.commandRunawayLimit = commandRunawayLimit;
	}

	/**
	 * Gets the amount of commands that can be executed in one 
	 * update before the runaway detection is triggered.
	 * By default, this is 0, which means no detection.
	 * @return the amount of commands.
	 */
	public int getCommandRunawayLimit()
	{
		return commandRunawayLimit;
	}
	
	/**
	 * Initializes the script with an entry point and parameters and calls {@link #update()} to execute it.
	 * The return value for the entry point should still be on the stack.
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
	 * If the amount of required parameters are less than the expected amount, nulls are pushed until the amount is met.
	 * @param <T> the return type.
	 * @param returnType the return type to get from the script.
	 * @param entryName the entry point name.
	 * @param parameters the starting parameters to push onto the stack.
	 * @return the returned value at the end of the script, converted to a specific class type.
	 * @throws ScriptExecutionException if the provided amount of parameters do not match the amount of parameters that the script requires, 
	 * 		or the provided entry point does not exist.
	 * @see #initialize(String, Object...)
	 * @see #update()
	 * @see #popStackValue(ScriptValue)
	 */
	public <T> T callAndReturnAs(Class<T> returnType, String entryName, Object ... parameters)
	{
		call(entryName, parameters);
		ScriptValue sv = CACHEVALUE.get();
		popStackValue(sv);
		T out = sv.createForType(returnType);
		sv.setNull();
		return out;
	}
	
	/**
	 * Initializes the script with parameters.
	 * If the amount of parameters are less than the expected amount, nulls are pushed until the amount is met.
	 * The method {@link #update()} can be called afterward.
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
	 * The method {@link #update()} can be called afterward.
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
	 * The method {@link #update()} can be called afterward.
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
	 * Executes the script.
	 * Note that the script may stop, but not terminate.
	 * @throws ScriptExecutionException if this instance is in {@link State#CREATED} state (init methods not called).
	 */
	public void update()
	{
		switch (state)
		{
			case CREATED:
				throw new ScriptExecutionException("Script not initialized.");
			case INIT:
				state = State.RUNNING;
				// fall through.
			case RUNNING:
			{
				// reset counter.
				commandsExecuted = 0;
				while (step())
				{
					commandsExecuted++;
					if (commandRunawayLimit > 0 && commandsExecuted > commandRunawayLimit)
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
	 * Sets the RUNNING state.
	 * This clears a wait state, if currently waiting.
	 * Does not actually run anything - this just sets the script's state as "enabled" for a future call to {@link #update()}.
	 * @see #update()
	 */
	public void resume()
	{
		this.state = State.RUNNING;
		this.waitType = null;
		this.waitParameter = null;
	}

	/**
	 * Sets the WAITING state and waiting parameters.
	 * A future call to {@link #update()} afterward will attempt to re-check and update the waiting state.
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
	 * Sets the SUSPENDED state.
	 * This clears a wait state, if currently waiting.
	 * A call to {@link #update()} afterward will do nothing.
	 */
	public void suspend()
	{
		this.state = State.SUSPENDED;
		this.waitType = null;
		this.waitParameter = null;
	}
	
	/**
	 * Sets the ENDED state.
	 * Also closes all registered closeables.
	 * This clears a wait state, if currently waiting.
	 * A future call to {@link #update()} afterward will do nothing.
	 * @see #registerCloseable(AutoCloseable)
	 */
	public void terminate()
	{
		this.state = State.ENDED;
		this.waitType = null;
		this.waitParameter = null;
		closeAllCloseables();
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
	 * Gets the activation (frame) depth.
	 * If 0, then this is 0 functions deep - the starting entry point.
	 * @return the depth of the activation stack. 
	 */
	public int getCurrentActivationStackDepth()
	{
		return scriptInstanceStack.getCurrentActivationStackDepth();
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
	 * @param out the output value.
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public void popStackValue(ScriptValue out)
	{
		scriptInstanceStack.popStackValue(out);
	}

	/**
	 * Pops a value off the stack, ignoring output.
	 * @throws ScriptStackException if there's nothing on the stack when this is called. 
	 */
	public void popStackValue()
	{
		scriptInstanceStack.popStackValue();
	}

	/**
	 * Gets a value on the stack (reference).
	 * @param depth the depth from the top (0 is top, 1 ... N is N places down).
	 * @param out the output value.
	 * @throws ScriptStackException if the top minus the depth escapes the active stack bounds. 
	 */
	public void getStackValue(int depth, ScriptValue out)
	{
		scriptInstanceStack.getStackValue(depth, out);
	}

	/**
	 * Clears the value stack.
	 */
	public void clearStackValues()
	{
		scriptInstanceStack.clearStackValues();
	}
	
	/**
	 * Registers a resource that will be automatically closed when this instance terminates.
	 * Function calls that open resources should call this.
	 * @param closeable the closeable to add to the instance registry.
	 */
	public void registerCloseable(AutoCloseable closeable)
	{
		if (closeables == null)
			closeables = new HashSet<AutoCloseable>();
		closeables.add(closeable);
	}
	
	/**
	 * Checks if a closeable resource is registered on this instance.
	 * @param closeable the closeable to look for.
	 * @return true if it is, false if it isn't.
	 */
	public boolean closeableIsRegistered(AutoCloseable closeable)
	{
		if (closeables == null)
			return false;
		return closeables.contains(closeable);
	}
	
	/**
	 * Unregisters a resource that should be closed when this instance terminates.
	 * Function calls that close resources should call this.
	 * @param closeable the closeable to remove from the instance registry.
	 */
	public void unregisterCloseable(AutoCloseable closeable)
	{
		if (closeables != null)
			closeables.remove(closeable);
	}
	
	/**
	 * Unregisters and closes all closeable resources that are registered on this instance.
	 * This is called when the instance enters the "terminated" state.
	 * @see #terminate()
	 */
	public void closeAllCloseables()
	{
		if (closeables != null) 
			for (AutoCloseable c : closeables)
				Utils.close(c);
		closeables = null;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(state);
		sb.append(" Commands:").append(' ').append(commandsExecuted);
		sb.append(" Index:").append(' ').append(scriptInstanceStack.getCommandIndex());
		sb.append(" Activation:").append(' ').append(scriptInstanceStack.getCurrentActivationStackDepth());
		sb.append(" Stack:").append(' ').append(scriptInstanceStack.getCurrentValueStackDepth());
		return sb.toString();
	}
	
}
