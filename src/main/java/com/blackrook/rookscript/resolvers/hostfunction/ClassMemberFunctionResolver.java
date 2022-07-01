/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.hostfunction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;
import com.blackrook.rookscript.annotations.ScriptValueType;
import com.blackrook.rookscript.exception.ScriptExecutionException;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionType.Usage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile.FieldInfo;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile.MethodInfo;
import com.blackrook.rookscript.struct.Utils;

/**
 * A function resolver that wraps individual constructors, fields, or functions.
 * Constructors create objects, methods/fields require an object instance, unless the method that was wrapped is static.
 * <p>The functions created this way have a little more overhead than proper {@link ScriptFunctionType}s, since they employ
 * reflection to invoke the underlying methods.
 * @author Matthew Tropiano 
 * @param <C> the class type that this uses.
 */
public class ClassMemberFunctionResolver<C> implements ScriptFunctionResolver
{
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<InvokerCache> OBJECTARRAYS = ThreadLocal.withInitial(()->new InvokerCache());

	/** The map of name to function type. */
	private SortedMap<String, ScriptFunctionType> map;
	/** The valid type to verify. */
	private Class<C> validType;

	/**
	 * Builds a function resolver from a class, obeying annotations on the class.
	 * Pays attention to annotations for naming/ignoring/converting.
	 * @param <C> the class type.
	 * @param classType the type to build a resolver for.
	 * @param getterPrefix the prefix to add for each getter.
	 * @param setterPrefix the prefix to add for each setter.
	 * @param methodPrefix the prefix to add for each method.
	 * @param chained if true, every setter function returns the object that it changed.
	 * @param errorHandling if true, exceptions that may occur on call are returned as Error type objects.
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
			
			resolver.addConstructor(name, (Constructor<C>)cons, valueType, errorHandling, null);
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
			
			resolver.addGetterField(getterPrefix + name, info.getField(), valueType, errorHandling, null);
			resolver.addSetterField(setterPrefix + name, info.getField(), chained, errorHandling, null);
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

			resolver.addMethod(getterPrefix + name, info.getMethod(), valueType, false, errorHandling, null);
		}

		for (Map.Entry<String, MethodInfo> entry : profile.getSetterMethodsByName().entrySet())
		{
			String name;
			MethodInfo info = entry.getValue();
			if (info.getAlias() != null)
				name = info.getAlias();
			else
				name = entry.getKey();
			
			resolver.addMethod(setterPrefix + name, info.getMethod(), null, chained, errorHandling, null);
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
		this.map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	}
	
	/**
	 * Adds a constructor to this resolver, using a specific function name for mapping.
	 * This will overwrite a previous mapping.
	 * <p>NOTE: This ignores annotations that may affect naming or ignoring this constructor.
	 * @param functionName the script function name.
	 * @param constructor the constructor method to wrap.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @return this function resolver.
	 */
	public ClassMemberFunctionResolver<C> addConstructor(String functionName, Constructor<C> constructor, boolean errorHandling)
	{
		return addConstructor(functionName, constructor, null, errorHandling, null);
	}
	
	/**
	 * Adds a constructor to this resolver, using a specific function name for mapping.
	 * This will overwrite a previous mapping.
	 * <p>NOTE: This ignores annotations that may affect naming or ignoring this constructor.
	 * @param functionName the script function name.
	 * @param constructor the constructor method to wrap.
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @return this function resolver.
	 */
	public ClassMemberFunctionResolver<C> addConstructor(String functionName, Constructor<C> constructor, Type type, boolean errorHandling)
	{
		return addConstructor(functionName, constructor, type, errorHandling, null);
	}
	
