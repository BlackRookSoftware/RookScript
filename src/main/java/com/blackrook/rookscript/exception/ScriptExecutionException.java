/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.exception;

/**
 * Exception that can be thrown during script execution.
 * @author Matthew Tropiano
 */
public class ScriptExecutionException extends RuntimeException
{
	private static final long serialVersionUID = -2032491701653096911L;

	public ScriptExecutionException()
	{
		super();
	}

	public ScriptExecutionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ScriptExecutionException(String message) 
	{
		super(message);
	}

	public ScriptExecutionException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ScriptExecutionException(Throwable cause)
	{
		super(cause);
	}

}
