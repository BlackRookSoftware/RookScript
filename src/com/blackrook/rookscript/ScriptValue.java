/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import com.blackrook.commons.Reflect;
import com.blackrook.commons.ResettableIterator;
import com.blackrook.commons.Sizable;
import com.blackrook.commons.list.List;
import com.blackrook.commons.util.ObjectUtils;
import com.blackrook.commons.util.ValueUtils;
import com.blackrook.rookscript.resolvers.variable.AbstractVariableResolver;
import com.blackrook.rookscript.resolvers.variable.AbstractVariableResolver.Entry;
import com.blackrook.rookscript.util.ScriptReflectionUtils;
import com.blackrook.rookscript.util.ScriptReflectionUtils.Profile;
import com.blackrook.rookscript.util.ScriptThreadLocal.Cache;

import static com.blackrook.rookscript.util.ScriptThreadLocal.getCache;

/**
 * Script value encapsulation.
 * @author Matthew Tropiano
 */
public class ScriptValue implements Comparable<ScriptValue>
{
	public static enum Type
	{
		NULL,
		BOOLEAN,
		INTEGER,
		FLOAT,
		STRING,
		OBJECTREF,
		LIST,
		MAP,
		ERROR;
	}
	
	/** Value internal type. */
	private Type type;
	/** Value raw data. */
	private long rawbits;
	/** Object reference data. */
	private Object ref;
	
	// Private constructor.
	private ScriptValue()
	{
		setNull();
	}

	/**
	 * Creates a script value.
	 * NOTE: Objects are turned into maps.
	 * @param value the source value.
	 * @return a new script value.
	 */
	public static ScriptValue create(Object value)
	{
		ScriptValue out = new ScriptValue();
		out.set(value);
		return out;
	}
	
	/**
	 * Creates a script value that is an object reference.
	 * @param value the source value.
	 * @return a new script value.
	 */
	public static ScriptValue createObjectRef(Object value)
	{
		ScriptValue out = new ScriptValue();
		out.setObjectRef(value);
		return out;
	}
	
	/**
	 * Creates a script value that is an empty list.
	 * @return a new script value.
	 */
	public static ScriptValue createEmptyList()
	{
		ScriptValue out = new ScriptValue();
		out.setEmptyList();
		return out;
	}
	
	/**
	 * Creates a script value that is an empty map.
	 * @return a new script value.
	 */
	public static ScriptValue createEmptyMap()
	{
		ScriptValue out = new ScriptValue();
		out.setEmptyMap();
		return out;
	}
	
	/**
	 * Creates an error value from a Throwable.
	 * Copies the simple class name, the message, and the localized message.
	 * @param t the Throwable to use.
	 * @return a new script value.
	 * @see #createError(String, String, String)
	 * @see Throwable#getClass()
	 * @see Class#getSimpleName()
	 * @see Throwable#getMessage()
	 * @see Throwable#getLocalizedMessage()
	 */
	public static ScriptValue createError(Throwable t)
	{
		ScriptValue sv = new ScriptValue();
		sv.set(t);
		return sv;
	}
	
	/**
	 * Creates an error value.
	 * @param type the error type.
	 * @param message the error message (will be same as localized).
	 * @return a new script value.
	 */
	public static ScriptValue createError(String type, String message)
	{
		return createError(type, message, message);
	}
	
	/**
	 * Creates an error value.
	 * @param type the error type.
	 * @param message the error message.
	 * @param localizedMessage a localized version of the error message.
	 * @return a new script value.
	 */
	public static ScriptValue createError(String type, String message, String localizedMessage)
	{
		ScriptValue sv = new ScriptValue();
		sv.set(ErrorType.create(type, message, localizedMessage));
		return sv;
	}
	
	/**
	 * Creates a copy of this value.
	 * The copy process is DEEP - lists and maps are copied as well, except for native objects
	 * (the references are copied, but not what they point to).
	 * @return a new ScriptValue.
	 */
	public ScriptValue copy()
	{
		if (isList())
		{
			@SuppressWarnings("unchecked")
			List<ScriptValue> list = (List<ScriptValue>)this.ref;
			ScriptValue[] array = new ScriptValue[list.size()];
			for (int i = 0; i < array.length; i++)
				array[i] = list.getByIndex(i).copy();
			return create(array);
		}
		else if (isMap())
		{
			MapType scope = (MapType)this.ref;
			ScriptValue copy = createEmptyMap();
			for (Entry e : scope)
				copy.mapSet(e.getName(), create(e.getValue().copy()));
			return copy;
		}
		else
			return create(this);
	}

	/**
	 * Sets this value to the null value.
	 */
	public void setNull()
	{
		this.type = Type.NULL;
		this.ref = null;
		this.rawbits = 0L;
	}
	
	/**
	 * Sets this value to a new empty list (new reference).
	 */
	public void setEmptyList()
	{
		this.type = Type.LIST;
		this.ref = new List<ScriptValue>();
		this.rawbits = 0L;
	}
	
	/**
	 * Sets this value to a new empty map (new reference).
	 */
	public void setEmptyMap()
	{
		this.type = Type.MAP;
		this.ref = new MapType(4);
		this.rawbits = 0L;
	}
	
	/**
	 * Explicitly sets this value as an object reference.
	 * If null, this is set to the null value.
	 * @param value the source value to use.
	 */
	public void setObjectRef(Object value)
	{
		if (value == null)
			setNull();
		else
		{
			this.type = Type.OBJECTREF;
			this.ref = value;
			this.rawbits = 0L;
		}
	}

	/**
	 * Sets this value as an error.
	 * If null, this is set to the null value.
	 * @param value the source error to use.
	 */
	public void set(ErrorType value)
	{
		if (value == null)
			setNull();
		else
		{
			this.type = Type.ERROR;
			this.ref = value;
			this.rawbits = 0L;
		}
	}
	
