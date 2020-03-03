/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions.io;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.struct.Utils;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.DataOutput;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Script common functions for generic data I/O.
 * @author Matthew Tropiano
 */
public enum DataIOFunctions implements ScriptFunctionType
{
	READBYTE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a byte from an open file, from the file's current cursor position. The file's cursor will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.returns(
					type(Type.INTEGER, "The read byte (0 - 255)."),
					type(Type.ERROR, "DataUnderflow", "If no bytes to read."),
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
				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
					return true;
				}

				int b;
				try {
					b = Utils.read(temp.asObjectType(DataInput.class));
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				if (b < 0)
					returnValue.setError("DataUnderflow", "End-of-stream reached.");
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
	
	READSHORT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a short (2 bytes) from an open file, from the file's current cursor position. The file's cursor will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.INTEGER, "The read short (-32768 - 32767)."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value, or end-of-stream was reached at the time of read."),
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
				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
					return true;
				}

				byte[] data = getByteArray(2);
				
				int b;
				try {
					b = Utils.read(temp.asObjectType(DataInput.class), data, 0, 2);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setError("DataUnderflow", "End-of-stream reached.");
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
	
	READUSHORT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads an unsigned short (2 bytes) from an open file, from the file's current cursor position. The file's cursor will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.INTEGER, "The read short (0 - 65535)."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value, or end-of-stream was reached at the time of read."),
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
				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
					return true;
				}

				byte[] data = getByteArray(2);
				
				int b;
				try {
					b = Utils.read(temp.asObjectType(DataInput.class), data, 0, 2);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setError("DataUnderflow", "End-of-stream reached.");
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
	
	READINT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads an integer (4 bytes) from an open file, from the file's current cursor position. The file's cursor will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.INTEGER, "The read integer (-2^31 - 2^31-1)."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value, or end-of-stream was reached at the time of read."),
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
				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
					return true;
				}

				byte[] data = getByteArray(4);
				
				int b;
				try {
					b = Utils.read(temp.asObjectType(DataInput.class), data, 0, 4);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setError("DataUnderflow", "End-of-stream reached.");
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
	
	READUINT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads an unsigned integer (4 bytes) from an open file, from the file's current cursor position. The file's cursor will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.INTEGER, "The read integer (0 - 2^32-1)."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value, or end-of-stream was reached at the time of read."),
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
				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
					return true;
				}

				byte[] data = getByteArray(4);
				
				int b;
				try {
					b = Utils.read(temp.asObjectType(DataInput.class), data, 0, 4);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setError("DataUnderflow", "End-of-stream reached.");
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
	
	READFLOAT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a 32-bit float (4 bytes) from an open file, from the file's current cursor position. The file's cursor will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.FLOAT, "The read float."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value, or end-of-stream was reached at the time of read."),
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
				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
					return true;
				}

				byte[] data = getByteArray(4);
				
				int b;
				try {
					b = Utils.read(temp.asObjectType(DataInput.class), data, 0, 4);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setError("DataUnderflow", "End-of-stream reached.");
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
	
	READLONG(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a long integer (8 bytes) from an open file, from the file's current cursor position. The file's cursor will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.INTEGER, "The read integer (-2^63 - 2^63-1)."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value, or end-of-stream was reached at the time of read."),
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
				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
					return true;
				}

				byte[] data = getByteArray(8);
				
				int b;
				try {
					b = Utils.read(temp.asObjectType(DataInput.class), data, 0, 8);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setError("DataUnderflow", "End-of-stream reached.");
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
	
	READDOUBLE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a 64-bit float (8 bytes) from an open file, from the file's current cursor position. The file's cursor will be advanced."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.FLOAT, "The read double."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value, or end-of-stream was reached at the time of read."),
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
				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
					return true;
				}

				byte[] data = getByteArray(8);
				
