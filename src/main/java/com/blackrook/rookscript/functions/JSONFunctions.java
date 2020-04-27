/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptIteratorType.IteratorPair;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.BufferType;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.exception.ScriptValueConversionException;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.struct.Lexer;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Script common functions for converting JSON to Maps (and vice-versa).
 * @author Matthew Tropiano
 * @since [NOW]
 */
public enum JSONFunctions implements ScriptFunctionType
{
	READJSON(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Converts JSON data to a script value. The provided stream is read until a full JSON value is parsed."
				)
				.parameter("jsoninput",
					type(Type.STRING, "The JSON string to convert."),
					type(Type.OBJECTREF, "File", "The file to read JSON from (assumes UTF-8 encoding)."),
					type(Type.OBJECTREF, "InputStream", "The input stream to read JSON from (assumes UTF-8 encoding)."),
					type(Type.OBJECTREF, "Reader", "The reader to read JSON from.")
				)
				.returns(
					type("The resultant value read."),
					type(Type.ERROR, "BadParameter", "If [json] is not a valid input type."),
					type(Type.ERROR, "BadFile", "If [json] is a file and is not found."),
					type(Type.ERROR, "BadParse", "If a JSON parse error occurs."),
					type(Type.ERROR, "Security", "If [json] is a file and the OS is preventing the read."),
					type(Type.ERROR, "IOError", "If a read or write error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				Reader reader;
				boolean close = false;
				if (temp.isString())
					reader = new StringReader(temp.asString());
				else if (temp.isObjectRef(File.class))
				{
					try {
						reader = new BufferedReader(new InputStreamReader(new FileInputStream(temp.asObjectType(File.class)), UTF_8));
						close = true;
					} catch (FileNotFoundException e) {
						returnValue.setError("BadFile", e.getMessage(), e.getLocalizedMessage());
						return true;
					} catch (SecurityException e) {
						returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
						return true;
					}
				}
				else if (temp.isObjectRef(InputStream.class))
					reader = new BufferedReader(new InputStreamReader(temp.asObjectType(InputStream.class), UTF_8));
				else if (temp.isObjectRef(Reader.class))
					reader = new BufferedReader(temp.asObjectType(Reader.class));
				else
				{
					returnValue.setError("BadParameter", "First parameter is not a valid input.");
					return true;
				}

				try {
					parseJSON(reader, returnValue);
				} catch (ScriptValueConversionException e) {
					returnValue.setError("BadParse", e.getMessage());
				} finally {
					if (close)
					{
						try {
							reader.close();
						} catch (IOException e) {
							// Ignore.
						}
					}
				}
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	WRITEJSON(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Converts a script value to JSON and writes it to a provided output. Objects will be " +
					"reflection-exported as maps, and buffers as arrays of unsigned byte values."
				)
				.parameter("output", 
					type(Type.OBJECTREF, "File", "The file to write the JSON to (encoding is UTF-8, file is overwritten, and then closed)."),
					type(Type.OBJECTREF, "OutputStream", "The output stream to write the JSON to (encoding is UTF-8)."),
					type(Type.OBJECTREF, "Writer", "The Writer to write the JSON to.")
				)
				.parameter("value",
					type("The value to export as JSON. Objects will be reflection-exported as maps, and buffers as arrays of unsigned byte values.")
				)
				.returns(
					type(Type.OBJECTREF, "output."),
					type(Type.ERROR, "Security", "If [output] is a file and the OS is preventing the creation/write."),
					type(Type.ERROR, "IOError", "If a write error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			ScriptValue output = CACHEVALUE2.get();
			try 
			{
				scriptInstance.popStackValue(temp);
				scriptInstance.popStackValue(output);
				
				Writer writer = null;
				boolean close = false;
				
				try {
					if (output.isObjectRef(File.class))
					{
						writer = new OutputStreamWriter(new FileOutputStream(output.asObjectType(File.class)), UTF_8);
						close = true;
					}
					else if (output.isObjectRef(OutputStream.class))
						writer = new OutputStreamWriter(output.asObjectType(OutputStream.class), UTF_8);
					else if (output.isObjectRef(Writer.class))
						writer = output.asObjectType(Writer.class);
					else
					{
						returnValue.setError("BadParameter", "First parameter is not a valid Writer.");
						return true;
					}
					
					writeJSONValue(temp, writer);
					
				} catch (SecurityException e) {
					returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
					return true;
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				} finally {
					if (close)
					{
						try {
							writer.flush();
							writer.close();
						} catch (IOException e) {
							// Ignore.
						}
					}
				}
				
				returnValue.set(temp);
				return true;
			}
			finally
			{
				temp.setNull();
				output.setNull();
			}
		}
	},

	JSONSTR(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Converts a script value to JSON and returns it as a string. Objects will be " +
					"reflection-exported as maps, and buffers as arrays of unsigned byte values."
				)
				.parameter("value",
					type("The value to export as JSON.")
				)
				.returns(
					type(Type.STRING, "The resultant string."),
					type(Type.ERROR, "IOError", "If a write error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try 
			{
				scriptInstance.popStackValue(temp);
				
				try {
					StringWriter sw = new StringWriter(256);
					writeJSONValue(temp, sw);
					returnValue.set(sw.toString());
					return true;
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				}
			}
			finally
			{
				temp.setNull();
			}
		}
	},

	;
	
	private final int parameterCount;
	private Usage usage;
	private JSONFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(JSONFunctions.values());
	}

	@Override
	public int getParameterCount()
	{
		return parameterCount;
	}

	@Override
	public Usage getUsage()
	{
		if (usage == null)
			usage = usage();
		return usage;
	}
	
	@Override
	public abstract boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue);

