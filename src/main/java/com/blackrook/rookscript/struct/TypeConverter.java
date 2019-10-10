/*******************************************************************************
 * Copyright (c) 2019 Black Rook Software
 * 
 * This program and the accompanying materials are made available under 
 * the terms of the MIT License, which accompanies this distribution.
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.blackrook.rookscript.struct.TypeProfileFactory.Profile;

/**
 * Type converter class for converting types to others.
 * @author Matthew Tropiano
 */
class TypeConverter
{
	/** The profile factory to use for caching factory. */
	private TypeProfileFactory profileFactory;
	
	/**
	 * Creates a type profiler.
	 * @param profileFactory the profile factory to use for caching reflection info.
	 */
	TypeConverter(TypeProfileFactory profileFactory)
	{
		this.profileFactory = profileFactory;
	}
	
	/**
	 * Creates a new instance of an object for placement in a POJO or elsewhere.
	 * @param <T> the return object type.
	 * @param object the object to convert to another object
	 * @param targetType the target class type to convert to, if the types differ.
	 * @return a suitable object of type <code>targetType</code>. 
	 * @throws ClassCastException if the incoming type cannot be converted.
	 */
	<T> T createForType(Object object, Class<T> targetType)
	{
		return createForType("source", object, targetType);
	}

	/**
	 * Creates a new instance of an object for placement in a POJO or elsewhere.
	 * @param <T> the return object type.
	 * @param memberName the name of the member that is being converted (for reporting). 
	 * @param object the object to convert to another object
	 * @param targetType the target class type to convert to, if the types differ.
	 * @return a suitable object of type <code>targetType</code>. 
	 * @throws ClassCastException if the incoming type cannot be converted.
	 */
	@SuppressWarnings("unchecked")
	<T> T createForType(String memberName, Object object, Class<T> targetType)
	{
		if (object == null)
		{
			if (targetType == Boolean.TYPE)
				return (T)Boolean.valueOf(false);
			else if (targetType == Byte.TYPE)
				return (T)Byte.valueOf((byte)0x00);
			else if (targetType == Short.TYPE)
				return (T)Short.valueOf((short)0);
			else if (targetType == Integer.TYPE)
				return (T)Integer.valueOf(0);
			else if (targetType == Float.TYPE)
				return (T)Float.valueOf(0f);
			else if (targetType == Long.TYPE)
				return (T)Long.valueOf(0L);
			else if (targetType == Double.TYPE)
				return (T)Double.valueOf(0.0);
			else if (targetType == Character.TYPE)
				return (T)Character.valueOf('\0');
			return null;
		}
		
		if (targetType.isAssignableFrom(object.getClass()))
			return targetType.cast(object);
		else if (Object.class == targetType)
			return targetType.cast(object);
		else if (isArray(object.getClass()))
			return convertArray(memberName, object, targetType);
		else if (object instanceof Map)
		{
			T out = create(targetType);
			for (Map.Entry<?, ?> pair : ((Map<?,?>)object).entrySet())
				applyMemberToObject(String.valueOf(pair.getKey()), pair.getValue(), out);
			return out;
		}
		else if (object instanceof Iterable)
			return convertIterable(memberName, (Iterable<?>)object, targetType);
		else if (object instanceof Enum<?>)
			return convertEnum(memberName, (Enum<?>)object, targetType);
		else if (object instanceof Boolean)
			return convertBoolean(memberName, (Boolean)object, targetType);
		else if (object instanceof Number)
			return convertNumber(memberName, (Number)object, targetType);
		else if (object instanceof Character)
			return convertCharacter(memberName, (Character)object, targetType);
		else if (object instanceof Date)
			return convertDate(memberName, (Date)object, targetType);
		else if (object instanceof String)
			return convertString(memberName, (String)object, targetType);
		
		throw new ClassCastException("Object could not be converted: "+memberName+" is "+object.getClass()+", target is "+targetType);
	}

