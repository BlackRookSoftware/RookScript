/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import com.blackrook.commons.util.ThreadUtils;
import com.blackrook.rookscript.exception.ScriptExecutionException;

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
	 * Restores previous command index.
	 */
	RETURN
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			if (scriptInstance.getFrameDepth() == 0)
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
			ScriptFunctionResolver resolver = scriptInstance.getFunctionResolver();
			ScriptFunctionType functionType = resolver.getFunctionByName(name);
			if (functionType == null)
				throw new ScriptExecutionException("host function "+name+" could not be resolved");
			try {
				return functionType.execute(scriptInstance);
			} catch (ScriptExecutionException e) {
				throw e;
			} catch (Throwable t) {
				throw new ScriptExecutionException("host function "+name+" threw an exception.", t);
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
			ScriptValue sv = scriptInstance.popStackValue();
			labelName = sv.asBoolean() ? String.valueOf(operand1) : String.valueOf(operand2);
			
			int index = scriptInstance.getCommandIndex(labelName);
			if (index < 0)
				throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
			scriptInstance.setCurrentCommandIndex(index);
			return true;
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
			ScriptValue sv = scriptInstance.popStackValue();
			if (sv.asBoolean())
			{
				String labelName =  String.valueOf(operand1);
				int index = scriptInstance.getCommandIndex(labelName);
				if (index < 0)
					throw new ScriptExecutionException("label "+labelName+" does not correspond to an index");
				scriptInstance.setCurrentCommandIndex(index);
			}
			return true;
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
			ScriptValue sv = scriptInstance.popStackValue();
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
	},
	
	/**
	 * Jump to label if stack top is not null, else pop.
	 * Operand is label if false.
	 * Sets a new command index.
	 */
	JUMP_COALESCE
	{
		@Override
		public boolean execute(ScriptInstance scriptInstance, Object operand1, Object operand2)
		{
			ScriptValue sv = scriptInstance.getStackValue(0);
			if (sv == null)
				throw new ScriptExecutionException("stack is empty.");
			else if (sv.isNull())
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
			Cache cache = getCache();
			if (operand1 instanceof Long)
				cache.tempValue.set((Long)operand1);
			else if (operand1 instanceof Double)
				cache.tempValue.set((Double)operand1);
			else if (operand1 instanceof Boolean)
				cache.tempValue.set((Boolean)operand1);
			else if (operand1 instanceof String)
				cache.tempValue.set((String)operand1);
			else if (operand1 == null)
				throw new ScriptExecutionException("Attempt to push null value");
			else
				cache.tempValue.set(operand1);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			Cache cache = getCache();
			cache.tempValue.setNull();
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value;
			if ((value = scriptInstance.getValue(name)) == null)
				scriptInstance.pushStackValue(null);
			else
				scriptInstance.pushStackValue(value);
			
			return true;
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
			ScriptVariableResolver scope;
			if ((scope = scriptInstance.getScopeResolver().getScope(scopeName)) == null)
			{
				scriptInstance.pushStackValue(null);
				return true;
			}

			ScriptValue value;
			if ((value = scope.getValue(variableName)) == null)
			{
				scriptInstance.pushStackValue(null);
				return true;
			}
			
			scriptInstance.pushStackValue(value);
			return true;
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
			Cache cache =  getCache();
			cache.tempValue.setEmptyList();
			scriptInstance.pushStackValue(cache.tempValue);
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
			int length = scriptInstance.popStackValue().asInt();

			ScriptValue[] list = new ScriptValue[length];
			for (int i = 0; i < length; i++)
				list[i] = ScriptValue.create(null);
			
			while (length-- > 0)
			{
				ScriptValue popped = scriptInstance.popStackValue();
				list[length].set(popped);
			}
			scriptInstance.pushStackValue(list);
			return true;
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
			ScriptValue indexValue = scriptInstance.popStackValue();
			ScriptValue listValue = scriptInstance.popStackValue();

			if (!listValue.isList())
			{
				scriptInstance.pushStackValue(null);
				return true;
			}

			int index = indexValue.asInt();
			scriptInstance.pushStackValue(listValue.listGetByIndex(index));
			return true;
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
			ScriptValue indexValue = scriptInstance.getStackValue(0);
			ScriptValue listValue = scriptInstance.getStackValue(1);

			if (!listValue.isList())
			{
				scriptInstance.pushStackValue(null);
				return true;
			}
			
			int index = indexValue.asInt();
			scriptInstance.pushStackValue(listValue.listGetByIndex(index));
			return true;
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
			Cache cache =  getCache();
			cache.tempValue.setEmptyMap();
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			int amount = scriptInstance.popStackValue().asInt();
			ScriptValue map = ScriptValue.createEmptyMap();
			
			while (amount-- > 0)
			{
				ScriptValue popped = scriptInstance.popStackValue();
				ScriptValue keyValue = scriptInstance.popStackValue();
				map.mapSet(keyValue.asString(), popped);
			}
			scriptInstance.pushStackValue(map);
			return true;
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
			ScriptValue keyValue = scriptInstance.popStackValue();
			ScriptValue mapValue = scriptInstance.popStackValue();
			
			if (!mapValue.isMap())
			{
				scriptInstance.pushStackValue(null);
				return true;
			}
			
			String key = keyValue.asString();
			scriptInstance.pushStackValue(mapValue.mapGet(key));
			return true;
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
			ScriptValue keyValue = scriptInstance.getStackValue(0);
			ScriptValue mapValue = scriptInstance.getStackValue(1);

			if (!mapValue.isMap())
			{
				scriptInstance.pushStackValue(null);
				return true;
			}
			
			String key = keyValue.asString();
			scriptInstance.pushStackValue(mapValue.mapGet(key));
			return true;
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
			ScriptValue value = scriptInstance.popStackValue();
			scriptInstance.setValue(name, value);
			return true;
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
			ScriptValue value = scriptInstance.popStackValue();

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
				scope.setValue(variableName, value.asObject());
			return true;
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
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue indexValue = scriptInstance.popStackValue();
			ScriptValue listValue = scriptInstance.popStackValue();
			
			if (!listValue.isList())
				return true;
			
			int index = indexValue.asInt();
			listValue.listSetByIndex(index, value);
			return true;
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
			ScriptValue value = scriptInstance.popStackValue();
			ScriptValue keyValue = scriptInstance.popStackValue();
			ScriptValue mapValue = scriptInstance.popStackValue();
			
			if (!mapValue.isMap())
				return true;
			
			String key = keyValue.asString();
			mapValue.mapSet(key, value);
			return true;
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
			scriptInstance.setValue(name, operand2);
			return true;
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
			ScriptValue value;
			if ((value = scriptInstance.getValue(valname)) == null)
				scriptInstance.setValue(name, null);
			else
				scriptInstance.setValue(name, value);
			return true;
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
			ScriptValue value = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.logicalNot(value, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.negate(value, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.absolute(value, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.logicalNot(value, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.add(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.subtract(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.multiply(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.divide(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.modulo(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.and(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.or(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.xor(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.logicalAnd(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.logicalOr(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.leftShift(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.rightShift(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.rightShiftPadded(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.less(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.lessOrEqual(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.greater(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.greaterOrEqual(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.equal(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.notEqual(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.strictEqual(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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
			ScriptValue value2 = scriptInstance.popStackValue();
			ScriptValue value1 = scriptInstance.popStackValue();
			Cache cache = getCache();
			ScriptValue.strictNotEqual(value1, value2, cache.tempValue);
			scriptInstance.pushStackValue(cache.tempValue);
			return true;
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

	private static final String CACHE_NAME = "$$"+Cache.class.getCanonicalName();

	// Get the cache.
	private static Cache getCache()
	{
		Cache out;
		if ((out = (Cache)ThreadUtils.getLocal(CACHE_NAME)) == null)
			ThreadUtils.setLocal(CACHE_NAME, out = new Cache());
		return out;
	}
	
	// Expression cache.
	private static class Cache
	{
		private ScriptValue tempValue;
		
		public Cache()
		{
			this.tempValue = ScriptValue.create(null);
		}
		
	}

}
