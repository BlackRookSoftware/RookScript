/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under 
 * the terms of the MIT License, which accompanies this distribution.
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

/**
 * HTTP Utilities.
 * @author Matthew Tropiano
 */
public final class HTTPUtils
{
	/** HTTP Method: GET. */
	public static final String HTTP_METHOD_GET = "GET"; 
	/** HTTP Method: HEAD. */
	public static final String HTTP_METHOD_HEAD = "HEAD";
	/** HTTP Method: DELETE. */
	public static final String HTTP_METHOD_DELETE = "DELETE"; 
	/** HTTP Method: OPTIONS. */
	public static final String HTTP_METHOD_OPTIONS = "OPTIONS"; 
	/** HTTP Method: TRACE. */
	public static final String HTTP_METHOD_TRACE = "TRACE"; 
	
	/** HTTP Method: POST. */
	public static final String HTTP_METHOD_POST = "POST";
	/** HTTP Method: PUT. */
	public static final String HTTP_METHOD_PUT = "PUT";

	private HTTPUtils() {}
	
	private static final Charset UTF8 = Charset.forName("utf-8");
	private static final String[] VALID_HTTP = new String[]{"http", "https"};
	private static final byte[] URL_RESERVED = "!#$%&'()*+,/:;=?@[]".getBytes(UTF8);
	private static final byte[] URL_UNRESERVED = "-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz~".getBytes(UTF8);
	private static final ThreadLocal<SimpleDateFormat> ISO_DATE = ThreadLocal.withInitial(()->
	{
		SimpleDateFormat out = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		out.setTimeZone(TimeZone.getTimeZone("GMT"));
		return out;
	});
	
	private static class BlobContent implements HTTPContent
	{
		private String contentType;
		private String contentEncoding;
		private byte[] data;
		
		private BlobContent(String contentType, String contentEncoding, byte[] data)
		{
			this.contentType = contentType;
			this.contentEncoding = contentEncoding;
			this.data = data;
		}
		
		@Override
		public String getContentType()
		{
			return contentType;
		}
		
		@Override
		public String getCharset()
		{
			return null;
		}
		
		@Override
		public String getEncoding()
		{
			return contentEncoding;
		}
		
		@Override
		public long getLength()
		{
			return data.length;
		}
		
		@Override
		public InputStream getInputStream() throws IOException
		{
			return new ByteArrayInputStream(data);
		}
		
	}

	private static class TextContent extends BlobContent
	{
		private String contentCharset;
	
		private TextContent(String contentType, String contentCharset, String contentEncoding, byte[] data)
		{
			super(contentType, contentEncoding, data);
			this.contentCharset = contentCharset;
		}
		
		@Override
		public String getCharset()
		{
			return contentCharset;
		}
		
	}

	private static class FileContent implements HTTPContent
	{
		private String contentType;
		private String encodingType;
		private File file;
		
		private FileContent(String contentType, String encodingType, File file)
		{
			if (file == null)
				throw new IllegalArgumentException("file cannot be null.");
			if (!file.exists())
				throw new IllegalArgumentException("File " + file.getPath() + " cannot be found.");

			this.contentType = contentType;
			this.encodingType = encodingType;
			this.file = file;
		}
		
		@Override
		public String getContentType()
		{
			return contentType;
		}
	
		@Override
		public String getCharset()
		{
			return null;
		}
	
		@Override
		public String getEncoding()
		{
			return encodingType;
		}
	
		@Override
		public long getLength()
		{
			return file.length();
		}
	
		@Override
		public InputStream getInputStream() throws IOException
		{
			return new FileInputStream(file);
		}
		
	}

	private static class FormContent extends TextContent
	{
		private FormContent(Map<String, List<String>> map)
		{
			super("x-www-form-urlencoded", Charset.defaultCharset().displayName(), null, mapToParameterString(map).getBytes());
		}
	}

	/**
	 * Interface for reading an HTTPResponse from a URL call.
	 * @param <R> the return type.
	 */
	@FunctionalInterface
	public interface HTTPReader<R>
	{
		/**
		 * Called to read the HTTP response from an HTTP call.
		 * @param response the response object.
		 * @return the returned decoded object.
		 * @throws IOException if read error occurs.
		 */
		R onHTTPResponse(HTTPResponse response) throws IOException;
		
		/**
		 * An HTTP Reader that reads byte content and returns a decoded String.
		 * Gets the string contents of the response, decoded using the response's charset.
		 */
		static HTTPReader<String> STRING_CONTENT_READER = (response) ->
		{
			String charset;
			if ((charset = response.getCharset()) == null)
				throw new UnsupportedEncodingException("No charset specified.");
			
			char[] c = new char[16384];
			StringBuilder sb = new StringBuilder();
			InputStreamReader reader = new InputStreamReader(response.getInputStream(), charset);
	
			int buf = 0;
			while ((buf = reader.read(c)) >= 0) 
				sb.append(c, 0, buf);
			
			return sb.toString();
		};
	}

	/**
	 * Content body abstraction.
	 */
	public interface HTTPContent
	{
		/**
		 * @return the content MIME-type of this content.
		 */
		String getContentType();
	
		/**
		 * @return the encoded charset of this content (can be null if not text).
		 */
		String getCharset();
	
