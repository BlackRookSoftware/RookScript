/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.compiler;

import java.util.Deque;
import java.util.LinkedList;

import com.blackrook.rookscript.Script;
import com.blackrook.rookscript.Script.Entry;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.exception.ScriptParseException;
import com.blackrook.rookscript.lang.ScriptCommand;
import com.blackrook.rookscript.lang.ScriptCommandType;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.struct.Lexer;

/**
 * The parser that parses text for the script reader. 
 * @author Matthew Tropiano
 * TODO: Parse namespaced host function calls.
 */
public class ScriptParser extends Lexer.Parser
{
	/** Label prefix. */
	public static final String LABEL_IF_CONDITIONAL = "_if_cond_";
	/** Label prefix. */
	public static final String LABEL_IF_SUCCESS = "_if_true_";
	/** Label prefix. */
	public static final String LABEL_IF_FAILURE = "_if_false_";
	/** Label prefix. */
	public static final String LABEL_IF_END = "_if_end_";
	/** Label prefix. */
	public static final String LABEL_WHILE_CONDITIONAL = "_while_cond_";
	/** Label prefix. */
	public static final String LABEL_WHILE_SUCCESS = "_while_true_";
	/** Label prefix. */
	public static final String LABEL_WHILE_END = "_while_end_";
	/** Label prefix. */
	public static final String LABEL_FOR_INIT = "_for_init_";
	/** Label prefix. */
	public static final String LABEL_FOR_CONDITIONAL = "_for_cond_";
	/** Label prefix. */
	public static final String LABEL_FOR_STEP = "_for_step_";
	/** Label prefix. */
	public static final String LABEL_FOR_SUCCESS = "_for_true_";
	/** Label prefix. */
	public static final String LABEL_FOR_END = "_for_end_";
	/** Label prefix. */
	public static final String LABEL_TERNARY_TRUE = "_tern_true_";
	/** Label prefix. */
	public static final String LABEL_TERNARY_FALSE = "_tern_false_";
	/** Label prefix. */
	public static final String LABEL_TERNARY_END = "_tern_end_";
	/** Label prefix. */
	public static final String LABEL_SSAND_TRUE = "_ssand_true_";
	/** Label prefix. */
	public static final String LABEL_SSAND_FALSE = "_ssand_false_";
	/** Label prefix. */
	public static final String LABEL_SSAND_END = "_ssand_end_";
	/** Label prefix. */
	public static final String LABEL_SSOR_TRUE = "_ssor_true_";
	/** Label prefix. */
	public static final String LABEL_SSOR_FALSE = "_ssor_false_";
	/** Label prefix. */
	public static final String LABEL_SSOR_END = "_ssor_end_";
	/** Label prefix. */
	public static final String LABEL_COALESCE_START = "_coalesce_start_";
	/** Label prefix. */
	public static final String LABEL_COALESCE_END = "_coalesce_end_";
	/** Label prefix. */
	public static final String LABEL_SCRIPTLET_START = "_scriptlet_start_";
	/** Label prefix. */
	public static final String LABEL_SCRIPTLET_END = "_scriptlet_end_";

	/** Return false. */
	public static final int PARSEFUNCTION_FALSE = 0;
	/** Return true. */
	public static final int PARSEFUNCTION_TRUE = 1;

	/** Return false. */
	public static final int PARSEFUNCTIONCALL_FALSE = -1;

	/** List of errors. */
	private LinkedList<String> errors;
	
	/**
	 * Creates a new script parser.
	 * @param lexer the lexer to fetch tokens from.
	 */
	public ScriptParser(ScriptLexer lexer)
	{
		super(lexer);
		this.errors = new LinkedList<>();
	}
	
	private void addErrorMessage(String message)
	{
		errors.add(getTokenInfoLine(message));
	}
	
	private String[] getErrorMessages()
	{
		String[] out = new String[errors.size()];
		errors.toArray(out);
		return out;
	}
	
	/**
	 * Starts parsing a script.
	 * @param script the script to start adding compiled code to.
	 */
	public void readScript(Script script)
	{
		// prime first token.
		nextToken();
		
		// keep parsing entries.
		boolean noError = true;
		
		try {
			while (currentToken() != null && (noError = parseEntryList(script))) ;
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
	}
	
	/* 
	 	<FunctionEntry> := 
	 		<IDENTIFIER> "(" <FunctionArgumentList> ")" "{" <StatementList> "}"
	 */
	protected boolean parseFunctionEntry(Script currentScript)
	{
		if (!currentType(ScriptKernel.TYPE_IDENTIFIER))
		{
			addErrorMessage("Expected an identifier for the function name.");
			return false;
		}
	
		String name = currentToken().getLexeme();
		nextToken();
		
		if (currentScript.getFunctionEntry(name) != null)
		{
			addErrorMessage("The function entry \"" + name + "\" was already defined.");
			return false;
		}
		
		int index = currentScript.getCommandCount();
		mark(currentScript, getFunctionLabel(name));
		
		if (!matchType(ScriptKernel.TYPE_LPAREN))
		{
			addErrorMessage("Expected \"(\" after function name.");
			return false;
		}
	
		int paramAmount = 0;
		if (currentType(ScriptKernel.TYPE_IDENTIFIER))
		{
			Deque<String> paramNameStack = new LinkedList<>();
			paramNameStack.push(currentToken().getLexeme());
			nextToken();
			
			while (matchType(ScriptKernel.TYPE_COMMA))
			{
				if (!currentType(ScriptKernel.TYPE_IDENTIFIER))
				{
					addErrorMessage("Expected identifier after \",\" for parameter name.");
					return false;
				}
				
				paramNameStack.push(currentToken().getLexeme());
				nextToken();
			}
			
			paramAmount = paramNameStack.size();
			
			while (!paramNameStack.isEmpty())
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.POP_VARIABLE, paramNameStack.pollFirst()));
			
		}
		
		currentScript.createFunctionEntry(name, paramAmount, index);
		
		if (!matchType(ScriptKernel.TYPE_RPAREN))
		{
			addErrorMessage("Expected \")\".");
			return false;
		}
	
		// start statement list?
		if (!matchType(ScriptKernel.TYPE_LBRACE))
		{
			addErrorMessage("Expected \"{\" to start \"function\" body.");
			return false;
		}
		
		if (!parseStatementList(currentScript, null, null))
			return false;
		
