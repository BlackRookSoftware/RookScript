package com.blackrook.rookscript.resolvers.variable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.TypeProfile;
import com.blackrook.commons.TypeProfile.MethodSignature;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.util.ThreadUtils;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptVariableResolver;
import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;

/**
 * A variable resolver that wraps an object instance's fields and getters/setters
 * as a scope. The "field names" of the fields/methods are used as the variable names. 
 * @author Matthew Tropiano
 */
public class ObjectVariableResolver<T> implements ScriptVariableResolver
{
	/** Instance. */
	private T instance;
	/** Map of resolvers. */
	private CaseInsensitiveHashMap<GetterSetter> fieldMap;
	
	/**
	 * Creates a new resolver for this object.
	 * @param instance the object instance to wrap.
	 */
	public ObjectVariableResolver(T instance)
	{
		this.instance = instance;
		this.fieldMap = new CaseInsensitiveHashMap<>();
		
		@SuppressWarnings("unchecked")
		TypeProfile<T> tp = (TypeProfile<T>)TypeProfile.getTypeProfile(instance.getClass());
		
		for (ObjectPair<String, Field> pair : tp.getPublicFields())
		{
			final Field field = pair.getValue();
			if (field.getAnnotation(ScriptIgnore.class) != null)
				continue;
			
			String name;
			ScriptName anno;
			if ((anno = field.getAnnotation(ScriptName.class)) != null)
				name = anno.value();
			else
				name = pair.getKey();
			
			GetterSetter gs = get(name);
			gs.type = field.getType();
			gs.field = field;
		}

		for (ObjectPair<String, MethodSignature> pair : tp.getGetterMethods())
		{
			Method method = pair.getValue().getMethod();
			if (method.getAnnotation(ScriptIgnore.class) != null)
				continue;
			
			String name;
			ScriptName anno;
			if ((anno = method.getAnnotation(ScriptName.class)) != null)
				name = anno.value();
			else
				name = pair.getKey();

			GetterSetter gs = get(name);
			gs.type = method.getReturnType();
			gs.getter = method;
		}

		for (ObjectPair<String, MethodSignature> pair : tp.getSetterMethods())
		{
			Method method = pair.getValue().getMethod();
			if (method.getAnnotation(ScriptIgnore.class) != null)
				continue;
			
			String name;
			ScriptName anno;
			if ((anno = method.getAnnotation(ScriptName.class)) != null)
				name = anno.value();
			else
				name = pair.getKey();

			Class<?> setType = pair.getValue().getType();
			GetterSetter gs = get(name);
			gs.type = setType;
			gs.setter = method;
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
	public ScriptValue getValue(String name)
	{
		ScriptValue sv = getCache().temp;

		GetterSetter out;
		if ((out = fieldMap.get(name)) == null)
		{
			sv.setNull();
			return sv;
		}
		sv.set(out.get());
		return sv;
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
		fieldMap.containsKey(name);
		return false;
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
		
		void set(ScriptValue value)
		{
			if (field != null)
				Reflect.setField(instance, field, value.createForType(type));
			else if (setter != null)
				Reflect.invokeBlind(setter, instance, value.createForType(type));
		}
		
		Object get()
		{
			if (field != null)
				return Reflect.getFieldValue(field, instance);
			else if (getter != null)
				return Reflect.invokeBlind(getter, instance);
			else
				return null;
		}
	}
	
	private static final String CACHE_NAME = "$$"+Cache.class.getCanonicalName();

	// Get the cache.
	private static Cache getCache()
	{
		Cache out;
		if ((out = (Cache)ThreadUtils.getLocal(CACHE_NAME)) == null)
			ThreadUtils.setLocal(CACHE_NAME, out = new Cache());
		return out;
	}
	
	// Mathematics cache.
	private static class Cache
	{
		private ScriptValue temp;
		
		public Cache()
		{
			this.temp = ScriptValue.create(null);
		}
		
	}
	
}
