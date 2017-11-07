/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
	 * Returns void.
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
	 * Returns void.
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
			else
				scriptInstance.pushStackValue(arg2);
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
			else
				scriptInstance.pushStackValue(arg1);
			return true;
		}
	},
	
	/**
	 * Raises a number to another mathematical power.
	 * Always returns a float.
	 * ARG1: The number.
	 * ARG2: The power.
	 */
	POW(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			double power = scriptInstance.popStackValue().asDouble();
			double value = scriptInstance.popStackValue().asDouble();
			scriptInstance.pushStackValue(Math.pow(value, power));
			return true;
		}
	},
	
	/**
	 * Rounds a number to the nearest whole number.
	 * Always returns an integer.
	 * ARG1: The number.
	 */
	ROUND(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			double value = scriptInstance.popStackValue().asDouble();
			scriptInstance.pushStackValue(Math.round(value));
			return true;
		}
	},
	
	/**
	 * Returns the mathematical floor of a number.
	 * Always returns a float.
	 * ARG1: The number.
	 */
	FLOOR(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			double value = scriptInstance.popStackValue().asDouble();
			scriptInstance.pushStackValue(Math.floor(value));
			return true;
		}
	},
	
	/**
	 * Returns the mathematical ceiling of a number.
	 * Always returns a float.
	 * ARG1: The number.
	 */
	CEILING(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			double value = scriptInstance.popStackValue().asDouble();
			scriptInstance.pushStackValue(Math.ceil(value));
			return true;
		}
	},
	
	/**
	 * Rounds a number to the nearest arbitrary place.
	 * The "place" is a power of 10. <code>FIX(n, 0) = ROUND(n)</code>
	 * Always returns a float.
	 * ARG1: The number.
	 * ARG2: The place.
	 */
	FIX(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			double place = scriptInstance.popStackValue().asDouble();
			double value = scriptInstance.popStackValue().asDouble();
			double p = Math.pow(10, -place);
			scriptInstance.pushStackValue(Math.round(value * p) / p);
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
	 * Returns the Natural Log (base e) of a value.
	 * ARG1: The value.
	 */
	LOGE(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(Math.log(arg.asDouble()));
			return true;
		}
	},
	
	/**
	 * Returns the Base 10 Log of a value.
	 * ARG1: The value.
	 */
	LOG10(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(Math.log10(arg.asDouble()));
			return true;
		}
	},
	
	/**
	 * Returns the square root of a number.
	 * Always returns a float.
	 * ARG1: The number.
	 */
	SQRT(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			double value = scriptInstance.popStackValue().asDouble();
			scriptInstance.pushStackValue(Math.sqrt(value));
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
	 * Tangent.
	 * ARG: Number in Radians
	 */
	TAN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(Math.tan(arg1.asDouble()));
			return true;
		}
	},
	
	/**
	 * Arc Sine.
	 * ARG: Number in Radians
	 */
	ASIN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(Math.asin(arg1.asDouble()));
			return true;
		}
	},
	
	/**
	 * Arc Cosine.
	 * ARG: Number in Radians
	 */
	ACOS(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(Math.acos(arg1.asDouble()));
			return true;
		}
	},
	
	/**
	 * Arc Tangent.
	 * ARG: Number in Radians
	 */
	ATAN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue arg1 = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(Math.atan(arg1.asDouble()));
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
	 * Returns false if either index out of bounds, or end index is less than the start index.
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
			else if (endIndex < 0 && endIndex > length)
				scriptInstance.pushStackValue(false);
			else if (endIndex < startIndex)
				scriptInstance.pushStackValue(false);
			else
				scriptInstance.pushStackValue(str.substring(startIndex, endIndex));
			return true;
		}
	},
	
	/**
	 * Returns the starting index of a string inside another string.
	 * If not found, this returns -1.
	 * ARG1: The string (converted). 
	 * ARG2: The string to search for (converted). 
	 */
	STRINDEX(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			String targetStr = scriptInstance.popStackValue().asString();
			String str = scriptInstance.popStackValue().asString();
			scriptInstance.pushStackValue(str.indexOf(targetStr));
			return true;
		}
	},
	
	/**
	 * Returns the starting index of a string inside another string, searching from the end.
	 * If not found, this returns -1.
	 * ARG1: The string (converted). 
	 * ARG2: The string to search for (converted). 
	 */
	STRLASTINDEX(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			String targetStr = scriptInstance.popStackValue().asString();
			String str = scriptInstance.popStackValue().asString();
			scriptInstance.pushStackValue(str.lastIndexOf(targetStr));
			return true;
		}
	},
	
	/**
	 * Splits a string by a RegEx pattern.
	 * Returns an array.
	 * If the pattern is malformed, this returns false.
	 * ARG1: The string (converted). 
	 * ARG2: The RegEx pattern to split on.
	 */
	STRSPLIT(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			String regex = scriptInstance.popStackValue().asString();
			String str = scriptInstance.popStackValue().asString();
			
			Pattern p = null;
			try {
				p = Pattern.compile(regex);
			} catch (PatternSyntaxException e) {
				// bad pattern.
			}
			if (p != null)
				scriptInstance.pushStackValue(Pattern.compile(regex).split(str));
			else
				scriptInstance.pushStackValue(false);
			return true;
		}
	},
	
	/**
	 * Creates a new list.
	 * Copies an existing list, or encapsulates a value as a list. 
	 * ARG1: The value to copy (and re-encapsulate in a list). 
	 */
	LISTNEW(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			if (value.isList())
				scriptInstance.pushStackValue(value.copy());
			else
				scriptInstance.pushStackValue(new Object[]{value.copy()});
			return true;
		}
	}, 
	
	/**
	 * Adds a value to a list. 
	 * If the "list" argument is not a list or not added, this returns false, else true.
	 * ARG1: The list to add the item to. 
	 * ARG2: The item to add.
	 */
	LISTADD(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.add(item));
			return true;
		}
	},
	
	/**
	 * Adds a value to a list. 
	 * If the "list" argument is not a list or not added, this returns false, else true.
	 * ARG1: The list to add the item to. 
	 * ARG2: The item to add.
	 * ARG3: The index to add it to.
	 */
	LISTADDAT(3)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			int index = scriptInstance.popStackValue().asInt();
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.addAt(index, item));
			return true;
		}
	},
	
	/**
	 * Removes a value from a list that matches the item. 
	 * Only removes list-typed items by reference.
	 * If the "list" argument is not a list or the provided item is not removed, this returns false, else true.
	 * ARG1: The list to remove the item from. 
	 * ARG2: The item to remove.
	 */
	LISTREMOVE(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.remove(item));
			return true;
		}
	},
	
	/**
	 * Removes a value from a list that matches the item. 
	 * Only removes list-typed items by reference.
	 * Returns the removed item, or false if no item removed due to a bad index.
	 * ARG1: The list to remove the item from. 
	 * ARG2: The index to remove.
	 */
	LISTREMOVEAT(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			int index = scriptInstance.popStackValue().asInt();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.removeAt(index));
			return true;
		}
	},
	
	/**
	 * Sorts a list in-place.
	 * Returns the list that was sorted (NOT a new copy!).
	 * If this list contains discretely different elements,  
	 * ARG1: The list to remove the item from. 
	 */
	LISTSORT(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue list = scriptInstance.popStackValue();
			list.sort();
			scriptInstance.pushStackValue(list);
			return true;
		}
	},
	
	/**
	 * Checks if a list contains a specific value.
	 * Sequential search.
	 * Return true if it contains the value, false if not.
	 * ARG1: The list to look in. 
	 * ARG2: The item to look for.
	 */
	LISTCONTAINS(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.contains(item));
			return true;
		}
	},
	
	/**
	 * Gets the index of a value in the list. 
	 * Only finds list-typed items by reference.
	 * If not found or not a list, this returns -1.
	 * ARG1: The list to look in. 
	 * ARG2: The item to look for.
	 */
	LISTINDEX(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue item = scriptInstance.popStackValue();
			ScriptValue list = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(list.getIndexOf(item));
			return true;
		}
	},
	
	/**
	 * Creates a new set of items from a list, such that 
	 * the contents of the list are discrete and sorted.
	 * The object returned is a list, but its contents are now suitable for set operations.
	 * ARG1: The list or value.
	 */
	SET(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue out = ScriptValue.createEmptyList();
			if (value.isList())
			{
				for (int i = 0; i < value.size(); i++)
					out.setAdd(value.getByIndex(i));
			}
			else
			{
				out.setAdd(value);
			}
			
			scriptInstance.pushStackValue(out);
			return true;
		}
	},
	
	/**
	 * Adds a value to a list, expected to be set up like a set.
	 * Returns true if value was added (and is not already in the list), false otherwise.
	 * ARG1: The set (list).
	 * ARG2: The value to add.
	 */
	SETADD(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue set = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(set.setAdd(value));
			return true;
		}
	},
	
	/**
	 * Removes a value from a list, expected to be set up like a set.
	 * Returns true if value was removed, false otherwise.
	 * ARG1: The set (list).
	 * ARG2: The value to remove.
	 */
	SETREMOVE(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue set = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(set.setRemove(value));
			return true;
		}
	},
	
	/**
	 * Checks if a value exists in a list, expected to be set up like a set.
	 * This is more performant than a list - search is binary search.
	 * Returns true if value was removed, false otherwise.
	 * ARG1: The set (list).
	 * ARG2: The value to look for.
	 */
	SETCONTAINS(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue set = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(set.setContains(value));
			return true;
		}
	},
	
	/**
	 * Gets the index of a value in a list, expected to be set up like a set.
	 * This is more performant than a list - search is binary search.
	 * If not found or not a list, this returns -1.
	 * ARG1: The set (list).
	 * ARG2: The value to look for.
	 */
	SETSEARCH(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue set = scriptInstance.popStackValue();
			scriptInstance.pushStackValue(set.setSearch(value));
			return true;
		}
	},
	
	/**
	 * Gets the union of two sets, returning a new set with values in both. 
	 * Returns a new set.
	 * ARG1: The first set (list).
	 * ARG2: The second set (list).
	 */
	SETUNION(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue set2 = scriptInstance.popStackValue();
			ScriptValue set1 = scriptInstance.popStackValue();
			ScriptValue out = ScriptValue.createEmptyList();
			
			ScriptValue union1;
			ScriptValue union2;
			
			if (set1.isList())
				union1 = set1;
			else
				union1 = wrapList(set1);
			
			if (set2.isList())
				union2 = set2;
			else
				union2 = wrapList(set2);

			for (int i = 0; i < union1.size(); i++)
				out.setAdd(union1.getByIndex(i));
			for (int i = 0; i < union2.size(); i++)
				out.setAdd(union2.getByIndex(i));
			
			scriptInstance.pushStackValue(out);
			return true;
		}
	},
	
	/**
	 * Gets the intersection of two sets, returning a new set with values in both. 
	 * Returns a new set.
	 * ARG1: The first set (list).
	 * ARG2: The second set (list).
	 */
	SETINTERSECT(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue set2 = scriptInstance.popStackValue();
			ScriptValue set1 = scriptInstance.popStackValue();
			ScriptValue out = ScriptValue.createEmptyList();
			
			ScriptValue intersect1;
			ScriptValue intersect2;
			
			if (set1.isList())
				intersect1 = set1;
			else
				intersect1 = wrapList(set1);
			
			if (set2.isList())
				intersect2 = set2;
			else
				intersect2 = wrapList(set2);

			ScriptValue smallest = intersect1.size() < intersect2.size() ? intersect1 : intersect2;
			ScriptValue largest = smallest == intersect1 ? intersect2 : intersect1;
			
			for (int i = 0; i < smallest.size(); i++)
			{
				ScriptValue sv = smallest.getByIndex(i);
				if (largest.setContains(sv))
					out.setAdd(sv);
			}
			
			scriptInstance.pushStackValue(out);
			return true;
		}
	},
	
	/**
	 * Gets the xor of two sets, returning the union of both sets minus the intersection. 
	 * Returns a new set.
	 * ARG1: The first set (list).
	 * ARG2: The second set (list).
	 */
	SETXOR(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue set2 = scriptInstance.popStackValue();
			ScriptValue set1 = scriptInstance.popStackValue();

			ScriptValue xor1;
			ScriptValue xor2;
			
			if (set1.isList())
				xor1 = set1;
			else
				xor1 = wrapList(set1);
			
			if (set2.isList())
				xor2 = set2;
			else
				xor2 = wrapList(set2);

			ScriptValue out = xor1.copy();
			for (int i = 0; i < xor2.size(); i++)
			{
				ScriptValue sv = xor2.getByIndex(i);
				if (out.setContains(sv))
					out.setRemove(sv);
				else
					out.setAdd(sv);
			}
			
			scriptInstance.pushStackValue(out);
			return true;
		}
	},
	
	/**
	 * Gets a new set that is the first set minus the values in the second set. 
	 * Returns a new set.
	 * ARG1: The first set (list).
	 * ARG2: The second set (list).
	 */
	SETDIFF(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue set2 = scriptInstance.popStackValue();
			ScriptValue set1 = scriptInstance.popStackValue();

			ScriptValue diff1;
			ScriptValue diff2;
			
			if (set1.isList())
				diff1 = set1;
			else
				diff1 = wrapList(set1);
			
			if (set2.isList())
				diff2 = set2;
			else
				diff2 = wrapList(set2);

			ScriptValue out = diff1.copy();
			for (int i = 0; i < diff2.size(); i++)
				out.setRemove(diff2.getByIndex(i));
			
			scriptInstance.pushStackValue(out);
			return true;
		}
	},
	
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

	// wraps a single value into a list.
	protected ScriptValue wrapList(ScriptValue sv)
	{
		ScriptValue out = ScriptValue.createEmptyList();
		out.add(sv);
		return out;
	}
	
}
