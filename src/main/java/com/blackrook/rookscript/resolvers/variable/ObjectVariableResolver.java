/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.variable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.annotations.ScriptValueType;
import com.blackrook.rookscript.resolvers.ScriptVariableResolver;
import com.blackrook.rookscript.struct.Utils;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile.FieldInfo;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile.MethodInfo;

/**
 * A variable resolver that wraps an object instance's fields and getters/setters
 * as a scope. The "field names" of the fields/methods are used as the variable names. 
 * @author Matthew Tropiano
 * @param <T> the object type.
 */
public class ObjectVariableResolver<T> implements ScriptVariableResolver
{
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<Object[]> OBJECTARRAY1 = ThreadLocal.withInitial(()->new Object[1]);

	/** Instance. */
	private T instance;
	/** Map of resolvers. */
	private Map<String, GetterSetter> fieldMap;
	
	/**
	 * Creates a new resolver for this object.
	 * @param instance the object instance to wrap.
	 */
	public ObjectVariableResolver(T instance)
	{
		this.instance = instance;
		this.fieldMap = new HashMap<>();
		
		@SuppressWarnings("unchecked")
		Profile<T> tp = (Profile<T>)Utils.getProfile(instance.getClass());
		
		for (Map.Entry<String, FieldInfo> pair : tp.getPublicFieldsByName().entrySet())
		{
			FieldInfo fieldInfo = pair.getValue();
			
			String name;
			if (fieldInfo.getAlias() != null)
				name = fieldInfo.getAlias();
			else
				name = pair.getKey();
			
			ScriptValueType typeAnno;
			Type valueType = null;
			if ((typeAnno = fieldInfo.getField().getAnnotation(ScriptValueType.class)) != null)
				valueType = typeAnno.value();
			
			GetterSetter gs = get(name);
			gs.type = fieldInfo.getType();
			gs.field = fieldInfo.getField();
			gs.valueType = valueType;
		}

		for (Map.Entry<String, MethodInfo> pair : tp.getGetterMethodsByName().entrySet())
		{
			MethodInfo methodInfo = pair.getValue();
			
			String name;
			if (methodInfo.getAlias() != null)
				name = methodInfo.getAlias();
			else
				name = pair.getKey();

			ScriptValueType typeAnno;
			Type valueType = null;
			if ((typeAnno = methodInfo.getMethod().getAnnotation(ScriptValueType.class)) != null)
				valueType = typeAnno.value();
			
			GetterSetter gs = get(name);
			gs.type = methodInfo.getType();
			gs.getter = methodInfo.getMethod();
			gs.valueType = valueType;
		}

		for (Map.Entry<String, MethodInfo> pair : tp.getSetterMethodsByName().entrySet())
		{
			MethodInfo methodInfo = pair.getValue();
			
			String name;
			if (methodInfo.getAlias() != null)
				name = methodInfo.getAlias();
			else
				name = pair.getKey();

			ScriptValueType typeAnno;
			Type valueType = null;
			if ((typeAnno = methodInfo.getMethod().getAnnotation(ScriptValueType.class)) != null)
				valueType = typeAnno.value();
			
			GetterSetter gs = get(name);
			gs.type = methodInfo.getType();
			gs.getter = methodInfo.getMethod();
			gs.valueType = valueType;
		}
	}
	
	/**
	 * @return the wrapped instance.
	 */
	public T getInstance()
	{
		return instance;
	}
	
	private GetterSetter get(String name)
	{
		GetterSetter out;
		if ((out = fieldMap.get(name)) == null)
			fieldMap.put(name, (out = new GetterSetter()));
		return out;
	}
	
	@Override
	public boolean getValue(String name, ScriptValue out)
	{
		ScriptValue sv = CACHEVALUE1.get();
		try 
		{
			GetterSetter gs;
			if ((gs = fieldMap.get(name)) == null)
			{
				out.setNull();
				return false;
			}
			
			if (gs.valueType != null)
				sv.set(gs.valueType, gs.get());
			else
				sv.set(gs.get());
			out.set(sv);
		} 
		finally 
		{
			sv.setNull();
		}
		return true;
	}

	@Override
	public void setValue(String name, ScriptValue value)
	{
		GetterSetter out;
		if ((out = fieldMap.get(name)) == null)
			return;
		
		out.set(value);
	}

	@Override
	public boolean containsValue(String name)
	{
		return fieldMap.containsKey(name);
	}

	@Override
	public boolean isReadOnly(String name)
	{
		GetterSetter gs;
		if ((gs = fieldMap.get(name)) == null)
			return true;
		return gs.field == null && gs.setter == null;
	}

	@Override
	public boolean isEmpty()
	{
		return fieldMap.isEmpty();
	}

	@Override
	public int size()
	{
		return fieldMap.size();
	}

	private class GetterSetter
	{
		private Class<?> type;
		private Field field;
		private Method getter;
		private Method setter;
		private ScriptValue.Type valueType;
		
		void set(ScriptValue value)
		{	
			if (field != null)
			{
				Object[] vbuf = OBJECTARRAY1.get();
				try {
					vbuf[0] = value.createForType(type);
					Utils.setFieldValue(instance, field, vbuf);
				} finally {
					vbuf[0] = null;
				}
			}
			else if (setter != null)
			{
				Object[] vbuf = OBJECTARRAY1.get();
				try {
					vbuf[0] = value.createForType(type);
					Utils.invokeBlind(setter, instance, vbuf);
				} finally {
					vbuf[0] = null;
				}
			}
		}
		
		Object get()
		{
			if (field != null)
				return Utils.getFieldValue(instance, field);
			else if (getter != null)
				return Utils.invokeBlind(getter, instance);
			else
				return null;
		}
	}
}
