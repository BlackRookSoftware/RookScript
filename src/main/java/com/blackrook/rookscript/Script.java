/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.blackrook.rookscript.lang.ScriptCommand;
import com.blackrook.rookscript.resolvers.ScriptHostFunctionResolver;
import com.blackrook.rookscript.resolvers.ScriptScopeResolver;
import com.blackrook.rookscript.struct.CountMap;
import com.blackrook.rookscript.struct.HashDequeMap;

/**
 * A compiled script.
 * @author Matthew Tropiano
 */
public class Script
{
	/** Function label prefix. */
	public static final String LABEL_FUNCTION_PREFIX = "function_";
	/** Script label prefix. */
	public static final String LABEL_ENTRY_PREFIX = "entry_";

	/** Script host function resolver. */
	private ScriptHostFunctionResolver hostFunctionResolver;
	/** Script host function resolver. */
	private ScriptScopeResolver scopeResolver;
	/** List of script commands. */
	private List<ScriptCommand> commands;

	/** Function name map (name to entry). */
	private Map<String, Entry> functionLabelMap;
	/** Script entry name map (name to entry). */
	private Map<String, Entry> scriptEntryMap;

	/** Label map (label to index). */
	private HashMap<String, Integer> labelMap;

	/** Reverse lookup map (index to labels - for debug). Transient. */
	private HashDequeMap<Integer, String> indexMap;
	
	/** Label generator sequencer for generated labels. */
	private CountMap<String> labelGeneratorCounter;
	
	/**
	 * Creates a new empty script.
	 */
	public Script()
	{
		this.hostFunctionResolver = ScriptHostFunctionResolver.EMPTY;
		this.scopeResolver = ScriptScopeResolver.EMPTY;
		this.commands = new ArrayList<>(256);
		this.functionLabelMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		this.scriptEntryMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		this.labelMap = new HashMap<>();
		this.indexMap = null;
		this.labelGeneratorCounter = null;
	}
	
	/**
	 * Sets this script's host function resolver.  
	 * @param hostFunctionResolver the function resolver.
	 * @throws NullPointerException if hostFunctionResolver is null.
	 */
	public void setHostFunctionResolver(ScriptHostFunctionResolver hostFunctionResolver)
	{
		Objects.requireNonNull(hostFunctionResolver);
		this.hostFunctionResolver = hostFunctionResolver;
	}
	
	/**
	 * Sets this script's scope resolver.
	 * @param scopeResolver the scope resolver.
	 * @throws NullPointerException if scopeResolver is null.
	 */
	public void setScopeResolver(ScriptScopeResolver scopeResolver) 
	{
		Objects.requireNonNull(scopeResolver);
		this.scopeResolver = scopeResolver;
	}
	
	/**
	 * Sets the commands to use in the script, replacing them entirely.
	 * @param commands the new command set.
	 */
	public void setCommands(ScriptCommand[] commands)
	{
		this.commands = new ArrayList<>(commands.length);
		for (ScriptCommand command : commands)
			this.commands.add(command);
	}

	/**
	 * Creates/sets a command index for a function entry name in the script.
	 * Also sets the function label index.
	 * <p> <strong>NOTE:</strong> Function entry names are case-insensitive, but the label that they convert to are not.
	 * Those labels are converted to lower-case, and prefixed with {@value #LABEL_FUNCTION_PREFIX}.
	 * @param name the name.
	 * @param parameterCount the amount of parameters that this takes.
	 * @param index the corresponding index.
	 * @return the entry created as a result of the set.
	 */
	public Entry createFunctionEntry(String name, int parameterCount, int index)
	{
		Entry out = new Entry(parameterCount, index);
		functionLabelMap.put(name, out);
		setIndex(LABEL_FUNCTION_PREFIX + name.toLowerCase(), index);
		return out;
	}

	/**
	 * Gets the corresponding index for a subscript function name.
	 * Entry names are case-insensitive.
	 * @param name the name to look up.
	 * @return the corresponding index or -1 if not found.
	 */
	public Entry getFunctionEntry(String name)
	{
		return functionLabelMap.get(name);
	}

