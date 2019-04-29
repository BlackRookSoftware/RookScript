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
import java.util.Iterator;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.TypeProfile;
import com.blackrook.commons.TypeProfile.MethodSignature;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.rookscript.ScriptFunctionResolver;
import com.blackrook.rookscript.ScriptFunctionType;
import com.blackrook.rookscript.ScriptFunctionType.Usage;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;
import com.blackrook.rookscript.exception.ScriptExecutionException;

/**
 * A function resolver that is a wrapper around a class.
 * <p>
 * The functions are linked to the getters and setters of the class.
 * Each "getter" and "setter" requires the object instance as the first parameter.
 * Public fields are prioritized over getters/setters.
 * <p>
 * Good for POJOs, mostly.
 * <p>
 * Constructors/fields/getters/setters annotated with @ScriptIgnore are ignored.
 * Constructors/getters/setters annotated with @ScriptName use the provided name instead of the generated one.
 * Fields annotated with @ScriptName use the provided name, but still prefixed with the get/setname.
 * @author Matthew Tropiano
 */
public class ClassWrapperFunctionResolver implements ScriptFunctionResolver
{
	public static final String DEFAULT_SEPARATOR = "_";
	public static final String DEFAULT_CONSTRUCTORNAME = "create";
	public static final String DEFAULT_GETNAME = "get";
	public static final String DEFAULT_SETNAME = "set";

	/** The valid type to verify. */
	private Class<?> validType;
	/** Setters are chain-able. */
	private boolean chainSetters;
	/** The mapping table. */
	private CaseInsensitiveHashMap<ScriptFunctionType> mappedFunctions;

	/**
	 * Creates a new class method resolver.
	 * Uses the class's simple name as the base, {@link #DEFAULT_SEPARATOR} as the separator, {@link #DEFAULT_CONSTRUCTORNAME} for the found constructor,
	 * {@link #DEFAULT_GETNAME} as the getter prefix, {@link #DEFAULT_SETNAME} as the setter prefix, with chained setters.
	 * @param clazz the class to wrap.
	 */
	public ClassWrapperFunctionResolver(Class<?> clazz)
	{
		this(clazz, clazz.getSimpleName(), DEFAULT_SEPARATOR, DEFAULT_CONSTRUCTORNAME, DEFAULT_GETNAME, DEFAULT_SETNAME, true);
	}

	/**
	 * Creates a new class method resolver.
	 * @param clazz the class to wrap.
	 * @param baseName the base name of the functions that use this class.
	 * @param separator the separator character between the base names and the get/set name and the field/method name.
	 * @param constructorName the name to use for the found constructor.
	 * @param getName the name to use for getters.
	 * @param setName the name to use for setters.
	 * @param chainSetters if true, all setters return the passed in object for chaining. if false, they are void.
	 */
	public ClassWrapperFunctionResolver(Class<?> clazz, String baseName, String separator, String constructorName, String getName, String setName, boolean chainSetters)
	{
		this.validType = clazz;
		this.chainSetters = chainSetters;
		this.mappedFunctions = new CaseInsensitiveHashMap<>();
		
		TypeProfile<?> profile = TypeProfile.getTypeProfile(clazz);
		StringBuilder sb = new StringBuilder();
		String objName = clazz.getSimpleName();
		
		int constructorCount = 0;
		
		for (Constructor<?> constructor : clazz.getConstructors())
		{
			if (constructor.getAnnotation(ScriptIgnore.class) != null)
				continue;
			
			String name;
			ScriptName anno;
			if ((anno = constructor.getAnnotation(ScriptName.class)) != null)
			{
				name = anno.value();
			}
			else
			{
				sb.append(baseName).append(separator).append(constructorName);
				if (constructorCount > 0)
					sb.append(separator).append(constructorCount - 1);
				
				name = sb.toString().toLowerCase();
				sb.delete(0, sb.length());
			}
			
			if (constructor.getParameterCount() == 0)
				mappedFunctions.put(name, new DefaultConstructorInvoker(name, new InvokerUsage("Creates a new "+objName+".")));
			else
			{
				String[] pu = new String[constructor.getParameterCount()];
				for (int i = 0; i < pu.length; i++)
					pu[i] = "Parameter " + i + ": " + constructor.getParameterTypes()[i].getSimpleName();
					
				mappedFunctions.put(name, new ConstructorInvoker(name, constructor, new InvokerUsage("Creates a new "+objName+".", pu)));
			}
			
			constructorCount++;
		}
		
		for (ObjectPair<String, Field> fieldPair : profile.getPublicFields())
		{
			Field field = fieldPair.getValue();
			if (field.getAnnotation(ScriptIgnore.class) != null)
				continue;

			String fieldName = fieldPair.getKey();
			String fieldUsageGet = "Gets the value of the \"" + fieldName + "\" field on a " + objName + " object (via public field).";
			String fieldUsageSet = "Sets the value of the \"" + fieldName + "\" field on a " + objName + " object (via public field).";
			
			String getter;
			String setter;
			ScriptName anno;
			if ((anno = field.getAnnotation(ScriptName.class)) != null)
			{
				getter = getName + separator + anno.value();
				setter = setName + separator + anno.value();
			}
			else
			{
				getter = (sb.append(baseName).append(separator).append(getName).append(separator).append(fieldName).toString()).toLowerCase();
				sb.delete(0, sb.length());
				setter = (sb.append(baseName).append(separator).append(setName).append(separator).append(fieldName).toString()).toLowerCase();
				sb.delete(0, sb.length());
			}
			
			mappedFunctions.put(getter, new GetterInvoker(getter, field, new InvokerUsage(fieldUsageGet, "The source object.")));
			mappedFunctions.put(setter, new SetterInvoker(setter, field, new InvokerUsage(fieldUsageSet, "The target object.", "The value to set (will be converted to "+fieldPair.getValue().getType().getSimpleName()+" if possible).")));
		}

		for (ObjectPair<String, MethodSignature> methodPair : profile.getGetterMethods())
		{
			Method method = methodPair.getValue().getMethod();
			if (method.getAnnotation(ScriptIgnore.class) != null)
				continue;

			String fieldName = methodPair.getKey();
			
			String getter;
			ScriptName anno;
			if ((anno = method.getAnnotation(ScriptName.class)) != null)
			{
				getter = anno.value();
			}
			else
			{
				getter = (sb.append(baseName).append(separator).append(getName).append(separator).append(fieldName).toString()).toLowerCase();
				sb.delete(0, sb.length());
			}
			
			if (mappedFunctions.containsKey(getter))
				continue;

			String fieldUsageGet = "Gets the value of the \"" + fieldName + "\" field on a " + objName + " object (via getter method).";
			mappedFunctions.put(getter, new GetterInvoker(getter, method, new InvokerUsage(fieldUsageGet, "The source object.")));
		}
		
		for (ObjectPair<String, MethodSignature> methodPair : profile.getSetterMethods())
		{
			Method method = methodPair.getValue().getMethod();
			if (method.getAnnotation(ScriptIgnore.class) != null)
				continue;

			String fieldName = methodPair.getKey();
			
			String setter;
			ScriptName anno;
			if ((anno = method.getAnnotation(ScriptName.class)) != null)
			{
				setter = anno.value();
			}
			else
			{
				setter = (sb.append(baseName).append(separator).append(setName).append(separator).append(fieldName).toString()).toLowerCase();
				sb.delete(0, sb.length());
			}
			
			if (mappedFunctions.containsKey(setter))
				continue;

			String fieldUsageSet = "Sets the value of the \"" + fieldName + "\" field on a " + objName + " object (via setter method).";
			mappedFunctions.put(setter, new GetterInvoker(setter, method, new InvokerUsage(fieldUsageSet, "The target object.", "The value to set (will be converted to "+methodPair.getValue().getType().getSimpleName()+" if possible).")));
		}
		
	}

