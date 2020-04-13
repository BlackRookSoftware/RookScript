/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions.common;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.ErrorType;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

/**
 * RookScript error functions.
 * @author Matthew Tropiano
 */
public enum ErrorFunctions implements ScriptFunctionType
{	
	ISERROR(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if the provided value is an error type."
				)
				.parameter("value", 
					type("The provided value.")
				)
				.returns(
					type(Type.BOOLEAN, "True if so, false if not.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(arg1);
				returnValue.set(arg1.isError());
				return true;
			}
			finally
			{
				arg1.setNull();
			}
		}
	},
	
	ERROR(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates an error type."
				)
				.parameter("type", 
					type(Type.STRING, "The error type.")
				)
				.parameter("message", 
					type(Type.STRING, "The error message.")
				)
				.parameter("messageLocalized", 
					type(Type.NULL, "Use the error message."),
					type(Type.STRING, "The error localized message.")
				)
				.returns(
					type(Type.ERROR, "The created error.")
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
				String messageLocalized = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				String message = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				String type = temp.isNull() ? null : temp.asString();
				
				if (messageLocalized == null)
					returnValue.setError(type, message);
				else
					returnValue.setError(type, message, messageLocalized);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	ERRORTYPE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the error type. If not an error, this returns null."
				)
				.parameter("error", 
					type(Type.ERROR, "The error.")
				)
				.returns(
					type(Type.NULL, "If not an error."),
					type(Type.STRING, "The error type.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(arg1);
				if (!arg1.isError())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					returnValue.set(arg1.asObjectType(ErrorType.class).getType());
					return true;
				}
			}
			finally
			{
				arg1.setNull();
			}
		}
	},

	ERRORMSG(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the error message. If not an error, this returns null."
				)
				.parameter("error", 
					type(Type.ERROR, "The error.")
				)
				.returns(
					type(Type.NULL, "If not an error."),
					type(Type.STRING, "The error message.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(arg1);
				if (!arg1.isError())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					returnValue.set(arg1.asObjectType(ErrorType.class).getMessage());
					return true;
				}
			}
			finally
			{
				arg1.setNull();
			}
		}
	},

	ERRORLOCALMSG(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the localized error message. If not an error, this returns null."
				)
				.parameter("error", 
					type(Type.ERROR, "The error.")
				)
				.returns(
					type(Type.NULL, "If not an error."),
					type(Type.STRING, "The localized error message.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(arg1);
				if (!arg1.isError())
				{
					returnValue.setNull();
					return true;
				}
				else
				{
					returnValue.set(arg1.asObjectType(ErrorType.class).getLocalizedMessage());
					return true;
				}
			}
			finally
			{
				arg1.setNull();
			}
		}
	},

	ERRORMAP(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns an error type as a map. If not an error, this returns null."
				)
				.parameter("error", 
					type(Type.ERROR, "The error.")
				)
				.returns(
					type(Type.NULL, "If not an error."),
					type(Type.MAP, "A map of {type:STRING, message:STRING, localizedmessage:STRING}.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(arg1);
				if (!arg1.isError())
				{
					returnValue.set(false);
					return true;
				}
				else
				{
					ErrorType error = arg1.asObjectType(ErrorType.class);
					temp.setEmptyMap();
					temp.mapSet("type", error.getType());
					temp.mapSet("message", error.getMessage());
					temp.mapSet("localizedmessage", error.getLocalizedMessage());
					returnValue.set(temp);
					return true;
				}
			}
			finally
			{
				arg1.setNull();
				temp.setNull();
			}
		}
	},
	
	;
	
	private final int parameterCount;
	private Usage usage;
	private ErrorFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(ErrorFunctions.values());
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
