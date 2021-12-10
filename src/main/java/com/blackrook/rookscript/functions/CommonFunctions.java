/*******************************************************************************
 * Copyright (c) 2017-2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.functions.common.BufferFunctions;
import com.blackrook.rookscript.functions.common.ErrorFunctions;
import com.blackrook.rookscript.functions.common.ListFunctions;
import com.blackrook.rookscript.functions.common.MapFunctions;
import com.blackrook.rookscript.functions.common.MiscFunctions;
import com.blackrook.rookscript.functions.common.StringFunctions;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.CompoundFunctionResolver;

/**
 * RookScript common functions.
 * Contains all functions in {@link ErrorFunctions}, {@link ListFunctions}, {@link MapFunctions}, 
 * {@link StringFunctions}, {@link BufferFunctions}, and {@link MiscFunctions}.
 * @author Matthew Tropiano
 */
public final class CommonFunctions
{
	private CommonFunctions() {}
	
	/**
	 * @return a function resolver that provides all of the common RookScript functions.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new CompoundFunctionResolver()
			.addResolver(new EnumFunctionResolver(MiscFunctions.values()))
			.addResolver(new EnumFunctionResolver(ErrorFunctions.values()))
			.addResolver(new EnumFunctionResolver(StringFunctions.values()))
			.addResolver(new EnumFunctionResolver(ListFunctions.values()))
			.addResolver(new EnumFunctionResolver(MapFunctions.values()))
			.addResolver(new EnumFunctionResolver(BufferFunctions.values()))
		;
	}

}
