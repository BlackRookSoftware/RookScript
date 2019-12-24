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
			scriptInstance.pushStackValue(Utils.degToRad(arg.asDouble()));
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
			scriptInstance.pushStackValue(Utils.radToDeg(arg.asDouble()));
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
			scriptInstance.pushStackValue(Utils.clampValue(arg1.asDouble(), arg2.asDouble(), arg3.asDouble()));
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
			scriptInstance.pushStackValue(Utils.wrapValue(arg1.asDouble(), arg2.asDouble(), arg3.asDouble()));
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
			scriptInstance.pushStackValue(Utils.linearInterpolate(arg1.asDouble(), arg2.asDouble(), arg3.asDouble()));
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
				scriptInstance.pushStackValue(Integer.parseInt(arg1.asString(), arg2.isNull() ? 10 : arg2.asInt()));
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

	;
	
	private final int parameterCount;
	private MathFunctions(int parameterCount)
	{
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
		out.listAdd(sv);
		return out;
	}
	
}
