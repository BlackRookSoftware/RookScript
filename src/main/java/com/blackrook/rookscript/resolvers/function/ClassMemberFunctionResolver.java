/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.function;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.blackrook.rookscript.ScriptFunctionResolver;
import com.blackrook.rookscript.ScriptFunctionType;
import com.blackrook.rookscript.ScriptFunctionType.Usage;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.exception.ScriptExecutionException;
import com.blackrook.rookscript.util.Utils;

/**
 * A function resolver that wraps individual constructors, fields, or functions.
 * Constructors create objects, methods/fields require an object instance (no static modifiers).
 * @author Matthew Tropiano 
 */
public class ClassMemberFunctionResolver<C> implements ScriptFunctionResolver
{
	/** The map of name to function type. */
	private Map<String, ScriptFunctionType> map;
	/** The valid type to verify. */
	private Class<C> validType;

	/**
	 * Creates a class function resolver.
	 * This does not extract methods/fields/constructors automatically, and creates an empty resolver.
	 * @param type the base class type.
	 */
	public ClassMemberFunctionResolver(Class<C> type)
	{
		this.validType = type;
		this.map = new HashMap<>(8, 1f);
	}
	
	/**
	 * Adds a constructor to this resolver, using a specific function name for mapping.
	 * This will overwrite a previous mapping.
	 * <p>NOTE: This ignores annotations that may affect naming or ignoring this constructor.
	 * The {@link ScriptFunctionType} that this will provide is never void.
	 * @param functionName the script function name.
	 * @param constructor the constructor method to wrap.
	 * @param usage function usage docs.
	 * @see ScriptFunctionType#isVoid()
	 */
	public void addConstructor(String functionName, Constructor<C> constructor, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new ConstructorInvoker(name, constructor, usage));
	}
	
	/**
	 * Adds a field setter to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * The {@link ScriptFunctionType} that this will provide is void unless <code>chained</code> is true.
	 * @param functionName the script function name.
	 * @param field the field to wrap.
	 * @param chained if true, this will return the object affected (for command chaining).
	 * @param usage function usage docs.
	 * @see ScriptFunctionType#isVoid()
	 */
	public void addSetterField(String functionName, Field field, boolean chained, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new SetterInvoker(name, field, usage, chained));
	}
	
	/**
	 * Adds a field getter to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * The {@link ScriptFunctionType} that this will provide is never void.
	 * @param functionName the script function name.
	 * @param field the field to wrap.
	 * @param usage function usage docs.
	 */
	public void addGetterField(String functionName, Field field, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new GetterInvoker(name, field, usage));
	}
	
	/**
	 * Adds a setter method to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * The {@link ScriptFunctionType} that this will provide is void unless <code>chained</code> is true.
	 * @param functionName the script function name.
	 * @param method the method to wrap.
	 * @param chained if true, this will return the object affected (for command chaining).
	 * @param usage function usage docs.
	 * @throws ScriptExecutionException if the invoked method throws an exception, or the object passed in is not the correct type.
	 * @see ScriptFunctionType#isVoid()
	 */
	public void addSetterMethod(String functionName, Method method, boolean chained, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new SetterInvoker(name, method, usage, chained));
	}
	
	/**
	 * Adds a getter method to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * The {@link ScriptFunctionType} that this will provide is never void.
	 * <p>If the wrapped method throws an exception, this will halt script execution.
	 * @param functionName the script function name.
	 * @param method the method to wrap.
	 * @param usage function usage docs.
	 * @throws ScriptExecutionException if the invoked method throws an exception, or the object passed in is not the correct type.
	 */
	public void addGetterMethod(String functionName, Method method, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new GetterInvoker(name, method, usage));
	}
	
	/**
	 * Adds a class method to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * The {@link ScriptFunctionType} that this will provide is void if not chained, nor error-handling, 
	 * nor if the wrapped method has a return type (void).
	 * 
	 * @param functionName the script function name.
	 * @param method the field to wrap.
	 * @param chained if true, this will return the object affected (for command chaining), overriding any return type.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of .
	 * @param usage function usage docs.
	 * @throws ScriptExecutionException if the invoked method throws an exception, or the object passed in is not the correct type.
	 * @see ScriptFunctionType#isVoid()
	 */
	public void addMethod(String functionName, Method method, boolean chained, boolean errorHandling, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new MethodInvoker(name, method, usage, chained, errorHandling));
	}
	
	/**
	 * Invoker type for default constructors.
	 */
	private class ConstructorInvoker implements ScriptFunctionType
	{
		private String name;
		private Usage usage;
		private Constructor<C> constructor;
		private Class<?>[] paramTypes;
		
		private ConstructorInvoker(String name, Constructor<C> constructor, Usage usage)
		{
			this.name = name;
			this.usage = usage;
			this.constructor = constructor;
			this.paramTypes = constructor.getParameterTypes();
		}
	
		@Override
		public boolean isVoid()
		{
			return false;
		}
	
		@Override
		public String name()
		{
			return name;
		}
	
		@Override
		public int getParameterCount()
		{
			return constructor.getParameterCount();
		}
	
		@Override
		public Usage getUsage()
		{
			return usage;
		}
	
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			Object[] vbuf = new Object[paramTypes.length];
			for (int i = vbuf.length - 1; i >= 0; i--)
				vbuf[i] = scriptInstance.popStackValue().createForType(paramTypes[i]);
			
			scriptInstance.pushStackValue(ScriptValue.create(Utils.construct(constructor, vbuf)));
			return true;
		}
	
	}

	/**
	 * Invoker type for setter.
	 */
	private class SetterInvoker implements ScriptFunctionType
	{
		private String name;
		private Usage usage;
		private Field field;
		private Method method;
		private Class<?> type;
		private boolean chained;
	
		private SetterInvoker(String name, Field field, Usage usage, boolean chained)
		{
			this.name = name;
			this.field = field;
			this.method = null;
			this.usage = usage;
			this.type = field.getType();
			this.chained = chained;
		}
	
		private SetterInvoker(String name, Method method, Usage usage, boolean chained)
		{
			this.name = name;
			this.field = null;
			this.method = method;
			this.usage = usage;
			this.type = method.getParameterTypes()[0];
			this.chained = chained;
		}
		
		@Override
		public boolean isVoid()
		{
			return !chained;
		}
	
		@Override
		public String name()
		{
			return name;
		}
	
		@Override
		public int getParameterCount()
		{
			return 2;
		}
	
		@Override
		public Usage getUsage()
		{
			return usage;
		}
	
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue instance = scriptInstance.popStackValue();
		
			Object object = instance.asObject();
			
			if (!validType.isAssignableFrom(object.getClass()))
				throw new ScriptExecutionException("First parameter is not the correct type.");
			
			if (field != null)
				Utils.setFieldValue(object, field, value.createForType(type));
			else
				Utils.invokeBlind(method, object, value.createForType(type));
	
			if (chained)
				scriptInstance.pushStackValue(object);
			
			return true;
		}
		
	}

	/**
	 * Invoker type for getter.
	 */
	private class GetterInvoker implements ScriptFunctionType
	{
		private String name;
		private Usage usage;
		private Field field;
		private Method method;
		
		private GetterInvoker(String name, Field field, Usage usage)
		{
			this.name = name;
			this.field = field;
			this.method = null;
			this.usage = usage;
		}
	
		private GetterInvoker(String name, Method method, Usage usage)
		{
			this.name = name;
			this.field = null;
			this.method = method;
			this.usage = usage;
		}
		
		@Override
		public boolean isVoid()
		{
			return false;
		}
	
		@Override
		public String name()
		{
			return name;
		}
	
		@Override
		public int getParameterCount()
		{
			return 1;
		}
	
		@Override
		public Usage getUsage()
		{
			return usage;
		}
	
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue instance = scriptInstance.popStackValue();
	
			Object object = instance.asObject();
			
			if (!validType.isAssignableFrom(object.getClass()))
				throw new ScriptExecutionException("First parameter is not the correct type.");
			
			if (field != null)
				scriptInstance.pushStackValue(Utils.getFieldValue(object, field));
			else
				scriptInstance.pushStackValue(Utils.invokeBlind(method, object));
			
			return true;
		}
		
	}

	/**
	 * Method Invoker type.
	 */
	private class MethodInvoker implements ScriptFunctionType
	{
		private String name;
		private Usage usage;
		private Method method;
		private boolean chained;
		private boolean errorHandling;
		
		private Class<?>[] paramTypes;
		private Class<?> returnType;

		private MethodInvoker(String name, Method method, Usage usage, boolean chained, boolean errorHandling)
		{
			this.name = name;
			this.method = method;
			this.usage = usage;
			this.chained = chained;
			this.errorHandling = errorHandling;
			this.returnType = method.getReturnType();
			this.paramTypes = method.getParameterTypes();
		}
		
		@Override
		public String name()
		{
			return name;
		}
		
		@Override
		public boolean isVoid()
		{
			return chained || errorHandling || returnType == Void.TYPE || returnType == Void.class;
		}
		
		@Override
		public int getParameterCount()
		{
			return paramTypes.length + 1;
		}
		
		@Override
		public Usage getUsage()
		{
			return usage;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			ScriptValue instance = scriptInstance.popStackValue();
			
			Object object = instance.asObject();
			
			if (!validType.isAssignableFrom(object.getClass()))
				throw new ScriptExecutionException("First parameter is not the correct type.");
			
			Object[] vbuf = new Object[paramTypes.length];
			for (int i = vbuf.length - 1; i >= 0; i++)
				vbuf[i] = scriptInstance.popStackValue().createForType(paramTypes[i]);

			Object retval = null;
			try {
				retval = Utils.invokeBlind(method, object, vbuf);
				if (chained)
					retval = object;
			} catch (Throwable t) {
				if (errorHandling)
					retval = t;
				else
					throw t;
			}

			if (!isVoid())
				scriptInstance.pushStackValue(retval);
			
			return true;
		}
	}
	
	@Override
	public ScriptFunctionType getFunctionByName(String name)
	{
		return map.get(name.toLowerCase());
	}
	
	@Override
	public boolean containsFunctionByName(String name)
	{
		return map.containsKey(name.toLowerCase());
	}

	@Override
	public ScriptFunctionType[] getFunctions()
	{
		ScriptFunctionType[] out = new ScriptFunctionType[map.size()];
		int i = 0;
		for (Map.Entry<String, ScriptFunctionType> type : map.entrySet())
			out[i++] = type.getValue();
		return out;
	}

}
