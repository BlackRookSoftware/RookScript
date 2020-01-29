/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Script common functions for data streaming.
 * @author Matthew Tropiano
 */
public enum StreamFunctions implements ScriptFunctionType
{
	STDINOPEN(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens an InputStream for reading from STDIN (resource is not registered). " +
					"This will return null if STDIN is not provided to the script's environment!"
				)
				.returns(
					type(Type.NULL, "If STDIN was not provided to the script's environment."),
					type(Type.OBJECTREF, "InputStream", "An open input stream for reading STDIN.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set(scriptInstance.getEnvironment().getStandardIn());
			return true;
		}
	},

	STDOUTOPEN(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens an OutputStream for writing to STDOUT (resource is not registered). " +
					"This will return null if STDOUT is not provided to the script's environment!"
				)
				.returns(
					type(Type.NULL, "If STDOUT was not provided to the script's environment."),
					type(Type.OBJECTREF, "OutputStream", "An open output stream for writing to STDOUT.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set(scriptInstance.getEnvironment().getStandardOut());
			return true;
		}
	},

	STDERROPEN(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens an OutputStream for writing to STDERR (resource is not registered). " +
					"This will return null if STDERR is not provided to the script's environment!"
				)
				.returns(
					type(Type.NULL, "If STDERR was not provided to the script's environment."),
					type(Type.OBJECTREF, "OutputStream", "An open output stream for writing to STDERR.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set(scriptInstance.getEnvironment().getStandardErr());
			return true;
		}
	},

	SIFOPEN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens an InputStream for reading from a file (and registers this resource as an open resource)."
				)
				.parameter("file", 
					type(Type.STRING, "A path to a file."),
					type(Type.OBJECTREF, "File", "A path to a file.")
				)
				.parameter("buffersize", 
					type(Type.STRING, "A path to a file."),
					type(Type.OBJECTREF, "File", "A path to a file.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.OBJECTREF, "InputStream", "An open input stream to read from."),
					type(Type.ERROR, "BadParameter", "If [buffersize] <= 0."),
					type(Type.ERROR, "BadFile", "If [file] could not be found."),
					type(Type.ERROR, "Security", "If the OS denied opening the file for reading.")
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
				int size = temp.isNull() ? 8192 : temp.asInt();
				File file = popFile(scriptInstance, temp);
				
				if (file == null)
				{
					returnValue.setNull();
					return true;
				}
				if (size <= 0)
				{
					returnValue.setError("BadParameter", "Desired buffer size is <= 0.");
					return true;
				}
				
				try {
					InputStream in = new BufferedInputStream(new FileInputStream(file), size);
					scriptInstance.registerCloseable(in);
					returnValue.set(in);
				} catch (SecurityException e) {
					returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
				} catch (FileNotFoundException e) {
					returnValue.setError("BadFile", e.getMessage(), e.getLocalizedMessage());
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	// TODO: Finish this.
	
	;
	
	private final int parameterCount;
	private Usage usage;
	private StreamFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(StreamFunctions.values());
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

	/**
	 * Pops a variable off the stack and, using a temp variable, extracts a File/String.
	 * @param scriptInstance the script instance.
	 * @param temp the temporary script value.
	 * @return a File object.
	 */
	protected File popFile(ScriptInstance scriptInstance, ScriptValue temp) 
	{
		scriptInstance.popStackValue(temp);
		if (temp.isNull())
			return null;
		else if (temp.isObjectRef(File.class))
			return temp.asObjectType(File.class);
		else
			return new File(temp.asString());
	}
	
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
