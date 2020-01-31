package com.blackrook.rookscript;

import java.util.Iterator;

/**
 * Describes a type that iterates over ScriptValues, generically. 
 * The returned {@link IteratorPair}s are considered to be RE-USED, and each piece meant to be pushed onto a stack. 
 */
public interface ScriptIteratorType extends Iterator<ScriptIteratorType.IteratorPair>
{
	/**
	 * The iterator pair that gets pushed onto a script stack on each iteration.
	 */
	public static class IteratorPair
	{
		private ScriptValue key;
		private ScriptValue value;
		
		/**
		 * Creates a new iterator pair.
		 */
		public IteratorPair()
		{
			this.key = ScriptValue.create(null);
			this.value = ScriptValue.create(null);
		}
		
		/**
		 * Creates a new iterator pair.
		 * @param key pair key.
		 * @param value pair value. 
		 */
		public IteratorPair(Object key, Object value)
		{
			set(key, value);
		}
		
		/**
		 * Sets the key and value in this pair.
		 * @param key pair key.
		 * @param value pair value. 
		 */
		public void set(Object key, Object value)
		{
			this.key.set(key);
			this.value.set(value);
		}
		
		/**
		 * @return the pair key.
		 */
		public Object getKey() 
		{
			return key;
		}
		
		/**
		 * @return the pair value.
		 */
		public ScriptValue getValue() 
		{
			return value;
		}
		
	}
	
}

