/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import com.blackrook.rookscript.resolvers.variable.AbstractVariableResolver;
import com.blackrook.rookscript.resolvers.variable.AbstractVariableResolver.Entry;
import com.blackrook.rookscript.struct.Utils;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile.FieldInfo;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile.MethodInfo;

/**
 * Script value encapsulation.
 * @author Matthew Tropiano
 */
public class ScriptValue implements Comparable<ScriptValue>
{
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<Object[]> OBJECTARRAY1 = ThreadLocal.withInitial(()->new Object[1]);

	public static enum Type
	{
		NULL,
		BOOLEAN,
		INTEGER,
		FLOAT,
		STRING,
		OBJECTREF,
		BUFFER,
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
	 * Creates a script value.
	 * @param type the target script value type.
	 * @param value the source value.
	 * @return a new script value.
	 */
	public static ScriptValue create(Type type, Object value)
	{
		ScriptValue out = new ScriptValue();
		out.set(type, value);
		return out;
	}
	
	/**
	 * Creates a script value that is an empty buffer.
	 * @param size the size of the buffer in bytes.
	 * @return a new script value.
	 */
	public static ScriptValue createEmptyBuffer(int size)
	{
		ScriptValue out = new ScriptValue();
		out.setEmptyBuffer(size);
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
		sv.setError(type, message, localizedMessage);
		return sv;
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
	 * Sets this value to a new buffer (new reference), native byte order.
	 * @param size the size of the new buffer in bytes.
	 */
	public void setEmptyBuffer(int size)
	{
		this.type = Type.BUFFER;
		this.ref = new BufferType(size, ByteOrder.nativeOrder());
		this.rawbits = 0L;
	}
	
	/**
	 * Sets this value to a new buffer (new reference).
	 * @param size the size of the new buffer in bytes.
	 * @param order the byte ordering.
	 */
	public void setEmptyBuffer(int size, ByteOrder order)
	{
		this.type = Type.BUFFER;
		this.ref = new BufferType(size, order);
		this.rawbits = 0L;
	}

	/**
	 * Sets this value to a new empty list (new reference) initialized by nulls.
	 * @param size the initial of the new empty list.
	 * @param capacity the inner capacity of the new empty list.
	 */
	public void setEmptyList(int size, int capacity)
	{
		this.type = Type.LIST;
		this.ref = new ListType(size, capacity);
		this.rawbits = 0L;
	}
	
	/**
	 * Sets this value to a new empty list (new reference).
	 * @param capacity the inner capacity of the new empty list.
	 */
	public void setEmptyList(int capacity)
	{
		this.type = Type.LIST;
		this.ref = new ListType(0, capacity);
		this.rawbits = 0L;
	}
	
	/**
	 * Sets this value to a new empty list (new reference).
	 */
	public void setEmptyList()
	{
		this.type = Type.LIST;
		this.ref = new ListType(0, 8);
		this.rawbits = 0L;
	}
	
	/**
	 * Sets this value to a new empty map (new reference).
	 * @param capacity the inner capacity of the new empty map.
	 */
	public void setEmptyMap(int capacity)
	{
		this.type = Type.MAP;
		this.ref = new MapType(capacity);
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
	 * Sets this value as an error.
	 * @param type the error type.
	 * @param message the error message.
	 */
	public void setError(String type, String message)
	{
		setError(type, message, message);
	}
	
	/**
	 * Sets this value as an error.
	 * @param type the error type.
	 * @param message the error message.
	 * @param localizedMessage a localized version of the error message.
	 */
	public void setError(String type, String message, String localizedMessage)
	{
		this.type = Type.ERROR;
		this.ref = ErrorType.create(type, message, localizedMessage);
		this.rawbits = 0L;
	}
	
	/**
	 * Sets this value as an error.
	 * If null, this is set to the null value.
	 * @param value the source error to use.
	 */
	public void setError(Throwable value)
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
	 * Sets this value using another object, 
	 * and converts it if possible to the target underlying type.
	 * @param type the target script value type.
	 * @param value the source value to use.
	 */
	public void set(Type type, Object value)
	{
		if (type == null)
		{
			set(value);
			return;
		}
		
		switch (type)
		{
			default:
			case NULL:
			{
				setNull();
			}
			break;
				
			case BOOLEAN:
			{
				if (value instanceof Boolean)
					set((boolean)value);
				else if (value instanceof Number)
				{
					double d = ((Number)value).doubleValue();
					set(!Double.isNaN(d) && d != 0.0);
				}
				else if (value instanceof Map)
					set(!((Collection<?>)value).isEmpty());
				else if (value instanceof Collection)
					set(!((Collection<?>)value).isEmpty());
				else
					set(value != null);
			}
			break;
				
			case INTEGER:
			{
				if (value instanceof Boolean)
					set(((boolean)value) ? 1 : 0);
				else if (value instanceof Number)
					set(((Number)value).longValue());
				else if (value instanceof CharSequence)
					set(Utils.parseLong((String)value, 0L));
				else
					set(value != null ? 1 : 0);
			}
			break;
				
			case FLOAT:
			{
				if (value instanceof Boolean)
					set(((boolean)value) ? 1.0 : 0.0);
				else if (value instanceof Number)
					set(((Number)value).doubleValue());
				else if (value instanceof CharSequence)
					set(Utils.parseDouble((String)value, Double.NaN));
				else
					set(value != null ? 1.0 : 0.0);
			}
			break;
				
			case STRING:
			{
				set(String.valueOf(value));
			}
			break;
				
			case LIST:
			{
				setEmptyList();
				listAdd(value);
			}
			break;
				
			case MAP:
			{
				setEmptyMap();
				if (value != null)
					mapExtract(value);
			}
			break;
				
			case ERROR:
			{
				if (value == null)
					set(ErrorType.create("null", null, null));
				else if (value instanceof Throwable)
					set(ErrorType.create((Throwable)value));
				else
					set(ErrorType.create(value.getClass().getSimpleName(), null, null));
			}
			break;

			case OBJECTREF:
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
			break;	
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
		else if (value instanceof CharSequence)
			set((String)value);
		else if (value instanceof Map)
		{
			Map<?,?> map = (Map<?, ?>)value;
			setEmptyMap(map.size());
			mapExtract(map);
		}
		else if (value instanceof Collection)
		{
			Collection<?> coll = (Collection<?>)value;
			setEmptyList(coll.size());
			listExtract(coll);
		}
		else
		{
			Class<?> clazz = value.getClass();
			if (clazz.isArray())
			{
				int len = Array.getLength(value);
				setEmptyList(len);
				listExtract((Object[])value);
			}
			else
			{
				this.type = Type.OBJECTREF;
				this.ref = value;
				this.rawbits = 0L;
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
	public void set(CharSequence value)
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
	 * Gets the length of this value, if this is a map, list, or the underlying object
	 * is an array or {@link Collection} type.
	 * @return the length in values, or 1 if not a collection of some kind.
	 */
	public int length()
	{
		switch (type)
		{
			case BUFFER:
				return ((BufferType)ref).size();
			case LIST:
				return ((ListType)ref).size();
			case MAP:
				return ((MapType)ref).size();
			case STRING:
				return ((String)ref).length();
			default:
			{
				if (ref instanceof Collection)
					return ((Collection<?>)ref).size();
				return 1;
			}
		}
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
			case LIST:
				return ((ListType)ref).isEmpty();
			case MAP:
				return ((MapType)ref).isEmpty();
			default:
			case STRING:
			case OBJECTREF:
				return Utils.isEmpty(ref);
		}
	}
	
	/**
	 * Sets the contents of this list to the provided collection.
	 * @param list the list to set.
	 * @return true if set, false if not.
	 */
	public boolean listExtract(Collection<?> list)
	{
		if (!isList())
			return false;

		for (Object obj : list)
			listAdd(obj);
		
		return true;
	}
	
	/**
	 * Sets the contents of this list to the provided collection.
	 * @param list the list to set.
	 * @return true if set, false if not.
	 */
	public <T> boolean listExtract(T[] list)
	{
		if (!isList())
			return false;

		for (T obj : list)
			listAdd(obj);
		
		return true;
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

		ListType list = (ListType)ref;
		if (index < 0 || index >= list.size())
			return false;
		list.set(index, value);
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

		ListType list = (ListType)ref;
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			list.add(temp);
			return true;
		}
		finally
		{
			temp.setNull();
		}
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
		
		ListType list = (ListType)ref;
		
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			list.add(index, temp);
			return true;
		}
		finally
		{
			temp.setNull();
		}
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

		ListType list = (ListType)ref;
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			boolean out = list.remove(temp);
			return out;
		}
		finally
		{
			temp.setNull();
		}
	}
	
	/**
	 * Removes a value from this value at an index, if it is a list.
	 * @param index the index to remove.
	 * @param out the output value - the value that was removed, or set to null if not a list nor a valid index.
	 * @return true if this is a list and the index is valid and a value was removed, false otherwise.
	 * @see #isList()
	 */
	public boolean listRemoveAt(int index, ScriptValue out)
	{
		if (!isList())
			return false;

		((ListType)ref).removeIndex(index, out);
		return true;
	}

	/**
	 * Gets a value at an index, if it is a list.
	 * NOTE: This returns a reference, not a new instance!
	 * @param index the list index to return.
	 * @param out the output value - the value at the index, or set to null if not a list nor a valid index.
	 * @return the value at the index, or null if not a list nor a valid index.
	 * @see #isList()
	 */
	public boolean listGetByIndex(int index, ScriptValue out)
	{
		if (!isList())
			return false;
	
		((ListType)ref).get(index, out);
		return true;
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
	
		ListType list = (ListType)ref;
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			int out = list.indexOf(temp);
			return out;
		}
		finally
		{
			temp.setNull();
		}
	}

	/**
	 * Gets the last index that a value is found at, if it is a list.
	 * @param value the value to look for.
	 * @return the index found, or -1 if not found or not a list.
	 * @see #isList()
	 */
	public int listGetLastIndexOf(Object value)
	{
		if (!isList())
			return -1;
	
		ListType list = (ListType)ref;
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			int out = list.lastIndexOf(temp);
			return out;
		}
		finally
		{
			temp.setNull();
		}
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
	public void sort()
	{
		if (!isList())
			return;
	
		((ListType)ref).sort();
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
	
		ListType list = (ListType)ref;
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			if (list.search(temp) < 0)
			{
				list.add(temp);
				list.sort();
				return true;
			}
			return false;
		}
		finally
		{
			temp.setNull();
		}
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

		ListType list = (ListType)ref;
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			int index = list.search(temp);
			if (index >= 0)
			{
				list.removeIndex(index, temp);
				return true;
			}
			else
			{
				return false;
			}
		}
		finally
		{
			temp.setNull();
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

		ListType list = (ListType)ref;
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			boolean out = list.search(temp) >= 0;
			return out;
		}
		finally
		{
			temp.setNull();
		}
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

