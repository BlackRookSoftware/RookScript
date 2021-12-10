/*******************************************************************************
 * Copyright (c) 2017-2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Comparator;

import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;
import com.blackrook.rookscript.struct.TypeProfileFactory.Profile;

/**
 * Utility functions.
 * @author Matthew Tropiano
 */
public final class Utils
{	
	/** Are we using Windows? */
	private static boolean IS_WINDOWS = false;

	private static final TypeProfileFactory DEFAULT_PROFILEFACTORY = new TypeProfileFactory(new TypeProfileFactory.MemberPolicy()
	{
		@Override
		public boolean isIgnored(Field field)
		{
			return field.getAnnotation(ScriptIgnore.class) != null;
		}

		@Override
		public boolean isIgnored(Method method)
		{
			return method.getAnnotation(ScriptIgnore.class) != null;
		}

		@Override
		public String getAlias(Field field)
		{
			ScriptName anno = field.getAnnotation(ScriptName.class);
			return anno != null ? anno.value() : null;
		}

		@Override
		public String getAlias(Method method)
		{
			ScriptName anno = method.getAnnotation(ScriptName.class);
			return anno != null ? anno.value() : null;
		}

	});
	
	private static final TypeConverter DEFAULT_CONVERTER = new TypeConverter(DEFAULT_PROFILEFACTORY);

	private static final ThreadLocal<Object[]> BLANK_PARAM_ARRAY = ThreadLocal.withInitial(()->new Object[]{});

	/**
	 * A null input stream (never has data to read).
	 */
	public static final InputStream NULL_INPUT = new InputStream() 
	{
		@Override
		public int read() throws IOException 
		{
			return -1;
		}
	};
	
	/**
	 * A null output stream (eats all data written).
	 */
	public static final OutputStream NULL_OUTPUT = new OutputStream() 
	{
		@Override
		public void write(int b) throws IOException
		{
			// Do nothing.
		}
	};
	
	static
	{
		String osName = System.getProperty("os.name");
		IS_WINDOWS = osName.contains("Windows");
	}

	/** @return true if we using Windows. */
	public static boolean isWindows()
	{
		return IS_WINDOWS;
	}

	/**
	 * Checks if a value is "empty."
	 * The following is considered "empty":
	 * <ul>
	 * <li><i>Null</i> references.
	 * <li>{@link Array} objects that have a length of 0.
	 * <li>{@link Boolean} objects that are false.
	 * <li>{@link Character} objects that are the null character ('\0', '\u0000').
	 * <li>{@link Number} objects that are zero.
	 * <li>{@link String} objects that are the empty string, or are {@link String#trim()}'ed down to the empty string.
	 * <li>{@link Collection} objects where {@link Collection#isEmpty()} returns true.
	 * </ul> 
	 * @param obj the object to check.
	 * @return true if the provided object is considered "empty", false otherwise.
	 */
	public static boolean isEmpty(Object obj)
	{
		if (obj == null)
			return true;
		else if (isArray(obj.getClass()))
			return Array.getLength(obj) == 0;
		else if (obj instanceof Boolean)
			return !((Boolean)obj);
		else if (obj instanceof Character)
			return ((Character)obj) == '\0';
		else if (obj instanceof Number)
			return ((Number)obj).doubleValue() == 0.0;
		else if (obj instanceof String)
			return ((String)obj).trim().length() == 0;
		else if (obj instanceof ScriptValue)
			return ((ScriptValue)obj).empty();
		else if (obj instanceof Collection<?>)
			return ((Collection<?>)obj).isEmpty();
		else
			return false;
	}

	/**
	 * Returns the first object if it is not null, otherwise returns the second. 
	 * @param <T> class that extends Object.
	 * @param testObject the first ("tested") object.
	 * @param nullReturn the object to return if testObject is null.
	 * @return testObject if not null, nullReturn otherwise.
	 */
	public static <T> T isNull(T testObject, T nullReturn)
	{
		return testObject != null ? testObject : nullReturn;
	}

	/**
	 * Converts radians to degrees.
	 * @param radians the input angle in radians.
	 * @return the resultant angle in degrees.
	 */
	public static double radToDeg(double radians)
	{
		return radians * (180/Math.PI);
	}

	/**
	 * Converts degrees to radians.
	 * @param degrees the input angle in degrees.
	 * @return the resultant angle in radians.
	 */
	public static double degToRad(double degrees)
	{
		return (degrees * Math.PI)/180;
	}