	/**
	 * Applies an object value to a target object via a "field" name (setter/field).
	 * @param <T> the target object type.
	 * @param name the field/setter name - this can be a member name or an alias.
	 * @param value the value to apply/convert.
	 * @param targetObject the target object to set stuff on.
	 */
	<T> void applyMemberToObject(String name, Object value, T targetObject)
	{
		@SuppressWarnings("unchecked")
		Profile<T> profile = profileFactory.getProfile((Class<T>)targetObject.getClass());
	
		Profile.FieldInfo field = null; 
		Profile.MethodInfo setter = null;
		if ((field = isNull(profile.getPublicFieldsByAlias().get(name), profile.getPublicFieldsByName().get(name))) != null)
		{
			Class<?> type = field.getType();
			setFieldValue(targetObject, field.getField(), createForType(name, value, type));
		}
		else if ((setter = isNull(profile.getSetterMethodsByAlias().get(name), profile.getSetterMethodsByName().get(name))) != null)
		{
			Class<?> type = setter.getType();
			Method method = setter.getMethod();
			invokeBlind(method, targetObject, createForType(name, value, type));
		}			
	}

	/**
	 * Converts a boolean to another type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param b the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	@SuppressWarnings("unchecked")
	protected final <T> T convertBoolean(String memberName, Boolean b, Class<T> targetType)
	{
		if (targetType == Boolean.TYPE)
			return (T)Boolean.valueOf(b);
		else if (targetType == Boolean.class)
			return targetType.cast(b);
		else if (targetType == Byte.TYPE)
			return (T)Byte.valueOf((byte)(b ? 1 : 0));
		else if (targetType == Byte.class)
			return targetType.cast(b ? 1 : 0);
		else if (targetType == Short.TYPE)
			return (T)Short.valueOf((short)(b ? 1 : 0));
		else if (targetType == Short.class)
			return targetType.cast(b ? 1 : 0);
		else if (targetType == Integer.TYPE)
			return (T)Integer.valueOf(b ? 1 : 0);
		else if (targetType == Integer.class)
			return targetType.cast(b ? 1 : 0);
		else if (targetType == Float.TYPE)
			return (T)Float.valueOf(b ? 1f : 0f);
		else if (targetType == Float.class)
			return targetType.cast(b ? 1f : 0f);
		else if (targetType == Long.TYPE)
			return (T)Long.valueOf(b ? 1L : 0L);
		else if (targetType == Long.class)
			return targetType.cast(b ? 1L : 0L);
		else if (targetType == Double.TYPE)
			return (T)Double.valueOf(b ? 1.0 : 0.0);
		else if (targetType == Double.class)
			return targetType.cast(b ? 1.0 : 0.0);
		else if (targetType == Character.TYPE)
			return (T)Character.valueOf(b ? (char)1 : '\0');
		else if (targetType == Character.class)
			return targetType.cast(b ? (char)1 : '\0');
		else if (targetType == String.class)
			return targetType.cast(String.valueOf(b));
		else if (isArray(targetType))
		{
			Class<?> atype = getArrayType(targetType);
			Object out = Array.newInstance(atype, 1);
			Array.set(out, 0, createForType(b, atype));
			return targetType.cast(out);
		}
		
		throw new ClassCastException("Object could not be converted: "+memberName+" is Boolean, target is "+targetType);
	}

	/**
	 * Converts a numeric value to a target type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param n the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	@SuppressWarnings("unchecked")
	protected final <T> T convertNumber(String memberName, Number n, Class<T> targetType)
	{
		if (targetType == Boolean.TYPE)
			return (T)Boolean.valueOf(n.intValue() != 0);
		else if (targetType == Boolean.class)
			return targetType.cast(n.intValue() != 0);
		else if (targetType == Byte.TYPE)
			return (T)Byte.valueOf(n.byteValue());
		else if (targetType == Byte.class)
			return targetType.cast(n.byteValue());
		else if (targetType == Short.TYPE)
			return (T)Short.valueOf(n.shortValue());
		else if (targetType == Short.class)
			return targetType.cast(n.shortValue());
		else if (targetType == Integer.TYPE)
			return (T)Integer.valueOf(n.intValue());
		else if (targetType == Integer.class)
			return targetType.cast(n.intValue());
		else if (targetType == Float.TYPE)
			return (T)Float.valueOf(n.floatValue());
		else if (targetType == Float.class)
			return targetType.cast(n.floatValue());
		else if (targetType == Long.TYPE)
			return (T)Long.valueOf(n.longValue());
		else if (targetType == Long.class)
			return targetType.cast(n.longValue());
		else if (targetType == Date.class)
			return targetType.cast(new Date(n.longValue()));
		else if (targetType == Double.TYPE)
			return (T)Double.valueOf(n.doubleValue());
		else if (targetType == Double.class)
			return targetType.cast(n.doubleValue());
		else if (targetType == Character.TYPE)
			return (T)Character.valueOf((char)(n.shortValue()));
		else if (targetType == Character.class)
			return targetType.cast((char)(n.shortValue()));
		else if (targetType == String.class)
			return targetType.cast(String.valueOf(n));
		else if (isArray(targetType))
		{
			Class<?> atype = getArrayType(targetType);
			Object out = Array.newInstance(atype, 1);
			Array.set(out, 0, createForType(n, atype));
			return targetType.cast(out);
		}
	
		throw new ClassCastException("Object could not be converted: "+memberName+" is numeric, target is "+targetType);
	}

	/**
	 * Converts a character value to a target type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param c the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	@SuppressWarnings("unchecked")
	protected final <T> T convertCharacter(String memberName, Character c, Class<T> targetType)
	{
		char cv = c.charValue();
		
		if (targetType == Character.TYPE)
			return (T)Character.valueOf(cv);
		else if (targetType == Character.class)
			return targetType.cast(cv);
		else if (targetType == Boolean.TYPE)
			return (T)Boolean.valueOf(c != 0);
		else if (targetType == Boolean.class)
			return targetType.cast(c != 0);
		else if (targetType == Byte.TYPE)
			return (T)Byte.valueOf((byte)cv);
		else if (targetType == Byte.class)
			return targetType.cast((byte)cv);
		else if (targetType == Short.TYPE)
			return (T)Short.valueOf((short)cv);
		else if (targetType == Short.class)
			return targetType.cast((short)cv);
		else if (targetType == Integer.TYPE)
			return (T)Integer.valueOf((int)cv);
		else if (targetType == Integer.class)
			return targetType.cast((int)cv);
		else if (targetType == Float.TYPE)
			return (T)Float.valueOf((float)cv);
		else if (targetType == Float.class)
			return targetType.cast((float)cv);
		else if (targetType == Long.TYPE)
			return (T)Long.valueOf((long)cv);
		else if (targetType == Long.class)
			return targetType.cast((long)cv);
		else if (targetType == Double.TYPE)
			return (T)Double.valueOf((double)cv);
		else if (targetType == Double.class)
			return targetType.cast((double)cv);
		else if (targetType == String.class)
			return targetType.cast(String.valueOf(c));
		else if (isArray(targetType))
		{
			Class<?> atype = getArrayType(targetType);
			Object out = Array.newInstance(atype, 1);
			Array.set(out, 0, createForType(c, atype));
			return targetType.cast(out);
		}
	
		throw new ClassCastException("Object could not be converted: "+memberName+" is numeric, target is "+targetType);
	}

	/**
	 * Converts a date value to a target type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param d the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	@SuppressWarnings("unchecked")
	protected final <T> T convertDate(String memberName, Date d, Class<T> targetType)
	{
		if (targetType == Long.TYPE)
			return (T)Long.valueOf(d.getTime());
		else if (targetType == Long.class)
			return targetType.cast(d.getTime());
		else if (targetType == String.class)
			return targetType.cast(String.valueOf(d));
		else if (targetType == Date.class)
			return targetType.cast(new Date(d.getTime()));
		else if (isArray(targetType))
		{
			Class<?> atype = getArrayType(targetType);
			Object out = Array.newInstance(atype, 1);
			Array.set(out, 0, createForType(d, atype));
			return targetType.cast(out);
		}
	
		throw new ClassCastException("Object could not be converted: "+memberName+" is Date, target is "+targetType);
	}

	/**
	 * Converts an enum value to a target type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param e the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected final <T> T convertEnum(String memberName, Enum<?> e, Class<T> targetType)
	{
		if (targetType == Byte.TYPE)
			return (T)Byte.valueOf((byte)e.ordinal());
		else if (targetType == Byte.class)
			return targetType.cast((byte)e.ordinal());
		else if (targetType == Short.TYPE)
			return (T)Short.valueOf((short)e.ordinal());
		else if (targetType == Short.class)
			return targetType.cast((short)e.ordinal());
		else if (targetType == Integer.TYPE)
			return (T)Integer.valueOf(e.ordinal());
		else if (targetType == Integer.class)
			return targetType.cast(e.ordinal());
		else if (targetType == Float.TYPE)
			return (T)Float.valueOf(e.ordinal());
		else if (targetType == Float.class)
			return targetType.cast(e.ordinal());
		else if (targetType == Long.TYPE)
			return (T)Long.valueOf(e.ordinal());
		else if (targetType == Long.class)
			return targetType.cast(e.ordinal());
		else if (targetType == Double.TYPE)
			return (T)Double.valueOf(e.ordinal());
		else if (targetType == Double.class)
			return targetType.cast(e.ordinal());
		else if (targetType == String.class)
			return targetType.cast(e.name());
		else if (targetType.isEnum())
			return targetType.cast(getEnumInstance(e.name(), (Class<Enum>)targetType));
		
		throw new ClassCastException("Object could not be converted: "+memberName+" is Enum, target is "+targetType);
	}

	/**
	 * Converts a string value to a target type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param s the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected final <T> T convertString(String memberName, String s, Class<T> targetType)
	{
		if (targetType == Boolean.TYPE)
			return (T)Boolean.valueOf(parseBoolean(s));
		else if (targetType == Boolean.class)
			return targetType.cast(parseBoolean(s));
		else if (targetType == Byte.TYPE)
			return (T)Byte.valueOf(parseByte(s));
		else if (targetType == Byte.class)
			return targetType.cast(parseByte(s));
		else if (targetType == Short.TYPE)
			return (T)Short.valueOf(parseShort(s));
		else if (targetType == Short.class)
			return targetType.cast(parseShort(s));
		else if (targetType == Integer.TYPE)
			return (T)Integer.valueOf(parseInt(s));
		else if (targetType == Integer.class)
			return targetType.cast(parseInt(s));
		else if (targetType == Float.TYPE)
			return (T)Float.valueOf(parseFloat(s));
		else if (targetType == Float.class)
			return targetType.cast(parseFloat(s));
		else if (targetType == Long.TYPE)
			return (T)Long.valueOf(parseLong(s));
		else if (targetType == Long.class)
			return targetType.cast(parseLong(s));
		else if (targetType == Double.TYPE)
			return (T)Double.valueOf(parseDouble(s));
		else if (targetType == Double.class)
			return targetType.cast(parseDouble(s));
		else if (targetType == Character.TYPE && s.length() == 1)
			return (T)Character.valueOf(s.charAt(0));
		else if (targetType == Character.class && s.length() == 1)
			return targetType.cast(s.charAt(0));
		else if (targetType == String.class)
			return targetType.cast(s);
		else if (targetType.isEnum())
			return targetType.cast(getEnumInstance(s, (Class<Enum>)targetType));
		else if (isArray(targetType))
		{
			if (getArrayType(targetType) == Character.TYPE)
				return targetType.cast(s.toCharArray());
			else if (getArrayType(targetType) == Byte.TYPE)
				return targetType.cast(s.getBytes());
			else if (isArray(targetType))
			{
				Class<?> atype = getArrayType(targetType);
				Object out = Array.newInstance(atype, 1);
				Array.set(out, 0, createForType(s, atype));
				return targetType.cast(out);
			}
		}
		
		throw new ClassCastException("Object could not be converted: "+memberName+" is String, target is "+targetType);
	}

	/**
	 * Converts an array value to a target type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param array the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	protected final <T> T convertArray(String memberName, Object array, Class<T> targetType)
	{
		Class<?> arrayType = getArrayType(array);
		int arrayDimensions = getArrayDimensions(array);
		
		if (arrayDimensions == 1)
		{
			if (arrayType == Character.TYPE)
			{
				return convertCharArray(memberName, (char[])array, targetType);
			}
			else if (arrayType == Character.class)
			{
				Character[] chars = (Character[])array;
				char[] charArray = new char[chars.length];
				for (int i = 0; i < charArray.length; i++)
					charArray[i] = chars[i];
				return convertCharArray(memberName, charArray, targetType);
			}
			else if (arrayType == Byte.TYPE)
			{
				return convertByteArray(memberName, (byte[])array, targetType);
			}
			else if (arrayType == Byte.class)
			{
				Byte[] bytes = (Byte[])array;
				byte[] byteArray = new byte[bytes.length];
				for (int i = 0; i < byteArray.length; i++)
					byteArray[i] = bytes[i];
				return convertByteArray(memberName, byteArray, targetType);
			}
			else
				return convertOtherArray(memberName, array, targetType);
		}
		else
			return convertOtherArray(memberName, array, targetType);
	}

	/**
	 * Converts a char array value to a target type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param charArray the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	protected final <T> T convertCharArray(String memberName, char[] charArray, Class<T> targetType)
	{
		if (isArray(targetType))
		{
			if (getArrayType(targetType) == Character.TYPE)
				return targetType.cast(charArray);
			else if (getArrayType(targetType) == Byte.TYPE)
				return targetType.cast((new String(charArray)).getBytes());
			else
				return convertOtherArray(memberName, charArray, targetType);
		}
		else if (targetType == String.class)
			return targetType.cast(new String(charArray));
		else
			return convertString(memberName, new String(charArray), targetType);
	}

	/**
	 * Converts a byte array value to a target type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param byteArray the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	protected final <T> T convertByteArray(String memberName, byte[] byteArray, Class<T> targetType)
	{
		if (isArray(targetType))
		{
			if (getArrayType(targetType) == Character.TYPE)
				return targetType.cast((new String(byteArray)).toCharArray());
			else if (getArrayType(targetType) == Byte.TYPE)
				return targetType.cast(Arrays.copyOf(byteArray, byteArray.length));
			else
				return convertOtherArray(memberName, byteArray, targetType);
		}
		else if (targetType == String.class)
			return targetType.cast(new String(byteArray));
		else
			return convertOtherArray(memberName, byteArray, targetType);
	}

	/**
	 * Converts a totally different array type.
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param array the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	protected final <T> T convertOtherArray(String memberName, Object array, Class<T> targetType)
	{
		Class<?> atype = getArrayType(targetType);
		if (atype == null)
			throw new ClassCastException("Array cannot be converted; "+memberName+" is array and target is not array typed.");
		
		int alen = Array.getLength(array);
		Object newarray = Array.newInstance(atype, alen);
		for (int i = 0; i < alen; i++)
			Array.set(newarray, i, createForType(String.format("%s[%d]", memberName, i), Array.get(array, i), atype));
			
		return targetType.cast(newarray);
	}

	/**
	 * Converts an iterable to another type (like an array).
	 * @param <T> the target value type.
	 * @param memberName the name of the member being converted (for logging).
	 * @param iter the value to convert.
	 * @param targetType the target type.
	 * @return the resultant type.
	 */
	protected final <T> T convertIterable(String memberName, Iterable<?> iter, Class<T> targetType)
	{
		if (isArray(targetType) && getArrayDimensions(targetType) == 1)
		{
			List<Object> templist = new ArrayList<Object>(64);
			for (Object obj : iter)
				templist.add(obj);
			
			Class<?> atype = getArrayType(targetType);
			int alen = templist.size();
			Object newarray = Array.newInstance(atype, alen);
			for (int i = 0; i < alen; i++)
				Array.set(newarray, i, createForType(String.format("%s, index %d", memberName, i), templist.get(i), atype));
			
			return targetType.cast(newarray);
		}
		else
			throw new ClassCastException("Object could not be converted: "+memberName+" is Iterable, target is "+targetType);
	}