	@Override
	public boolean containsFunctionByName(String name)
	{
		return mappedFunctions.containsKey(name);
	}

	@Override
	public ScriptFunctionType getFunctionByName(String name)
	{
		return mappedFunctions.get(name);
	}

	@Override
	public ScriptFunctionType[] getFunctions()
	{
		int i = 0;
		ScriptFunctionType[] out = new ScriptFunctionType[mappedFunctions.size()];
		Iterator<ScriptFunctionType> it = mappedFunctions.valueIterator();
		while (it.hasNext())
			out[i++] = it.next();
		return out;
	}

	/**
	 * Invoker usage stuff.
	 */
	private static class InvokerUsage implements Usage
	{
		private String instructions;
		private String[] paramInstructions;
		
		private InvokerUsage(String instructions, String ... paramInstructions)
		{
			this.instructions = instructions;
			this.paramInstructions = paramInstructions;
		}

		@Override
		public String getUsageInstructions()
		{
			return instructions;
		}

		@Override
		public String[] getUsageParameterInstructions()
		{
			return paramInstructions;
		}
		
	}

	/**
	 * Invoker type for default constructors.
	 */
	private class ConstructorInvoker implements ScriptFunctionType
	{
		private String name;
		private Usage usage;
		private Constructor<?> constructor;
		
		private Object[] vbuf;
		
		private ConstructorInvoker(String name, Constructor<?> constructor, Usage usage)
		{
			this.name = name;
			this.usage = usage;
			this.constructor = constructor;
			this.vbuf = new Object[constructor.getParameterCount()];
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
			for (int i = vbuf.length - 1; i >= 0; i++)
				vbuf[i] = scriptInstance.popStackValue().asObject();
			
			scriptInstance.pushStackValue(Reflect.construct(constructor, vbuf));
			return true;
		}

	}
	
	/**
	 * Invoker type for default constructors.
	 */
	private class DefaultConstructorInvoker implements ScriptFunctionType
	{
		private String name;
		private Usage usage;

		private DefaultConstructorInvoker(String name, Usage usage)
		{
			this.name = name;
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
			return 0;
		}

		@Override
		public Usage getUsage()
		{
			return usage;
		}

		@Override
		public boolean execute(ScriptInstance scriptInstance)
		{
			scriptInstance.pushStackValue(Reflect.create(validType));
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

		private SetterInvoker(String name, Field field, Usage usage)
		{
			this.name = name;
			this.field = field;
			this.method = null;
			this.usage = usage;
			this.type = field.getType();
		}

		private SetterInvoker(String name, Method method, Usage usage)
		{
			this.name = name;
			this.field = null;
			this.method = method;
			this.usage = usage;
			this.type = method.getParameterTypes()[0];
		}
		
		@Override
		public boolean isVoid()
		{
			return !chainSetters;
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
				Reflect.setField(object, field, value.createForType(type));
			else
				Reflect.invokeBlind(method, object, value.createForType(type));

			if (chainSetters)
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
				scriptInstance.pushStackValue(Reflect.getFieldValue(field, object));
			else
				scriptInstance.pushStackValue(Reflect.invokeBlind(method, object));
			
			return true;
		}
		
	}
	
}
