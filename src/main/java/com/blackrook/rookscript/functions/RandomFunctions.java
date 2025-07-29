/*******************************************************************************
 * Copyright (c) 2017-2025 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.util.Random;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.BufferType;
import com.blackrook.rookscript.ScriptValue.ListType;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;

/**
 * RookScript Random functions.
 * @author Matthew Tropiano
 * @since 1.16.0
 */
public enum RandomFunctions implements ScriptFunctionType
{
	RANDOM(0)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new random number generator object. Algorithm is implementation-provided."
				)
				.returns(
					type(Type.OBJECTREF, "Random", "The new random number generator.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			returnValue.set(new Random());
			return true;
		}
	},

	RANDOMSEED(1)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a new random number generator object with a defined random seed. Algorithm is implementation-provided."
				)
				.parameter("seed",
					type(Type.INTEGER, "The seeding number.")
				)
				.returns(
					type(Type.OBJECTREF, "Random", "The new random number generator.")
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
				long seed = temp.asLong();
				
				returnValue.set(new Random(seed));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	RANDOMINT(2)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Generates a random number from 0 (inclusive) to an integer value (exclusive)."
				)
				.parameter("random",
					type(Type.OBJECTREF, "Random", "The random number generator.")
				)
				.parameter("number",
					type(Type.INTEGER, "The upper bound (32-bit).")
				)
				.returns(
					type(Type.INTEGER, "A random integer from 0 to [number] - 1."),
					type(Type.ERROR, "BadRandom", "If a random number generator was not provided.")
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
				int number = temp.asInt();
				scriptInstance.popStackValue(temp);
				
				if (!temp.isObjectRef(Random.class))
				{
					returnValue.setError("BadRandom", "First parameter is not a Random.");
					return true;
				}
				
				Random random = temp.asObjectType(Random.class);
				returnValue.set(random.nextInt(number));
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	RANDOMRANGE(3)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Generates a random number from [min] to [max] (inclusive)."
				)
				.parameter("random",
					type(Type.OBJECTREF, "Random", "The random number generator.")
				)
				.parameter("min",
					type(Type.INTEGER, "The lower bound.")
				)
				.parameter("max",
					type(Type.INTEGER, "The upper bound.")
				)
				.returns(
					type(Type.INTEGER, "A random integer from [min, max]."),
					type(Type.ERROR, "BadRandom", "If a random number generator was not provided.")
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
				long max = temp.asLong();
				scriptInstance.popStackValue(temp);
				long min = temp.asLong();
				scriptInstance.popStackValue(temp);
				
				if (!temp.isObjectRef(Random.class))
				{
					returnValue.setError("BadRandom", "First parameter is not a Random.");
					return true;
				}
				
				Random random = temp.asObjectType(Random.class);
				double value = random.nextDouble();
				
				returnValue.set((long)((max - min + 1) * value) + min);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	RANDOMFLOAT(1)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Generates a random number from 0.0 (inclusive) to 1.0 (exclusive)."
				)
				.parameter("random",
					type(Type.OBJECTREF, "Random", "The random number generator.")
				)
				.returns(
					type(Type.FLOAT, "A random float from 0.0 to 1.0 (exclusive)."),
					type(Type.ERROR, "BadRandom", "If a random number generator was not provided.")
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
				
				if (!temp.isObjectRef(Random.class))
				{
					returnValue.setError("BadRandom", "First parameter is not a Random.");
					return true;
				}
				
				Random random = temp.asObjectType(Random.class);
				returnValue.set(random.nextDouble());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	RANDOMGAUSS(1)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Generates a random Gaussian-distributed number with mean 0.0 and standard deviation 1.0."
				)
				.parameter("random",
					type(Type.OBJECTREF, "Random", "The random number generator.")
				)
				.returns(
					type(Type.FLOAT, "A random float, such that the mean is around 0.0, but the range is infinite."),
					type(Type.ERROR, "BadRandom", "If a random number generator was not provided.")
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
				
				if (!temp.isObjectRef(Random.class))
				{
					returnValue.setError("BadRandom", "First parameter is not a Random.");
					return true;
				}
				
				Random random = temp.asObjectType(Random.class);
				returnValue.set(random.nextGaussian());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	RANDOMSTRING(3)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Generates a random string of characters."
				)
				.parameter("random",
					type(Type.OBJECTREF, "Random", "The random number generator.")
				)
				.parameter("length",
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "The amount of characters to generate.")
				)
				.parameter("alphabet",
					type(Type.NULL, "Use \"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\"."),
					type(Type.STRING, "The alphabet of characters to use to generate the string (with equal chance).")
				)
				.returns(
					type(Type.STRING, "The resultant string."),
					type(Type.ERROR, "BadRandom", "If a random number generator was not provided.")
				)
			;
		}
		
		private static final String DEFAULT_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				String alphabet = temp.isNull() ? DEFAULT_ALPHABET : temp.asString();
				scriptInstance.popStackValue(temp);
				int length = temp.asInt();
				scriptInstance.popStackValue(temp);
				
				if (!temp.isObjectRef(Random.class))
				{
					returnValue.setError("BadRandom", "First parameter is not a Random.");
					return true;
				}
				
				Random random = temp.asObjectType(Random.class);

				StringBuilder sb = new StringBuilder(length);
				while (length-- > 0)
					sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
				
				returnValue.set(sb.toString());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	RANDOMBYTES(2)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Generates a random buffer of bytes (native byte order)."
				)
				.parameter("random",
					type(Type.OBJECTREF, "Random", "The random number generator.")
				)
				.parameter("length",
					type(Type.NULL, "Use 0."),
					type(Type.INTEGER, "The amount of bytes to generate.")
				)
				.returns(
					type(Type.BUFFER, "The resultant buffer."),
					type(Type.ERROR, "BadRandom", "If a random number generator was not provided.")
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
				int length = temp.asInt();
				scriptInstance.popStackValue(temp);
				
				if (!temp.isObjectRef(Random.class))
				{
					returnValue.setError("BadRandom", "First parameter is not a Random.");
					return true;
				}
				
				Random random = temp.asObjectType(Random.class);
				returnValue.setEmptyBuffer(length);
				BufferType buf = returnValue.asObjectType(BufferType.class);
				
				int i = 0;
				while (length-- > 0)
					buf.putByte(i++, (byte)(random.nextInt(256) - 128));
				
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	RANDOMELEMENT(2)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Selects and returns a random element in a provided list."
				)
				.parameter("random",
					type(Type.OBJECTREF, "Random", "The random number generator.")
				)
				.parameter("list",
					type(Type.LIST, "The list to select an item from.")
				)
				.returns(
					type(Type.NULL, "If the list is empty."),
					type("The chosen element in the list."),
					type(Type.ERROR, "BadList", "If a list was not provided."),
					type(Type.ERROR, "BadRandom", "If a random number generator was not provided.")
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

				if (!temp.isList())
				{
					returnValue.setError("BadList", "Second parameter is not a list.");
					return true;
				}
				
				ListType list = temp.asObjectType(ListType.class);
				
				scriptInstance.popStackValue(temp);
				
				if (!temp.isObjectRef(Random.class))
				{
					returnValue.setError("BadRandom", "First parameter is not a Random.");
					return true;
				}
				
				Random random = temp.asObjectType(Random.class);
				
				if (list.isEmpty())
				{
					returnValue.setNull();
					return true;
				}
				
				list.get(random.nextInt(list.size()), returnValue);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	RANDOMSHUFFLE(2)
	{
		@Override
		protected Usage usage() 
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Shuffles the contents of a list in-place such that the elements are in a different order."
				)
				.parameter("random",
					type(Type.OBJECTREF, "Random", "The random number generator.")
				)
				.parameter("list",
					type(Type.LIST, "The list to select an item from.")
				)
				.returns(
					type(Type.LIST, "The list that was provided (not a new list)."),
					type(Type.ERROR, "BadList", "If a list was not provided."),
					type(Type.ERROR, "BadRandom", "If a random number generator was not provided.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue temp2 = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(temp);

				if (!temp.isList())
				{
					returnValue.setError("BadList", "Second parameter is not a list.");
					return true;
				}
				
				ListType list = temp.asObjectType(ListType.class);
				
				scriptInstance.popStackValue(temp);
				
				if (!temp.isObjectRef(Random.class))
				{
					returnValue.setError("BadRandom", "First parameter is not a Random.");
					return true;
				}
				
				Random random = temp.asObjectType(Random.class);

				returnValue.set(list);

				if (list.isEmpty() || list.size() == 1)
				{
					return true;
				}
				
				for (int i = list.size() - 1; i > 0; i--)
				{
					int src = random.nextInt(i);
					list.get(src, temp);
					list.get(i, temp2);
					list.set(i, temp);
					list.set(src, temp2);
				}

				return true;
			}
			finally
			{
				temp.setNull();
				temp2.setNull();
			}
		}
	},

	;
	
	private final int parameterCount;
	private Usage usage;
	private RandomFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(RandomFunctions.values());
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
