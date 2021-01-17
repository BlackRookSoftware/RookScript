/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
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
import com.blackrook.rookscript.struct.PatternUtils;
import com.blackrook.rookscript.struct.Utils;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Script common functions for files.
 * @author Matthew Tropiano
 */
public enum FileSystemFunctions implements ScriptFunctionType
{
	FILE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Wraps a path into an OBJECTREF:File. Useful for functions that expect file objects specifically."
				)
				.parameter("path", 
					type(Type.STRING, "Path to file."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.NULL, "If nothing was provided."),
					type(Type.OBJECTREF, "File", "A file object that represents the path.")
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
				if (file != null)
					returnValue.set(Type.OBJECTREF, file);
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

	FILEEXISTS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a file exists."
				)
				.parameter("path", 
					type(Type.STRING, "Path to file."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.BOOLEAN, "True if the file exists, false otherwise."),
					type(Type.ERROR, "Security", "If the OS is preventing the search.")
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
					if (file != null)
						returnValue.set(file.exists());
					else
						returnValue.set(false);
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

	FILEISDIR(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a file is a directory path."
				)
				.parameter("path", 
					type(Type.STRING, "Path to file."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.BOOLEAN, "True if the file is a directory, false otherwise."),
					type(Type.ERROR, "Security", "If the OS is preventing the read.")
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
					if (file != null)
						returnValue.set(file.isDirectory());
					else
						returnValue.set(false);
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

	FILEISHIDDEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a file is hidden."
				)
				.parameter("path", 
					type(Type.STRING, "Path to file."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.BOOLEAN, "True if the file is hidden, false otherwise."),
					type(Type.ERROR, "Security", "If the OS is preventing the read.")
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
					if (file != null)
						returnValue.set(file.isHidden());
					else
						returnValue.set(false);
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

	FILEPATH(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the path used to open the file."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.STRING, "The file's path.")
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
				if (file != null)
					returnValue.set(file.getPath());
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

	FILENAME(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the name of a file."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.STRING, "The file's name only.")
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
				if (file != null)
					returnValue.set(file.getName());
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

	/** @since 1.3.0 */
	FILENAMENOEXT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the name of a file, without extension."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.parameter("extension", 
					type(Type.NULL, "Use \".\""),
					type(Type.STRING, "The file extension separator string.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.STRING, "The file's name without its extension.")
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
				String ext = temp.isNull() ? "." : temp.asString();
				File file = popFile(scriptInstance, temp);
				if (file != null)
					returnValue.set(Utils.getFileNameWithoutExtension(file, ext));
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

	FILEEXT(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a file's extension."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.STRING, "The file's extension only. Empty string if no extension.")
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
				if (file != null)
					returnValue.set(Utils.getFileExtension(file, "."));
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

	FILELEN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a file's length in bytes."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.INTEGER, "The file's length."),
					type(Type.ERROR, "BadFile", "If the file could not be found."),
					type(Type.ERROR, "Security", "If the OS is preventing the read.")
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
						returnValue.setNull();
					else if (!file.exists())
						returnValue.setError("BadFile", "File \"" + file.getPath() + "\" not found.");
					else
						returnValue.set(file.length());
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

	FILEDATE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a file's modified date in milliseconds since Epoch (Jan. 1, 1970 UTC)."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.INTEGER, "The file's modified date in milliseconds since Epoch."),
					type(Type.ERROR, "BadFile", "If the file could not be found."),
					type(Type.ERROR, "Security", "If the OS is preventing the read.")
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
						returnValue.setNull();
					else if (!file.exists())
						returnValue.setError("BadFile", "File \"" + file.getPath() + "\" not found.");
					else
						returnValue.set(file.lastModified());
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

	FILEPARENT(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a file's parent path."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.STRING, "The file's parent path (if parameter was string)."),
					type(Type.OBJECTREF, "File", "The file's parent path (if parameter was file)."),
					type(Type.ERROR, "Security", "If the OS is preventing the search.")
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
						returnValue.setNull();
					else if (temp.isString())
						returnValue.set(file.getParent());
					else
						returnValue.set(file.getParentFile());
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

	FILEABSOLUTEPATH(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the absolute path of a file."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.STRING, "The file's absolute path (if parameter was string)."),
					type(Type.OBJECTREF, "File", "The file's absolute path (if parameter was file)."),
					type(Type.ERROR, "Security", "If the OS is preventing the search.")
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
						returnValue.setNull();
					else if (temp.isString())
						returnValue.set(file.getAbsolutePath());
					else
						returnValue.set(file.getAbsoluteFile());
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

	FILECANONPATH(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the canonical path of a file."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.returns(
					type(Type.NULL, "If [file] is null."),
					type(Type.STRING, "The file's canonical path (if parameter was string)."),
					type(Type.OBJECTREF, "File", "The file's canonical path (if parameter was file)."),
					type(Type.ERROR, "Security", "If the OS is preventing the search."),
					type(Type.ERROR, "IOError", "If an error occurred during search.")
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
						returnValue.setNull();
					else if (temp.isString())
						returnValue.set(file.getCanonicalPath());
					else
						returnValue.set(file.getCanonicalFile());
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

	FILELIST(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns all files in a directory (and optionally, whose name fits a RegEx pattern). " +
					"If recursive traversal is used, no directories will be added to the resultant list, just the files."
				)
				.parameter("path", 
					type(Type.STRING, "A file path to inspect."),
					type(Type.OBJECTREF, "File", "A file path to inspect.")
				)
				.parameter("recursive",
					type(Type.BOOLEAN, "If true, scan recursively.")
				)
				.parameter("regex",
					type(Type.NULL, "Include everything."),
					type(Type.STRING, "The pattern to match each file against.")
				)
				.returns(
					type(Type.NULL, "If not a directory or [path] is null."),
					type(Type.LIST, "[STRING, ...]", "A list of file paths in the directory (if parameter was string)."),
					type(Type.LIST, "[OBJECTREF:File, ...]", "A list of file paths in the directory (if parameter was file)."),
					type(Type.ERROR, "Security", "If the OS is preventing the search."),
					type(Type.ERROR, "BadPattern", "If the input RegEx pattern is malformed.")
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
				String regex = temp.isNull() ? null : temp.asString();
				scriptInstance.popStackValue(temp);
				boolean recursive = temp.asBoolean();
				File file = popFile(scriptInstance, temp);
				boolean wasString = temp.isString();
				
				FileFilter filter = ((f) -> true);
				if (regex != null)
				{
					try {
						final Pattern p = PatternUtils.get(regex);
						filter = ((f) -> p.matcher(f.getPath()).matches());
					} catch (PatternSyntaxException e) {
						returnValue.setError("BadPattern", e.getMessage(), e.getLocalizedMessage());
						return true;
					}
				}
				
				try {
					if (file == null || !file.isDirectory())
					{
						returnValue.setNull();
					}
					else
					{
						returnValue.setEmptyList(128);
						fillFiles(file, wasString, recursive, returnValue, filter);
					}
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

	FILEDELETE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Attempts to delete a file."
				)
				.parameter("path", 
					type(Type.STRING, "The file to delete."),
					type(Type.OBJECTREF, "File", "The file to delete.")
				)
				.returns(
					type(Type.NULL, "If [path] is null."),
					type(Type.BOOLEAN, "True if the file existed and was deleted, false otherwise."),
					type(Type.ERROR, "Security", "If the OS is preventing the delete.")
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
						returnValue.setNull();
					else if (!file.exists())
						returnValue.set(false);
					else
						returnValue.set(file.delete());
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

	FILERENAME(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Attempts to rename/move a file."
				)
				.parameter("path", 
					type(Type.STRING, "The file to rename."),
					type(Type.OBJECTREF, "File", "The file to rename.")
				)
				.parameter("newname", 
					type(Type.STRING, "The new path."),
					type(Type.OBJECTREF, "File", "The new path.")
				)
				.returns(
					type(Type.NULL, "If [path] or [newname] is null."),
					type(Type.BOOLEAN, "True if renamed/moved, false if not."),
					type(Type.ERROR, "Security", "If the OS is preventing the rename/move.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				File dest = popFile(scriptInstance, temp);
				File source = popFile(scriptInstance, temp);
				try {
					if (source == null || dest == null)
						returnValue.setNull();
					else if (!source.exists())
						returnValue.set(false);
					else
						returnValue.set(source.renameTo(dest));
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

	CREATEDIR(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Attempts to create a directory using an abstract pathname."
				)
				.parameter("path", 
					type(Type.STRING, "The directory to create."),
					type(Type.OBJECTREF, "File", "The directory to create.")
				)
				.returns(
					type(Type.BOOLEAN, "True if and only if the directory did not exist and was created, false if not."),
					type(Type.ERROR, "Security", "If the OS is preventing the creation.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				File dir = popFile(scriptInstance, temp);
				try {
					if (dir == null)
						returnValue.set(false);
					else
						returnValue.set(dir.mkdir());
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

	CREATEDIRS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Attempts to create a directory using an abstract pathname and all of directories in between, if they also don't exist. " +
					"NOTE: A failure may still involve some directories being created!"
				)
				.parameter("path", 
					type(Type.STRING, "The directory or directories to create."),
					type(Type.OBJECTREF, "File", "The directory or directories to create.")
				)
				.returns(
					type(Type.BOOLEAN, "True if and only if one or more directories did not exist before and were then created, and false if not."),
					type(Type.ERROR, "Security", "If the OS is preventing the creation.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				File dir = popFile(scriptInstance, temp);
				try {
					if (dir == null)
						returnValue.set(false);
					else
						returnValue.set(dir.mkdirs());
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

	VERIFYDIRS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Attempts to create a directory using an abstract pathname and all of the " +
					"directories in between, if they also don't exist, OR verify that the directory path that was specified already exists. " +
					"NOTE: A failure due to path creation may still involve some directories being created!"
				)
				.parameter("path", 
					type(Type.STRING, "The directory or directories to create."),
					type(Type.OBJECTREF, "File", "The directory or directories to create.")
				)
				.returns(
					type(Type.BOOLEAN, 
						"True if ALL of the directories did not exist and were created or if " +
						"the path is a directory and already exists, and false otherwise."
					),
					type(Type.ERROR, "Security", "If the OS is preventing the creation.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				File dir = popFile(scriptInstance, temp);
				try {
					if (dir == null)
						returnValue.set(false);
					else
						returnValue.set((dir.exists() && dir.isDirectory()) || dir.mkdirs());
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

	;
	
	private final int parameterCount;
	private Usage usage;
	private FileSystemFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(FileSystemFunctions.values());
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
	
	private static void fillFiles(File directory, boolean stringPaths, boolean recursive, ScriptValue list, FileFilter filter)
	{
		File[] files = directory.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			File f = files[i];
			if (recursive && f.isDirectory())
			{
				fillFiles(f, stringPaths, recursive, list, filter);
			}
			else if (filter.accept(f))
			{
				if (stringPaths)
					list.listAdd(f.getPath());
				else
					list.listAdd(f);
			}
		}
	}
	
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