	/**
	 * Sets this value as an error.
	 * If null, this is set to the null value.
	 * @param value the source error to use.
	 */
	public void set(Throwable value)
	{
		if (value == null)
			setNull();
		else
		{
			this.type = Type.ERROR;
			this.ref = ErrorType.create(value);
			this.rawbits = 0L;
		}
	}
	
	/**
	 * Sets this value using another value.
	 * If null, this is set to the null value.
	 * @param value the source value to use.
	 */
	public void set(Object value)
	{
		if (value == null)
			setNull();
		else if (value instanceof ScriptValue)
			set((ScriptValue)value);
		else if (value instanceof ErrorType)
			set((ErrorType)value);
		else if (value instanceof Throwable)
			set((Throwable)value);
		else if (value instanceof Boolean)
			set((boolean)value);
		else if (value instanceof Byte)
			set((byte)value);
		else if (value instanceof Short)
			set((short)value);
		else if (value instanceof Character)
			set((char)value);
		else if (value instanceof Integer)
			set((int)value);
		else if (value instanceof Long)
			set((long)value);
		else if (value instanceof Float)
			set((float)value);
		else if (value instanceof Double)
			set((double)value);
		else if (value instanceof String)
			set((String)value);
		else
		{
			Class<?> clazz = value.getClass();
			if (clazz.isArray())
			{
				int len = Array.getLength(value);
				List<ScriptValue> list = new List<>(len); 
				this.type = Type.LIST;
				this.ref = list;
				this.rawbits = 0L;
				for (int i = 0; i < len; i++)
					list.add(create(Array.get(value, i)));
			}
			else
			{
				this.type = Type.MAP;
				this.ref = new MapType(4);
				this.rawbits = 0L;
				mapExtract(value);
			}
		}
	}

	/**
	 * Sets this value using another value.
	 * If null, this is set to the null value.
	 * @param value the source value to use.
	 */
	public void set(ScriptValue value)
	{
		if (value == null)
		{
			setNull();
		}
		else
		{
			this.type = value.type;
			this.ref = value.ref;
			this.rawbits = value.rawbits;
		}
	}

	/**
	 * Sets this value using another value.
	 * @param value the source value to use.
	 */
	public void set(boolean value)
	{
		this.type = Type.BOOLEAN;
		this.ref = null;
		this.rawbits = value ? 1L : 0L;
	}

	/**
	 * Sets this value using another value.
	 * @param value the source value to use.
	 */
	public void set(byte value)
	{
		this.type = Type.INTEGER;
		this.ref = null;
		this.rawbits = (long)value;
	}

	/**
	 * Sets this value using another value.
	 * @param value the source value to use.
	 */
	public void set(short value)
	{
		this.type = Type.INTEGER;
		this.ref = null;
		this.rawbits = (long)value;
	}

	/**
	 * Sets this value using another value.
	 * @param value the source value to use.
	 */
	public void set(char value)
	{
		this.type = Type.INTEGER;
		this.ref = null;
		this.rawbits = (long)value & 0x00ffff;
	}

	/**
	 * Sets this value using another value.
	 * @param value the source value to use.
	 */
	public void set(int value)
	{
		this.type = Type.INTEGER;
		this.ref = null;
		this.rawbits = (long)value;
	}

	/**
	 * Sets this value using another value.
	 * @param value the source value to use.
	 */
	public void set(long value)
	{
		this.type = Type.INTEGER;
		this.ref = null;
		this.rawbits = value;
	}

	/**
	 * Sets this value using another value.
	 * @param value the source value to use.
	 */
	public void set(float value)
	{
		this.type = Type.FLOAT;
		this.ref = null;
		this.rawbits = Double.doubleToRawLongBits((double)value);
	}

	/**
	 * Sets this value using another value.
	 * @param value the source value to use.
	 */
	public void set(double value)
	{
		this.type = Type.FLOAT;
		this.ref = null;
		this.rawbits = Double.doubleToRawLongBits(value);
	}

	/**
	 * Sets this value using another value.
	 * @param value the source value to use.
	 */
	public void set(String value)
	{
		if (value == null)
		{
			setNull();
		}
		else
		{
			this.type = Type.STRING;
			this.ref = value;
			this.rawbits = 0L;
		}
	}
	
	/**
	 * Gets the length of this value, if this is an array, or the underlying object
	 * is a {@link Sizable} or {@link Collection} type.
	 * @return the length in values, or 1 if not a list.
	 */
	public int length()
	{
		if (ref != null)
		{
			if (ref instanceof Sizable)
				return ((Sizable)ref).size();
			else if (ref instanceof Collection)
				return ((Collection<?>)ref).size();
		}
		return 1;
	}
	
	/**
	 * Gets if this value is considered "empty".
	 * <p>
	 * Null is empty.<br>
	 * If boolean, false is empty.<br>
	 * If integer, 0 is empty.<br>
	 * If float, 0.0 or NaN.<br>
	 * If string, trimmed and zero length.<br>
	 * If list or map, 0 keys or 0 items is empty.<br>
	 * @return true if so, false if not.
	 */
	public boolean empty()
	{
		switch (type)
		{
			case NULL:
				return true;
			case BOOLEAN:
				return rawbits == 0L;
			case INTEGER:
				return rawbits == 0L;
			case FLOAT:
				return isNaN() || Double.longBitsToDouble(rawbits) == 0.0;
			default:
			case STRING:
			case LIST:
			case MAP:
			case OBJECTREF:
				return ObjectUtils.isEmpty(ref);
		}
	}
	
	/**
	 * Sets a value in this list.
	 * If the index is outside of the range of the list's indices, it is not added.
	 * @param index the list index to set.
	 * @param value the value to add (converted to internal value).
	 * @return true if set, false if not.
	 * @see #isList()
	 */
	public boolean listSetByIndex(int index, Object value)
	{
		if (!isList())
			return false;

		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		if (index < 0 || index >= list.size())
			return false;
		list.getByIndex(index).set(value);
		return true;
	}
	
