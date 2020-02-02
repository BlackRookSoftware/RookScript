/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.compiler;

import com.blackrook.rookscript.struct.Lexer;

/**
 * The script language lexer kernel.
 * @author Matthew Tropiano
 */
public class ScriptKernel extends Lexer.Kernel
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
	public static final int TYPE_DOUBLECOLON = 11;
	public static final int TYPE_PERIOD = 12;
	
	public static final int TYPE_FALSECOALESCE = 17;
	public static final int TYPE_NULLCOALESCE = 18;
	public static final int TYPE_RIGHTARROW = 19;
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

	public static final int TYPE_NULL = 100;
	public static final int TYPE_TRUE = 101;
	public static final int TYPE_FALSE = 102;
	public static final int TYPE_LBRACE = 103;
	public static final int TYPE_RBRACE = 104;
	public static final int TYPE_INFINITY = 105;
	public static final int TYPE_NAN = 106;
	public static final int TYPE_RETURN = 107;
	public static final int TYPE_IF = 108;
	public static final int TYPE_ELSE = 109;
	public static final int TYPE_WHILE = 110;
	public static final int TYPE_FOR = 111;
	public static final int TYPE_ENTRY = 112;
	public static final int TYPE_FUNCTION = 113;
	public static final int TYPE_PRAGMA = 114;
	public static final int TYPE_BREAK = 115;
	public static final int TYPE_CONTINUE = 116;
	public static final int TYPE_EACH = 117;
	public static final int TYPE_TRY = 118;
	public static final int TYPE_CATCH = 119;
	public static final int TYPE_FINALLY = 120;
	
	/**
	 * Creates a new script lexer kernel.
	 */
	public ScriptKernel()
	{
		setDecimalSeparator('.');

		addStringDelimiter('"', '"');
		addRawStringDelimiter('`', '`');
		
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
		addDelimiter("::", TYPE_DOUBLECOLON);
		addDelimiter("?", TYPE_QUESTIONMARK);
		addDelimiter("->", TYPE_RIGHTARROW);
		addDelimiter("?:", TYPE_FALSECOALESCE);
		addDelimiter("??", TYPE_NULLCOALESCE);

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
		
		addCaseInsensitiveKeyword("null", TYPE_NULL);
		addCaseInsensitiveKeyword("true", TYPE_TRUE);
		addCaseInsensitiveKeyword("false", TYPE_FALSE);
		addCaseInsensitiveKeyword("infinity", TYPE_INFINITY);
		addCaseInsensitiveKeyword("nan", TYPE_NAN);
		addCaseInsensitiveKeyword("if", TYPE_IF);
		addCaseInsensitiveKeyword("else", TYPE_ELSE);
		addCaseInsensitiveKeyword("return", TYPE_RETURN);
		addCaseInsensitiveKeyword("while", TYPE_WHILE);
		addCaseInsensitiveKeyword("for", TYPE_FOR);
		addCaseInsensitiveKeyword("each", TYPE_EACH);
		addCaseInsensitiveKeyword("entry", TYPE_ENTRY);
		addCaseInsensitiveKeyword("function", TYPE_FUNCTION);
		addCaseInsensitiveKeyword("break", TYPE_BREAK);
		addCaseInsensitiveKeyword("continue", TYPE_CONTINUE);
		addCaseInsensitiveKeyword("pragma", TYPE_PRAGMA);
		addCaseInsensitiveKeyword("try", TYPE_TRY);
		addCaseInsensitiveKeyword("catch", TYPE_CATCH);
		addCaseInsensitiveKeyword("finally", TYPE_FINALLY);
	}
}
