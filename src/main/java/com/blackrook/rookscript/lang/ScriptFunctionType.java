/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.lang;

import java.util.List;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
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
	 * @return the total amount of parameters this takes.
	 */
	public int getParameterCount();
	
	/**
	 * @return this function's usage instructions.
	 */
	public Usage getUsage();
	
	/**
	 * Executes this function. The value in <code>returnValue</code> is automatically pushed onto the stack after the call.
	 * If a {@link Throwable} is thrown from this call, it is wrapped in a {@link ScriptExecutionException}.
	 * {@link ScriptExecutionException}s are thrown as-is.
	 * @param scriptInstance the originating script instance.
	 * @param returnValue the value pushed to the stack on return.
	 * @return if false, this halts script execution, else if true, continue.
	 */
	public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	/**
	 * Function usage info.
	 */
	public interface Usage
	{
		/**
		 * Gets the function usage instructions.
		 * @return the function usage instructions. Never returns null.
		 */
		String getInstructions();
		
		/**
		 * Gets the usage instructions per function parameter, in the order of the functions parameters.
		 * Can be null for no instructions. Must match function's parameter count.
		 * @return the usage instructions per function parameter. Never returns null.
		 * @see ScriptFunctionType#getParameterCount()
		 */
		List<ParameterUsage> getParameterInstructions();

		/**
		 * Gets a list of accepted, expected return types for this function.
		 * @return the list of return types. Never returns null.
		 */
		List<TypeUsage> getReturnTypes();

		/**
		 * A single parameter's usage instructions.
		 */
		public interface ParameterUsage
		{
			/**
			 * @return the name of the parameter.
			 */
			String getParameterName();
			
			/**
			 * Gets a list of accepted parameter types for this parameter.
			 * Other types may result in an error.
			 * @return the list of accepted types and usages. Never returns null.
			 */
			List<TypeUsage> getTypes();
			
		}
		
		/**
		 * Per-relevant-type usage.
		 */
		public interface TypeUsage
		{
			/**
			 * @return the script value type. If this returns null, it means "any type."
			 */
			ScriptValue.Type getType();
			
			/**
			 * @return the object ref type. Can return null.
			 */
			String getObjectRefType();
			
			/**
			 * @return the description.
			 */
			String getDescription();
			
		}
		
	}

}
