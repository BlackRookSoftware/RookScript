/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptIteratorType;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Script common functions for date/time stuff.
 * @author Matthew Tropiano
 */
public enum ZipFunctions implements ScriptFunctionType
{
	ZFOPEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a Zip file. " +
					"The file, if opened, is registered as a resource, and will be closed when the script terminates."
				)
				.parameter("path", 
					type(Type.STRING, "Path to zip file. Relative paths are relative to working directory."),
					type(Type.OBJECTREF, "File", "Path to zip file. Relative paths are relative to working directory.")
				)
				.returns(
					type(Type.OBJECTREF, "ZipFile", "An open Zip file."),
					type(Type.ERROR, "Security", "If the OS denied opening the file for the required permissions."),
					type(Type.ERROR, "IOError", "If [path] is null or the file is not a Zip file, or it does could not be opened/found for some reason.")
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
				try {
					if (file == null)
						returnValue.setError("IOError", "A file was not provided.");
					else
					{
						ZipFile zf = new ZipFile(file);
						scriptInstance.registerCloseable(zf);
						returnValue.set(zf);
					}
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

	ZFENTRY(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a list of all of the entries in an open Zip File."
				)
				.parameter("zip", 
					type(Type.OBJECTREF, "ZipFile", "The open zip file.")
				)
				.parameter("entry", 
					type(Type.STRING, "The entry name.")
				)
				.returns(
					type(Type.NULL, "If an entry by that name could not be found."),
					type(Type.MAP, "{name:STRING, dir:BOOLEAN, size:INTEGER, time:INTEGER, comment:STRING, compressedsize:INTEGER, crc:INTEGER, creationtime:INTEGER, lastaccesstime:INTEGER, lastmodifiedtime:INTEGER}", "A map of entry info."),
					type(Type.ERROR, "BadParameter", "If an open zip file was not provided, or [entry] is null."),
					type(Type.ERROR, "IOError", "If a read error occurs, or the zip is not open.")
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
				String name = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(ZipFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open zip file.");
					return true;
				}
				if (name == null)
				{
					returnValue.setError("BadParameter", "No entry name provided.");
					return true;
				}

				ZipFile zf = temp.asObjectType(ZipFile.class);
				
				ZipEntry entry;
				try {
					entry = zf.getEntry(name);
				} catch (IllegalStateException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				if (entry != null)
					setEntryInfo(entry, returnValue);
				else
					returnValue.setNull();
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	ZFENTRIES(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a list of all of the entries in an open Zip File."
				)
				.parameter("zip", 
					type(Type.OBJECTREF, "ZipFile", "The open zip file.")
				)
				.returns(
					type(Type.LIST, "[MAP:{name:STRING, dir:BOOLEAN, size:INTEGER, time:INTEGER, comment:STRING, compressedsize:INTEGER, crc:INTEGER, creationtime:INTEGER, lastaccesstime:INTEGER, lastmodifiedtime:INTEGER}, ...]", "A list of maps of entry info."),
					type(Type.ERROR, "BadParameter", "If an open zip file was not provided."),
					type(Type.ERROR, "IOError", "If a read error occurs, or the zip is not open.")
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
				if (!temp.isObjectType(ZipFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open zip file.");
					return true;
				}

				ZipFile zf = temp.asObjectType(ZipFile.class);
				
				Enumeration<? extends ZipEntry> entryEnum;
				try {
					entryEnum = zf.entries();
				} catch (IllegalStateException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				returnValue.setEmptyList(zf.size());
				while (entryEnum.hasMoreElements())
				{
					setEntryInfo(entryEnum.nextElement(), temp);
					returnValue.listAdd(temp);
				}
				
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	/** @since 1.3.0 */
	ZFITERATE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a list of all of the entries in an open Zip File."
				)
				.parameter("zip", 
					type(Type.OBJECTREF, "Script", "The open zip file.")
				)
				.returns(
					type(Type.OBJECTREF, "ScriptIteratorType", "An iterator for each entry - Key: name:STRING, value: MAP{name:STRING, dir:BOOLEAN, size:INTEGER, time:INTEGER, comment:STRING, compressedsize:INTEGER, crc:INTEGER, creationtime:INTEGER, lastaccesstime:INTEGER, lastmodifiedtime:INTEGER}."),
					type(Type.ERROR, "BadParameter", "If an open zip file was not provided."),
					type(Type.ERROR, "IOError", "If a read error occurs, or the zip is not open.")
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
				if (!temp.isObjectType(ZipFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open zip file.");
					return true;
				}

				final Enumeration<? extends ZipEntry> entryEnum;
				try {
					entryEnum = temp.asObjectType(ZipFile.class).entries();
				} catch (IllegalStateException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
				
				returnValue.set(new ScriptIteratorType() 
				{
					private final IteratorPair pair = new IteratorPair();
					private final Enumeration<? extends ZipEntry> en = entryEnum;
					
					@Override
					public IteratorPair next() 
					{
						ZipEntry ze = en.nextElement();
						pair.set(ze.getName(), null);
						setEntryInfo(ze, pair.getValue());
						return pair;
					}
					
					@Override
					public boolean hasNext()
					{
						return en.hasMoreElements();
					}
				});
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	ZFEOPEN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data input stream for reading from a zip file entry (and registers this resource as an open resource)."
				)
				.parameter("zip", 
					type(Type.OBJECTREF, "ZipFile", "The open zip file.")
				)
				.parameter("entry", 
					type(Type.STRING, "The entry name."),
					type(Type.MAP, "{... name:STRING ...}", "A map of zip entry info containing the name of the entry.")
				)
				.returns(
					type(Type.OBJECTREF, "DataInputStream", "An open data input stream to read from."),
					type(Type.ERROR, "BadParameter", "If an open zip file was not provided, or [entry] is null or [entry].name is null."),
					type(Type.ERROR, "BadEntry", "If [entry] could not be found in the zip."),
					type(Type.ERROR, "IOError", "If a read error occurs, or the zip is not open.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue temp2 = CACHEVALUE2.get();
			try 
			{
				scriptInstance.popStackValue(temp);
				String name;
				if (temp.isNull())
					name = null;
				else if (temp.isMap())
				{
					temp.mapGet("name", temp2);
					name = temp2.isNull() ? null : temp2.asString();
				}
				else
					name = temp.asString();
				
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectType(ZipFile.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an open zip file.");
					return true;
				}
				if (name == null)
				{
					returnValue.setError("BadParameter", "No entry name provided.");
					return true;
				}

				ZipFile zf = temp.asObjectType(ZipFile.class);
				
				ZipEntry entry;
				try {
					entry = zf.getEntry(name);
					if (entry == null)
					{
						returnValue.setError("BadEntry", "Entry named \"" + name + "\" could not be found.");
					}
					else
					{
						DataInputStream in = new DataInputStream(zf.getInputStream(entry));
						scriptInstance.registerCloseable(in);
						returnValue.set(in);
					}
				} catch (IllegalStateException | IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
				}
				return true;
			}
			finally
			{
				temp.setNull();
				temp2.setNull();
			}
		}
	},

	/** @since 1.1.0 */
	GZISOPEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data input stream for reading from a GZIP stream (and registers this resource as an open resource)."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "InputStream", "A valid open input stream.")
				)
				.returns(
					type(Type.OBJECTREF, "DataInputStream", "An open data input stream to read from."),
					type(Type.ERROR, "BadParameter", "If an open input stream was not provided."),
					type(Type.ERROR, "BadStream", "If the provided input stream is not a GZIPped stream of data."),
					type(Type.ERROR, "IOError", "If a read error occurs, or the provided stream is not open.")
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
				if (temp.isNull())
				{
					returnValue.setNull();
					return true;
				}
				if (!temp.isObjectRef(InputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an input stream type.");
					return true;
				}

				InputStream in = temp.asObjectType(InputStream.class);
				
				DataInputStream gzin;
				try {
					gzin = new DataInputStream(new GZIPInputStream(in));
				} catch (ZipException e) {
					returnValue.setError("BadStream", e.getLocalizedMessage());
					return true;
				} catch (IOException e) {
					returnValue.setError("IOError", e.getLocalizedMessage());
					return true;
				}

				if (scriptInstance.closeableIsRegistered(in))
				{
					scriptInstance.unregisterCloseable(in);
					scriptInstance.registerCloseable(gzin);
				}
				returnValue.set(gzin);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	/** @since 1.1.0 */
	GZOSOPEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Opens a data output stream for writing to a GZIP stream (and registers this resource as an open resource)."
				)
				.parameter("instream", 
					type(Type.OBJECTREF, "OutputStream", "A valid open output stream.")
				)
				.returns(
					type(Type.OBJECTREF, "DataOutput", "An open data output stream to read from."),
					type(Type.ERROR, "BadParameter", "If an open output stream was not provided."),
					type(Type.ERROR, "IOError", "If a write error occurs, or the provided stream is not open.")
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
				if (temp.isNull())
				{
					returnValue.setNull();
					return true;
				}
				if (!temp.isObjectRef(OutputStream.class))
				{
					returnValue.setError("BadParameter", "First parameter is not an output stream type.");
					return true;
				}

				OutputStream out = temp.asObjectType(OutputStream.class);
				
				DataOutputStream gzout;
				try {
					gzout = new DataOutputStream(new GZIPOutputStream(out));
				} catch (IOException e) {
					returnValue.setError("IOError", e.getLocalizedMessage());
					return true;
				}

				if (scriptInstance.closeableIsRegistered(out))
				{
					scriptInstance.unregisterCloseable(out);
					scriptInstance.registerCloseable(gzout);
				}
				returnValue.set(gzout);
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
	private ZipFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(ZipFunctions.values());
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
	 * Sets a script value to a map with zip entry data.
	 * @param entry the zip entry.
	 * @param out the value to change.
	 */
	protected void setEntryInfo(ZipEntry entry, ScriptValue out) 
	{
		out.setEmptyMap(8);
		
		if (entry.getComment() != null)
			out.mapSet("comment", entry.getComment());
		if (entry.getCompressedSize() >= 0)
			out.mapSet("compressedsize", entry.getCompressedSize());
		if (entry.getCrc() >= 0)
			out.mapSet("crc", entry.getCrc());
		if (entry.getCreationTime() != null)
			out.mapSet("creationtime", entry.getCreationTime().toMillis());
		
		out.mapSet("dir", entry.isDirectory());

		if (entry.getLastAccessTime() != null)
			out.mapSet("lastaccesstime", entry.getLastAccessTime().toMillis());
		if (entry.getLastModifiedTime() != null)
			out.mapSet("lastmodifiedtime", entry.getLastModifiedTime().toMillis());
		
		out.mapSet("name", entry.getName());
		
		if (entry.getSize() >= 0)
			out.mapSet("size", entry.getSize());
		if (entry.getTime() >= 0)
			out.mapSet("time", entry.getTime());
	}
	
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
