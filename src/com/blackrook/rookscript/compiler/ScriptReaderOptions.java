/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.compiler;

/**
 * An interface for script reader options. 
 * These influence reader/compiler behavior.
 * @author Matthew Tropiano
 */
public interface ScriptReaderOptions 
{
	/**
	 * Checks if this reader is supposed to optimize scripts after compile.
	 * @return true if so, false if not.
	 */
	public boolean isOptimizing();
	
	/**
	 * Gets what to predefine in the preprocessor.
	 * This can affect what gets compiled and what doesn't.
	 * Must not return null.
	 * @return a list of defined tokens.
	 */
	public String[] getDefines();
	
}