		/**
		 * @return the encoding type of this content (like GZIP or somesuch).
		 */
		String getEncoding();
	
		/**
		 * @return the length of the content in bytes.
		 */
		long getLength();
	
		/**
		 * @return an input stream for the data.
		 * @throws IOException if the stream can't be opened.
		 * @throws SecurityException if the OS forbids opening it.
		 */
		InputStream getInputStream() throws IOException;
		
	}

	/**
	 * Multipart form data.
	 */
	public static class MultipartFormContent implements HTTPContent
	{
		private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
		private static final byte[] CRLF;
		
		private static final byte[] DISPOSITION_HEADER;
		private static final byte[] DISPOSITION_NAME;
		private static final byte[] DISPOSITION_NAME_END;
		private static final byte[] DISPOSITION_FILENAME;
		private static final byte[] DISPOSITION_FILENAME_END;

		private static final byte[] TYPE_HEADER;
		private static final byte[] ENCODING_HEADER;

		private static final int BLUEPRINT_BOUNDARY_START = 0;
		private static final int BLUEPRINT_PART = 1;
		private static final int BLUEPRINT_BOUNDARY_MIDDLE = 2;
		private static final int BLUEPRINT_BOUNDARY_END = 3;

		static
		{
			CRLF = "\r\n".getBytes(UTF8);
			DISPOSITION_HEADER = "Content-Disposition: form-data".getBytes(UTF8);
			DISPOSITION_NAME = "; name=\"".getBytes(UTF8);
			DISPOSITION_NAME_END = "\"".getBytes(UTF8);
			DISPOSITION_FILENAME = "; filename=\"".getBytes(UTF8);
			DISPOSITION_FILENAME_END = "\"".getBytes(UTF8);
			TYPE_HEADER = "Content-Type: ".getBytes(UTF8);
			ENCODING_HEADER = "Content-Transfer-Encoding: ".getBytes(UTF8);
		}
		
		/** The form part first boundary. */
		private byte[] boundaryFirst;
		/** The form part middle boundary. */
		private byte[] boundaryMiddle;
		/** The form part ending boundary. */
		private byte[] boundaryEnd;
		
		/** List of Parts. */
		private List<Part> parts;
		/** Total length. */
		private long length;
		
		private MultipartFormContent() 
		{
			this.parts = new LinkedList<>();
			String boundaryText = generateBoundary();
			
			this.boundaryFirst = ("--" + boundaryText + "\r\n").getBytes(UTF8);
			this.boundaryMiddle = ("\r\n--" + boundaryText + "\r\n").getBytes(UTF8);
			this.boundaryEnd = ("\r\n--" + boundaryText + "--").getBytes(UTF8);
			
			// account for start and end boundary at least.
			this.length = boundaryFirst.length + boundaryEnd.length;
		}

		private static String generateBoundary()
		{
			Random r = new Random();
			StringBuilder sb = new StringBuilder();
			int dashes = r.nextInt(15) + 10;
			int letters = r.nextInt(24) + 16;
			while (dashes-- > 0)
				sb.append('-');
			while (letters-- > 0)
				sb.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
			return sb.toString();
		}
		
		// Adds a part and calculates change in length.
		private void addPart(final Part p)
		{
			boolean hadOtherParts = !parts.isEmpty();
			parts.add(p);
			length += p.getLength();
			if (hadOtherParts)
				length += boundaryMiddle.length;
		}
		
		/**
		 * Adds a single field to this multipart form.
		 * @param name the field name.
		 * @param value the value.
		 * @return itself, for chaining.
		 * @throws IOException if the data can't be encoded.
		 */
		public MultipartFormContent addField(String name, String value) throws IOException
		{
			return addTextPart(name, null, value);
		}
		
		/**
		 * Adds a single text part to this multipart form.
		 * @param name the field name.
		 * @param mimeType the mimeType of the text part.
		 * @param text the text data.
		 * @return itself, for chaining.
		 * @throws IOException if the data can't be encoded.
		 */
		public MultipartFormContent addTextPart(String name, final String mimeType, final String text) throws IOException
		{
			return addDataPart(name, mimeType, null, null, text.getBytes(UTF8));
		}
		
		/**
		 * Adds a file part to this multipart form.
		 * The name of the file is passed along.
		 * @param name the field name.
		 * @param mimeType the mimeType of the file part.
		 * @param data the file data.
		 * @return itself, for chaining.
		 * @throws IllegalArgumentException if data is null or the file cannot be found.
		 */
		public MultipartFormContent addFilePart(String name, String mimeType, final File data)
		{
			return addFilePart(name, mimeType, data.getName(), data);
		}
		