	private static <T> T isNull(T testObject, T nullReturn)
	{
		return testObject != null ? testObject : nullReturn;
	}

	private static boolean parseBoolean(String s)
	{
		if (s == null || !s.equalsIgnoreCase("true"))
			return false;
		else
			return true;
	}

	private static byte parseByte(String s)
	{
		if (s == null)
			return 0;
		try {
			return Byte.parseByte(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static short parseShort(String s)
	{
		if (s == null)
			return 0;
		try {
			return Short.parseShort(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static int parseInt(String s)
	{
		if (s == null)
			return 0;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long parseLong(String s)
	{
		if (s == null)
			return 0L;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static float parseFloat(String s)
	{
		if (s == null)
			return 0f;
		try {
			return Float.parseFloat(s);
		} catch (NumberFormatException e) {
			return 0f;
		}
	}

	private static double parseDouble(String s)
	{
		if (s == null)
			return 0.0;
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private static <T> T create(Class<T> clazz)
	{
		Object out = null;
		try {
			out = construct(clazz.getDeclaredConstructor());
		} catch (NoSuchMethodException ex) {
			throw new RuntimeException(ex);
		} catch (SecurityException ex) {
			throw new RuntimeException(ex);
		}
		
		return clazz.cast(out);
	}

	@SuppressWarnings("unchecked")
	private static <T> T construct(Constructor<T> constructor, Object ... params)
	{
		Object out = null;
		try {
			out = (T)constructor.newInstance(params);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		
		return (T)out;
	}

	private static boolean isArray(Class<?> clazz)
	{
		return clazz.getName().startsWith("["); 
	}

	private static boolean isArray(Object object)
	{
		return isArray(object.getClass()); 
	}

	private static int getArrayDimensions(Class<?> arrayType)
	{
		if (!isArray(arrayType))
			return 0;
			
		String cname = arrayType.getName();
		
		int dims = 0;
		while (dims < cname.length() && cname.charAt(dims) == '[')
			dims++;
		
		if (dims == cname.length())
			return 0;
		
		return dims;
	}

	private static int getArrayDimensions(Object array)
	{
		if (!isArray(array))
			return 0;
			
		return getArrayDimensions(array.getClass());
	}

	private static Class<?> getArrayType(Class<?> arrayType)
	{
		String cname = arrayType.getName();
	
		int typeIndex = getArrayDimensions(arrayType);
		if (typeIndex == 0)
			return null;
		
		char t = cname.charAt(typeIndex);
		if (t == 'L') // is object.
		{
			String classtypename = cname.substring(typeIndex + 1, cname.length() - 1);
			try {
				return Class.forName(classtypename);
			} catch (ClassNotFoundException e){
				return null;
			}
		}
		else switch (t)
		{
			case 'Z': return Boolean.TYPE; 
			case 'B': return Byte.TYPE; 
			case 'S': return Short.TYPE; 
			case 'I': return Integer.TYPE; 
			case 'J': return Long.TYPE; 
			case 'F': return Float.TYPE; 
			case 'D': return Double.TYPE; 
			case 'C': return Character.TYPE; 
		}
		
		return null;
	}

	private static Class<?> getArrayType(Object object)
	{
		if (!isArray(object))
			return null;
		
		return getArrayType(object.getClass());
	}

	private static void setFieldValue(Object instance, Field field, Object value)
	{
		try {
			field.set(instance, value);
		} catch (ClassCastException ex) {
			throw ex;
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static Object invokeBlind(Method method, Object instance, Object ... params)
	{
		Object out = null;
		try {
			out = method.invoke(instance, params);
		} catch (ClassCastException ex) {
			throw ex;
		} catch (InvocationTargetException ex) {
			throw new RuntimeException(ex);
		} catch (IllegalArgumentException ex) {
			throw new RuntimeException(ex);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
		return out;
	}

	private static <T extends Enum<T>> T getEnumInstance(String value, Class<T> enumClass)
	{
		if (value == null)
			return null;
		
		try {
			return Enum.valueOf(enumClass, value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

}
