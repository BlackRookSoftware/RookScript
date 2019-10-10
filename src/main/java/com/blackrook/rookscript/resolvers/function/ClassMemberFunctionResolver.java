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
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;
import com.blackrook.rookscript.annotations.ScriptValueType;
import com.blackrook.rookscript.exception.ScriptExecutionException;
import com.blackrook.rookscript.struct.ScriptThreadLocal;
import com.blackrook.rookscript.struct.Utils;
import com.blackrook.rookscript.struct.ScriptThreadLocal.Cache;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile.FieldInfo;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile.MethodInfo;

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
	 * Builds a function resolver from a class, obeying annotations on the class.
	 * Pays attention to annotations for naming/ignoring/converting.
	 * @param <C> the class type.
	 * @param classType the type to build a resolver for.
	 * @return a resolver for this class.
	 * @see ScriptIgnore
	 * @see ScriptName
	 * @see ScriptValueType
	 */
	@SuppressWarnings("unchecked")
	public static <C> ClassMemberFunctionResolver<C> create(Class<C> classType, String getterPrefix, String setterPrefix, String methodPrefix, boolean chained, boolean errorHandling)
	{
		ClassMemberFunctionResolver<C> resolver = new ClassMemberFunctionResolver<>(classType);
		
		Profile<C> profile = (Profile<C>)Utils.getProfile(classType);
		String simpleName = classType.getSimpleName().toLowerCase();
		
		for (Constructor<?> cons : classType.getDeclaredConstructors())
		{
			if (cons.getAnnotation(ScriptIgnore.class) != null)
				continue;

			ScriptValueType typeAnno;
			Type valueType = null;
			if ((typeAnno = cons.getAnnotation(ScriptValueType.class)) != null)
				valueType = typeAnno.value();
			
			ScriptName nameAnno;
			String name;
			if ((nameAnno = cons.getAnnotation(ScriptName.class) ) != null)
				name = nameAnno.value();
			else
				name = simpleName;
			
			resolver.addConstructor(name, (Constructor<C>)cons, valueType, null);
		}

		for (Map.Entry<String, FieldInfo> entry : profile.getPublicFieldsByName().entrySet())
		{
			String name;
			FieldInfo info = entry.getValue();
			if (info.getAlias() != null)
				name = info.getAlias();
			else
				name = entry.getKey();

			ScriptValueType typeAnno;
			Type valueType = null;
			if ((typeAnno = info.getField().getAnnotation(ScriptValueType.class)) != null)
				valueType = typeAnno.value();
			
			resolver.addGetterField(getterPrefix + name, info.getField(), valueType, null);
			resolver.addSetterField(setterPrefix + name, info.getField(), chained, null);
		}
		
		for (Map.Entry<String, MethodInfo> entry : profile.getGetterMethodsByName().entrySet())
		{
			String name;
			MethodInfo info = entry.getValue();
			if (info.getAlias() != null)
				name = info.getAlias();
			else
				name = entry.getKey();

			ScriptValueType typeAnno;
			Type valueType = null;
			if ((typeAnno = info.getMethod().getAnnotation(ScriptValueType.class)) != null)
				valueType = typeAnno.value();

			resolver.addGetterMethod(getterPrefix + name, info.getMethod(), valueType, null);
		}

		for (Map.Entry<String, MethodInfo> entry : profile.getSetterMethodsByName().entrySet())
		{
			String name;
			MethodInfo info = entry.getValue();
			if (info.getAlias() != null)
				name = info.getAlias();
			else
				name = entry.getKey();
			
			resolver.addSetterMethod(setterPrefix + name, info.getMethod(), chained, null);
		}
		
		for (Map.Entry<String, MethodInfo> entry : profile.getMethodsByName().entrySet())
		{
			String name;
			MethodInfo info = entry.getValue();
			if (info.getAlias() != null)
				name = info.getAlias();
			else
				name = entry.getKey();
			
			ScriptValueType typeAnno;
			Type valueType = null;
			if ((typeAnno = info.getMethod().getAnnotation(ScriptValueType.class)) != null)
				valueType = typeAnno.value();

			resolver.addMethod(methodPrefix + name, info.getMethod(), valueType, chained, errorHandling, null);
		}
		
		return resolver;
	}
	
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
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param usage function usage docs.
	 * @see ScriptFunctionType#isVoid()
	 */
	public ClassMemberFunctionResolver<C> addConstructor(String functionName, Constructor<C> constructor, Type type, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new ConstructorInvoker(name, constructor, type, usage));
		return this;
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
	public ClassMemberFunctionResolver<C> addSetterField(String functionName, Field field, boolean chained, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new SetterInvoker(name, field, usage, chained));
		return this;
	}
	
	/**
	 * Adds a field getter to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * The {@link ScriptFunctionType} that this will provide is never void.
	 * @param functionName the script function name.
	 * @param field the field to wrap.
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param usage function usage docs.
	 */
	public ClassMemberFunctionResolver<C> addGetterField(String functionName, Field field, Type type, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new GetterInvoker(name, field, type, usage));
		return this;
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
	public ClassMemberFunctionResolver<C> addSetterMethod(String functionName, Method method, boolean chained, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new SetterInvoker(name, method, usage, chained));
		return this;
	}
	
	/**
	 * Adds a getter method to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * The {@link ScriptFunctionType} that this will provide is never void.
	 * <p>If the wrapped method throws an exception, this will halt script execution.
	 * @param functionName the script function name.
	 * @param method the method to wrap.
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param usage function usage docs.
	 * @throws ScriptExecutionException if the invoked method throws an exception, or the object passed in is not the correct type.
	 */
	public ClassMemberFunctionResolver<C> addGetterMethod(String functionName, Method method, Type type, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new GetterInvoker(name, method, type, usage));
		return this;
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
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param chained if true, this will return the object affected (for command chaining), overriding any return type.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @param usage function usage docs.
	 * @throws ScriptExecutionException if the invoked method throws an exception, or the object passed in is not the correct type.
	 * @see ScriptFunctionType#isVoid()
	 */
	public ClassMemberFunctionResolver<C> addMethod(String functionName, Method method, Type type, boolean chained, boolean errorHandling, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new MethodInvoker(name, method, type, usage, chained, errorHandling));
		return this;
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

	/**
	 * Invoker type for default constructors.
	 */
	private class ConstructorInvoker implements ScriptFunctionType
	{
		private String name;
		private Usage usage;
		private Type type;
		private Constructor<C> constructor;
		private Class<?>[] paramTypes;
		
		private ConstructorInvoker(String name, Constructor<C> constructor, Type type, Usage usage)
		{
			this.name = name;
			this.usage = usage;
			this.type = type;
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
			
			scriptInstance.pushStackValue(ScriptValue.create(type, Utils.construct(constructor, vbuf)));
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
		private Field field;
		private Method method;
		private Type type;
		private Usage usage;
		
		private GetterInvoker(String name, Field field, Type type, Usage usage)
		{
			this.name = name;
			this.field = field;
			this.method = null;
			this.type = type;
			this.usage = usage;
		}
	
		private GetterInvoker(String name, Method method, Type type, Usage usage)
		{
			this.name = name;
			this.field = null;
			this.method = method;
			this.type = type;
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
			{
				Cache cache = ScriptThreadLocal.getCache();
				cache.temp.set(type, Utils.getFieldValue(object, field));
				scriptInstance.pushStackValue(cache.temp);
			}
			else
			{
				Cache cache = ScriptThreadLocal.getCache();
				cache.temp.set(type, Utils.invokeBlind(method, object));
				scriptInstance.pushStackValue(cache.temp);
			}
			
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
		private Type type;
		private boolean chained;
		private boolean errorHandling;
		
		private Class<?>[] paramTypes;
		private Class<?> returnType;

		private MethodInvoker(String name, Method method, Type type, Usage usage, boolean chained, boolean errorHandling)
		{
			this.name = name;
			this.method = method;
			this.type = type;
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
			{
				ScriptExecutionException see = new ScriptExecutionException("First parameter is not the correct type.");
				if (errorHandling)
					scriptInstance.pushStackValue(see);
				else
					throw see; 
			}
			
			Object[] vbuf = new Object[paramTypes.length];
			for (int i = vbuf.length - 1; i >= 0; i++)
				vbuf[i] = scriptInstance.popStackValue().createForType(paramTypes[i]);

			Object retval = null;
			try {
				retval = Utils.invokeBlind(method, object, vbuf);
				if (chained)
					retval = object;
				else
				{
					Cache cache = ScriptThreadLocal.getCache();
					cache.temp.set(type, retval);
					retval = cache.temp;
				}
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

}