				int b;
				try {
					b = Utils.read(temp.asObjectType(DataInput.class), data, 0, 8);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setError("DataUnderflow", "End-of-stream reached.");
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
	
	READSTR(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Reads a set of bytes from an open file, from the file's current cursor position, and converts it into a string."
				)
				.parameter("input", 
					type(Type.OBJECTREF, "DataInput", "A readable data input.")
				)
				.parameter("length", 
					type(Type.INTEGER, "The number of bytes to read.")
				)
				.parameter("encoding", 
					type(Type.NULL, "Use platform encoding."),
					type(Type.STRING, "A charset name.")
				)
				.returns(
					type(Type.STRING, "The read string."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "BadEncoding", "If an unknown encoding was provided."),
					type(Type.ERROR, "DataUnderflow", "If not enough bytes for the value, or end-of-stream was reached at the time of read."),
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

				if (!temp.isObjectRef(DataInput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a readable data input");
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

				byte[] data = getByteArray(length);
				
				int b;
				try {
					b = Utils.read(temp.asObjectType(DataInput.class), data, 0, length);
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}

				if (b < 0)
					returnValue.setError("DataUnderflow", "End-of-stream reached.");
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
	
	WRITEBYTE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes a byte to a writeable data output."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The byte to write (first 8 bits used).")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "output."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
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
				byte value = temp.asByte();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data input");
					return true;
				}

				try {
					temp.asObjectType(DataOutput.class).write(value);
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
	
	WRITESHORT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes a short (2 bytes) to a writeable data output."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The short to write (-32768 - 32767).")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "output."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				short value = temp.asShort();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data output");
					return true;
				}

				byte[] data = getByteArray(2);

				try {
					Utils.putShort(value, order, data, 0);
					temp.asObjectType(DataOutput.class).write(data, 0, 2);
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
	
	WRITEUSHORT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes an unsigned short (2 bytes) to a writeable data output."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The short to write (0 - 65535).")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "output."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				int value = temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data output");
					return true;
				}

				byte[] data = getByteArray(2);

				try {
					Utils.putUnsignedShort(value, order, data, 0);
					temp.asObjectType(DataOutput.class).write(data, 0, 2);
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
	
	WRITEINT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes an integer (4 bytes) to a writeable data output."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The integer to write (-2^31 - 2^31-1).")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "output."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				int value = temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data output");
					return true;
				}

				byte[] data = getByteArray(4);

				try {
					Utils.putInteger(value, order, data, 0);
					temp.asObjectType(DataOutput.class).write(data, 0, 4);
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
	
	WRITEUINT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes an unsigned integer (4 bytes) to a writeable data output."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The integer to write (0 - 2^32-1).")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "output."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				long value = temp.asLong();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data output");
					return true;
				}

				byte[] data = getByteArray(4);

				try {
					Utils.putUnsignedInteger(value, order, data, 0);
					temp.asObjectType(DataOutput.class).write(data, 0, 4);
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
	
	WRITEFLOAT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes a 32-bit floating-point number to a writeable data output."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("value", 
					type(Type.FLOAT, "The float to write.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "output."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				float value = temp.asFloat();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data output");
					return true;
				}

				byte[] data = getByteArray(4);

				try {
					Utils.putFloat(value, order, data, 0);
					temp.asObjectType(DataOutput.class).write(data, 0, 4);
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
	
	WRITELONG(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes a long integer (8 bytes) to a writeable data output."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The integer to write (-2^63 - 2^63-1).")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "output."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				long value = temp.asLong();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data output");
					return true;
				}

				byte[] data = getByteArray(8);

				try {
					Utils.putLong(value, order, data, 0);
					temp.asObjectType(DataOutput.class).write(data, 0, 8);
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
	
	WRITEDOUBLE(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes a 64-bit floating-point number to a writeable data output."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("value", 
					type(Type.FLOAT, "The double to write.")
				)
				.parameter("endianmode", 
					type(Type.NULL, "Use native."),
					type(Type.BOOLEAN, "True - big endian, false - little endian.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "output."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
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
				ByteOrder order = temp.isNull() 
						? ByteOrder.nativeOrder() 
						: (temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
				scriptInstance.popStackValue(temp);
				double value = temp.asDouble();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data output");
					return true;
				}

				byte[] data = getByteArray(8);

				try {
					Utils.putDouble(value, order, data, 0);
					temp.asObjectType(DataOutput.class).write(data, 0, 8);
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
	
	WRITESTR(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes a string to a writeable data output, converting it into a set of bytes."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "DataOutput", "A writeable data output.")
				)
				.parameter("value", 
					type(Type.NULL, "Write nothing."),
					type(Type.STRING, "The string to write.")
				)
				.parameter("encoding", 
					type(Type.NULL, "Use platform encoding."),
					type(Type.STRING, "A charset name.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "output."),
					type(Type.ERROR, "BadParameter", "If a file handle was not provided."),
					type(Type.ERROR, "BadEncoding", "If an unknown encoding was provided."),
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
				String encodingName = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				String value = temp.asString();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(DataOutput.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a writeable data output");
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

				if (value != null)
				{
					byte[] data = value.getBytes(encoding);

					try {
						temp.asObjectType(DataOutput.class).write(data);
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
	
	;
	
	private final int parameterCount;
	private Usage usage;
	private DataIOFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(DataIOFunctions.values());
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
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<byte[]> BYTEARRAY = ThreadLocal.withInitial(()->new byte[8]);

}