	/**
	 * Coerces a double to the range bounded by lo and hi.
	 * <br>Example: clampValue(32,-16,16) returns 16.
	 * <br>Example: clampValue(4,-16,16) returns 4.
	 * <br>Example: clampValue(-1000,-16,16) returns -16.
	 * @param val the double.
	 * @param lo the lower bound.
	 * @param hi the upper bound.
	 * @return the value after being "forced" into the range.
	 */
	public static double clampValue(double val, double lo, double hi)
	{
		return Math.min(Math.max(val,lo),hi);
	}

	/**
	 * Coerces a double to the range bounded by lo and hi, by "wrapping" the value.
	 * <br>Example: wrapValue(32,-16,16) returns 0.
	 * <br>Example: wrapValue(4,-16,16) returns 4.
	 * <br>Example: wrapValue(-1000,-16,16) returns 8.
	 * @param val the double.
	 * @param lo the lower bound.
	 * @param hi the upper bound.
	 * @return the value after being "wrapped" into the range.  
	 */
	public static double wrapValue(double val, double lo, double hi)
	{
		double range = hi - lo;
		val = val - lo;
		val = (val % range);
		if (val < 0.0)
			val = val + hi;
		return val;
	}

	/**
	 * Gives a value that is the result of a linear interpolation between two values.
	 * @param factor the interpolation factor.
	 * @param x the first value.
	 * @param y the second value.
	 * @return the interpolated value.
	 */
	public static double linearInterpolate(double factor, double x, double y)
	{
		return factor * (y - x) + x;
	}