		/**
		 * Adds a file part to this multipart form.
		 * @param name the field name.
		 * @param mimeType the mimeType of the file part.
		 * @param fileName the file name to send (overridden).
		 * @param data the file data.
		 * @return itself, for chaining.
		 * @throws IllegalArgumentException if data is null or the file cannot be found.
		 */
		public MultipartFormContent addFilePart(String name, final String mimeType, final String fileName, final File data)
		{
			if (data == null)
				throw new IllegalArgumentException("data cannot be null.");
			if (!data.exists())
				throw new IllegalArgumentException("File " + data.getPath() + " cannot be found.");
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
			try {
				// Content Disposition Line
				bos.write(DISPOSITION_HEADER);
				bos.write(DISPOSITION_NAME);
				bos.write(name.getBytes(UTF8));
				bos.write(DISPOSITION_NAME_END);
				bos.write(DISPOSITION_FILENAME);
				bos.write(fileName.getBytes(UTF8));
				bos.write(DISPOSITION_FILENAME_END);
				bos.write(CRLF);

				// Content Type Line
				bos.write(TYPE_HEADER);
				bos.write(mimeType.getBytes(UTF8));
				bos.write(CRLF);

				// Blank line for header end.
				bos.write(CRLF);
				
				// ... data follows here.
			} catch (IOException e) {
				// should never happen.
				throw new RuntimeException(e);
			}

			final byte[] headerBytes = bos.toByteArray();
			
			addPart(new Part(headerBytes, new PartData() 
			{
				@Override
				public long getDataLength() 
				{
					return data.length();
				}
	
				@Override
				public InputStream getInputStream() throws IOException
				{
					return new FileInputStream(data);
				}
			}));
			return this;
		}
		
		/**
		 * Adds a byte data part to this multipart form.
		 * @param name the field name.
		 * @param mimeType the mimeType of the file part.
		 * @param dataIn the input data.
		 * @return itself, for chaining.
		 */
		public MultipartFormContent addDataPart(String name, String mimeType, byte[] dataIn)
		{
			return addDataPart(name, mimeType, null, null, dataIn);
		}
	
		/**
		 * Adds a byte data part to this multipart form as though it came from a file.
		 * @param name the field name.
		 * @param mimeType the mimeType of the file part.
		 * @param fileName the name of the file, as though this were originating from a file (can be null, for "no file").
		 * @param dataIn the input data.
		 * @return itself, for chaining.
		 */
		public MultipartFormContent addDataPart(String name, String mimeType, String fileName, byte[] dataIn)
		{
			return addDataPart(name, mimeType, null, fileName, dataIn);
		}
		
		/**
		 * Adds a byte data part (translated as text) to this multipart form as though it came from a file.
		 * @param name the field name.
		 * @param mimeType the mimeType of the file part.
		 * @param encoding the encoding type name for the data sent, like 'base64' or 'gzip' or somesuch (can be null to signal no encoding type).
		 * @param fileName the name of the file, as though this were originating from a file (can be null, for "no file").
		 * @param dataIn the input data.
		 * @return itself, for chaining.
		 */
		public MultipartFormContent addDataPart(String name, final String mimeType, final String encoding, final String fileName, final byte[] dataIn)
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
			try {
				// Content Disposition Line
				bos.write(DISPOSITION_HEADER);
				bos.write(DISPOSITION_NAME);
				bos.write(name.getBytes(UTF8));
				bos.write(DISPOSITION_NAME_END);
				if (fileName != null)
				{
					bos.write(DISPOSITION_FILENAME);
					bos.write(fileName.getBytes(UTF8));
					bos.write(DISPOSITION_FILENAME_END);
				}
				bos.write(CRLF);

				// Content Type Line
				if (mimeType != null)
				{
					bos.write(TYPE_HEADER);
					bos.write(mimeType.getBytes(UTF8));
					bos.write(CRLF);
				}

				// Content transfer encoding
				if (encoding != null)
				{
					bos.write(ENCODING_HEADER);
					bos.write(encoding.getBytes(UTF8));
					bos.write(CRLF);
				}
				
				// Blank line for header end.
				bos.write(CRLF);
				
				// ... data follows here.
			} catch (IOException e) {
				// should never happen.
				throw new RuntimeException(e);
			}

			final byte[] headerBytes = bos.toByteArray();
			
			addPart(new Part(headerBytes, new PartData() 
			{
				@Override
				public long getDataLength() 
				{
					return dataIn.length;
				}
	
				@Override
				public InputStream getInputStream()
				{
					return new ByteArrayInputStream(dataIn);
				}
			}));
			return this;
		}

		@Override
		public String getContentType()
		{
			return "multipart/form-data";
		}

		@Override
		public String getCharset()
		{
			return "utf-8";
		}

		@Override
		public String getEncoding()
		{
			return null;
		}

		@Override
		public long getLength()
		{
			return length;
		}

		@Override
		public InputStream getInputStream() throws IOException
		{
			return new MultiformInputStream();
		}
		
		/**
		 * Part data.
		 */
		private interface PartData
		{
			/**
			 * @return the content length of this part in bytes.
			 */
			long getDataLength();
			
			/**
			 * @return an open input stream to read from this part.
			 * @throws IOException if an I/O error occurs on read.
			 */
			InputStream getInputStream() throws IOException;
		}
		
		/**
		 * A single form part.
		 */
		private static class Part
		{
			private byte[] headerbytes;
			private PartData data;
			
			private Part(final byte[] headerbytes, final PartData data)
			{
				this.headerbytes = headerbytes;
				this.data = data;
			}
			
			/**
			 * @return the boundary-plus-header bytes that make up the start of this part.
			 */
			public byte[] getPartHeaderBytes()
			{
				return headerbytes;
			}

			/**
			 * @return the full length of this part, plus headers, in bytes.
			 */
			public long getLength()
			{
				return getPartHeaderBytes().length + data.getDataLength();				
			}

