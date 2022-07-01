/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Script common functions for date/time stuff.
 * @author Matthew Tropiano
 */
public enum DateFunctions implements ScriptFunctionType
{
	DATE(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns current date/time as milliseconds since Epoch (January 1, 1970 UTC)."
				)
				.returns(
					type(Type.INTEGER, "The current date/time in milliseconds since Epoch.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set(System.currentTimeMillis());
			return true;
		}
	},

	DATEFORMAT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Formats a date using milliseconds since Epoch."
				)
				.parameter("timemillis",
					type(Type.INTEGER, "Milliseconds since Epoch.")
				)
				.parameter("formatstring",
					type(Type.STRING, "A SimpleDateFormat formatting string.")
				)
				.returns(
					type(Type.STRING, "The formatted time string."),
					type(Type.ERROR, "BadFormat", "If bad format string.")
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
				String formatString = temp.asString();
				scriptInstance.popStackValue(temp);
				long time = temp.asLong();
				
				SimpleDateFormat sdf;
				try {
					Map<String, SimpleDateFormat> map = SIMPLEDATEFORMATCACHE.get();
					if ((sdf = map.get(formatString)) == null)
						map.put(formatString, sdf = new SimpleDateFormat(formatString));
				} catch (IllegalArgumentException e) {
					returnValue.setError("BadFormat", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				returnValue.set(sdf.format(new Date(time)));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	;
	
	private final int parameterCount;
	private Usage usage;
	private DateFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(DateFunctions.values());
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
	private static final ThreadLocal<Map<String, SimpleDateFormat>> SIMPLEDATEFORMATCACHE = ThreadLocal.withInitial(()->new HashMap<>());

}
