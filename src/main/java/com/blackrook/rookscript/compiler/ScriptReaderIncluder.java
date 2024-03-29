/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.compiler;

import com.blackrook.rookscript.struct.PreprocessorLexer;

/**
 * An interface that allows the user to resolve a resource by path when the
 * {@link ScriptReader} parses it. This is a "rebrand" in case that this needs to be expanded in the future.
 * @author Matthew Tropiano
 */
public interface ScriptReaderIncluder extends PreprocessorLexer.Includer
{
}