	protected abstract Usage usage();
	
	
	// Throws a ParseException.
	private static void parseJSON(Reader reader, ScriptValue target)
	{
		(new JSONParser(reader)).parseValueInto(target);
	}

	/**
	 * Writes a ScriptValue as JSON.
	 * @param object the object to write.
	 * @param writer the target writer.
	 * @throws IOException if an error occurs on the write.
	 */
	private static void writeJSONValue(ScriptValue object, Writer writer) throws IOException
	{
		if (object.isNull())
			writer.append("null");
		else if (object.isObjectRef())
		{
			ScriptValue value = ScriptValue.createEmptyMap();
			value.mapExtract(object.asObject());
			writeJSONValue(value, writer);
		}
		else if (object.isBuffer())
		{
			writer.write('[');
			BufferType buf = object.asObjectType(BufferType.class);
			for (int i = 0; i < buf.size(); i++)
			{
				writer.write(String.valueOf(buf.getByte(i)));
				if (i < buf.size() - 1)
					writer.append(',');
			}
			writer.append(']');
		}
		else if (object.isList())
		{
			writer.write('[');
			int i = 0;
			int len = object.length();
			for (IteratorPair member : object)
			{
				writeJSONValue(member.getValue(), writer);
				if (i < len - 1)
					writer.append(',');
				i++;
			}
			writer.append(']');
		}
		else if (object.isMap())
		{
			writer.write('{');
			int i = 0;
			int len = object.length();
			for (IteratorPair member : object)
			{
				writer.write('"');
				escape(member.getKey().asString(), writer);
				writer.write("\":");
				writeJSONValue(member.getValue(), writer);
				if (i < len - 1)
					writer.write(',');
				i++;
			}
			writer.write('}');
		}
		else if (object.isString())
		{
			writer.write('"');
			escape(object.asString(), writer);
			writer.write('"');
		}
		else if (object.isInfinite() || object.isNaN())
		{
			writer.write('"');
			escape(String.valueOf(object.asDouble()), writer);
			writer.write('"');
		}
		else if (object.isFloat())
		{
			writer.write(String.valueOf(object.asDouble()));
		}
		else if (object.isInteger())
		{
			writer.write(String.valueOf(object.asLong()));
		}
		else if (object.isBoolean())
		{
			writer.write(String.valueOf(object.asBoolean()));
		}
		else
		{
			writer.write('"');
			escape(String.valueOf(object.asObject()), writer);
			writer.write('"');
		}
	}
	