		ListType list = (ListType)ref;
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			int out = list.search(temp);
			return out;
		}
		finally
		{
			temp.setNull();
		}
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
		ScriptValue temp = CACHEVALUE1.get();
		try
		{
			temp.set(value);
			map.setValue(key, temp);
			return true;
		}
		finally
		{
			temp.setNull();
		}
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
	 * Extracts a map's key-value pairs and sets those values to this map.
	 * This can be expensive, depending on what needs converting. 
	 * <p>If you are passing an object to a script repeatedly, it may be better to just pass it
	 * as an object reference, if there are associated host functions that manipulate it.
	 * @param map the source map.
	 * @return true if this is a map and extraction was successful.
	 */
	public <T> boolean mapExtract(Map<?, ?> map)
	{
		if (!isMap())
			return false;

		for (Map.Entry<?, ?> entry : map.entrySet())
			mapSet(String.valueOf(entry.getKey()), entry.getValue());
		return true;
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
		Profile<T> profile = Utils.getProfile((Class<T>)object.getClass());
		
		for (Map.Entry<String, MethodInfo> entry : profile.getGetterMethodsByName().entrySet())
		{
			String name;
			MethodInfo info = entry.getValue();
			if (info.getAlias() != null)
				name = info.getAlias();
			else
				name = entry.getKey();
			Object value = Utils.invokeBlind(info.getMethod(), object);
			mapSet(name, value);
		}
		
		for (Map.Entry<String, FieldInfo> entry : profile.getPublicFieldsByName().entrySet())
		{
			String name;
			FieldInfo info = entry.getValue();
			if (info.getAlias() != null)
				name = info.getAlias();
			else
				name = entry.getKey();
			Object value = Utils.getFieldValue(object, info.getField());
			mapSet(name, value);
		}

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
		Profile<T> profile = Utils.getProfile((Class<T>)object.getClass());
		MapType map = (MapType)ref;
		
		for (Entry entry : map)
		{
			String name = entry.getName().toLowerCase();
			ScriptValue value = entry.getValue();


			FieldInfo fi;
			MethodInfo mi;
			if ((fi = Utils.isNull(profile.getPublicFieldsByAlias().get(name), profile.getPublicFieldsByName().get(name))) != null)
			{
				Object[] vbuf = OBJECTARRAY1.get();
				vbuf[0] = value.createForType(fi.getType());
				Utils.setFieldValue(object, fi.getField(), vbuf);
				Arrays.fill(vbuf, null); // arrays are shared - purge refs after use.
			}
			else if ((mi = Utils.isNull(profile.getSetterMethodsByAlias().get(name), profile.getSetterMethodsByName().get(name))) != null)
			{
				Object[] vbuf = OBJECTARRAY1.get();
				vbuf[0] = value.createForType(mi.getType());
				Utils.invokeBlind(mi.getMethod(), object, vbuf);
				Arrays.fill(vbuf, null); // arrays are shared - purge refs after use.
			}

		}
		
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
			case NULL:
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
	 * @return true if this value is a buffer type.
	 */
	public boolean isBuffer()
	{
		return type == Type.BUFFER;
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
				return ((String)ref).length() == 0;
			case OBJECTREF:
			case LIST:
			case MAP:
			case ERROR:
				return ref != null; 
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
				return Utils.parseLong((String)ref, 0L); 
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
				return Utils.parseDouble((String)ref, Double.NaN); 
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
				return Utils.isArray(ref) ? Arrays.toString((Object[])ref) : String.valueOf(ref);
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
				return Utils.isArray(clazz) && targetType.isAssignableFrom(Utils.getArrayType(clazz));
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
				if (Utils.isArray(ref))
					return "objectref:array:" + Utils.getArrayType(ref).getSimpleName();
				else
					return "objectref:" + ref.getClass().getSimpleName();
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
	 */
	public <T> T createForType(Class<T> targetType)
	{
		switch(type)
		{
			case NULL:
				return (T)null;
			case BOOLEAN:
				return Utils.createForType(asBoolean(), targetType);
			case INTEGER:
				return Utils.createForType(asLong(), targetType);
			case FLOAT:
				return Utils.createForType(asDouble(), targetType);
			case STRING:
				return Utils.createForType(asString(), targetType);
			case LIST:
				return Utils.createForType(asObjectType(ListType.class), targetType);
			case MAP:
				T out = Utils.create(targetType);
				mapApply(out);
				return out;
			default:
			case ERROR:
			case OBJECTREF:
				return Utils.createForType(asObject(), targetType);
		}
	}
	
	/**
	 * @return an iterator of the list, or null if not a list.
	 */
	public Iterator<ScriptValue> listIterator()
	{
		if (!isList())
			return null;
		return ((ListType)ref).iterator();
	}
	
	@Override
	public int hashCode()
	{
		if (isRaw())
			return Long.hashCode(rawbits);
		else
			return ref.hashCode();
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
		else if (isNumeric() && o.isNumeric())
		{
			double d1 = asDouble();
			double d2 = o.asDouble();
			return d1 == d2 ? 0 : (d1 < d2 ? -1 : 1);
		}
		else if (type == Type.STRING || o.type == Type.STRING)
			return asString().compareTo(o.asString());
		else
			return ref == o.ref ? 0 : -1;
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
			case OBJECTREF:
				return String.valueOf(ref);
			default:
				if (Utils.isArray(ref))
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
	private static void convertUp(ScriptValue cache1, ScriptValue cache2, ScriptValue operand, ScriptValue operand2)
	{
		if (operand.type.ordinal() < operand2.type.ordinal())
			cache1.convertTo(operand2.type);
		else if (operand.type.ordinal() > operand2.type.ordinal())
			cache2.convertTo(operand.type);
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
		
		ScriptValue cache1 = CACHEVALUE1.get();
		ScriptValue cache2 = CACHEVALUE2.get();

		try
		{
			cache1.set(operand);
			cache2.set(operand2);
			convertUp(cache1, cache2, operand, operand2);
			switch (cache2.type)
			{
				default:
					out.set(Double.NaN);
					return;
				case BOOLEAN:
					out.set(cache1.asBoolean() || cache2.asBoolean());
					return;
				case INTEGER:
					out.set(cache1.asLong() + cache2.asLong());
					return;
				case FLOAT:
					out.set(cache1.asDouble() + cache2.asDouble());
					return;
				case STRING:
					out.set(cache1.asString() + cache2.asString());
					return;
			}
		}
		finally
		{
			cache1.setNull();
			cache2.setNull();
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

		ScriptValue cache1 = CACHEVALUE1.get();
		ScriptValue cache2 = CACHEVALUE2.get();
		try
		{
			cache1.set(operand);
			cache2.set(operand2);
			convertUp(cache1, cache2, operand, operand2);
			switch (cache2.type)
			{
				default:
					out.set(Double.NaN);
					return;
				case BOOLEAN:
					boolean v1 = cache1.asBoolean();
					out.set(!v1 ? false : (cache2.asBoolean() ? false : v1));
					return;
				case INTEGER:
					out.set(cache1.asLong() - cache2.asLong());
					return;
				case FLOAT:
					out.set(cache1.asDouble() - cache2.asDouble());
					return;
			}
		}
		finally
		{
			cache1.setNull();
			cache2.setNull();
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

		ScriptValue cache1 = CACHEVALUE1.get();
		ScriptValue cache2 = CACHEVALUE2.get();
		try
		{
			cache1.set(operand);
			cache2.set(operand2);
			convertUp(cache1, cache2, operand, operand2);
			switch (cache2.type)
			{
				default:
					out.set(Double.NaN);
					return;
				case BOOLEAN:
					out.set(cache1.asBoolean() && cache2.asBoolean());
					return;
				case INTEGER:
					out.set(cache1.asLong() * cache2.asLong());
					return;
				case FLOAT:
					out.set(cache1.asDouble() * cache2.asDouble());
					return;
			}
		}
		finally
		{
			cache1.setNull();
			cache2.setNull();
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

		ScriptValue cache1 = CACHEVALUE1.get();
		ScriptValue cache2 = CACHEVALUE2.get();
		try
		{
			cache1.set(operand);
			cache2.set(operand2);
			convertUp(cache1, cache2, operand, operand2);
			switch (cache2.type)
			{
				default:
					out.set(Double.NaN);
					return;
				case BOOLEAN:
					out.set(cache1.asBoolean());
					return;
				case INTEGER:
					long dividend = cache2.asLong();
					if (dividend != 0)
						out.set(cache1.asLong() / dividend);
					else
						out.set(Double.NaN);
					return;
				case FLOAT:
					out.set(cache1.asDouble() / cache2.asDouble());
					return;
			}
		}
		finally
		{
			cache1.setNull();
			cache2.setNull();
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

		ScriptValue cache1 = CACHEVALUE1.get();
		ScriptValue cache2 = CACHEVALUE2.get();
		try
		{
			cache1.set(operand);
			cache2.set(operand2);
			convertUp(cache1, cache2, operand, operand2);
			switch (cache2.type)
			{
				default:
					out.set(Double.NaN);
					return;
				case BOOLEAN:
					out.set(cache1.asBoolean());
					return;
				case INTEGER:
					long dividend = cache2.asLong();
					if (dividend != 0)
						out.set(cache1.asLong() % dividend);
					else
						out.set(Double.NaN);
					return;
				case FLOAT:
					out.set(cache1.asDouble() % cache2.asDouble());
					return;
			}
		}
		finally
		{
			cache1.setNull();
			cache2.setNull();
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

		ScriptValue cache1 = CACHEVALUE1.get();
		ScriptValue cache2 = CACHEVALUE2.get();
		try
		{
			cache1.set(operand);
			cache2.set(operand2);
			convertUp(cache1, cache2, operand, operand2);
			switch (cache2.type)
			{
				default:
					out.set(Double.NaN);
					return;
				case BOOLEAN:
					out.set(cache1.asBoolean() && cache2.asBoolean());
					return;
				case INTEGER:
				case FLOAT:
					out.set(cache1.rawbits & cache2.rawbits);
					return;
			}
		}
		finally
		{
			cache1.setNull();
			cache2.setNull();
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

		ScriptValue cache1 = CACHEVALUE1.get();
		ScriptValue cache2 = CACHEVALUE2.get();
		try
		{
			cache1.set(operand);
			cache2.set(operand2);
			convertUp(cache1, cache2, operand, operand2);
			switch (cache2.type)
			{
				default:
					out.set(Double.NaN);
					return;
				case BOOLEAN:
					out.set(cache1.asBoolean() || cache2.asBoolean());
					return;
				case INTEGER:
				case FLOAT:
					out.set(cache1.rawbits | cache2.rawbits);
					return;
			}
		}
		finally
		{
			cache1.setNull();
			cache2.setNull();
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

		ScriptValue cache1 = CACHEVALUE1.get();
		ScriptValue cache2 = CACHEVALUE2.get();
		try
		{
			cache1.set(operand);
			cache2.set(operand2);
			convertUp(cache1, cache2, operand, operand2);
			switch (cache2.type)
			{
				default:
					out.set(Double.NaN);
					return;
				case BOOLEAN:
					out.set(cache1.asBoolean() ^ cache2.asBoolean());
					return;
				case INTEGER:
				case FLOAT:
					out.set(cache1.rawbits ^ cache2.rawbits);
					return;
			}
		}
		finally
		{
			cache1.setNull();
			cache2.setNull();
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
	 * The class used for a buffer of bytes.
	 */
	public static class BufferType
	{
		static final String BYTEALPHABET = "0123456789abcdef";
		
		private int position;
		private byte[] data;
		private ByteOrder order;
		
		private BufferType(int size, ByteOrder byteOrder)
		{
			setPosition(0);
			setSize(size);
			setByteOrder(byteOrder);
		}

		/**
		 * Sets the buffer's current cursor position.
		 * @param position the new position.
		 */
		public void setPosition(int position)
		{
			this.position = position;
		}

		/**
		 * @return the buffer's current cursor position.
		 */
		public int getPosition() 
		{
			return position;
		}
		
		/**
		 * Sets the size of this buffer in bytes.
		 * If this is resized, the buffer's data is kept.
		 * @param size the new buffer size.
		 */
		public void setSize(int size)
		{
			byte[] newdata = new byte[size];
			if (data != null)
				System.arraycopy(data, 0, newdata, 0, Math.min(data.length, newdata.length));
			data = newdata;
		}
		
		/**
		 * Sets the byte order of this buffer.
		 * @param byteOrder the new byte order.
		 */
		public void setByteOrder(ByteOrder byteOrder)
		{
			this.order = byteOrder;
		}
		
		/**
		 * Gets the byte order of this buffer.
		 * @return order the current byte order.
		 */
		public ByteOrder getByteOrder()
		{
			return order;
		}
		
		/**
		 * Reads bytes from an input stream into this buffer.
		 * If null is passed in as the index, the buffer's cursor position is advanced by the length.
		 * @param index the destination index (or null for current position).
		 * @param in the input stream to read from.
		 * @param length the amount of bytes to read.
		 * @return the amount of bytes actually read. May be less than length (see {@link InputStream#read(byte[], int, int)}).
		 * @throws IOException if a read error occurs.
		 * @throws IndexOutOfBoundsException if <code>index + length</code> exceeds the buffer length. 
		 */
		public int readBytes(Integer index, InputStream in, int length) throws IOException
		{
			int out = in.read(data, index != null ? index : position, length);
			if (index == null)
				position += out;
			return out;
		}

		/**
		 * Reads bytes from an open file handle into this buffer.
		 * This relies on the file's position to be set to where the read should occur.
		 * If null is passed in as the index, the buffer's cursor position is advanced by the length.
		 * @param index the destination index (or null for current position).
		 * @param file the source buffer.
		 * @param length the amount of bytes to read.
		 * @return the amount of bytes actually read. May be less than length (see {@link RandomAccessFile#read(byte[], int, int)}).
		 * @throws IOException if a read error occurs.
		 * @throws IndexOutOfBoundsException if <code>index</code> exceeds this buffer's length. 
		 */
		public int readBytes(Integer index, RandomAccessFile file, int length) throws IOException
		{
			int out = file.read(data, index != null ? index : position, length);
			if (index == null)
				position += out;
			return out;
		}
		
		/**
		 * Reads bytes from another buffer into this one.
		 * This relies on the other buffer's position to be set to where the read should occur.
		 * If null is passed in as the index, the buffer's cursor position is advanced by the length.
		 * @param index the destination index (or null for current position).
		 * @param buffer the source buffer.
		 * @param length the amount of bytes to read.
		 * @return the amount of bytes actually read.
		 * @throws IndexOutOfBoundsException if <code>index</code> exceeds this buffer's length. 
		 */
		public int readBytes(Integer index, BufferType buffer, int length)
		{
			int out = 0;
			if (index == null) while (position < data.length && length > 0)
			{
				putByte(null, buffer.getByte(null));
				length--;
				out++;
			}
			else while (index < data.length && length > 0)
			{
				putByte(index++, buffer.getByte(null));
				length--;
				out++;
			}
			return out;
		}
		
		/**
		 * Reads bytes from another buffer into this one.
		 * This relies on the other buffer's position to be set to where the read should occur.
		 * If null is passed in as the index, the buffer's cursor position is advanced by the length.
		 * If null is passed in as the offset, the source buffer's cursor position is advanced by the length.
		 * @param index the destination index (or null for current position).
		 * @param buffer the source buffer.
		 * @param offset the offset into the source buffer (or null for its current position).
		 * @param length the amount of bytes to read.
		 * @return the amount of bytes actually read (length).
		 * @throws IndexOutOfBoundsException if <code>index + length</code> exceeds this buffer's length. 
		 */
		public int readBytes(Integer index, BufferType buffer, Integer offset, int length)
		{
			int i = index != null ? index : position;
			int o = offset != null ? offset : buffer.position;
			System.arraycopy(buffer.data, o, data, i, length);
			if (index == null)
				position += length;
			if (offset == null)
				buffer.position += length;
			return length;
		}
		
		/**
		 * Reads bytes from a byte array into this buffer.
		 * If null is passed in as the index, the buffer's cursor position is advanced by the length.
		 * @param index the destination index (or null for current position).
		 * @param bytes the source array.
		 * @param offset the offset into the provided array to start the read from.
		 * @param length the amount of bytes to read.
		 * @return the amount of bytes read (length).
		 * @throws IndexOutOfBoundsException if <code>index + length</code> exceeds this buffer's length 
		 * 		or <code>index + length</code> exceeds the length of the provided buffer. 
		 */
		public int readBytes(Integer index, byte[] bytes, int offset, int length)
		{
			System.arraycopy(bytes, offset, data, index != null ? index : position, length);
			if (index == null)
				position += length;
			return length;
		}

		/**
		 * Fills bytes using a value.
		 * If null is passed in as the index, the buffer's cursor position is advanced by the length.
		 * @param index the destination index (or null for current position).
		 * @param value the fill value.
		 * @param length the amount of bytes to add.
		 * @return the amount of bytes read (length).
		 * @throws ArrayIndexOutOfBoundsException if <code>index + length</code> exceeds this buffer's length 
		 * 		or <code>index + length</code> exceeds the length of the provided buffer. 
		 */
		public int putBytes(Integer index, byte value, int length)
		{
			int i = index != null ? index : position;
			Arrays.fill(data, i, i + length, value);
			if (index == null)
				position += length;
			return length;
		}

		/**
		 * Sets a byte value.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 1.
		 * @param index the destination index (or null for current position).
		 * @param value the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public void putByte(Integer index, byte value)
		{
			data[index != null ? index : position] = value;
			if (index == null)
				position++;
		}
		
		/**
		 * Gets a byte value.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 1.
		 * @param index the source index (or null for current position).
		 * @return the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public byte getByte(Integer index)
		{
			byte out = data[index != null ? index : position];
			if (index == null)
				position++;
			return out;
		}
		
		/**
		 * Sets a short value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 2.
		 * @param index the destination index (or null for current position).
		 * @param value the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public void putShort(Integer index, short value)
		{
			int i = index != null ? index : position;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				data[i + 0] = (byte)((value & 0x000ff) >> 0);
				data[i + 1] = (byte)((value & 0x0ff00) >> 8);
			}
			else // BIG_ENDIAN
			{
				data[i + 0] = (byte)((value & 0x0ff00) >> 8);
				data[i + 1] = (byte)((value & 0x000ff) >> 0);
			}
			if (index == null)
				position += 2;
		}
		
		/**
		 * Gets a short value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 2.
		 * @param index the source index (or null for current position).
		 * @return the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public short getShort(Integer index)
		{
			int i = index != null ? index : position;
			short out = 0;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				out |= (data[i + 0] & 0x0ff) << 0;
				out |= (data[i + 1] & 0x0ff) << 8;
			}
			else // BIG_ENDIAN
			{
				out |= (data[i + 0] & 0x0ff) << 8;
				out |= (data[i + 1] & 0x0ff) << 0;
			}
			if (index == null)
				position += 2;
			return out;
		}
		
		/**
		 * Sets an unsigned short value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 2.
		 * @param index the destination index (or null for current position).
		 * @param value the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public void putUnsignedShort(Integer index, int value)
		{
			int i = index != null ? index : position;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				data[i + 0] = (byte)((value & 0x000ff) >> 0);
				data[i + 1] = (byte)((value & 0x0ff00) >> 8);
			}
			else // BIG_ENDIAN
			{
				data[i + 0] = (byte)((value & 0x0ff00) >> 8);
				data[i + 1] = (byte)((value & 0x000ff) >> 0);
			}
			if (index == null)
				position += 2;
		}
		
		/**
		 * Gets an unsigned short value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 2.
		 * @param index the source index (or null for current position).
		 * @return the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public int getUnsignedShort(Integer index)
		{
			int i = index != null ? index : position;
			int out = 0;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				out |= (data[i + 0] & 0x0ff) << 0;
				out |= (data[i + 1] & 0x0ff) << 8;
			}
			else // BIG_ENDIAN
			{
				out |= (data[i + 0] & 0x0ff) << 8;
				out |= (data[i + 1] & 0x0ff) << 0;
			}
			if (index == null)
				position += 2;
			return out;
		}
		
		/**
		 * Sets an integer value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 4.
		 * @param index the destination index (or null for current position).
		 * @param value the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public void putInteger(Integer index, int value)
		{
			int i = index != null ? index : position;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				data[i + 0] = (byte)((value & 0x000000ff) >> 0);
				data[i + 1] = (byte)((value & 0x0000ff00) >> 8);
				data[i + 2] = (byte)((value & 0x00ff0000) >> 16);
				data[i + 3] = (byte)((value & 0xff000000) >> 24);
			}
			else // BIG_ENDIAN
			{
				data[i + 0] = (byte)((value & 0xff000000) >> 24);
				data[i + 1] = (byte)((value & 0x00ff0000) >> 16);
				data[i + 2] = (byte)((value & 0x0000ff00) >> 8);
				data[i + 3] = (byte)((value & 0x000000ff) >> 0);
			}
			if (index == null)
				position += 4;
		}
		
		/**
		 * Gets an integer value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 4.
		 * @param index the source index (or null for current position).
		 * @return the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public int getInteger(Integer index)
		{
			int i = index != null ? index : position;
			int out = 0;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				out |= (data[i + 0] & 0x0ff) << 0;
				out |= (data[i + 1] & 0x0ff) << 8;
				out |= (data[i + 2] & 0x0ff) << 16;
				out |= (data[i + 3] & 0x0ff) << 24;
			}
			else // BIG_ENDIAN
			{
				out |= (data[i + 0] & 0x0ff) << 24;
				out |= (data[i + 1] & 0x0ff) << 16;
				out |= (data[i + 2] & 0x0ff) << 8;
				out |= (data[i + 3] & 0x0ff) << 0;
			}
			if (index == null)
				position += 4;
			return out;
		}
		
		/**
		 * Sets an unsigned integer value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 4.
		 * @param index the destination index (or null for current position).
		 * @param value the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public void putUnsignedInteger(Integer index, long value)
		{
			int i = index != null ? index : position;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				data[i + 0] = (byte)((value & 0x0000000ffL) >> 0);
				data[i + 1] = (byte)((value & 0x00000ff00L) >> 8);
				data[i + 2] = (byte)((value & 0x000ff0000L) >> 16);
				data[i + 3] = (byte)((value & 0x0ff000000L) >> 24);
			}
			else // BIG_ENDIAN
			{
				data[i + 0] = (byte)((value & 0x0ff000000L) >> 24);
				data[i + 1] = (byte)((value & 0x000ff0000L) >> 16);
				data[i + 2] = (byte)((value & 0x00000ff00L) >> 8);
				data[i + 3] = (byte)((value & 0x0000000ffL) >> 0);
			}
			if (index == null)
				position += 4;
		}
		
		/**
		 * Gets an unsigned integer value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 4.
		 * @param index the source index (or null for current position).
		 * @return the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public long getUnsignedInteger(Integer index)
		{
			int i = index != null ? index : position;
			long out = 0L;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				out |= (data[i + 0] & 0x0ffL) << 0;
				out |= (data[i + 1] & 0x0ffL) << 8;
				out |= (data[i + 2] & 0x0ffL) << 16;
				out |= (data[i + 3] & 0x0ffL) << 24;
			}
			else // BIG_ENDIAN
			{
				out |= (data[i + 0] & 0x0ffL) << 24;
				out |= (data[i + 1] & 0x0ffL) << 16;
				out |= (data[i + 2] & 0x0ffL) << 8;
				out |= (data[i + 3] & 0x0ffL) << 0;
			}
			if (index == null)
				position += 4;
			return out;
		}
		
		/**
		 * Sets a float value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 4.
		 * @param index the destination index (or null for current position).
		 * @param value the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public void putFloat(Integer index, float value)
		{
			putInteger(index, Float.floatToRawIntBits(value));
		}
		
		/**
		 * Gets a float value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 4.
		 * @param index the source index (or null for current position).
		 * @return the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public float getFloat(Integer index)
		{
			return Float.intBitsToFloat(getInteger(index));
		}
		
		/**
		 * Sets a long value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 8.
		 * @param index the destination index (or null for current position).
		 * @param value the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public void putLong(Integer index, long value)
		{
			int i = index != null ? index : position;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				data[i + 0] = (byte)((value & 0x00000000000000ffL) >> 0);
				data[i + 1] = (byte)((value & 0x000000000000ff00L) >> 8);
				data[i + 2] = (byte)((value & 0x0000000000ff0000L) >> 16);
				data[i + 3] = (byte)((value & 0x00000000ff000000L) >> 24);
				data[i + 4] = (byte)((value & 0x000000ff00000000L) >> 32);
				data[i + 5] = (byte)((value & 0x0000ff0000000000L) >> 40);
				data[i + 6] = (byte)((value & 0x00ff000000000000L) >> 48);
				data[i + 7] = (byte)((value & 0xff00000000000000L) >> 56);
			}
			else // BIG_ENDIAN
			{
				data[i + 0] = (byte)((value & 0xff00000000000000L) >> 56);
				data[i + 1] = (byte)((value & 0x00ff000000000000L) >> 48);
				data[i + 2] = (byte)((value & 0x0000ff0000000000L) >> 40);
				data[i + 3] = (byte)((value & 0x000000ff00000000L) >> 32);
				data[i + 4] = (byte)((value & 0x00000000ff000000L) >> 24);
				data[i + 5] = (byte)((value & 0x0000000000ff0000L) >> 16);
				data[i + 6] = (byte)((value & 0x000000000000ff00L) >> 8);
				data[i + 7] = (byte)((value & 0x00000000000000ffL) >> 0);
			}
			if (index == null)
				position += 8;
		}
		
		/**
		 * Gets a long value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 8.
		 * @param index the source index (or null for current position).
		 * @return the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public long getLong(Integer index)
		{
			int i = index != null ? index : position;
			long out = 0;
			if (order == ByteOrder.LITTLE_ENDIAN)
			{
				out |= (data[i + 0] & 0x0ffL) << 0;
				out |= (data[i + 1] & 0x0ffL) << 8;
				out |= (data[i + 2] & 0x0ffL) << 16;
				out |= (data[i + 3] & 0x0ffL) << 24;
				out |= (data[i + 4] & 0x0ffL) << 32;
				out |= (data[i + 5] & 0x0ffL) << 40;
				out |= (data[i + 6] & 0x0ffL) << 48;
				out |= (data[i + 7] & 0x0ffL) << 56;
			}
			else // BIG_ENDIAN
			{
				out |= (data[i + 0] & 0x0ffL) << 56;
				out |= (data[i + 1] & 0x0ffL) << 48;
				out |= (data[i + 2] & 0x0ffL) << 40;
				out |= (data[i + 3] & 0x0ffL) << 32;
				out |= (data[i + 4] & 0x0ffL) << 24;
				out |= (data[i + 5] & 0x0ffL) << 16;
				out |= (data[i + 6] & 0x0ffL) << 8;
				out |= (data[i + 7] & 0x0ffL) << 0;
			}
			if (index == null)
				position += 8;
			return out;
		}
		
		/**
		 * Sets a double value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 8.
		 * @param index the destination index (or null for current position).
		 * @param value the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public void putDouble(Integer index, double value)
		{
			putLong(index, Double.doubleToRawLongBits(value));
		}
		
		/**
		 * Gets a double value.
		 * Pays attention to current byte order.
		 * If null is passed in as the index, the buffer's cursor position is advanced by 8.
		 * @param index the source index (or null for current position).
		 * @return the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public double getDouble(Integer index)
		{
			return Double.longBitsToDouble(getLong(index));
		}

		/**
		 * Sets a string value.
		 * If null is passed in as the index, the buffer's cursor position is advanced by the amount of bytes written.
		 * @param index the destination index (or null for current position).
		 * @param charset the encoding charset.
		 * @param value the value.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public void putString(Integer index, Charset charset, String value)
		{
			byte[] bytes = value.getBytes(charset);
			readBytes(index, bytes, 0, bytes.length);
		}
		
		/**
		 * Sets a string value.
		 * If null is passed in as the index, the buffer's cursor position is advanced by the length.
		 * @param index the destination index (or null for current position).
		 * @param charset the encoding charset.
		 * @param length the amount of bytes to use to create a string.
		 * @return the decoded string.
		 * @throws ArrayIndexOutOfBoundsException if the index is out-of-bounds. 
		 */
		public String getString(Integer index, Charset charset, int length)
		{
			int i = index != null ? index : position;
			String out = new String(data, i, length, charset);
			if (index == null)
				position += length;
			return out;
		}

		/**
		 * @return the size of this buffer in bytes. 
		 */
		public int size()
		{
			return data.length;
		}
	
		@Override
		public String toString() 
		{
			StringBuilder sb = new StringBuilder();
			sb.append(order == ByteOrder.BIG_ENDIAN ? "BE" : "LE").append(':').append('@').append(position).append(':');
			sb.append('[');
			for (int i = 0; i < data.length; i++)
			{
				byte b = data[i];
				sb.append(BYTEALPHABET.charAt((b & 0x0f0) >> 4));
				sb.append(BYTEALPHABET.charAt(b & 0x00f));
				if (i < data.length - 1)
					sb.append(' ');
			}
			sb.append(']');
			return sb.toString();
		}
	}
	
	/**
	 * The class used for a list/set.
	 */
	public static class ListType implements Iterable<ScriptValue>
	{
		private ScriptValue[] data;
		private int size;
		
		private ListType(int size, int capacity)
		{
			this.data = null;
			this.size = 0;
			setCapacity(capacity);
			setSize(size);
		}
		
		/**
		 * Sets the size of the list.
		 * @param size the new size.
		 */
		public void setSize(int size)
		{
			if (size < 0)
				size = 0;
			
			int oldSize = this.size;
			this.size = size;
			if (size > oldSize)
			{
				int end = data.length;
				if (size > data.length)
					setCapacity(size);
				for (int i = oldSize; i < end; i++)
					data[i].setNull();
			}
			else if (size < oldSize)
			{
				for (int i = oldSize; i >= size; i--)
					data[i].setNull();
			}
		}
		
		void setCapacity(int capacity)
		{
			if (data != null)
			{
				if (capacity > data.length)
				{
					ScriptValue[] newList = new ScriptValue[capacity];
					System.arraycopy(data, 0, newList, 0, data.length);
					for (int i = data.length; i < newList.length; i++)
						newList[i] = ScriptValue.create(null);
					this.data = newList; 
				}
				else if (capacity < data.length)
				{
					ScriptValue[] newList = new ScriptValue[capacity];
					System.arraycopy(data, 0, newList, 0, data.length);
					this.data = newList;
					this.size = data.length;
				}
				// else no change
			}
			else 
			{
				ScriptValue[] newList = new ScriptValue[capacity];
				for (int i = 0; i < newList.length; i++)
					newList[i] = ScriptValue.create(null);
				this.data = newList;
			}
		}

		/**
		 * Adds an item to the list and expands its size by one.
		 * @param value the value to add.
		 */
		public void add(ScriptValue value)
		{
			add(size, value);
		}
		
		/**
		 * Adds an item to the list and expands its size by one.
		 * @param index the index to add it to.
		 * @param value the value to add.
		 */
		public void add(int index, ScriptValue value)
		{
			if (index < 0)
				index = 0;
			else if (index > size)
				index = size;
			
			if (size >= data.length)
				setCapacity(data.length * 2);
			
			ScriptValue sv = data[size];
			for (int i = size; i > index; i--)
				data[i] = data[i - 1];
			data[index] = sv;
			sv.set(value);
			size++;
		}
		
		/**
		 * Finds a script value sequentially (strict equals).
		 * @param value the values.
		 * @return the index of the found value or -1 if not found.
		 */
		public int indexOf(ScriptValue value)
		{
			for (int i = 0; i < size; i++)
				if (value.equals(data[i]))
					return i;
			return -1;
		}
		
		/**
		 * Finds a script value sequentially, in reverse (strict equals).
		 * @param value the values.
		 * @return the index of the found value or -1 if not found.
		 */
		public int lastIndexOf(ScriptValue value)
		{
			for (int i = size - 1; i >= 0; i--)
				if (value.equals(data[i]))
					return i;
			return -1;
		}
		
		/**
		 * Removes an object at an index.
		 * @param index the index.
		 * @param out the output value, set to the removed value.
		 * @return true if a value was removed, false if not (index was out of range).
		 */
		public boolean removeIndex(int index, ScriptValue out)
		{
			ScriptValue sv = data[index];
			if (index < 0 || index >= size)
			{
				out.setNull();
				return false;
			}
			for (int i = index; i < size - 1; i++)
				data[i] = data[i + 1];
			data[--size] = sv;
			out.set(sv);
			sv.setNull();
			return true;
		}

		/**
		 * Finds an object and removes it from the list.
		 * @param value the value to remove.
		 * @return true if removed, false if not.
		 */
		public boolean remove(ScriptValue value)
		{
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				int idx = indexOf(value);
				if (idx >= 0)
				{
					removeIndex(idx, temp);
					return true;
				}
				return false;
			}
			finally
			{
				temp.setNull();
			}
		}
		
		/**
		 * Gets a value from the list at an index.
		 * @param index the provided index.
		 * @param out the output value, set to the desired value.
		 */
		public void get(int index, ScriptValue out)
		{
			out.set(Utils.arrayElement(data, index));
		}
		
		/**
		 * Sets a value from the list at an index.
		 * @param index the provided index.
		 * @param value the value to set.
		 */
		public void set(int index, Object value)
		{
			ScriptValue sv = Utils.arrayElement(data, index);
			if (sv != null)
				sv.set(value);
		}
		
		/**
		 * Sorts this array.
		 */
		public void sort()
		{
			Utils.quicksort(data, 0, size - 1);
		}
		
		/**
		 * Binary searches for a value.
		 * @param value the value to search for.
		 * @return the index of the found value or &lt; 0.
		 */
		public int search(ScriptValue value)
		{
			return Arrays.binarySearch(data, 0, size, value, Comparator.naturalOrder());
		}
		
		/**
		 * @return the size of this list in items. 
		 */
		public int size()
		{
			return size;
		}
		
		/**
		 * @return true if this list is empty, false otherwise.
		 */
		public boolean isEmpty()
		{
			return size() == 0;
		}
		
		@Override
		public Iterator<ScriptValue> iterator()
		{
			return new ListTypeIterator();
		}
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (int i = 0; i < size; i++)
			{
				sb.append(data[i].toString());
				if (i < size - 1)
					sb.append(", ");
			}
			sb.append(']');
			return sb.toString();
		}
		
		private class ListTypeIterator implements Iterator<ScriptValue>
		{
			private int cur = 0;
			private boolean removed = false;

			@Override
			public boolean hasNext()
			{
				return cur < size;
			}

			@Override
			public ScriptValue next()
			{
				removed = false;
				return data[cur++];
			}
		
			@Override
			public void remove()
			{
				ScriptValue temp = CACHEVALUE2.get();
				try
				{
					if (removed)
						return;
					
					removeIndex(cur - 1, temp);
					removed = true;
					cur--;
				}
				finally
				{
					temp.setNull();
				}
			}
		}
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
		public Iterator<Entry> iterator()
		{
			return new MapTypeIterator();
		}

		private class MapTypeIterator implements Iterator<Entry>
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
				
				removeIndex(cur - 1);
				removed = true;
				cur--;
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
		 * @return a new ErrorType.
		 */
		public static ErrorType create(String type, String message)
		{
			return create(type, message, message);
		}
		
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
