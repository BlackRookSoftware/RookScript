/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import com.blackrook.rookscript.exception.ScriptExecutionException;

/**
 * Host function type for scripts.
 * @author Matthew Tropiano
 */
public interface ScriptFunctionType
{
	/**
	 * Gets the name of this function.
	 * This name returned must be a valid name that can be parsed in the script ("identifier" type: starts with letter, alphanumeric plus "_").
	 * @return the function name.
	 */
	public String name();
	
	/**
	 * Checks if this function returns nothing.
	 * @return true if so, false if not.
	 */
	public boolean isVoid();
	
	/**
	 * @return the total amount of parameters this takes.
	 */
	public int getParameterCount();
	
	/**
	 * @return this function's usage instructions.
	 */
	public Usage getUsage();
	
	/**
	 * Executes this function.
	 * If a {@link Throwable} is thrown from this call, it is wrapped in a {@link ScriptExecutionException}.
	 * {@link ScriptExecutionException}s are thrown as-is.
	 * @param scriptInstance the originating script instance.
	 * @return if false, this halts script execution, else if true, continue.
	 */
	public boolean execute(ScriptInstance scriptInstance);

	/**
	 * Function usage info.
	 */
	public static interface Usage
	{
		/**
		 * Gets the function usage instructions.
		 * @return the function usage instructions.
		 */
		public String getUsageInstructions();
		
		/**
		 * Gets the usage instructions per function parameter.
		 * Can be null for no instructions. Must match function's parameter count.
		 * @return the usage instructions per function parameter.
		 * @see ScriptFunctionType#getParameterCount()
		 */
		public String[] getUsageParameterInstructions();

	}
	
}