	private static void escape(String s, Writer writer) throws IOException
	{
    	for (int i = 0; i < s.length(); i++)
    	{
    		char c = s.charAt(i);
    		switch (c)
    		{
				case '\0':
					writer.append("\\0");
					break;
    			case '\b':
    				writer.append("\\b");
    				break;
    			case '\t':
    				writer.append("\\t");
    				break;
    			case '\n':
    				writer.append("\\n");
    				break;
    			case '\f':
    				writer.append("\\f");
    				break;
    			case '\r':
    				writer.append("\\r");
    				break;
    			case '\\':
    				writer.append("\\\\");
    				break;
    			case '"':
    				writer.append("\\\"");    					
    				break;
    			default:
    				if (c < 0x0020 || c >= 0x7f)
    				{
    					writer.write('\\');
    					writer.write('u');
    					writer.write(HEXALPHABET.charAt((c & 0x0f000) >> 12));
    					writer.write(HEXALPHABET.charAt((c & 0x00f00) >> 8));
    					writer.write(HEXALPHABET.charAt((c & 0x000f0) >> 4));
    					writer.write(HEXALPHABET.charAt(c & 0x0000f));
    				}
    				else
    					writer.append(c);
    				break;
    		}
    	}
	}

	private static class JSONLexerKernel extends Lexer.Kernel
	{
		static final int TYPE_TRUE = 		0;
		static final int TYPE_FALSE = 		1;
		static final int TYPE_NULL = 		2;
		static final int TYPE_LBRACE = 		3;
		static final int TYPE_RBRACE = 		4;
		static final int TYPE_COLON = 		5;
		static final int TYPE_COMMA = 		6;
		static final int TYPE_LBRACK = 		7;
		static final int TYPE_RBRACK = 		8;
		static final int TYPE_MINUS = 		9;

		private JSONLexerKernel()
		{
			addStringDelimiter('"', '"');
			
			addDelimiter("{", TYPE_LBRACE);
			addDelimiter("}", TYPE_RBRACE);
			addDelimiter("[", TYPE_LBRACK);
			addDelimiter("]", TYPE_RBRACK);
			addDelimiter(":", TYPE_COLON);
			addDelimiter(",", TYPE_COMMA);
			addDelimiter("-", TYPE_MINUS);
			
			addKeyword("true", TYPE_TRUE);
			addKeyword("false", TYPE_FALSE);
			addKeyword("null", TYPE_NULL);
			
			setDecimalSeparator('.');
		}
	}
	
	private static class JSONParser extends Lexer.Parser
	{
		protected JSONParser(Reader reader) 
		{
			super(new Lexer(LEXERKERNEL, reader));
			nextToken();
		}
		
