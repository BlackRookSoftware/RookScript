/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.struct.Utils;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

/**
 * Script common functions for mathematic functions.
 * @author Matthew Tropiano
 */
public enum MathFunctions implements ScriptFunctionType
{
	MIN(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the minimum of two values."
				)
				.parameter("value1", 
					type("The first value.")
				)
				.parameter("value2", 
					type("The second value.")
				)
				.returns(
					type("The value that is less than the other value.")
				)
			;
		}
		
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
		
	MAX(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the maximum of two values."
				)
				.parameter("value1", 
					type("The first value.")
				)
				.parameter("value2", 
					type("The second value.")
				)
				.returns(
					type("The value that is greater than the other value.")
				)
			;
		}
		
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
	
	POW(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Raises a number to another mathematical power."
				)
				.parameter("value", 
					type(Type.FLOAT, "The first value.")
				)
				.parameter("power", 
					type(Type.FLOAT, "The second value.")
				)
				.returns(
					type(Type.FLOAT, "The value raised to the provided power.")
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
	
	ROUND(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Rounds a number to the nearest whole number."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value to round.")
				)
				.returns(
					type(Type.INTEGER, "The value rounded to the nearest integer value.")
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
				returnValue.set(Math.round(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	FLOOR(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the mathematical floor of a number."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value.")
				)
				.returns(
					type(Type.FLOAT, "The floor of the provided value.")
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
				returnValue.set(Math.floor(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	CEILING(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the mathematical ceiling of a number."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value.")
				)
				.returns(
					type(Type.FLOAT, "The ceiling of the provided value.")
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
				returnValue.set(Math.ceil(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	FIX(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Rounds a number to the nearest arbitrary digit place. The \"place\" is a power of 10. FIX(n, 0) = ROUND(n)"
				)
				.parameter("value", 
					type(Type.FLOAT, "The value.")
				)
				.parameter("place", 
					type(Type.INTEGER, "The place.")
				)
				.returns(
					type(Type.FLOAT, "The rounded value.")
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
	
	E(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns Euler's constant."
				)
				.returns(
					type(Type.FLOAT, String.valueOf(Math.E))
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set(Math.E);
			return true;
		}
	},
	
	LOGE(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the Natural Log (base e) of a value."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value.")
				)
				.returns(
					type(Type.FLOAT, "Log (base e) of the provided value.")
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
				returnValue.set(Math.log(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	LOG10(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the Base 10 Log of a value."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value.")
				)
				.returns(
					type(Type.FLOAT, "Base 10 Log of the provided value.")
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
				returnValue.set(Math.log10(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	SQRT(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns the square root of a value."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value.")
				)
				.returns(
					type(Type.FLOAT, "The square root of the provided value.")
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
				returnValue.set(Math.sqrt(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	PI(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns Pi."
				)
				.returns(
					type(Type.FLOAT, String.valueOf(Math.PI))
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set(Math.PI);
			return true;
		}
	},
	
	DEG2RAD(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Converts degrees to radians."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value in degrees.")
				)
				.returns(
					type(Type.FLOAT, "The value in radians.")
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
				returnValue.set(Utils.degToRad(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	RAD2DEG(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Converts radians to degrees."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value in radians.")
				)
				.returns(
					type(Type.FLOAT, "The value in degrees.")
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
				returnValue.set(Utils.radToDeg(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	 
	SIN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Mathematical sine."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value in radians.")
				)
				.returns(
					type(Type.FLOAT, "SIN(value)")
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
				returnValue.set(Math.sin(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	COS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Mathematical cosine."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value in radians.")
				)
				.returns(
					type(Type.FLOAT, "COS(value)")
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
				returnValue.set(Math.cos(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	TAN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Mathematical tangent."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value in radians.")
				)
				.returns(
					type(Type.FLOAT, "TAN(value)")
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
				returnValue.set(Math.tan(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	ASIN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Mathematical arc sine."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value in radians.")
				)
				.returns(
					type(Type.FLOAT, "ASIN(value)")
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
				returnValue.set(Math.asin(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	ACOS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Mathematical arc cosine."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value in radians.")
				)
				.returns(
					type(Type.FLOAT, "ACOS(value)")
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
				returnValue.set(Math.acos(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	ATAN(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Mathematical arc tangent."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value in radians.")
				)
				.returns(
					type(Type.FLOAT, "ATAN(value)")
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
				returnValue.set(Math.atan(temp.asDouble()));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	CLAMP(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Clamps a value between two values."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value.")
				)
				.parameter("lo", 
					type(Type.FLOAT, "The lower bound.")
				)
				.parameter("hi", 
					type(Type.FLOAT, "The higher bound.")
				)
				.returns(
					type(Type.FLOAT, "The value clamped to the provided range.")
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
	
	WRAP(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Wraps a value around a value range. The higher bound is never returned."
				)
				.parameter("value", 
					type(Type.FLOAT, "The value.")
				)
				.parameter("lo", 
					type(Type.FLOAT, "The lower bound.")
				)
				.parameter("hi", 
					type(Type.FLOAT, "The higher bound.")
				)
				.returns(
					type(Type.FLOAT, "The value wrapped around the provided range.")
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
	
	LERP(3)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Linearly interpolates from the first value to the second."
				)
				.parameter("scalar", 
					type(Type.FLOAT, 
						"The unit scalar between the first and second value. 0.0 is first, 1.0 is the second."
					)
				)
				.parameter("value1", 
					type(Type.FLOAT, "The first value.")
				)
				.parameter("value2", 
					type(Type.FLOAT, "The second value.")
				)
				.returns(
					type(Type.FLOAT, 
						"A value scaled between the provided range. If the scalar is beyond the range of 0.0 to 1.0, " +
						"this will return a value outside the range."
					)
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
	
	PARSEINT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Parses a string to an integer with an optional radix."
				)
				.parameter("value", 
					type(Type.STRING, "The value to parse.")
				)
				.parameter("radix", 
					type(Type.NULL, "Radix 10."),
					type(Type.INTEGER, "An optional radix.")
				)
				.returns(
					type(Type.INTEGER, "The parsed value."),
					type(Type.FLOAT, "NaN, if not parsable.")
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
	
	COLOR(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Converts color (byte, 0 to 255) components to an ARGB integer."
				)
				.parameter("red", 
					type(Type.INTEGER, "Red component byte.")
				)
				.parameter("green", 
					type(Type.INTEGER, "Green component byte.")
				)
				.parameter("blue", 
					type(Type.INTEGER, "Blue component byte.")
				)
				.parameter("alpha", 
					type(Type.INTEGER, "Alpha component byte.")
				)
				.returns(
					type(Type.INTEGER, "The output ARGB color value.")
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
				byte alpha = temp.asByte();
				scriptInstance.popStackValue(temp);
				byte blue = temp.asByte();
				scriptInstance.popStackValue(temp);
				byte green = temp.asByte();
				scriptInstance.popStackValue(temp);
				byte red = temp.asByte();
				long argb = 
					  (alpha & 0x0ff) << 24 
					| (red & 0x0ff) << 16 
					| (green & 0x0ff) << 8 
					| (blue & 0x0ff);
				returnValue.set(argb);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	COLORF(4)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Converts color (float, 0.0 to 1.0) components to an ARGB integer."
				)
				.parameter("red", 
					type(Type.FLOAT, "Red component float.")
				)
				.parameter("green", 
					type(Type.FLOAT, "Green component float.")
				)
				.parameter("blue", 
					type(Type.FLOAT, "Blue component float.")
				)
				.parameter("alpha", 
					type(Type.FLOAT, "Alpha component float.")
				)
				.returns(
					type(Type.INTEGER, "The output ARGB color value.")
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
	private Usage usage;
	private MathFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
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
		if (usage == null)
			usage = usage();
		return usage;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