	/**
	 * Attempts to parse a long from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted long integer or def if the input string is blank.
	 */
	public static long parseLong(String s, long def)
	{
		if (isEmpty(s))
			return def;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/**
	 * Attempts to parse a double from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted double or def if the input string is blank.
	 */
	public static double parseDouble(String s, double def)
	{
		if (isEmpty(s))
			return def;
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	/**
	 * Sets the value of a field on an object.
	 * @param instance the object instance to set the field on.
	 * @param field the field to set.
	 * @param value the value to set.
	 * @throws NullPointerException if the field or object provided is null.
	 * @throws ClassCastException if the value could not be cast to the proper type.
	 * @throws RuntimeException if anything goes wrong (bad field name, 
	 * bad target, bad argument, or can't access the field).
	 * @see Field#set(Object, Object)
	 */
	public static void setFieldValue(Object instance, Field field, Object value)
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

	/**
	 * Gets the value of a field on an object.
	 * @param instance the object instance to get the field value of.
	 * @param field the field to get the value of.
	 * @return the current value of the field.
	 * @throws NullPointerException if the field or object provided is null.
	 * @throws RuntimeException if anything goes wrong (bad target, bad argument, 
	 * or can't access the field).
	 * @see Field#set(Object, Object)
	 */
	public static Object getFieldValue(Object instance, Field field)
	{
		try {
			return field.get(instance);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Blindly invokes a method, with no parameters, only throwing a {@link RuntimeException} if
	 * something goes wrong. Here for the convenience of not making a billion
	 * try/catch clauses for a method invocation.
	 * This method exists to avoid unnecessary memory allocations.
	 * @param method the method to invoke.
	 * @param instance the object instance that is the method target.
	 * @return the return value from the method invocation. If void, this is null.
	 * @throws ClassCastException if one of the parameters could not be cast to the proper type.
	 * @throws RuntimeException if anything goes wrong (bad target, bad argument, or can't access the method).
	 * @see Method#invoke(Object, Object...)
	 */
	public static Object invokeBlind(Method method, Object instance)
	{
		return invokeBlind(method, instance, BLANK_PARAM_ARRAY.get());
	}

	/**
	 * Blindly invokes a method, only throwing a {@link RuntimeException} if
	 * something goes wrong. Here for the convenience of not making a billion
	 * try/catch clauses for a method invocation.
	 * @param method the method to invoke.
	 * @param instance the object instance that is the method target.
	 * @param params the parameters to pass to the method.
	 * @return the return value from the method invocation. If void, this is null.
	 * @throws ClassCastException if one of the parameters could not be cast to the proper type.
	 * @throws RuntimeException if anything goes wrong (bad target, bad argument, or can't access the method).
	 * @see Method#invoke(Object, Object...)
	 */
	public static Object invokeBlind(Method method, Object instance, Object ... params)
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

	/**
	 * Concatenates a set of arrays together, such that the contents of each
	 * array are joined into one array. Null arrays are skipped.
	 * @param <T> the object type stored in the arrays.
	 * @param arrays the list of arrays.
	 * @return a new array with all objects in each provided array added 
	 * to the resultant one in the order in which they appear.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] joinArrays(T[]...  arrays)
	{
		int totalLen = 0;
		for (T[] a : arrays)
			if (a != null)
				totalLen += a.length;
		
		Class<?> type = getArrayType(arrays);
		T[] out = (T[])Array.newInstance(type, totalLen);
		
		int offs = 0;
		for (T[] a : arrays)
		{
			System.arraycopy(a, 0, out, offs, a.length);
			offs += a.length;
		}
		
		return out;
	}

	/**
	 * Gets the element at an index in the array, but returns 
	 * null if the index is outside of the array bounds.
	 * @param <T> the array type.
	 * @param array the array to use.
	 * @param index the index to use.
	 * @return <code>array[index]</code> or null if out of bounds.
	 */
	public static <T> T arrayElement(T[] array, int index)
	{
		if (index < 0 || index >= array.length)
			return null;
		else
			return array[index];
	}

	/**
	 * Tests if a class is actually an array type.
	 * @param clazz the class to test.
	 * @return true if so, false if not. 
	 */
	public static boolean isArray(Class<?> clazz)
	{
		return clazz.getName().startsWith("["); 
	}

	/**
	 * Tests if an object is actually an array type.
	 * @param object the object to test.
	 * @return true if so, false if not. 
	 */
	public static boolean isArray(Object object)
	{
		return isArray(object.getClass()); 
	}

	/**
	 * Gets the class type of this array type, if this is an array type.
	 * @param arrayType the type to inspect.
	 * @return this array's type, or null if the provided type is not an array,
	 * or if the found class is not on the classpath.
	 */
	public static Class<?> getArrayType(Class<?> arrayType)
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

	/**
	 * Gets the class type of this array, if this is an array.
	 * @param object the object to inspect.
	 * @return this array's type, or null if the provided object is not an array, or if the found class is not on the classpath.
	 */
	public static Class<?> getArrayType(Object object)
	{
		if (!isArray(object))
			return null;
		
		return getArrayType(object.getClass());
	}

	/**
	 * Gets how many dimensions that this array, represented by the provided type, has.
	 * @param arrayType the type to inspect.
	 * @return the number of array dimensions, or 0 if not an array.
	 */
	public static int getArrayDimensions(Class<?> arrayType)
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

	/**
	 * Gets how many array dimensions that an object (presumably an array) has.
	 * @param array the object to inspect.
	 * @return the number of array dimensions, or 0 if not an array.
	 */
	public static int getArrayDimensions(Object array)
	{
		if (!isArray(array))
			return 0;
			
		return getArrayDimensions(array.getClass());
	}

	/**
	 * Opens an {@link InputStream} to a resource using the current thread's {@link ClassLoader}.
	 * @param pathString the resource pathname.
	 * @return an open {@link InputStream} for reading the resource or null if not found.
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	public static InputStream openResource(String pathString)
	{
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(pathString);
	}

	/**
	 * Reads from an input stream, reading in a consistent set of data
	 * and writing it to the output stream. The read/write is buffered
	 * so that it does not bog down the OS's other I/O requests.
	 * This method finishes when the end of the source stream is reached.
	 * Note that this may block if the input stream is a type of stream
	 * that will block if the input stream blocks for additional input.
	 * This method is thread-safe.
	 * @param in the input stream to grab data from.
	 * @param out the output stream to write the data to.
	 * @param bufferSize the buffer size for the I/O. Must be &gt; 0.
	 * @param maxLength the maximum amount of bytes to relay, or a value &lt; 0 for no max.
	 * @return the total amount of bytes relayed.
	 * @throws IOException if a read or write error occurs.
	 */
	public static int relay(DataInput in, DataOutput out, int bufferSize, int maxLength) throws IOException
	{
		int total = 0;
		int buf = 0;
			
		byte[] RELAY_BUFFER = new byte[bufferSize];
		
		while ((buf = Utils.read(in, RELAY_BUFFER, 0, Math.min(maxLength < 0 ? Integer.MAX_VALUE : maxLength, bufferSize))) > 0)
		{
			out.write(RELAY_BUFFER, 0, buf);
			total += buf;
			if (maxLength >= 0)
				maxLength -= buf;
		}
		return total;
	}

	/**
	 * Attempts to close an {@link AutoCloseable} object.
	 * If the object is null, this does nothing.
	 * @param c the reference to the AutoCloseable object.
	 */
	public static void close(AutoCloseable c)
	{
		if (c == null) return;
		try { c.close(); } catch (Exception e){}
	}

	/**
	 * Creates a new profile for a provided type.
	 * Generated profiles are stored in memory, and retrieved again by class type.
	 * <p>This method is thread-safe.
	 * @param <T> the class type.
	 * @param clazz the class.
	 * @return a new profile.
	 */
	public static <T> Profile<T> getProfile(Class<T> clazz)
	{
		return DEFAULT_PROFILEFACTORY.getProfile(clazz);
	}

	/**
	 * Creates a new instance of a class from a class type.
	 * This essentially calls {@link Class#newInstance()}, but wraps the call
	 * in a try/catch block that only throws an exception if something goes wrong.
	 * @param <T> the return object type.
	 * @param clazz the class type to instantiate.
	 * @return a new instance of an object.
	 * @throws RuntimeException if instantiation cannot happen, either due to
	 * a non-existent constructor or a non-visible constructor.
	 */
	public static <T> T create(Class<T> clazz)
	{
		Object out = null;
		try {
			out = clazz.getDeclaredConstructor().newInstance();
		} catch (SecurityException ex) {
			throw new RuntimeException(ex);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		
		return clazz.cast(out);
	}

	/**
	 * Creates a new instance of a class from a class type.
	 * This essentially calls {@link Class#newInstance()}, but wraps the call
	 * in a try/catch block that only throws an exception if something goes wrong.
	 * @param <T> the return object type.
	 * @param constructor the constructor to call.
	 * @param params the constructor parameters.
	 * @return a new instance of an object created via the provided constructor.
	 * @throws RuntimeException if instantiation cannot happen, either due to
	 * a non-existent constructor or a non-visible constructor.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T construct(Constructor<T> constructor, Object ... params)
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

	/**
	 * Creates a new instance of an object for placement in a POJO or elsewhere.
	 * @param <T> the return object type.
	 * @param object the object to convert to another object
	 * @param targetType the target class type to convert to, if the types differ.
	 * @return a suitable object of type <code>targetType</code>. 
	 * @throws ClassCastException if the incoming type cannot be converted.
	 */
	public static <T> T createForType(Object object, Class<T> targetType)
	{
		return DEFAULT_CONVERTER.createForType("source", object, targetType);
	}

	/**
	 * Swaps the contents of two indices of an array.
	 * @param <T> the object type stored in the array.
	 * @param array the input array.
	 * @param a the first index.
	 * @param b the second index.
	 */
	public static <T> void arraySwap(T[] array, int a, int b)
	{
		T temp = array[a];
		array[a] = array[b];
		array[b] = temp;
	}

	/**
	 * Shifts an object to an appropriate position according to the object's {@link Comparable#compareTo(Object)} function.
	 * @param <T> the object type stored in the array that extends {@link Comparable}.
	 * @param array the array to shift the contents of.
	 * @param index the index to add it to (the contents are replaced).
	 * @return the final index in the array of the sorted object.
	 */
	public static <T extends Comparable<T>> int sortFrom(T[] array, int index)
	{
		while (index > 0 && array[index].compareTo(array[index - 1]) < 0)
		{
			arraySwap(array, index, index - 1);
			index--;
		}
		return index;
	}

	/**
	 * Shifts an object to an appropriate position according to the provided <code>comparator</code> function.
	 * @param <T> the object type stored in the arrays.
	 * @param array the array to shift the contents of.
	 * @param index the index to add it to (the contents are replaced).
	 * @param comparator the comparator to use.
	 * @return the final index in the array of the sorted object.
	 */
	public static <T> int sortFrom(T[] array, int index, Comparator<? super T> comparator)
	{
		while (index > 0 && comparator.compare(array[index], array[index - 1]) < 0)
		{
			arraySwap(array, index, index - 1);
			index--;
		}
		return index;
	}

	/**
	 * Performs an in-place QuickSort on the provided array.
	 * The array's contents will change upon completion.
	 * Convenience method for <code>quicksort(array, 0, array.length - 1);</code>
	 * @param <T> the object type stored in the array that extends {@link Comparable}.
	 * @param array the input array.
	 */
	public static <T extends Comparable<T>> void quicksort(T[] array)
	{
		quicksort(array, 0, array.length - 1);
	}

	/**
	 * Performs an in-place QuickSort on the provided array using a compatible Comparator.
	 * The array's contents will change upon completion.
	 * Convenience method for <code>quicksort(array, 0, array.length - 1, comparator);</code>
	 * @param <T> the object type stored in the array.
	 * @param array the input array.
	 * @param comparator the comparator to use for comparing.
	 */
	public static <T> void quicksort(T[] array, Comparator<? super T> comparator)
	{
		quicksort(array, 0, array.length - 1, comparator);
	}

	/**
	 * Performs an in-place QuickSort on the provided array within an interval of indices.
	 * The array's contents will change upon completion.
	 * If <code>lo</code> is greater than <code>hi</code>, this does nothing. 
	 * @param <T> the object type stored in the array that extends {@link Comparable}.
	 * @param array the input array.
	 * @param lo the low index to start the sort (inclusive).
	 * @param hi the high index to start the sort (inclusive).
	 */
	public static <T extends Comparable<T>> void quicksort(T[] array, int lo, int hi)
	{
		if (lo >= hi)
			return;
	    int p = quicksortPartition(array, lo, hi);
	    quicksort(array, lo, p - 1);
	    quicksort(array, p + 1, hi);
	}

	/**
	 * Performs an in-place QuickSort on the provided array within an interval of indices.
	 * The array's contents will change upon completion.
	 * If <code>lo</code> is greater than <code>hi</code>, this does nothing. 
	 * @param <T> the object type stored in the array.
	 * @param array the input array.
	 * @param lo the low index to start the sort (inclusive).
	 * @param hi the high index to start the sort (inclusive).
	 * @param comparator the comparator to use for comparing.
	 */
	public static <T> void quicksort(T[] array, int lo, int hi, Comparator<? super T> comparator)
	{
		if (lo >= hi)
			return;
	    int p = quicksortPartition(array, lo, hi, comparator);
	    quicksort(array, lo, p - 1, comparator);
	    quicksort(array, p + 1, hi, comparator);
	}

	// Do quicksort partition - pivot sort.
	private static <T extends Comparable<T>> int quicksortPartition(T[] array, int lo, int hi)
	{
		T pivot = array[hi];
	    int i = lo;
	    for (int j = lo; j <= hi - 1; j++)
	    {
	        if (array[j].compareTo(pivot) <= 0)
	        {
	        	arraySwap(array, i, j);
	            i++;
	        }
	    }
		arraySwap(array, i, hi);
	    return i;
	}

	// Do quicksort partition - pivot sort.
	private static <T> int quicksortPartition(T[] array, int lo, int hi, Comparator<? super T> comparator)
	{
		T pivot = array[hi];
	    int i = lo;
	    for (int j = lo; j <= hi - 1; j++)
	    {
	        if (comparator.compare(array[j], pivot) <= 0)
	        {
	        	arraySwap(array, i, j);
	            i++;
	        }
	    }
		arraySwap(array, i, hi);
	    return i;
	}

	/**
	 * Returns the extension of a filename.
	 * @param filename the file name.
	 * @param extensionSeparator the text or characters that separates file name from extension.
	 * @return the file's extension, or an empty string for no extension.
	 */
	public static String getFileExtension(String filename, String extensionSeparator)
	{
		int extindex = filename.lastIndexOf(extensionSeparator);
		if (extindex >= 0)
			return filename.substring(extindex+1);
		return "";
	}

	/**
	 * Returns the extension of a file's name.
	 * @param file the file.
	 * @param extensionSeparator the text or characters that separates file name from extension.
	 * @return the file's extension, or an empty string for no extension.
	 */
	public static String getFileExtension(File file, String extensionSeparator)
	{
		return getFileExtension(file.getName(), extensionSeparator);
	}

	/**
	 * Returns the file's name, no extension.
	 * @param file the file.
	 * @param extensionSeparator the text or characters that separates file name from extension.
	 * @return the file's name without extension.
	 */
	public static String getFileNameWithoutExtension(File file, String extensionSeparator)
	{
		return getFileNameWithoutExtension(file.getName(), extensionSeparator);
	}

	/**
	 * Returns the file's name, no extension.
	 * @param filename the file name.
	 * @param extensionSeparator the text or characters that separates file name from extension.
	 * @return the file's name without extension.
	 */
	public static String getFileNameWithoutExtension(String filename, String extensionSeparator)
	{
		int extindex = filename.lastIndexOf(extensionSeparator);
		if (extindex >= 0)
			return filename.substring(0, extindex);
		return "";
	}

	/**
	 * Puts a short into an array.
	 * Writes 2 bytes.
	 * @param value the value to convert.
	 * @param order the byte ordering.
	 * @param data the output array.
	 * @param offset the offset into the array to write.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 2</code> exceeds <code>data.length</code>.
	 */
	public static void putShort(short value, ByteOrder order, byte[] data, int offset)
	{
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			data[offset] =     (byte)((value & 0x000ff));
			data[offset + 1] = (byte)((value & 0x0ff00) >> 8);
		}
		else // BIG_ENDIAN
		{
			data[offset] =     (byte)((value & 0x0ff00) >> 8);
			data[offset + 1] = (byte)((value & 0x000ff));
		}
	}
	
	/**
	 * Reads a set of bytes until end-of-stream or file.
	 * @param input the DataInput to read from.
	 * @return the byte read (0 - 255), or -1 if end-of-stream/file at time of call.
	 * @throws IOException if a read error occurs.
	 */
	public static int read(DataInput input) throws IOException
	{
		int out;
		try {
			out = input.readUnsignedByte();
		} catch (EOFException e) {
			return -1;
		}
		return out;
	}
	
	/**
	 * Reads a set of bytes until end-of-stream or file.
	 * @param input the DataInput to read from.
	 * @param data the recipient array for the byte data.
	 * @param offset the starting offset into the array to put the bytes.
	 * @param length the maximum amount of bytes to read.
	 * @return the amount of bytes read, or -1 if end-of-stream/file at time of call.
	 * @throws IOException if a read error occurs.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + length</code> breaches the end of the data array.
	 */
	public static int read(DataInput input, byte[] data, int offset, int length) throws IOException
	{
		int i = offset;
		int out = 0;
		while (i < length)
		{
			int b = read(input);
			if (b < 0)
				break;
			data[i++] = (byte)b;
			out++;
		}
		if (out == 0)
			return -1;
		return out;
	}
	
	/**
	 * Gets a short from an array.
	 * Reads 2 bytes.
	 * @param order the byte ordering.
	 * @param data the input array.
	 * @param offset the offset into the array to read.
	 * @return the resultant short.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 2</code> exceeds <code>data.length</code>.
	 */
	public static short getShort(ByteOrder order, byte[] data, int offset)
	{
		short out = 0;
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			out |= (data[offset]     & 0x0ff);
			out |= (data[offset + 1] & 0x0ff) << 8;
		}
		else // BIG_ENDIAN
		{
			out |= (data[offset]     & 0x0ff) << 8;
			out |= (data[offset + 1] & 0x0ff);
		}
		return out;
	}
	
	/**
	 * Puts an unsigned short into an array.
	 * Writes 2 bytes.
	 * @param value the value to convert.
	 * @param order the byte ordering.
	 * @param data the output array.
	 * @param offset the offset into the array to write.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 2</code> exceeds <code>data.length</code>.
	 */
	public static void putUnsignedShort(int value, ByteOrder order, byte[] data, int offset)
	{
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			data[offset] =     (byte)((value & 0x000ff));
			data[offset + 1] = (byte)((value & 0x0ff00) >> 8);
		}
		else // BIG_ENDIAN
		{
			data[offset] =     (byte)((value & 0x0ff00) >> 8);
			data[offset + 1] = (byte)((value & 0x000ff));
		}
	}
	
	/**
	 * Gets an unsigned short from an array.
	 * Reads 2 bytes.
	 * @param order the byte ordering.
	 * @param data the input array.
	 * @param offset the offset into the array to read.
	 * @return the resultant short.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 2</code> exceeds <code>data.length</code>.
	 */
	public static int getUnsignedShort(ByteOrder order, byte[] data, int offset)
	{
		int out = 0;
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			out |= (data[offset]     & 0x0ff);
			out |= (data[offset + 1] & 0x0ff) << 8;
		}
		else // BIG_ENDIAN
		{
			out |= (data[offset]     & 0x0ff) << 8;
			out |= (data[offset + 1] & 0x0ff);
		}
		return out;
	}
	
	/**
	 * Puts an integer into an array.
	 * Writes 4 bytes.
	 * @param value the value to convert.
	 * @param order the byte ordering.
	 * @param data the output array.
	 * @param offset the offset into the array to write.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 4</code> exceeds <code>data.length</code>.
	 */
	public static void putInteger(int value, ByteOrder order, byte[] data, int offset)
	{
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			data[offset] =     (byte)((value & 0x000000ff));
			data[offset + 1] = (byte)((value & 0x0000ff00) >> 8);
			data[offset + 2] = (byte)((value & 0x00ff0000) >> 16);
			data[offset + 3] = (byte)((value & 0xff000000) >> 24);
		}
		else // BIG_ENDIAN
		{
			data[offset] =     (byte)((value & 0xff000000) >> 24);
			data[offset + 1] = (byte)((value & 0x00ff0000) >> 16);
			data[offset + 2] = (byte)((value & 0x0000ff00) >> 8);
			data[offset + 3] = (byte)((value & 0x000000ff));
		}
	}
	
	/**
	 * Gets an integer from an array.
	 * Reads 4 bytes.
	 * @param order the byte ordering.
	 * @param data the input array.
	 * @param offset the offset into the array to read.
	 * @return the resultant integer.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 4</code> exceeds <code>data.length</code>.
	 */
	public static int getInteger(ByteOrder order, byte[] data, int offset)
	{
		int out = 0;
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			out |= (data[offset]     & 0x0ff);
			out |= (data[offset + 1] & 0x0ff) << 8;
			out |= (data[offset + 2] & 0x0ff) << 16;
			out |= (data[offset + 3] & 0x0ff) << 24;
		}
		else // BIG_ENDIAN
		{
			out |= (data[offset]     & 0x0ff) << 24;
			out |= (data[offset + 1] & 0x0ff) << 16;
			out |= (data[offset + 2] & 0x0ff) << 8;
			out |= (data[offset + 3] & 0x0ff);
		}
		return out;
	}
	