		if (!matchType(ScriptKernel.TYPE_RBRACE))
		{
			addErrorMessage("Expected statement or \"}\" to close \"function\" body.");
			return false;
		}
		
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_NULL));
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.RETURN));
		return true;
	}

	/*
	 *  <Function> := "(" <Expression> ... ")"
	 */
	protected int parseFunctionCall(Script currentScript, String functionNamespace, String functionName, boolean partial)
	{
		if (!matchType(ScriptKernel.TYPE_LPAREN))
		{
			addErrorMessage("INTERNAL ERROR: Expected \"(\" - not verified first!");
			return PARSEFUNCTION_FALSE;
		}

		// test type of call: host function first, then local script function.
		ScriptFunctionType functionType;
		Entry functionEntry;
		if ((functionType = currentScript.getHostFunctionResolver().getNamespacedFunction(functionNamespace, functionName)) != null)
		{
			int parsedCount;
			if ((parsedCount = parseHostFunctionCall(currentScript, functionType, partial)) == PARSEFUNCTIONCALL_FALSE)
				return PARSEFUNCTION_FALSE;
							
			if (!matchType(ScriptKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\" after a host function call's parameters.");
				return PARSEFUNCTION_FALSE;
			}
			
			// fill last arguments left with null.
			while (functionType.getParameterCount() - (parsedCount++) > 0)
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_NULL));
			
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.CALL_HOST, functionName));
			return PARSEFUNCTION_TRUE;
		}
		else if ((functionEntry = currentScript.getFunctionEntry(functionName)) != null)
		{
			int paramCount = functionEntry.getParameterCount();
			int parsedCount;
			if ((parsedCount = parseLocalFunctionCall(currentScript, paramCount - (partial ? 1 : 0))) == PARSEFUNCTIONCALL_FALSE)
				return PARSEFUNCTION_FALSE;
			if (partial)
				parsedCount++;
							
			if (!matchType(ScriptKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\" after a function call's parameters.");
				return PARSEFUNCTION_FALSE;
			}
			
			// fill last arguments left with null.
			while (paramCount - (parsedCount++) > 0)
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_NULL));
			
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.CALL, getFunctionLabel(functionName)));
	
			return PARSEFUNCTION_TRUE;
		}
		else
		{
			addErrorMessage("\"" + functionName + "\" is not the name of a valid function call - not host or local.");
			return PARSEFUNCTION_FALSE;
		}
	}

	/** 
	 * Marks a label on the current command. 
	 * @param currentScript the current script.
	 * @param label the label to set for the current command index.
	 * @return the command index marked.
	 */
	protected int mark(Script currentScript, String label)
	{
		int out;
		currentScript.setIndex(label, out = currentScript.getCommandCount());
		return out;
	}

	/**
	 * Parses a scriptlet to the main script.
	 * <pre>
	 * [Scriptlet] :=
	 * 		[IDENTIFIER] [FunctionCall] 
	 * 		"{" [StatementList] "}"
	 * </pre>
	 * @param script the script.
	 * @return true if parse is good, false if not.
	 */
	protected boolean parseScriptlet(Script script)
	{
		// start statement list?
		if (matchType(ScriptKernel.TYPE_LBRACE))
		{
			String startLabel = script.getNextGeneratedLabel(LABEL_SCRIPTLET_START);
			String endLabel = script.getNextGeneratedLabel(LABEL_SCRIPTLET_END);
			
			mark(script, startLabel);
			
			if (!parseStatementList(script, null, null))
				return false;
			
			mark(script, endLabel);

			if (!matchType(ScriptKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected \"}\" to close scriptlet body.");
				return false;
			}
		
			return true;
		}
		else if (currentType(ScriptKernel.TYPE_IDENTIFIER))
		{
			String name = currentToken().getLexeme();
			nextToken();
			
			if (!currentType(ScriptKernel.TYPE_LPAREN))
			{
				addErrorMessage("Expected \"(\" after a function name.");
				return false;
			}

			int ret;
			if ((ret = parseFunctionCall(script, null, name, false)) == PARSEFUNCTION_FALSE)
				return false;
			
			if (ret == PARSEFUNCTION_TRUE)
				script.addCommand(ScriptCommand.create(ScriptCommandType.POP));
			
			return true;
		}
		else
		{
			addErrorMessage("Expected \"{\" to start scriptlet body or function name.");
			return false;
		}
	}

	// Parse a single value to add to an object.
	// If null, bad parse.
	protected <T> T parseValueFor(Class<T> clazz)
	{
		if (matchType(ScriptKernel.TYPE_NULL))
		{
			return null;
		}
		else if (matchType(ScriptKernel.TYPE_TRUE))
		{
			return ScriptValue.create(true).createForType(clazz);
		}
		else if (matchType(ScriptKernel.TYPE_FALSE))
		{
			return ScriptValue.create(false).createForType(clazz);
		}
		else if (matchType(ScriptKernel.TYPE_DASH))
		{
			if (matchType(ScriptKernel.TYPE_INFINITY))
			{
				return ScriptValue.create(Double.NEGATIVE_INFINITY).createForType(clazz);
			}
			else if (matchType(ScriptKernel.TYPE_NAN))
			{
				return ScriptValue.create(Double.NaN).createForType(clazz);
			}
			else if (currentType(ScriptKernel.TYPE_NUMBER))
			{
				String lexeme = currentToken().getLexeme();
				nextToken();
				if (lexeme.startsWith("0X") || lexeme.startsWith("0x"))
				{
					return ScriptValue.create(-Long.parseLong(lexeme.substring(2), 16)).createForType(clazz);
				}
				else if (lexeme.contains("."))
				{
					return ScriptValue.create(-Double.parseDouble(lexeme)).createForType(clazz);
				}
				else
				{
					return ScriptValue.create(-Long.parseLong(lexeme)).createForType(clazz);
				}
			}
			else
			{
				addErrorMessage("Expression - Expected a single value.");
				return null;
			}
		}
		else if (matchType(ScriptKernel.TYPE_INFINITY))
		{
			return ScriptValue.create(Double.POSITIVE_INFINITY).createForType(clazz);
		}
		else if (matchType(ScriptKernel.TYPE_NAN))
		{
			return ScriptValue.create(Double.NaN).createForType(clazz);
		}
		else if (currentType(ScriptKernel.TYPE_NUMBER))
		{
			String lexeme = currentToken().getLexeme();
			nextToken();
			if (lexeme.startsWith("0X") || lexeme.startsWith("0x"))
			{
				return ScriptValue.create(Long.parseLong(lexeme.substring(2), 16)).createForType(clazz);
			}
			else if (lexeme.contains("."))
			{
				return ScriptValue.create(Double.parseDouble(lexeme)).createForType(clazz);
			}
			else
			{
				return ScriptValue.create(Long.parseLong(lexeme)).createForType(clazz);
			}
		}
		else if (currentType(ScriptKernel.TYPE_STRING))
		{
			String lexeme = currentToken().getLexeme();
			nextToken();
			return ScriptValue.create(lexeme).createForType(clazz);
		}
		else
		{
			addErrorMessage("Expression - Expected a single value.");
			return null;
		}
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

	// Parse a bunch of entries.
	private boolean parseEntryList(Script currentScript)
	{
		while (currentToken() != null && isEntryStart(currentToken().getType()))
		{
			if (!parseEntry(currentScript))
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
	private boolean parseEntry(Script currentScript)
	{
		if (matchType(ScriptKernel.TYPE_FUNCTION))
		{
			return parseFunctionEntry(currentScript);
		}
		// entry.
		else if (matchType(ScriptKernel.TYPE_ENTRY))
		{
			return parseNamedEntry(currentScript);
		}
		// pragma entry.
		else if (matchType(ScriptKernel.TYPE_PRAGMA))
		{
			return parsePragmaEntry(currentScript);
		}
		else
		{
			addErrorMessage("Expected an \"main\", \"function\", \"entry\", or \"pragma\" entry.");
			return false;
		}
	}

	/*
		<StatementBody> :=
			"{" <StatementList> "}"
			<StatementClause> ";"
	 */
	private boolean parseStatementBody(Script currentScript, String breakLabel, String continueLabel)
	{
		// start statement list?
		if (matchType(ScriptKernel.TYPE_LBRACE))
		{
			if (!parseStatementList(currentScript, breakLabel, continueLabel))
				return false;
			
			if (!matchType(ScriptKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected \"}\" to terminate statement list.");
				return false;
			}
	
			return true;
		}
		// standard statement?
		else if (isStatementStart(currentToken().getType()))
		{
			if (!parseStatementClause(currentScript, breakLabel, continueLabel))
				return false;
			
			if (!matchType(ScriptKernel.TYPE_SEMICOLON))
			{
				addErrorMessage("Expected \";\" to terminate statement.");
				return false;
			}
	
			return true;
		}
		// control statement?
		else if (isControlStatementStart(currentToken().getType()))
		{
			return parseStatementClause(currentScript, breakLabel, continueLabel);
		}
		else
		{
			addErrorMessage("Expected statement.");
			return false;
		}
		
	}

	/* 
		<ScriptEntry> := 
			<ScriptName> "(" <ScriptEntryArgumentList> ")" "{" <StatementList> "}"
	 */
	private boolean parseNamedEntry(Script currentScript)
	{
		if (!currentType(ScriptKernel.TYPE_IDENTIFIER, ScriptKernel.TYPE_NUMBER, ScriptKernel.TYPE_STRING))
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
		
		int index = mark(currentScript, label);
		
		if (!matchType(ScriptKernel.TYPE_LPAREN))
		{
			addErrorMessage("Expected \"(\" after function name.");
			return false;
		}
	
		int paramAmount = 0;
		if (currentType(ScriptKernel.TYPE_IDENTIFIER))
		{
			Deque<String> paramNameStack = new LinkedList<>();
			paramNameStack.push(currentToken().getLexeme());
			nextToken();
			
			while (matchType(ScriptKernel.TYPE_COMMA))
			{
				if (!currentType(ScriptKernel.TYPE_IDENTIFIER))
				{
					addErrorMessage("Expected identifier after \",\" for parameter name.");
					return false;
				}
				
				paramNameStack.push(currentToken().getLexeme());
				nextToken();
			}
			
			paramAmount = paramNameStack.size();
			
			while (!paramNameStack.isEmpty())
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.POP_VARIABLE, paramNameStack.pop()));
			
		}
		
		currentScript.setScriptEntry(name, paramAmount, index);
		
		if (!matchType(ScriptKernel.TYPE_RPAREN))
		{
			addErrorMessage("Expected \")\".");
			return false;
		}
	
		// start statement list?
		if (!matchType(ScriptKernel.TYPE_LBRACE))
		{
			addErrorMessage("Expected \"{\" to start \"entry\" body.");
			return false;
		}
		
		if (!parseStatementList(currentScript, null, null))
			return false;
		
		if (!matchType(ScriptKernel.TYPE_RBRACE))
		{
			addErrorMessage("Expected \"}\" to close \"entry\" body.");
			return false;
		}
	
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_NULL));
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.RETURN));
		return true;
	}

	/* 
		<PragmaEntry> := ...
	 */
	private boolean parsePragmaEntry(Script currentScript)
	{
		if (!matchType(ScriptKernel.TYPE_SEMICOLON))
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
	private boolean parseStatementList(Script currentScript, String breakLabel, String continueLabel)
	{
		while (isStatementStart(currentToken().getType()) || isControlStatementStart(currentToken().getType()))
		{
			// standard statement?
			if (isStatementStart(currentToken().getType()))
			{
				if (!parseStatementClause(currentScript, breakLabel, continueLabel))
					return false;
				
				if (!matchType(ScriptKernel.TYPE_SEMICOLON))
				{
					addErrorMessage("Expected \";\" to terminate statement.");
					return false;
				}
			}
			// control statement?
			else if (isControlStatementStart(currentToken().getType()))
			{
				if (!parseStatementClause(currentScript, breakLabel, continueLabel))
					return false;
			}
		}
		
		return true;
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
	private boolean parseStatementClause(Script currentScript, String breakLabel, String continueLabel)
	{
		// empty statement
		if (currentType(ScriptKernel.TYPE_SEMICOLON))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.NOOP));
			return true;
		}
		// break clause.
		else if (matchType(ScriptKernel.TYPE_BREAK))
		{
			if (breakLabel == null)
			{
				addErrorMessage("\"Break\" used outside of a loop.");
				return false;
			}
			
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, breakLabel));
			return true;
		}
		// continue clause.
		else if (matchType(ScriptKernel.TYPE_CONTINUE))
		{
			if (continueLabel == null)
			{
				addErrorMessage("\"Continue\" used outside of a loop.");
				return false;
			}
			
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, continueLabel));
			return true;
		}
		// return clause.
		else if (matchType(ScriptKernel.TYPE_RETURN))
		{
			// if no return, return null.
			if (currentType(ScriptKernel.TYPE_SEMICOLON))
			{
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_NULL));
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.RETURN));
				return true;
			}
			
			if (!parseExpression(currentScript))
				return false;
			
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.RETURN));
			return true;
		}
		// if control clause.
		else if (matchType(ScriptKernel.TYPE_IF))
		{
			return parseIfClause(currentScript, breakLabel, continueLabel);
		}
		// while control clause.
		else if (matchType(ScriptKernel.TYPE_WHILE))
		{
			return parseWhileClause(currentScript);
		}
		// for control clause.
		else if (matchType(ScriptKernel.TYPE_FOR))
		{
			return parseForClause(currentScript);
		}
		// assignment statement or function call.
		else if (currentType(ScriptKernel.TYPE_IDENTIFIER))
		{
			String lexeme = currentToken().getLexeme();
			nextToken();
			
			if (!parseIdentifierStatement(currentScript, lexeme))
				return false;
			
			return true;
		}
		else if (matchType(ScriptKernel.TYPE_LPAREN))
		{
			if (!parseExpression(currentScript))
				return false;
			
			if (!matchType(ScriptKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected ending \")\".");
				return false;
			}
			
			if (!matchType(ScriptKernel.TYPE_RIGHTARROW))
			{
				addErrorMessage("Expected a valid statement - loose expressions need to be followed up with a partial application operator (\"->\") to become a statement.");
				return false;
			}
			
			if (!parsePartialChain(currentScript))
				return false;

			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.POP));
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
			"." <IDENTIFIER> <ASSIGNMENTOPERATOR> <Expression> ";"				(Map variable assignment)
			"::" <IDENTIFIER> 
				"(" <ParameterList> ")" ";"										(Namespaced host function call)
				<ASSIGNMENTOPERATOR> <Expression> ";"							(Scope variable assignment)
			<ASSIGNMENTOPERATOR> <Expression> ";"								(variable assignment)
			-> <PartialChain> ";"												(partial application chain)
	 */
	private boolean parseIdentifierStatement(Script currentScript, String identifierName)
	{
		// function call.
		if (currentType(ScriptKernel.TYPE_LPAREN))
		{
			int funcret;
			if ((funcret = parseFunctionCall(currentScript, null, identifierName, false)) == PARSEFUNCTION_FALSE)
				return false;

			if (funcret == PARSEFUNCTION_TRUE)
			{
				if (matchType(ScriptKernel.TYPE_RIGHTARROW))
				{
					if (!parsePartialChain(currentScript))
						return false;
				}

				// statements should not have a stack value linger.
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.POP));
			}

			return true;
		}
		// scope variable
		else if (matchType(ScriptKernel.TYPE_DOUBLECOLON))
		{
			if (!currentType(ScriptKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected identifier after scope dereference operator.");
				return false;
			}
			
			String scopeVar = currentToken().getLexeme();
			nextToken();
			
			// if deref list or map...
			if (currentType(ScriptKernel.TYPE_LBRACK, ScriptKernel.TYPE_PERIOD))
			{
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_SCOPE_VARIABLE, identifierName, scopeVar));

				return parseListMapDerefStatementChain(currentScript);
			}
			
			// else just push scope var
			int assignmentType = currentToken().getType();
			if (!isAssignmentOperator(assignmentType))
			{
				addErrorMessage("Expected assignment operator after a scope dereference.");
				return false;
			}
			nextToken();
			
			if (isAccumulatingAssignmentOperator(assignmentType))
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_SCOPE_VARIABLE, identifierName, scopeVar));
			
			if (!parseExpression(currentScript))
				return false;
			
			if (isAccumulatingAssignmentOperator(assignmentType))
				emitArithmeticCommand(currentScript, assignmentType);

			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.POP_SCOPE_VARIABLE, identifierName, scopeVar));
			return true;
		}
		// list index assignment or map assignment.
		else if (currentType(ScriptKernel.TYPE_LBRACK, ScriptKernel.TYPE_PERIOD))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_VARIABLE, identifierName));
			return parseListMapDerefStatementChain(currentScript);
		}
		// is assignment?
		else if (isAssignmentOperator(currentToken().getType()))
		{
			int assignmentType = currentToken().getType();
			nextToken();
			
			if (isAccumulatingAssignmentOperator(assignmentType))
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_VARIABLE, identifierName));
			
			if (!parseExpression(currentScript))
				return false;
			
			if (isAccumulatingAssignmentOperator(assignmentType))
				emitArithmeticCommand(currentScript, assignmentType);
			
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.POP_VARIABLE, identifierName));
			return true;
		}
		else if (matchType(ScriptKernel.TYPE_RIGHTARROW))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_VARIABLE, identifierName));
			
			if (!parsePartialChain(currentScript))
				return false;
				
			// statements should not have a stack value linger.
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.POP));
			return true;
		}
		else
		{
			addErrorMessage("Expected a valid statement.");
			return false;
		}
	}

	private boolean parseListMapDerefStatementChain(Script currentScript)
	{
		boolean lastWasList = false;
		while (currentType(ScriptKernel.TYPE_LBRACK, ScriptKernel.TYPE_PERIOD))
		{
			if (currentType(ScriptKernel.TYPE_LBRACK))
			{
				nextToken();
				if (!parseExpression(currentScript))
					return false;

				if (!matchType(ScriptKernel.TYPE_RBRACK))
				{
					addErrorMessage("Expected \"]\" after a list index expression.");
					return false;
				}

				// another dimension or deref incoming?
				if (currentType(ScriptKernel.TYPE_LBRACK, ScriptKernel.TYPE_PERIOD))
					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_LIST_INDEX));
				
				lastWasList = true;
			}
			else if (currentType(ScriptKernel.TYPE_PERIOD))
			{
				nextToken();
				if (!currentType(ScriptKernel.TYPE_IDENTIFIER))
				{
					addErrorMessage("Expected map key identifier.");
					return false;
				}
				
				String key = currentToken().getLexeme();
				nextToken();

				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, key));

				// another dimension or deref incoming?
				if (currentType(ScriptKernel.TYPE_LBRACK, ScriptKernel.TYPE_PERIOD))
					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_MAP_KEY));

				lastWasList = false;
			}
			else
			{
				addErrorMessage("INTERNAL ERROR - EXPECTED [ or .");
				return false;
			}
			
		}
		
		int assignmentType = currentToken().getType();
		if (!isAssignmentOperator(assignmentType))
		{
			addErrorMessage("Expected assignment operator after a " + (lastWasList ? "list reference." : "map dereference."));
			return false;
		}
		nextToken();
		
		if (isAccumulatingAssignmentOperator(assignmentType))
			currentScript.addCommand(ScriptCommand.create(
					lastWasList ? ScriptCommandType.PUSH_LIST_INDEX_CONTENTS : ScriptCommandType.PUSH_MAP_KEY_CONTENTS
			));
		
		if (!parseExpression(currentScript))
			return false;
		
		if (isAccumulatingAssignmentOperator(assignmentType))
			emitArithmeticCommand(currentScript, assignmentType);

		currentScript.addCommand(ScriptCommand.create(lastWasList ? ScriptCommandType.POP_LIST : ScriptCommandType.POP_MAP));
		return true;
	}
	
	// 	<IF> "(" <Expression> ")" <StatementBody> <ElseClause>
	private boolean parseIfClause(Script currentScript, String breakLabel, String continueLabel)
	{
		if (!matchType(ScriptKernel.TYPE_LPAREN))
		{
			addErrorMessage("Expected \"(\" to start \"if\" conditional.");
			return false;
		}
	
		String condLabel = currentScript.getNextGeneratedLabel(LABEL_IF_CONDITIONAL); 
		String successLabel = currentScript.getNextGeneratedLabel(LABEL_IF_SUCCESS); 
		String failLabel = currentScript.getNextGeneratedLabel(LABEL_IF_FAILURE); 
		String endLabel = currentScript.getNextGeneratedLabel(LABEL_IF_END); 
		
		mark(currentScript, condLabel);
		if (!parseExpression(currentScript))
			return false;
	
		if (!matchType(ScriptKernel.TYPE_RPAREN))
		{
			addErrorMessage("Expected \")\" to end \"if\" conditional.");
			return false;
		}
	
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP_FALSE, failLabel));
	
		mark(currentScript, successLabel);
		if (!parseStatementBody(currentScript, breakLabel, continueLabel))
			return false;
	
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, endLabel));
	
		// look for else block.
		mark(currentScript, failLabel);
		if (currentType(ScriptKernel.TYPE_ELSE))
		{
			nextToken();
			if (!parseStatementBody(currentScript, breakLabel, continueLabel))
				return false;
	
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, endLabel));
		}
		
		mark(currentScript, endLabel);
		return true;
	}

	// <WHILE> "(" <Expression> ")" <StatementBody>
	private boolean parseWhileClause(Script currentScript)
	{
		if (!matchType(ScriptKernel.TYPE_LPAREN))
		{
			addErrorMessage("Expected \"(\" to start \"while\" conditional.");
			return false;
		}
	
		String condLabel = currentScript.getNextGeneratedLabel(LABEL_WHILE_CONDITIONAL); 
		String successLabel = currentScript.getNextGeneratedLabel(LABEL_WHILE_SUCCESS); 
		String endLabel = currentScript.getNextGeneratedLabel(LABEL_WHILE_END); 
		
		mark(currentScript, condLabel);
		if (!parseExpression(currentScript))
			return false;
	
		if (!matchType(ScriptKernel.TYPE_RPAREN))
		{
			addErrorMessage("Expected \")\" to end \"while\" conditional.");
			return false;
		}
	
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP_FALSE, endLabel));
	
		mark(currentScript, successLabel);
		if (!parseStatementBody(currentScript, endLabel, condLabel))
			return false;

		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, condLabel));
	
		mark(currentScript, endLabel);
		return true;
	}

	// <FOR> "(" <Statement> ";" <Expression> ";" <Statement> ")" <StatementBody>
	private boolean parseForClause(Script currentScript)
	{
		if (!matchType(ScriptKernel.TYPE_LPAREN))
		{
			addErrorMessage("Expected \"(\" to start \"for\" clauses.");
			return false;
		}

		String initLabel = currentScript.getNextGeneratedLabel(LABEL_FOR_INIT); 
		String condLabel = currentScript.getNextGeneratedLabel(LABEL_FOR_CONDITIONAL); 
		String stepLabel = currentScript.getNextGeneratedLabel(LABEL_FOR_STEP); 
		String successLabel = currentScript.getNextGeneratedLabel(LABEL_FOR_SUCCESS); 
		String endLabel = currentScript.getNextGeneratedLabel(LABEL_FOR_END); 
		
		mark(currentScript, initLabel);
		if (!parseStatementClause(currentScript, null, null))
			return false;

		if (!matchType(ScriptKernel.TYPE_SEMICOLON))
		{
			addErrorMessage("Expected a \";\" to terminate init statement.");
			return false;
		}

		mark(currentScript, condLabel);
		if (!parseExpression(currentScript))
			return false;
		
		if (!matchType(ScriptKernel.TYPE_SEMICOLON))
		{
			addErrorMessage("Expected a \";\" to terminate conditional expression.");
			return false;
		}

		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP_BRANCH, successLabel, endLabel));

		mark(currentScript, stepLabel);
		if (!parseStatementClause(currentScript, null, null))
			return false;

		if (!matchType(ScriptKernel.TYPE_RPAREN))
		{
			addErrorMessage("Expected \")\" to end \"for\" clauses.");
			return false;
		}

		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, condLabel));

		mark(currentScript, successLabel);
		if (!parseStatementBody(currentScript, endLabel, stepLabel))
			return false;

		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, stepLabel));

		mark(currentScript, endLabel);
		return true;
	}

	// <ExpressionPhrase>
	// If null, bad parse.
	private boolean parseExpression(Script currentScript)
	{
		// make stacks.
		Deque<Integer> operatorStack = new LinkedList<>();
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
						case ScriptKernel.TYPE_PLUS:
						case ScriptKernel.TYPE_DASH:
						case ScriptKernel.TYPE_STAR:
						case ScriptKernel.TYPE_SLASH:
						case ScriptKernel.TYPE_PERCENT:
						case ScriptKernel.TYPE_AMPERSAND:
						case ScriptKernel.TYPE_PIPE:
						case ScriptKernel.TYPE_CARAT:
						case ScriptKernel.TYPE_GREATER:
						case ScriptKernel.TYPE_GREATEREQUAL:
						case ScriptKernel.TYPE_DOUBLEGREATER:
						case ScriptKernel.TYPE_TRIPLEGREATER:
						case ScriptKernel.TYPE_LESS:
						case ScriptKernel.TYPE_DOUBLELESS:
						case ScriptKernel.TYPE_LESSEQUAL:
						case ScriptKernel.TYPE_DOUBLEEQUAL:
						case ScriptKernel.TYPE_TRIPLEEQUAL:
						case ScriptKernel.TYPE_NOTEQUAL:
						case ScriptKernel.TYPE_NOTDOUBLEEQUAL:
							nextOperator = type;
							break;
						default:
						{
							addErrorMessage("Unexpected binary operator miss.");
							return false;
						}
					}
					
					nextToken();
					if (!operatorReduce(currentScript, operatorStack, expressionValueCounter, nextOperator))
						return false;
					
					operatorStack.push(nextOperator);
					lastWasValue = false;
				}
				// partial application operator
				else if (matchType(ScriptKernel.TYPE_RIGHTARROW))
				{
					if (!parsePartialChain(currentScript))
						return false;
					
					lastWasValue = true;
				}
				// logical and: short circuit
				else if (matchType(ScriptKernel.TYPE_DOUBLEAMPERSAND))
				{
					// treat with low precedence.
					if (!expressionReduceAll(currentScript, operatorStack, expressionValueCounter))
						return false;
					
					String labeltrue = currentScript.getNextGeneratedLabel(LABEL_SSAND_TRUE);
					String labelfalse = currentScript.getNextGeneratedLabel(LABEL_SSAND_FALSE);
					String labelend = currentScript.getNextGeneratedLabel(LABEL_SSAND_END);
					
					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP_FALSE, labelfalse));
					
					mark(currentScript, labeltrue);
					if (!parseExpression(currentScript))
						return false;
					
					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, labelend));
					mark(currentScript, labelfalse);

					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, false));
					mark(currentScript, labelend);	
				}
				// logical or: short circuit
				else if (matchType(ScriptKernel.TYPE_DOUBLEPIPE))
				{
					// treat with low precedence.
					if (!expressionReduceAll(currentScript, operatorStack, expressionValueCounter))
						return false;
					
					String labeltrue = currentScript.getNextGeneratedLabel(LABEL_SSOR_TRUE);
					String labelfalse = currentScript.getNextGeneratedLabel(LABEL_SSOR_FALSE);
					String labelend = currentScript.getNextGeneratedLabel(LABEL_SSOR_END);
					
					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP_TRUE, labeltrue));
					
					mark(currentScript, labelfalse);
					if (!parseExpression(currentScript))
						return false;
					
					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, labelend));
					mark(currentScript, labeltrue);

					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, true));
					mark(currentScript, labelend);	
					
				}
				// null coalesce "Elvis" operator.
				else if (matchType(ScriptKernel.TYPE_COALESCE))
				{
					// treat with lowest possible precedence.
					if (!expressionReduceAll(currentScript, operatorStack, expressionValueCounter))
						return false;
					
					String startLabel = currentScript.getNextGeneratedLabel(LABEL_COALESCE_START);
					String endLabel = currentScript.getNextGeneratedLabel(LABEL_COALESCE_END);
					
					mark(currentScript, startLabel);
					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP_COALESCE, endLabel));

					if (!parseExpression(currentScript))
						return false;

					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, endLabel));
					
					mark(currentScript, endLabel);
				}
				// ternary operator type.
				else if (matchType(ScriptKernel.TYPE_QUESTIONMARK))
				{
					// treat with lowest possible precedence.
					if (!expressionReduceAll(currentScript, operatorStack, expressionValueCounter))
						return false;
					
					String trueLabel = currentScript.getNextGeneratedLabel(LABEL_TERNARY_TRUE);
					String falseLabel = currentScript.getNextGeneratedLabel(LABEL_TERNARY_FALSE);
					String endLabel = currentScript.getNextGeneratedLabel(LABEL_TERNARY_END);
					
					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP_FALSE, falseLabel));
					
					mark(currentScript, trueLabel);
					if (!parseExpression(currentScript))
						return false;
					
					if (!matchType(ScriptKernel.TYPE_COLON))
					{
						addErrorMessage("Expected \":\" for ternary operator separator.");
						return false;
					}
					currentScript.addCommand(ScriptCommand.create(ScriptCommandType.JUMP, endLabel));

					mark(currentScript, falseLabel);
					if (!parseExpression(currentScript))
						return false;

					mark(currentScript, endLabel);
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
						case ScriptKernel.TYPE_PLUS:
							operatorStack.push(ScriptKernel.TYPE_ABSOLUTE);
							break;
						case ScriptKernel.TYPE_DASH:
							operatorStack.push(ScriptKernel.TYPE_NEGATE);
							break;
						case ScriptKernel.TYPE_EXCLAMATION:
							operatorStack.push(ScriptKernel.TYPE_EXCLAMATION);
							break;
						case ScriptKernel.TYPE_TILDE:
							operatorStack.push(ScriptKernel.TYPE_TILDE);
							break;
						default:
							throw new ScriptParseException("Unexpected unary operator miss.");
					}
					nextToken();
					lastWasValue = false;
				}
				// parens.
				else if (matchType(ScriptKernel.TYPE_LPAREN))
				{
					if (!parseExpression(currentScript))
						return false;
					
					if (!matchType(ScriptKernel.TYPE_RPAREN))
					{
						addErrorMessage("Expected ending \")\".");
						return false;
					}
					
					expressionValueCounter[0] += 1;
					lastWasValue = true;
				}

				// square brackets (literal list).
				else if (matchType(ScriptKernel.TYPE_LBRACK))
				{
					if (!parseListLiteral(currentScript))
						return false;
					
					if (!matchType(ScriptKernel.TYPE_RBRACK))
					{
						addErrorMessage("Expected ending \"]\" to terminate list.");
						return false;
					}
					
					expressionValueCounter[0] += 1;
					lastWasValue = true;
				}
				
				// braces (literal map).
				else if (matchType(ScriptKernel.TYPE_LBRACE))
				{
					if (!parseMapLiteral(currentScript))
						return false;
					
					if (!matchType(ScriptKernel.TYPE_RBRACE))
					{
						addErrorMessage("Expected ending \"}\" to terminate map.");
						return false;
					}
					
					expressionValueCounter[0] += 1;
					lastWasValue = true;
				}
				
				// identifier - can be the start of a lot of things.
				else if (currentType(ScriptKernel.TYPE_IDENTIFIER))
				{
					String lexeme = currentToken().getLexeme();
					nextToken();
					
					// function call?
					if (currentType(ScriptKernel.TYPE_LPAREN))
					{
						if (parseFunctionCall(currentScript, null, lexeme, false) == PARSEFUNCTION_FALSE)
							return false;
					}
					// array resolution or map deref?
					else if (currentType(ScriptKernel.TYPE_LBRACK, ScriptKernel.TYPE_PERIOD))
					{
						currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_VARIABLE, lexeme));

						if (!parseListMapDerefChain(currentScript))
							return false;
					}
					// scope deref?
					else if (currentType(ScriptKernel.TYPE_DOUBLECOLON))
					{
						nextToken();
						if (!currentType(ScriptKernel.TYPE_IDENTIFIER))
						{
							addErrorMessage("Expected identifier after scope dereference.");
							return false;
						}
						
						String var = currentToken().getLexeme();
						nextToken();
						
						currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_SCOPE_VARIABLE, lexeme, var));

						if (!parseListMapDerefChain(currentScript))
							return false;
					}
					// must be local variable?
					else
					{
						currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_VARIABLE, lexeme));
					}
					
					expressionValueCounter[0] += 1;
					lastWasValue = true;
				}
				// literal value?
				else if (isValidLiteralType(type))
				{
					if (!parseSingleValue(currentScript))
						return false;
	
					expressionValueCounter[0] += 1;
					lastWasValue = true;
				}
				else
					throw new ScriptParseException("Expression - Expected value.");
				
			}
			
		}
		
		if (!expressionReduceAll(currentScript, operatorStack, expressionValueCounter))
			return false;
		
		if (expressionValueCounter[0] != 1)
		{
			addErrorMessage("Expected valid expression.");
			return false;
		}
	
		return true;
	}

	// Parses a dereferencing expression chain of list indices and map values.
	private boolean parseListMapDerefChain(Script currentScript)
	{
		while (currentType(ScriptKernel.TYPE_LBRACK, ScriptKernel.TYPE_PERIOD))
		{
			if (currentType(ScriptKernel.TYPE_LBRACK))
			{
				nextToken();
				if (!parseExpression(currentScript))
					return false;

				if (!matchType(ScriptKernel.TYPE_RBRACK))
				{
					addErrorMessage("Expected \"]\" after a list index expression.");
					return false;
				}

				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_LIST_INDEX));
			}
			else if (currentType(ScriptKernel.TYPE_PERIOD))
			{
				nextToken();
				if (!currentType(ScriptKernel.TYPE_IDENTIFIER))
				{
					addErrorMessage("Expected map key identifier.");
					return false;
				}
				
				String key = currentToken().getLexeme();
				nextToken();

				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, key));
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_MAP_KEY));
			}
			else
			{
				addErrorMessage("INTERNAL ERROR - EXPECTED [ or .");
				return false;
			}
		}
		
		return true;
	}

	// parses a partial application chain.
	private boolean parsePartialChain(Script currentScript)
	{
		if (!currentType(ScriptKernel.TYPE_IDENTIFIER))
		{
			addErrorMessage("Expected function call after partial operator.");
			return false;
		}

		String functionName = currentToken().getLexeme();
		nextToken();
		
		if (!currentType(ScriptKernel.TYPE_LPAREN))
		{
			addErrorMessage("Expected function call after partial operator.");
			return false;
		}
		
		int funcret = parseFunctionCall(currentScript, null, functionName, true);
		if (funcret == PARSEFUNCTION_FALSE)
			return false;
		
		if (currentType(ScriptKernel.TYPE_RIGHTARROW))
		{
			nextToken();
			if (!parsePartialChain(currentScript))
				return false;
			return true;
		}
		
		return true;
	}

	// Parses a literally defined map.
	// 		{ .... , .... }
	private boolean parseMapLiteral(Script currentScript)
	{
		// if no map fields.
		if (currentType(ScriptKernel.TYPE_RBRACE))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_MAP_NEW));
			return true;
		}
		
		if (!parseMapField(currentScript))
			return false;
		
		int i = 1;
		while (matchType(ScriptKernel.TYPE_COMMA))
		{
			if (!parseMapField(currentScript))
				return false;
			i++;
		}
		
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, i));
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_MAP_INIT));
		return true;
	}
	
	// Parses a map field.
	// 		[IDENTIFIER] ":" <Expression>
	private boolean parseMapField(Script currentScript)
	{
		// if no map fields.
		if (!currentType(ScriptKernel.TYPE_IDENTIFIER))
		{
			addErrorMessage("Expected map key (identifier) or '}' to end map literal.");
			return false;
		}
		
		String key = currentToken().getLexeme();
		nextToken();
		
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, key));
		
		if (!matchType(ScriptKernel.TYPE_COLON))
		{
			addErrorMessage("Expected ':' after map key.");
			return false;			
		}

		if (!parseExpression(currentScript))
			return false;
		
		return true;
	}
	
	// Parses a literally defined list.
	// 		[ .... , .... ]
	private boolean parseListLiteral(Script currentScript)
	{
		// if no elements.
		if (currentType(ScriptKernel.TYPE_RBRACK))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_LIST_NEW));
			return true;
		}
		
		if (!parseExpression(currentScript))
			return false;
		
		int i = 1;
		while (matchType(ScriptKernel.TYPE_COMMA))
		{
			if (!parseExpression(currentScript))
				return false;
			i++;
		}
		
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, i));
		currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_LIST_INIT));
		return true;
	}

	// Parses a function call.
	// 		( .... , .... )
	// Returns amount of arguments parsed.
	private int parseLocalFunctionCall(Script currentScript, int paramCount)
	{
		int parsed = 0;
		if (currentType(ScriptKernel.TYPE_RPAREN))
			return 0;
		while (paramCount-- > 0)
		{
			if (!parseExpression(currentScript))
				return PARSEFUNCTIONCALL_FALSE;
			
			parsed++;
			
			if (paramCount > 0)
			{
				if (!matchType(ScriptKernel.TYPE_COMMA))
					return parsed; 
			}
		}
	
		return parsed;
	}

	// Parses a host function call.
	// 		( .... , .... )
	// Returns amount of arguments parsed.
	private int parseHostFunctionCall(Script currentScript, ScriptFunctionType functionType, boolean partial)
	{
		int paramCount = functionType.getParameterCount() - (partial ? 1 : 0);
		int parsed = partial ? 1 : 0;
		while (paramCount-- > 0)
		{
			if (!parseExpression(currentScript))
				return PARSEFUNCTIONCALL_FALSE;
			
			parsed++;
			
			if (paramCount > 0)
			{
				if (!matchType(ScriptKernel.TYPE_COMMA))
					return parsed; 
			}
		}
	
		return parsed;
	}

	// <ExpressionValue> :
	//		
	// If null, bad parse.
	private boolean parseSingleValue(Script currentScript)
	{
		if (matchType(ScriptKernel.TYPE_NULL))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH_NULL));
			return true;
		}
		else if (matchType(ScriptKernel.TYPE_TRUE))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, true));
			return true;
		}
		else if (matchType(ScriptKernel.TYPE_FALSE))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, false));
			return true;
		}
		else if (matchType(ScriptKernel.TYPE_INFINITY))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, Double.POSITIVE_INFINITY));
			return true;
		}
		else if (matchType(ScriptKernel.TYPE_NAN))
		{
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, Double.NaN));
			return true;
		}
		else if (currentType(ScriptKernel.TYPE_NUMBER))
		{
			String lexeme = currentToken().getLexeme();
			nextToken();
			if (lexeme.startsWith("0X") || lexeme.startsWith("0x"))
			{
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, Long.parseLong(lexeme.substring(2), 16)));
				return true;
			}
			else if (lexeme.contains("."))
			{
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, Double.parseDouble(lexeme)));
				return true;
			}
			else
			{
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, Long.parseLong(lexeme)));
				return true;
			}
		}
		else if (currentType(ScriptKernel.TYPE_STRING))
		{
			String lexeme = currentToken().getLexeme();
			nextToken();
			currentScript.addCommand(ScriptCommand.create(ScriptCommandType.PUSH, lexeme));
			return true;
		}
		else
		{
			addErrorMessage("Expression - Expected a literal value.");
			return false;
		}
	}

	// Operator reduce.
	private boolean operatorReduce(Script currentScript, Deque<Integer> operatorStack, int[] expressionValueCounter, int nextOperator) 
	{
		Integer top = operatorStack.peek();
		while (top != null && (getOperatorPrecedence(top) > getOperatorPrecedence(nextOperator) || (getOperatorPrecedence(top) == getOperatorPrecedence(nextOperator) && !isOperatorRightAssociative(nextOperator))))
		{
			if (!expressionReduce(currentScript, operatorStack, expressionValueCounter))
				return false;
			top = operatorStack.peek();
		}
		
		return true;
	}

	// Reduces an expression by operator.
	private boolean expressionReduce(Script currentScript, Deque<Integer> operatorStack, int[] expressionValueCounter)
	{
		if (operatorStack.isEmpty())
			throw new ScriptParseException("Internal error - operator stack must have one operator in it.");
	
		int operator = operatorStack.pollFirst();
		
		if (isBinaryOperatorType(operator))
			expressionValueCounter[0] -= 2;
		else
			expressionValueCounter[0] -= 1;
		
		if (expressionValueCounter[0] < 0)
			throw new ScriptParseException("Internal error - value counter did not have enough counter.");
		
		expressionValueCounter[0] += 1; // the "push"
	
		return emitArithmeticCommand(currentScript, operator);
		
	}

	// reduce everything currently pending.
	private boolean expressionReduceAll(Script currentScript, Deque<Integer> operatorStack, int[] expressionValueCounter)
	{
		// end of expression - reduce.
		while (!operatorStack.isEmpty())
		{
			if (!expressionReduce(currentScript, operatorStack, expressionValueCounter))
				return false;
		}
		
		return true;
	}

	// Emits an arithmetic command based on an operator token type.
	private boolean emitArithmeticCommand(Script currentScript, int operator)
	{
		switch (operator)
		{
			case ScriptKernel.TYPE_ABSOLUTE:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.ABSOLUTE));
				return true;
			case ScriptKernel.TYPE_EXCLAMATION: 
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.LOGICAL_NOT));
				return true;
			case ScriptKernel.TYPE_TILDE: 
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.NOT));
				return true;
			case ScriptKernel.TYPE_NEGATE:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.NEGATE));
				return true;
			case ScriptKernel.TYPE_PLUS:
			case ScriptKernel.TYPE_PLUSEQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.ADD));
				return true;
			case ScriptKernel.TYPE_DASH:
			case ScriptKernel.TYPE_DASHEQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.SUBTRACT));
				return true;
			case ScriptKernel.TYPE_STAR:
			case ScriptKernel.TYPE_STAREQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.MULTIPLY));
				return true;
			case ScriptKernel.TYPE_SLASH:
			case ScriptKernel.TYPE_SLASHEQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.DIVIDE));
				return true;
			case ScriptKernel.TYPE_PERCENT:
			case ScriptKernel.TYPE_PERCENTEQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.MODULO));
				return true;
			case ScriptKernel.TYPE_AMPERSAND:
			case ScriptKernel.TYPE_AMPERSANDEQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.AND));
				return true;
			case ScriptKernel.TYPE_PIPE:
			case ScriptKernel.TYPE_PIPEEQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.OR));
				return true;
			case ScriptKernel.TYPE_CARAT:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.XOR));
				return true;
			case ScriptKernel.TYPE_GREATER:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.GREATER));
				return true;
			case ScriptKernel.TYPE_GREATEREQUAL:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.GREATER_OR_EQUAL));
				return true;
			case ScriptKernel.TYPE_DOUBLEGREATER:
			case ScriptKernel.TYPE_DOUBLEGREATEREQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.RIGHT_SHIFT));
				return true;
			case ScriptKernel.TYPE_TRIPLEGREATER:
			case ScriptKernel.TYPE_TRIPLEGREATEREQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.RIGHT_SHIFT_PADDED));
				return true;
			case ScriptKernel.TYPE_LESS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.LESS));
				return true;
			case ScriptKernel.TYPE_DOUBLELESS:
			case ScriptKernel.TYPE_DOUBLELESSEQUALS:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.LEFT_SHIFT));
				return true;
			case ScriptKernel.TYPE_LESSEQUAL:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.LESS_OR_EQUAL));
				return true;
			case ScriptKernel.TYPE_DOUBLEEQUAL:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.EQUAL));
				return true;
			case ScriptKernel.TYPE_TRIPLEEQUAL:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.STRICT_EQUAL));
				return true;
			case ScriptKernel.TYPE_NOTEQUAL:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.NOT_EQUAL));
				return true;
			case ScriptKernel.TYPE_NOTDOUBLEEQUAL:
				currentScript.addCommand(ScriptCommand.create(ScriptCommandType.STRICT_NOT_EQUAL));
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
			case ScriptKernel.TYPE_FUNCTION:
			case ScriptKernel.TYPE_ENTRY:
			case ScriptKernel.TYPE_PRAGMA:
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
			case ScriptKernel.TYPE_SEMICOLON:
			case ScriptKernel.TYPE_BREAK:
			case ScriptKernel.TYPE_CONTINUE:
			case ScriptKernel.TYPE_RETURN:
			case ScriptKernel.TYPE_IDENTIFIER:
			case ScriptKernel.TYPE_LPAREN:
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
			case ScriptKernel.TYPE_IF:
			case ScriptKernel.TYPE_WHILE:
			case ScriptKernel.TYPE_FOR:
				return true;
		}
	}

	// Return true if token type can be a unary operator.
	private boolean isValidLiteralType(int tokenType)
	{
		switch (tokenType)
		{
			case ScriptKernel.TYPE_NUMBER:
			case ScriptKernel.TYPE_TRUE:
			case ScriptKernel.TYPE_FALSE:
			case ScriptKernel.TYPE_INFINITY:
			case ScriptKernel.TYPE_NAN:
			case ScriptKernel.TYPE_STRING:
			case ScriptKernel.TYPE_NULL:
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
			case ScriptKernel.TYPE_DASH:
			case ScriptKernel.TYPE_PLUS:
			case ScriptKernel.TYPE_EXCLAMATION:
			case ScriptKernel.TYPE_TILDE:
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
			case ScriptKernel.TYPE_PLUS:
			case ScriptKernel.TYPE_DASH:
			case ScriptKernel.TYPE_STAR:
			case ScriptKernel.TYPE_SLASH:
			case ScriptKernel.TYPE_PERCENT:
			case ScriptKernel.TYPE_AMPERSAND:
			case ScriptKernel.TYPE_PIPE:
			case ScriptKernel.TYPE_CARAT:
			case ScriptKernel.TYPE_GREATER:
			case ScriptKernel.TYPE_GREATEREQUAL:
			case ScriptKernel.TYPE_DOUBLEGREATER:
			case ScriptKernel.TYPE_TRIPLEGREATER:
			case ScriptKernel.TYPE_LESS:
			case ScriptKernel.TYPE_DOUBLELESS:
			case ScriptKernel.TYPE_LESSEQUAL:
			case ScriptKernel.TYPE_DOUBLEEQUAL:
			case ScriptKernel.TYPE_TRIPLEEQUAL:
			case ScriptKernel.TYPE_NOTEQUAL:
			case ScriptKernel.TYPE_NOTDOUBLEEQUAL:
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
			case ScriptKernel.TYPE_ABSOLUTE: 
			case ScriptKernel.TYPE_EXCLAMATION: 
			case ScriptKernel.TYPE_TILDE: 
			case ScriptKernel.TYPE_NEGATE:
				return 20;
			case ScriptKernel.TYPE_STAR:
			case ScriptKernel.TYPE_SLASH:
			case ScriptKernel.TYPE_PERCENT:
				return 18;
			case ScriptKernel.TYPE_PLUS:
			case ScriptKernel.TYPE_DASH:
				return 16;
			case ScriptKernel.TYPE_DOUBLEGREATER:
			case ScriptKernel.TYPE_TRIPLEGREATER:
			case ScriptKernel.TYPE_DOUBLELESS:
				return 14;
			case ScriptKernel.TYPE_GREATER:
			case ScriptKernel.TYPE_GREATEREQUAL:
			case ScriptKernel.TYPE_LESS:
			case ScriptKernel.TYPE_LESSEQUAL:
				return 12;
			case ScriptKernel.TYPE_DOUBLEEQUAL:
			case ScriptKernel.TYPE_TRIPLEEQUAL:
			case ScriptKernel.TYPE_NOTEQUAL:
			case ScriptKernel.TYPE_NOTDOUBLEEQUAL:
				return 10;
			case ScriptKernel.TYPE_AMPERSAND:
				return 8;
			case ScriptKernel.TYPE_CARAT:
				return 6;
			case ScriptKernel.TYPE_PIPE:
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
			case ScriptKernel.TYPE_ABSOLUTE: 
			case ScriptKernel.TYPE_EXCLAMATION: 
			case ScriptKernel.TYPE_TILDE: 
			case ScriptKernel.TYPE_NEGATE: 
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
			case ScriptKernel.TYPE_EQUAL: 
			case ScriptKernel.TYPE_PLUSEQUALS:
			case ScriptKernel.TYPE_DASHEQUALS:
			case ScriptKernel.TYPE_STAREQUALS:
			case ScriptKernel.TYPE_SLASHEQUALS:
			case ScriptKernel.TYPE_PERCENTEQUALS:
			case ScriptKernel.TYPE_AMPERSANDEQUALS:
			case ScriptKernel.TYPE_PIPEEQUALS:
			case ScriptKernel.TYPE_DOUBLEGREATEREQUALS:
			case ScriptKernel.TYPE_TRIPLEGREATEREQUALS:
			case ScriptKernel.TYPE_DOUBLELESSEQUALS:
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
			case ScriptKernel.TYPE_PLUSEQUALS:
			case ScriptKernel.TYPE_DASHEQUALS:
			case ScriptKernel.TYPE_STAREQUALS:
			case ScriptKernel.TYPE_SLASHEQUALS:
			case ScriptKernel.TYPE_PERCENTEQUALS:
			case ScriptKernel.TYPE_AMPERSANDEQUALS:
			case ScriptKernel.TYPE_PIPEEQUALS:
			case ScriptKernel.TYPE_DOUBLEGREATEREQUALS:
			case ScriptKernel.TYPE_TRIPLEGREATEREQUALS:
			case ScriptKernel.TYPE_DOUBLELESSEQUALS:
				return true;
			default:
				return false;
		}
	}
	
}