			/**
			 * @return an open input stream for reading from this form.
			 * @throws IOException if an input stream could not be opened.
			 */
			public InputStream getInputStream() throws IOException
			{
				return new PartInputStream();				
			}

			private class PartInputStream extends InputStream
			{
				private boolean readHeader;
				private InputStream currentStream;
				
				private PartInputStream()
				{
					this.readHeader = false;
					this.currentStream = new ByteArrayInputStream(headerbytes);
				}
				
				@Override
				public int read() throws IOException
				{
					if (currentStream == null)
						return -1;
					
					int out;
					if ((out = currentStream.read()) < 0)
					{
						currentStream.close();
						if (!readHeader)
						{
							currentStream = data.getInputStream();
							readHeader = true;
						}
						else
							currentStream = null;
						return read();
					}
					else
						return out;
				}
				
				@Override
				public void close() throws IOException
				{
					if (currentStream != null)
					{
						currentStream.close();
						currentStream = null;
					}
					super.close();
				}
			}
		}

		private class MultiformInputStream extends InputStream
		{	
			private int[] blueprint;
			private int currentBlueprint;
			private Iterator<Part> streamIterator;
			private Part currentPart;
			private InputStream currentStream;
			
			private MultiformInputStream() throws IOException
			{
				this.streamIterator = parts.iterator();
				this.currentBlueprint = 0;
				this.currentPart = null;
				this.currentStream = null;
				this.blueprint = new int[parts.isEmpty() ? 0 : parts.size() * 2 + 1];
				
				if (blueprint.length > 0)
				{
					this.blueprint[0] = BLUEPRINT_BOUNDARY_START;
					for (int i = 1; i < blueprint.length; i += 2)
					{
						this.blueprint[i] = BLUEPRINT_PART;
						this.blueprint[i + 1] = i + 1 < blueprint.length - 1 ? BLUEPRINT_BOUNDARY_MIDDLE : BLUEPRINT_BOUNDARY_END;
					}
				}
				nextStream();
			}
		
			private void nextStream() throws IOException
			{
				if (currentBlueprint >= blueprint.length)
				{
					currentPart = null;
					currentStream = null;
				}
				else switch (blueprint[currentBlueprint++])
				{
					case BLUEPRINT_BOUNDARY_START:
						currentStream = new ByteArrayInputStream(boundaryFirst);
						break;
					case BLUEPRINT_PART:
						currentPart = streamIterator.hasNext() ? streamIterator.next() : null;
						currentStream = currentPart != null ? currentPart.getInputStream() : null;
						break;
					case BLUEPRINT_BOUNDARY_MIDDLE:
						currentStream = new ByteArrayInputStream(boundaryMiddle);
						break;
					case BLUEPRINT_BOUNDARY_END:
						currentStream = new ByteArrayInputStream(boundaryEnd);
						break;
				}
			}
			
			@Override
			public int read() throws IOException
			{
				if (currentStream == null)
					return -1;
				
				int out;
				if ((out = currentStream.read()) < 0)
				{
					nextStream();
					return read();
				}
				
				return out;
			}
			
