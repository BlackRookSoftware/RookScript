/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.ScriptValue.ErrorType;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.struct.PatternUtils;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

/**
 * A set of RegEx functions.
 * @author Matthew Tropiano
 */
public enum RegexFunctions implements ScriptFunctionType
{
	
	ISREGEX(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a string is a valid RegEx pattern."
				)
				.parameter("pattern", 
					type(Type.STRING, "The string to test.")
				)
				.returns(
					type(Type.BOOLEAN, "True if the pattern is malformed, false otherwise.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				String regex = temp.asString();
				
				try {
					PatternUtils.get(regex);
				} catch (PatternSyntaxException e) {
					returnValue.set(false);
					return true;
				}
				
				returnValue.set(true);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	REGEXSPLIT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Splits a string by a RegEx pattern."
				)
				.parameter("pattern", 
					type(Type.STRING, "The RegEx pattern to use.")
				)
				.parameter("string", 
					type(Type.STRING, "The string to split.")
				)
				.returns(
					type(Type.LIST, "A list of strings."), 
					type(Type.ERROR, "If the pattern is malformed.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				scriptInstance.popStackValue(temp);
				String regex = temp.asString();

				Pattern p = null;
				try {
					p = PatternUtils.get(regex);
				} catch (PatternSyntaxException e) {
					returnValue.set(ErrorType.create(e));
					return true;
				}
				
				returnValue.set(p.split(str));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	REGEXCONTAINS(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a string contains a set of characters that matches the provided RegEx."
				)
				.parameter("pattern", 
					type(Type.STRING, "The RegEx pattern to use.")
				)
				.parameter("string", 
					type(Type.STRING, "The string to inspect.")
				)
				.returns(
					type(Type.BOOLEAN, "True if so, false if not."), 
					type(Type.ERROR, "If the pattern is malformed.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				scriptInstance.popStackValue(temp);
				String regex = temp.asString();

				Pattern p = null;
				try {
					p = PatternUtils.get(regex);
				} catch (PatternSyntaxException e) {
					returnValue.set(ErrorType.create(e));
					return true;
				}
				
				returnValue.set(p.matcher(str).find());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	REGEXMATCHES(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Checks if a string completely matches a set of characters using the provided RegEx."
				)
				.parameter("pattern", 
					type(Type.STRING, "The RegEx pattern to use.")
				)
				.parameter("string", 
					type(Type.STRING, "The string to inspect.")
				)
				.returns(
					type(Type.BOOLEAN, "True if so, false if not."), 
					type(Type.ERROR, "If the pattern is malformed.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				String str = temp.asString();
				scriptInstance.popStackValue(temp);
				String regex = temp.asString();

				Pattern p = null;
				try {
					p = PatternUtils.get(regex);
				} catch (PatternSyntaxException e) {
					returnValue.set(ErrorType.create(e));
					return true;
				}
				
				returnValue.set(p.matcher(str).matches());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	// TODO: Add more functions.
	
	;
	
	private final int parameterCount;
	private Usage usage;
	private RegexFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(RegexFunctions.values());
	}

	@Override
	public int getParameterCount()
	{
		return parameterCount;
	}

	@Override
	public Usage getUsage()
	{
		if (usage == null)
			usage = usage();
		return usage;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
