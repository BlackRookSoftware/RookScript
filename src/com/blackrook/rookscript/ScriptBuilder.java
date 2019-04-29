package com.blackrook.rookscript;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.rookscript.compiler.ScriptReaderIncluder;
import com.blackrook.rookscript.resolvers.scope.DefaultScopeResolver;

/**
 * A script instance builder for new script instances and such. 
 * @author Matthew Tropiano
 */
public final class ScriptBuilder
{
	/** The source stream path. */
	private String sourceStreamPath;
	
	/** The source data. */
	private String sourceData;
	/** The source file. */
	private File sourceFile;
	/** The source input stream. */
	private InputStream sourceInputStream;
	/** The source input stream. */
	private Reader sourceReader;
	/** The script to use for each instance. */
	private Script script;
	
	/** The optional reader includer. */
	private ScriptReaderIncluder readerIncluder;
	
	/** New instance activation depth. */
	private int activationDepth;
	/** New instance Stack depth. */
	private int stackDepth;
	/** The script stack to use. */
	private ScriptInstanceStack stack;
	
	/** Function resolver to use with each instance. */
	private Queue<ScriptFunctionResolver> functionResolvers;

	/** Scope resolver to use with each instance. */
	private DefaultScopeResolver scopeResolver;
	
	/** Wait handler to use with each instance. */
	private ScriptWaitHandler waitHandler;
	
	/** The host interface to use for each instance. */
	private Object hostInterface;

	// Can't instantiate via new.
	ScriptBuilder()
	{
		this.sourceData = null;
		this.sourceFile = null;
		this.sourceInputStream = null;
		this.sourceReader = null;
		this.script = null;
	}
	
	/**
	 * Attaches script source to compile on instantiation.
	 * Replaces an existing script source, if set before.
	 * @param sourceData the source data.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withSource(String sourceData)
	{
		this.sourceStreamPath = null;
		this.sourceData = sourceData;
		this.sourceFile = null;
		this.sourceInputStream = null;
		this.sourceReader = null;
		this.script = null;
		return this;
	}
	
	/**
	 * Attaches script source to compile on instantiation. 
	 * Replaces an existing script source, if set before.
	 * @param sourceFile the source file.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withSource(File sourceFile)
	{
		this.sourceStreamPath = null;
		this.sourceData = null;
		this.sourceFile = sourceFile;
		this.sourceInputStream = null;
		this.sourceReader = null;
		this.script = null;
		return this;
	}
	
	/**
	 * Attaches script input stream source to compile on instantiation. 
	 * Replaces an existing script source, if set before.
	 * @param streamPath the stream path (used for errors and include directives).
	 * @param sourceStream the source stream.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withSource(String streamPath, InputStream sourceStream)
	{
		this.sourceStreamPath = streamPath;
		this.sourceData = null;
		this.sourceFile = null;
		this.sourceInputStream = sourceStream;
		this.sourceReader = null;
		this.script = null;
		return this;
	}
	
	/**
	 * Attaches script source to compile on instantiation. 
	 * Replaces an existing script source, if set before.
	 * @param streamPath the stream path (used for errors and include directives).
	 * @param sourceReader the source reader.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withSource(String streamPath, Reader sourceReader)
	{
		this.sourceStreamPath = streamPath;
		this.sourceData = null;
		this.sourceFile = null;
		this.sourceInputStream = null;
		this.sourceReader = sourceReader;
		this.script = null;
		return this;
	}
	
	/**
	 * Adds the optional reader includer to use for compiling code. 
	 * @param includer the new includer to use.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder usingReaderIncluder(ScriptReaderIncluder includer)
	{
		this.readerIncluder = includer;
		return this;
	}
	
	/**
	 * Attaches the script used for this instance. 
	 * Removes an existing script source, if set before.
	 * @param script the script to use.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withScript(Script script)
	{
		this.sourceStreamPath = null;
		this.sourceData = null;
		this.sourceFile = null;
		this.sourceInputStream = null;
		this.sourceReader = null;
		this.script = script;
		return this;
	}
	
	// TODO: Finish this.
}