			@Override
			public void close() throws IOException
			{
				if (currentStream != null)
					currentStream.close();
				
				currentStream = null;
				currentPart = null;
				streamIterator = null;
				super.close();
			}
		}
	}

	/**
	 * HTTP headers object.
	 */
	public static class HTTPHeaders
	{
		Map<String, String> map;
		
		private HTTPHeaders()
		{
			this.map = new HashMap<>(); 
		}

		/**
		 * Copies this header object.
		 * @return a copy of this header.
		 */
		public HTTPHeaders copy()
		{
			HTTPHeaders out = new HTTPHeaders();
			for (Map.Entry<String, String> entry : map.entrySet())
				out.setHeader(entry.getKey(), entry.getValue());
			return out;
		}

		/**
		 * Sets a header.
		 * @param header the header name.
		 * @param value the header value.
		 * @return this, for chaining.
		 */
		public HTTPHeaders setHeader(String header, String value)
		{
			map.put(header, value);
			return this;
		}

	}
	
	/**
	 * HTTP Parameters object.
	 */
	public static class HTTPParameters
	{
		Map<String, List<String>> map;
		
		private HTTPParameters()
		{
			this.map = new HashMap<>(); 
		}

		/**
		 * Copies this parameters object.
		 * @return a copy of this parameter object.
		 */
		public HTTPParameters copy()
		{
			HTTPParameters out = new HTTPParameters();
			for (Map.Entry<String, List<String>> entry : map.entrySet())
				for (String value : entry.getValue())
					out.addParameter(entry.getKey(), value);
			return out;
		}

		/**
		 * Adds/creates a parameter.
		 * @param key the parameter name.
		 * @param value the parameter value.
		 * @return this, for chaining.
		 */
		public HTTPParameters addParameter(String key, Object value)
		{
			List<String> list;
			if ((list = map.get(key)) == null)
				map.put(key, (list = new LinkedList<>()));
			list.add(String.valueOf(value));
			return this;
		}

		/**
		 * Sets/resets a parameter and its values.
		 * If the parameter is already set, it is replaced.
		 * @param key the parameter name.
		 * @param value the parameter value.
		 * @return this, for chaining.
		 */
		public HTTPParameters setParameter(String key, Object value)
		{
			List<String> list;
			map.put(key, (list = new LinkedList<>()));
			list.add(String.valueOf(value));
			return this;
		}

		/**
		 * Sets/resets a parameter and its values.
		 * If the parameter is already set, it is replaced.
		 * @param key the parameter name.
		 * @param values the parameter values.
		 * @return this, for chaining.
		 */
		public HTTPParameters setParameter(String key, Object... values)
		{
			List<String> list;
			map.put(key, (list = new LinkedList<>()));
			for (Object v : values)
				list.add(String.valueOf(v));
			return this;
		}

	}
	
	/**
	 * Response from an HTTP call.
	 */
	public static class HTTPResponse
	{
		private Map<String, List<String>> headers;
		private int statusCode;
		private String statusMessage;
		private int length;
		private InputStream input;
		private String charset;
		private String contentType;
		private String contentTypeHeader;
		private String encoding;
		private String contentDisposition;
		private String filename;
	
		private HTTPResponse() {}
		
		/**
		 * @return the headers on the response.
		 */
		public Map<String, List<String>> getHeaders()
		{
			return headers;
		}
		
		/**
		 * @return the response status code.
		 */
		public int getStatusCode()
		{
			return statusCode;
		}
	
		/**
		 * @return the response status message.
		 */
		public String getStatusMessage()
		{
			return statusMessage;
		}
	
		/**
		 * @return the response's content length.
		 */
		public int getLength() 
		{
			return length;
		}
	
		/**
		 * @return an open input stream for reading the response's content.
		 */
		public InputStream getInputStream() 
		{
			return input;
		}
		
		/**
		 * @return the response's charset. can be null.
		 */
		public String getCharset()
		{
			return charset;
		}
	
		/**
		 * @return the response's content type. can be null.
		 */
		public String getContentType()
		{
			return contentType;
		}
	
		/**
		 * @return the response's content type (full unparsed header). can be null.
		 */
		public String getContentTypeHeader()
		{
			return contentTypeHeader;
		}
	
		/**
		 * @return the response's encoding. can be null.
		 */
		public String getEncoding()
		{
			return encoding;
		}
	
		/**
		 * @return the content disposition. if present, usually "attachment". Can be null.
		 */
		public String getContentDisposition() 
		{
			return contentDisposition;
		}

		/**
		 * @return the content filename. Set if content disposition is "attachment". Can be null.
		 */
		public String getFilename() 
		{
			return filename;
		}
	}

	/**
	 * Makes an HTTP-acceptable ISO date string from a Date.
	 * @param date the date to format.
	 * @return the resultant string.
	 */
	public static String date(Date date)
	{
		return ISO_DATE.get().format(date);
	}
	
	/**
	 * Makes a comma-space-separated list of values.
	 * @param values the values to join together.
	 * @return the resultant string.
	 */
	public static String list(String ... values)
	{
		return join(", ", values);
	}
	
	/**
	 * Joins a list of values into one string, placing a joiner between all of them.
	 * @param joiner the joining string.
	 * @param values the values to join together.
	 * @return the resultant string.
	 */
	public static String join(String joiner, String ... values)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++)
		{
			sb.append(values[i]);
			if (i < values.length - 1)
				sb.append(joiner);
		}
		return sb.toString();
	}
	
	/**
	 * Makes a "Value; Parameter" string.
	 * @param value the value.
	 * @param parameter the value parameter.
	 * @return a string that is equivalent to: <code>String.valueOf(value) + "; " + String.valueOf(parameter)</code>.
	 */
	public static String valueParam(String value, String parameter)
	{
		return String.valueOf(value) + "; " + String.valueOf(parameter);
	}
	
	/**
	 * Makes a "Key=Value" string.
	 * @param key the key.
	 * @param value the value.
	 * @return a string that is equivalent to: <code>String.valueOf(key) + '=' + String.valueOf(value)</code>.
	 */
	public static String keyValue(String key, String value)
	{
		return String.valueOf(key) + '=' + String.valueOf(value);
	}
	
	/**
	 * Starts a new {@link HTTPHeaders} object.
	 * @return a new header object.
	 */
	public static HTTPHeaders headers()
	{
		return new HTTPHeaders();
	}
	
	/**
	 * Starts a new {@link HTTPParameters} object.
	 * @return a new parameters object.
	 */
	public static HTTPParameters parameters()
	{
		return new HTTPParameters();
	}
	
	/**
	 * Creates a text blob content body for an HTTP request.
	 * @param contentType the data's content type.
	 * @param text the text data.
	 * @return a content object representing the content.
	 */
	public static HTTPContent createTextContent(String contentType, String text)
	{
		try {
			return new TextContent(contentType, "utf-8", null, text.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("JVM does not support the UTF-8 charset [INTERNAL ERROR].");
		}
	}

	/**
	 * Creates a byte blob content body for an HTTP request.
	 * @param contentType the data's content type.
	 * @param bytes the byte data.
	 * @return a content object representing the content.
	 */
	public static HTTPContent createByteContent(String contentType, byte[] bytes)
	{
		return new BlobContent(contentType, null, bytes);
	}

	/**
	 * Creates a byte blob content body for an HTTP request.
	 * @param contentType the data's content type.
	 * @param contentEncoding the data's encoding type (like gzip or what have you, can be null for none).
	 * @param bytes the byte data.
	 * @return a content object representing the content.
	 */
	public static HTTPContent createByteContent(String contentType, String contentEncoding, byte[] bytes)
	{
		return new BlobContent(contentType, contentEncoding, bytes);
	}

	/**
	 * Creates a file-based content body for an HTTP request.
	 * <p>Note: This is NOT form-data content! See {@link MultipartFormContent} for that.
	 * @param contentType the file's content type.
	 * @param file the file to read from.
	 * @return a content object representing the content.
	 */
	public static HTTPContent createFileContent(String contentType, File file)
	{
		return new FileContent(contentType, null, file);
	}

	/**
	 * Creates a file-based content body for an HTTP request.
	 * <p>Note: This is NOT form-data content! See {@link MultipartFormContent} for that.
	 * @param contentType the file's content type.
	 * @param encodingType the data encoding type for the file's payload (e.g. "gzip" or "base64").
	 * @param file the file to read from.
	 * @return a content object representing the content.
	 */
	public static HTTPContent createFileContent(String contentType, String encodingType, File file)
	{
		return new FileContent(contentType, encodingType, file);
	}

	/**
	 * Creates a WWW form, URL encoded content body for an HTTP request.
	 * <p>Note: This is NOT mulitpart form-data content! 
	 * See {@link MultipartFormContent} for mixed file attachments and fields.
	 * @param keyValueMap the map of key to value.
	 * @return a content object representing the content.
	 */
	public static HTTPContent createFormContent(HTTPParameters keyValueMap)
	{
		return new FormContent(keyValueMap.map);
	}

	/**
	 * Creates a WWW form, URL encoded content body for an HTTP request.
	 * <p>Note: This is NOT mulitpart form-data content! 
	 * See {@link MultipartFormContent} for mixed file attachments and fields.
	 * @return a content object representing the content.
	 */
	public static MultipartFormContent createMultipartContent()
	{
		return new MultipartFormContent();
	}

	/**
	 * Sends a GET request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 */
	public static <R> R httpGet(String url, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpGet(url, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a GET request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param parameters the mapping of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPParameters
	 */
	public static <R> R httpGet(String url, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpGet(url, null, parameters, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a GET request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 */
	public static <R> R httpGet(String url, HTTPHeaders headers, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpGet(url, headers, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a GET request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param parameters the map of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 * @see HTTPParameters
	 */
	public static <R> R httpGet(String url, HTTPHeaders headers, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return getHTTPContent(HTTP_METHOD_GET, new URL(urlParams(url, parameters)), headers, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a HEAD request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 */
	public static <R> R httpHead(String url, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpHead(url, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a HEAD request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param parameters the map of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPParameters
	 */
	public static <R> R httpHead(String url, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpHead(url, null, parameters, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a HEAD request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 */
	public static <R> R httpHead(String url, HTTPHeaders headers, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpHead(url, headers, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a HEAD request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param parameters the map of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 * @see HTTPParameters
	 */
	public static <R> R httpHead(String url, HTTPHeaders headers, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return getHTTPContent(HTTP_METHOD_HEAD, new URL(urlParams(url, parameters)), headers, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a DELETE request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 */
	public static <R> R httpDelete(String url, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpDelete(url, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a DELETE request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param parameters the map of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPParameters
	 */
	public static <R> R httpDelete(String url, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpDelete(url, null, parameters, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a DELETE request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 */
	public static <R> R httpDelete(String url, HTTPHeaders headers, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpDelete(url, headers, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a DELETE request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param parameters the map of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 * @see HTTPParameters
	 */
	public static <R> R httpDelete(String url, HTTPHeaders headers, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return getHTTPContent(HTTP_METHOD_DELETE, new URL(urlParams(url, parameters)), headers, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends an OPTIONS request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 */
	public static <R> R httpOptions(String url, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpOptions(url, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends an OPTIONS request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param parameters the map of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPParameters
	 */
	public static <R> R httpOptions(String url, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpOptions(url, null, parameters, socketTimeoutMillis, reader);
	}

	/**
	 * Sends an OPTIONS request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 */
	public static <R> R httpOptions(String url, HTTPHeaders headers, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpOptions(url, headers, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends an OPTIONS request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param parameters the map of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 * @see HTTPParameters
	 */
	public static <R> R httpOptions(String url, HTTPHeaders headers, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return getHTTPContent(HTTP_METHOD_OPTIONS, new URL(urlParams(url, parameters)), headers, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends an TRACE request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 */
	public static <R> R httpTrace(String url, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpTrace(url, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends an TRACE request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param parameters the map of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPParameters
	 */
	public static <R> R httpTrace(String url, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpTrace(url, null, parameters, socketTimeoutMillis, reader);
	}

	/**
	 * Sends an TRACE request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 */
	public static <R> R httpTrace(String url, HTTPHeaders headers, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpTrace(url, headers, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends an TRACE request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param parameters the map of key to values representing parameters to append as a query string to the URL (can be null).
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 * @see HTTPParameters
	 */
	public static <R> R httpTrace(String url, HTTPHeaders headers, HTTPParameters parameters, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return getHTTPContent(HTTP_METHOD_TRACE, new URL(urlParams(url, parameters)), headers, null, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a PUT request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param content if not null, add this content to the body. Otherwise, the body will be empty.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see #createByteContent(String, byte[])
	 * @see #createByteContent(String, String, byte[])
	 * @see #createTextContent(String, String)
	 * @see #createFileContent(String, File)
	 * @see #createFileContent(String, String, File)
	 * @see #createFormContent(HTTPParameters)
	 * @see #createMultipartContent()
	 */
	public static <R> R httpPut(String url, HTTPContent content, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpPut(url, null, content, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a PUT request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param content if not null, add this content to the body. Otherwise, the body will be empty.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 * @see #createByteContent(String, byte[])
	 * @see #createByteContent(String, String, byte[])
	 * @see #createTextContent(String, String)
	 * @see #createFileContent(String, File)
	 * @see #createFileContent(String, String, File)
	 * @see #createFormContent(HTTPParameters)
	 * @see #createMultipartContent()
	 */
	public static <R> R httpPut(String url, HTTPHeaders headers, HTTPContent content, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return getHTTPContent(HTTP_METHOD_PUT, new URL(url), headers, content, null, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a POST request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param content if not null, add this content to the body. Otherwise, the body will be empty.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see #createByteContent(String, byte[])
	 * @see #createByteContent(String, String, byte[])
	 * @see #createTextContent(String, String)
	 * @see #createFileContent(String, File)
	 * @see #createFileContent(String, String, File)
	 * @see #createFormContent(HTTPParameters)
	 * @see #createMultipartContent()
	 */
	public static <R> R httpPost(String url, HTTPContent content, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return httpPost(url, null, content, socketTimeoutMillis, reader);
	}

	/**
	 * Sends a POST request to an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param content if not null, add this content to the body. Otherwise, the body will be empty.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the content from opening an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @see HTTPHeaders
	 * @see #createByteContent(String, byte[])
	 * @see #createByteContent(String, String, byte[])
	 * @see #createTextContent(String, String)
	 * @see #createFileContent(String, File)
	 * @see #createFileContent(String, String, File)
	 * @see #createFormContent(HTTPParameters)
	 * @see #createMultipartContent()
	 */
	public static <R> R httpPost(String url, HTTPHeaders headers, HTTPContent content, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		return getHTTPContent(HTTP_METHOD_POST, new URL(url), headers, content, null, socketTimeoutMillis, reader);
	}

	/**
	 * Gets the content from a opening an HTTP URL.
	 * The connection is closed afterward.
	 * @param <R> the return type.
	 * @param requestMethod the request method.
	 * @param url the URL to open and read.
	 * @param headers a map of header to header value to add to the request (can be null for no headers).
	 * @param content if not null, add this content to the body.
	 * @param defaultResponseCharset if the response charset is not specified, use this one.
	 * @param socketTimeoutMillis the socket timeout time in milliseconds. 0 is forever.
	 * @param reader the reader to use to read the response and return the data in a useful shape.
	 * @return the read content from an HTTP request.
	 * @throws IOException if an error happens during the read/write.
	 * @throws SocketTimeoutException if the socket read times out.
	 * @throws ProtocolException if the requestMethod is incorrect, or not an HTTP URL.
	 * @see HTTPHeaders
	 * @see #createByteContent(String, byte[])
	 * @see #createByteContent(String, String, byte[])
	 * @see #createTextContent(String, String)
	 * @see #createFileContent(String, File)
	 * @see #createFileContent(String, String, File)
	 * @see #createFormContent(HTTPParameters)
	 * @see #createMultipartContent()
	 */
	public static <R> R getHTTPContent(String requestMethod, URL url, HTTPHeaders headers, HTTPContent content, String defaultResponseCharset, int socketTimeoutMillis, HTTPReader<R> reader) throws IOException
	{
		if (Arrays.binarySearch(VALID_HTTP, url.getProtocol()) < 0)
			throw new ProtocolException("This is not an HTTP URL.");
	
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setReadTimeout(socketTimeoutMillis);
		conn.setRequestMethod(requestMethod);
		
		if (headers != null) for (Map.Entry<String, String> entry : headers.map.entrySet())
			conn.setRequestProperty(entry.getKey(), entry.getValue());
	
		// set up body data.
		if (content != null)
		{
			conn.setFixedLengthStreamingMode(content.getLength());
			conn.setRequestProperty("Content-Type", content.getContentType() == null ? "application/octet-stream" : content.getContentType());
			if (content.getEncoding() != null)
				conn.setRequestProperty("Content-Encoding", content.getEncoding());
			conn.setDoOutput(true);
			try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream()))
			{
				relay(content.getInputStream(), dos, 8192);
			}
		}
	
		HTTPResponse response = new HTTPResponse();
		response.statusCode = conn.getResponseCode();
		response.statusMessage = conn.getResponseMessage();
		response.length = conn.getContentLength();
		response.encoding = conn.getContentEncoding();
		response.contentTypeHeader = conn.getContentType();

		int mimeEnd = response.contentTypeHeader.indexOf(';');
		
		response.contentType = response.contentTypeHeader.substring(0, mimeEnd >= 0 ? mimeEnd : response.contentTypeHeader.length()).trim();
		
		int charsetindex;
		if ((charsetindex = response.contentTypeHeader.toLowerCase().indexOf("charset=")) >= 0)
		{
			int endIndex = response.contentTypeHeader.indexOf(";", charsetindex);
			if (endIndex >= 0)
				response.charset = response.contentTypeHeader.substring(charsetindex + "charset=".length(), endIndex).trim();
			else
				response.charset = response.contentTypeHeader.substring(charsetindex + "charset=".length()).trim();
			if (response.charset.startsWith("\"")) // remove surrounding quotes
				response.charset = response.charset.substring(1, response.charset.length() - 1); 
		}
		
		if (response.charset == null)
			response.charset = defaultResponseCharset;
		
		response.headers = conn.getHeaderFields();
		
		// content disposition?
		if ((response.contentDisposition = conn.getHeaderField("content-disposition")) != null)
		{
			int fileNameIndex;
			if ((fileNameIndex = response.contentDisposition.toLowerCase().indexOf("filename=")) >= 0)
			{
				int endIndex = response.contentDisposition.indexOf(";", fileNameIndex);
				if (endIndex >= 0)
					response.filename = response.contentDisposition.substring(fileNameIndex + "filename=".length(), endIndex).trim();
				else
					response.filename = response.contentDisposition.substring(fileNameIndex + "filename=".length()).trim();
				if (response.filename.startsWith("\"")) // remove surrounding quotes
					response.filename = response.filename.substring(1, response.filename.length() - 1); 
			}
		}
		
		response.input = new BufferedInputStream(conn.getInputStream());
		R out = reader.onHTTPResponse(response);
		response.input.close();
		return out;
	}
	
	private static final char[] HEX_NYBBLE = "0123456789ABCDEF".toCharArray();

	private static void writePercentChar(StringBuilder target, byte b)
	{
		target.append('%');
		target.append(HEX_NYBBLE[(b & 0x0f0) >> 4]);
		target.append(HEX_NYBBLE[b & 0x00f]);
	}
	
	private static String toURLEncoding(String s)
	{
		StringBuilder sb = new StringBuilder();
		byte[] bytes = s.getBytes(UTF8);
		for (int i = 0; i < s.length(); i++)
		{
			byte b = bytes[i];
			if (Arrays.binarySearch(URL_UNRESERVED, b) >= 0)
				sb.append((char)b);
			else if (Arrays.binarySearch(URL_RESERVED, b) >= 0)
				writePercentChar(sb, b);
			else
				writePercentChar(sb, b);
		}
		return sb.toString();
	}
	
	private static String mapToParameterString(Map<String, List<String>> map)
	{
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<String>> entry : map.entrySet())
		{
			String key = entry.getKey();
			for (String value : entry.getValue())
			{
				if (sb.length() > 0)
					sb.append('&');
				sb.append(toURLEncoding(key));
				sb.append('=');
				sb.append(toURLEncoding(value));
			}
		}
		return sb.toString();
	}
	
	private static String urlParams(String url, HTTPParameters parameters)
	{
		String param = parameters != null ? mapToParameterString(parameters.map) : null;
		
		if (param != null && !param.isEmpty())
			url = url + (url.indexOf('?') >= 0 ? '&' : '?') + param;
		return url;
	}

	/**
	 * Reads from an input stream, reading in a consistent set of data
	 * and writing it to the output stream. The read/write is buffered
	 * so that it does not bog down the OS's other I/O requests.
	 * This method finishes when the end of the source stream is reached.
	 * Note that this may block if the input stream is a type of stream
	 * that will block if the input stream blocks for additional input.
	 * This method is thread-safe.
	 * @param in the input stream to grab data from.
	 * @param out the output stream to write the data to.
	 * @param bufferSize the buffer size for the I/O. Must be &gt; 0.
	 * @return the total amount of bytes relayed.
	 * @throws IOException if a read or write error occurs.
	 */
	private static int relay(InputStream in, OutputStream out, int bufferSize) throws IOException
	{
		return relay(in, out, bufferSize, -1);
	}

	/**
	 * Reads from an input stream, reading in a consistent set of data
	 * and writing it to the output stream. The read/write is buffered
	 * so that it does not bog down the OS's other I/O requests.
	 * This method finishes when the end of the source stream is reached.
	 * Note that this may block if the input stream is a type of stream
	 * that will block if the input stream blocks for additional input.
	 * This method is thread-safe.
	 * @param in the input stream to grab data from.
	 * @param out the output stream to write the data to.
	 * @param bufferSize the buffer size for the I/O. Must be &gt; 0.
	 * @param maxLength the maximum amount of bytes to relay, or a value &lt; 0 for no max.
	 * @return the total amount of bytes relayed.
	 * @throws IOException if a read or write error occurs.
	 */
	private static int relay(InputStream in, OutputStream out, int bufferSize, int maxLength) throws IOException
	{
		int total = 0;
		int buf = 0;
			
		byte[] RELAY_BUFFER = new byte[bufferSize];
		
		while ((buf = in.read(RELAY_BUFFER, 0, Math.min(maxLength < 0 ? Integer.MAX_VALUE : maxLength, bufferSize))) > 0)
		{
			out.write(RELAY_BUFFER, 0, buf);
			total += buf;
			if (maxLength >= 0)
				maxLength -= buf;
		}
		return total;
	}
	
}
