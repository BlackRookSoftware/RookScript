/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions.common;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.ScriptValue.IteratorPair;
import com.blackrook.rookscript.ScriptValue.MapType;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

/**
 * RookScript map functions.
 * @author Matthew Tropiano
 */
public enum MapFunctions implements ScriptFunctionType
{	
	MAPKEYS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a list of all of the keys in a map. The returned list " +
					"is suitable for set operations (sorted, discrete)."
				)
				.parameter("map", 
					type(Type.MAP, "The map.")
				)
				.returns(
					type(Type.NULL, "If not a map."),
					type(Type.LIST, "[STRING, ...]", "A new list of the map's keys.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue map = CACHEVALUE1.get();
			ScriptValue out = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(map);
				if (!map.isMap())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					out.setEmptyList();
					for (IteratorPair e : map.asObjectType(MapType.class))
						out.setAdd(e.getKey());
					returnValue.set(out);
					return true;
				}
			}
			finally
			{
				map.setNull();
				out.setNull();
			}
		}
	},
	
	MAPVALUE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a value that corresponds to a key in the map."
				)
				.parameter("map", 
					type(Type.MAP, "The map.")
				)
				.parameter("key", 
					type(Type.STRING, "The key.")
				)
				.returns(
					type(Type.NULL, "If not a map or no value for that key."),
					type("The corresponding value, if found.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue keyValue = CACHEVALUE1.get();
			ScriptValue map = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			try 
			{
				scriptInstance.popStackValue(keyValue);
				scriptInstance.popStackValue(map);

				if (!map.isMap())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					map.mapGet(keyValue.asString(), out);
					returnValue.set(out);
					return true;
				}
			}
			finally
			{
				keyValue.setNull();
				map.setNull();
				out.setNull();
			}
		}
	},
	
	MAPMERGE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a new map that is the result of taking the first map and adding all " + 
					"of the keys of the second, replacing the keys that exist in the first. " + 
					"The copies are shallow - references are preserved."
				)
				.parameter("map1", 
					type(Type.MAP, "The first map."),
					type("An empty map.")
				)
				.parameter("map2", 
					type(Type.MAP, "The second map."),
					type("An empty map.")
				)
				.returns(
					type(Type.MAP, "A new map.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue map2 = CACHEVALUE1.get();
			ScriptValue map1 = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(map2);
				scriptInstance.popStackValue(map1);
				out.setEmptyMap();
				
				if (!map1.isMap())
				{
					returnValue.set(out);
					return true;
				}
				
				for (IteratorPair e : map1.asObjectType(MapType.class))
					out.mapSet(String.valueOf(e.getKey()), e.getValue());
				
				if (!map2.isMap())
				{
					returnValue.set(out);
					return true;
				}

				for (IteratorPair e : map2.asObjectType(MapType.class))
					out.mapSet(String.valueOf(e.getKey()), e.getValue());
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				map2.setNull();
				map1.setNull();
				out.setNull();
			}
		}
	},
	
	;
	
	private final int parameterCount;
	private Usage usage;
	private MapFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(MapFunctions.values());
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
	
	protected abstract Usage usage();

	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE3 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
