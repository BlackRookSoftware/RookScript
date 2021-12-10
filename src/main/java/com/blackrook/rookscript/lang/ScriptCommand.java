/*******************************************************************************
 * Copyright (c) 2017-2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.lang;

import com.blackrook.rookscript.ScriptInstance;

/**
 * Single script directive.
 * @author Matthew Tropiano
 */
public final class ScriptCommand
{
	/** Directive type. */
	private ScriptCommandType type;
	/** First Operand. */
	private Object operand1;
	/** Second Operand. */
	private Object operand2;
	
	// Private constructor.
	private ScriptCommand(ScriptCommandType type, Object operand1, Object operand2)
	{
		this.type = type;
		this.operand1 = operand1;
		this.operand2 = operand2;
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type)
	{
		return new ScriptCommand(type, null, null);
	}

	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand the operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, boolean operand)
	{
		return new ScriptCommand(type, operand, null);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand the operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, long operand)
	{
		return new ScriptCommand(type, operand, null);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand the operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, double operand)
	{
		return new ScriptCommand(type, operand, null);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand the only operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, String operand)
	{
		return new ScriptCommand(type, operand, null);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand the only operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, Object operand)
	{
		if (operand instanceof Boolean)
			return new ScriptCommand(type, (Boolean)operand, null);
		else if (operand instanceof Long)
			return new ScriptCommand(type, (Long)operand, null);
		else if (operand instanceof Double)
			return new ScriptCommand(type, (Double)operand, null);
		else if (operand instanceof String)
			return new ScriptCommand(type, (String)operand, null);
		else
			throw new IllegalArgumentException("Bad object type."); 
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand1 the first operand.
	 * @param operand2 the second operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, String operand1, boolean operand2)
	{
		return new ScriptCommand(type, operand1, operand2);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand1 the first operand.
	 * @param operand2 the second operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, Integer operand1, boolean operand2)
	{
		return create(type, (long)operand1, operand2);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand1 the first operand.
	 * @param operand2 the second operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, Long operand1, boolean operand2)
	{
		return new ScriptCommand(type, operand1, operand2);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand1 the first operand.
	 * @param operand2 the second operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, String operand1, long operand2)
	{
		return new ScriptCommand(type, operand1, operand2);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand1 the first operand.
	 * @param operand2 the second operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, String operand1, double operand2)
	{
		return new ScriptCommand(type, operand1, operand2);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand1 the first operand.
	 * @param operand2 the second operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, String operand1, String operand2)
	{
		return new ScriptCommand(type, operand1, operand2);
	}
	
	/**
	 * Creates a new script directive.
	 * @param type the directive type.
	 * @param operand1 the first operand.
	 * @param operand2 the second operand.
	 * @return a new script directive.
	 */
	public static ScriptCommand create(ScriptCommandType type, String operand1, Object operand2)
	{
		if (operand2 instanceof Boolean)
			return new ScriptCommand(type, operand1, (Boolean)operand2);
		else if (operand2 instanceof Long)
			return new ScriptCommand(type, operand1, (Long)operand2);
		else if (operand2 instanceof Double)
			return new ScriptCommand(type, operand1, (Double)operand2);
		else if (operand2 instanceof String)
			return new ScriptCommand(type, operand1, (String)operand2);
		else
			throw new IllegalArgumentException("Bad object type."); 
	}

	
	public ScriptCommandType getType()
	{
		return type;
	}

	public Object getOperand1()
	{
		return operand1;
	}
	
	public Object getOperand2()
	{
		return operand2;
	}
	
    /**
     * Executes this command.
     * @param scriptInstance the originating script instance.
     * @return if false, stop script running, else if true, continue.
     */	
	public boolean execute(ScriptInstance scriptInstance)
	{
		return type.execute(scriptInstance, operand1, operand2);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(type.name());
		if (operand1 != null)
		{
			sb.append(' ');
			if (operand1 instanceof String)
				sb.append('"').append(String.valueOf(operand1)).append('"');
			else
				sb.append(String.valueOf(operand1));
			
			if (operand2 != null)
			{
				sb.append(' ');
				if (operand2 instanceof String)
					sb.append('"').append(String.valueOf(operand2)).append('"');
				else
					sb.append(String.valueOf(operand2));
			}
		}
		return sb.toString();
	}

}
