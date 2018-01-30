package com.blackrook.rookscript.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import com.blackrook.lang.CommonLexer;

/**
 * The lexer for a script reader context.
 * @author Matthew Tropiano
 */
public class ScriptLexer extends CommonLexer
{
	/** The includer to use. */
	private ScriptReaderIncluder includer;
	
	/**
	 * Creates a new lexer around a String, that will
	 * be wrapped into a StringReader class chain.
	 * This will also assign this lexer a default name.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param in the input stream to read from.
	 * @param includer the script reader includer to use.
	 */
	public ScriptLexer(ScriptKernel kernel, Reader in, ScriptReaderIncluder includer)
	{
		super(kernel, in);
		this.includer = includer;
	}

	/**
	 * Constructs this script lexer from a reader.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param in the reader to read from.
	 * @param includer the script reader includer to use.
	 */
	public ScriptLexer(ScriptKernel kernel, String in, ScriptReaderIncluder includer)
	{
		super(kernel, in);
		this.includer = includer;
	}
	
	/**
	 * Creates a new script lexer around a String, that will
	 * be wrapped into a StringReader class chain.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param name	the name of this lexer.
	 * @param in the input stream to read from.
	 * @param includer the script reader includer to use.
	 */
	public ScriptLexer(ScriptKernel kernel, String name, Reader in, ScriptReaderIncluder includer)
	{
		super(kernel, name, in);
		this.includer = includer;
	}

	/**
	 * Creates a new script lexer from a reader.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param name the name of this lexer.
	 * @param in the reader to read from.
	 * @param includer the script reader includer to use.
	 */
	public ScriptLexer(ScriptKernel kernel, String name, String in, ScriptReaderIncluder includer)
	{
		super(kernel, name, in);
		this.includer = includer;
	}
	
	@Override
	protected String getNextResourceName(String currentStreamName, String includePath) throws IOException 
	{
		return includer.getIncludeResourceName(currentStreamName, includePath);
	}
	
	@Override
	protected InputStream getResource(String path) throws IOException
	{
		return includer.getIncludeResource(path);
	}
}

