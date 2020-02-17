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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Script common functions for standard input/output.
 * @author Matthew Tropiano
 * @since [NOW]
 */
public enum DigestFunctions implements ScriptFunctionType
{
	HASHSTR(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Takes a buffer and outputs its contents as a string of hexadecimal digits."
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

	DIGEST(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Takes a buffer or input stream and outputs a buffer containing bytes in the desired hash digest. " +
					"This is meant to be a pure convenience function - use the other digest functions for greater hash calculation control. " +
					"Input streams are not closed after this completes."
				)
				.parameter("data",
					type(Type.BUFFER, "The buffer to read fully (from index 0)."),
					type(Type.OBJECTREF, "File", "The file read from (until end-of-file)."),
					type(Type.OBJECTREF, "InputStream", "The input stream to read from (until end-of-stream).")
				)
				.parameter("algorithm",
					type(Type.STRING, "The name of the algorithm to use.")
				)
				.returns(
					type(Type.BUFFER, "A buffer containng the resultant hash digest."),
					type(Type.ERROR, "BadParameter", "If [data] is not one of the required types."),
					type(Type.ERROR, "BadFile", "If [data] is a file, and it does not exist."),
					type(Type.ERROR, "BadAlgorithm", "If [algorithm] is not supported by the host."),
					type(Type.ERROR, "Security", "If the OS is preventing opening a file for reading, or the file is a directory."),
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
				String algo = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);

				InputStream in;
				boolean closeStream = false;
				
				if (temp.isBuffer())
					in = temp.asObjectType(BufferType.class).getAbsoluteInputStream();
				else if (temp.isObjectRef(InputStream.class))
					in = temp.asObjectType(InputStream.class);
				else if (temp.isObjectRef(File.class))
				{
					try {
						in = new FileInputStream(temp.asObjectType(File.class));
						closeStream = true;
					} catch (SecurityException e) {
						returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
						return true;
					} catch (FileNotFoundException e) {
						returnValue.setError("BadFile", e.getMessage(), e.getLocalizedMessage());
						return true;
					} 
				}
				else
				{
					returnValue.setError("BadParameter", "First parameter is not an expected type.");
					return true;
				}

				if (algo == null)
				{
					returnValue.setError("BadAlgorithm", "Hash algorithm was not provided.");
					return true;
				}
				
				MessageDigest digest;
				try {
					digest = MessageDigest.getInstance(algo);
					byte[] buf = new byte[2048];
					int b;
					while ((b = in.read(buf)) > 0)
						digest.update(buf, 0, b);
				} catch (NoSuchAlgorithmException e) {
					returnValue.setError("BadAlgorithm", "Hash algorithm is not available: " + algo);
					return true;
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				} finally {
					if (closeStream) Utils.close(in);
				}

				byte[] hash = digest.digest();
				returnValue.setEmptyBuffer(hash.length);
				returnValue.asObjectType(BufferType.class).readBytes(0, hash, 0, hash.length);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	MD5(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Takes a buffer or input stream and outputs a buffer containing bytes using the MD5 hash digest. " +
					"This is meant to be a pure convenience function - use the other digest functions for greater hash calculation control. " +
					"Input streams are not closed after this completes."
				)
				.parameter("data",
					type(Type.BUFFER, "The buffer to read fully (from index 0)."),
					type(Type.OBJECTREF, "File", "The file read from (until end-of-file)."),
					type(Type.OBJECTREF, "InputStream", "The input stream to read from (until end-of-stream).")
				)
				.returns(
					type(Type.BUFFER, "A buffer containng the resultant hash digest."),
					type(Type.ERROR, "BadParameter", "If [data] is not one of the required types."),
					type(Type.ERROR, "BadFile", "If [data] is a file, and it does not exist."),
					type(Type.ERROR, "Security", "If the OS is preventing opening a file for reading, or the file is a directory."),
					type(Type.ERROR, "IOError", "If a read error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE2.get(); // V1 is used by DIGEST
			try
			{
				// The data source to use is already on the stack - just push "md5".
				scriptInstance.pushStackValue("MD5");
				DIGEST.execute(scriptInstance, temp);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	SHA1(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Takes a buffer or input stream and outputs a buffer containing bytes using the SHA1 hash digest. " +
					"This is meant to be a pure convenience function - use the other digest functions for greater hash calculation control. " +
					"Input streams are not closed after this completes."
				)
				.parameter("data",
					type(Type.BUFFER, "The buffer to read fully (from index 0)."),
					type(Type.OBJECTREF, "File", "The file read from (until end-of-file)."),
					type(Type.OBJECTREF, "InputStream", "The input stream to read from (until end-of-stream).")
				)
				.returns(
					type(Type.BUFFER, "A buffer containng the resultant hash digest."),
					type(Type.ERROR, "BadParameter", "If [data] is not one of the required types."),
					type(Type.ERROR, "BadFile", "If [data] is a file, and it does not exist."),
					type(Type.ERROR, "Security", "If the OS is preventing opening a file for reading, or the file is a directory."),
					type(Type.ERROR, "IOError", "If a read error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE2.get(); // V1 is used by DIGEST
			try
			{
				// The data source to use is already on the stack - just push "sha-1".
				scriptInstance.pushStackValue("SHA-1");
				DIGEST.execute(scriptInstance, temp);
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	SHA256(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Takes a buffer or input stream and outputs a buffer containing bytes using the SHA256 hash digest. " +
					"This is meant to be a pure convenience function - use the other digest functions for greater hash calculation control. " +
					"Input streams are not closed after this completes."
				)
				.parameter("data",
					type(Type.BUFFER, "The buffer to read fully (from index 0)."),
					type(Type.OBJECTREF, "File", "The file read from (until end-of-file)."),
					type(Type.OBJECTREF, "InputStream", "The input stream to read from (until end-of-stream).")
				)
				.returns(
					type(Type.BUFFER, "A buffer containng the resultant hash digest."),
					type(Type.ERROR, "BadParameter", "If [data] is not one of the required types."),
					type(Type.ERROR, "BadFile", "If [data] is a file, and it does not exist."),
					type(Type.ERROR, "Security", "If the OS is preventing opening a file for reading, or the file is a directory."),
					type(Type.ERROR, "IOError", "If a read error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE2.get(); // V1 is used by DIGEST
			try
			{
				// The data source to use is already on the stack - just push "sha-256".
				scriptInstance.pushStackValue("SHA-256");
				DIGEST.execute(scriptInstance, temp);
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
	private DigestFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(DigestFunctions.values());
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
	private static final ThreadLocal<StringBuilder> STRINGBUILDER = ThreadLocal.withInitial(()->new StringBuilder(64));

}
