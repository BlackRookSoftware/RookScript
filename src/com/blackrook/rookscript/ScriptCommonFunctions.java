/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import com.blackrook.commons.math.RMath;
import com.blackrook.rookscript.resolver.EnumResolver;
import com.blackrook.rookscript.struct.ScriptValue;

/**
 * Script common functions that work for all scripts.
 * @author Matthew Tropiano
 */
public enum ScriptCommonFunctions implements ScriptFunctionType
{
	/**
	 * Prints something to STDOUT.
	 * ARG: Value to print.
	 */
	OUT(true, 1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			System.out.println(arg.asString());
			return true;
		}
	},

	/**
	 * Prints something to STDERR.
	 * ARG: Value to print.
	 */
	ERR(true, 1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			System.err.println(arg.asString());
			return true;
		}
	},

	/**
	 * Minimum.
	 * ARG1: First number.
	 * ARG2: Second number.
	 */
	MIN(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg2 = scriptInstance.popStackValue();
			ScriptValue arg1 = scriptInstance.popStackValue();
			if (arg1.asDouble() < arg2.asDouble())
				scriptInstance.pushStackValue(arg1);
			return true;
		}
	},
		
	/**
	 * Maximum.
	 * ARG1: First number.
	 * ARG2: Second number.
	 */
	MAX(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg2 = scriptInstance.popStackValue();
			ScriptValue arg1 = scriptInstance.popStackValue();
			if (arg1.asDouble() < arg2.asDouble())
				scriptInstance.pushStackValue(arg2);
			return true;
		}
	},
	
	/**
	 * Sine.
	 * ARG: Number in Radians
	 */
	SIN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(Math.sin(arg1.asDouble()));
			return true;
		}
	},
	
	/**
	 * Cosine.
	 * ARG: Number in Radians
	 */
	COS(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(Math.cos(arg1.asDouble()));
			return true;
		}
	},
	
	/**
	 * Clamp.
	 * ARG1: Value.
	 * ARG2: Low bound.
	 * ARG3: High bound.
	 */
	CLAMP(3)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg3 = scriptInstance.popStackValue();
			ScriptValue arg2 = scriptInstance.popStackValue();
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(RMath.clampValue(arg1.asDouble(), arg2.asDouble(), arg3.asDouble()));
			return true;
		}
	},
	
	/**
	 * Wrap.
	 * ARG1: Value.
	 * ARG2: Low bound.
	 * ARG3: High bound.
	 */
	WRAP(3)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg3 = scriptInstance.popStackValue();
			ScriptValue arg2 = scriptInstance.popStackValue();
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(RMath.wrapValue(arg1.asDouble(), arg2.asDouble(), arg3.asDouble()));
			return true;
		}
	},
	
	/**
	 * Linear-interpolate.
	 * ARG1: Scalar.
	 * ARG2: First value.
	 * ARG3: Second value.
	 */
	LERP(3)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg3 = scriptInstance.popStackValue();
			ScriptValue arg2 = scriptInstance.popStackValue();
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(RMath.linearInterpolate(arg1.asDouble(), arg2.asDouble(), arg3.asDouble()));
			return true;
		}
	},
	
	/**
	 * Convert to boolean.
	 * ARG: Value
	 */
	TOBOOLEAN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(arg1.asBoolean());
			return true;
		}
	},
	
	/**
	 * Convert to integer (long, internally).
	 * ARG: Value
	 */
	TOINT(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(arg1.asLong());
			return true;
		}
	},
	
	/**
	 * Convert to floating point (double, internally).
	 * ARG: Value
	 */
	TOFLOAT(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(arg1.asDouble());
			return true;
		}
	},
	
	/**
	 * Convert to string (double, internally).
	 * ARG: Value
	 */
	TOSTRING(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(arg1.asString());
			return true;
		}
	},
	
	/**
	 * Parses a string to an integer with an optional radix.
	 * ARG1: String value.
	 * ARG2: Radix.
	 */
	PARSEINT(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg2 = scriptInstance.popStackValue();
			ScriptValue arg1 = scriptInstance.popStackValue();
			try {
				scriptInstance.pushStackValue(Integer.parseInt(arg1.asString(), arg2.isFalse() ? 10 : arg2.asInt()));
				return true;
			} catch (NumberFormatException e) {
				scriptInstance.pushStackValue(Double.NaN);
				return true;
			}
		}
	},
	
	/**
	 * Color (byte) components to ARGB.
	 * ARG1: Red byte.
	 * ARG2: Green byte.
	 * ARG3: Blue byte.
	 * ARG4: Alpha byte.
	 */
	COLOR(4)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue alpha = scriptInstance.popStackValue();
			ScriptValue blue = scriptInstance.popStackValue();
			ScriptValue green = scriptInstance.popStackValue();
			ScriptValue red = scriptInstance.popStackValue();
			long argb = alpha.asByte() << 24 
					| red.asByte() << 16 
					| green.asByte() << 8 
					| blue.asByte();
			scriptInstance.pushStackValue(argb);
			return true;
		}
	},
	
	/**
	 * Color (float) components to ARGB.
	 * ARG1: Red component.
	 * ARG2: Green component.
	 * ARG3: Blue component.
	 * ARG4: Alpha component.
	 */
	COLORF(4)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue alpha = scriptInstance.popStackValue();
			ScriptValue blue = scriptInstance.popStackValue();
			ScriptValue green = scriptInstance.popStackValue();
			ScriptValue red = scriptInstance.popStackValue();
			long argb = ((int)(alpha.asDouble() * 255.0) & 0x0ff) << 24 
					| ((int)(red.asDouble() * 255.0) & 0x0ff) << 16 
					| ((int)(green.asDouble() * 255.0) & 0x0ff) << 8 
					| ((int)(blue.asDouble() * 255.0) & 0x0ff);
			scriptInstance.pushStackValue(argb);
			return true;
		}
	},
	
	/**
	 * Degrees to radians.
	 * ARG1: Value in degrees.
	 */
	DEG2RAD(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(RMath.degToRad(arg.asDouble()));
			return true;
		}
	},
	
	/**
	 * Radians to degrees.
	 * ARG1: Value in radians.
	 */
	RAD2DEG(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(RMath.radToDeg(arg.asDouble()));
			return true;
		}
	},
	
	/**
	 * Returns PI.
	 */
	PI(0)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(Math.PI);
			return true;
		}
	},
	
	/**
	 * Returns Euler's constant.
	 */
	E(0)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(Math.E);
			return true;
		}
	},
	
	/**
	 * Returns the "length" of a value.
	 * ARG1: The value. 
	 * 
	 * Strings: string length.
	 * Lists: list length.
	 * Others: 1
	 */
	LENGTH(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			if (value.isString())
				scriptInstance.pushStackValue(value.asString().length());
			else
				scriptInstance.pushStackValue(value.size());
			return true;
		}
	},

	/**
	 * Returns a string in full uppercase.
	 * ARG1: The value (converted to string, first). 
	 */
	STRUPPER(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(scriptInstance.popStackValue().asString().toUpperCase());
			return true;
		}
	},
	
	/**
	 * Returns a string in full lowercase.
	 * ARG1: The value (converted to string, first). 
	 */
	STRLOWER(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(scriptInstance.popStackValue().asString().toLowerCase());
			return true;
		}
	},
	
	/**
	 * Returns a string trimmed of whitespace at both ends.
	 * ARG1: The value (converted to string, first). 
	 */
	STRTRIM(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(scriptInstance.popStackValue().asString().trim());
			return true;
		}
	},
	
	/**
	 * Returns a single character from a string.
	 * If ARG2 is out of bounds, this returns false.
	 * ARG1: The string value (may be converted). 
	 * ARG2: The string index. 
	 */
	STRCHAR(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			int value = scriptInstance.popStackValue().asInt();
			String str = scriptInstance.popStackValue().asString();
			if (value < 0 || value >= str.length())
				scriptInstance.pushStackValue(false);
			else
				scriptInstance.pushStackValue(String.valueOf(str.charAt(value)));
			return true;
		}
	},
	
	/**
	 * Returns a substring of another string.
	 * Returns false if either index out of bounds.
	 * If end index is 0 or less, it is index from the end.
	 * ARG1: The string (converted). 
	 * ARG2: The starting index (inclusive). 
	 * ARG3: The ending index (exclusive). 
	 */
	SUBSTR(3)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			int endIndex = scriptInstance.popStackValue().asInt();
			int startIndex = scriptInstance.popStackValue().asInt();
			String str = scriptInstance.popStackValue().asString();
			int length = str.length();
			if (startIndex < 0 || startIndex >= length)
				scriptInstance.pushStackValue(false);
			else if (endIndex <= 0 && -endIndex > length)
				scriptInstance.pushStackValue(false);
			else if (endIndex >= length)
				scriptInstance.pushStackValue(false);
			else
				scriptInstance.pushStackValue(str.substring(startIndex, endIndex <= 0 ? length - -endIndex : endIndex));
			return true;
		}
	},
	
	/*
	 * STRINDEXOF
	 * STRSPLIT
	 * LISTADD
	 * LISTADDAT
	 * LISTREMOVE
	 * LISTSORT
	 * LISTCOPY
	 * LISTSHUFFLE
	 * LISTINDEX
	 * LISTTOSET
	 * SETNEW
	 * SETADD
	 * SETREMOVE
	 * SETCONTAINS
	 * SETSEARCH
	 * SETUNION
	 * SETINTERSECT
	 * SETXOR
	 * SETDIFF
	 * TODO: Finish these.
	 */

	;
	
	
	private final boolean isVoid;
	private final int parameterCount;
	private ScriptCommonFunctions(int parameterCount)
	{
		this(false, parameterCount);
	}
	
	private ScriptCommonFunctions(boolean isVoid, int parameterCount)
	{
		this.isVoid = isVoid;
		this.parameterCount = parameterCount;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver getResolver()
	{
		return new EnumResolver(ScriptCommonFunctions.values());
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
