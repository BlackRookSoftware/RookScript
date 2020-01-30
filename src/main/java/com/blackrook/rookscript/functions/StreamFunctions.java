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
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

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
					type(Type.NULL, "Use 8192 (8 KB)."),
					type(Type.INTEGER, "The input buffer size.")
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
				} catch (FileNotFoundException e) {
					returnValue.setError("BadFile", e.getMessage(), e.getLocalizedMessage());
				} catch (SecurityException e) {
					returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
				} 
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	SOFOPEN(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens an OutputStream for writing to a file (and registers this resource as an open resource)."
				)
				.parameter("file", 
					type(Type.STRING, "A path to a file."),
					type(Type.OBJECTREF, "File", "A path to a file.")
				)
				.parameter("append", 
					type(Type.BOOLEAN, "If true, appends to the file instead of overwriting it.")
				)
				.parameter("buffersize", 
					type(Type.NULL, "Use 8192 (8 KB)."),
					type(Type.INTEGER, "The input buffer size.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.OBJECTREF, "OutputStream", "An open output stream to write to."),
					type(Type.ERROR, "BadParameter", "If [buffersize] <= 0."),
					type(Type.ERROR, "BadFile", "If [file] is a directory, or the file could not be made."),
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
				scriptInstance.popStackValue(temp);
				boolean append = temp.asBoolean();
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
					OutputStream in = new BufferedOutputStream(new FileOutputStream(file, append), size);
					scriptInstance.registerCloseable(in);
					returnValue.set(in);
				} catch (FileNotFoundException e) {
					returnValue.setError("BadFile", e.getMessage(), e.getLocalizedMessage());
				} catch (SecurityException e) {
					returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
				} 
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	CSROPEN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a \"character stream\" reader around an input stream (and registers this resource as " +
					"an open resource, but only if the underlying stream is registered - it is deregistered before " +
					"registering this one). Closing this Reader will also close the underlying stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "A valid open input stream.")
				)
				.parameter("encoding", 
					type(Type.NULL, "Use platform encoding."),
					type(Type.STRING, "A charset name.")
				)
				.returns(
					type(Type.NULL, "If [instream] is null."),
					type(Type.OBJECTREF, "Reader", "The Reader to use for reading characters from."),
					type(Type.ERROR, "BadParameter", "If [instream] is not an InputStream object type."),
					type(Type.ERROR, "BadEncoding", "If [encoding] is not a valid charset name.")
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
				String encodingName = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				if (temp.isNull())
				{
					returnValue.setNull();
					return true;
				}
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);

				Charset encoding;
				if (encodingName == null)
					encoding = Charset.defaultCharset();
				else try {
					encoding = Charset.forName(encodingName);
				} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
					returnValue.setError("BadEncoding", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				Reader reader = new BufferedReader(new InputStreamReader(in, encoding));
				if (scriptInstance.closeableIsRegistered(in))
				{
					scriptInstance.unregisterCloseable(in);
					scriptInstance.registerCloseable(reader);
				}
				returnValue.set(reader);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	CSWOPEN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a \"character stream\" writer around an output stream (and registers this resource as " +
					"an open resource, but only if the underlying stream is registered - it is deregistered before " +
					"registering this one). Closing this Writer will also close the underlying stream."
				)
				.parameter("outstream", 
					type(Type.OBJECTREF, "OutputStream", "A valid open output stream.")
				)
				.parameter("encoding", 
					type(Type.NULL, "Use platform encoding."),
					type(Type.STRING, "A charset name.")
				)
				.returns(
					type(Type.NULL, "If [outstream] is null."),
					type(Type.OBJECTREF, "Reader", "The Reader to use for reading characters from."),
					type(Type.ERROR, "BadParameter", "If [outstream] is not an OutputStream object type."),
					type(Type.ERROR, "BadEncoding", "If [encoding] is not a valid charset name.")
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
				String encodingName = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				if (temp.isNull())
				{
					returnValue.setNull();
					return true;
				}
				if (!temp.isObjectRef(OutputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an OutputStream type.");
					return true;
				}

				OutputStream out = temp.asObjectType(OutputStream.class);

				Charset encoding;
				if (encodingName == null)
					encoding = Charset.defaultCharset();
				else try {
					encoding = Charset.forName(encodingName);
				} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
					returnValue.setError("BadEncoding", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				Writer writer = new BufferedWriter(new OutputStreamWriter(out, encoding));
				if (scriptInstance.closeableIsRegistered(out))
				{
					scriptInstance.unregisterCloseable(out);
					scriptInstance.registerCloseable(writer);
				}
				returnValue.set(writer);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	CSREAD(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads characters from a reader and returns a string of all of the read characters."
				)
				.parameter("reader", 
					type(Type.OBJECTREF, "Reader", "A valid open Reader.")
				)
				.parameter("charlen", 
					type(Type.NULL, "Use 1."),
					type(Type.INTEGER, "The maximum amount of characters to read.")
				)
				.returns(
					type(Type.NULL, "If the Reader is at End-Of-Stream at the time of read."),
					type(Type.STRING, "The read characters."),
					type(Type.ERROR, "BadParameter", "If a valid Reader was not provided."),
					type(Type.ERROR, "IOError", "If a read error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			StringBuilder sb = STRINGBUILDER.get();
			try
			{
				scriptInstance.popStackValue(temp);
				int length = temp.isNull() ? 1 : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(Reader.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a Reader type.");
					return true;
				}

				int readchar = -1;
				Reader reader = temp.asObjectType(Reader.class);
				try {
					while (length-- > 0 && (readchar = reader.read()) >= 0)
						sb.append((char)readchar);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (sb.length() == 0 && readchar == -1)
					returnValue.setNull();
				else
					returnValue.set(sb.toString());
				return true;
			}
			finally
			{
				temp.setNull();
				sb.delete(0, sb.length());
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
	private static final ThreadLocal<StringBuilder> STRINGBUILDER = ThreadLocal.withInitial(()->new StringBuilder(1024));

}
