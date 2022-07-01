/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.exception;

import com.blackrook.rookscript.compiler.ScriptParser.ErrorMessage;

/**
 * Exception that can be thrown during script parse.
 * @author Matthew Tropiano
 */
public class ScriptParseException extends RuntimeException
{
	private static final long serialVersionUID = -2032491701653096911L;
	
	private ErrorMessage[] errorMessages;

	public ScriptParseException()
	{
		super();
	}

	public ScriptParseException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ScriptParseException(String message) 
	{
		super(message);
	}

	public ScriptParseException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ScriptParseException(Throwable cause)
	{
		super(cause);
	}

	public ScriptParseException(ErrorMessage ... errorMessages)
	{
		super(mergeMessages(errorMessages));
		this.errorMessages = errorMessages;
	}

	/**
	 * @return the list of error messages used to construct this exception.
	 * @since 1.11.0
	 */
	public ErrorMessage[] getErrorMessages()
	{
		return errorMessages;
	}
	
	private static String mergeMessages(ErrorMessage ... errorMessages)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < errorMessages.length; i++)
		{
			sb.append(errorMessages[i].toString());
			if (i < errorMessages.length - 1)
				sb.append('\n');
		}
		return sb.toString();
	}
	
}
