package com.blackrook.rookscript.util;

import com.blackrook.commons.util.ThreadUtils;
import com.blackrook.rookscript.ScriptValue;

/**
 * This contains thread local caches for fast script value manipulation without
 * causing additional allocations.
 * @author Matthew Tropiano
 */
public final class ScriptThreadLocal
{
	private static final String CACHE_NAME = "$$"+Cache.class.getCanonicalName();

	// Get the cache.
	public static Cache getCache()
	{
		Cache out;
		if ((out = (Cache)ThreadUtils.getLocal(CACHE_NAME)) == null)
			ThreadUtils.setLocal(CACHE_NAME, out = new Cache());
		return out;
	}
	
	// value cache.
	public static class Cache
	{
		public ScriptValue temp;
		public ScriptValue value1;
		public ScriptValue value2;
		
		private Cache()
		{
			this.temp = ScriptValue.create(null);
			this.value1 = ScriptValue.create(null);
			this.value2 = ScriptValue.create(null);
		}
		
	}

}