	/**
	 * Sets an index for a subscript entry name in the script.
	 * Also sets the entry label index.
	 * <p> <strong>NOTE:</strong> Script entry names are case-insensitive, but the label that they convert to are not.
	 * Those labels are converted to lower-case, and prefixed with {@value #LABEL_ENTRY_PREFIX}.
	 * @param name the name.
	 * @param parameterCount the amount of parameters that this takes.
	 * @param index the corresponding index.
	 * @see #setIndex(String, int)
	 */
	public void setScriptEntry(String name, int parameterCount, int index)
	{
		scriptEntryMap.put(name, new Entry(parameterCount, index));
		setIndex(LABEL_ENTRY_PREFIX + name.toLowerCase(), index);
	}

	/**
	 * Gets the corresponding index for a subscript entry name.
	 * Entry names are case-insensitive.
	 * @param name the name to look up.
	 * @return the corresponding entry or null if not found.
	 */
	public Entry getScriptEntry(String name)
	{
		return scriptEntryMap.get(name);
	}

	/**
	 * @return an array of this script's entry point names.
	 * @since 1.13.0
	 */
	public String[] getScriptEntryNames()
	{
		Set<String> nameSet = scriptEntryMap.keySet();
		return nameSet.toArray(new String[nameSet.size()]);
	}
	
	/**
	 * Sets an index for a label in the script.
	 * The label is used case-sensitively! Use caution when setting it!
	 * @param label the label name.
	 * @param index the corresponding index.
	 */
	public void setIndex(String label, int index)
	{
		if (indexMap != null && labelMap.containsKey(label))
			indexMap.removeValue(labelMap.get(label), label);
		labelMap.put(label, index);
		if (indexMap != null)
			indexMap.add(index, label);
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
	public ScriptHostFunctionResolver getHostFunctionResolver()
	{
		return hostFunctionResolver;
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
	 * Gets the command at a specific index in the script.
	 * @param index the index of the command.
	 * @return the corresponding command, or null if out of range.
	 */
	public ScriptCommand getCommand(int index)
	{
		return index < 0 || index >= commands.size() ? null : commands.get(index); 
	}
	
	/**
	 * Gets the amount of commands in this script.
	 * @return the amount of commands.
	 */
	public int getCommandCount()
	{
		return commands.size();
	}
	
	/**
	 * Adds a command directive to the script.
	 * Be very careful with this!
	 * @param command the command to add.
	 */
	public void addCommand(ScriptCommand command)
	{
		commands.add(command);
	}
	
	/**
	 * Creates the reverse lookup.
	 * Only valuable on debug, so this is not created at first instantiation to save memory.
	 */
	private void createReverseIndexLookup()
	{
		if (indexMap != null)
			return;
		indexMap = new HashDequeMap<>();
		for (Map.Entry<String, Integer> p : labelMap.entrySet())
		{
			indexMap.add(p.getValue(), p.getKey());
		}
	}
	
	/**
	 * Generates the next label for a specific label prefix.
	 * @param prefix the label prefix.
	 * @return a new label to use.
	 */
	public String getNextGeneratedLabel(String prefix)
	{
		if (labelGeneratorCounter == null)
			labelGeneratorCounter = new CountMap<>();
		
		int i = labelGeneratorCounter.amount(prefix);
		labelGeneratorCounter.give(prefix, 1);
		return prefix + i;
	}
	
	/**
	 * Returns the current generated label counter for a specific label prefix.
	 * @param prefix the label prefix.
	 * @return the current counter value.
	 */
	public int getNextGeneratedLabelNumber(String prefix)
	{
		if (labelGeneratorCounter == null)
			return 0;
		else
			return labelGeneratorCounter.amount(prefix);
	}
	
	/**
	 * @return the map that contains the generated label counters.
	 */
	CountMap<String> getLabelGeneratorCounter()
	{
		return labelGeneratorCounter;
	}

	/**
	 * Sets the next generated label number for a label prefix.
	 * @param prefix the label prefix.
	 * @param count the next count.
	 */
	void setNextGeneratedLabelNumber(String prefix, int count)
	{
		if (labelGeneratorCounter == null)
			labelGeneratorCounter = new CountMap<>();
		
		labelGeneratorCounter.put(prefix, count);
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
