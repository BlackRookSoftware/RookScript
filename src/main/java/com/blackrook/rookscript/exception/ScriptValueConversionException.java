/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.exception;

/**
 * Exception that can be thrown during script value conversion.
 * @author Matthew Tropiano
 */
public class ScriptValueConversionException extends RuntimeException
{
	private static final long serialVersionUID = -2032491701653096911L;

	public ScriptValueConversionException()
	{
		super();
	}

	public ScriptValueConversionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ScriptValueConversionException(String message) 
	{
		super(message);
	}

	public ScriptValueConversionException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ScriptValueConversionException(Throwable cause)
	{
		super(cause);
	}

}
