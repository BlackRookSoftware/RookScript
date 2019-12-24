/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.io.IOException;
import java.io.Writer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.blackrook.rookscript.Script.Entry;
import com.blackrook.rookscript.lang.ScriptCommand;
import com.blackrook.rookscript.lang.ScriptCommandType;
import com.blackrook.rookscript.struct.Utils;

/**
 * The assembler part of the script factories.
 * @author Matthew Tropiano
 */
public final class ScriptAssembler
{
	/**
	 * Disassembles a script into a text representation.
	 * The writer is not closed.
	 * @param script the compiled script.
	 * @param writer the output writer.
	 * @throws IOException if the writer cannot be written to.
	 */
	public static void disassemble(Script script, Writer writer) throws IOException
	{
		int commandCount = script.getCommandCount();
		for (int i = 0; i < commandCount; i++)
		{
			dumpLineLabels(script, writer, i);
			ScriptCommand command = script.getCommand(i);
			writer.write("\t");
			writer.write(command.toString());
			writer.write("\n");
			writer.flush();
		}
		
		dumpLineLabels(script, writer, commandCount);
	}

	// Dumps the line labels.
	private static void dumpLineLabels(Script script, Writer out, int line) throws IOException
	{
		Iterable<String> labels = script.getLabelsAtIndex(line);
		if (!Utils.isEmpty(labels)) for (String label : labels)
		{
			out.write(label);
			out.write(":");
			out.write("\n");
			out.flush();
		}
	}

	/**
	 * Optimizes a script.
	 * @param script the input script.
	 * @return the new script after optimization.
	 */
	public static Script optimize(Script script)
	{
		Deque<ScriptCommand> reduceStack = new LinkedList<>();
		Deque<ScriptCommand> backwardsStack = new LinkedList<>();
		Queue<ScriptCommand> outCommands = new LinkedList<>();
		
		Script optimizedScript = new Script();
		
		final int STATE_INIT = 0;
		final int STATE_PUSH_VAR_1 = 1;
		final int STATE_PUSH_LITERAL_1 = 2;
		final int STATE_PUSH_LITERAL_2 = 3;
		final int STATE_RETURN = 4;
		int state = STATE_INIT;
		for (int index = 0; index < script.getCommandCount(); index++)
		{
			ScriptCommand command = script.getCommand(index);
			
			if (optimizeHasLabels(script, index))
			{
				optimizeEmitAll(reduceStack, backwardsStack, outCommands);
				optimizeEmitLabels(script, optimizedScript, index, outCommands.size());
				state = STATE_INIT;
			}
	
			switch (state)
			{
				case STATE_INIT:
				{
					if (command.getType() == ScriptCommandType.PUSH)
					{
						reduceStack.push(command);
						state = STATE_PUSH_LITERAL_1;
					}
					else if (command.getType() == ScriptCommandType.RETURN)
					{
						outCommands.add(command);
						state = STATE_RETURN;
					}
					else if (command.getType() == ScriptCommandType.PUSH_VARIABLE)
					{
						reduceStack.push(command);
						state = STATE_PUSH_VAR_1;
					}
					else if (command.getType() == ScriptCommandType.JUMP)
					{
						// remove unnecessary jumps
						String label = command.getOperand1().toString();
						if (script.getIndex(label) != index + 1)
							outCommands.add(command);
					}
					else
					{
						outCommands.add(command);
					}
					break;
				}
	
				case STATE_PUSH_VAR_1:
				{
					if (command.getType() == ScriptCommandType.POP_VARIABLE)
					{
						ScriptCommand popped = reduceStack.pop();
						ScriptCommand newCommand = ScriptCommand.create(ScriptCommandType.SET_VARIABLE, command.getOperand1().toString(), popped.getOperand1().toString());
						optimizeEmitAll(reduceStack, backwardsStack, outCommands);
						outCommands.add(newCommand);
						state = STATE_INIT;
					}
					else
					{
						optimizeEmitAll(reduceStack, backwardsStack, outCommands);
						index--;
						state = STATE_INIT;
					}
					break;
				}
				
				case STATE_PUSH_LITERAL_1:
				{
					if (isUnaryOperatorCommand(command.getType()))
					{
						reduceStack.push(command);
						optimizeReduce(reduceStack);
					}
					else if (command.getType() == ScriptCommandType.PUSH)
					{
						reduceStack.push(command);
						state = STATE_PUSH_LITERAL_2;
					}
					else if (command.getType() == ScriptCommandType.PUSH_VARIABLE)
					{
						optimizeEmitAll(reduceStack, backwardsStack, outCommands);
						reduceStack.push(command);
						state = STATE_PUSH_VAR_1;
					}
					else if (command.getType() == ScriptCommandType.POP_VARIABLE)
					{
						ScriptCommand popped = reduceStack.pop();
						ScriptCommand newCommand = ScriptCommand.create(ScriptCommandType.SET, command.getOperand1().toString(), popped.getOperand1());
						optimizeEmitAll(reduceStack, backwardsStack, outCommands);
						outCommands.add(newCommand);
						state = STATE_INIT;
					}
					else
					{
						optimizeEmitAll(reduceStack, backwardsStack, outCommands);
						index--;
						state = STATE_INIT;
					}
					break;
				}
				
				case STATE_PUSH_LITERAL_2:
				{
					if (isUnaryOperatorCommand(command.getType()))
					{
						reduceStack.push(command);
						optimizeReduce(reduceStack);
						if (reduceStack.size() < 2)
							state = STATE_PUSH_LITERAL_1;
					}
					else if (isBinaryOperatorCommand(command.getType()))
					{
						reduceStack.push(command);
						optimizeReduce(reduceStack);
						if (reduceStack.size() < 2)
							state = STATE_PUSH_LITERAL_1;
					}
					else if (command.getType() == ScriptCommandType.PUSH)
					{
						reduceStack.push(command);
						state = STATE_PUSH_LITERAL_2;
					}
					else
					{
						optimizeEmitAll(reduceStack, backwardsStack, outCommands);
						index--;
						state = STATE_INIT;
					}
					break;
				}
				
				// eats commands until a label is found.
				case STATE_RETURN:
				{
					break;
				}
			}
		}
	
		ScriptCommand[] optimizedCommands = new ScriptCommand[outCommands.size()];
		outCommands.toArray(optimizedCommands);
		optimizedScript.setCommands(optimizedCommands);
		optimizedScript.setHostFunctionResolver(script.getHostFunctionResolver());
		optimizedScript.setCommandRunawayLimit(script.getCommandRunawayLimit());
		if (script.getLabelGeneratorCounter() != null) for (Map.Entry<String, Integer> count : script.getLabelGeneratorCounter().entrySet())
			optimizedScript.setNextGeneratedLabelNumber(count.getKey(), count.getValue());
		return optimizedScript;
	}



