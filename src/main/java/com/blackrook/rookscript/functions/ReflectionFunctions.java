/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptIteratorType.IteratorPair;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import com.blackrook.rookscript.Script.Entry;

/**
 * Script common functions for RookScript reflection.
 * @author Matthew Tropiano
 * @since [NOW]
 */
public enum ReflectionFunctions implements ScriptFunctionType
{
	FUNCTIONINFO(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns information on a function (host or local)."
				)
				.parameter("name",
					type(Type.STRING, "The name of the function (converted to string if not string).")
				)
				.parameter("namespace",
					type(Type.NULL, "No namespace - match globals only."),
					type(Type.STRING, "Only sometimes necessary for host functions: the namespace of the function (converted to string if not string).")
				)
				.returns(
					type(Type.MAP, "{host:BOOLEAN, name:STRING, namespace:STRING, parameterCount:INTEGER, index:INTEGER}", "A map of function info."),
					type(Type.ERROR, "NotFound", "If the function does not exist.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try 
			{
				scriptInstance.popStackValue(temp);
				String namespace = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				String name = temp.asString();
				
				Entry entry;
				ScriptFunctionType scriptFunction;
				if (namespace == null && (entry = scriptInstance.getScript().getFunctionEntry(name)) != null)
				{
					returnValue.setEmptyMap(5);
					returnValue.mapSet("host", false);
					returnValue.mapSet("index", entry.getIndex());
					returnValue.mapSet("name", name.toLowerCase());
					returnValue.mapSet("namespace", null);
					returnValue.mapSet("parametercount", entry.getParameterCount());
				}
				else if ((scriptFunction = scriptInstance.getHostFunctionResolver().getNamespacedFunction(namespace, name)) != null)
				{
					returnValue.setEmptyMap(5);
					returnValue.mapSet("host", true);
					returnValue.mapSet("index", null);
					returnValue.mapSet("name", name.toLowerCase());
					returnValue.mapSet("namespace", namespace == null ? null : namespace.toLowerCase());
					returnValue.mapSet("parametercount", scriptFunction.getParameterCount());
				}
				else
				{
					returnValue.setError("NotFound", "Function \"" + name + "\" could not be found.");
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	ENTRYINFO(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns information on a script entry point."
				)
				.parameter("name",
					type(Type.STRING, "The name of the entry point (converted to string if not string).")
				)
				.returns(
					type(Type.MAP, "{name:STRING, parameterCount:INTEGER, index:INTEGER}", "A map of entry info."),
					type(Type.ERROR, "NotFound", "If the function does not exist.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try 
			{
				scriptInstance.popStackValue(temp);
				String name = temp.asString();
				
				Entry entry;
				if ((entry = scriptInstance.getScript().getScriptEntry(name)) != null)
				{
					returnValue.setEmptyMap(3);
					returnValue.mapSet("index", entry.getIndex());
					returnValue.mapSet("name", name.toLowerCase());
					returnValue.mapSet("parametercount", entry.getParameterCount());
				}
				else
				{
					returnValue.setError("NotFound", "Entry point \"" + name + "\" could not be found.");
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	INVOKE(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns information on a script entry point."
				)
				.parameter("name",
					type(Type.STRING, "The name of the entry point (converted to string if not string).")
				)
				.parameter("namespace",
					type(Type.NULL, "No namespace - match globals only."),
					type(Type.STRING, "Only sometimes necessary for host functions: the namespace of the function (converted to string if not string).")
				)
				.parameter("parameters",
					type(Type.NULL, "Pass an empty list."),
					type(Type.LIST, "The parameters to pass to the function."),
					type("Pass a single parameter.")
				)
				.returns(
					type("The return value of the invoked function."),
					type(Type.ERROR, "NotFound", "If the function does not exist.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			Entry entry = null;
			ScriptFunctionType scriptFunction = null;
			
			ScriptValue params = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try 
			{
				scriptInstance.popStackValue(params);
				scriptInstance.popStackValue(temp);
				String namespace = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				String name = temp.asString();
				
				int count;
				if (namespace == null && (entry = scriptInstance.getScript().getFunctionEntry(name)) != null)
					count = entry.getParameterCount();
				else if ((scriptFunction = scriptInstance.getHostFunctionResolver().getNamespacedFunction(namespace, name)) != null)
					count = scriptFunction.getParameterCount();
				else
				{
					returnValue.setError("NotFound", "Function \"" + name + "\" could not be found.");
					return true;
				}
				
				// TODO: Maybe prevent invocation of this function?
				
				int i = 0;
				if (params.isNull())
				{
					// Do Nothing
				}
				else if (params.isList()) for (IteratorPair p : params)
				{
					scriptInstance.pushStackValue(p.getValue());
					if (++i == count)
						break;
				}
				else if (count >= 1)
				{
					scriptInstance.pushStackValue(params);
					i++;
				}
				
				while (i++ < count)
					scriptInstance.pushStackValue(null);
			}
			finally
			{
				temp.setNull();
				params.setNull();
			}
			
			// local function
			if (entry != null)
			{
				scriptInstance.pushFrame(entry.getIndex());
				scriptInstance.popStackValue(returnValue);
				return true;
			}
			// host function
			else
			{
				// TODO: Finish.
				scriptInstance.popStackValue(returnValue);
				return true;
			}
		}
	},

	;
	
	private final int parameterCount;
	private Usage usage;
	private ReflectionFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(ReflectionFunctions.values());
	}

	@Override
	public int getParameterCount()
	{
		return parameterCount;
	}

	@Override
	public Usage getUsage()
	{
		if (usage == null)
			usage = usage();
		return usage;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
