/*******************************************************************************
 * Copyright (c) 2017-2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.compiler;

import java.io.Reader;

import com.blackrook.rookscript.struct.PreprocessorLexer;

/**
 * The lexer for a script reader context.
 * @author Matthew Tropiano
 */
public class ScriptLexer extends PreprocessorLexer
{
	/**
	 * Creates a new lexer around a String, that will
	 * be wrapped into a StringReader class chain.
	 * This will also assign this lexer a default name.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param in the input stream to read from.
	 * @param includer the script reader includer to use.
	 * @param options the script reader options to use.
	 */
	public ScriptLexer(ScriptKernel kernel, Reader in, ScriptReaderIncluder includer, ScriptReaderOptions options)
	{
		super(kernel, in);
		setIncluder(includer);
		for (String define : options.getDefines())
			addDefine(define, "");
	}

	/**
	 * Creates a new script lexer around a String, that will
	 * be wrapped into a StringReader class chain.
	 * @param kernel the lexer kernel to use for defining how to parse the input text.
	 * @param name	the name of this lexer.
	 * @param in the input stream to read from.
	 * @param includer the script reader includer to use.
	 * @param options the script reader options to use.
	 */
	public ScriptLexer(ScriptKernel kernel, String name, Reader in, ScriptReaderIncluder includer, ScriptReaderOptions options)
	{
		super(kernel, name, in);
		setIncluder(includer);
		for (String define : options.getDefines())
			addDefine(define, "");
	}

}

