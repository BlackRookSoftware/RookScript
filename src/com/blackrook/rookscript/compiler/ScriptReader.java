/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.CountMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.lang.CommonLexer;
import com.blackrook.lang.CommonLexerKernel;
import com.blackrook.lang.Parser;
import com.blackrook.rookscript.Script;
import com.blackrook.rookscript.ScriptCommand;
import com.blackrook.rookscript.ScriptCommandType;
import com.blackrook.rookscript.ScriptFunctionResolver;
import com.blackrook.rookscript.ScriptFunctionType;
import com.blackrook.rookscript.Script.Entry;
import com.blackrook.rookscript.exception.ScriptParseException;
import com.blackrook.rookscript.struct.ScriptValue;

/**
 * The main factory for reading and parsing a script file.
 * @author Matthew Tropiano
 */
public final class ScriptReader
{
	public static final String STREAMNAME_TEXT = "[Text String]";
	
	/** Label prefix. */
	public static final String LABEL_IF_CONDITIONAL = "if_cond_";
	/** Label prefix. */
	public static final String LABEL_IF_SUCCESS = "if_true_";
	/** Label prefix. */
	public static final String LABEL_IF_FAILURE = "if_false_";
	/** Label prefix. */
	public static final String LABEL_IF_END = "if_end_";
	/** Label prefix. */
	public static final String LABEL_WHILE_CONDITIONAL = "while_cond_";
	/** Label prefix. */
	public static final String LABEL_WHILE_SUCCESS = "while_true_";
	/** Label prefix. */
	public static final String LABEL_WHILE_END = "while_end_";
	/** Label prefix. */
	public static final String LABEL_FOR_INIT = "for_init_";
	/** Label prefix. */
	public static final String LABEL_FOR_CONDITIONAL = "for_cond_";
	/** Label prefix. */
	public static final String LABEL_FOR_STEP = "for_step_";
	/** Label prefix. */
	public static final String LABEL_FOR_SUCCESS = "for_true_";
	/** Label prefix. */
	public static final String LABEL_FOR_END = "for_end_";
	/** Label prefix. */
	public static final String LABEL_TERNARY_TRUE = "tern_true_";
	/** Label prefix. */
	public static final String LABEL_TERNARY_FALSE = "tern_false_";
	/** Label prefix. */
	public static final String LABEL_TERNARY_END = "tern_end_";
	/** Label prefix. */
	public static final String LABEL_SSAND_TRUE = "ssand_true_";
	/** Label prefix. */
	public static final String LABEL_SSAND_FALSE = "ssand_false_";
	/** Label prefix. */
	public static final String LABEL_SSAND_END = "ssand_end_";
	/** Label prefix. */
	public static final String LABEL_SSOR_TRUE = "ssor_true_";
	/** Label prefix. */
	public static final String LABEL_SSOR_FALSE = "ssor_false_";
	/** Label prefix. */
	public static final String LABEL_SSOR_END = "ssor_end_";

	/** The singular instance for the kernel. */
	private static final SKernel KERNEL_INSTANCE = new SKernel();
	/** The singular instance for the default includer. */
	private static final ScriptReaderIncluder DEFAULT_INCLUDER = new DefaultIncluder();
	/** The singular instance for the default includer. */
	private static final ScriptReaderOptions DEFAULT_OPTIONS = new ScriptReaderOptions()
	{
		@Override
		public boolean isOptimizing() 
		{
			return true;
		}
		
		@Override
		public String[] getDefines()
		{
			return new String[0];
		}
	};
	
	/** 
	 * Default includer to use when none specified.
	 * This includer can either pull from the classpath, URIs, or files.
	 * <p>&nbsp;</p>
	 * <ul>
	 * <li>Paths that start with {@code classpath:} are parsed as resource paths in the current classpath.</li>
	 * <li>
	 * 		Else, the path is interpreted as a file path, with the following search order:
	 * 		<ul>
	 * 			<li>Relative to parent of source stream.</li>
	 * 			<li>As is.</li>
	 * 		</ul>
	 * </li>
	 * </ul> 
	 */
	public static class DefaultIncluder implements ScriptReaderIncluder
	{
		private static final String CLASSPATH_PREFIX = "classpath:";
		
		// cannot be instantiated outside of this class.
		private DefaultIncluder(){}

		@Override
		public String getIncludeResourceName(String streamName, String path) throws IOException
		{
			if (Common.isWindows() && streamName.contains("\\")) // check for Windows paths.
				streamName = streamName.replace('\\', '/');
			
			String streamParent = null;
			int lidx = -1; 
			if ((lidx = streamName.lastIndexOf('/')) >= 0)
				streamParent = streamName.substring(0, lidx + 1);

			if (path.startsWith(CLASSPATH_PREFIX) || (streamParent != null && streamParent.startsWith(CLASSPATH_PREFIX)))
				return ((streamParent != null ? streamParent : "") + path);
			else
			{
				File f = null;
				if (streamParent != null)
				{
					f = new File(streamParent + path);
					if (f.exists())
						return f.getPath();
					else
						return path;
				}
				else
				{
					return path;
				}
			}
		}
		
		@Override
		public InputStream getIncludeResource(String path) throws IOException
		{
			if (path.startsWith(CLASSPATH_PREFIX))
				return Common.openResource(path.substring(CLASSPATH_PREFIX.length()));
			else
				return new FileInputStream(new File(path));
		}
	};
	
	private ScriptReader() {}
	
	/**
	 * The script language kernel.
	 */
	private static class SKernel extends CommonLexerKernel
	{
		public static final int TYPE_COMMENT = 0;
		public static final int TYPE_LPAREN = 1;
		public static final int TYPE_RPAREN = 2;
		public static final int TYPE_COMMA = 5;
		public static final int TYPE_SEMICOLON = 6;
		public static final int TYPE_LBRACK = 7;
		public static final int TYPE_RBRACK = 8;
		public static final int TYPE_QUESTIONMARK = 9;
		public static final int TYPE_COLON = 10;
		public static final int TYPE_PERIOD = 11;
		
		public static final int TYPE_DASH = 20;
		public static final int TYPE_PLUS = 21;
		public static final int TYPE_STAR = 22;
		public static final int TYPE_SLASH = 23;
		public static final int TYPE_PERCENT = 24;
		public static final int TYPE_AMPERSAND = 25;
		public static final int TYPE_DOUBLEAMPERSAND = 26;
		public static final int TYPE_PIPE = 27;
		public static final int TYPE_DOUBLEPIPE = 28;
		public static final int TYPE_GREATER = 29;
		public static final int TYPE_DOUBLEGREATER = 30;
		public static final int TYPE_TRIPLEGREATER = 31;
		public static final int TYPE_GREATEREQUAL = 32;
		public static final int TYPE_LESS = 33;
		public static final int TYPE_LESSEQUAL = 34;
		public static final int TYPE_DOUBLELESS = 35;
		public static final int TYPE_EQUAL = 36;
		public static final int TYPE_DOUBLEEQUAL = 37;
		public static final int TYPE_TRIPLEEQUAL = 38;
		public static final int TYPE_NOTEQUAL = 39;
		public static final int TYPE_NOTDOUBLEEQUAL = 40;
		public static final int TYPE_EXCLAMATION = 41;
		public static final int TYPE_TILDE = 42;
		public static final int TYPE_CARAT = 43;
		public static final int TYPE_ABSOLUTE = 44; // not scanned
		public static final int TYPE_NEGATE = 45; // not scanned

		public static final int TYPE_DASHEQUALS = 70;
		public static final int TYPE_PLUSEQUALS = 71;
		public static final int TYPE_STAREQUALS = 72;
		public static final int TYPE_SLASHEQUALS = 73;
		public static final int TYPE_PERCENTEQUALS = 74;
		public static final int TYPE_AMPERSANDEQUALS = 75;
		public static final int TYPE_PIPEEQUALS = 76;
		public static final int TYPE_DOUBLEGREATEREQUALS = 77;
		public static final int TYPE_TRIPLEGREATEREQUALS = 78;
		public static final int TYPE_DOUBLELESSEQUALS = 79;

		public static final int TYPE_TRUE = 100;
		public static final int TYPE_FALSE = 101;
		public static final int TYPE_LBRACE = 102;
		public static final int TYPE_RBRACE = 103;
		public static final int TYPE_INFINITY = 104;
		public static final int TYPE_NAN = 105;
		public static final int TYPE_RETURN = 106;
		public static final int TYPE_IF = 107;
		public static final int TYPE_ELSE = 108;
		public static final int TYPE_WHILE = 109;
		public static final int TYPE_FOR = 110;
		public static final int TYPE_ENTRY = 111;
		public static final int TYPE_FUNCTION = 112;
		public static final int TYPE_PRAGMA = 113;
		public static final int TYPE_MAIN = 114;
		public static final int TYPE_BREAK = 115;
		public static final int TYPE_CONTINUE = 116;
		
