/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import java.util.HashMap;

import com.blackrook.rookscript.ScriptValue;

/**
 * This contains thread local caches for fast script value manipulation without
 * causing additional allocations.
 * @author Matthew Tropiano
 */
public final class ScriptThreadLocal
{
	private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(()->new Cache());
	private static final ThreadLocal<InvokerCache> INVOKERCACHE = ThreadLocal.withInitial(()->new InvokerCache());
	
	// Get the cache.
	public static Cache getCache()
	{
		return CACHE.get();
	}
	
	// Get the cache.
	public static InvokerCache getInvokerCache()
	{
		return INVOKERCACHE.get();
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

	// Parameter array cache.
	public static class InvokerCache
	{
		private HashMap<Integer, Object[]> map;
		
		private InvokerCache()
		{
			map = new HashMap<>();
		}
		
		public Object[] getParamArray(int size)
		{
			Object[] out;
			if ((out = map.get(size)) == null)
				map.put(size, out = new Object[size]);
			return out;
		}

	}
	
}
