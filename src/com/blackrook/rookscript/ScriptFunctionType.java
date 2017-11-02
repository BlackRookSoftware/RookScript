/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

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
	 * Executes this function.
	 * @param scriptInstance the originating script instance.
	 * @return if false, this halts script execution, else if true, continue.
	 */
	public boolean execute(ScriptInstance scriptInstance);

}
