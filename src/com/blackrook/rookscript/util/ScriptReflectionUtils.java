package com.blackrook.rookscript.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.TypeProfile;
import com.blackrook.commons.TypeProfile.MethodSignature;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;
import static com.blackrook.rookscript.util.ScriptThreadLocal.getCache;

/**
 * Script reflection utilities.
 * @author Matthew Tropiano
 */
public final class ScriptReflectionUtils
{
	/** Type profiles. */
	private static final HashMap<Class<?>, Profile<?>> REGISTERED_TYPES = new HashMap<Class<?>, Profile<?>>();

	/**
	 * Gets a new or existing type profile for mapping fields to objects.
	 * The fields that are omitted are fields/methods annotated with {@link ScriptIgnore}.
	 * Also pays attention to {@link ScriptName} annotations.
	 * @param <T> the class type.
	 * @param clazz the class to get the profile for.
	 * @return a new or existing type profile.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Profile<T> getProfile(Class<T> clazz)
	{
		Profile<T> out = null;
		if ((out = (Profile<T>)REGISTERED_TYPES.get(clazz)) == null)
		{
			synchronized (REGISTERED_TYPES)
			{
				if ((out = (Profile<T>)REGISTERED_TYPES.get(clazz)) == null)
				{
					out = new Profile<T>(clazz);
					REGISTERED_TYPES.put(clazz, out);
				}
			}
		}
		else
			out = (Profile<T>)REGISTERED_TYPES.get(clazz);
		
		return out;
	}
	
	/**
	 * A profile map of field name to getters and setters.
	 * @param <C> the type that this represents.
	 */
	public static class Profile<C>
	{
		Class<C> type;
		CaseInsensitiveHashMap<Getter<C>> getterMap;
		CaseInsensitiveHashMap<Setter<C>> setterMap;
		
		private Profile(Class<C> type)
		{
			this.type = type;
			this.getterMap = new CaseInsensitiveHashMap<>();
			this.setterMap = new CaseInsensitiveHashMap<>();
		
			TypeProfile<?> tp = TypeProfile.getTypeProfile(type);

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
				
				getterMap.put(name, (i)->Reflect.getFieldValue(field, i));
				setterMap.put(name, new FieldSetter<C>(field));
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

				if (!getterMap.containsKey(name))
					getterMap.put(name, (i)->Reflect.invokeBlind(method, i));
			}

			for (ObjectPair<String, MethodSignature> pair : tp.getSetterMethods())
			{
				Method method = pair.getValue().getMethod();
				Class<?> setType = pair.getValue().getType();
				if (method.getAnnotation(ScriptIgnore.class) != null)
					continue;
				
				String name;
				ScriptName anno;
				if ((anno = method.getAnnotation(ScriptName.class)) != null)
					name = anno.value();
				else
					name = pair.getKey();

				if (!setterMap.containsKey(name))
					setterMap.put(name, new SetterMethod<C>(setType, method));
			}
		}
		
		/**
		 * Applies an instance of an object of this type to a map value.
		 * @param obj the object.
		 * @param value the script value.
		 */
		public void objectToMap(C obj, ScriptValue value)
		{
			for (ObjectPair<String, Getter<C>> pair : getterMap)
				value.mapSet(pair.getKey(), pair.getValue().get(obj));
		}

		/**
		 * Applies a map value to an instance of an object of this type.
		 * @param obj the object.
		 * @param value the script value.
		 */
		public void mapToObject(ScriptValue value, C obj)
		{
			for (ObjectPair<String, Setter<C>> pair : setterMap)
			{
				ScriptValue sv = getCache().temp; 
				if (value.mapGet(pair.getKey(), sv))
					pair.getValue().set(obj, sv);
			}
		}

	}
	
	@FunctionalInterface
	private interface Getter<C>
	{
		Object get(C instance);
	}

	private static abstract class Setter<C>
	{
		protected Class<?> type;
		
		private Setter(Class<?> fieldType)
		{
			this.type = fieldType;
		}
		
		abstract void set(C instance, ScriptValue value);
	}
	
	private static class FieldSetter<C> extends Setter<C>
	{
		private Field field;
		private FieldSetter(Field field)
		{
			super(field.getType());
			this.field = field;
		}
		
		void set(C instance, ScriptValue value)
		{
			Reflect.setField(instance, field, value.createForType(type));
		}
	}

	private static class SetterMethod<C> extends Setter<C>
	{
		private Method method;
		private SetterMethod(Class<?> fieldType, Method method)
		{
			super(fieldType);
			this.method = method;
		}
		
		void set(C instance, ScriptValue value)
		{
			Reflect.invokeBlind(method, instance, value.createForType(type));
		}
	}

}
