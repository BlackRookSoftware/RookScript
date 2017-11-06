/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;

/**
 * A compiled script.
 * @author Matthew Tropiano
 */
public class Script
{
	/** Command runaway. */
	public static final int DEFAULT_COMMAND_RUNAWAY_LIMIT = 500000;
	/** Function label prefix. */
	public static final String LABEL_FUNCTION_PREFIX = "function_";
	/** Script label prefix. */
	public static final String LABEL_ENTRY_PREFIX = "entry_";
	/** Init label. */
	public static final String LABEL_MAIN = "main";

	/** Script host function resolver. */
	private ScriptFunctionResolver hostFunctionResolver;
	/** List of script commands. */
	private ScriptCommand[] commands;

	/** Script entry name map (name to index). */
	private CaseInsensitiveHashMap<Entry> scriptLabelMap;
	/** Label map (label to index). */
	private HashMap<String, Integer> labelMap;

	/** Reverse lookup map (index to labels - for debug). Transient. */
	private HashedQueueMap<Integer, String> indexMap;
	
	/** Pragma setting - runaway limit. */
	private int commandRunawayLimit;
	
	/**
	 * Creates a new empty script.
	 */
	public Script()
	{
		this.hostFunctionResolver = null;
		this.commands = null;
		this.scriptLabelMap = new CaseInsensitiveHashMap<>();
		this.labelMap = new HashMap<>();
		this.indexMap = null;
		
		this.commandRunawayLimit = DEFAULT_COMMAND_RUNAWAY_LIMIT;
	}
	
	/**
	 * Sets this script's host function resolver.  
	 * @param hostFunctionResolver the function resolver.
	 */
	public void setHostFunctionResolver(ScriptFunctionResolver hostFunctionResolver)
	{
		this.hostFunctionResolver = hostFunctionResolver;
	}
	
	/**
	 * Sets the commands to use in the script.
	 * @param commands the command set.
	 */
	public void setCommands(ScriptCommand[] commands)
	{
		this.commands = commands;
	}

	/**
	 * Sets the amount of commands that can be executed in one 
	 * update before the runaway detection is triggered.
	 * @param commandRunawayLimit the amount of commands.
	 * @see #DEFAULT_COMMAND_RUNAWAY_LIMIT
	 */
	public void setCommandRunawayLimit(int commandRunawayLimit)
	{
		this.commandRunawayLimit = commandRunawayLimit;
	}

	/**
	 * Gets the amount of commands that can be executed in one 
	 * update before the runaway detection is triggered.
	 * @return the amount of commands.
	 * @see #DEFAULT_COMMAND_RUNAWAY_LIMIT
	 */
	public int getCommandRunawayLimit()
	{
		return commandRunawayLimit;
	}
	
	/**
	 * Sets an index for a subscript entry name in the script.
	 * Entry names are case-insensitive.
	 * @param name the name.
	 * @param parameterCount the amount of parameters that this takes.
	 * @param index the corresponding index.
	 */
	public void setScriptEntry(String name, int parameterCount, int index)
	{
		scriptLabelMap.put(name, new Entry(parameterCount, index));
	}

	/**
	 * Gets the corresponding index for a subscript entry name.
	 * Entry names are case-insensitive.
	 * @param name the name to look up.
	 * @return the corresponding index or -1 if not found.
	 */
	public Entry getScriptEntry(String name)
	{
		return scriptLabelMap.get(name);
	}

	/**
	 * Sets an index for a label in the script.
	 * @param label the label name.
	 * @param index the corresponding index.
	 */
	public void setIndex(String label, int index)
	{
		if (indexMap != null && labelMap.containsKey(label))
			indexMap.removeValue(labelMap.get(label), label);
		labelMap.put(label, index);
		if (indexMap != null)
			indexMap.enqueue(index, label);
	}
	
	/**
	 * Gets the corresponding index for a label.
	 * @param label the label to look up.
	 * @return the corresponding index or -1 if not found.
	 */
	public int getIndex(String label)
	{
		Integer out;
		if ((out = labelMap.get(label)) == null)
			return -1;
		else
			return out;
	}
	
	/**
	 * Returns this script's host function resolver.
	 * @return the function resolver.
	 */
	public ScriptFunctionResolver getHostFunctionResolver()
	{
		return hostFunctionResolver;
	}

	/**
	 * Gets the command at a specific index in the script.
	 * @param index the index of the command.
	 * @return the corresponding command, or null if out of range.
	 */
	public ScriptCommand getCommand(int index)
	{
		return index < 0 || index >= commands.length ? null : commands[index]; 
	}
	
	/**
	 * Gets the amount of commands in this script.
	 * @return the amount of commands.
	 */
	public int getCommandCount()
	{
		return commands.length;
	}
	
	/**
	 * Creates the reverse lookup.
	 * Only valuable on debug, so this is not created at first instantiation to save memory.
	 */
	private void createReverseIndexLookup()
	{
		if (indexMap != null)
			return;
		indexMap = new HashedQueueMap<>();
		for (ObjectPair<String, Integer> p : labelMap)
			indexMap.enqueue(p.getValue(), p.getKey());
	}
	
	/**
	 * Returns the list of labels at an index.
	 * Creates a reverse lookup if this is the first time called.
	 * @param index the index to look up.
	 * @return an iterable structure with labels, or null for no labels.
	 */
	public Iterable<String> getLabelsAtIndex(int index)
	{
		createReverseIndexLookup();
		return indexMap.get(index);
	}
	
	/**
	 * Single script entry.
	 */
	public static class Entry
	{
		private int parameterCount;
		private int index;
		
		private Entry(int parameterCount, int index)
		{
			this.parameterCount = parameterCount;
			this.index = index;
		}

		/**
		 * @return how many parameters this takes.
		 */
		public int getParameterCount()
		{
			return parameterCount;
		}
		
		/**
		 * @return the command index at the start of this script.
		 */
		public int getIndex()
		{
			return index;
		}
		
	}
	
}