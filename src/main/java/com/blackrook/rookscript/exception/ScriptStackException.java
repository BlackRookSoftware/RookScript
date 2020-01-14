/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.exception;

/**
 * Exception that can be thrown for script stack problems.
 * @author Matthew Tropiano
 */
public class ScriptStackException extends RuntimeException
{
	private static final long serialVersionUID = 2294405398523398608L;

	public ScriptStackException()
	{
		super();
	}

	public ScriptStackException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ScriptStackException(String message) 
	{
		super(message);
	}

	public ScriptStackException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ScriptStackException(Throwable cause)
	{
		super(cause);
	}

}
