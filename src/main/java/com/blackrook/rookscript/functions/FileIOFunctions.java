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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Script common functions for random-access file I/O.
 * @author Matthew Tropiano
 */
public enum FileIOFunctions implements ScriptFunctionType
{
	FOPEN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns an open handle to a file. " +
					"The file, if opened, is registered as a resource, and will be closed when the script terminates."
				)
				.parameter("path", 
					type(Type.STRING, "Path to file. Relative paths are relative to working directory."),
					type(Type.OBJECTREF, "File", "Path to file. Relative paths are relative to working directory.")
				)
				.parameter("mode", 
					type(Type.STRING, 
						"Mode string.\n" +
						"\"r\" = readonly,\n" +
						"\"rw\" = read/write/create buffered\n" +
						"\"rwd\" = read/write/create/synchronous data\n" +
						"\"rws\" = read/write/create/synchronous data/metadata"
					)
				)
				.returns(
					type(Type.NULL, "If [path] is null."),
					type(Type.OBJECTREF, "RandomAccessFile", "If successfully open/created."),
					type(Type.ERROR, "BadMode", "If [mode] is an unexpected value."),
					type(Type.ERROR, "Security", "If the OS denied opening the file for the required permissions."),
					type(Type.ERROR, "IOError", "If the file could not be opened/found for some reason.")
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
				String mode = temp.asString();
				scriptInstance.popStackValue(temp);
				File file = popFile(scriptInstance, temp);
				try {
					if (file == null)
						returnValue.setNull();
					else
					{
						RandomAccessFile raf = new RandomAccessFile(file, mode);
						returnValue.set(raf);
						scriptInstance.registerCloseable(raf);
					}
				} catch (IllegalArgumentException e) {
					returnValue.setError("BadMode", e.getMessage(), e.getLocalizedMessage());
				} catch (SecurityException e) {
					returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	FCLOSE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Attempts to close an open file handle. " +
					"On successful close, it is unregistered as an open resource."
				)
				.parameter("rafile", 
					type(Type.OBJECTREF, "RandomAccessFile", "An open file handle.")
				)
				.returns(
					type(Type.BOOLEAN, "True, if the file was closed."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "IOError", "If the file could not be closed for some reason.")
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
				if (!temp.isObjectRef(RandomAccessFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open file handle.");
					return true;
				}
				
				try {
					RandomAccessFile raf = temp.asObjectType(RandomAccessFile.class);
					raf.close();
					returnValue.set(true);
					scriptInstance.unregisterCloseable(raf);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
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
	private FileIOFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(FileIOFunctions.values());
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
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
