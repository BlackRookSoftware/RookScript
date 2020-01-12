/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.struct.Utils;

/**
 * Script common functions for mathematic functions.
 * @author Matthew Tropiano
 */
public enum MathFunctions implements ScriptFunctionType
{
	/**
	 * Minimum.
	 * ARG1: First number.
	 * ARG2: Second number.
	 */
	MIN(2)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			ScriptValue arg2 = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(arg2);
				scriptInstance.popStackValue(arg1);
				if (arg1.compareTo(arg2) < 0)
					returnValue.set(arg1);
				else
					returnValue.set(arg2);
				return true;
			}
			finally
			{
				arg1.setNull();
				arg2.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue arg1 = CACHEVALUE1.get();
			ScriptValue arg2 = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(arg2);
				scriptInstance.popStackValue(arg1);
				if (arg1.compareTo(arg2) < 0)
					returnValue.set(arg2);
				else
					returnValue.set(arg1);
				return true;
			}
			finally
			{
				arg1.setNull();
				arg2.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				double power = temp.asDouble();
				scriptInstance.popStackValue(temp);
				double value = temp.asDouble();
				returnValue.set(Math.pow(value, power));
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.round(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.floor(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.ceil(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				double place = temp.asDouble();
				scriptInstance.popStackValue(temp);
				double value = temp.asDouble();
				double p = Math.pow(10, -place);
				returnValue.set(Math.round(value * p) / p);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Returns Euler's constant.
	 */
	E(0)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set(Math.E);
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.log(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Returns the Base 10 Log of a value.
	 * ARG1: The value.
	 */
	LOG10(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.log10(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.sqrt(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Returns PI.
	 */
	PI(0)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set(Math.PI);
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Utils.degToRad(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	/**
	 * Radians to degrees.
	 * ARG1: Value in radians.
	 */
	RAD2DEG(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Utils.radToDeg(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	 
	/**
	 * Sine.
	 * ARG: Number in Radians
	 */
	SIN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.sin(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Cosine.
	 * ARG: Number in Radians
	 */
	COS(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.cos(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Tangent.
	 * ARG: Number in Radians
	 */
	TAN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.tan(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Arc Sine.
	 * ARG: Number in Radians
	 */
	ASIN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.asin(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Arc Cosine.
	 * ARG: Number in Radians
	 */
	ACOS(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.acos(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Arc Tangent.
	 * ARG: Number in Radians
	 */
	ATAN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(Math.atan(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				double arg3 = temp.asDouble();
				scriptInstance.popStackValue(temp);
				double arg2 = temp.asDouble();
				scriptInstance.popStackValue(temp);
				double arg1 = temp.asDouble();
				returnValue.set(Utils.clampValue(arg1, arg2, arg3));
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				double arg3 = temp.asDouble();
				scriptInstance.popStackValue(temp);
				double arg2 = temp.asDouble();
				scriptInstance.popStackValue(temp);
				double arg1 = temp.asDouble();
				returnValue.set(Utils.wrapValue(arg1, arg2, arg3));
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				double arg3 = temp.asDouble();
				scriptInstance.popStackValue(temp);
				double arg2 = temp.asDouble();
				scriptInstance.popStackValue(temp);
				double arg1 = temp.asDouble();
				returnValue.set(Utils.linearInterpolate(arg1, arg2, arg3));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Convert to boolean.
	 * ARG: Value
	 */
	TOBOOLEAN(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(temp.asBoolean());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Convert to integer (long, internally).
	 * ARG: Value
	 */
	TOINT(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(temp.asLong());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Convert to floating point (double, internally).
	 * ARG: Value
	 */
	TOFLOAT(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(temp.asDouble());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Convert to string (double, internally).
	 * ARG: Value
	 */
	TOSTRING(1)
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				returnValue.set(temp.asString());
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				int radix = temp.isNull() ? 10 : temp.asInt();
				scriptInstance.popStackValue(temp);
				String value = temp.asString();
				try {
					returnValue.set(Integer.parseInt(value, radix));
					return true;
				} catch (NumberFormatException e) {
					returnValue.set(Double.NaN);
					return true;
				}
			}
			finally
			{
				temp.setNull();
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				byte alpha = temp.asByte();
				scriptInstance.popStackValue(temp);
				byte blue = temp.asByte();
				scriptInstance.popStackValue(temp);
				byte green = temp.asByte();
				scriptInstance.popStackValue(temp);
				byte red = temp.asByte();
				long argb = alpha << 24 | red << 16 | green << 8 | blue;
				returnValue.set(argb);
				return true;
			}
			finally
			{
				temp.setNull();
			}
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				double alpha = temp.asByte();
				scriptInstance.popStackValue(temp);
				double blue = temp.asByte();
				scriptInstance.popStackValue(temp);
				double green = temp.asByte();
				scriptInstance.popStackValue(temp);
				double red = temp.asByte();
				long argb = 
					((int)(alpha * 255.0) & 0x0ff) << 24 
					| ((int)(red * 255.0) & 0x0ff) << 16 
					| ((int)(green * 255.0) & 0x0ff) << 8 
					| ((int)(blue * 255.0) & 0x0ff);
				returnValue.set(argb);
				return true;
			}
			finally
			{
				temp.setNull();
			}

		}
	},

	;
	
	private final int parameterCount;
	private MathFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(MathFunctions.values());
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
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
