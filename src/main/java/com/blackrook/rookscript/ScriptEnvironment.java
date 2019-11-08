package com.blackrook.rookscript;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * A script environment accessor object.
 * This manages references to the host, standard streams, and system/environment variables.
 * @author Matthew Tropiano
 */
public class ScriptEnvironment
{
	/** Host interface reference. */
	private Object hostInterface;
	/** Standard out. */
	private PrintStream standardOut;
	/** Standard error. */
	private PrintStream standardErr;
	/** Standard in. */
	private BufferedInputStream standardIn;

	private ScriptEnvironment(Object hostInterface, PrintStream standardOut, PrintStream standardErr, InputStream standardIn)
	{
		this.hostInterface = hostInterface;
		this.standardOut = standardOut;
		this.standardErr = standardErr;
		this.standardIn = new BufferedInputStream(standardIn);
	}
	
	/**
	 * Builds an environment link connected to a host and standard streams.
	 * @param hostInterface the host interface object.
	 * @param standardOut the stream to use for the standard output stream.
	 * @param standardErr the stream to use for the standard error stream.
	 * @param standardIn the stream to use for the standard input stream.
	 * @return a new script environment to bind to an instance.
	 */
	public static ScriptEnvironment create(Object hostInterface, PrintStream standardOut, PrintStream standardErr, InputStream standardIn)
	{
		return new ScriptEnvironment(hostInterface, standardOut, standardErr, standardIn);
	}

	/**
	 * Builds an environment link for the standard I/O streams.
	 * @param standardOut the stream to use for the standard output stream.
	 * @param standardErr the stream to use for the standard error stream.
	 * @param standardIn the stream to use for the standard input stream.
	 * @return a new script environment to bind to an instance.
	 */
	public static ScriptEnvironment create(PrintStream standardOut, PrintStream standardErr, InputStream standardIn)
	{
		return new ScriptEnvironment(null, standardOut, standardErr, standardIn);
	}

	/**
	 * Builds an environment link for just the host interface object.
	 * @param hostInterface the host interface object.
	 * @return a new script environment to bind to an instance.
	 */
	public static ScriptEnvironment create(Object hostInterface)
	{
		return new ScriptEnvironment(hostInterface, null, null, null);
	}

	/**
	 * Builds an environment link for the standard I/O streams and the host interface object.
	 * All of standard streams are used for out, error, and in.
	 * @param hostInterface the host interface object.
	 * @return a new script environment to bind to an instance.
	 * @see System#out
	 * @see System#err
	 * @see System#in
	 */
	public static ScriptEnvironment createStandardEnvironment(Object hostInterface)
	{
		return new ScriptEnvironment(hostInterface, System.out, System.err, System.in);
	}

	/**
	 * Builds an environment link for the standard I/O streams.
	 * All of standard streams are used for out, error, and in.
	 * @return a new script environment to bind to an instance.
	 * @see System#out
	 * @see System#err
	 * @see System#in
	 */
	public static ScriptEnvironment createStandardEnvironment()
	{
		return new ScriptEnvironment(null, System.out, System.err, System.in);
	}

	/**
	 * Builds an environment link connected to nothing.
	 * @return a new script environment to bind to an instance.
	 */
	public static ScriptEnvironment create()
	{
		return new ScriptEnvironment(null, null, null, null);
	}

	/**
	 * Gets the host interface that this environment uses for host calls.
	 * @return the instance to use.
	 */
	public Object getHostInterface()
	{
		return hostInterface;
	}
		
	/**
	 * Prints the string representation of an object to the standard out stream, if bound.
	 * If this is not bound to a standard out stream, this does nothing.
	 * @param object the object to print.
	 */
	public void print(Object object)
	{
		if (standardOut != null)
			standardOut.print(object);
	}
	
	/**
	 * Prints the string representation of an object to the standard error stream, if bound.
	 * If this is not bound to a standard error stream, this does nothing.
	 * @param object the object to print.
	 */
	public void printErr(Object object)
	{
		if (standardErr != null)
			standardErr.print(object);
	}
	
	/**
	 * Reads from the standard in stream, if bound.
	 * If this is not bound to a standard input stream, this returns -1.
	 * @return the byte value read from input.
	 * @throws IOException if a read error occurs.
	 * @see InputStream#read()
	 */
	public int read() throws IOException
	{
		if (standardIn != null)
			return standardIn.read();
		return -1;
	}

	/**
	 * Reads from the standard in stream into an array, if bound.
	 * If this is not bound to a standard input stream, this returns -1.
	 * @param buffer an array of bytes to write into.
	 * @param offset the offset into the array to start the write.
	 * @param len the maximum amount of bytes to read.
	 * @return the amount of bytes read from input.
	 * @throws IOException if a read error occurs.
	 * @see InputStream#read(byte[], int, int)
	 */
	public int read(byte[] buffer, int offset, int len) throws IOException
	{
		if (standardIn != null)
			return standardIn.read(buffer, offset, len);
		return -1;
	}
	
}
