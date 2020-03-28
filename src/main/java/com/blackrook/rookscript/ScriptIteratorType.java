/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
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
		 * @since 1.3.0, returns {@link ScriptValue}, not {@link Object}.
		 */
		public ScriptValue getKey() 
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

