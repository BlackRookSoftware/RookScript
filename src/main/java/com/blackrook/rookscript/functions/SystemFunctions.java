/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.functions;

import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.ScriptIteratorType.IteratorPair;
import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.hostfunction.EnumFunctionResolver;
import com.blackrook.rookscript.struct.Utils;

import static com.blackrook.rookscript.lang.ScriptFunctionUsage.type;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Script common functions for system level stuff, like process creation, control, and environment stuff.
 * @author Matthew Tropiano
 * @since 1.7.0
 */
public enum SystemFunctions implements ScriptFunctionType
{
	PROPERTIES(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a map of JVM properties. Best to read this once and re-query the returned map."
				)
				.parameter("key", 
					type(Type.NULL, "Get all properties as a map."),
					type(Type.STRING, "Get a single property as a string value."),
					type(Type.LIST, "[STRING, ...]", "Get all provided properties as a map.")
				)
				.returns(
					type(Type.NULL, "If no corresponding property, if [key] is a string."),
					type(Type.STRING, "The corresponding property, if [key] is a string."),
					type(Type.MAP, "The corresponding map of properties, if [key] is null or a list."),
					type(Type.ERROR, "Security", "If a property cannot be queried.")
				);
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (temp.isNull())
				{
					Properties props = System.getProperties();
					returnValue.setEmptyMap(props.size());
					for (Map.Entry<Object, Object> entry : props.entrySet())
						returnValue.mapSet(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
				}
				else if (temp.isString())
				{
					returnValue.set(System.getProperty(temp.asString()));
				}
				else if (temp.isList())
				{
					returnValue.setEmptyMap(temp.length());
					for (IteratorPair pair : temp)
						returnValue.mapSet(pair.getValue().asString(), System.getProperty(pair.getValue().asString()));					
				}
				else
				{
					returnValue.setEmptyMap();
				}
				return true;
			} 
			catch (SecurityException e) 
			{
				temp.setError("Security", e.getMessage());
				return true;
			}
			finally
			{
				temp.isNull();
			}
		}
	},
	
	ENVVARS(1)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Gets one or more system environment variables."
				)
				.parameter("variable", 
					type(Type.NULL, "Get all variables as a map."),
					type(Type.STRING, "Get a single variable as a string value."),
					type(Type.LIST, "[STRING, ...]", "Get all provided variables as a map.")
				)
				.returns(
					type(Type.STRING, "The corresponding value, if [variable] is a string."),
					type(Type.MAP, "The corresponding map of values, if [variable] is null or a list."),
					type(Type.ERROR, "Security", "If a value cannot be queried.")
				);
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				scriptInstance.popStackValue(temp);
				if (temp.isNull())
				{
					Map<String, String> env = System.getenv();
					returnValue.setEmptyMap(env.size());
					for (Map.Entry<String, String> entry : env.entrySet())
						returnValue.mapSet(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
				}
				else if (temp.isString())
				{
					returnValue.set(System.getenv(temp.asString()));
				}
				else if (temp.isList())
				{
					returnValue.setEmptyMap(temp.length());
					for (IteratorPair pair : temp)
						returnValue.mapSet(pair.getValue().asString(), System.getenv(pair.getValue().asString()));					
				}
				else
				{
					returnValue.setEmptyMap();
				}
				return true;
			} 
			catch (SecurityException e) 
			{
				temp.setError("Security", e.getMessage());
				return true;
			}
			finally
			{
				temp.isNull();
			}
		}
	},
	
	EXEC(7)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Spawns a process instance. A set of daemon threads are created that assist the streams on the process. " +
					"The process, if created successfully, is registered as an open resource, and is closeable like any other resource (closing it will terminate it, if running). " +
					"The script may end before the process does, so you may want to wait for its end via EXECRESULT(). " +
					"All input/output streams are not closed, unless OBJECTREF:Files are used. " +
					"If a file is used for output, it is overwritten. " +
					"If you wish to keep a process running after the script terminates, call DONOTCLOSE with the process to avoid its cleanup."
				)
				.parameter("command", 
					type(Type.STRING, "Process name or path to execute.")
				)
				.parameter("args", 
					type(Type.LIST, "[STRING, ...]", "Argument list to pass to the process.")
				)
				.parameter("env", 
					type(Type.MAP, "Map of key-value pairs for environment values.")
				)
				.parameter("workdir", 
					type(Type.NULL, "Current working directory."),
					type(Type.STRING, "Path to working directory."),
					type(Type.OBJECTREF, "File", "Path to working directory.")
				)
				.parameter("output",
					type(Type.NULL, "Consume all output."),
					type(Type.OBJECTREF, "File", "The file to write the process output to (closed on process end, native encoding)."),
					type(Type.OBJECTREF, "OutputStream", "The output stream to write the process output to (encoding is native).")
				)
				.parameter("errout", 
					type(Type.NULL, "Consume all output."),
					type(Type.OBJECTREF, "File", "The file to write the process error output to (closed on process end, native encoding)."),
					type(Type.OBJECTREF, "OutputStream", "The output stream to write the process error output to (encoding is native).")
				)
				.parameter("input", 
					type(Type.NULL, "No input, first read is end-of-stream."),
					type(Type.OBJECTREF, "File", "The file to read process input from (assumes native encoding)."),
					type(Type.OBJECTREF, "InputStream", "The input stream to read process input from (assumes native encoding).")
				)
				.returns(
					type(Type.OBJECTREF, "ProcessInstance", "An encapsulated, spawned process."),
					type(Type.ERROR, "BadParameter", "A type of a parameter is unexpected."),
					type(Type.ERROR, "BadFile", "If a stream is a file and it is an invalid file."),
					type(Type.ERROR, "Security", "If a stream is denied access or the process can't be instantiated due to OS denial."),
					type(Type.ERROR, "IOError", "If an I/O error occurs.")
				)
			;
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue stdin = CACHEVALUE1.get();
			ScriptValue stderr = CACHEVALUE2.get();
			ScriptValue stdout = CACHEVALUE3.get();
			ScriptValue work = CACHEVALUE4.get();
			ScriptValue temp = CACHEVALUE5.get();
			try
			{
				scriptInstance.popStackValue(stdin);
				scriptInstance.popStackValue(stderr);
				scriptInstance.popStackValue(stdout);
				scriptInstance.popStackValue(work);
				scriptInstance.popStackValue(temp);
				
				String[] argv;
				File workingDir;
				OutputStream out, err;
				InputStream in;
				Map<String, String> env = new HashMap<>(temp.length());
				boolean closeOut = false; 
				boolean closeErr = false;
				boolean closeIn = false;
				
				// Runtime
				if (temp.isMap())
				{
					for (IteratorPair pair : temp)
					{
						String key = pair.getKey().asString();
						String value = pair.getValue().isNull() ? null : pair.getValue().asString();
						env.put(key, value);
					}
				}

				scriptInstance.popStackValue(temp);
				if (temp.isList())
				{
					int i = 1;
					argv = new String[temp.length() + 1];
					for (IteratorPair pair : temp)
						argv[i++] = pair.getValue().asString();
				}
				else
				{
					argv = new String[1];
				}
				
				scriptInstance.popStackValue(temp);
				argv[0] = temp.asString();

				if (work.isNull())
				{
					workingDir = null;
				}
				else if (work.isObjectRef(File.class))
				{
					workingDir = work.asObjectType(File.class);
					if (!workingDir.isDirectory())
					{
						returnValue.setError("BadParameter", "Fourth parameter is not a valid directory.");
						return true;
					}
				}
				else
				{
					workingDir = new File(work.asString());
					if (!workingDir.isDirectory())
					{
						returnValue.setError("BadParameter", "Fourth parameter is not a valid directory.");
						return true;
					}
				}

				// Streams

				if (stdout.isNull())
				{
					out = Utils.NULL_OUTPUT;
				}
				else if (stdout.isObjectRef(OutputStream.class))
				{
					out = stdout.asObjectType(OutputStream.class);
				}
				else if (stdout.isObjectRef(File.class))
				{
					try {
						out = new FileOutputStream(stdout.asObjectType(File.class));
						closeOut = true;
					} catch (FileNotFoundException e) {
						returnValue.setError("BadFile", "Fifth parameter, the output stream, could not be opened.");
						return true;
					} catch (SecurityException e) {
						returnValue.setError("Security", "Fifth parameter, the output stream, could not be opened. OS is denying access.");
						return true;
					}
				}
				else
				{
					returnValue.setError("BadParameter", "Fifth parameter is not a valid output.");
					return true;
				}
				
				if (stderr.equals(stdout))
				{
					err = out;
				}
				else if (stderr.isNull())
				{
					err = Utils.NULL_OUTPUT;
				}
				else if (stderr.isObjectRef(OutputStream.class))
				{
					err = stderr.asObjectType(OutputStream.class);
				}
				else if (stderr.isObjectRef(File.class))
				{
					try {
						err = new FileOutputStream(stderr.asObjectType(File.class));
						closeErr = true;
					} catch (FileNotFoundException e) {
						returnValue.setError("BadFile", "Sixth parameter, the error stream, could not be opened.");
						return true;
					} catch (SecurityException e) {
						returnValue.setError("Security", "Sixth parameter, the error stream, could not be opened. OS is denying access.");
						return true;
					}
				}
				else
				{
					returnValue.setError("BadParameter", "Sixth parameter is not a valid output.");
					return true;
				}
				
				if (stdin.isNull())
				{
					in = Utils.NULL_INPUT;
				}
				else if (stdin.isObjectRef(InputStream.class))
				{
					in = stdin.asObjectType(InputStream.class);
				}
				else if (stdin.isObjectRef(File.class))
				{
					try {
						in = new FileInputStream(stdin.asObjectType(File.class));
						closeIn = true;
					} catch (FileNotFoundException e) {
						returnValue.setError("BadFile", "Seventh parameter, the input stream, could not be opened.");
						return true;
					} catch (SecurityException e) {
						returnValue.setError("Security", "Seventh parameter, the input stream, could not be opened. OS is denying access.");
						return true;
					}
				}
				else
				{
					returnValue.setError("BadParameter", "Seventh parameter is not a valid input.");
					return true;
				}

				try {
					ProcessInstance instance = new ProcessInstance(argv, env, workingDir, out, err, in, closeOut, closeErr, closeIn);
					scriptInstance.registerCloseable(instance);
					returnValue.set(instance);
					return true;
				} catch (SecurityException e) {
					returnValue.setError("Security", e.getMessage(), e.getLocalizedMessage());
					return true;
				} catch (IOException e) {
					returnValue.setError("IOError", e.getMessage(), e.getLocalizedMessage());
					return true;
				} 
			}
			finally
			{
				temp.setNull();
				work.setNull();
				stdin.setNull();
				stderr.setNull();
				stdout.setNull();
			}
		}
	},

	EXECRESULT(2)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Waits for a process to complete and returns its result."
				)
				.parameter("process", 
					type(Type.OBJECTREF, "ProcessInstance", "The process instance.")
				)
				.parameter("waitmillis", 
					type(Type.NULL, "Wait indefinitely."),
					type(Type.INTEGER, "The amount of time to wait for a result, in milliseconds. If 0 or less, wait forever.")
				)
				.returns(
					type(Type.INTEGER, "The process return result."),
					type(Type.ERROR, "BadParameter", "A type of a parameter is unexpected."),
					type(Type.ERROR, "Timeout", "If the wait timed out.")
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
				long wait = temp.isNull() ? 0L : temp.asLong();
				scriptInstance.popStackValue(temp);
				if (!temp.isObjectRef(ProcessInstance.class))
				{
					returnValue.setError("BadParameter", "First parameter is not a process instance.");
					return true;
				}
				
				ProcessInstance instance = temp.asObjectType(ProcessInstance.class);
				
				if (wait > 0)
				{
					if (!instance.waitTime(wait))
					{
						returnValue.setError("Timeout", "Process wait timed out after " + wait + " ms.");
						return true;
					}
				}
				
				returnValue.set(instance.get());
				return true;
			}
			finally
			{
				temp.setNull();
			}
		}
	}
	
	;
	
	private final int parameterCount;
	private Usage usage;
	private SystemFunctions(int parameterCount)
	{
		this.parameterCount = parameterCount;
		this.usage = null;
	}
	
	/**
	 * @return a function resolver that handles all of the functions in this enum.
	 */
	public static final ScriptFunctionResolver createResolver()
	{
		return new EnumFunctionResolver(SystemFunctions.values());
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
	
	private static class ProcessInstance implements AutoCloseable
	{
		private static final AtomicLong PROCESSTHREAD_ID = new AtomicLong(0L);
		
		private final Process process;
		
		private ProcessInstance(String[] argv, Map<String, String> env, File workingDir, OutputStream out, OutputStream err, InputStream in, boolean closeOut, boolean closeErr, boolean closeIn) throws IOException
		{
			long id = PROCESSTHREAD_ID.getAndIncrement();
			ProcessBuilder builder = new ProcessBuilder(argv);
			Map<String, String> envVarMap = builder.environment();
			for (Map.Entry<String, String> entry : env.entrySet())
				envVarMap.put(entry.getKey(), entry.getValue());
			builder.directory(workingDir);
			process = builder.start();
			(new PipeInToOutThread(id, "stdout", process.getInputStream(), out,                       false, closeOut)).start();
			(new PipeInToOutThread(id, "stderr", process.getErrorStream(), err,                       false, closeErr)).start();
			(new PipeInToOutThread(id, "stdin",  in,                       process.getOutputStream(), closeIn, false)).start();
		}
		
		/**
		 * Wait for and get return value.
		 * @return the return value.
		 */
		public int get()
		{
			try {
				return process.waitFor();
			} catch (InterruptedException e) {
				return 0;
			}
		}
		
		/**
		 * Attempts to destroy the process.
		 * @param force if true, force its closure.
		 */
		public void destroy(boolean force)
		{
			if (force)
				process.destroyForcibly();
			else
				process.destroy();
		}
		
		/**
		 * Wait for process completion for some time.
		 * @param millis wait milliseconds.
		 * @return true if done, false if not.
		 */
		public boolean waitTime(long millis)
		{
			try {
				return process.waitFor(millis, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				return false;
			}
		}
		
		@Override
		public void close() throws Exception 
		{
			destroy(false);
		}
	}
	
	// Piping thread from in to out.
	private static class PipeInToOutThread extends Thread
	{
		private InputStream srcIn;
		private OutputStream destOut;
		private boolean closeIn;
		private boolean closeOut;
		
		private PipeInToOutThread(long processId, String suffix, InputStream in, OutputStream out, boolean closeIn, boolean closeOut)
		{
			setDaemon(false);
			setName("RookScriptProcess-" + processId + "-" + suffix);
			this.srcIn = in;
			this.destOut = out;
			this.closeIn = closeIn;
			this.closeOut = closeOut;
		}
		
		@Override
		public void run() 
		{
			int buf = 0;
			byte[] BUFFER = new byte[8192];
			try {
				while ((buf = srcIn.read(BUFFER)) > 0)
				{
					destOut.write(BUFFER, 0, buf);
					destOut.flush();
				}
			} catch (IOException e) {
				// Eat exception.
			} finally {
				if (closeIn)
					Utils.close(srcIn);
				if (closeOut)
					Utils.close(destOut);
			}
		}
	}

	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE3 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE4 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE5 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
