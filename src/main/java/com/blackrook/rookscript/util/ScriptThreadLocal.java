package com.blackrook.rookscript.util;

import com.blackrook.rookscript.ScriptValue;

/**
 * This contains thread local caches for fast script value manipulation without
 * causing additional allocations.
 * @author Matthew Tropiano
 */
public final class ScriptThreadLocal
{
	private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(()->new Cache());

	// Get the cache.
	public static Cache getCache()
	{
		return CACHE.get();
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