	// Checks if a script has labels at a command index.
	private static boolean optimizeHasLabels(Script script, int index)
	{
		return script.getLabelsAtIndex(index) != null;
	}

	// Emits a script's labels at a command index.
	private static boolean optimizeEmitLabels(Script script, Script optimizedScript, int srcIndex, int targetIndex)
	{
		Iterable<String> labelIterable;
		if ((labelIterable = script.getLabelsAtIndex(srcIndex)) != null)
		{
			for (String label : labelIterable)
			{
				optimizedScript.setIndex(label, targetIndex);
				if (label.startsWith(Script.LABEL_ENTRY_PREFIX))
				{
					String name = label.substring(Script.LABEL_ENTRY_PREFIX.length());
					Entry e = script.getScriptEntry(name);
					optimizedScript.setScriptEntry(name, e.getParameterCount(), targetIndex);
				}
				else if (label.startsWith(Script.LABEL_FUNCTION_PREFIX))
				{
					String name = label.substring(Script.LABEL_FUNCTION_PREFIX.length());
					Entry e = script.getFunctionEntry(name);
					optimizedScript.createFunctionEntry(name, e.getParameterCount(), targetIndex);
				}
			}
			return true;
		}
		return false;
	}

	// Reduces the reduction stack, emitting commands until it can't.
	private static void optimizeEmitAll(Deque<ScriptCommand> reduceStack, Deque<ScriptCommand> backwardsStack, Queue<ScriptCommand> emitQueue)
	{
		while (!reduceStack.isEmpty())
			backwardsStack.push(reduceStack.poll());
		while (!backwardsStack.isEmpty())
			emitQueue.add(backwardsStack.poll());
	}

	// Reduces the reduction stack.
	private static void optimizeReduce(Deque<ScriptCommand> reduceStack)
	{
		ScriptCommand operator = reduceStack.pop();
		ScriptValue surrogateValue1; 
		ScriptValue surrogateValue2;
		ScriptValue surrogateValueOut;
		
		if (isUnaryOperatorCommand(operator.getType()))
		{
			surrogateValue1 = ScriptValue.create(reduceStack.pop().getOperand1());
			surrogateValueOut = ScriptValue.create(false);
			doUnaryOperatorCommand(operator.getType(), surrogateValue1, surrogateValueOut);
			reduceStack.push(ScriptCommand.create(ScriptCommandType.PUSH, surrogateValueOut.asObject()));
		}
		else if (isBinaryOperatorCommand(operator.getType()))
		{
			surrogateValue2 = ScriptValue.create(reduceStack.pop().getOperand1());
			surrogateValue1 = ScriptValue.create(reduceStack.pop().getOperand1());
			surrogateValueOut = ScriptValue.create(false);
			doBinaryOperatorCommand(operator.getType(), surrogateValue1, surrogateValue2, surrogateValueOut);
			reduceStack.push(ScriptCommand.create(ScriptCommandType.PUSH, surrogateValueOut.asObject()));
		}
	}

