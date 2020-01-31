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
import com.blackrook.rookscript.struct.Utils;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.io.Reader;
import java.io.Writer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Script common functions for data streaming.
 * @author Matthew Tropiano
 */
public enum StreamFunctions implements ScriptFunctionType
{
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
	
	// =======================================================================

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

	// =======================================================================

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

	SSKIP(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Skips the next amount of bytes in an input stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.parameter("length", 
					type(Type.INTEGER, "The amount of bytes to skip.")
				)
				.returns(
					type(Type.INTEGER, "The amount of bytes actually skipped."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
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
				long length = temp.asLong();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				
				long totalSkipped = 0;
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
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				returnValue.set(totalSkipped);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SREAD(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads bytes from an Input Stream into a buffer."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
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
					type(Type.INTEGER, "The actual amount of bytes read, or -1 if end-of-stream was reached at time of read."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
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
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}
				if (!buffer.isBuffer())
				{
					returnValue.setError("BadParameter", "Second parameter is not a buffer type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				BufferType bufferType = buffer.asObjectType(BufferType.class);
				
				if (length == null)
					length = bufferType.size() - (offset == null ? bufferType.getPosition() : offset);
				
				int buf = 0;
				try {
					buf = bufferType.readBytes(offset, in, length);
				} catch (IndexOutOfBoundsException e) {
					returnValue.setError("OutOfBounds", e.getMessage(), e.getLocalizedMessage());
					return true;
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				returnValue.set(buf);
				return true;
			}
			finally
			{
				temp.setNull();
				buffer.setNull();
			}
		}
	},
	
	SREADBYTE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a byte from an input stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.returns(
					type(Type.NULL, "If the end-of-stream was reached at the time of read."),
					type(Type.INTEGER, "The read byte (0 - 255)."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
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
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				
				int b;
				try {
					b = in.read();
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				if (b < 0)
					returnValue.setNull();
				else
					returnValue.set(b & 0x0ff);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SREADSHORT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a short (2 bytes) from an input stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.NULL, "If the end-of-stream was reached at the time of read."),
					type(Type.INTEGER, "The read short (-32768 - 32767)."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				byte[] data = getByteArray(2);
				
				int b;
				try {
					b = in.read(data, 0, 2);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setNull();
				else if (b < 2)
					returnValue.setError("DataUnderflow", "Not enough bytes for value - read " + b + " bytes.");
				else
					returnValue.set(Utils.getShort(order, data, 0));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SREADUSHORT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads an unsigned short (2 bytes) from an input stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.NULL, "If the end-of-stream was reached at the time of read."),
					type(Type.INTEGER, "The read short (0 - 65535)."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				byte[] data = getByteArray(2);
				
				int b;
				try {
					b = in.read(data, 0, 2);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setNull();
				else if (b < 2)
					returnValue.setError("DataUnderflow", "Not enough bytes for value - read " + b + " bytes.");
				else
					returnValue.set(Utils.getUnsignedShort(order, data, 0));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SREADINT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads an integer (4 bytes) from an input stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.NULL, "If the end-of-stream was reached at the time of read."),
					type(Type.INTEGER, "The read integer (-2^31 - 2^31-1)."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				byte[] data = getByteArray(4);
				
				int b;
				try {
					b = in.read(data, 0, 4);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setNull();
				else if (b < 4)
					returnValue.setError("DataUnderflow", "Not enough bytes for value - read " + b + " bytes.");
				else
					returnValue.set(Utils.getInteger(order, data, 0));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SREADUINT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads an unsigned integer (4 bytes) from an input stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.NULL, "If the end-of-stream was reached at the time of read."),
					type(Type.INTEGER, "The read integer (0 - 2^32-1)."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				byte[] data = getByteArray(4);
				
				int b;
				try {
					b = in.read(data, 0, 4);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setNull();
				else if (b < 4)
					returnValue.setError("DataUnderflow", "Not enough bytes for value - read " + b + " bytes.");
				else
					returnValue.set(Utils.getUnsignedInteger(order, data, 0));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SREADFLOAT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a 32-bit float (4 bytes) from an input stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.NULL, "If the end-of-stream was reached at the time of read."),
					type(Type.FLOAT, "The read float."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				byte[] data = getByteArray(4);
				
				int b;
				try {
					b = in.read(data, 0, 4);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setNull();
				else if (b < 4)
					returnValue.setError("DataUnderflow", "Not enough bytes for value - read " + b + " bytes.");
				else
					returnValue.set(Utils.getFloat(order, data, 0));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SREADLONG(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a long integer (8 bytes) from an input stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.NULL, "If the end-of-stream was reached at the time of read."),
					type(Type.FLOAT, "The read integer (-2^63 - 2^63-1)."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				byte[] data = getByteArray(8);
				
				int b;
				try {
					b = in.read(data, 0, 8);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setNull();
				else if (b < 8)
					returnValue.setError("DataUnderflow", "Not enough bytes for value - read " + b + " bytes.");
				else
					returnValue.set(Utils.getLong(order, data, 0));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SREADDOUBLE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a 64-bit float (8 bytes) from an input stream."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.NULL, "If the end-of-stream was reached at the time of read."),
					type(Type.FLOAT, "The read double."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				byte[] data = getByteArray(8);
				
				int b;
				try {
					b = in.read(data, 0, 8);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setNull();
				else if (b < 8)
					returnValue.setError("DataUnderflow", "Not enough bytes for value - read " + b + " bytes.");
				else
					returnValue.set(Utils.getDouble(order, data, 0));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SREADSTR(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a set of bytes from an input stream and converts it into a string."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "An open input stream.")
				)
				.parameter("length", 
					type(Type.INTEGER, "The number of bytes to read.")
				)
				.parameter("encoding", 
					type(Type.NULL, "Use platform encoding."),
					type(Type.STRING, "A charset name.")
				)
				.returns(
					type(Type.NULL, "If the end-of-stream was reached at the time of read."),
					type(Type.STRING, "The read string."),
					type(Type.ERROR, "BadParameter", "If a valid input stream was not provided."),
					type(Type.ERROR, "BadEncoding", "If an unknown encoding was provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value."),
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
				String encodingName = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				int length = temp.asInt();
				scriptInstance.popStackValue(temp);

				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an InputStream type.");
					return true;
				}

				Charset encoding;
				if (encodingName == null)
					encoding = Charset.defaultCharset();
				else try {
					encoding = Charset.forName(encodingName);
				} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
					returnValue.setError("BadEncoding", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (length <= 0)
				{
					returnValue.set("");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				byte[] data = getByteArray(length);
				
				int b;
				try {
					b = in.read(data, 0, length);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setNull();
				else if (b < length)
					returnValue.setError("DataUnderflow", "Not enough bytes for value - read " + b + " bytes.");
				else
					returnValue.set(new String(data, 0, length, encoding));
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

				int readchar = -1;
				final int STATE_START = 0;
				final int STATE_MAYBE = 1;
				int state = STATE_START;
				boolean keepalive = true;
				try {
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
					type(Type.NULL, "Writes nothing."),
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
				String value = temp.isNull() ? null : temp.asString();
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
					type(Type.NULL, "Writes only a newline."),
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
				String value = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(Writer.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a Writer type.");
					return true;
				}

				Writer writer = temp.asObjectType(Writer.class);
				try {
					if (value != null)
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
	
	// =======================================================================

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

	/**
	 * Gets or resizes the byte array for serial reads.
	 * @param wantedlength the desired length. 
	 * @return the byte array to use.
	 */
	protected byte[] getByteArray(int wantedlength) 
	{
		byte[] out = BYTEARRAY.get();
		if (out.length < wantedlength)
			BYTEARRAY.set(out = new byte[wantedlength]);
		return out;
	}
	
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<StringBuilder> STRINGBUILDER = ThreadLocal.withInitial(()->new StringBuilder(1024));
	private static final ThreadLocal<byte[]> BYTEARRAY = ThreadLocal.withInitial(()->new byte[8]);

}