		private SKernel()
		{
			addStringDelimiter('"', '"');
			setDecimalSeparator('.');
			
			addCommentStartDelimiter("/*", TYPE_COMMENT);
			addCommentLineDelimiter("//", TYPE_COMMENT);
			addCommentEndDelimiter("*/", TYPE_COMMENT);

			addDelimiter("(", TYPE_LPAREN);
			addDelimiter(")", TYPE_RPAREN);
			addDelimiter("{", TYPE_LBRACE);
			addDelimiter("}", TYPE_RBRACE);
			addDelimiter("[", TYPE_LBRACK);
			addDelimiter("]", TYPE_RBRACK);
			addDelimiter(",", TYPE_COMMA);
			addDelimiter(".", TYPE_PERIOD);
			addDelimiter(";", TYPE_SEMICOLON);
			addDelimiter(":", TYPE_COLON);
			addDelimiter("?", TYPE_QUESTIONMARK);

			addDelimiter("+", TYPE_PLUS);
			addDelimiter("-", TYPE_DASH);
			addDelimiter("*", TYPE_STAR);
			addDelimiter("/", TYPE_SLASH);
			addDelimiter("%", TYPE_PERCENT);
			addDelimiter("&", TYPE_AMPERSAND);
			addDelimiter("|", TYPE_PIPE);
			addDelimiter(">>", TYPE_DOUBLEGREATER);
			addDelimiter(">>>", TYPE_TRIPLEGREATER);
			addDelimiter("<<", TYPE_DOUBLELESS);
			
			addDelimiter("!", TYPE_EXCLAMATION);
			addDelimiter("~", TYPE_TILDE);
			addDelimiter("&&", TYPE_DOUBLEAMPERSAND);
			addDelimiter("||", TYPE_DOUBLEPIPE);
			addDelimiter("^", TYPE_CARAT);
			addDelimiter(">", TYPE_GREATER);
			addDelimiter(">=", TYPE_GREATEREQUAL);
			addDelimiter("<", TYPE_LESS);
			addDelimiter("<=", TYPE_LESSEQUAL);
			addDelimiter("==", TYPE_DOUBLEEQUAL);
			addDelimiter("===", TYPE_TRIPLEEQUAL);
			addDelimiter("!=", TYPE_NOTEQUAL);
			addDelimiter("!==", TYPE_NOTDOUBLEEQUAL);
			
			addDelimiter("=", TYPE_EQUAL);

			addDelimiter("+=", TYPE_PLUSEQUALS);
			addDelimiter("-=", TYPE_DASHEQUALS);
			addDelimiter("*=", TYPE_STAREQUALS);
			addDelimiter("/=", TYPE_SLASHEQUALS);
			addDelimiter("%=", TYPE_PERCENTEQUALS);
			addDelimiter("&=", TYPE_AMPERSANDEQUALS);
			addDelimiter("|=", TYPE_PIPEEQUALS);
			addDelimiter(">>=", TYPE_DOUBLEGREATEREQUALS);
			addDelimiter(">>>=", TYPE_TRIPLEGREATEREQUALS);
			addDelimiter("<<=", TYPE_DOUBLELESSEQUALS);
			
			addCaseInsensitiveKeyword("true", TYPE_TRUE);
			addCaseInsensitiveKeyword("false", TYPE_FALSE);
			addCaseInsensitiveKeyword("infinity", TYPE_INFINITY);
			addCaseInsensitiveKeyword("nan", TYPE_NAN);
			addCaseInsensitiveKeyword("if", TYPE_IF);
			addCaseInsensitiveKeyword("else", TYPE_ELSE);
			addCaseInsensitiveKeyword("return", TYPE_RETURN);
			addCaseInsensitiveKeyword("while", TYPE_WHILE);
			addCaseInsensitiveKeyword("for", TYPE_FOR);
			addCaseInsensitiveKeyword("entry", TYPE_ENTRY);
			addCaseInsensitiveKeyword("function", TYPE_FUNCTION);
			addCaseInsensitiveKeyword("main", TYPE_MAIN);
			addCaseInsensitiveKeyword("break", TYPE_BREAK);
			addCaseInsensitiveKeyword("continue", TYPE_CONTINUE);
			addCaseInsensitiveKeyword("pragma", TYPE_PRAGMA);
			
		}
		
	}
	
	/**
	 * The lexer for a reader context.
	 */
	private static class SLexer extends CommonLexer
	{
		private ScriptReaderIncluder includer;
		
		private SLexer(Reader in, ScriptReaderIncluder includer)
		{
			super(KERNEL_INSTANCE, in);
			this.includer = includer;
		}
	
		private SLexer(String in, ScriptReaderIncluder includer)
		{
			super(KERNEL_INSTANCE, in);
			this.includer = includer;
		}
		
		private SLexer(String name, Reader in, ScriptReaderIncluder includer)
		{
			super(KERNEL_INSTANCE, name, in);
			this.includer = includer;
		}
	
		private SLexer(String name, String in, ScriptReaderIncluder includer)
		{
			super(KERNEL_INSTANCE, name, in);
			this.includer = includer;
		}
		
		@Override
		protected String getNextResourceName(String currentStreamName, String includePath) throws IOException 
		{
			return includer.getIncludeResourceName(currentStreamName, includePath);
		}
		
		@Override
		protected InputStream getResource(String path) throws IOException
		{
			return includer.getIncludeResource(path);
		}
	}

	/**
	 * The parser that parses text for the resultant script. 
	 */
	private static class SParser extends Parser
	{
		/** The reader options. */
		private ScriptReaderOptions options;
		/** The resolver for host functions. */
		private ScriptFunctionResolver hostFunctionResolver;

		/** Current script. */
		private Script currentScript;
		/** Command list. */
		private Queue<ScriptCommand> commandList;
		/** Label counter map. */
		private CountMap<String> labelCounter;
		/** Map of local function declarations. */
		private CaseInsensitiveHashMap<Integer> functionMap;
		
		// Creates the next parser.
		private SParser(SLexer lexer, ScriptReaderOptions options, ScriptFunctionResolver hostFunctionResolver)
		{
			super(lexer);
			for (String def : options.getDefines())
				lexer.addDefineMacro(def);
			this.options = options;
			this.hostFunctionResolver = hostFunctionResolver;
		}
		
		// Gets a named label for output.
		private String getFunctionLabel(String name)
		{
			return Script.LABEL_FUNCTION_PREFIX+name;
		}

		// Gets a named label for output.
		private String getScriptLabel(String name)
		{
			return Script.LABEL_ENTRY_PREFIX+name;
		}

		// Gets the next label for output.
		private String getNextLabel(String prefix)
		{
			int i = labelCounter.getCount(prefix);
			labelCounter.give(prefix);
			return prefix+i;
		}
		
		/** Marks a label on the current command. */
		private int mark(String label)
		{
			int out;
			currentScript.setIndex(label, out = commandList.size());
			return out;
		}

		/** Emits a command. */
		private void emit(ScriptCommand command)
		{
			commandList.add(command);
		}
		
		// Starts the script parsing.
		private Script readScript()
		{
			commandList = new Queue<>();
			labelCounter = new CountMap<>();
			currentScript = new Script();
			functionMap = new CaseInsensitiveHashMap<>();

			// prime first token.
			nextToken();
			
			// keep parsing entries.
			boolean noError = true;
			
			try {
				while (currentToken() != null && (noError = parseEntryList())) ;
			} catch (ScriptParseException e) {
				addErrorMessage(e.getMessage());
				noError = false;
			}
			
			if (!noError) // awkward, I know.
			{
				String[] errors = getErrorMessages();
				if (errors.length > 0)
				{
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < errors.length; i++)
					{
						sb.append(errors[i]);
						if (i < errors.length-1)
							sb.append('\n');
					}
					throw new ScriptParseException(sb.toString());
				}
			}

			ScriptCommand[] commands = new ScriptCommand[commandList.size()];
			commandList.toArray(commands);
			currentScript.setCommands(commands);
			if (options.isOptimizing())
			{
				Script outScript = optimize(currentScript);
				outScript.setHostFunctionResolver(hostFunctionResolver);
				return outScript;
			}
			else
			{
				currentScript.setHostFunctionResolver(hostFunctionResolver);
				return currentScript;
			}
		}
		
