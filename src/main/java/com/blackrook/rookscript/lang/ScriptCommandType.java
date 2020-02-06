/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.lang;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptIteratorType;
import com.blackrook.rookscript.ScriptIteratorType.IteratorPair;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.exception.ScriptExecutionException;
import com.blackrook.rookscript.resolvers.ScriptHostFunctionResolver;
import com.blackrook.rookscript.resolvers.ScriptScopeResolver;
import com.blackrook.rookscript.resolvers.ScriptVariableResolver;

/**
 * Directive type for scripts.
 * @author Matthew Tropiano
 */
public enum ScriptCommandType
{
	/**
	 * Do nothing.
	 * No operand.
	 */
	NOOP
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			return true;
		}
	},
	
	/**
	 * Return value.
	 * No operand.
	 * Restores previous command index / frame.
	 */
	RETURN
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			if (scriptInstance.getCurrentActivationStackDepth() == 0)
			{
				scriptInstance.popFrame();
				scriptInstance.terminate();
				return false;
			}
			else
			{
				scriptInstance.popFrame();
				return true;
			}
		}
	},
	
	/**
	 * Call function.
	 * Operand is label.
	 * Pushes and sets a new command index.
	 */
	CALL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String labelName = String.valueOf(operand1);
			int index = scriptInstance.getCommandIndex(labelName);
			if (index < 0)
				throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
			
			scriptInstance.pushFrame(index);
			return true;
		}
	},
	
	/**
	 * Call host function.
	 * Operand is function name.
	 */
	CALL_HOST
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String name = String.valueOf(operand1);
			ScriptHostFunctionResolver resolver = scriptInstance.getHostFunctionResolver();
			ScriptFunctionType functionType = resolver.getNamespacedFunction(null, name);
			if (functionType == null)
				throw new ScriptExecutionException("host function \""+name+"\" could not be resolved");
			
			ScriptValue ret = RETURNVALUE.get();
			ret.setNull();
			try {
				boolean c = functionType.execute(scriptInstance, ret);
				scriptInstance.pushStackValue(ret);
				return c;
			} catch (ScriptExecutionException e) {
				throw e;
			} catch (Throwable t) {
				throw new ScriptExecutionException("host function \""+name+"\" threw an exception.", t);
			} finally {
				ret.setNull();
			}
		}
	},
	
	/**
	 * Call a namespaced host function.
	 * Operand1 is namespace name.
	 * Operand2 is function name.
	 */
	CALL_HOST_NAMESPACE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String namespace = String.valueOf(operand1);
			String name = String.valueOf(operand2);
			ScriptHostFunctionResolver resolver = scriptInstance.getHostFunctionResolver();
			ScriptFunctionType functionType = resolver.getNamespacedFunction(namespace, name);
			if (functionType == null)
				throw new ScriptExecutionException("host function \""+namespace+"::"+name+"\" could not be resolved");

			ScriptValue ret = RETURNVALUE.get();
			ret.setNull();
			try {
				boolean c = functionType.execute(scriptInstance, ret);
				scriptInstance.pushStackValue(ret);
				return c;
			} catch (ScriptExecutionException e) {
				throw e;
			} catch (Throwable t) {
				throw new ScriptExecutionException("host function \""+namespace+"::"+name+"\" threw an exception.", t);
			} finally {
				ret.setNull();
			}
		}
	},
	
	/**
	 * Jump to index.
	 * Operand is label.
	 * Sets a new command index.
	 */
	JUMP
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String labelName = String.valueOf(operand1);
			int index = scriptInstance.getCommandIndex(labelName);
			if (index < 0)
				throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
			
			scriptInstance.setCurrentCommandIndex(index);
			return true;
		}
	},
	
	/**
	 * Jump to index based on POP.
	 * Operand1 is label if true.
	 * Operand2 is label if false.
	 * Sets a new command index.
	 */
	JUMP_BRANCH
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String labelName;
			ScriptValue sv = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(sv);
				labelName = sv.asBoolean() ? String.valueOf(operand1) : String.valueOf(operand2);
				
				int index = scriptInstance.getCommandIndex(labelName);
				if (index < 0)
					throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
				scriptInstance.setCurrentCommandIndex(index);
				return true;
			} 
			finally 
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * Jump to index if POP is true.
	 * Operand is label if true.
	 * Sets a new command index.
	 */
	JUMP_TRUE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			try 
			{
				scriptInstance.popStackValue(sv);
				boolean b = sv.asBoolean();
				if (b)
				{
					String labelName =  String.valueOf(operand1);
					int index = scriptInstance.getCommandIndex(labelName);
					if (index < 0)
						throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
					scriptInstance.setCurrentCommandIndex(index);
				}
				return true;
			} 
			finally
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * Jump to index if POP is false.
	 * Operand is label if false.
	 * Sets a new command index.
	 */
	JUMP_FALSE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			try 
			{
				scriptInstance.popStackValue(sv);
				if (!sv.asBoolean())
				{
					String labelName =  String.valueOf(operand1);
					int index = scriptInstance.getCommandIndex(labelName);
					if (index < 0)
						throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
					scriptInstance.setCurrentCommandIndex(index);
				}
				return true;
			} 
			finally
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * Jump to label if stack top is true-equivalent, else pop.
	 * Operand is label if false.
	 * Sets a new command index.
	 */
	JUMP_FALSECOALESCE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			try 
			{
				scriptInstance.getStackValue(0, sv);
				if (!sv.asBoolean())
					scriptInstance.popStackValue();
				else
				{
					String labelName =  String.valueOf(operand1);
					int index = scriptInstance.getCommandIndex(labelName);
					if (index < 0)
						throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
					scriptInstance.setCurrentCommandIndex(index);
				}
				return true;
			} 
			finally 
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * Jump to label if stack top is not null, else pop.
	 * Operand is label if false.
	 * Sets a new command index.
	 */
	JUMP_NULLCOALESCE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			try 
			{
				scriptInstance.getStackValue(0, sv);
				if (sv.isNull())
					scriptInstance.popStackValue();
				else
				{
					String labelName =  String.valueOf(operand1);
					int index = scriptInstance.getCommandIndex(labelName);
					if (index < 0)
						throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
					scriptInstance.setCurrentCommandIndex(index);
				}
				return true;
			} 
			finally 
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * Performs a single iteration, expecting an OBJECTREF:ScriptIteratorType 
	 * on the stack top when this is executed.
	 * 
	 * If stack top is not OBJECTREF:ScriptIteratorType, Exception - script was compiled badly.
	 * If stack top's ScriptIteratorType.hasNext() is null, POP, then JUMP to [label].
	 * Else, call ScriptIteratorType.next(), then push value. If operand1 is true, also push key.
	 *  
	 * Operand1 is label.
	 * Operand2 is value/key-value flag.
	 * Sets a new command index and one POP, or 1-2 PUSHes.
	 */
	ITERATE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			try 
			{
				scriptInstance.getStackValue(0, sv);
				
				if (!sv.isObjectRef(ScriptIteratorType.class))
					throw new ScriptExecutionException("Called ITERATE with stack top that isn't a ScriptIteratorType!");
				
				ScriptIteratorType iter = sv.asObjectType(ScriptIteratorType.class);
				if (iter.hasNext())
				{
					IteratorPair pair = iter.next();
					scriptInstance.pushStackValue(pair.getValue());
					if ((Boolean)operand2) // true = push key as well.
						scriptInstance.pushStackValue(pair.getKey());
				}
				else
				{
					scriptInstance.popStackValue();
					String labelName =  String.valueOf(operand1);
					int index = scriptInstance.getCommandIndex(labelName);
					if (index < 0)
						throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
					scriptInstance.setCurrentCommandIndex(index);
				}
				return true;
			} 
			finally 
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * If the stack top is an error, jump to label.
	 * Operand1 is label.
	 * Pushes nothing.
	 */
	CHECK_ERROR
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			try 
			{
				scriptInstance.getStackValue(0, sv);
				if (sv.isError())
					return JUMP.execute(scriptInstance, operand1, null);
				else
					return true;
			} 
			finally 
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * PUSH value.
	 * Operand is Boolean, Double, Long, String, Object.
	 * Pushes one value.
	 */
	PUSH
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			try 
			{
				if (operand1 instanceof Long)
					sv.set((Long)operand1);
				else if (operand1 instanceof Double)
					sv.set((Double)operand1);
				else if (operand1 instanceof Boolean)
					sv.set((Boolean)operand1);
				else if (operand1 instanceof String)
					sv.set((String)operand1);
				else if (operand1 == null)
					throw new ScriptExecutionException("Attempt to push null value");
				else
					sv.set(operand1);
				scriptInstance.pushStackValue(sv);
				return true;
			} 
			finally 
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * PUSH null literal.
	 * No Operands.
	 * Pushes one value.
	 */
	PUSH_NULL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			try
			{
				scriptInstance.pushStackValue(sv);
				return true;
			}
			finally
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * PUSH variable.
	 * Operand1 is String - variable name.
	 * Pushes one value.
	 */
	PUSH_VARIABLE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String name = String.valueOf(operand1);
			ScriptValue sv = CACHEVALUE1.get();
			try
			{
				scriptInstance.getValue(name, sv);
				scriptInstance.pushStackValue(sv);
				return true;
			}
			finally
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * PUSH scoped variable.
	 * Operand1 is String - scope name.
	 * Operand2 is String - variable name.
	 * Pushes one value, or null if no scope.
	 */
	PUSH_SCOPE_VARIABLE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String scopeName = String.valueOf(operand1);
			String variableName = String.valueOf(operand2);
			ScriptValue sv = CACHEVALUE1.get();
			try
			{
				ScriptVariableResolver scope;
				if ((scope = scriptInstance.getScopeResolver().getScope(scopeName)) == null)
				{
					scriptInstance.pushStackValue(null);
					return true;
				}

				if (!scope.getValue(variableName, sv))
				{
					scriptInstance.pushStackValue(null);
					return true;
				}
				
				scriptInstance.pushStackValue(sv);
				return true;
			}
			finally
			{
				sv.setNull();
			}
		}
	},
	
	/**
	 * PUSH a new blank list.
	 * No operands.
	 */
	PUSH_LIST_NEW
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			try
			{
				sv.setEmptyList();
				scriptInstance.pushStackValue(sv);
			}
			finally
			{
				sv.setNull();
			}
			return true;
		}
	},
	
	/**
	 * PUSH a new blank list initialized with values.
	 * Pops a value for list length.
	 * Pops [length] values into array backwards (order pushed).
	 * Pushes array value into stack.
	 * No operands.
	 */
	PUSH_LIST_INIT
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(sv);
				int length = sv.asInt();
				sv.setEmptyList(length, length);
				while (length-- > 0)
				{
					scriptInstance.popStackValue(temp);
					sv.listSetByIndex(length, temp);
				}
				scriptInstance.pushStackValue(sv);
				return true;
			}
			finally
			{
				sv.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * PUSH a list value.
	 * Pops two values - an index and the array.
	 * Pushes one value onto stack.
	 * No operands.
	 */
	PUSH_LIST_INDEX
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue indexValue = CACHEVALUE1.get();
			ScriptValue listValue = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try 
			{
				scriptInstance.popStackValue(indexValue);
				scriptInstance.popStackValue(listValue);

				if (!listValue.isList())
				{
					scriptInstance.pushStackValue(null);
					return true;
				}

				int index = indexValue.asInt();
				listValue.listGetByIndex(index, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			} 
			finally 
			{
				indexValue.setNull();
				listValue.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * PUSH a list value.
	 * Pops ZERO values - only inspects two spots down in the stack!
	 * Pushes one value onto stack.
	 * No operands.
	 */
	PUSH_LIST_INDEX_CONTENTS
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue indexValue = CACHEVALUE1.get();
			ScriptValue listValue = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.getStackValue(0, indexValue);
				scriptInstance.getStackValue(1, listValue);

				if (!listValue.isList())
				{
					scriptInstance.pushStackValue(null);
					return true;
				}
				
				int index = indexValue.asInt();
				listValue.listGetByIndex(index, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				indexValue.setNull();
				listValue.setNull();
				temp.setNull();
			}
		}
	},

	/**
	 * PUSH a new blank map.
	 * No operands.
	 */
	PUSH_MAP_NEW
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				temp.setEmptyMap();
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	/**
	 * PUSH a new blank map initialized with values.
	 * Pops a value for amount of entries.
	 * Pops [amount] values into map.
	 * Pushes map value into stack.
	 * No operands.
	 */
	PUSH_MAP_INIT
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue popped = CACHEVALUE2.get();
			ScriptValue keyValue = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(temp);
				int amount = temp.asInt();
				temp.setEmptyMap(amount);
				while (amount-- > 0)
				{
					scriptInstance.popStackValue(popped);
					scriptInstance.popStackValue(keyValue);
					temp.mapSet(keyValue.asString(), popped);
				}
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				temp.setNull();
				popped.setNull();
				keyValue.setNull();
			}
		}
	},
	
	/**
	 * PUSHes a map value using a key.
	 * Pops two values - key and map.
	 * If map value is not a map, pushes NULL.
	 * No operands.
	 */
	PUSH_MAP_KEY
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue keyValue = CACHEVALUE2.get();
			ScriptValue mapValue = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(keyValue);
				scriptInstance.popStackValue(mapValue);
				
				if (!mapValue.isMap())
				{
					scriptInstance.pushStackValue(null);
					return true;
				}
				
				String key = keyValue.asString();
				mapValue.mapGet(key, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				temp.setNull();
				keyValue.setNull();
				mapValue.setNull();
			}
		}
	}, 
	
	/**
	 * PUSHes a map value using a key.
	 * Pops ZERO values - only inspects two spots down in the stack!
	 * Pushes one value onto stack.
	 * No operands.
	 */
	PUSH_MAP_KEY_CONTENTS
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue keyValue = CACHEVALUE2.get();
			ScriptValue mapValue = CACHEVALUE3.get();
			try
			{
				scriptInstance.getStackValue(0, keyValue);
				scriptInstance.getStackValue(1, mapValue);

				if (!mapValue.isMap())
				{
					scriptInstance.pushStackValue(null);
					return true;
				}
				
				String key = keyValue.asString();
				mapValue.mapGet(key, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				temp.setNull();
				keyValue.setNull();
				mapValue.setNull();
			}
		}
	},
	
	/**
	 * Pops a variable and pushes an iterator for it onto the stack.
	 * Pops one value.
	 * Pushes one value onto stack.
	 * No operands.
	 */
	PUSH_ITERATOR
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				scriptInstance.pushStackValue(temp.iterator());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Pushes a Sentinel Object onto the stack.
	 * Pushes one value onto stack.
	 * No operands.
	 */
	PUSH_SENTINEL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.pushStackValue(new SentinelObject());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * POP value into nothing.
	 * No operands.
	 */
	POP
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			scriptInstance.popStackValue();
			return true;
		}
	},
	
	/**
	 * POP into variable variable.
	 * Operand is String - variable name.
	 */
	POP_VARIABLE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String name = String.valueOf(operand1);
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				scriptInstance.setValue(name, temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * POP into variable variable.
	 * Operand is String - variable name.
	 */
	POP_SCOPE_VARIABLE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String scopeName = String.valueOf(operand1);
			String variableName = String.valueOf(operand2);
			ScriptValue value = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(value);

				ScriptScopeResolver resolver = scriptInstance.getScopeResolver();
				if (resolver == null)
				{
					scriptInstance.pushStackValue(null);
					return true;
				}

				ScriptVariableResolver scope;
				if ((scope = resolver.getScope(scopeName)) == null)
				{
					scriptInstance.pushStackValue(null);
					return true;
				}

				if (!scope.isReadOnly(variableName))
					scope.setValue(variableName, value);
				return true;
			}
			finally
			{
				value.setNull();
			}
		}
	},
	
	/**
	 * Sets a list value.
	 * Pops three values - the value, the index, and then the list.
	 * No operands.
	 */
	POP_LIST
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue indexValue = CACHEVALUE2.get();
			ScriptValue listValue = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value);
				scriptInstance.popStackValue(indexValue);
				scriptInstance.popStackValue(listValue);

				if (!listValue.isList())
					return true;
				
				int index = indexValue.asInt();
				listValue.listSetByIndex(index, value);
				return true;
			}
			finally
			{
				value.setNull();
				indexValue.setNull();
				listValue.setNull();
			}
		}
	},
	
	/**
	 * Sets a map value.
	 * Pops three values - the value, the key, and then the map.
	 * No operands.
	 */
	POP_MAP
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue keyValue = CACHEVALUE2.get();
			ScriptValue mapValue = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value);
				scriptInstance.popStackValue(keyValue);
				scriptInstance.popStackValue(mapValue);

				if (!mapValue.isMap())
					return true;
				
				String key = keyValue.asString();
				mapValue.mapSet(key, value);
				return true;
			}
			finally
			{
				value.setNull();
				keyValue.setNull();
				mapValue.setNull();
			}
		}
	},
	
	/**
	 * POPs values until the sentinel was popped.
	 * Operand1 is amount of sentinel objects to pop.
	 */
	POP_SENTINEL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try 
			{
				long amount = (Long)operand1;
				while (amount > 0)
				{
					scriptInstance.popStackValue(temp);
					if (temp.isObjectRef(SentinelObject.class))
						amount--;
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Set's a variable to a value.
	 * Operand1 is String - variable name.
	 * Operand2 is Object value.
	 */
	SET
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String name = String.valueOf(operand1);
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				temp.set(operand2);
				scriptInstance.setValue(name, temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},
	
	/**
	 * Set's a variable to another variable's value.
	 * Operand1 is String - variable name.
	 * Operand2 is String - variable name.
	 */
	SET_VARIABLE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			String name = String.valueOf(operand1);
			String valname = String.valueOf(operand2);
			ScriptValue value = CACHEVALUE1.get();
			try
			{
				scriptInstance.getValue(valname, value);
				scriptInstance.setValue(name, value);
				return true;
			}
			finally
			{
				value.setNull();
			}
		}
	},

	/**
	 * Bitwise NOT.
	 * Pushes one value.
	 */
	NOT
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				ScriptValue.logicalNot(value, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Negate.
	 * Pops one value.
	 * Pushes one value.
	 */
	NEGATE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				ScriptValue.negate(value, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Absolute.
	 * Pops one value.
	 * Pushes one value.
	 */
	ABSOLUTE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				ScriptValue.absolute(value, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Turns the topmost value into a boolean-equivalent value (the same as a not-not).
	 * Pops one value.
	 * Pushes one value.
	 */
	LOGICAL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				temp.set(temp.asBoolean());
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}, 
	
	/**
	 * Logical Not.
	 * Pops one value.
	 * Pushes one value.
	 */
	LOGICAL_NOT
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value = CACHEVALUE1.get();
			ScriptValue temp = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(value);
				ScriptValue.logicalNot(value, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Add.
	 * Pops two values.
	 * Pushes one value.
	 */
	ADD
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.add(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Subtract.
	 * Pops two values.
	 * Pushes one value.
	 */
	SUBTRACT
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.subtract(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Multiply.
	 * Pops two values.
	 * Pushes one value.
	 */
	MULTIPLY
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.multiply(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Divide.
	 * Pops two values.
	 * Pushes one value.
	 */
	DIVIDE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.divide(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Modulo.
	 * Pops two values.
	 * Pushes one value.
	 */
	MODULO
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.modulo(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Bitwise And.
	 * Pops two values.
	 * Pushes one value.
	 */
	AND
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.and(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Bitwise Or.
	 * Pops two values.
	 * Pushes one value.
	 */
	OR
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.or(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Bitwise Xor.
	 * Pops two values.
	 * Pushes one value.
	 */
	XOR
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.xor(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Logical And.
	 * Pops two values.
	 * Pushes one value.
	 */
	LOGICAL_AND
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.logicalAnd(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Logical Or.
	 * Pops two values.
	 * Pushes one value.
	 */
	LOGICAL_OR
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.logicalOr(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Left Bit Shift.
	 * Pops two values.
	 * Pushes one value.
	 */
	LEFT_SHIFT
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.leftShift(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Right Bit Shift.
	 * Pops two values.
	 * Pushes one value.
	 */
	RIGHT_SHIFT
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.rightShift(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Right Bit Shift Padded.
	 * Pops two values.
	 * Pushes one value.
	 */
	RIGHT_SHIFT_PADDED
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.rightShiftPadded(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Less than.
	 * Pops two values.
	 * Pushes one value.
	 */
	LESS
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.less(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Less than or equal.
	 * Pops two values.
	 * Pushes one value.
	 */
	LESS_OR_EQUAL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.lessOrEqual(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Greater than.
	 * Pops two values.
	 * Pushes one value.
	 */
	GREATER
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.greater(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Greater than or equal.
	 * Pops two values.
	 * Pushes one value.
	 */
	GREATER_OR_EQUAL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.greaterOrEqual(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Equal.
	 * Pops two values.
	 * Pushes one value.
	 */
	EQUAL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.equal(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Not Equal.
	 * Pops two values.
	 * Pushes one value.
	 */
	NOT_EQUAL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.notEqual(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Strict Equal.
	 * Pops two values.
	 * Pushes one value.
	 */
	STRICT_EQUAL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.strictEqual(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	/**
	 * Strict Not Equal.
	 * Pops two values.
	 * Pushes one value.
	 */
	STRICT_NOT_EQUAL
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue value2 = CACHEVALUE1.get();
			ScriptValue value1 = CACHEVALUE2.get();
			ScriptValue temp = CACHEVALUE3.get();
			try
			{
				scriptInstance.popStackValue(value2);
				scriptInstance.popStackValue(value1);
				ScriptValue.strictNotEqual(value1, value2, temp);
				scriptInstance.pushStackValue(temp);
				return true;
			}
			finally
			{
				value2.setNull();
				value1.setNull();
				temp.setNull();
			}
		}
	},
	
	;

	/**
	 * Executes this directive.
	 * @param scriptInstance the originating script instance.
	 * @param operand1 the first operand.
	 * @param operand2 the second operand.
	 * @return if false, this halts script execution, else if true, continue.
	 */
	public abstract boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2);

	// Sentinel object for PUSH/POP Sentinel
	private static class SentinelObject
	{
		private SentinelObject() {}
	}
	
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE3 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> RETURNVALUE = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
