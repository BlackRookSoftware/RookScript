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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Script common functions for system level stuff, like process creation, control, and environment stuff.
 * @author Matthew Tropiano
 * @since [NOW]
 */
public enum SystemFunctions implements ScriptFunctionType
{
	OS(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a map of Operating System information."
				)
				.returns(
					type(Type.MAP, "A map of values for OS values.")
				);
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				// TODO: Finish this.
				return true;
			}
			finally
			{
				temp.isNull();
			}
		}
	},
	
	JAVA(0)
	{
		@Override
		protected Usage usage()
		{
			return ScriptFunctionUsage.create()
				.instructions(
					"Returns a map of JVM information."
				)
				.returns(
					type(Type.MAP, "A map of values for Java properties.")
				);
		}
		
		@Override
		public boolean execute(ScriptInstance scriptInstance, ScriptValue returnValue)
		{
			ScriptValue temp = CACHEVALUE1.get();
			try
			{
				// TODO: Finish this.
				return true;
			}
			finally
			{
				temp.isNull();
			}
		}
	},
	
	ENV(1)
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
					type(Type.LIST, "Get all provided variables as a map.")
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
				// TODO: Finish this.
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
					"The process, if created successfully, is registered as an open resource, and is closeable like any other resource. " +
					"The script may end before the process does, so you may want to wait for its end via PROCESSRESULT()."
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
				
				String[] argv;
				String[] env;
				File workingDir;
				OutputStream out;
				boolean closeOut = false;
				OutputStream err;
				boolean closeErr = false;
				InputStream in;
				
				// Runtime

				scriptInstance.popStackValue(temp);
				if (temp.isMap())
				{
					int i = 0;
					env = new String[temp.length()];
					for (IteratorPair pair : temp)
						env[i++] = pair.getKey().asString() + "=" + pair.getValue().asString();
				}
				else
				{
					env = NO_STRINGS;
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
				
				if (stderr.isNull())
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
					returnValue.set(new ProcessInstance(argv, env, workingDir, out, closeOut, err, closeErr, in));
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
				.parameter("command", 
					type(Type.OBJECTREF, "File", "The file to read process input from (assumes native encoding).")
				)
				.parameter("waitmillis", 
					type(Type.NULL, "Wait indefinitely."),
					type(Type.INTEGER, "The amount of time to wait for a result, in milliseconds.")
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
				long wait = temp.isNull() ? -1 : temp.asLong();
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
		private final InputStream inPipe;
		private Thread stdOutThread;
		private Thread stdErrThread;
		private Thread stdInThread;
		
		private ProcessInstance(String[] argv, String[] env, File workingDir, final OutputStream out, final boolean closeOut, final OutputStream err, final boolean closeErr, final InputStream in) throws IOException
		{
			long id = PROCESSTHREAD_ID.getAndIncrement();
			process = Runtime.getRuntime().exec(argv, env, workingDir);
			inPipe = in;
			
			(stdOutThread = new ProcessStreamThread(id, "Out", ()->{
				try {
					int buf;
					byte[] buffer = new byte[4096];
					InputStream stdOut = process.getInputStream();
					while ((buf = stdOut.read(buffer)) > 0)
					{
						out.write(buffer, 0, buf);
						out.flush();
					}
				} catch (IOException e) {
					// Eat exception.
				} finally {
					if (closeOut)
						Utils.close(out);
				}
			})).start();
			
			(stdErrThread = new ProcessStreamThread(id, "Err", ()->{
				try {
					int buf;
					byte[] buffer = new byte[4096];
					InputStream stdErr = process.getErrorStream();
					while ((buf = stdErr.read(buffer)) > 0)
					{
						err.write(buffer, 0, buf);
						err.flush();
					}
				} catch (IOException e) {
					// Eat exception.
				} finally {
					if (closeErr)
						Utils.close(err);
				} 
			})).start();
			
			(stdInThread = new ProcessStreamThread(id, "In", ()->{
				try (OutputStream stdIn = process.getOutputStream()) 
				{
					int buf;
					byte[] buffer = new byte[4096];
					while ((buf = inPipe.read(buffer)) > 0)
					{
						stdIn.write(buffer, 0, buf);
						stdIn.flush();
					}
				} catch (IOException e) {
					// Eat exception.
				} finally {
					Utils.close(inPipe);
				}
			})).start();
		}
		
		/**
		 * Wait for and get return value.
		 * @return the return value.
		 */
		public int get()
		{
			try {
				int out = process.waitFor();
				cleanup();
				return out;
			} catch (InterruptedException e) {
				return 0;
			}
		}
		
		/**
		 * Wait for process completion for some time.
		 * @param millis wait milliseconds.
		 * @return true if done, false if not.
		 */
		public boolean waitTime(long millis)
		{
			try {
				boolean out = process.waitFor(millis, TimeUnit.MILLISECONDS);
				if (out)
					cleanup();
				return out;
			} catch (InterruptedException e) {
				return false;
			}
		}
		
		@Override
		public void close() throws Exception 
		{
			process.destroy();
			cleanup();
		}
		
		private void cleanup() throws InterruptedException
		{
			Utils.close(inPipe);
			Utils.close(process.getOutputStream());
			stdInThread.join();
			stdOutThread.join();
			stdErrThread.join();
		}
	}
	
	private static class ProcessStreamThread extends Thread
	{
		private ProcessStreamThread(long processId, String suffix, Runnable runnable)
		{
			super(runnable);
			setDaemon(false);
			setName("RookScriptProcess-" + processId + "-" + suffix);
		}
	}

	private static final String[] NO_STRINGS = new String[0];
	
	// Threadlocal "stack" values.
	private static final ThreadLocal<ScriptValue> CACHEVALUE1 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE2 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE3 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE4 = ThreadLocal.withInitial(()->ScriptValue.create(null));
	private static final ThreadLocal<ScriptValue> CACHEVALUE5 = ThreadLocal.withInitial(()->ScriptValue.create(null));

}
