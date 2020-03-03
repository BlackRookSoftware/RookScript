/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * A factory that produces type profiles for POJOs and data objects.
 * @author Matthew Tropiano
 */
public class TypeProfileFactory
{
	/** The policy used by this factory. */
	private MemberPolicy policy;
	/** Generated profiles. */
	private HashMap<Class<?>, Profile<?>> generatedProfiles;
	
	/**
	 * Creates a new TypeProfileFactory.
	 * This creates type profiles using an additional policy that changes method/field handling
	 * for each type.
	 * @param policy the member policy.
	 */
	TypeProfileFactory(MemberPolicy policy)
	{
		this.policy = policy;
		this.generatedProfiles = new HashMap<>(8);
	}

	/**
	 * Creates a new profile for a provided type.
	 * Generated profiles are stored in memory, and retrieved again by class type.
	 * <p>This method is thread-safe.
	 * @param <T> the class type.
	 * @param clazz the class.
	 * @return a new profile.
	 */
	@SuppressWarnings("unchecked")
	<T> Profile<T> getProfile(Class<T> clazz)
	{
		Profile<T> out = null;
		if ((out = (Profile<T>)generatedProfiles.get(clazz)) == null)
		{
			synchronized (generatedProfiles)
			{
				// early out.
				if ((out = (Profile<T>)generatedProfiles.get(clazz)) == null)
				{
					out = new Profile<>(clazz, policy);
					generatedProfiles.put(clazz, out);
				}
			}
		}
		else
			out = (Profile<T>)generatedProfiles.get(clazz);
		
		return out;
	}
	
	/**
	 * Returns the field name for a getter/setter method.
	 * If the method name is not a getter or setter name, then this will return <code>methodName</code>
	 * <p>
	 * For example, the field name "setColor" will return "color" and "isHidden" returns "hidden". 
	 * (note the change in camel case).
	 * @param methodName the name of the method.
	 * @return the modified method name.
	 */
	private static String getFieldName(String methodName)
	{
		if (isGetterName(methodName))
		{
			if (methodName.startsWith("is"))
				return truncateMethodName(methodName, true);
			else if (methodName.startsWith("get"))
				return truncateMethodName(methodName, false);
		}
		else if (isSetterName(methodName))
			return truncateMethodName(methodName, false);
		
		return methodName;
	}

	/**
	 * Checks if a method name describes a "setter" method. 
	 * @param methodName the name of the method.
	 * @return true if so, false if not.
	 */
	private static boolean isSetterName(String methodName)
	{
		if (methodName.startsWith("set"))
		{
			if (methodName.length() < 4)
				return false;
			else
				return Character.isUpperCase(methodName.charAt(3));
		}
		return false;
	}

	/**
	 * Checks if a method name describes a "getter" method (also detects "is" methods). 
	 * @param methodName the name of the method.
	 * @return true if so, false if not.
	 */
	private static boolean isGetterName(String methodName)
	{
		if (methodName.startsWith("is"))
		{
			if (methodName.length() < 3)
				return false;
			else
				return Character.isUpperCase(methodName.charAt(2));
		}
		else if (methodName.startsWith("get"))
		{
			if (methodName.length() < 4)
				return false;
			else
				return Character.isUpperCase(methodName.charAt(3));
		}
		return false;
	}

	/**
	 * Checks if a method is a "setter" method.
	 * This checks its name, if it returns a void value or the source class type (chain setters), takes one argument, and if it is <b>public</b>.
	 * @param method the method to inspect.
	 * @return true if so, false if not.
	 */
	private static boolean isSetter(Method method, Class<?> sourceType)
	{
		Class<?> rettype = method.getReturnType();
		int modifiers = method.getModifiers();
		return isSetterName(method.getName()) 
			&& method.getParameterTypes().length == 1
			&& (rettype == Void.TYPE || rettype == sourceType || rettype == Void.class) 
			&& (modifiers & Modifier.PUBLIC) != 0
			&& (modifiers & Modifier.STATIC) == 0;
	}

	/**
	 * Checks if a method is a "getter" method.
	 * This checks its name, if it returns a non-void value, takes no arguments, and if it is <b>public</b>.
	 * @param method the method to inspect.
	 * @return true if so, false if not.
	 */
	private static boolean isGetter(Method method)
	{
		Class<?> rettype = method.getReturnType();
		int modifiers = method.getModifiers();
		return isGetterName(method.getName()) 
			&& method.getParameterTypes().length == 0
			&& !(rettype == Void.TYPE || rettype == Void.class) 
			&& (modifiers & Modifier.PUBLIC) != 0
			&& (modifiers & Modifier.STATIC) == 0;
	}

