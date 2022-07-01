/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.functions.io.DataIOFunctions;
import com.blackrook.rookscript.functions.io.FileIOFunctions;
import com.blackrook.rookscript.functions.io.StreamingIOFunctions;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.CompoundFunctionResolver;

/**
 * RookScript associated I/O functions.
 * Contains all functions in {@link FileIOFunctions}, {@link StreamingIOFunctions}, and {@link DataIOFunctions}.
 * @author Matthew Tropiano
 */
public final class IOFunctions
{
	private IOFunctions() {}
	
	/**
	 * @return a function resolver that provides all of the I/O-based RookScript functions.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new CompoundFunctionResolver()
			.addResolver(new EnumFunctionResolver(FileIOFunctions.values()))
			.addResolver(new EnumFunctionResolver(StreamingIOFunctions.values()))
			.addResolver(new EnumFunctionResolver(DataIOFunctions.values()))
		;
	}

}
