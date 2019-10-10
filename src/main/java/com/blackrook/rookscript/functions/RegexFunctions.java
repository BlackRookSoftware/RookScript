/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.blackrook.rookscript.ScriptFunctionResolver;
import com.blackrook.rookscript.ScriptFunctionType;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue.ErrorType;
import com.blackrook.rookscript.resolvers.function.EnumFunctionResolver;
import com.blackrook.rookscript.struct.PatternUtils;

/**
 * A set of RegEx functions.
 * @author Matthew Tropiano
 */
public enum RegexFunctions implements ScriptFunctionType
{
	
	/**
	 * Checks if a string is a valid RegEx pattern.
	 * If the pattern is malformed, this returns false, else true.
	 * ARG1: The string. 
	 */
	ISREGEX(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			String regex = scriptInstance.popStackValue().asString();
			
			try {
				PatternUtils.get(regex);
			} catch (PatternSyntaxException e) {
				scriptInstance.pushStackValue(false);
				return true;
			}
			
			scriptInstance.pushStackValue(true);
			return true;
		}
	},
	
	/**
	 * Splits a string by a RegEx pattern.
	 * Returns an array.
	 * If the pattern is malformed, this returns an error type.
	 * ARG1: The string (converted). 
	 * ARG2: The RegEx pattern to split on.
	 */
	REGEXSPLIT(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			String regex = scriptInstance.popStackValue().asString();
			String str = scriptInstance.popStackValue().asString();
			
			Pattern p = null;
			try {
				p = PatternUtils.get(regex);
			} catch (PatternSyntaxException e) {
				scriptInstance.pushStackValue(ErrorType.create(e));
				return true;
			}
			if (p != null)
				scriptInstance.pushStackValue(Pattern.compile(regex).split(str));
			else
				scriptInstance.pushStackValue(null);
			return true;
		}
	},
	
	;
	
	private final boolean isVoid;
	private final int parameterCount;
	private RegexFunctions(int parameterCount)
	{
		this(false, parameterCount);
	}
	
	private RegexFunctions(boolean isVoid, int parameterCount)
	{
		this.isVoid = isVoid;
		this.parameterCount = parameterCount;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver getResolver()
	{
		return new EnumFunctionResolver(MathFunctions.values());
	}

	@Override
	public boolean isVoid()
	{
		return isVoid;
	}

	@Override
	public int getParameterCount()
	{
		return parameterCount;
	}

	@Override
	public Usage getUsage()
	{
		return null;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance);

}