	/**
	 * Adds a constructor to this resolver, using a specific function name for mapping.
	 * This will overwrite a previous mapping.
	 * <p>NOTE: This ignores annotations that may affect naming or ignoring this constructor.
	 * @param functionName the script function name.
	 * @param constructor the constructor method to wrap.
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @param usage function usage docs.
	 * @return this function resolver.
	 */
	public ClassMemberFunctionResolver<C> addConstructor(String functionName, Constructor<C> constructor, Type type, boolean errorHandling, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new ConstructorInvoker(name, constructor, type, errorHandling, usage));
		return this;
	}
	
	/**
	 * Adds a field setter to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param fieldName the name of the class's field to wrap.
	 * @param chained if true, this will return the object affected (for command chaining), else null.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @return this function resolver.
	 * @throws IllegalArgumentException if the field could not be found.
	 */
	public ClassMemberFunctionResolver<C> addSetterField(String functionName, String fieldName, boolean chained, boolean errorHandling)
	{
		try {
			return addSetterField(functionName, validType.getField(fieldName), chained, errorHandling, null);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new IllegalArgumentException("Could not find field: " + fieldName, e);
		}
	}
	
	/**
	 * Adds a field setter to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param fieldName the name of the class's field to wrap.
	 * @param chained if true, this will return the object affected (for command chaining), else null.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @param usage function usage docs.
	 * @return this function resolver.
	 * @throws IllegalArgumentException if the field could not be found.
	 */
	public ClassMemberFunctionResolver<C> addSetterField(String functionName, String fieldName, boolean chained, boolean errorHandling, Usage usage)
	{
		try {
			return addSetterField(functionName, validType.getField(fieldName), chained, errorHandling, usage);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new IllegalArgumentException("Could not find field: " + fieldName, e);
		}
	}
	
	/**
	 * Adds a field setter to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param field the field to wrap.
	 * @param chained if true, this will return the object affected (for command chaining), else null.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @param usage function usage docs.
	 * @return this function resolver.
	 */
	public ClassMemberFunctionResolver<C> addSetterField(String functionName, Field field, boolean chained, boolean errorHandling, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new SetterFieldInvoker(name, field, chained, errorHandling, usage));
		return this;
	}
	
	/**
	 * Adds a field getter to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param fieldName the name of the class's field to wrap.
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @return this function resolver.
	 * @throws IllegalArgumentException if the field could not be found.
	 */
	public ClassMemberFunctionResolver<C> addGetterField(String functionName, String fieldName, Type type, boolean errorHandling)
	{
		try {
			return addGetterField(functionName, validType.getField(fieldName), type, errorHandling, null);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new IllegalArgumentException("Could not find field: " + fieldName, e);
		}
	}
	
	/**
	 * Adds a field getter to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param fieldName the name of the class's field to wrap.
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @param usage function usage docs.
	 * @return this function resolver.
	 * @throws IllegalArgumentException if the field could not be found.
	 */
	public ClassMemberFunctionResolver<C> addGetterField(String functionName, String fieldName, Type type, boolean errorHandling, Usage usage)
	{
		try {
			return addGetterField(functionName, validType.getField(fieldName), type, errorHandling, usage);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new IllegalArgumentException("Could not find field: " + fieldName, e);
		}
	}
	
	/**
	 * Adds a field getter to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param field the field to wrap.
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @param usage function usage docs.
	 * @return this function resolver.
	 */
	public ClassMemberFunctionResolver<C> addGetterField(String functionName, Field field, Type type, boolean errorHandling, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new GetterFieldInvoker(name, field, type, errorHandling, usage));
		return this;
	}
	
	/**
	 * Adds a class method to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param methodName the name of the class's method to wrap.
	 * @param chained if true, this will return the object affected (for command chaining), overriding any return type.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @return this function resolver.
	 * @throws IllegalArgumentException if the method could not be found.
	 */
	public ClassMemberFunctionResolver<C> addMethod(String functionName, String methodName, boolean chained, boolean errorHandling)
	{
		return addMethod(functionName, methodName, null, chained, errorHandling, null);
	}
	
	/**
	 * Adds a class method to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param methodName the name of the class's method to wrap.
	 * @param chained if true, this will return the object affected (for command chaining), overriding any return type.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @param usage function usage docs.
	 * @return this function resolver.
	 * @throws IllegalArgumentException if the method could not be found.
	 */
	public ClassMemberFunctionResolver<C> addMethod(String functionName, String methodName, boolean chained, boolean errorHandling, Usage usage)
	{
		return addMethod(functionName, methodName, null, chained, errorHandling, usage);
	}
	
	/**
	 * Adds a class method to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param methodName the name of the class's method to wrap.
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param chained if true, this will return the object affected (for command chaining), overriding any return type.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @param usage function usage docs.
	 * @return this function resolver.
	 * @throws ScriptExecutionException (errorHandling is not true) if the invoked method throws an exception, or the object passed in is not the correct type.
	 * @throws IllegalArgumentException if the method could not be found.
	 */
	public ClassMemberFunctionResolver<C> addMethod(String functionName, String methodName, Type type, boolean chained, boolean errorHandling, Usage usage)
	{
		try {
			return addMethod(functionName, validType.getMethod(methodName), type, chained, errorHandling, usage);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException("Could not find method: " + methodName, e);
		}
	}
	
	/**
	 * Adds a class method to this resolver, using specific a function name for mapping.
	 * This will overwrite previous mappings.
	 * <p>NOTE: This ignores any annotations that may affect naming or ignoring this field.
	 * @param functionName the script function name.
	 * @param method the method to wrap.
	 * @param type the target script value type on conversion. Can be null for automatic.
	 * @param chained if true, this will return the object affected (for command chaining), overriding any return type.
	 * @param errorHandling if true, this will return thrown errors as an Error type, instead of throwing it as a ScriptExecutionException.
	 * @param usage function usage docs.
	 * @return this function resolver.
	 */
	public ClassMemberFunctionResolver<C> addMethod(String functionName, Method method, Type type, boolean chained, boolean errorHandling, Usage usage)
	{
		String name = functionName.toLowerCase();
		map.put(name, new MethodInvoker(name, method, type, chained, errorHandling, usage));
		return this;
	}
	
	@Override
	public ScriptFunctionType getFunction(String name)
	{
		return map.get(name.toLowerCase());
	}

	@Override
	public boolean containsFunction(String name)
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
		private boolean errorHandling;
		private Constructor<C> constructor;
		private Class<?>[] paramTypes;
		
		private ConstructorInvoker(String name, Constructor<C> constructor, Type type, boolean errorHandling, Usage usage)
		{
			this.name = name;
			this.usage = usage;
			this.type = type;
			this.errorHandling = errorHandling;
			this.constructor = constructor;
			this.paramTypes = constructor.getParameterTypes();
		}
	
		@Override
		public String name()
		{
			return name;
		}
		
		@Override
		public int getParameterCount()
		{
			return paramTypes.length;
		}
	
		@Override
		public Usage getUsage()
		{
			return usage;
		}
	
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue value = CACHEVALUE1.get();
			Object[] vbuf = OBJECTARRAYS.get().getParamArray(paramTypes.length);
			try 
			{
				for (int i = vbuf.length - 1; i >= 0; i--)
				{
					scriptInstance.popStackValue(value);
					vbuf[i] = value.createForType(paramTypes[i]);
				}
				
				value.set(type, Utils.construct(constructor, vbuf));
				returnValue.set(value);
				return true;
			}
			catch (Exception e)
			{
				if (errorHandling)
				{
					returnValue.set(e);
					return true;
				}
				else
					throw e; 
			}
			finally
			{
				value.setNull();
				Arrays.fill(vbuf, null); // arrays are shared - purge refs after use.
			}
		}
	
		@Override
		public String toString()
		{
			return "Constructor " + validType.getSimpleName() + ": " + name + "(" + getParameterCount() + ")";
		}
		
	}

	/**
	 * Invoker type for setter.
	 */
	private class SetterFieldInvoker implements ScriptFunctionType
	{
		private String name;
		private Usage usage;
		private Field field;
		private Class<?> type;
		private boolean chained;
		private boolean errorHandling;

		private SetterFieldInvoker(String name, Field field, boolean chained, boolean errorHandling, Usage usage)
		{
			this.name = name;
			this.field = field;
			this.usage = usage;
			this.type = field.getType();
			this.errorHandling = errorHandling;
			this.chained = chained;
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue instance = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				scriptInstance.popStackValue(instance);
			
				Object object = instance.asObject();
				
				if (!validType.isAssignableFrom(object.getClass()))
				{
					String message = "First parameter is not " + object.getClass().getSimpleName() + ".";
					if (errorHandling)
					{
						returnValue.setError("BadParameter", message);
						return true;
					}
					else
						throw new ScriptExecutionException(message); 
				}
				
				Utils.setFieldValue(object, field, value.createForType(type));
				returnValue.set(chained ? object : null);
				return true;
			}
			finally
			{
				value.setNull();
				instance.setNull();
			}
		}
		
		@Override
		public String toString()
		{
			return "Field Setter " + validType.getSimpleName() + "." + field.getName() + ": " + name + "(" + getParameterCount() + ")";
		}
		
	}

	/**
	 * Invoker type for getter.
	 */
	private class GetterFieldInvoker implements ScriptFunctionType
	{
		private String name;
		private Field field;
		private Type type;
		private boolean errorHandling;
		private Usage usage;
		
		private GetterFieldInvoker(String name, Field field, Type type, boolean errorHandling, Usage usage)
		{
			this.name = name;
			this.field = field;
			this.type = type;
			this.errorHandling = errorHandling;
			this.usage = usage;
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
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				
				Object object = temp.asObject();
				
				if (!validType.isAssignableFrom(object.getClass()))
				{
					String message = "First parameter is not " + object.getClass().getSimpleName() + ".";
					if (errorHandling)
					{
						returnValue.setError("BadParameter", message);
						return true;
					}
					else
						throw new ScriptExecutionException(message); 
				}
				
				temp.set(type, Utils.getFieldValue(object, field));
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
		
		@Override
		public String toString()
		{
			return "Field Getter " + validType.getSimpleName() + "." + field.getName() + ": " + name + "(" + getParameterCount() + ")";
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
		
		private boolean isStatic;
		private Class<?>[] paramTypes;

		private MethodInvoker(String name, Method method, Type type, boolean chained, boolean errorHandling, Usage usage)
		{
			this.name = name;
			this.method = method;
			this.type = type;
			this.usage = usage;
			this.chained = chained;
			this.errorHandling = errorHandling;
			this.isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
			this.paramTypes = method.getParameterTypes();
		}
		
		@Override
		public String name()
		{
			return name;
		}
		
		@Override
		public int getParameterCount()
		{
			return paramTypes.length + (isStatic ? 0 : 1);
		}
		
		@Override
		public Usage getUsage()
		{
			return usage;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			Object[] vbuf = OBJECTARRAYS.get().getParamArray(paramTypes.length);
			try
			{
				for (int i = vbuf.length - 1; i >= 0; i++)
				{
					scriptInstance.popStackValue(temp);
					vbuf[i] = temp.createForType(paramTypes[i]);
				}

				Object object = null;
				if (!isStatic)
				{
					scriptInstance.popStackValue(temp);
					object = temp.asObject();
					
					if (!validType.isAssignableFrom(object.getClass()))
					{
						String message = "First parameter is not " + object.getClass().getSimpleName() + ".";
						if (errorHandling)
						{
							returnValue.setError("BadParameter", message);
							return true;
						}
						else
							throw new ScriptExecutionException(message); 
					}
				}
				
				Object retval = null;
				try 
				{
					retval = Utils.invokeBlind(method, object, vbuf);

					if (chained)
					{
						retval = object;
					}
					else
					{
						temp.set(type, retval);
						retval = temp;
					}
				} 
				catch (Throwable t) 
				{
					if (errorHandling)
						retval = t;
					else
						throw t;
				}

				returnValue.set(retval);
				return true;
			}
			finally
			{
				temp.setNull();
				Arrays.fill(vbuf, null);
			}
		}

		@Override
		public String toString()
		{
			return "Method " + validType.getSimpleName() + "." + method.getName() + ": " + name + "(" + getParameterCount() + ")";
		}
		
	}

	// Parameter array cache.
	private static class InvokerCache
	{
		private HashMap<Integer, Object[]> map;
		
		private InvokerCache()
		{
			map = new HashMap<>();
		}
		
		public Object[] getParamArray(int size)
		{
			Object[] out;
			if ((out = map.get(size)) == null)
				map.put(size, out = new Object[size]);
			return out;
		}

	}

}