		/**
		 * Optimizes a script.
		 * @param script the input script.
		 * @return the new script after optimization.
		 */
		private Script optimize(Script script)
		{
			Stack<ScriptCommand> reduceStack = new Stack<>();
			Stack<ScriptCommand> backwardsStack = new Stack<>();
			Queue<ScriptCommand> outCommands = new Queue<>();
			
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
							outCommands.enqueue(newCommand);
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
							outCommands.enqueue(newCommand);
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
				}
				return true;
			}
			return false;
		}

		// Reduces the reduction stack, emitting commands until it can't.
		private static void optimizeEmitAll(Stack<ScriptCommand> reduceStack, Stack<ScriptCommand> backwardsStack, Queue<ScriptCommand> emitQueue)
		{
			while (!reduceStack.isEmpty())
				backwardsStack.push(reduceStack.pop());
			while (!backwardsStack.isEmpty())
				emitQueue.enqueue(backwardsStack.pop());
		}

		// Reduces the reduction stack.
		private static void optimizeReduce(Stack<ScriptCommand> reduceStack)
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

		// Parse a bunch of entries.
		private boolean parseEntryList()
		{
			while (currentToken() != null && isEntryStart(currentToken().getType()))
			{
				if (!parseEntry())
					return false;
			}
			
			if (currentToken() != null)
			{
				addErrorMessage("Expected an \"main\", \"function\", \"entry\", or \"pragma\" entry.");
				return false;
			}
			
			return true;
		}
		
		/*
			<Entry> :=
				<INIT> <InitEntry>
				<FUNCTION> <FunctionEntry>
				<SCRIPT> <ScriptName> "(" <ScriptEntryArgumentList> ")" "{" <StatementList> "}"
				<PRAGMA> ...
		 */
		private boolean parseEntry()
		{
			// main entry.
			if (matchType(SKernel.TYPE_MAIN))
			{
				return parseMainEntry();
			}
			// function entry.
			else if (matchType(SKernel.TYPE_FUNCTION))
			{
				return parseFunctionEntry();
			}
			// entry.
			else if (matchType(SKernel.TYPE_ENTRY))
			{
				return parseNamedEntry();
			}
			// pragma entry.
			else if (matchType(SKernel.TYPE_PRAGMA))
			{
				return parsePragmaEntry();
			}
			else
			{
				addErrorMessage("Expected an \"main\", \"function\", \"entry\", or \"pragma\" entry.");
				return false;
			}
		}

		
		/* 
		 	<InitEntry> := 
		 		"(" ")" "{" <StatementList> "}"
		 */
		private boolean parseMainEntry()
		{
			if (currentScript.getIndex(Script.LABEL_MAIN) >= 0)
			{
				addErrorMessage("The \"main\" entry was already defined.");
				return false;
			}
			
			mark(Script.LABEL_MAIN);
			
			if (!matchType(SKernel.TYPE_LPAREN))
			{
				addErrorMessage("Expected \"(\" after \"main\".");
				return false;
			}

			if (!matchType(SKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}

			// start statement list?
			if (!matchType(SKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" to start \"main\" body.");
				return false;
			}
			
			if (!parseStatementList(null, null))
				return false;
			
			if (!matchType(SKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected \"}\" to close \"main\" body.");
				return false;
			}

			emit(ScriptCommand.create(ScriptCommandType.PUSH, false));
			emit(ScriptCommand.create(ScriptCommandType.RETURN));
			return true;
		}
		
		/* 
		 	<FunctionEntry> := 
		 		<IDENTIFIER> "(" <FunctionArgumentList> ")" "{" <StatementList> "}"
		 */
		private boolean parseFunctionEntry()
		{
			if (!currentType(SKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected an identifier for the function name.");
				return false;
			}
		
			String name = currentToken().getLexeme();
			nextToken();
			
			String label = getFunctionLabel(name);
			if (currentScript.getIndex(label) >= 0)
			{
				addErrorMessage("The function entry \"" + name + "\" was already defined.");
				return false;
			}
			
			mark(label);
			
			if (!matchType(SKernel.TYPE_LPAREN))
			{
				addErrorMessage("Expected \"(\" after function name.");
				return false;
			}
		
			int paramAmount = 0;
			if (currentType(SKernel.TYPE_IDENTIFIER))
			{
				Stack<String> paramNameStack = new Stack<>();
				paramNameStack.push(currentToken().getLexeme());
				nextToken();
				
				while (matchType(SKernel.TYPE_COMMA))
				{
					if (!currentType(SKernel.TYPE_IDENTIFIER))
					{
						addErrorMessage("Expected identifier after \",\" for parameter name.");
						return false;
					}
					
					paramNameStack.push(currentToken().getLexeme());
					nextToken();
				}
				
				paramAmount = paramNameStack.size();
				
				while (!paramNameStack.isEmpty())
					emit(ScriptCommand.create(ScriptCommandType.POP_VARIABLE, paramNameStack.pop()));
				
			}
			
			functionMap.put(name, paramAmount);
			
			if (!matchType(SKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
		
			// start statement list?
			if (!matchType(SKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" to start \"main\" body.");
				return false;
			}
			
			if (!parseStatementList(null, null))
				return false;
			
			if (!matchType(SKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected \"}\" to close \"main\" body.");
				return false;
			}
			
			emit(ScriptCommand.create(ScriptCommandType.PUSH, false));
			emit(ScriptCommand.create(ScriptCommandType.RETURN));
			return true;
		}

		/* 
	 		<ScriptEntry> := 
	 			<ScriptName> "(" <ScriptEntryArgumentList> ")" "{" <StatementList> "}"
		 */
		private boolean parseNamedEntry()
		{
			if (!currentType(SKernel.TYPE_IDENTIFIER, SKernel.TYPE_NUMBER, SKernel.TYPE_STRING))
			{
				addErrorMessage("Expected an identifier for the function name.");
				return false;
			}
		
			String name = currentToken().getLexeme();
			nextToken();
			
			String label = getScriptLabel(name);
			if (currentScript.getScriptEntry(name) != null)
			{
				addErrorMessage("The script entry \"" + name + "\" was already defined.");
				return false;
			}
			
			int index = mark(label);
			
			if (!matchType(SKernel.TYPE_LPAREN))
			{
				addErrorMessage("Expected \"(\" after function name.");
				return false;
			}
		
			int paramAmount = 0;
			if (currentType(SKernel.TYPE_IDENTIFIER))
			{
				Stack<String> paramNameStack = new Stack<>();
				paramNameStack.push(currentToken().getLexeme());
				nextToken();
				
				while (matchType(SKernel.TYPE_COMMA))
				{
					if (!currentType(SKernel.TYPE_IDENTIFIER))
					{
						addErrorMessage("Expected identifier after \",\" for parameter name.");
						return false;
					}
					
					paramNameStack.push(currentToken().getLexeme());
					nextToken();
				}
				
				paramAmount = paramNameStack.size();
				
				while (!paramNameStack.isEmpty())
					emit(ScriptCommand.create(ScriptCommandType.POP_VARIABLE, paramNameStack.pop()));
				
			}
			
			currentScript.setScriptEntry(name, paramAmount, index);
			
			if (!matchType(SKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
		
			// start statement list?
			if (!matchType(SKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" to start \"main\" body.");
				return false;
			}
			
			if (!parseStatementList(null, null))
				return false;
			
			if (!matchType(SKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected \"}\" to close \"main\" body.");
				return false;
			}
		
			emit(ScriptCommand.create(ScriptCommandType.PUSH, false));
			emit(ScriptCommand.create(ScriptCommandType.RETURN));
			return true;
		}

		/* 
			<PragmaEntry> := ...
		 */
		private boolean parsePragmaEntry()
		{
			if (!matchType(SKernel.TYPE_SEMICOLON))
			{
				addErrorMessage("Expected \";\" to terminate PRAGMA directive statement.");
				return false;
			}
			
			return true;
		}
		
		/*
			<StatementList> :=
				<StatementClause> ";" <StatementList>
				[e]
		 */
		private boolean parseStatementList(String breakLabel, String continueLabel)
		{
			while (isStatementStart(currentToken().getType()) || isControlStatementStart(currentToken().getType()))
			{
				// standard statement?
				if (isStatementStart(currentToken().getType()))
				{
					if (!parseStatementClause(breakLabel, continueLabel))
						return false;
					
					if (!matchType(SKernel.TYPE_SEMICOLON))
					{
						addErrorMessage("Expected \";\" to terminate statement.");
						return false;
					}
				}
				// control statement?
				else if (isControlStatementStart(currentToken().getType()))
				{
					if (!parseStatementClause(breakLabel, continueLabel))
						return false;
				}
			}
			
			return true;
		}
		
		/*
			<StatementBody> :=
				"{" <StatementList> "}"
				<StatementClause> ";"
		 */
		private boolean parseStatementBody(String breakLabel, String continueLabel)
		{
			// start statement list?
			if (matchType(SKernel.TYPE_LBRACE))
			{
				if (!parseStatementList(breakLabel, continueLabel))
					return false;
				
				if (!matchType(SKernel.TYPE_RBRACE))
				{
					addErrorMessage("Expected \"}\" to terminate statement list.");
					return false;
				}

				return true;
			}
			// standard statement?
			else if (isStatementStart(currentToken().getType()))
			{
				if (!parseStatementClause(breakLabel, continueLabel))
					return false;
				
				if (!matchType(SKernel.TYPE_SEMICOLON))
				{
					addErrorMessage("Expected \";\" to terminate statement.");
					return false;
				}

				return true;
			}
			// control statement?
			else if (isControlStatementStart(currentToken().getType()))
			{
				return parseStatementClause(breakLabel, continueLabel);
			}
			else
			{
				addErrorMessage("Expected statement.");
				return false;
			}
			
		}
		
		/*
			<Statement> :=
				";"																	(No-op)
				<BREAK>																(only in for or while)
				<CONTINUE>															(only in for or while)
				<RETURN>
				<IF> "(" <Expression> ")" <StatementBody> <ElseClause>
				<WHILE> "(" <Expression> ")" <StatementBody>
				<FOR> "(" <Statement> ";" <Expression> ";" <Statement> ")" <StatementBody>
				<IDENTIFIER> <IdentifierStatement>
		 */
		// the breaklabel or continuelabel can both be null.
		private boolean parseStatementClause(String breakLabel, String continueLabel)
		{
			// empty statement
			if (currentType(SKernel.TYPE_SEMICOLON))
			{
				emit(ScriptCommand.create(ScriptCommandType.NOOP));
				return true;
			}
			// break clause.
			else if (matchType(SKernel.TYPE_BREAK))
			{
				if (breakLabel == null)
				{
					addErrorMessage("\"Break\" used outside of a loop.");
					return false;
				}
				
				emit(ScriptCommand.create(ScriptCommandType.JUMP, breakLabel));
				return true;
			}
			// continue clause.
			else if (matchType(SKernel.TYPE_CONTINUE))
			{
				if (continueLabel == null)
				{
					addErrorMessage("\"Continue\" used outside of a loop.");
					return false;
				}
				
				emit(ScriptCommand.create(ScriptCommandType.JUMP, continueLabel));
				return true;
			}
			// return clause.
			else if (matchType(SKernel.TYPE_RETURN))
			{
				// if no return, return false.
				if (currentType(SKernel.TYPE_SEMICOLON))
				{
					emit(ScriptCommand.create(ScriptCommandType.PUSH, false));
					emit(ScriptCommand.create(ScriptCommandType.RETURN));
					return true;
				}
				
				if (!parseExpression())
					return false;
				
				emit(ScriptCommand.create(ScriptCommandType.RETURN));
				return true;
			}
			// if control clause.
			else if (matchType(SKernel.TYPE_IF))
			{
				return parseIfClause(breakLabel, continueLabel);
			}
			// while control clause.
			else if (matchType(SKernel.TYPE_WHILE))
			{
				return parseWhileClause();
			}
			// for control clause.
			else if (matchType(SKernel.TYPE_FOR))
			{
				return parseForClause();
			}
			// assignment statement or function call.
			else if (currentType(SKernel.TYPE_IDENTIFIER))
			{
				String lexeme = currentToken().getLexeme();
				nextToken();
				
				if (!parseIdentifierStatement(lexeme))
					return false;
				
				return true;
			}
			else
			{
				addErrorMessage("Expected a valid statement.");
				return false;
			}
			
		}

		/*
			<IdentifierStatement> :=
				"(" <ParameterList> ")" ";"   										(Must be function or host function)
				"[" <Expression> "]" <ASSIGNMENTOPERATOR> <VariableAssignment> ";"	(Array variable assignment)
				"." <IDENTIFIER> <ASSIGNMENTOPERATOR> <Expression> ";"				(scoped variable)
				<ASSIGNMENTOPERATOR> <Expression> ";"								(variable assignment)
		 */
		private boolean parseIdentifierStatement(String identifierName)
		{
			// function call.
			if (matchType(SKernel.TYPE_LPAREN))
			{
				// test type of call: host function first, then local script function.
				ScriptFunctionType functionType;
				if ((functionType = hostFunctionResolver.getFunctionByName(identifierName)) != null)
				{
					if (!parseHostFunctionCall(functionType))
						return false;
						
					if (!matchType(SKernel.TYPE_RPAREN))
					{
						addErrorMessage("Expected \")\" after a host function call's parameters.");
						return false;
					}
					
					emit(ScriptCommand.create(ScriptCommandType.CALL_HOST, identifierName));
					
					if (!functionType.isVoid())
						emit(ScriptCommand.create(ScriptCommandType.POP));
					
					return true;
				}
				else if (functionMap.containsKey(identifierName))
				{
					int paramCount = functionMap.get(identifierName);
					if (!parseFunctionCall(paramCount))
						return false;
						
					if (!matchType(SKernel.TYPE_RPAREN))
					{
						addErrorMessage("Expected \")\" after a function call's parameters.");
						return false;
					}
					
					emit(ScriptCommand.create(ScriptCommandType.CALL, (Script.LABEL_FUNCTION_PREFIX + identifierName).toLowerCase()));
					// local functions always return things. Must pop.
					emit(ScriptCommand.create(ScriptCommandType.POP));
					return true;
				}
				else
				{
					addErrorMessage("\"" + identifierName + "\" is not the name of a valid function call - not host or local.");
					return false;
				}
				
			}
			// list index assignment.
			else if (currentType(SKernel.TYPE_LBRACK))
			{
				emit(ScriptCommand.create(ScriptCommandType.PUSH_VARIABLE, identifierName));
				
				while (currentType(SKernel.TYPE_LBRACK))
				{
					nextToken();
					if (!parseExpression())
						return false;

					if (!matchType(SKernel.TYPE_RBRACK))
					{
						addErrorMessage("Expected \"]\" after a list index expression.");
						return false;
					}

					// another dimension incoming?
					if (currentType(SKernel.TYPE_LBRACK))
					{
						emit(ScriptCommand.create(ScriptCommandType.PUSH_LIST_INDEX));
					}
					
				}
				
				int assignmentType = currentToken().getType();
				if (!isAssignmentOperator(assignmentType))
				{
					addErrorMessage("Expected assignment operator after a list reference.");
					return false;
				}
				nextToken();
				
				if (isAccumulatingAssignmentOperator(assignmentType))
					emit(ScriptCommand.create(ScriptCommandType.PUSH_LIST_INDEX_CONTENTS));
				
				if (!parseExpression())
					return false;
				
				if (isAccumulatingAssignmentOperator(assignmentType))
					emitArithmeticCommand(assignmentType);

				emit(ScriptCommand.create(ScriptCommandType.POP_LIST));
				return true;
			}
			// is assignment?
			else if (isAssignmentOperator(currentToken().getType()))
			{
				int assignmentType = currentToken().getType();
				nextToken();
				
				if (isAccumulatingAssignmentOperator(assignmentType))
					emit(ScriptCommand.create(ScriptCommandType.PUSH_VARIABLE, identifierName));
				
				if (!parseExpression())
					return false;
				
				if (isAccumulatingAssignmentOperator(assignmentType))
					emitArithmeticCommand(assignmentType);
				
				emit(ScriptCommand.create(ScriptCommandType.POP_VARIABLE, identifierName));
				return true;
			}
			else
			{
				addErrorMessage("Expected a valid statement.");
				return false;
			}
		}
		
		// 	<IF> "(" <Expression> ")" <StatementBody> <ElseClause>
		private boolean parseIfClause(String breakLabel, String continueLabel)
		{
			if (!matchType(SKernel.TYPE_LPAREN))
			{
				addErrorMessage("Expected \"(\" to start \"if\" conditional.");
				return false;
			}
		
			String condLabel = getNextLabel(LABEL_IF_CONDITIONAL); 
			String successLabel = getNextLabel(LABEL_IF_SUCCESS); 
			String failLabel = getNextLabel(LABEL_IF_FAILURE); 
			String endLabel = getNextLabel(LABEL_IF_END); 
			
			mark(condLabel);
			if (!parseExpression())
				return false;
		
			if (!matchType(SKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\" to end \"if\" conditional.");
				return false;
			}
		
			emit(ScriptCommand.create(ScriptCommandType.JUMP_FALSE, failLabel));
		
			mark(successLabel);
			if (!parseStatementBody(breakLabel, continueLabel))
				return false;
		
			emit(ScriptCommand.create(ScriptCommandType.JUMP, endLabel));
		
			// look for else block.
			mark(failLabel);
			if (currentType(SKernel.TYPE_ELSE))
			{
				nextToken();
				if (!parseStatementBody(breakLabel, continueLabel))
					return false;
		
				emit(ScriptCommand.create(ScriptCommandType.JUMP, endLabel));
			}
			
			mark(endLabel);
			return true;
		}

		// <WHILE> "(" <Expression> ")" <StatementBody>
		private boolean parseWhileClause()
		{
			if (!matchType(SKernel.TYPE_LPAREN))
			{
				addErrorMessage("Expected \"(\" to start \"while\" conditional.");
				return false;
			}
		
			String condLabel = getNextLabel(LABEL_WHILE_CONDITIONAL); 
			String successLabel = getNextLabel(LABEL_WHILE_SUCCESS); 
			String endLabel = getNextLabel(LABEL_WHILE_END); 
			
			mark(condLabel);
			if (!parseExpression())
				return false;
		
			if (!matchType(SKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\" to end \"while\" conditional.");
				return false;
			}
		
			emit(ScriptCommand.create(ScriptCommandType.JUMP_FALSE, endLabel));
		
			mark(successLabel);
			if (!parseStatementBody(endLabel, condLabel))
				return false;

			emit(ScriptCommand.create(ScriptCommandType.JUMP, condLabel));
		
			mark(endLabel);
			return true;
		}

		// <FOR> "(" <Statement> ";" <Expression> ";" <Statement> ")" <StatementBody>
		private boolean parseForClause()
		{
			if (!matchType(SKernel.TYPE_LPAREN))
			{
				addErrorMessage("Expected \"(\" to start \"for\" clauses.");
				return false;
			}

			String initLabel = getNextLabel(LABEL_FOR_INIT); 
			String condLabel = getNextLabel(LABEL_FOR_CONDITIONAL); 
			String stepLabel = getNextLabel(LABEL_FOR_STEP); 
			String successLabel = getNextLabel(LABEL_FOR_SUCCESS); 
			String endLabel = getNextLabel(LABEL_FOR_END); 
			
			mark(initLabel);
			if (!parseStatementClause(null, null))
				return false;

			if (!matchType(SKernel.TYPE_SEMICOLON))
			{
				addErrorMessage("Expected a \";\" to terminate init statement.");
				return false;
			}

			mark(condLabel);
			if (!parseExpression())
				return false;
			
			if (!matchType(SKernel.TYPE_SEMICOLON))
			{
				addErrorMessage("Expected a \";\" to terminate conditional expression.");
				return false;
			}

			emit(ScriptCommand.create(ScriptCommandType.JUMP_BRANCH, successLabel, endLabel));

			mark(stepLabel);
			if (!parseStatementClause(null, null))
				return false;

			if (!matchType(SKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\" to end \"for\" clauses.");
				return false;
			}

			emit(ScriptCommand.create(ScriptCommandType.JUMP, condLabel));

			mark(successLabel);
			if (!parseStatementBody(endLabel, stepLabel))
				return false;

			emit(ScriptCommand.create(ScriptCommandType.JUMP, stepLabel));

			mark(endLabel);
			return true;
		}

		// <ExpressionPhrase>
		// If null, bad parse.
		private boolean parseExpression()
		{
			// make stacks.
			Stack<Integer> operatorStack = new Stack<>();
			int[] expressionValueCounter = new int[1];
			
			// was the last read token a value?
			boolean lastWasValue = false;
			boolean keepGoing = true;		
		
			while (keepGoing)
			{
				// if no more tokens...
				if (currentToken() == null)
				{
					keepGoing = false;
				}
				// if the last thing seen was a value....
				else if (lastWasValue)
				{
					int type = currentToken().getType();
					if (isBinaryOperatorType(type))
					{
						int nextOperator;
						switch (type)
						{
							case SKernel.TYPE_PLUS:
							case SKernel.TYPE_DASH:
							case SKernel.TYPE_STAR:
							case SKernel.TYPE_SLASH:
							case SKernel.TYPE_PERCENT:
							case SKernel.TYPE_AMPERSAND:
							case SKernel.TYPE_PIPE:
							case SKernel.TYPE_CARAT:
							case SKernel.TYPE_GREATER:
							case SKernel.TYPE_GREATEREQUAL:
							case SKernel.TYPE_DOUBLEGREATER:
							case SKernel.TYPE_TRIPLEGREATER:
							case SKernel.TYPE_LESS:
							case SKernel.TYPE_DOUBLELESS:
							case SKernel.TYPE_LESSEQUAL:
							case SKernel.TYPE_DOUBLEEQUAL:
							case SKernel.TYPE_TRIPLEEQUAL:
							case SKernel.TYPE_NOTEQUAL:
							case SKernel.TYPE_NOTDOUBLEEQUAL:
								nextOperator = type;
								break;
							default:
							{
								addErrorMessage("Unexpected binary operator miss.");
								return false;
							}
						}
						
						nextToken();
						if (!operatorReduce(operatorStack, expressionValueCounter, nextOperator))
							return false;
						
						operatorStack.push(nextOperator);
						lastWasValue = false;
					}
					// logical and: short circuit
					else if (matchType(SKernel.TYPE_DOUBLEAMPERSAND))
					{
						// treat with low precedence.
						if (!expressionReduceAll(operatorStack, expressionValueCounter))
							return false;
						
						String labeltrue = getNextLabel("ssand_true_");
						String labelfalse = getNextLabel("ssand_false_");
						String labelend = getNextLabel("ssand_end_");
						
						emit(ScriptCommand.create(ScriptCommandType.JUMP_FALSE, labelfalse));
						
						mark(labeltrue);
						if (!parseExpression())
							return false;
						
						emit(ScriptCommand.create(ScriptCommandType.JUMP, labelend));
						mark(labelfalse);

						emit(ScriptCommand.create(ScriptCommandType.PUSH, false));
						mark(labelend);	
					}
					// logical or: short circuit
					else if (matchType(SKernel.TYPE_DOUBLEPIPE))
					{
						// treat with low precedence.
						if (!expressionReduceAll(operatorStack, expressionValueCounter))
							return false;
						
						String labeltrue = getNextLabel("ssor_true_");
						String labelfalse = getNextLabel("ssor_false_");
						String labelend = getNextLabel("ssor_end_");
						
						emit(ScriptCommand.create(ScriptCommandType.JUMP_TRUE, labeltrue));
						
						mark(labelfalse);
						if (!parseExpression())
							return false;
						
						emit(ScriptCommand.create(ScriptCommandType.JUMP, labelend));
						mark(labeltrue);

						emit(ScriptCommand.create(ScriptCommandType.PUSH, true));
						mark(labelend);	
						
					}
					// ternary operator type.
					else if (matchType(SKernel.TYPE_QUESTIONMARK))
					{
						// treat with lowest possible precedence.
						if (!expressionReduceAll(operatorStack, expressionValueCounter))
							return false;
						
						String trueLabel = getNextLabel(LABEL_TERNARY_TRUE);
						String falseLabel = getNextLabel(LABEL_TERNARY_FALSE);
						String endLabel = getNextLabel(LABEL_TERNARY_END);
						emit(ScriptCommand.create(ScriptCommandType.JUMP_FALSE, falseLabel));
						
						mark(trueLabel);
						if (!parseExpression())
							return false;
						
						if (!matchType(SKernel.TYPE_COLON))
						{
							addErrorMessage("Expected \":\" for ternary operator separator.");
							return false;
						}
						emit(ScriptCommand.create(ScriptCommandType.JUMP, endLabel));

						mark(falseLabel);
						if (!parseExpression())
							return false;

						mark(endLabel);
					}
					else
					{
						// end on a value.
						keepGoing = false;
					}
				}
				// if the last thing seen was an operator (or nothing)...
				else
				{
					int type = currentToken().getType();
					// unary operator
					if (isUnaryOperatorType(type))
					{
						switch (type)
						{
							case SKernel.TYPE_PLUS:
								operatorStack.push(SKernel.TYPE_ABSOLUTE);
								break;
							case SKernel.TYPE_DASH:
								operatorStack.push(SKernel.TYPE_NEGATE);
								break;
							case SKernel.TYPE_EXCLAMATION:
								operatorStack.push(SKernel.TYPE_EXCLAMATION);
								break;
							case SKernel.TYPE_TILDE:
								operatorStack.push(SKernel.TYPE_TILDE);
								break;
							default:
								throw new ScriptParseException("Unexpected unary operator miss.");
						}
						nextToken();
						lastWasValue = false;
					}
					// parens.
					else if (matchType(SKernel.TYPE_LPAREN))
					{
						if (!parseExpression())
							return false;
						
						if (!matchType(SKernel.TYPE_RPAREN))
						{
							addErrorMessage("Expected ending \")\".");
							return false;
						}
						
						expressionValueCounter[0] += 1;
						lastWasValue = true;
					}

					// square brackets (literal list).
					else if (matchType(SKernel.TYPE_LBRACK))
					{
						if (!parseListLiteral())
							return false;
						
						if (!matchType(SKernel.TYPE_RBRACK))
						{
							addErrorMessage("Expected ending \"]\" to terminate list.");
							return false;
						}
						
						expressionValueCounter[0] += 1;
						lastWasValue = true;
					}
					
					// identifier - can be the start of a lot of things.
					else if (currentType(SKernel.TYPE_IDENTIFIER))
					{
						String lexeme = currentToken().getLexeme();
						nextToken();
						
						// function call?
						if (matchType(SKernel.TYPE_LPAREN))
						{
							// test type of call: host function first, then local script function.
							ScriptFunctionType functionType;
							if ((functionType = hostFunctionResolver.getFunctionByName(lexeme)) != null)
							{
								if (functionType.isVoid())
								{
									addErrorMessage("Host function returns void - cannot be used in expressions.");
									return false;
								}
								
								if (!parseHostFunctionCall(functionType))
									return false;
									
								if (!matchType(SKernel.TYPE_RPAREN))
								{
									addErrorMessage("Expected \")\" after a host function call's parameters.");
									return false;
								}
								
								emit(ScriptCommand.create(ScriptCommandType.CALL_HOST, lexeme));
							}
							else if (functionMap.containsKey(lexeme))
							{
								int paramCount = functionMap.get(lexeme);
								if (!parseFunctionCall(paramCount))
									return false;
									
								if (!matchType(SKernel.TYPE_RPAREN))
								{
									addErrorMessage("Expected \")\" after a function call's parameters.");
									return false;
								}
								
								emit(ScriptCommand.create(ScriptCommandType.CALL,  (Script.LABEL_FUNCTION_PREFIX + lexeme).toLowerCase()));
							}
							else
							{
								addErrorMessage("\"" + lexeme + "\" is not the name of a valid function call - not host or local.");
								return false;
							}

						}
						// array resolution?
						else if (currentType(SKernel.TYPE_LBRACK))
						{
							emit(ScriptCommand.create(ScriptCommandType.PUSH_VARIABLE, lexeme));

							while (matchType(SKernel.TYPE_LBRACK))
							{
								if (!parseExpression())
									return false;

								if (!matchType(SKernel.TYPE_RBRACK))
								{
									addErrorMessage("Expected \"]\" after a list index expression.");
									return false;
								}

								// another dimension incoming?
								if (currentType(SKernel.TYPE_LBRACK))
								{
									emit(ScriptCommand.create(ScriptCommandType.PUSH_LIST_INDEX));
								}
								
							}

							emit(ScriptCommand.create(ScriptCommandType.PUSH_LIST_INDEX));
						}
						// must be local variable?
						else
						{
							emit(ScriptCommand.create(ScriptCommandType.PUSH_VARIABLE, lexeme));
						}
						
						expressionValueCounter[0] += 1;
						lastWasValue = true;
					}
					// literal value?
					else if (isValidLiteralType(type))
					{
						if (!parseSingleValue())
							return false;
		
						expressionValueCounter[0] += 1;
						lastWasValue = true;
					}
					else
						throw new ScriptParseException("Expression - Expected value.");
					
				}
				
			}
			
			if (!expressionReduceAll(operatorStack, expressionValueCounter))
				return false;
			
			if (expressionValueCounter[0] != 1)
			{
				addErrorMessage("Expected valid expression.");
				return false;
			}
		
			return true;
		}

		// Parses a literally defined list.
		// 		[ .... , .... ]
		private boolean parseListLiteral()
		{
			// if no elements.
			if (currentType(SKernel.TYPE_RBRACK))
			{
				emit(ScriptCommand.create(ScriptCommandType.PUSH_LIST_NEW));
				return true;
			}
			
			if (!parseExpression())
				return false;
			
			int i = 1;
			while (matchType(SKernel.TYPE_COMMA))
			{
				if (!parseExpression())
					return false;
				i++;
			}
			
			emit(ScriptCommand.create(ScriptCommandType.PUSH, i));
			emit(ScriptCommand.create(ScriptCommandType.PUSH_LIST_INIT));
			return true;
		}

		// Parses a function call.
		// 		( .... , .... )
		private boolean parseFunctionCall(int paramCount)
		{
			while (paramCount-- > 0)
			{
				if (!parseExpression())
					return false;
				
				if (paramCount > 0)
				{
					if (!matchType(SKernel.TYPE_COMMA))
					{
						addErrorMessage("Expected \",\" after a host function parameter.");
						return false;
					}
				}
			}
		
			return true;
		}

		// Parses a host function call.
		// 		( .... , .... )
		private boolean parseHostFunctionCall(ScriptFunctionType functionType)
		{
			int paramCount = functionType.getParameterCount();
			while (paramCount-- > 0)
			{
				if (!parseExpression())
					return false;
				
				if (paramCount > 0)
				{
					if (!matchType(SKernel.TYPE_COMMA))
					{
						addErrorMessage("Expected \",\" after a host function parameter.");
						return false;
					}
				}
			}
		
			return true;
		}

		// <ExpressionValue> :
		//		
		// If null, bad parse.
		private boolean parseSingleValue()
		{
			if (matchType(SKernel.TYPE_TRUE))
			{
				emit(ScriptCommand.create(ScriptCommandType.PUSH, true));
				return true;
			}
			else if (matchType(SKernel.TYPE_FALSE))
			{
				emit(ScriptCommand.create(ScriptCommandType.PUSH, false));
				return true;
			}
			else if (matchType(SKernel.TYPE_INFINITY))
			{
				emit(ScriptCommand.create(ScriptCommandType.PUSH, Double.POSITIVE_INFINITY));
				return true;
			}
			else if (matchType(SKernel.TYPE_NAN))
			{
				emit(ScriptCommand.create(ScriptCommandType.PUSH, Double.NaN));
				return true;
			}
			else if (currentType(SKernel.TYPE_NUMBER))
			{
				String lexeme = currentToken().getLexeme();
				nextToken();
				if (lexeme.startsWith("0X") || lexeme.startsWith("0x"))
				{
					emit(ScriptCommand.create(ScriptCommandType.PUSH, Long.parseLong(lexeme.substring(2), 16)));
					return true;
				}
				else if (lexeme.contains("."))
				{
					emit(ScriptCommand.create(ScriptCommandType.PUSH, Double.parseDouble(lexeme)));
					return true;
				}
				else
				{
					emit(ScriptCommand.create(ScriptCommandType.PUSH, Long.parseLong(lexeme)));
					return true;
				}
			}
			else if (currentType(SKernel.TYPE_STRING))
			{
				String lexeme = currentToken().getLexeme();
				nextToken();
				emit(ScriptCommand.create(ScriptCommandType.PUSH, lexeme));
				return true;
			}
			else
			{
				addErrorMessage("Expression - Expected a literal value.");
				return false;
			}
		}

		// Operator reduce.
		private boolean operatorReduce(Stack<Integer> operatorStack, int[] expressionValueCounter, int nextOperator) 
		{
			Integer top = operatorStack.peek();
			while (top != null && (getOperatorPrecedence(top) > getOperatorPrecedence(nextOperator) || (getOperatorPrecedence(top) == getOperatorPrecedence(nextOperator) && !isOperatorRightAssociative(nextOperator))))
			{
				if (!expressionReduce(operatorStack, expressionValueCounter))
					return false;
				top = operatorStack.peek();
			}
			
			return true;
		}

		// Reduces an expression by operator.
		private boolean expressionReduce(Stack<Integer> operatorStack, int[] expressionValueCounter)
		{
			if (operatorStack.isEmpty())
				throw new ScriptParseException("Internal error - operator stack must have one operator in it.");
		
			int operator = operatorStack.pop();
			
			if (isBinaryOperatorType(operator))
				expressionValueCounter[0] -= 2;
			else
				expressionValueCounter[0] -= 1;
			
			if (expressionValueCounter[0] < 0)
				throw new ScriptParseException("Internal error - value counter did not have enough counter.");
			
			expressionValueCounter[0] += 1; // the "push"
		
			return emitArithmeticCommand(operator);
			
		}

		// reduce everything currently pending.
		private boolean expressionReduceAll(Stack<Integer> operatorStack, int[] expressionValueCounter)
		{
			// end of expression - reduce.
			while (!operatorStack.isEmpty())
			{
				if (!expressionReduce(operatorStack, expressionValueCounter))
					return false;
			}
			
			return true;
		}

		// Emits an arithmetic command based on an operator token type.
		private boolean emitArithmeticCommand(int operator)
		{
			switch (operator)
			{
				case SKernel.TYPE_ABSOLUTE:
					emit(ScriptCommand.create(ScriptCommandType.ABSOLUTE));
					return true;
				case SKernel.TYPE_EXCLAMATION: 
					emit(ScriptCommand.create(ScriptCommandType.LOGICAL_NOT));
					return true;
				case SKernel.TYPE_TILDE: 
					emit(ScriptCommand.create(ScriptCommandType.NOT));
					return true;
				case SKernel.TYPE_NEGATE:
					emit(ScriptCommand.create(ScriptCommandType.NEGATE));
					return true;
				case SKernel.TYPE_PLUS:
				case SKernel.TYPE_PLUSEQUALS:
					emit(ScriptCommand.create(ScriptCommandType.ADD));
					return true;
				case SKernel.TYPE_DASH:
				case SKernel.TYPE_DASHEQUALS:
					emit(ScriptCommand.create(ScriptCommandType.SUBTRACT));
					return true;
				case SKernel.TYPE_STAR:
				case SKernel.TYPE_STAREQUALS:
					emit(ScriptCommand.create(ScriptCommandType.MULTIPLY));
					return true;
				case SKernel.TYPE_SLASH:
				case SKernel.TYPE_SLASHEQUALS:
					emit(ScriptCommand.create(ScriptCommandType.DIVIDE));
					return true;
				case SKernel.TYPE_PERCENT:
				case SKernel.TYPE_PERCENTEQUALS:
					emit(ScriptCommand.create(ScriptCommandType.MODULO));
					return true;
				case SKernel.TYPE_AMPERSAND:
				case SKernel.TYPE_AMPERSANDEQUALS:
					emit(ScriptCommand.create(ScriptCommandType.AND));
					return true;
				case SKernel.TYPE_PIPE:
				case SKernel.TYPE_PIPEEQUALS:
					emit(ScriptCommand.create(ScriptCommandType.OR));
					return true;
				case SKernel.TYPE_CARAT:
					emit(ScriptCommand.create(ScriptCommandType.XOR));
					return true;
				case SKernel.TYPE_GREATER:
					emit(ScriptCommand.create(ScriptCommandType.GREATER));
					return true;
				case SKernel.TYPE_GREATEREQUAL:
					emit(ScriptCommand.create(ScriptCommandType.GREATER_OR_EQUAL));
					return true;
				case SKernel.TYPE_DOUBLEGREATER:
				case SKernel.TYPE_DOUBLEGREATEREQUALS:
					emit(ScriptCommand.create(ScriptCommandType.RIGHT_SHIFT));
					return true;
				case SKernel.TYPE_TRIPLEGREATER:
				case SKernel.TYPE_TRIPLEGREATEREQUALS:
					emit(ScriptCommand.create(ScriptCommandType.RIGHT_SHIFT_PADDED));
					return true;
				case SKernel.TYPE_LESS:
					emit(ScriptCommand.create(ScriptCommandType.LESS));
					return true;
				case SKernel.TYPE_DOUBLELESS:
				case SKernel.TYPE_DOUBLELESSEQUALS:
					emit(ScriptCommand.create(ScriptCommandType.LEFT_SHIFT));
					return true;
				case SKernel.TYPE_LESSEQUAL:
					emit(ScriptCommand.create(ScriptCommandType.LESS_OR_EQUAL));
					return true;
				case SKernel.TYPE_DOUBLEEQUAL:
					emit(ScriptCommand.create(ScriptCommandType.EQUAL));
					return true;
				case SKernel.TYPE_TRIPLEEQUAL:
					emit(ScriptCommand.create(ScriptCommandType.STRICT_EQUAL));
					return true;
				case SKernel.TYPE_NOTEQUAL:
					emit(ScriptCommand.create(ScriptCommandType.NOT_EQUAL));
					return true;
				case SKernel.TYPE_NOTDOUBLEEQUAL:
					emit(ScriptCommand.create(ScriptCommandType.STRICT_NOT_EQUAL));
					return true;
				default:
					throw new ScriptParseException("Internal error - Bad operator pushed for expression.");
			}
		}

		// Checks for a entry statement type.
		private boolean isEntryStart(int currentType)
		{
			switch (currentType)
			{
				default:
					return false;
				case SKernel.TYPE_MAIN:
				case SKernel.TYPE_FUNCTION:
				case SKernel.TYPE_ENTRY:
				case SKernel.TYPE_PRAGMA:
					return true;
			}
		}

		// Checks for a starting statement type.
		private boolean isStatementStart(int currentType)
		{
			switch (currentType)
			{
				default:
					return false;
				case SKernel.TYPE_SEMICOLON:
				case SKernel.TYPE_BREAK:
				case SKernel.TYPE_CONTINUE:
				case SKernel.TYPE_RETURN:
				case SKernel.TYPE_IDENTIFIER:
					return true;
			}
		}

		// Checks for a control statement type.
		private boolean isControlStatementStart(int currentType)
		{
			switch (currentType)
			{
				default:
					return false;
				case SKernel.TYPE_IF:
				case SKernel.TYPE_WHILE:
				case SKernel.TYPE_FOR:
					return true;
			}
		}

		// Return true if token type can be a unary operator.
		private boolean isValidLiteralType(int tokenType)
		{
			switch (tokenType)
			{
				case SKernel.TYPE_NUMBER:
				case SKernel.TYPE_TRUE:
				case SKernel.TYPE_FALSE:
				case SKernel.TYPE_INFINITY:
				case SKernel.TYPE_NAN:
				case SKernel.TYPE_STRING:
					return true;
				default:
					return false;
			}
		}

		// Return true if token type can be a unary operator.
		private boolean isUnaryOperatorType(int tokenType)
		{
			switch (tokenType)
			{
				case SKernel.TYPE_DASH:
				case SKernel.TYPE_PLUS:
				case SKernel.TYPE_EXCLAMATION:
				case SKernel.TYPE_TILDE:
					return true;
				default:
					return false;
			}
		}

		// Return true if token type can be a binary operator.
		private boolean isBinaryOperatorType(int tokenType)
		{
			switch (tokenType)
			{
				case SKernel.TYPE_PLUS:
				case SKernel.TYPE_DASH:
				case SKernel.TYPE_STAR:
				case SKernel.TYPE_SLASH:
				case SKernel.TYPE_PERCENT:
				case SKernel.TYPE_AMPERSAND:
				case SKernel.TYPE_PIPE:
				case SKernel.TYPE_CARAT:
				case SKernel.TYPE_GREATER:
				case SKernel.TYPE_GREATEREQUAL:
				case SKernel.TYPE_DOUBLEGREATER:
				case SKernel.TYPE_TRIPLEGREATER:
				case SKernel.TYPE_LESS:
				case SKernel.TYPE_DOUBLELESS:
				case SKernel.TYPE_LESSEQUAL:
				case SKernel.TYPE_DOUBLEEQUAL:
				case SKernel.TYPE_TRIPLEEQUAL:
				case SKernel.TYPE_NOTEQUAL:
				case SKernel.TYPE_NOTDOUBLEEQUAL:
					return true;
				default:
					return false;
			}
		}

		// Return operator precedence (higher is better).
		private int getOperatorPrecedence(int tokenType)
		{
			switch (tokenType)
			{
				case SKernel.TYPE_ABSOLUTE: 
				case SKernel.TYPE_EXCLAMATION: 
				case SKernel.TYPE_TILDE: 
				case SKernel.TYPE_NEGATE:
					return 20;
				case SKernel.TYPE_STAR:
				case SKernel.TYPE_SLASH:
				case SKernel.TYPE_PERCENT:
					return 18;
				case SKernel.TYPE_PLUS:
				case SKernel.TYPE_DASH:
					return 16;
				case SKernel.TYPE_DOUBLEGREATER:
				case SKernel.TYPE_TRIPLEGREATER:
				case SKernel.TYPE_DOUBLELESS:
					return 14;
				case SKernel.TYPE_GREATER:
				case SKernel.TYPE_GREATEREQUAL:
				case SKernel.TYPE_LESS:
				case SKernel.TYPE_LESSEQUAL:
					return 12;
				case SKernel.TYPE_DOUBLEEQUAL:
				case SKernel.TYPE_TRIPLEEQUAL:
				case SKernel.TYPE_NOTEQUAL:
				case SKernel.TYPE_NOTDOUBLEEQUAL:
					return 10;
				case SKernel.TYPE_AMPERSAND:
					return 8;
				case SKernel.TYPE_CARAT:
					return 6;
				case SKernel.TYPE_PIPE:
					return 4;
				default:
					return 0;
			}
		}

		// Return true if token type is an operator that is right-associative.
		private boolean isOperatorRightAssociative(int tokenType)
		{
			switch (tokenType)
			{
				case SKernel.TYPE_ABSOLUTE: 
				case SKernel.TYPE_EXCLAMATION: 
				case SKernel.TYPE_TILDE: 
				case SKernel.TYPE_NEGATE: 
				case SKernel.TYPE_DOUBLEPIPE:
					return true;
				default:
					return false;
			}
		
		}
		
		// Return true if the type is an assignment operator.
		private boolean isAssignmentOperator(int tokenType)
		{
			switch (tokenType)
			{
				case SKernel.TYPE_EQUAL: 
				case SKernel.TYPE_PLUSEQUALS:
				case SKernel.TYPE_DASHEQUALS:
				case SKernel.TYPE_STAREQUALS:
				case SKernel.TYPE_SLASHEQUALS:
				case SKernel.TYPE_PERCENTEQUALS:
				case SKernel.TYPE_AMPERSANDEQUALS:
				case SKernel.TYPE_PIPEEQUALS:
				case SKernel.TYPE_DOUBLEGREATEREQUALS:
				case SKernel.TYPE_TRIPLEGREATEREQUALS:
				case SKernel.TYPE_DOUBLELESSEQUALS:
					return true;
				default:
					return false;
			}
		}
		
		// Return true if the type is an accumulating operator (eg: += -= *= etc.).
		private boolean isAccumulatingAssignmentOperator(int tokenType)
		{
			switch (tokenType)
			{
				case SKernel.TYPE_PLUSEQUALS:
				case SKernel.TYPE_DASHEQUALS:
				case SKernel.TYPE_STAREQUALS:
				case SKernel.TYPE_SLASHEQUALS:
				case SKernel.TYPE_PERCENTEQUALS:
				case SKernel.TYPE_AMPERSANDEQUALS:
				case SKernel.TYPE_PIPEEQUALS:
				case SKernel.TYPE_DOUBLEGREATEREQUALS:
				case SKernel.TYPE_TRIPLEGREATEREQUALS:
				case SKernel.TYPE_DOUBLELESSEQUALS:
					return true;
				default:
					return false;
			}
		}
		
	}
	
	/**
	 * Reads a script from a String of text.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(String text, ScriptFunctionResolver resolver) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), resolver, DEFAULT_OPTIONS, DEFAULT_INCLUDER);
	}

	/**
	 * Reads a script from a String of text.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if text is null. 
	 */
	public static Script read(String text, ScriptFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), resolver, DEFAULT_OPTIONS, includer);
	}

	/**
	 * Reads a script from a String of text.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(String text, ScriptFunctionResolver resolver, ScriptReaderOptions options) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), resolver, options, DEFAULT_INCLUDER);
	}

	/**
	 * Reads a script from a String of text.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(String text, ScriptFunctionResolver resolver, ScriptReaderOptions options, ScriptReaderIncluder includer) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), resolver, includer);
	}

	/**
	 * Reads a script from a String of text.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(String streamName, String text, ScriptFunctionResolver resolver) throws IOException
	{
		return read(streamName, new StringReader(text), resolver, DEFAULT_OPTIONS, DEFAULT_INCLUDER);
	}

	/**
	 * Reads a script from a String of text.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if text is null. 
	 */
	public static Script read(String streamName, String text, ScriptFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		return read(streamName, new StringReader(text), resolver, DEFAULT_OPTIONS, includer);
	}

	/**
	 * Reads a script from a String of text.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(String streamName, String text, ScriptFunctionResolver resolver, ScriptReaderOptions options) throws IOException
	{
		return read(streamName, new StringReader(text), resolver, options, DEFAULT_INCLUDER);
	}

	/**
	 * Reads a script from a String of text.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(String streamName, String text, ScriptFunctionResolver resolver, ScriptReaderOptions options, ScriptReaderIncluder includer) throws IOException
	{
		return read(streamName, new StringReader(text), resolver, includer);
	}

	/**
	 * Reads a script from a starting text file.
	 * @param file the file to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(File file, ScriptFunctionResolver resolver) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return read(file.getPath(), fis, resolver, DEFAULT_OPTIONS, DEFAULT_INCLUDER);
		} finally {
			Common.close(fis);
		}
	}

	/**
	 * Reads a script from a starting text file.
	 * @param file	the file to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(File file, ScriptFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return read(file.getPath(), fis, resolver, includer);
		} finally {
			Common.close(fis);
		}
	}

	/**
	 * Reads a script from a starting text file.
	 * @param file	the file to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(File file, ScriptFunctionResolver resolver, ScriptReaderOptions options) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return read(file.getPath(), fis, resolver, options, DEFAULT_INCLUDER);
		} finally {
			Common.close(fis);
		}
	}

	/**
	 * Reads a script from a starting text file.
	 * @param file	the file to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(File file, ScriptFunctionResolver resolver, ScriptReaderOptions options, ScriptReaderIncluder includer) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return read(file.getPath(), fis, resolver, options, includer);
		} finally {
			Common.close(fis);
		}
	}

	/**
	 * Reads a script.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static Script read(String streamName, InputStream in, ScriptFunctionResolver resolver) throws IOException
	{
		return read(streamName, new InputStreamReader(in), resolver, DEFAULT_OPTIONS, DEFAULT_INCLUDER);
	}

	/**
	 * Reads a script.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static Script read(String streamName, InputStream in, ScriptFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		return read(streamName, new InputStreamReader(in), resolver, DEFAULT_OPTIONS, includer);
	}

	/**
	 * Reads a script.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static Script read(String streamName, InputStream in, ScriptFunctionResolver resolver, ScriptReaderOptions options) throws IOException
	{
		return read(streamName, new InputStreamReader(in), resolver, options, DEFAULT_INCLUDER);
	}

	/**
	 * Reads a script.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static Script read(String streamName, InputStream in, ScriptFunctionResolver resolver, ScriptReaderOptions options, ScriptReaderIncluder includer) throws IOException
	{
		return read(streamName, new InputStreamReader(in), resolver, options, includer);
	}

	/**
	 * Reads a script from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param resolver the host function resolver to use.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if f is null. 
	 */
	public static Script read(String streamName, Reader reader, ScriptFunctionResolver resolver) throws IOException
	{
		return read(streamName, reader, resolver, DEFAULT_OPTIONS, DEFAULT_INCLUDER);
	}

	/**
	 * Reads a script from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param resolver the host function resolver to use.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
	 */
	public static Script read(String streamName, Reader reader, ScriptFunctionResolver resolver, ScriptReaderIncluder includer) throws IOException
	{
		return read(streamName, reader, resolver, DEFAULT_OPTIONS, includer);
	}

	/**
	 * Reads a script from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if reader is null. 
	 */
	public static Script read(String streamName, Reader reader, ScriptFunctionResolver resolver, ScriptReaderOptions options) throws IOException
	{
		return read(streamName, reader, resolver, options, DEFAULT_INCLUDER);
	}

	/**
	 * Reads a script from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param resolver the host function resolver to use.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new Script that contains all the read object hierarchy.
	 * @throws ScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if reader is null. 
	 */
	public static Script read(String streamName, Reader reader, ScriptFunctionResolver resolver, ScriptReaderOptions options, ScriptReaderIncluder includer) throws IOException
	{
		return (new SParser(new SLexer(streamName, reader, includer), options, resolver)).readScript();
	}

}

