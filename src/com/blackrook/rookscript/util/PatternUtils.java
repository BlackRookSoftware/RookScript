package com.blackrook.rookscript.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.blackrook.commons.hash.HashMap;

/**
 * A caching structure for all RegEx patterns.
 * <p>RegExs, when compiled into {@link Pattern}s, can be reused several times.
 * This cache is for automatic building/returning existing patterns by String.
 * <p>All operations are thread-safe.
 * @author Matthew Tropiano
 */
public final class PatternUtils
{
	// Can't instantiate.
	private PatternUtils() {}
	
	/** The main cache map. */
	private static HashMap<String, Pattern> cacheMap;
	
	static
	{
		cacheMap = new HashMap<>(8);
	}
	
	/**
	 * Gets an existing, compiled pattern or a newly-compiled one for the provided expression.
	 * Also useful for pre-warming oft-used expressions.
	 * @param regex the input RegEx.
	 * @return a newly-compiled {@link Pattern} or an existing one.
	 * @throws PatternSyntaxException if the expression cannot be compiled.
	 */
	public static Pattern get(String regex)
	{
		if (cacheMap.containsKey(regex))
			return cacheMap.get(regex);
		
		Pattern out;
		synchronized (cacheMap)
		{
			// return to threads compiling the same pattern
			if (cacheMap.containsKey(regex))
				return cacheMap.get(regex);
			
			cacheMap.put(regex, out = Pattern.compile(regex));
		}
		
		return out;
	}

}