	/**
	 * Adds a value to this value, if it is a list.
	 * @param value the value to add (converted to internal value).
	 * @return true if added, false if not.
	 * @see #isList()
	 */
	public boolean listAdd(Object value)
	{
		if (!isList())
			return false;

		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		list.add(create(value));
		return true;
	}
	
	/**
	 * Adds a value to this value, only if it is a list.
	 * @param index the index to add the value to.
	 * @param value the value to add (converted to internal value).
	 * @return true if added, false if not.
	 * @see #isList()
	 */
	public boolean listAddAt(int index, Object value)
	{
		if (!isList())
			return false;
		
		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		list.add(index, create(value));
		return true;
	}
	
	/**
	 * Removes a value from this value, if it is a list.
	 * @param value the value to remove (converted to internal value).
	 * @return true if removed, false if not.
	 * @see #isList()
	 */
	public boolean listRemove(Object value)
	{
		if (!isList())
			return false;

		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		Cache cache = getCache();
		cache.value1.set(value);
		return list.remove(cache.value1);
	}
	
	/**
	 * Removes a value from this value at an index, if it is a list.
	 * @param index the index to remove.
	 * @return the value removed if removed, null if not a list nor set.
	 * @see #isList()
	 */
	public ScriptValue listRemoveAt(int index)
	{
		if (!isList())
			return null;
	
		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		return list.removeIndex(index);
	}

	/**
	 * Gets a value at an index, if it is a list.
	 * NOTE: This returns a reference, not a new instance!
	 * @param index the list index to return.
	 * @return the value at the index, or null if not a list nor a valid index.
	 * @see #isList()
	 */
	@SuppressWarnings("unchecked")
	public ScriptValue listGetByIndex(int index)
	{
		if (!isList())
			return null;
	
		return ((List<ScriptValue>)ref).getByIndex(index);
	}

	/**
	 * Gets the index that a value is found at, if it is a list.
	 * @param value the value to look for.
	 * @return the index found, or -1 if not found or not a list.
	 * @see #isList()
	 */
	public int listGetIndexOf(Object value)
	{
		if (!isList())
			return -1;
	
		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		Cache cache = getCache();
		cache.value1.set(value);
		return list.getIndexOf(cache.value1);
	}

	/**
	 * Gets if this list contains an object, if it is a list.
	 * @param value the value to look for.
	 * @return true if found, false if not or not a list.
	 * @see #isList()
	 */
	public boolean listContains(Object value)
	{
		if (!isList())
			return false;
		else
			return listGetIndexOf(value) >= 0;
	}

	/**
	 * Sorts the contents of this list.
	 * Does nothing if this is not a list.
	 */
	@SuppressWarnings("unchecked")
	public void sort()
	{
		if (!isList())
			return;
	
		((List<ScriptValue>)ref).sort();
	}

	/**
	 * Adds a value to this value, if it is a list, 
	 * treating the structure like a set of discrete items.
	 * This assumes that the list is sorted - if not, this will have undefined behavior.
	 * @param value the value to add (converted to internal value).
	 * @return true if added, false if not.
	 * @see #isList()
	 */
	public boolean setAdd(Object value)
	{
		if (!isList())
			return false;
	
		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		Cache cache = getCache();
		cache.value1.set(value);
		if (list.search(cache.value1, Comparator.naturalOrder()) < 0)
		{
			list.add(create(value));
			list.sort(0, list.size());
		}
		return true;
	}