	/**
	 * Checks if a method is a visible method.
	 * This checks if it is <b>public</b> but not <b>static</b>, and not a part of Object.
	 * @param method the method to inspect.
	 * @return true if so, false if not.
	 */
	private static boolean isVisibleMethod(Method method)
	{
		int modifiers = method.getModifiers();
		return method.getDeclaringClass() != Object.class
			&& (modifiers & Modifier.PUBLIC) != 0
			&& (modifiers & Modifier.STATIC) == 0;
	}

	// truncator method
	private static String truncateMethodName(String methodName, boolean is)
	{
		return is 
			? Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3)
			: Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
	}

	/**
	 * An interface for figuring out whether to ignore certain fields or methods
	 * on an object that a profile is being generated for (if they are already 
	 * going to pass testing for includable fields).
	 */
	public interface MemberPolicy
	{
		/**
		 * Checks if a field is ignored by the profile generator.
		 * @param field the field to test.
		 * @return true if so, false if not.
		 */
		boolean isIgnored(Field field);
	
		/**
		 * Checks if a method is ignored by the profile generator.
		 * @param method the method to test.
		 * @return true if so, false if not.
		 */
		boolean isIgnored(Method method);
	
		/**
		 * Gets an alias name for the provided field.
		 * This is used for mapping this field to another name, usually for type/object conversions or the like.
		 * @param field the field to use.
		 * @return the name to use, or null for no alias.
		 */
		String getAlias(Field field);
	
		/**
		 * Gets an alias name for the provided method.
		 * This is used for mapping this method to another name, usually for type/object conversions or the like.
		 * @param method the method to use.
		 * @return the name to use, or null for no alias.
		 */
		String getAlias(Method method);
	}

	/**
	 * Type profile for an unknown object that has an ambiguous signature for 
	 * applying values to POJOs and beans.
	 * This only cares about setter methods with one argument and public fields.
	 * @param <T> the class
	 */
	public static class Profile<T>
	{
		/** Map of Public fields by name. */
		private HashMap<String, FieldInfo> publicFieldsByName;
		/** Map of getters by name. */
		private HashMap<String, MethodInfo> getterMethodsByName;
		/** Map of setters by name. */
		private HashMap<String, MethodInfo> setterMethodsByName;

		/** Map of Public fields by alias. */
		private HashMap<String, FieldInfo> publicFieldsByAlias;
		/** Map of getters by alias. */
		private HashMap<String, MethodInfo> getterMethodsByAlias;
		/** Map of setters by alias. */
		private HashMap<String, MethodInfo> setterMethodsByAlias;

		/** Map of methods by alias. */
		private HashMap<String, MethodInfo> methodsByAlias;
		/** Map of methods by name. */
		private HashMap<String, MethodInfo> methodsByName;

		
		// Creates a profile from a class. 
		private Profile(Class<? extends T> inputClass, MemberPolicy policy)
		{
			publicFieldsByName = new HashMap<String, FieldInfo>(4);
			getterMethodsByName = new HashMap<String, MethodInfo>(4);
			setterMethodsByName = new HashMap<String, MethodInfo>(4);
			publicFieldsByAlias = new HashMap<String, FieldInfo>(4);
			getterMethodsByAlias = new HashMap<String, MethodInfo>(4);
			setterMethodsByAlias = new HashMap<String, MethodInfo>(4);
			methodsByAlias = new HashMap<String, MethodInfo>(4);
			methodsByName = new HashMap<String, MethodInfo>(4);

			for (Field f : inputClass.getFields())
			{
				if (Modifier.isStatic(f.getModifiers()))
					continue;
					
				if (!policy.isIgnored(f))
				{
					String alias = policy.getAlias(f);
					if (alias != null)
						alias = alias.toLowerCase();
					FieldInfo fi = new FieldInfo(f.getType(), f, alias);
					publicFieldsByName.put(f.getName().toLowerCase(), fi);
					if (alias != null)
						publicFieldsByAlias.put(alias, fi);
				}
			}
			
			for (Method m : inputClass.getMethods())
			{
				if (policy.isIgnored(m))
					continue;
				
				if (isGetter(m) && !m.getName().equals("getClass"))
				{
					String alias = policy.getAlias(m);
					if (alias != null)
						alias = alias.toLowerCase();
					MethodInfo mi = new MethodInfo(m.getReturnType(), m, alias);
					getterMethodsByName.put(getFieldName(m.getName()).toLowerCase(), mi);
					if (alias != null)
						getterMethodsByAlias.put(alias, mi);
				}
				else if (isSetter(m, inputClass))
				{
					String alias = policy.getAlias(m);
					if (alias != null)
						alias = alias.toLowerCase();
					MethodInfo mi = new MethodInfo(m.getParameterTypes()[0], m, alias);
					setterMethodsByName.put(getFieldName(m.getName()).toLowerCase(), mi);
					if (alias != null)
						setterMethodsByAlias.put(alias, mi);
				}
				else if (isVisibleMethod(m))
				{
					String alias = policy.getAlias(m);
					if (alias != null)
						alias = alias.toLowerCase();
					MethodInfo mi = new MethodInfo(m.getReturnType(), m, alias);
					methodsByName.put(m.getName().toLowerCase(), mi);
					if (alias != null)
						methodsByAlias.put(alias, mi);
				}
			}
		}
		
		/** 
		 * Returns a reference to the map that contains this profile's public fields.
		 * Maps "field name" to {@link FieldInfo} object.
		 * @return the map of field name to field.  
		 */
		public HashMap<String, FieldInfo> getPublicFieldsByName()
		{
			return publicFieldsByName;
		}

		/** 
		 * Returns a reference to the map that contains this profile's getter methods.
		 * Maps "field name" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself.
		 * @return the map of getter name to method.  
		 */
		public HashMap<String, MethodInfo> getGetterMethodsByName()
		{
			return getterMethodsByName;
		}

		/** 
		 * Returns a reference to the map that contains this profile's setter methods.
		 * Maps "field name" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself. 
		 * @return the map of setter name to method.  
		 */
		public HashMap<String, MethodInfo> getSetterMethodsByName()
		{
			return setterMethodsByName;
		}
		
		/** 
		 * Returns a reference to the map that contains this profile's other public, non-static methods.
		 * Maps "method name" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself. 
		 * @return the map of name to method.  
		 */
		public HashMap<String, MethodInfo> getMethodsByName()
		{
			return methodsByName;
		}

		/** 
		 * Returns a reference to the map that contains this profile's public fields.
		 * Maps "field alias" to {@link FieldInfo} object.
		 * @return the map of field alias to field.  
		 */
		public HashMap<String, FieldInfo> getPublicFieldsByAlias()
		{
			return publicFieldsByAlias;
		}

		/** 
		 * Returns a reference to the map that contains this profile's getter methods.
		 * Maps "field alias" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself.
		 * @return the map of getter alias to method.  
		 */
		public HashMap<String, MethodInfo> getGetterMethodsByAlias()
		{
			return getterMethodsByAlias;
		}

		/** 
		 * Returns a reference to the map that contains this profile's setter methods.
		 * Maps "field alias" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself. 
		 * @return the map of setter alias to method.  
		 */
		public HashMap<String, MethodInfo> getSetterMethodsByAlias()
		{
			return setterMethodsByAlias;
		}

		/** 
		 * Returns a reference to the map that contains this profile's other public, non-static methods.
		 * Maps "method alias" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself. 
		 * @return the map of alias to method.
		 */
		public HashMap<String, MethodInfo> getMethodsByAlias()
		{
			return methodsByAlias;
		}
		
		/**
		 * Field information.
		 * Contains the relevant type and getter/setter method.
		 */
		public static class FieldInfo
		{
			/** Object Type. */
			private Class<?> type;
			/** Field reference. */
			private Field field;
			/** Alias, if any. */
			private String alias;

			private FieldInfo(Class<?> type, Field field, String alias)
			{
				this.type = type;
				this.field = field;
				this.alias = alias;
			}

			/**
			 * @return the type that this setter takes as an argument, or this getter returns.
			 */
			public Class<?> getType()
			{
				return type;
			}

			/**
			 * @return the setter method itself.
			 */
			public Field getField()
			{
				return field;
			}
			
			/**
			 * @return the alias for the field info, if any.
			 */
			public String getAlias()
			{
				return alias;
			}
			
		}
		
		/**
		 * Method signature.
		 * Contains the relevant type and getter/setter method.
		 */
		public static class MethodInfo
		{
			/** Object Type. */
			private Class<?> type;
			/** Method reference. */
			private Method method;
			/** Alias, if any. */
			private String alias;

			private MethodInfo(Class<?> type, Method method, String alias)
			{
				this.type = type;
				this.method = method;
				this.alias = alias;
			}

			/**
			 * @return the type that this setter takes as an argument, or this getter returns.
			 */
			public Class<?> getType()
			{
				return type;
			}

			/**
			 * @return the setter method itself.
			 */
			public Method getMethod()
			{
				return method;
			}
			
			/**
			 * @return the alias for the field info, if any.
			 */
			public String getAlias()
			{
				return alias;
			}
			
		}
		
	}
	
}
