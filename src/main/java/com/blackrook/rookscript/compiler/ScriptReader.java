/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import com.blackrook.rookscript.Script;
import com.blackrook.rookscript.ScriptAssembler;
import com.blackrook.rookscript.exception.ScriptParseException;
import com.blackrook.rookscript.resolvers.ScriptHostFunctionResolver;
import com.blackrook.rookscript.struct.Utils;

/**
 * The main factory for reading and parsing a script file.
 * @author Matthew Tropiano
 */
public final class ScriptReader
{
	public static final String STREAMNAME_TEXT = "[Text String]";
	
	/** The singular instance for the kernel. */
	private static final ScriptKernel KERNEL_INSTANCE = new ScriptKernel();
	/** The singular instance for the default includer. */
	public static final ScriptReaderIncluder DEFAULT_INCLUDER = new DefaultIncluder();
	/** The singular instance for default options. */
	public static final ScriptReaderOptions DEFAULT_OPTIONS = new ScriptReaderOptions()
	{
		@Override
		public String[] getDefines()
		{
			return new String[]{};
		}
	};
	
	/** 
	 * Default includer to use when none specified.
	 * This includer can either pull from the classpath, URIs, or files.
	 * <p>&nbsp;</p>
	 * <ul>
	 * <li>Paths that start with {@code classpath:} are parsed as resource paths in the current classpath.</li>
	 * <li>
	 * 		Else, the path is interpreted as a file path, with the following search order:
	 * 		<ul>
	 * 			<li>Relative to parent of source stream.</li>
	 * 			<li>As is.</li>
	 * 		</ul>
	 * </li>
	 * </ul> 
	 */
	public static class DefaultIncluder implements ScriptReaderIncluder
	{
		private static final String CLASSPATH_PREFIX = "classpath:";
		
		// cannot be instantiated outside of this class.
		private DefaultIncluder(){}

		@Override
		public String getIncludeResourcePath(String streamName, String path) throws IOException
		{
			if (Utils.isWindows() && streamName.contains("\\")) // check for Windows paths.
				streamName = streamName.replace('\\', '/');
			
			String streamParent = null;
			int lidx = -1; 
			if ((lidx = streamName.lastIndexOf('/')) >= 0)
				streamParent = streamName.substring(0, lidx + 1);

			if (path.startsWith(CLASSPATH_PREFIX) || (streamParent != null && streamParent.startsWith(CLASSPATH_PREFIX)))
				return ((streamParent != null ? streamParent : "") + path);
			else
			{
				File f = null;
				if (streamParent != null)
				{
					f = new File(streamParent + path);
					if (f.exists())
						return f.getPath();
					else
						return path;
				}
				else
				{
					return path;
				}
			}
		}
		
		@Override
		public InputStream getIncludeResource(String path) throws IOException
		{
			if (path.startsWith(CLASSPATH_PREFIX))
				return Utils.openResource(path.substring(CLASSPATH_PREFIX.length()));
			else
				return new FileInputStream(new File(path));
		}
	};
	
	private ScriptReader() {}
		
	/**
	 * Reads a script from a String of text.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(String text, ScriptHostFunctionResolver resolver) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), resolver, DEFAULT_INCLUDER, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script from a String of text.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if text is null. 
	 */
	public static Script read(String text, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), resolver, includer, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script from a String of text.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @param options the reader options to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if text is null. 
	 */
	public static Script read(String text, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer, ScriptReaderOptions options) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), resolver, includer, options);
	}

	/**
	 * Reads a script from a String of text.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(String streamName, String text, ScriptHostFunctionResolver resolver) throws IOException
	{
		return read(streamName, new StringReader(text), resolver, DEFAULT_INCLUDER, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script from a String of text.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if text is null. 
	 */
	public static Script read(String streamName, String text, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		return read(streamName, new StringReader(text), resolver, includer, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script from a String of text.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @param options the reader options to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if text is null. 
	 */
	public static Script read(String streamName, String text, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer, ScriptReaderOptions options) throws IOException
	{
		return read(streamName, new StringReader(text), resolver, includer, options);
	}

	/**
	 * Reads a script from a starting text file.
	 * @param file the file to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(File file, ScriptHostFunctionResolver resolver) throws IOException
	{
		return read(file, resolver, DEFAULT_INCLUDER, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script from a starting text file.
	 * @param file	the file to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(File file, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		return read(file, resolver, includer, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script from a starting text file.
	 * @param file	the file to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @param options the reader options to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(File file, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer, ScriptReaderOptions options) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return read(file.getPath(), fis, resolver, includer, options);
		} finally {
			Utils.close(fis);
		}
	}

	/**
	 * Reads a script.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static Script read(String streamName, InputStream in, ScriptHostFunctionResolver resolver) throws IOException
	{
		return read(streamName, new InputStreamReader(in), resolver, DEFAULT_INCLUDER, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static Script read(String streamName, InputStream in, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		return read(streamName, new InputStreamReader(in), resolver, includer, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @param options the reader options to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static Script read(String streamName, InputStream in, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer, ScriptReaderOptions options) throws IOException
	{
		return read(streamName, new InputStreamReader(in), resolver, includer, options);
	}

	/**
	 * Reads a script from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if f is null. 
	 */
	public static Script read(String streamName, Reader reader, ScriptHostFunctionResolver resolver) throws IOException
	{
		return read(streamName, reader, resolver, DEFAULT_INCLUDER, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if reader is null. 
	 */
	public static Script read(String streamName, Reader reader, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		return read(streamName, reader, resolver, includer, DEFAULT_OPTIONS);
	}

	/**
	 * Reads a script from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @param options the reader options to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if reader is null. 
	 */
	public static Script read(String streamName, Reader reader, ScriptHostFunctionResolver resolver, ScriptReaderIncluder includer, ScriptReaderOptions options) throws IOException
	{
		Script script = new Script();
		script.setHostFunctionResolver(resolver);
		(new ScriptParser(new ScriptLexer(KERNEL_INSTANCE, streamName, reader, includer, options))).readScript(script);
		return ScriptAssembler.optimize(script);
	}

}

