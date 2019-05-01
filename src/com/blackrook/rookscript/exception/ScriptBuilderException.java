/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.exception;

import com.blackrook.rookscript.ScriptBuilder;

/**
 * Exception that can be thrown from {@link ScriptBuilder}.
 * @author Matthew Tropiano
 */
public class ScriptBuilderException extends RuntimeException
{
	private static final long serialVersionUID = -2032491701653096911L;

	public ScriptBuilderException()
	{
		super();
	}

	public ScriptBuilderException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ScriptBuilderException(String message) 
	{
		super(message);
	}

	public ScriptBuilderException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ScriptBuilderException(Throwable cause)
	{
		super(cause);
	}

}