	/**
	 * Puts an unsigned integer into an array.
	 * Writes 4 bytes.
	 * @param value the value to convert.
	 * @param order the byte ordering.
	 * @param data the output array.
	 * @param offset the offset into the array to write.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 4</code> exceeds <code>data.length</code>.
	 */
	public static void putUnsignedInteger(long value, ByteOrder order, byte[] data, int offset)
	{
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			data[offset] =     (byte)((value & 0x0000000ffL));
			data[offset + 1] = (byte)((value & 0x00000ff00L) >> 8);
			data[offset + 2] = (byte)((value & 0x000ff0000L) >> 16);
			data[offset + 3] = (byte)((value & 0x0ff000000L) >> 24);
		}
		else // BIG_ENDIAN
		{
			data[offset] =     (byte)((value & 0x0ff000000L) >> 24);
			data[offset + 1] = (byte)((value & 0x000ff0000L) >> 16);
			data[offset + 2] = (byte)((value & 0x00000ff00L) >> 8);
			data[offset + 3] = (byte)((value & 0x0000000ffL));
		}
	}
	
	/**
	 * Gets an unsigned integer from an array.
	 * Reads 4 bytes.
	 * @param order the byte ordering.
	 * @param data the input array.
	 * @param offset the offset into the array to read.
	 * @return the resultant integer.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 4</code> exceeds <code>data.length</code>.
	 */
	public static long getUnsignedInteger(ByteOrder order, byte[] data, int offset)
	{
		long out = 0L;
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			out |= (data[offset]     & 0x0ffL);
			out |= (data[offset + 1] & 0x0ffL) << 8;
			out |= (data[offset + 2] & 0x0ffL) << 16;
			out |= (data[offset + 3] & 0x0ffL) << 24;
		}
		else // BIG_ENDIAN
		{
			out |= (data[offset]     & 0x0ffL) << 24;
			out |= (data[offset + 1] & 0x0ffL) << 16;
			out |= (data[offset + 2] & 0x0ffL) << 8;
			out |= (data[offset + 3] & 0x0ffL);
		}
		return out;
	}
	
	/**
	 * Puts a floating-point number into an array.
	 * Writes 4 bytes.
	 * @param value the value to convert.
	 * @param order the byte ordering.
	 * @param data the output array.
	 * @param offset the offset into the array to write.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 4</code> exceeds <code>data.length</code>.
	 */
	public static void putFloat(float value, ByteOrder order, byte[] data, int offset)
	{
		putInteger(Float.floatToRawIntBits(value), order, data, offset);
	}
	
	/**
	 * Gets a floating-point number from an array.
	 * Reads 4 bytes.
	 * @param order the byte ordering.
	 * @param data the input array.
	 * @param offset the offset into the array to read.
	 * @return the resultant float.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 4</code> exceeds <code>data.length</code>.
	 */
	public static float getFloat(ByteOrder order, byte[] data, int offset)
	{
		return Float.intBitsToFloat(getInteger(order, data, offset));
	}
	
	/**
	 * Puts a long integer into an array.
	 * Writes 8 bytes.
	 * @param value the value to convert.
	 * @param order the byte ordering.
	 * @param data the output array.
	 * @param offset the offset into the array to write.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 8</code> exceeds <code>data.length</code>.
	 */
	public static void putLong(long value, ByteOrder order, byte[] data, int offset)
	{
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			data[offset] =     (byte)((value & 0x00000000000000ffL));
			data[offset + 1] = (byte)((value & 0x000000000000ff00L) >> 8);
			data[offset + 2] = (byte)((value & 0x0000000000ff0000L) >> 16);
			data[offset + 3] = (byte)((value & 0x00000000ff000000L) >> 24);
			data[offset + 4] = (byte)((value & 0x000000ff00000000L) >> 32);
			data[offset + 5] = (byte)((value & 0x0000ff0000000000L) >> 40);
			data[offset + 6] = (byte)((value & 0x00ff000000000000L) >> 48);
			data[offset + 7] = (byte)((value & 0xff00000000000000L) >> 56);
		}
		else // BIG_ENDIAN
		{
			data[offset] =     (byte)((value & 0xff00000000000000L) >> 56);
			data[offset + 1] = (byte)((value & 0x00ff000000000000L) >> 48);
			data[offset + 2] = (byte)((value & 0x0000ff0000000000L) >> 40);
			data[offset + 3] = (byte)((value & 0x000000ff00000000L) >> 32);
			data[offset + 4] = (byte)((value & 0x00000000ff000000L) >> 24);
			data[offset + 5] = (byte)((value & 0x0000000000ff0000L) >> 16);
			data[offset + 6] = (byte)((value & 0x000000000000ff00L) >> 8);
			data[offset + 7] = (byte)((value & 0x00000000000000ffL));
		}
	}
	
	/**
	 * Gets a long integer from an array.
	 * Reads 8 bytes.
	 * @param order the byte ordering.
	 * @param data the input array.
	 * @param offset the offset into the array to read.
	 * @return the resultant integer.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 8</code> exceeds <code>data.length</code>.
	 */
	public static long getLong(ByteOrder order, byte[] data, int offset)
	{
		long out = 0;
		if (order == ByteOrder.LITTLE_ENDIAN)
		{
			out |= (data[offset]     & 0x0ffL);
			out |= (data[offset + 1] & 0x0ffL) << 8;
			out |= (data[offset + 2] & 0x0ffL) << 16;
			out |= (data[offset + 3] & 0x0ffL) << 24;
			out |= (data[offset + 4] & 0x0ffL) << 32;
			out |= (data[offset + 5] & 0x0ffL) << 40;
			out |= (data[offset + 6] & 0x0ffL) << 48;
			out |= (data[offset + 7] & 0x0ffL) << 56;
		}
		else // BIG_ENDIAN
		{
			out |= (data[offset]     & 0x0ffL) << 56;
			out |= (data[offset + 1] & 0x0ffL) << 48;
			out |= (data[offset + 2] & 0x0ffL) << 40;
			out |= (data[offset + 3] & 0x0ffL) << 32;
			out |= (data[offset + 4] & 0x0ffL) << 24;
			out |= (data[offset + 5] & 0x0ffL) << 16;
			out |= (data[offset + 6] & 0x0ffL) << 8;
			out |= (data[offset + 7] & 0x0ffL);
		}
		return out;
	}
	
	/**
	 * Puts a double floating-point number into an array.
	 * Writes 8 bytes.
	 * @param value the value to convert.
	 * @param order the byte ordering.
	 * @param data the output array.
	 * @param offset the offset into the array to write.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 8</code> exceeds <code>data.length</code>.
	 */
	public static void putDouble(double value, ByteOrder order, byte[] data, int offset)
	{
		putLong(Double.doubleToRawLongBits(value), order, data, offset);
	}
	
	/**
	 * Gets a double floating-point number from an array.
	 * Reads 8 bytes.
	 * @param order the byte ordering.
	 * @param data the input array.
	 * @param offset the offset into the array to read.
	 * @return the resultant float.
	 * @throws ArrayIndexOutOfBoundsException if <code>offset + 8</code> exceeds <code>data.length</code>.
	 */
	public static double getDouble(ByteOrder order, byte[] data, int offset)
	{
		return Double.longBitsToDouble(getLong(order, data, offset));
	}
	
}
