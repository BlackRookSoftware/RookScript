/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions.common;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.BufferType;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

/**
 * RookScript buffer functions.
 * @author Matthew Tropiano
 */
public enum BufferFunctions implements ScriptFunctionType
{	
	BUFNEW(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new, blank buffer."
				)
				.parameter("size", 
					type(Type.INTEGER, "The size of the buffer.")
				)
				.parameter("order", 
					type(Type.NULL, "Use native byte order/endian mode."),
					type(Type.BOOLEAN, "True = big endian, false = little endian.")
				)
				.returns(
					type(Type.BUFFER, "A new allocated buffer of [size] bytes."),
					type(Type.ERROR, "BadParameter", "If size is < 0."),
					type(Type.ERROR, "OutOfMemory", "If not allocated.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				ByteOrder order = null;
				scriptInstance.popStackValue(temp);
				if (temp.isNull())
					order = ByteOrder.nativeOrder();
				else
					order = temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
				scriptInstance.popStackValue(temp);
				int size = temp.asInt();
				
				if (size < 0)
				{
					returnValue.setError("BadParameter", "Size is < 0.");
					return true;
				}

				try {
					returnValue.setEmptyBuffer(size, order);
				} catch (Exception e) {
					returnValue.setError(e);
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFSETSIZE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Resizes a buffer (without creating a new one). The values of the data are kept, " +
					"except for what would be discarded on a buffer shrink, and the data in an expanded part is set to 0."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to resize.")
				)
				.parameter("size", 
					type(Type.INTEGER, "The new size of the buffer.")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "Size is < 0."),
					type(Type.ERROR, "OutOfMemory", "If not allocated.")
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
				int size = temp.asInt();
				
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}

				BufferType buffer = temp.asObjectType(BufferType.class);
				
				if (size < 0)
				{
					returnValue.setError("BadParameter", "Size is < 0.");
					return true;
				}

				buffer.setSize(size);
				
				returnValue.set(temp);				
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFWRAP(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Wraps a list as a buffer."
				)
				.parameter("list", 
					type(Type.LIST, "[INTEGER, ...]", "The list of values to interpret as (clamped) byte values from [0, 255].")
				)
				.parameter("endian", 
					type(Type.NULL, "Use native endian."),
					type(Type.BOOLEAN, "True = big, false = little.")
				)
				.returns(
					type(Type.BUFFER, "A new allocated buffer of [length(list)] bytes."),
					type(Type.ERROR, "OutOfMemory", "If not allocated.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue list = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			ScriptValue out = CACHEVALUE3.get();
			try
			{
				ByteOrder order = null;
				scriptInstance.popStackValue(temp);
				if (temp.isNull())
					order = ByteOrder.nativeOrder();
				else
					order = temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;

				scriptInstance.popStackValue(list);
				if (!list.isList())
				{
					temp.setEmptyList(1);
					temp.listAdd(list);
					list.set(temp);
				}
				
				try {
					out.setEmptyBuffer(list.length(), order);
				} catch (Exception e) {
					returnValue.setError(e);
					return true;
				}
				
				BufferType buf = out.asObjectType(BufferType.class);
				for (int i = 0; i < list.length(); i++)
				{
					list.listGetByIndex(i, temp);
					buf.putByte(i, temp.asByte());
				}

				returnValue.set(out);
				return true;
			}
			finally
			{
				list.setNull();
				temp.setNull();
				out.setNull();
			}
		}
	},
	
	BUFUNWRAP(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Unwraps a buffer into a list."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to unwrap.")
				)
				.returns(
					type(Type.LIST, "A new list where each element is a value from 0 to 255, representing all of the values in the buffer in order."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer.")
				)
			;
		}

		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue) 
		{
			ScriptValue out = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buffer = temp.asObjectType(BufferType.class);
				int len = buffer.size();
				out.setEmptyList(len);
				for (int i = 0; i < len; i++)
				{
					temp.set(buffer.getByte(i));
					out.listAdd(temp);
				}
				
				returnValue.set(out);
				return true;
			}
			finally
			{
				out.setNull();
				temp.setNull();
			}
		}
		
	},
	
	BUFSLICE(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new buffer that is a subset of bytes from another buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The source buffer to use.")
				)
				.parameter("length", 
					type(Type.NULL, "Use length(buffer) - index."),
					type(Type.INTEGER, "The amount of bytes to copy to the new buffer.")
				)
				.parameter("index", 
					type(Type.NULL, "Use current position as the starting index."),
					type(Type.INTEGER, "The starting index.")
				)
				.returns(
					type(Type.BUFFER, "New allocated buffer of [length] bytes, copied from source (same byte order)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If index or index + length is out of bounds.")
				)
			;
		}
	
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue out = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer length = temp.isNull() ? null : temp.asInt();
	
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
	
				BufferType buf = temp.asObjectType(BufferType.class);
				if (index == null)
					index = buf.getPosition();
				if (length == null)
					length = buf.size() - index;
				
				if (index < 0)
				{
					returnValue.setError("OutOfBounds", "Index < 0");
					return true;
				}
				else if (index + length > buf.size())
				{
					returnValue.setError("OutOfBounds", "Index + length = " + (index + length));
					return true;
				}
				
				temp.setEmptyBuffer(length, buf.getByteOrder());
				temp.asObjectType(BufferType.class).readBytes(0, buf, index, length);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				out.setNull();
				temp.setNull();
			}
		}
	}, 
	
	BUFSETPOS(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets the current cursor position for a buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("position", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "The new current buffer cursor position (0-based).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If the position is out of bounds.")
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
				int position = temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				if (position < 0 || position > buf.size())
				{
					returnValue.setError("OutOfBounds", "Position is out of bounds. size: " + buf.size() +", pos: " + position);
					return true;
				}
				
				buf.setPosition(position);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETPOS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the current cursor position for a buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.returns(
					type(Type.INTEGER, "The buffer's current cursor position (0-based)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer.")
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
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				returnValue.set(temp.asObjectType(BufferType.class).getPosition());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFSETORDER(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets the current cursor position for a buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("order", 
					type(Type.NULL, "Use native byte order/endian mode."),
					type(Type.BOOLEAN, "True = big endian, false = little endian.")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If the position is out of bounds.")
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
				ByteOrder order;
				if (temp.isNull())
					order = ByteOrder.nativeOrder();
				else
					order = temp.asBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;

				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				buf.setByteOrder(order);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETORDER(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets the current byte order for a buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.returns(
					type(Type.BOOLEAN, "The buffer's current byte order - true if big endian, false if little endian."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer.")
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
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				returnValue.set(temp.asObjectType(BufferType.class).getByteOrder() == ByteOrder.BIG_ENDIAN);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFREAD(5)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Read bulk bytes from another buffer. Will stop if the end of this buffer is reached."
				)
				.parameter("destbuf", 
					type(Type.BUFFER, "The destination buffer to fill.")
				)
				.parameter("srcbuf", 
					type(Type.BUFFER, "The source buffer to read from."),
					type(Type.LIST, "[INTEGER, ...]", "The list of integers to use as bytes.")
				)
				.parameter("maxlength", 
					type(Type.NULL, "Read as much as possible to fill the destination or read the rest of source."),
					type(Type.INTEGER, "The maximum amount of bytes to read/set.")
				)
				.parameter("destindex", 
					type(Type.NULL, "Use the destination buffer's current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the destination buffer (cursor position will NOT be advanced).")
				)
				.parameter("srcindex", 
					type(Type.NULL, "Use the source buffer's current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the source buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.INTEGER, "The amount of the bytes read, or -1 if the end of the source was already reached at call."),
					type(Type.ERROR, "BadParameter", "If either destbuf or srcbuf are not buffers."),
					type(Type.ERROR, "OutOfBounds", "If index or index + length is out of bounds.")
				)
			;
		}
	
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue srcbuf = CACHEVALUE2.get();
			ScriptValue destbuf = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(temp);
				Integer srcindex = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer destindex = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer length = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(srcbuf);
				scriptInstance.popStackValue(destbuf);
				
				if (!destbuf.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer (destination).");
					return true;
				}
				if (!srcbuf.isBuffer())
				{
					returnValue.setError("BadParameter", "Second parameter is not a buffer (source).");
					return true;
				}
				
				BufferType destination = destbuf.asObjectType(BufferType.class);
				BufferType source = srcbuf.asObjectType(BufferType.class);

				int src = srcindex != null ? srcindex : source.getPosition(); 
				int dest = destindex != null ? destindex : destination.getPosition(); 
				int srclength = source.size() - src;
				int destlength = destination.size() - dest;
				int minlength = Math.min(srclength, destlength);
				
				length = length != null ? Math.min(minlength, length) : minlength;
				
				returnValue.set(destination.readBytes(destindex, source, srcindex, length));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	BUFFILL(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Fills a buffer with a single value. Does not advance any cursor positions."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The destination buffer to use.")
				)
				.parameter("data", 
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "The byte value to fill.")
				)
				.parameter("amount", 
					type(Type.NULL, "Use length(buffer) - index."),
					type(Type.INTEGER, "The amount of bytes to set.")
				)
				.parameter("index", 
					type(Type.NULL, "Use buffer's current cursor position (cursor position will be advanced)."),
					type(Type.INTEGER, "The starting index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				Integer amount = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				byte data = temp.isNull() ? 0 : temp.asByte();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
	
				BufferType buf = temp.asObjectType(BufferType.class);
				if (amount == null)
					amount = buf.size() - (index == null ? buf.getPosition() : index);
	
				try {
					buf.putBytes(index, data, amount);
					returnValue.set(temp);
					return true;
				} catch (IndexOutOfBoundsException e) {
					returnValue.setError("OutOfBounds", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	BUFPUTBYTE(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets a byte in a buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The byte value to set (0 - 255)")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index] is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				byte value = temp.isNull() ? 0 : temp.asByte();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) >= buf.size())
				{
					returnValue.setError("OutOfBounds", "Index " + i + " is out of bounds.");
					return true;
				}
	
				buf.putByte(index, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETBYTE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a byte in a buffer (returned unsigned)."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.INTEGER, "The value at the index (0 - 255)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index] is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) >= buf.size() || i < 0)
				{
					returnValue.setError("OutOfBounds", "Index " + i + " is out of bounds.");
					return true;
				}
	
				returnValue.set(buf.getByte(index) & 0x0ff);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFPUTSHORT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets a short value in a buffer (16-bit, signed). BufferType byte order affects how the bytes are written."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The short value to set (-32768 - 32767)")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+2) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				short value = temp.isNull() ? 0 : temp.asShort();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 2 > buf.size())
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 2 will be out of bounds.");
					return true;
				}
	
				buf.putShort(index, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETSHORT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a short in a buffer (16-bit, signed). BufferType byte order affects how the bytes are read."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.INTEGER, "The value at the index (-32768 - 32767)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+2) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 2 > buf.size() || i < 0)
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 2 will be out of bounds.");
					return true;
				}
	
				returnValue.set(buf.getShort(index));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFPUTUSHORT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets a short value in a buffer (16-bit, unsigned). BufferType byte order affects how the bytes are written."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The short value to set (0 - 65535)")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+2) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				int value = temp.isNull() ? 0 : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 2 > buf.size())
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 2 will be out of bounds.");
					return true;
				}
	
				buf.putUnsignedShort(index, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETUSHORT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a short in a buffer (16-bit, unsigned). BufferType byte order affects how the bytes are read."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.INTEGER, "The value at the index (0 - 65535)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+2) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 2 > buf.size() || i < 0)
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 2 will be out of bounds.");
					return true;
				}
	
				returnValue.set(buf.getUnsignedShort(index));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFPUTINT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets an integer value in a buffer (32-bit, signed). BufferType byte order affects how the bytes are written."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The integer value to set (-2^31 - 2^31-1)")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+4) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				int value = temp.isNull() ? 0 : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 4 > buf.size())
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 4 will be out of bounds.");
					return true;
				}
	
				buf.putInteger(index, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETINT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets an integer in a buffer (32-bit, signed). BufferType byte order affects how the bytes are read."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.INTEGER, "The value at the index (-2^31 - 2^31-1)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+4) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 4 > buf.size() || i < 0)
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 4 will be out of bounds.");
					return true;
				}
	
				returnValue.set(buf.getInteger(index));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFPUTUINT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets an integer value in a buffer (32-bit, unsigned). BufferType byte order affects how the bytes are written."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The integer value to set (0 - 2^32-1)")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+4) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				long value = temp.isNull() ? 0 : temp.asLong();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 4 > buf.size())
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 4 will be out of bounds.");
					return true;
				}
	
				buf.putUnsignedInteger(index, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETUINT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets an integer in a buffer (32-bit, unsigned). BufferType byte order affects how the bytes are read."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.INTEGER, "The value at the index (0 - 2^32-1)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+4) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 4 > buf.size() || i < 0)
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 4 will be out of bounds.");
					return true;
				}
	
				returnValue.set(buf.getUnsignedInteger(index));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFPUTFLOAT(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets a floating-point value in a buffer (32-bit). BufferType byte order affects how the bytes are written. " +
					"NOTE: Floating-point values are stored in memory as double-precision - some precision may be lost on write!"
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The float value to set.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+4) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				float value = temp.isNull() ? 0f : temp.asFloat();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 4 > buf.size())
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 4 will be out of bounds.");
					return true;
				}
	
				buf.putFloat(index, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETFLOAT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a float in a buffer (32-bit). BufferType byte order affects how the bytes are read." +
					"NOTE: Floating-point values are stored in memory as double-precision - some data may be extrapolated on read!"
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.FLOAT, "The value at the index."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+4) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 4 > buf.size() || i < 0)
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 4 will be out of bounds.");
					return true;
				}
	
				returnValue.set(buf.getFloat(index));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFPUTLONG(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets a long integer value in a buffer (64-bit, signed). BufferType byte order affects how the bytes are written."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The integer value to set (-2^63 - 2^63-1)")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+8) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				long value = temp.isNull() ? 0 : temp.asLong();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 8 > buf.size())
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 8 will be out of bounds.");
					return true;
				}
	
				buf.putLong(index, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETLONG(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a long integer in a buffer (64-bit, signed). BufferType byte order affects how the bytes are read."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.INTEGER, "The value at the index (-2^63 - 2^63-1)."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+8) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 8 > buf.size() || i < 0)
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 8 will be out of bounds.");
					return true;
				}
	
				returnValue.set(buf.getLong(index));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFPUTDOUBLE(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Sets a double-precision floating-point value in a buffer (64-bit). " +
					"BufferType byte order affects how the bytes are written. "
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.INTEGER, "The float value to set.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+8) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				float value = temp.isNull() ? 0f : temp.asFloat();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 8 > buf.size())
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 8 will be out of bounds.");
					return true;
				}
	
				buf.putDouble(index, value);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFGETDOUBLE(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a double-precision float in a buffer (64-bit). " +
					"BufferType byte order affects how the bytes are read."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.FLOAT, "The value at the index."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If [index, index+8) is out of the buffer's bounds.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
					return true;
				}
				
				BufferType buf = temp.asObjectType(BufferType.class);
				int i;
				if ((i = index == null ? buf.getPosition() : index) + 8 > buf.size() || i < 0)
				{
					returnValue.setError("OutOfBounds", "Index " + i + " + 8 will be out of bounds.");
					return true;
				}
	
				returnValue.set(buf.getDouble(index));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFPUTSTR(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Writes a string to a buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("value", 
					type(Type.STRING, "The string to write.")
				)
				.parameter("encoding", 
					type(Type.NULL, "Use native encoding."),
					type(Type.STRING, "The name of the encoding to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.BUFFER, "buffer."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "BadEncoding", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If not enough room to write the string.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				String encodingName = temp.isNull() ? null: temp.asString();
				scriptInstance.popStackValue(temp);
				String value = temp.asString();
				scriptInstance.popStackValue(temp);

				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
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
				
				try {
					BufferType buf = temp.asObjectType(BufferType.class);
					buf.putString(index, encoding, value);
				} catch (IndexOutOfBoundsException e) {
					returnValue.setError("OutOfBounds", e.getMessage(), e.getLocalizedMessage());
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
	
	BUFGETSTR(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets a string from the buffer."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer to use.")
				)
				.parameter("length", 
					type(Type.INTEGER, "The amount of bytes to read and decode into a string.")
				)
				.parameter("encoding", 
					type(Type.NULL, "Use native encoding."),
					type(Type.STRING, "The name of the encoding to use.")
				)
				.parameter("index", 
					type(Type.NULL, "Use the current position (cursor position will be advanced)."),
					type(Type.INTEGER, "The index into the buffer (cursor position will NOT be advanced).")
				)
				.returns(
					type(Type.STRING, "The string read and decoded."),
					type(Type.ERROR, "BadParameter", "If [buffer] is not a buffer."),
					type(Type.ERROR, "BadEncoding", "If [buffer] is not a buffer."),
					type(Type.ERROR, "OutOfBounds", "If not enough room to write the string.")
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
				Integer index = temp.isNull() ? null : temp.asInt();
				scriptInstance.popStackValue(temp);
				String encodingName = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				int length = temp.asInt();
				scriptInstance.popStackValue(temp);

				if (length <= 0)
				{
					returnValue.set("");
					return true;
				}
				
				if (!temp.isBuffer())
				{
					returnValue.setError("BadParameter", "First parameter is not a buffer.");
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
				
				try {
					returnValue.set(temp.asObjectType(BufferType.class).getString(index, encoding, length));
					return true;
				} catch (IndexOutOfBoundsException e) {
					returnValue.setError("OutOfBounds", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	BUFSTR(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Outputs a buffer's contents as a string of hexadecimal digits, from first byte to last byte."
				)
				.parameter("buffer", 
					type(Type.BUFFER, "The buffer.")
				)
				.returns(
					type(Type.NULL, "If the input is not a buffer."),
					type(Type.STRING, "A contiguous string of hexadecimal characters representing the buffer's contents, two characters per byte.")
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
				if (!temp.isBuffer())
				{
					returnValue.setNull();
					return true;
				}
				
				BufferType buffer = temp.asObjectType(BufferType.class);
				for (int i = 0; i < buffer.size(); i++)
				{
					byte b = buffer.getByte(i);
					sb.append(HEXALPHABET.charAt((b & 0x0f0) >> 4));
					sb.append(HEXALPHABET.charAt(b & 0x0f));
				}
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

	;
	
	private final int parameterCount;
	private Usage usage;
	private BufferFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(BufferFunctions.values());
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

	private static final String HEXALPHABET = "0123456789abcdef";
	
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE3 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<StringBuilder> STRINGBUILDER = ThreadLocal.withInitial(()->new StringBuilder(256));

}
