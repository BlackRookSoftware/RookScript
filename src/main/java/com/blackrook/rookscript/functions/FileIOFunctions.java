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
import com.blackrook.rookscript.ScriptValue.BufferType;
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

	FGETLEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns an open file's length in bytes."
				)
				.parameter("rafile", 
					type(Type.OBJECTREF, "RandomAccessFile", "An open file handle.")
				)
				.returns(
					type(Type.INTEGER, "The file's current length in bytes."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "IOError", "If a read error occurs.")
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
					returnValue.set(temp.asObjectType(RandomAccessFile.class).length());
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

	FSETLEN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets an open file's length in bytes. A [length] less than current will truncate the file, " +
					"and a greater [length] will expand the file with undefined contents."
				)
				.parameter("rafile", 
					type(Type.OBJECTREF, "RandomAccessFile", "An open file handle.")
				)
				.parameter("length", 
					type(Type.INTEGER, "The new length of the file.")
				)
				.returns(
					type(Type.OBJECTREF, "RandomAccessFile", "rafile."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "IOError", "If a read/write error occurs.")
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
				long length = temp.asLong();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(RandomAccessFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open file handle.");
					return true;
				}
				
				try {
					temp.asObjectType(RandomAccessFile.class).setLength(length);
					returnValue.set(temp);
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

	FGETPOS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns an open file's current cursor position."
				)
				.parameter("rafile", 
					type(Type.OBJECTREF, "RandomAccessFile", "An open file handle.")
				)
				.returns(
					type(Type.INTEGER, "The file's current cursor position."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "IOError", "If a read error occurs.")
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
					returnValue.set(temp.asObjectType(RandomAccessFile.class).getFilePointer());
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

	FSETPOS(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets an open file's current cursor position."
				)
				.parameter("rafile", 
					type(Type.OBJECTREF, "RandomAccessFile", "An open file handle.")
				)
				.parameter("position", 
					type(Type.INTEGER, "The new position.")
				)
				.returns(
					type(Type.INTEGER, "The file's current cursor position."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "IOError", "If the position could not be set ( < 0 ).")
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
				long position = temp.asLong();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(RandomAccessFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open file handle.");
					return true;
				}
				
				try {
					temp.asObjectType(RandomAccessFile.class).seek(position);
					returnValue.set(temp);
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

	FSKIP(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Skips reading [length] amount of bytes from an open file, from the file's " +
					"current cursor position. The file's cursor will be advanced."
				)
				.parameter("rafile", 
					type(Type.OBJECTREF, "RandomAccessFile", "An open file handle.")
				)
				.parameter("length",
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "The amount of bytes to skip.")
				)
				.returns(
					type(Type.INTEGER, "The amount of bytes actually skipped."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "IOError", "If a read error occurs.")
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
				int length = temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(RandomAccessFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open file handle.");
					return true;
				}
				
				try {
					returnValue.set(temp.asObjectType(RandomAccessFile.class).skipBytes(length));
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

	FREAD(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads [length] amount of bytes from an open file, from the file's current " +
					"cursor position. The file's cursor will be advanced."
				)
				.parameter("rafile", 
					type(Type.OBJECTREF, "RandomAccessFile", "An open file handle.")
				)
				.parameter("buffer",
					type(Type.BUFFER, "The buffer object to read into.")
				)
				.parameter("offset",
					type(Type.NULL, "Use current buffer cursor position (buffer cursor will be advanced)."),
					type(Type.INTEGER, "The offset into the buffer object to put the bytes (buffer cursor will NOT be advanced).")
				)
				.parameter("length",
					type(Type.NULL, "Use length(buffer) - offset."),
					type(Type.INTEGER, "The maximum amount of bytes to read.")
				)
				.returns(
					type(Type.INTEGER, "The actual amount of bytes read, or -1 if end-of-file was reached at time of read."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "IOError", "If a read error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue buffer = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				Integer length = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer offset = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(buffer);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(RandomAccessFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open file handle.");
					return true;
				}
				if (!buffer.isBuffer())
				{
					returnValue.setError("BadParameter", "Second parameter is not a buffer.");
					return true;
				}

				BufferType buf = buffer.asObjectType(BufferType.class);
				
				if (length == null)
					length = buf.size() - (offset == null ? buf.getPosition() : offset);
				
				try {
					returnValue.set(buf.readBytes(offset, temp.asObjectType(RandomAccessFile.class), length));
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

	FWRITE(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes [length] amount of bytes to the file, from the file's current cursor " +
					"position using the bytes in a buffer. The file's cursor will be advanced."
				)
				.parameter("rafile", 
					type(Type.OBJECTREF, "RandomAccessFile", "An open file handle.")
				)
				.parameter("buffer",
					type(Type.BUFFER, "The buffer object to read from.")
				)
				.parameter("offset",
					type(Type.NULL, "Use current buffer cursor position (buffer cursor will be advanced)."),
					type(Type.INTEGER, "The offset into the buffer object to get the bytes to write (buffer cursor will NOT be advanced).")
				)
				.parameter("length",
					type(Type.NULL, "Use length(buffer) - offset."),
					type(Type.INTEGER, "The maximum amount of bytes to write.")
				)
				.returns(
					type(Type.INTEGER, "The actual amount of bytes written."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "IOError", "If a write error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue buffer = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				Integer length = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer offset = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(buffer);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(RandomAccessFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open file handle.");
					return true;
				}
				if (!buffer.isBuffer())
				{
					returnValue.setError("BadParameter", "Second parameter is not a buffer.");
					return true;
				}

				BufferType buf = buffer.asObjectType(BufferType.class);
				
				if (length == null)
					length = buf.size() - (offset == null ? buf.getPosition() : offset);
				
				RandomAccessFile file = temp.asObjectType(RandomAccessFile.class);
				
				try {
					returnValue.set(buf.writeBytes(offset, file, length));
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
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