	// Checks if a script command type is a binary stack operator.
	private static boolean isBinaryOperatorCommand(ScriptCommandType type)
	{
		switch (type)
		{
			case ADD:
			case SUBTRACT:
			case MULTIPLY:
			case DIVIDE:
			case MODULO:
			case AND:
			case OR:
			case XOR:
			case LOGICAL_AND:
			case LOGICAL_OR:
			case LEFT_SHIFT:
			case RIGHT_SHIFT:
			case RIGHT_SHIFT_PADDED:
			case LESS:
			case LESS_OR_EQUAL:
			case GREATER:
			case GREATER_OR_EQUAL:
			case EQUAL:
			case NOT_EQUAL:
			case STRICT_EQUAL:
			case STRICT_NOT_EQUAL:
				return true;
			default:
				return false;
		}
	}

	// Performs a binary operator command.
	private static void doBinaryOperatorCommand(ScriptCommandType type, ScriptValue s1, ScriptValue s2, ScriptValue sout)
	{
		switch (type)
		{
			case ADD:
				ScriptValue.add(s1, s2, sout);
				return;
			case SUBTRACT:
				ScriptValue.subtract(s1, s2, sout);
				return;
			case MULTIPLY:
				ScriptValue.multiply(s1, s2, sout);
				return;
			case DIVIDE:
				ScriptValue.divide(s1, s2, sout);
				return;
			case MODULO:
				ScriptValue.modulo(s1, s2, sout);
				return;
			case AND:
				ScriptValue.and(s1, s2, sout);
				return;
			case OR:
				ScriptValue.or(s1, s2, sout);
				return;
			case XOR:
				ScriptValue.xor(s1, s2, sout);
				return;
			case LOGICAL_AND:
				ScriptValue.logicalAnd(s1, s2, sout);
				return;
			case LOGICAL_OR:
				ScriptValue.logicalOr(s1, s2, sout);
				return;
			case LEFT_SHIFT:
				ScriptValue.leftShift(s1, s2, sout);
				return;
			case RIGHT_SHIFT:
				ScriptValue.rightShift(s1, s2, sout);
				return;
			case RIGHT_SHIFT_PADDED:
				ScriptValue.rightShiftPadded(s1, s2, sout);
				return;
			case LESS:
				ScriptValue.less(s1, s2, sout);
				return;
			case LESS_OR_EQUAL:
				ScriptValue.lessOrEqual(s1, s2, sout);
				return;
			case GREATER:
				ScriptValue.greater(s1, s2, sout);
				return;
			case GREATER_OR_EQUAL:
				ScriptValue.greaterOrEqual(s1, s2, sout);
				return;
			case EQUAL:
				ScriptValue.equal(s1, s2, sout);
				return;
			case NOT_EQUAL:
				ScriptValue.notEqual(s1, s2, sout);
				return;
			case STRICT_EQUAL:
				ScriptValue.strictEqual(s1, s2, sout);
				return;
			case STRICT_NOT_EQUAL:
				ScriptValue.strictNotEqual(s1, s2, sout);
				return;
			default:
				return;
		}
	}

	// Checks if a script command type is a binary stack operator.
	private static boolean isUnaryOperatorCommand(ScriptCommandType type)
	{
		switch (type)
		{
			case ABSOLUTE:
			case NEGATE:
			case LOGICAL_NOT:
			case NOT:
				return true;
			default:
				return false;
		}
	}

	// Performs a unary operator command.
	private static void doUnaryOperatorCommand(ScriptCommandType type, ScriptValue s1, ScriptValue sout)
	{
		switch (type)
		{
			case ABSOLUTE:
				ScriptValue.absolute(s1, sout);
				return;
			case NEGATE:
				ScriptValue.negate(s1, sout);
				return;
			case LOGICAL_NOT:
				ScriptValue.logicalNot(s1, sout);
				return;
			case NOT:
				ScriptValue.not(s1, sout);
				return;
			default:
				return;
		}
	}
}

