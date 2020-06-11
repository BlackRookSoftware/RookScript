package com.blackrook.rookscript.functions;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptIteratorType.IteratorPair;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.BufferType;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.struct.AsyncFactory;
import com.blackrook.rookscript.struct.HTTPUtils;
import com.blackrook.rookscript.struct.HTTPUtils.HTTPContent;
import com.blackrook.rookscript.struct.HTTPUtils.HTTPHeaders;
import com.blackrook.rookscript.struct.HTTPUtils.HTTPParameters;
import com.blackrook.rookscript.struct.HTTPUtils.HTTPReader;
import com.blackrook.rookscript.struct.HTTPUtils.HTTPResponse;

/**
 * Script common functions for HTTP stuff.
 * These functions make use of an internal threadpool for asynchronous calls.
 * @author Matthew Tropiano
 */
public enum HTTPFunctions implements ScriptFunctionType
{
	CONTENT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Creates a content body for HTTP calls that require a body (PUT/POST)."
				)
				.parameter("type",
					type(Type.STRING, "The content MIME-Type (converted to string if not a string).")
				)
				.parameter("content",
					type(Type.STRING, "The text content (non-buffers are converted to string)."),
					type(Type.BUFFER, "Raw byte content (read from buffer cursor, cursor is advanced).")
				)
				.returns(
					type(Type.ERROR, "BadURL", "If [url] is malformed."),
					type(Type.ERROR, "BadProtocol", "If [method] is unsupported."),
					type(Type.ERROR, "Timeout", "If a connection timeout occurs."),
					type(Type.ERROR, "IOError", "If an I/O error occurs reading the response.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue type = CACHEVALUE1.get();
			ScriptValue content = CACHEVALUE2.get();
			try
			{
				scriptInstance.popStackValue(content);
				scriptInstance.popStackValue(type);
				String contentType = type.asString();
				
				if (content.isBuffer()) {
					BufferType buffer = content.asObjectType(BufferType.class);
					byte[] b = new byte[buffer.size() - buffer.getPosition()];
					
				}
				
				return true;
			}
			finally
			{
				type.setNull();
				content.setNull();
			}
		}
	},

	CALL(5)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Performs an HTTP call and returns a response. NOTE: If successful, the \"input\" field on " +
					"the returned map is registered as an open resource."
				)
				.parameter("method",
					type(Type.STRING, "The HTTP method (GET/PUT/POST/DELETE/HEAD/OPTIONS/TRACE).")
				)
				.parameter("url",
					type(Type.STRING, "The HTTP/HTTPS URL address to fetch from.")
				)
				.parameter("headers",
					type(Type.NULL, "No additional headers."),
					type(Type.MAP, "A map of additional headers to pass along to the request. Key is header name, value is header value.")
				)
				.parameter("content",
					type(Type.NULL, "No content."),
					type(Type.OBJECTREF, "HTTPContent", "Content to send (overwrites \"Content-Type\" header).")
				)
				.parameter("timeoutMillis",
					type(Type.NULL, "Use 10000 milliseconds."),
					type(Type.INTEGER, "Socket timeout in milliseconds.")
				)
				.returns(
					type(Type.MAP, "{headers:MAP, contenttype:STRING, status:INTEGER, message:STRING, length:INTEGER, type:STRING, charset:STRING, filename:STRING, encoding:STRING, input:OBJECTREF:DataInputStream}", 
						"A response info map. The \"input\" field is an open DataInputStream. " +
						"If this is a file download (attachment disposition), filename will not be null."
					),
					type(Type.ERROR, "BadURL", "If [url] is malformed."),
					type(Type.ERROR, "BadProtocol", "If [method] is unsupported."),
					type(Type.ERROR, "Timeout", "If a connection timeout occurs."),
					type(Type.ERROR, "IOError", "If an I/O error occurs reading the response.")
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
				String method = temp.asString();
				scriptInstance.popStackValue(temp);
				URL url = new URL(temp.asString());
				scriptInstance.popStackValue(temp);
				HTTPHeaders headers = createHeaders(temp);
				scriptInstance.popStackValue(temp);
				int timeout = temp.isNull() ? 10000 : temp.asInt();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(HTTPContent.class))
				{
					returnValue.setError("BadContent", "Parameter");
					return true;
				}
				
				HTTPResponse response = HTTPUtils.getHTTPContent(method, url, headers, temp.asObjectType(HTTPContent.class), null, timeout, HTTP_RESPONSE_READER);
				responseToMap(scriptInstance, response, returnValue);
				return true;
			}
			catch (MalformedURLException e)
			{
				returnValue.setError("BadURL", e.getMessage(), e.getLocalizedMessage());
				return true;
			}
			catch (ProtocolException e)
			{
				returnValue.setError("BadProtocol", e.getMessage(), e.getLocalizedMessage());
				return true;
			}
			catch (SocketTimeoutException e)
			{
				returnValue.setError("Timeout", e.getMessage(), e.getLocalizedMessage());
				return true;
			}
			catch (IOException e)
			{
				returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
				return true;
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
	private HTTPFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(HTTPFunctions.values());
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

	/**
	 * @return function usage.
	 */
	protected abstract Usage usage();
	
	private static HTTPHeaders createHeaders(ScriptValue value)
	{
		if (value.isMap())
		{
			HTTPHeaders headers = HTTPUtils.headers();
			for (IteratorPair pair : value)
				headers.setHeader(pair.getKey().asString(), pair.getValue().asString());
			return headers;
		}
		else
			return HTTPUtils.headers();
	}

	private static HTTPParameters createParameters(ScriptValue value)
	{
		if (value.isMap())
		{
			HTTPParameters parameters = HTTPUtils.parameters();
			for (IteratorPair pair : value)
			{
				String param = pair.getKey().asString();
				if (pair.getValue().isList())
				{
					for (IteratorPair v : pair.getValue())
						parameters.addParameter(param, v.getValue().asString());
				}
				else
					parameters.addParameter(param, value);
			}
			return parameters;
		}
		else
			return HTTPUtils.parameters();
	}

	private static HTTPResponse callResponse(String requestMethod, URL url, HTTPHeaders headers, HTTPContent content, String defaultResponseCharset, int socketTimeoutMillis) throws IOException
	{
		return HTTPUtils.getHTTPContent(requestMethod, url, headers, content, defaultResponseCharset, socketTimeoutMillis, HTTP_RESPONSE_READER);
	}

	private static Future<String> callString(String requestMethod, URL url, HTTPHeaders headers, HTTPContent content, String defaultResponseCharset, int socketTimeoutMillis)
	{
		return HTTP_JOBS.spawn(()->HTTPUtils.getHTTPContent(requestMethod, url, headers, content, defaultResponseCharset, socketTimeoutMillis, HTTP_STRING_READER));
	}

	// Response stream handling.
	private static void responseToMap(ScriptInstance instance, HTTPResponse response, ScriptValue value)
	{
		value.setEmptyMap(10);
		value.mapSet("charset", response.getCharset());
		value.mapSet("contenttype", response.getContentTypeHeader());
		value.mapSet("encoding", response.getEncoding());
		value.mapSet("filename", response.getFilename());
		value.mapSet("headers", response.getHeaders());
		InputStream in;
		if ((in = response.getInputStream()) != null)
		{
			DataInputStream dataIn = new DataInputStream(in);
			value.mapSet("input", dataIn);
			instance.registerCloseable(dataIn);
		}
		value.mapSet("length", response.getLength());
		value.mapSet("message", response.getStatusMessage());
		value.mapSet("status", response.getStatusCode());
		value.mapSet("type", response.getContentType());
	}

	// Response reader.
	private static HTTPReader<HTTPResponse> HTTP_RESPONSE_READER = (response)->response;
	// String reader.
	private static HTTPReader<String> HTTP_STRING_READER = HTTPReader.STRING_CONTENT_READER;
	// Thread Pool.
	private static final AsyncFactory HTTP_JOBS = new AsyncFactory(0, 10, 2, TimeUnit.SECONDS);
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