		private void parseValueInto(ScriptValue target)
		{
			if (matchType(JSONLexerKernel.TYPE_MINUS))
			{
				if (!currentType(JSONLexerKernel.TYPE_NUMBER))
					throw new ScriptValueConversionException(getTokenInfoLine("Expected number after \"-\"."));
				
				parseNumberInto(currentToken().getLexeme(), true, target);
				nextToken();
			}
			else if (currentType(JSONLexerKernel.TYPE_TRUE))
			{
				target.set(true);
				nextToken();
			}
			else if (currentType(JSONLexerKernel.TYPE_FALSE))
			{
				target.set(false);
				nextToken();
			}
			else if (currentType(JSONLexerKernel.TYPE_NULL))
			{
				target.setNull();
				nextToken();
			}
			else if (currentType(JSONLexerKernel.TYPE_NUMBER))
			{
				parseNumberInto(currentToken().getLexeme(), false, target);
				nextToken();
			}
			else if (currentType(JSONLexerKernel.TYPE_STRING))
			{
				target.set(currentToken().getLexeme());
				nextToken();
			}
			else if (matchType(JSONLexerKernel.TYPE_LBRACK))
			{
				// empty array
				if (matchType(JSONLexerKernel.TYPE_RBRACK))
				{
					target.setEmptyList(8);
					return;
				}
				
				ScriptValue temp = ScriptValue.create(null);
				target.setEmptyList(8);
				do {
					parseValueInto(temp);
					target.listAdd(temp);
				} while (matchType(JSONLexerKernel.TYPE_COMMA));
				
				if (!matchType(JSONLexerKernel.TYPE_RBRACK))
					throw new ScriptValueConversionException(getTokenInfoLine("Expected \"]\" for end of array."));
			}
			else if (matchType(JSONLexerKernel.TYPE_LBRACE))
			{
				// empty object
				if (matchType(JSONLexerKernel.TYPE_RBRACE))
				{
					target.setEmptyMap(4);
					return;
				}
				
				target.setEmptyMap(8);
				ScriptValue temp = ScriptValue.create(null);
				
				do {
					if (!currentType(JSONLexerKernel.TYPE_STRING))
						throw new ScriptValueConversionException(getTokenInfoLine("Expected object member name."));
					
					String key = currentToken().getLexeme();
					nextToken();

					if (!matchType(JSONLexerKernel.TYPE_COLON))
						throw new ScriptValueConversionException(getTokenInfoLine("Expected \":\" after object member."));

					parseValueInto(temp);
					target.mapSet(key, temp);
					
				} while (matchType(JSONLexerKernel.TYPE_COMMA));
				
				if (!matchType(JSONLexerKernel.TYPE_RBRACE))
					throw new ScriptValueConversionException(getTokenInfoLine("Expected \"}\" for end of object."));
			}
		}

		// Parses a validated number.
		private void parseNumberInto(String s, boolean minus, ScriptValue out)
		{
			int idx = s.indexOf("x");
			int edx = Math.max(s.indexOf("e"), s.indexOf("E"));
			int pdx = -1;
			if (edx >= 0) 
				pdx = Math.max(edx, s.indexOf("+"));
			
			int fdx = s.indexOf(".");
			if (idx >= 0)
			{
				String lng = s.substring(idx + 1);
				try {
					long v = Long.parseLong(lng, 16);
					out.set(minus ? -v : v);
				} catch (NumberFormatException n) {
					throw new ScriptValueConversionException(getTokenInfoLine("Malformed number."), n);
				}
			}
			else if (edx >= 0)
			{
				String num = s.substring(0, edx);
				String exp = s.substring(pdx + 1);
				try {
					double v = Double.parseDouble(num) * Math.pow(10, Double.parseDouble(exp));
					out.set(minus ? -v : v);
				} catch (NumberFormatException n) {
					throw new ScriptValueConversionException(getTokenInfoLine("Malformed number."), n);
				}
			}
			else if (fdx >= 0)
			{
				try {
					double v = Double.parseDouble(s.toString());
					out.set(minus ? -v : v);
				} catch (NumberFormatException n) {
					throw new ScriptValueConversionException(getTokenInfoLine("Malformed number."), n);
				}
			}
			else
			{
				try {
					long v = Long.parseLong(s.toString());
					out.set(minus ? -v : v);
				} catch (NumberFormatException n) {
					throw new ScriptValueConversionException(getTokenInfoLine("Malformed number."), n);
				}
			}
		}
	}

	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final String HEXALPHABET = "0123456789ABCDEF";
	private static final JSONLexerKernel LEXERKERNEL = new JSONLexerKernel();
	
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