	/**
	 * Removes a value from this value, if it is a list, 
	 * treating the structure like a set of discrete items.
	 * This assumes that the list is sorted - if not, this will have undefined behavior.
	 * @param value the value to remove (converted to internal value).
	 * @return true if removed, false if not.
	 * @see #isList()
	 */
	public boolean setRemove(Object value)
	{
		if (!isList())
			return false;

		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		Cache cache = getCache();
		cache.value1.set(value);
		int index = list.search(cache.value1, Comparator.naturalOrder());
		if (index >= 0)
		{
			list.removeIndex(index);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Gets if this list contains an object, if it is a list, 
	 * treating the structure like a set of discrete items.
	 * This assumes that the list is sorted - if not, this will have undefined behavior.
	 * This has better performance than {@link #listContains(Object)}, but only if this is a sorted set.
	 * @param value the value to look for.
	 * @return true if found, false if not or not a list.
	 * @see #isList()
	 */
	public boolean setContains(Object value)
	{
		if (!isList())
			return false;

		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		Cache cache = getCache();
		cache.value1.set(value);
		return list.search(cache.value1, Comparator.naturalOrder()) >= 0;
	}

	/**
	 * Gets a value's index, if it is a list, 
	 * treating the structure like a set of discrete items.
	 * This assumes that the list is sorted - if not, this will have undefined behavior.
	 * This has better performance than {@link #listGetIndexOf(Object)}, but only if this is a sorted set.
	 * @param value the value to look for.
	 * @return true if found, false if not or not a list.
	 * @see #isList()
	 */
	public int setSearch(Object value)
	{
		if (!isList())
			return -1;

		@SuppressWarnings("unchecked")
		List<ScriptValue> list = (List<ScriptValue>)ref;
		Cache cache = getCache();
		cache.value1.set(value);
		return list.search(cache.value1, Comparator.naturalOrder());
	}

	/**
	 * If this is a map, sets a key on it to a value.
	 * @param key the key. 
	 * @param value the associated value.
	 * @return true if this is a map and the value was assigned, false otherwise.
	 * @see #isMap()
	 */
	public boolean mapSet(String key, Object value)
	{
		if (!isMap())
			return false;
		
		MapType map = (MapType)ref;
		Cache cache = getCache();
		cache.value1.set(value);
		map.setValue(key, cache.value1);
		return true;
	}
	
	/**
	 * If this is a map, gets the value that corresponds to a provided key.
	 * Changing the returned value does not change the value, unless it is a reference type
	 * like a map or list.
	 * @param key the key. 
	 * @param out the destination variable for the value.
	 * @return true if a corresponding value was replaced, false if not or this is not a map. If false, out is set to the null value.
	 * @see #isMap()
	 */
	public boolean mapGet(String key, ScriptValue out)
	{
		if (!isMap())
		{
			out.setNull();
			return false;
		}
		
		MapType map = (MapType)ref;
		return map.getValue(key, out);
	}
	
	/**
	 * If this is a map, removes the value that corresponds to a provided key.
	 * @param key the key. 
	 * @return true if the value existed and was removed, false otherwise.
	 * @see #isMap()
	 */
	public boolean mapRemove(String key)
	{
		if (!isMap())
			return false;
		
		MapType map = (MapType)ref;
		return map.clearValue(key);
	}
	
	/**
	 * Extracts an object's fields/getters and sets those values to this map.
	 * This can be expensive, depending on what needs converting. 
	 * <p>If you are passing an object to a script repeatedly, it may be better to just pass it
	 * as an object reference, if there are associated host functions that manipulate it.
	 * @param object the source object.
	 * @return true if this is a map and extraction was successful.
	 */
	public <T> boolean mapExtract(T object)
	{
		if (!isMap())
			return false;
	
		@SuppressWarnings("unchecked")
		Profile<T> profile = ScriptReflectionUtils.getProfile((Class<T>)object.getClass());
		profile.objectToMap(object, this);
		return true;
	}

	/**
	 * Applies this map to an object's fields/setters.
	 * This can be expensive, depending on what needs converting. 
	 * @param object the source object.
	 * @return true if this is a map and application was successful.
	 */
	public <T> boolean mapApply(T object)
	{
		if (!isMap())
			return false;
	
		@SuppressWarnings("unchecked")
		Profile<T> profile = ScriptReflectionUtils.getProfile((Class<T>)object.getClass());
		profile.mapToObject(this, object);
		return true;
	}

	/**
	 * @return true if this value is a raw type (rawbits only, no object reference).
	 */
	private boolean isRaw()
	{
		return type == Type.BOOLEAN || type == Type.INTEGER || type == Type.FLOAT;
	}

	/**
	 * @return true if this value is an addable type.
	 */
	private boolean isAddableType()
	{
		switch (type)
		{
			default:
				return false;
			case BOOLEAN:
			case INTEGER:
			case FLOAT:
			case STRING:
				return true;
		}
	}

	/**
	 * @return true if this value is null.
	 */
	public boolean isNull()
	{
		return type == Type.NULL && ref == null;
	}

	/**
	 * @return true if this value is strictly NaN.
	 */
	public boolean isNaN()
	{
		return type == Type.FLOAT && Double.isNaN(Double.longBitsToDouble(rawbits));
	}
	
	/**
	 * @return true if this value is positive or negative infinity.
	 */
	public boolean isInfinite()
	{
		return type == Type.FLOAT && Double.isInfinite(Double.longBitsToDouble(rawbits));
	}
	
	/**
	 * @return true if this value is a numeric type.
	 */
	public boolean isNumeric()
	{
		return type == Type.INTEGER || type == Type.FLOAT;
	}

	/**
	 * @return true if this value is a string type.
	 */
	public boolean isString()
	{
		return type == Type.STRING;
	}

	/**
	 * @return true if this value is a list type.
	 */
	public boolean isList()
	{
		return type == Type.LIST;
	}

	/**
	 * @return true if this value is a map type.
	 */
	public boolean isMap()
	{
		return type == Type.MAP;
	}
	
	/**
	 * @return true if this value is an error type.
	 */
	public boolean isError()
	{
		return type == Type.ERROR;
	}
	
	/**
	 * @return true if this value is an object reference type.
	 */
	public boolean isObjectRef()
	{
		return type == Type.OBJECTREF;
	}
	
	/**
	 * Gets this value as a boolean.
	 * @return true if the value is nonzero and not NaN, false otherwise.
	 */
	public boolean asBoolean()
	{
		if (isNull())
			return false;
		switch (type)
		{
			default:
				return false;
			case BOOLEAN:
			case INTEGER:
				return rawbits != 0L; 
			case FLOAT:
				return !isNaN() && Double.longBitsToDouble(rawbits) != 0.0; 
			case STRING:
			case OBJECTREF:
				return !ObjectUtils.isEmpty(ref); 
		}
	}

	/**
	 * Gets this value as a byte.
	 * Depending on the internal value, this may end up truncating data.
	 * <pre>(byte)asLong()</pre>
	 * @return the byte value of this value.
	 */
	public byte asByte()
	{
		return (byte)asLong();
	}

	/**
	 * Gets this value as a short.
	 * Depending on the internal value, this may end up truncating data.
	 * <pre>(short)asLong()</pre>
	 * @return the byte value of this value.
	 */
	public short asShort()
	{
		return (short)asLong();
	}

	/**
	 * Gets this value as a char.
	 * Depending on the internal value, this may end up truncating data.
	 * <pre>(short)asLong()</pre>
	 * @return the byte value of this value.
	 */
	public char asChar()
	{
		return (char)asLong();
	}

	/**
	 * Gets this value as an integer.
	 * Depending on the internal value, this may end up truncating data.
	 * <pre>(int)asLong()</pre>
	 * @return the byte value of this value.
	 */
	public int asInt()
	{
		return (int)asLong();
	}

	/**
	 * Gets this value as a float.
	 * Depending on the internal value, this may end up truncating data.
	 * <pre>isNaN() ? Float.NaN : (float)asDouble()</pre>
	 * @return the byte value of this value.
	 */
	public float asFloat()
	{
		return isNaN() ? Float.NaN : (float)asDouble();
	}

	/**
	 * Gets this value as a long integer.
	 * If this is a boolean type, this returns <code>-1L</code>.
	 * If this is a double type, this is cast to a long.
	 * If this is null (see {@link #isNull()}), this returns <code>0</code>. 
	 * @return the long value of this value.
	 */
	public long asLong()
	{
		if (isNull())
			return 0L;
		switch (type)
		{
			default:
				return 0L;
			case BOOLEAN:
				return asBoolean() ? -1L : 0L; 
			case INTEGER:
				return rawbits; 
			case FLOAT:
				return (long)asDouble(); 
			case STRING:
				return ValueUtils.parseLong((String)ref, 0L); 
		}
	}

	/**
	 * Gets this value as a double-precision float.
	 * If this is a boolean type, this returns <code>1.0</code>.
	 * If this is a long type, this is cast to a double.
	 * If this is null (see {@link #isNull()}), this returns <code>0.0</code>. 
	 * If anything else, this returns {@link Double#NaN}.
	 * @return the double value of this value.
	 */
	public double asDouble()
	{
		if (isNull())
			return 0.0;
		switch (type)
		{
			default:
				return Double.NaN;
			case BOOLEAN:
				return asBoolean() ? 1.0 : 0.0; 
			case INTEGER:
				return (double)rawbits; 
			case FLOAT:
				return Double.longBitsToDouble(rawbits); 
			case STRING:
				return ValueUtils.parseDouble((String)ref, Double.NaN); 
		}
	}
	
	/**
	 * Gets this value as a string.
	 * If this is null (see {@link #isNull()}), this returns <code>"null"</code>. 
	 * @return the string value of this value.
	 */
	public String asString()
	{
		if (isNull())
			return "null";
		switch (type)
		{
			default:
				return String.valueOf(ref);
			case OBJECTREF:
				return Reflect.isArray(ref) ? Arrays.toString((Object[])ref) : String.valueOf(ref);
			case ERROR:
			{
				ErrorType err = (ErrorType)ref;
				return err.getType() + ": " + err.getLocalizedMessage();
			}
			case BOOLEAN:
				return String.valueOf(asBoolean()); 
			case INTEGER:
				return String.valueOf(asLong()); 
			case FLOAT:
				return String.valueOf(asDouble()); 
			case STRING:
				return (String)ref; 
		}
	}
	
	/**
	 * Gets this value as an object.
	 * @return the object representation of this value.
	 */
	public Object asObject()
	{
		switch (type)
		{
			default:
				return ref;
			case BOOLEAN:
				return asBoolean(); 
			case INTEGER:
				return asLong(); 
			case FLOAT:
				return asDouble(); 
		}
	}
	
	/**
	 * Gets this value cast as a different object type.
	 * Does no conversion nor coersion.
	 * @param targetType the class type to cast to.
	 * @param <T> the returned type.
	 * @return the object representation of this value.
	 */
	public <T> T asObjectType(Class<T> targetType)
	{
		switch (type)
		{
			default:
				return targetType.cast(ref);
			case BOOLEAN:
				return targetType.cast(Boolean.valueOf(asBoolean())); 
			case INTEGER:
				return targetType.cast(Long.valueOf(asLong())); 
			case FLOAT:
				return targetType.cast(Double.valueOf(asDouble())); 
		}
	}
	
	/**
	 * Checks if this script value can be cast to the target type.
	 * If this is null (see {@link #isNull()}), this returns <code>false</code>. 
	 * @param targetType the type to test.
	 * @return if the underlying object can be cast to the target type.
	 */
	public boolean isObjectType(Class<?> targetType)
	{
		if (isNull())
			return false;
		switch (type)
		{
			default:
				return targetType.isAssignableFrom(ref.getClass());
			case BOOLEAN:
				return targetType.isAssignableFrom(Boolean.class);
			case INTEGER:
				return targetType.isAssignableFrom(Long.class);
			case FLOAT:
				return targetType.isAssignableFrom(Double.class);
		}
	}

	/**
	 * Checks if this script value is both an array and a particular type.
	 * @param targetType the type to test.
	 * @return if the underlying object can be cast to an array of the target type.
	 */
	public boolean isObjectArrayType(Class<?> targetType)
	{
		switch (type)
		{
			case OBJECTREF:
			{
				Class<?> clazz = ref.getClass();
				return Reflect.isArray(clazz) && targetType.isAssignableFrom(Reflect.getArrayType(clazz));
			}
			default:
				return false;
		}
	}
	
	/**
	 * Gets the type name of this value.
	 * @return the type name.
	 */
	public String getTypeName()
	{
		if (isNull())
		{
			return "null";
		}
		else switch (type)
		{
			default:
			case OBJECTREF:
				if (Reflect.isArray(ref))
					return "objectref:array:"+Reflect.getArrayType(ref).getSimpleName();
				else
					return "objectref:"+ref.getClass().getSimpleName();
			case BOOLEAN:
				return "boolean";
			case FLOAT:
				return "float";
			case INTEGER:
				return "integer";
			case STRING:
				return "string";
			case LIST:
				return "list";
			case MAP:
				return "map";
			case ERROR:
				return "error";
		}
	}
	
	/**
	 * Converts this value to another value.
	 * @param newType the new type to convert to.
	 * @throws IllegalArgumentException if newType is null.
	 */
	public void convertTo(Type newType)
	{
		switch (type)
		{
			default:
				throw new IllegalArgumentException("Cannot convert current type "+type);
				
			case NULL:
			{
				switch (newType)
				{
					default:
						throw new IllegalArgumentException("Cannot convert "+type+" to type "+newType);
					case NULL:
						return;
					case BOOLEAN:
						set(asBoolean());
						return;
					case INTEGER:
						set(asLong());
						return;
					case FLOAT:
						set(asFloat());
						return;
					case STRING:
						set(asString());
						return;
				}
			}

			case BOOLEAN:
			{
				switch (newType)
				{
					default:
						throw new IllegalArgumentException("Cannot convert "+type+" to type "+newType);
					case BOOLEAN:
						return;
					case INTEGER:
						set(asLong());
						return;
					case FLOAT:
						set(asBoolean() ? 1.0 : 0.0);
						return;
					case STRING:
						set(asString());
						return;
				}
			}
			
			case INTEGER:
			{
				switch (newType)
				{
					default:
						throw new IllegalArgumentException("Cannot convert "+type+" to type "+newType);
					case INTEGER:
						return;
					case BOOLEAN:
						set(asBoolean());
						return;
					case FLOAT:
						set(asDouble());
						return;
					case STRING:
						set(asString());
						return;
				}
			}
			
			case FLOAT:
			{
				switch (newType)
				{
					default:
						throw new IllegalArgumentException("Cannot convert "+type+" to type "+newType);
					case FLOAT:
						return;
					case BOOLEAN:
						set(asBoolean());
						return;
					case INTEGER:
						set(asLong());
						return;
					case STRING:
						set(asString());
						return;
				}
			}
			
			case STRING:
			{
				switch (newType)
				{
					default:
						throw new IllegalArgumentException("Cannot convert "+type+" to type "+newType);
					case STRING:
						return;
					case BOOLEAN:
						set(asBoolean());
						return;
					case INTEGER:
						set(asLong());
						return;
					case FLOAT:
						set(asDouble());
						return;
				}
			}
		}
	}
	
	/**
	 * Gets this object coerced or converted to another class type.
	 * Not to be confused with {@link #asObjectType(Class)}, which just recasts.
	 * If this is a map, this applies its fields to the new object's setter methods and fields.
	 * @param targetType the target class type to convert to.
	 * @param <T> the returned type.
	 * @return a suitable object of type <code>targetType</code>. 
	 * @throws ClassCastException if the incoming type cannot be converted.
	 * @see Reflect#createForType(Object, Class)
	 */
	public <T> T createForType(Class<T> targetType)
	{
		switch(type)
		{
			case NULL:
				return (T)null;
			case BOOLEAN:
				return Reflect.createForType(asBoolean(), targetType);
			case INTEGER:
				return Reflect.createForType(asLong(), targetType);
			case FLOAT:
				return Reflect.createForType(asDouble(), targetType);
			case STRING:
				return Reflect.createForType(asString(), targetType);
			case LIST:
				return Reflect.createForType(asObjectType(List.class), targetType);
			case MAP:
				T out = Reflect.create(targetType);
				mapApply(out);
				return out;
			default:
			case ERROR:
			case OBJECTREF:
				return Reflect.createForType(asObject(), targetType);
		}
	}
	
	/**
	 * @return an iterator of the list, or null if not a list.
	 */
	@SuppressWarnings("unchecked")
	public ResettableIterator<ScriptValue> listIterator()
	{
		if (!isList())
			return null;
		return ((List<ScriptValue>)ref).iterator();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ScriptValue)
			return this.equals((ScriptValue)obj);
		return super.equals(obj);
	}
	
	/**
	 * @param value the other value.
	 * @return true if this value is STRICTLY EQUAL to another.
	 */
	public boolean equals(ScriptValue value)
	{
		if (this.type != value.type)
			return false;
		
		if (this.isRaw())
			return this.rawbits == value.rawbits;
		else if (this.ref == null)
			return value.ref == null;
		else
			return this.ref.equals(value.ref);
	}
	
	@Override
	public int compareTo(ScriptValue o)
	{
		if (type == Type.NULL)
			return o.type != Type.NULL ? -1 : 0;
		else if (o.type == Type.NULL)
			return 1;
		else if (type == Type.OBJECTREF || o.type == Type.OBJECTREF)
			return ref.equals(o.ref) ? 0 : -1;
		else if (type == Type.LIST || o.type == Type.LIST)
			return ref == o.ref ? 0 : -1;
		else if (type == Type.STRING || o.type == Type.STRING)
			return asString().compareTo(o.asString());
		else
		{
			double d1 = asDouble();
			double d2 = o.asDouble();
			return d1 == d2 ? 0 : (d1 < d2 ? -1 : 1);
		}
	}

	@Override
	public String toString()
	{
		switch (type)
		{
			case BOOLEAN:
				return String.valueOf(asBoolean());
			case INTEGER:
				return String.valueOf(asLong());
			case FLOAT:
				return String.valueOf(asDouble());
			case STRING:
				return String.valueOf("\""+ref+"\"");
			case NULL:
				return "null";
			default:
			case OBJECTREF:
				if (Reflect.isArray(ref))
					return Arrays.toString((Object[])ref);
				else
					return String.valueOf(ref);
		}
	}
	
	/**
	 * @return string representation of this value suitable for debugging.
	 */
	public String toDebugString()
	{
		return String.format("%s: %s 0x%016x", type.name(), toString(), rawbits); 
	}
	
	/**
	 * Bitwise not calculation.
	 * @param operand the input value.
	 * @param out the output value.
	 */
	public static void not(ScriptValue operand, ScriptValue out)
	{
		if (operand.isNull())
			out.setNull();
		else switch (operand.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(!operand.asBoolean());
				return;
			case INTEGER:
				out.set(~operand.asLong());
				return;
			case FLOAT:
				out.set(Double.longBitsToDouble(~operand.rawbits));
				return;
		}
	}
	
	/**
	 * Negate calculation.
	 * @param operand the input value.
	 * @param out the output value.
	 */
	public static void negate(ScriptValue operand, ScriptValue out)
	{
		if (operand.isNull())
			out.setNull();
		else switch (operand.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(!operand.asBoolean());
				return;
			case INTEGER:
				out.set(-operand.asLong());
				return;
			case FLOAT:
				out.set(-operand.asDouble());
				return;
		}
	}
	
	/**
	 * Absolute calculation.
	 * @param operand the input value.
	 * @param out the output value.
	 */
	public static void absolute(ScriptValue operand, ScriptValue out)
	{
		if (operand.isNull())
			out.setNull();
		else switch (operand.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(operand.asBoolean());
				return;
			case INTEGER:
				out.set(Math.abs(operand.asLong()));
				return;
			case FLOAT:
				out.set(Math.abs(operand.asDouble()));
				return;
		}
	}
	
	/**
	 * Logical not calculation.
	 * @param operand the input value.
	 * @param out the output value.
	 */
	public static void logicalNot(ScriptValue operand, ScriptValue out)
	{
		if (operand.isNull())
			out.setNull();
		else 
			out.set(!operand.asBoolean());
	}
	
	// Up-converts the cached values.
	private static void convertUp(Cache cacheValue, ScriptValue operand, ScriptValue operand2)
	{
		if (operand.type.ordinal() < operand2.type.ordinal())
			cacheValue.value1.convertTo(operand2.type);
		else if (operand.type.ordinal() > operand2.type.ordinal())
			cacheValue.value2.convertTo(operand.type);
	}

	/**
	 * Add calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void add(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		if (!operand.isAddableType() || !operand2.isAddableType())
		{
			out.set(Double.NaN);
			return;
		}
		
		Cache cacheValue = getCache();
		cacheValue.value1.set(operand);
		cacheValue.value2.set(operand2);
		convertUp(cacheValue, operand, operand2);
		
		switch (cacheValue.value2.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(cacheValue.value1.asBoolean() || cacheValue.value2.asBoolean());
				return;
			case INTEGER:
				out.set(cacheValue.value1.asLong() + cacheValue.value2.asLong());
				return;
			case FLOAT:
				out.set(cacheValue.value1.asDouble() + cacheValue.value2.asDouble());
				return;
			case STRING:
				out.set(cacheValue.value1.asString() + cacheValue.value2.asString());
				return;
		}
	}
	
	/**
	 * Subtract calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void subtract(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		if (!operand.isRaw() || !operand2.isRaw())
		{
			out.set(Double.NaN);
			return;
		}

		Cache cacheValue = getCache();
		cacheValue.value1.set(operand);
		cacheValue.value2.set(operand2);
		convertUp(cacheValue, operand, operand2);
		
		switch (cacheValue.value2.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				boolean v1 = cacheValue.value1.asBoolean();
				out.set(!v1 ? false : (cacheValue.value2.asBoolean() ? false : v1));
				return;
			case INTEGER:
				out.set(cacheValue.value1.asLong() - cacheValue.value2.asLong());
				return;
			case FLOAT:
				out.set(cacheValue.value1.asDouble() - cacheValue.value2.asDouble());
				return;
		}
	}

	/**
	 * Multiply calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void multiply(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		if (!operand.isRaw() || !operand2.isRaw())
		{
			out.set(Double.NaN);
			return;
		}

		Cache cacheValue = getCache();
		cacheValue.value1.set(operand);
		cacheValue.value2.set(operand2);
		convertUp(cacheValue, operand, operand2);
		
		switch (cacheValue.value2.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(cacheValue.value1.asBoolean() && cacheValue.value2.asBoolean());
				return;
			case INTEGER:
				out.set(cacheValue.value1.asLong() * cacheValue.value2.asLong());
				return;
			case FLOAT:
				out.set(cacheValue.value1.asDouble() * cacheValue.value2.asDouble());
				return;
		}
	}

	/**
	 * Divide calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void divide(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		if (!operand.isRaw() || !operand2.isRaw())
		{
			out.set(Double.NaN);
			return;
		}

		Cache cacheValue = getCache();
		cacheValue.value1.set(operand);
		cacheValue.value2.set(operand2);
		convertUp(cacheValue, operand, operand2);
		
		switch (cacheValue.value2.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(cacheValue.value1.asBoolean());
				return;
			case INTEGER:
				long dividend = cacheValue.value2.asLong();
				if (dividend != 0)
					out.set(cacheValue.value1.asLong() / dividend);
				else
					out.set(Double.NaN);
				return;
			case FLOAT:
				out.set(cacheValue.value1.asDouble() / cacheValue.value2.asDouble());
				return;
		}
	}

	/**
	 * Modulo calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void modulo(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		if (!operand.isRaw() || !operand2.isRaw())
		{
			out.set(Double.NaN);
			return;
		}

		Cache cacheValue = getCache();
		cacheValue.value1.set(operand);
		cacheValue.value2.set(operand2);
		convertUp(cacheValue, operand, operand2);
		
		switch (cacheValue.value2.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(cacheValue.value1.asBoolean());
				return;
			case INTEGER:
				long dividend = cacheValue.value2.asLong();
				if (dividend != 0)
					out.set(cacheValue.value1.asLong() % dividend);
				else
					out.set(Double.NaN);
				return;
			case FLOAT:
				out.set(cacheValue.value1.asDouble() % cacheValue.value2.asDouble());
				return;
		}
	}

	/**
	 * Bitwise And calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void and(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		if (!operand.isRaw() || !operand2.isRaw())
		{
			out.set(Double.NaN);
			return;
		}

		Cache cacheValue = getCache();
		cacheValue.value1.set(operand);
		cacheValue.value2.set(operand2);
		convertUp(cacheValue, operand, operand2);
		
		switch (cacheValue.value2.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(cacheValue.value1.asBoolean() && cacheValue.value2.asBoolean());
				return;
			case INTEGER:
			case FLOAT:
				out.set(cacheValue.value1.rawbits & cacheValue.value2.rawbits);
				return;
		}
	}

	/**
	 * Bitwise Or calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void or(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		if (!operand.isRaw() || !operand2.isRaw())
		{
			out.set(Double.NaN);
			return;
		}

		Cache cacheValue = getCache();
		cacheValue.value1.set(operand);
		cacheValue.value2.set(operand2);
		convertUp(cacheValue, operand, operand2);
		
		switch (cacheValue.value2.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(cacheValue.value1.asBoolean() || cacheValue.value2.asBoolean());
				return;
			case INTEGER:
			case FLOAT:
				out.set(cacheValue.value1.rawbits | cacheValue.value2.rawbits);
				return;
		}
	}

	/**
	 * Bitwise XOr calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void xor(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		if (!operand.isRaw() || !operand2.isRaw())
		{
			out.set(Double.NaN);
			return;
		}

		Cache cacheValue = getCache();
		cacheValue.value1.set(operand);
		cacheValue.value2.set(operand2);
		convertUp(cacheValue, operand, operand2);
		
		switch (cacheValue.value2.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(cacheValue.value1.asBoolean() ^ cacheValue.value2.asBoolean());
				return;
			case INTEGER:
			case FLOAT:
				out.set(cacheValue.value1.rawbits ^ cacheValue.value2.rawbits);
				return;
		}
	}

	/**
	 * Logical And calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void logicalAnd(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(operand.asBoolean() && operand2.asBoolean());
	}

	/**
	 * Logical Or calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void logicalOr(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(operand.asBoolean() || operand2.asBoolean());
	}

	/**
	 * Left shift calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void leftShift(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		switch (operand.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(operand.asBoolean());
				return;
			case INTEGER:
				out.set(operand.asLong() << operand2.asLong());
				return;
			case FLOAT:
				out.set(Double.longBitsToDouble(operand.rawbits << operand2.asLong()));
				return;
		}
	}

	/**
	 * Right shift calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void rightShift(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		switch (operand.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(operand.asBoolean());
				return;
			case INTEGER:
				out.set(operand.asLong() >> operand2.asLong());
				return;
			case FLOAT:
				out.set(Double.longBitsToDouble(operand.rawbits >> operand2.asLong()));
				return;
		}
	}

	/**
	 * Right shift padded calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void rightShiftPadded(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		switch (operand.type)
		{
			default:
				out.set(Double.NaN);
				return;
			case BOOLEAN:
				out.set(operand.asBoolean());
				return;
			case INTEGER:
				out.set(operand.asLong() >>> operand2.asLong());
				return;
			case FLOAT:
				out.set(Double.longBitsToDouble(operand.rawbits >>> operand2.asLong()));
				return;
		}
	}

	/**
	 * Less-than calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void less(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(operand.compareTo(operand2) < 0);
	}

	/**
	 * Less-than-or-equal calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void lessOrEqual(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(operand.compareTo(operand2) <= 0);
	}

	/**
	 * Greater-than calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void greater(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(operand.compareTo(operand2) > 0);
	}

	/**
	 * Greater-than-or-equal calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void greaterOrEqual(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(operand.compareTo(operand2) >= 0);
	}

	/**
	 * Logical Equal calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void equal(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(operand.compareTo(operand2) == 0);
	}

	/**
	 * Logical Not Equal calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void notEqual(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(operand.compareTo(operand2) != 0);
	}

	/**
	 * Strict Equal calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void strictEqual(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(operand.equals(operand2));
	}

	/**
	 * Strict Not Equal calculation.
	 * @param operand the source operand.
	 * @param operand2 the second operand. 
	 * @param out the output value.
	 */
	public static void strictNotEqual(ScriptValue operand, ScriptValue operand2, ScriptValue out)
	{
		out.set(!operand.equals(operand2));
	}

	/**
	 * The class used for a map type.
	 */
	public static class MapType extends AbstractVariableResolver implements Iterable<AbstractVariableResolver.Entry>
	{
		private MapType()
		{
			super(DEFAULT_CAPACITY);
		}

		private MapType(int capacity)
		{
			super(capacity);
		}
		
	    /**
	     * Removes a value by variable name.
	     * This should fail if the provided name corresponds to a read-only variable. 
		 * @param name the variable name.
		 * @return true if the value existed and was removed, false otherwise.
		 * @throws IllegalArgumentException if the provided name refers to a value that is read-only.
	     */
		private synchronized boolean clearValue(String name)
		{
			int i;
			if ((i = getIndex(name)) < 0)
				return false;

			removeIndex(i);
			return true;
		}
		
		@Override
		public ResettableIterator<Entry> iterator()
		{
			return new EntryIterator();
		}

		private class EntryIterator implements ResettableIterator<Entry>
		{
			private int cur = 0;
			private boolean removed = false;
			
			@Override
			public boolean hasNext()
			{
				return cur < size();
			}
			
			@Override
			public Entry next()
			{
				removed = false;
				return entries[cur++];
			}
			
			@Override
			public void remove()
			{
				if (removed)
					return;
				
				removeIndex(cur);
				removed = true;
				cur--;
			}
			
			@Override
			public void reset()
			{
				cur = 0;
			}
		}
		
	}
	
	/**
	 * The class used as an error type.
	 * Errors are a completely separate type, in order to differentiate them from objects,
	 * and check them in return from host functions (or other functions).
	 */
	public static class ErrorType
	{
		private String type;
		private String message;
		private String localizedMessage;
		
		/**
		 * Creates an error value.
		 * @param type the error type.
		 * @param message the error message.
		 * @param localizedMessage a localized version of the error message.
		 * @return a new ErrorType.
		 */
		public static ErrorType create(String type, String message, String localizedMessage)
		{
			ErrorType out = new ErrorType();
			out.type = type;
			out.message = message;
			out.localizedMessage = localizedMessage;
			return out;
		}
		
		/**
		 * Creates an error value from a Throwable.
		 * Copies the simple class name, the message, and the localized message.
		 * @param t the Throwable to use.
		 * @return a new ErrorType.
		 * @see #createError(String, String, String)
		 * @see Throwable#getClass()
		 * @see Class#getSimpleName()
		 * @see Throwable#getMessage()
		 * @see Throwable#getLocalizedMessage()
		 */
		public static ErrorType create(Throwable t)
		{
			ErrorType out = new ErrorType();
			out.type = t.getClass().getSimpleName();
			out.message = t.getMessage();
			out.localizedMessage = t.getLocalizedMessage();
			return out;
		}
		
		private ErrorType() {}
		
		public String getType() 
		{
			return type;
		}

		public String getMessage() 
		{
			return message;
		}
		
		public String getLocalizedMessage() 
		{
			return localizedMessage;
		}
		
	}
	
}
