/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions.io;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptIteratorType;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.BufferType;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.struct.Utils;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Script common functions for data streaming.
 * @author Matthew Tropiano
 */
public enum StreamingIOFunctions implements ScriptFunctionType
{
	/** @since 1.7.0, accepts InputStreams. */
	SKIP(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Skips reading [length] amount of bytes from a readable data input, from the file's " +
					"current cursor position. The stream's position will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input."),
					type(Type.OBJECTREF, "InputStream", "An input stream.")
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

				long totalSkipped = 0;
				if (temp.isObjectRef(DataInput.class))
				{
					DataInput in = temp.asObjectType(DataInput.class);
					try {
						// some inputstreams are buffered - skip() will only skip up to the buffer's length if data is buffered.
						// this ensures the maximum possible/desired, and breaks out when it truly cannot skip any more.
						long buf = 0;
						long lastlength = length;
						while (length > 0)
						{
							buf = in.skipBytes(length);
							length -= buf;
							totalSkipped += buf;
							if (lastlength == length)
								break;
							lastlength = length;
						}
						returnValue.set(totalSkipped);
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					}
					return true;
				}
				else if (temp.isObjectRef(InputStream.class))
				{
					InputStream in = temp.asObjectType(InputStream.class);
					try {
						// some inputstreams are buffered - skip() will only skip up to the buffer's length if data is buffered.
						// this ensures the maximum possible/desired, and breaks out when it truly cannot skip any more.
						long buf = 0;
						long lastlength = length;
						while (length > 0)
						{
							buf = in.skip(length);
							length -= buf;
							totalSkipped += buf;
							if (lastlength == length)
								break;
							lastlength = length;
						}
						returnValue.set(totalSkipped);
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					}
					return true;
				}
				else
				{
					returnValue.setError("BadParameter", "First parameter is not a valid input.");
					return true;
				}
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	FLUSH(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Flushes an output stream or other flushable stream - anything buffered is written immediately."
				)
				.parameter("flushable", 
					type(Type.OBJECTREF, "Flushable", "An open, flushable stream (usually an output stream).")
				)
				.returns(
					type(Type.OBJECTREF, "flushable."),
					type(Type.ERROR, "BadParameter", "If the provided object is not flushable."),
					type(Type.ERROR, "IOError", "If an I/O error occurs.")
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
				if (!temp.isObjectRef(Flushable.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a Flushable type.");
					return true;
				}

				try {
					temp.asObjectType(Flushable.class).flush();
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	RELAY(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads from a data input and writes the data read from it to " +
					"the data output until the end of the input is reached."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("buffersize", 
					type(Type.NULL, "Use 8192 (8 KB)."),
					type(Type.STRING, "The size of the intermediate buffer (in bytes) to use for transfer.")
				)
				.parameter("maxlength", 
					type(Type.NULL, "Unlimited."),
					type(Type.INTEGER, "The maximum amount of bytes to transfer.")
				)
				.returns(
					type(Type.INTEGER, "The actual amount of bytes moved."),
					type(Type.ERROR, "BadParameter", "If a valid stream was not provided for [input] or [output]."),
					type(Type.ERROR, "IOError", "If a read or write error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue val1 = CACHEVALUE1.get();
			ScriptValue val2 = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(val2);
				int maxlen = val2.isNull() ? -1 : val2.asInt(); 
				scriptInstance.popStackValue(val2);
				int bufsize = val2.isNull() ? 8192 : val2.asInt(); 
				scriptInstance.popStackValue(val2);
				scriptInstance.popStackValue(val1);
				if (!val1.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a data input type.");
					return true;
				}
				if (!val2.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "Second parameter is not a data output type.");
					return true;
				}

				long out;
				try {
					out = Utils.relay(
						val1.asObjectType(DataInput.class), 
						val2.asObjectType(DataOutput.class), 
						bufsize, maxlen
					);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				val1.setNull();
				val2.setNull();
			}
		}
	},
	
	READ(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads [length] amount of bytes from a readable data input, from the file or stream's current position. " +
					"The position will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
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
				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
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
					returnValue.set(buf.readBytes(offset, temp.asObjectType(DataInput.class), length));
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

	WRITE(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes [length] amount of bytes to a writable data output, to the file or stream " +
					"position using the bytes in a buffer. The position will be advanced."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
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
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data output");
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
				
				DataOutput file = temp.asObjectType(DataOutput.class);
				
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
	
	STDIN(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data input stream for reading from STDIN (resource is not registered)."
				)
				.returns(
					type(Type.OBJECTREF, "DataInputStream", "An open input stream for reading STDIN."),
					type(Type.ERROR, "Unavailable", "If STDIN was not provided to the script's environment.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			InputStream in = scriptInstance.getEnvironment().getStandardIn();
			if (in == null)
				returnValue.setError("Unavailable", "STDIN not available.");
			else
				returnValue.set(new DataInputStream(in));
			return true;
		}
	},

	STDOUT(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data output stream for writing to STDOUT (resource is not registered)."
				)
				.returns(
					type(Type.OBJECTREF, "DataOutputStream", "An open output stream for writing to STDOUT."),
					type(Type.ERROR, "Unavailable", "If STDOUT was not provided to the script's environment.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			PrintStream out = scriptInstance.getEnvironment().getStandardOut();
			if (out == null)
				returnValue.setError("Unavailable", "STDOUT not available.");
			else
				returnValue.set(new DataOutputStream(out));
			return true;
		}
	},

	STDERR(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data output stream for writing to STDERR (resource is not registered)."
				)
				.returns(
					type(Type.OBJECTREF, "DataOutputStream", "An open output stream for writing to STDERR."),
					type(Type.ERROR, "Unavailable", "If STDERR was not provided to the script's environment.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			PrintStream err = scriptInstance.getEnvironment().getStandardErr();
			if (err == null)
				returnValue.setError("Unavailable", "STDOUT not available.");
			else
				returnValue.set(new DataOutputStream(err));
			return true;
		}
	},

	// =======================================================================

	FISOPEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data input stream for reading from a file (and registers this resource as an open resource)."
				)
				.parameter("file", 
					type(Type.STRING, "A path to a file."),
					type(Type.OBJECTREF, "File", "A path to a file.")
				)
				.returns(
					type(Type.OBJECTREF, "DataInputStream", "An open data input stream to read from."),
					type(Type.ERROR, "BadParameter", "If [file] is null."),
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
				File file = popFile(scriptInstance, temp);
				
				if (file == null)
				{
					returnValue.setError("BadParameter", "No file provided.");
					return true;
				}
				
				try {
					DataInputStream in = new DataInputStream(new FileInputStream(file));
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

	FOSOPEN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data output stream for writing to a file (and registers this resource as an open resource)."
				)
				.parameter("file", 
					type(Type.STRING, "A path to a file."),
					type(Type.OBJECTREF, "File", "A path to a file.")
				)
				.parameter("append", 
					type(Type.BOOLEAN, "If true, appends to the file instead of overwriting it.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutputStream", "An open data output stream to write to."),
					type(Type.ERROR, "BadParameter", "If [file] is null."),
					type(Type.ERROR, "BadFile", "If [file] is null, a directory, or the file could not be made."),
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
				boolean append = temp.asBoolean();
				File file = popFile(scriptInstance, temp);
				
				if (file == null)
				{
					returnValue.setError("BadParameter", "No file provided.");
					return true;
				}

				try {
					DataOutputStream in = new DataOutputStream(new FileOutputStream(file, append));
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

	// =======================================================================

	BISOPEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data input stream for reading from a buffer from its current cursor position " +
					"(cursor advances with reading). This is NOT registered as a closeable resource."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.returns(
					type(Type.OBJECTREF, "DataInputStream", "An open data input stream to read from."),
					type(Type.ERROR, "BadParameter", "If [buffer] is null or not a buffer.")
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
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "No buffer provided.");
					return true;
				}
				
				DataInputStream in = new DataInputStream(temp.asObjectType(BufferType.class).getInputStream());
				scriptInstance.registerCloseable(in);
				returnValue.set(in);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	BOSOPEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data output stream for writing from a buffer from its current cursor position " +
					"(cursor advances with writing). This is NOT registered as a closeable resource."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutputStream", "An open data output stream to write to."),
					type(Type.ERROR, "BadParameter", "If [buffer] is null or not a buffer.")
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
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "No buffer provided.");
					return true;
				}
				
				DataOutputStream in = new DataOutputStream(temp.asObjectType(BufferType.class).getOutputStream());
				scriptInstance.registerCloseable(in);
				returnValue.set(in);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	// =======================================================================
	
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
					type(Type.NULL, "Use native encoding."),
					type(Type.STRING, "A charset name.")
				)
				.returns(
					type(Type.NULL, "If [instream] is null."),
					type(Type.OBJECTREF, "Reader", "The Reader to use for reading characters from."),
					type(Type.ERROR, "BadParameter", "If [instream] is not a data input stream object type."),
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
					returnValue.setError("BadParameter", "First parameter is not a data input stream type.");
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
					type(Type.NULL, "Use native encoding."),
					type(Type.STRING, "A charset name.")
				)
				.returns(
					type(Type.NULL, "If [outstream] is null."),
					type(Type.OBJECTREF, "Writer", "The Writer to use for writing characters to."),
					type(Type.ERROR, "BadParameter", "If [outstream] is not a data output stream object type."),
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
					returnValue.setError("BadParameter", "First parameter is not a data output stream type.");
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
	
	/** @since 1.5.0 */
	SROPEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a \"character stream\" reader around a string (not registered as an open resource)."
				)
				.parameter("string", 
					type(Type.STRING, "The string to read from (value is converted to a string).")
				)
				.returns(
					type(Type.OBJECTREF, "Reader", "The Reader to use for reading characters from.")
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
				returnValue.set((Reader)(new StringReader(temp.asString())));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/** @since 1.5.0 */
	SWOPEN(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a \"character stream\" writer for writing into a string (not registered as an open resource). " +
					"The string can then be fetched by calling TOSTRING() on this Writer."
				)
				.returns(
					type(Type.OBJECTREF, "Writer", "The Writer to use for writing characters to.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set((Writer)(new StringWriter(256)));
			return true;
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
	
	CSREADLN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads characters from a reader until an end-of-line (or stream) is reached " +
					"and returns a string of all of the read characters (minus the newline)."
				)
				.parameter("reader", 
					type(Type.OBJECTREF, "Reader", "A valid open Reader.")
				)
				.returns(
					type(Type.NULL, "If the Reader is at End-Of-Stream at the time of read."),
					type(Type.STRING, "The read characters (without the newline)."),
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
				if (!temp.isObjectRef(Reader.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a Reader type.");
					return true;
				}

				Reader reader = temp.asObjectType(Reader.class);
				try {
					returnValue.set(readLine(reader, sb));
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				return true;
			}
			finally
			{
				temp.setNull();
				sb.delete(0, sb.length());
			}
		}
	},
	
	CSITERATE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates an iterator that reads lines from a Reader until it reaches the end of the stream. " +
					"The value that this produces can be used in an each(...) loop. The keys are line numbers (starts at 1), and " +
					"values are the lines read (without newline). If an error occurs at any point, the value is an Error."
				)
				.parameter("reader", 
					type(Type.OBJECTREF, "Reader", "A valid open Reader.")
				)
				.returns(
					type(Type.OBJECTREF, "ScriptIteratorType", "The stream iterator."),
					type(Type.ERROR, "BadParameter", "If a valid Reader was not provided."),
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
				if (!temp.isObjectRef(Reader.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a Reader type.");
					return true;
				}

				returnValue.set(new ReaderIterator(temp.asObjectType(Reader.class)));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	CSWRITE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes characters to a writer."
				)
				.parameter("writer", 
					type(Type.OBJECTREF, "Writer", "A valid open Writer.")
				)
				.parameter("str", 
					type(Type.STRING, "The string of characters to write (if not a string, it is converted to one).")
				)
				.returns(
					type(Type.OBJECTREF, "Writer", "writer."),
					type(Type.ERROR, "BadParameter", "If a valid Writer was not provided."),
					type(Type.ERROR, "IOError", "If a write error occurs.")
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
				String value = temp.asString();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(Writer.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a Writer type.");
					return true;
				}

				if (value != null)
				{
					Writer writer = temp.asObjectType(Writer.class);
					try {
						writer.write(value);
					} catch (IOException e) {
						returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
						return true;
					}
				}
				
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	CSWRITELN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes characters to a writer."
				)
				.parameter("writer", 
					type(Type.OBJECTREF, "Writer", "A valid open Writer.")
				)
				.parameter("str", 
					type(Type.STRING, "The string of characters to write (if not a string, it is converted to one).")
				)
				.returns(
					type(Type.OBJECTREF, "Writer", "writer."),
					type(Type.ERROR, "BadParameter", "If a valid Writer was not provided."),
					type(Type.ERROR, "IOError", "If a write error occurs.")
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
				String value = temp.asString();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(Writer.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a Writer type.");
					return true;
				}

				Writer writer = temp.asObjectType(Writer.class);
				try {
					writer.write(value);
					writer.write('\n');
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				returnValue.set(temp);
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
	private StreamingIOFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(StreamingIOFunctions.values());
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
	private static File popFile(ScriptInstance scriptInstance, ScriptValue temp) 
	{
		scriptInstance.popStackValue(temp);
		if (temp.isNull())
			return null;
		else if (temp.isObjectRef(File.class))
			return temp.asObjectType(File.class);
		else
			return new File(temp.asString());
	}

	private static String readLine(Reader reader, StringBuilder sb) throws IOException
	{
		int readchar = -1;
		final int STATE_START = 0;
		final int STATE_MAYBE = 1;
		int state = STATE_START;
		boolean keepalive = true;
		while (keepalive && (readchar = reader.read()) >= 0)
		{
			char c = (char)readchar;
			switch (state)
			{
				case STATE_START:
				{
					if (c == '\r')
						state = STATE_MAYBE;
					else if (c == '\n')
						keepalive = false;
					else
						sb.append(c);
				}
				break;

				case STATE_MAYBE:
				{
					if (c == '\n')
						keepalive = false;
					else
					{
						sb.append('\r');
						sb.append(c);
					}
				}
				break;
			}
		}
		if (sb.length() == 0 && readchar == -1)
			return null;
		else
			return sb.toString();
	}
	
	/**
	 * An iterator that iterates through a Reader line by line until the end is reached. 
	 */
	private static class ReaderIterator implements ScriptIteratorType
	{
		private Reader reader;
		private boolean loaded;
		private String nextLine;
		private IOException exception;
		
		private StringBuilder sb;
		private IteratorPair pair;
		private int cur;

		private ReaderIterator(Reader reader)
		{
			this.reader = reader;
			this.loaded = false;
			this.nextLine = null;
			this.exception = null;
			this.sb = new StringBuilder(512);
			this.pair = new IteratorPair();
			this.cur = 0;
		}
		
		private void readNext()
		{
			sb.delete(0, sb.length());
			nextLine = null;
			exception = null;
			try {
				nextLine = readLine(reader, sb);
			} catch (IOException e) {
				exception = e;
			}
			loaded = true;
		}
		
		@Override
		public boolean hasNext() 
		{
			if (!loaded)
				readNext();
			return nextLine != null || exception != null;
		}

		@Override
		public IteratorPair next() 
		{
			if (!loaded)
				readNext();
			
			if (nextLine != null)
				pair.set(++cur, nextLine);
			else if (exception != null)
				pair.set(++cur, exception);
				
			loaded = false;
			return pair;
		}
	}
	
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<StringBuilder> STRINGBUILDER = ThreadLocal.withInitial(()->new StringBuilder(1024));
}
