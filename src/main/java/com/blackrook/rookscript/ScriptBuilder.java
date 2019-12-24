/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.blackrook.rookscript.compiler.ScriptReader;
import com.blackrook.rookscript.compiler.ScriptReaderIncluder;
import com.blackrook.rookscript.compiler.ScriptReaderOptions;
import com.blackrook.rookscript.exception.ScriptBuilderException;
import com.blackrook.rookscript.exception.ScriptExecutionException;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;
import com.blackrook.rookscript.resolvers.ScriptHostFunctionResolver;
import com.blackrook.rookscript.resolvers.ScriptVariableResolver;
import com.blackrook.rookscript.resolvers.hostfunction.MultiHostFunctionResolver;
import com.blackrook.rookscript.resolvers.scope.DefaultScopeResolver;

/**
 * A script instance builder for new script instances and such. 
 * @author Matthew Tropiano
 */
public final class ScriptBuilder
{
	@FunctionalInterface
	private interface ScriptProvider
	{
		Script getScript(ScriptHostFunctionResolver functionResolver, ScriptReaderIncluder includer, ScriptReaderOptions options) throws IOException;
	}

	@FunctionalInterface
	private interface ScriptInstanceStackProvider
	{
		ScriptInstanceStack getStack();
	}

	/** Script provider. */
	private ScriptProvider scriptProvider;
	
	/** The optional reader includer. */
	private ScriptReaderIncluder readerIncluder;
	
	/** The optional reader options. */
	private ScriptReaderOptions readerOptions;
	
	/** The script stack to use. */
	private ScriptInstanceStackProvider stackProvider;
	
	/** Resolvers in the global namespace. */
	private Queue<ScriptFunctionResolver> globalResolvers;
	
	/** Resolvers in the named namespaces. */
	private Map<String, ScriptFunctionResolver> namedResolvers;

	/** Scope resolver to use with each instance. */
	private DefaultScopeResolver scopeResolver;
	
	/** Wait handler to use with each instance. */
	private ScriptWaitHandler waitHandler;
	
	/** The the environment to use for each instance. */
	private ScriptEnvironment environment;

	// Can't instantiate via new.
	ScriptBuilder()
	{
		this.scriptProvider = null;
		this.readerIncluder = null;
		this.readerOptions = null;
		this.stackProvider = null;
		this.globalResolvers = new LinkedList<>();
		this.namedResolvers = new HashMap<>();
		this.scopeResolver = new DefaultScopeResolver();
		this.waitHandler = null;
		this.environment = null;
	}
	
	/**
	 * Attaches script source to compile on instantiation.
	 * Replaces an existing script source, if set before.
	 * @param sourceData the source data.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withSource(final String sourceData)
	{
		scriptProvider = (functionResolver, includer, options)->{
			return ScriptReader.read(sourceData, functionResolver, includer, options);
		};
		return this;
	}
	
	/**
	 * Attaches script source to compile on instantiation.
	 * Replaces an existing script source, if set before.
	 * @param sourcePath the path name of this string data (for include path parent).
	 * @param sourceData the source data.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withSource(final String sourcePath, final String sourceData)
	{
		scriptProvider = (functionResolver, includer, options)->{
			return ScriptReader.read(sourcePath, sourceData, functionResolver, includer, options);
		};
		return this;
	}
	
	/**
	 * Attaches script source to compile on instantiation. 
	 * Replaces an existing script source, if set before.
	 * @param sourceFile the source file.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withSource(final File sourceFile)
	{
		scriptProvider = (functionResolver, includer, options)->{
			return ScriptReader.read(sourceFile, functionResolver, includer, options);
		};
		return this;
	}
	
	/**
	 * Attaches script input stream source to compile on instantiation. 
	 * Replaces an existing script source, if set before.
	 * @param streamPath the stream path (used for errors and include directives).
	 * @param sourceStream the source stream.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withSource(final String streamPath, final InputStream sourceStream)
	{
		scriptProvider = (functionResolver, includer, options)->{
			return ScriptReader.read(streamPath, sourceStream, functionResolver, includer, options);
		};
		return this;
	}
	
	/**
	 * Attaches script source to compile on instantiation. 
	 * Replaces an existing script source, if set before.
	 * @param streamPath the stream path (used for errors and include directives).
	 * @param sourceReader the source reader.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withSource(final String streamPath, final Reader sourceReader)
	{
		scriptProvider = (functionResolver, includer, options)->{
			return ScriptReader.read(streamPath, sourceReader, functionResolver, includer, options);
		};
		return this;
	}
	
	/**
	 * Adds the optional reader includer to use for compiling code. 
	 * @param includer the reader includer to use.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder usingReaderIncluder(ScriptReaderIncluder includer)
	{
		this.readerIncluder = includer;
		return this;
	}
	
	/**
	 * Adds the optional reader options to use for compiling code. 
	 * @param options the reader options to use.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder usingReaderOptions(ScriptReaderOptions options)
	{
		this.readerOptions = options;
		return this;
	}
	
	/**
	 * Attaches the script used for this instance. 
	 * Removes an existing script source, if set before.
	 * @param script the script to use.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withScript(final Script script)
	{
		this.scriptProvider = (functionResolver, includer, options)->{
			return script;
		};
		return this;
	}
	
	/**
	 * Attaches a new script instance stack used for this instance. 
	 * @param activationDepth the activation stack depth.
	 * @param stackDepth the value stack depth.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withScriptStack(final int activationDepth, final int stackDepth)
	{
		this.stackProvider = ()->{
			return new ScriptInstanceStack(activationDepth, stackDepth);
		};
		return this;
	}
	
	/**
	 * Attaches a script instance stack for this instance. 
	 * @param stack the stack to use.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withScriptStack(final ScriptInstanceStack stack)
	{
		this.stackProvider = ()->stack;
		return this;
	}
	
	/**
	 * Adds a function resolver to this builder to be used in the script, clearing all resolvers first.
	 * @param resolver the resolver to add.
	 * @return the builder, for chained calls.
	 * @see #andFunctionResolver(ScriptFunctionResolver)
	 */
	public ScriptBuilder withFunctionResolver(ScriptFunctionResolver resolver)
	{
		globalResolvers.clear();
		namedResolvers.clear();
		return andFunctionResolver(resolver);
	}
	
	/**
	 * Adds a function resolver to this builder to be used in the script, clearing all resolvers first.
	 * @param namespace the namespace to use for the resolver.
	 * @param resolver the resolver to add.
	 * @return the builder, for chained calls.
	 * @see #andFunctionResolver(ScriptFunctionResolver)
	 */
	public ScriptBuilder withFunctionResolver(String namespace, ScriptFunctionResolver resolver)
	{
		globalResolvers.clear();
		namedResolvers.clear();
		return andFunctionResolver(namespace, resolver);
	}
	
	/**
	 * Adds a function resolver to this builder to be used in the script.
	 * @param resolver the resolver to add.
	 * @return the builder, for chained calls.
	 * @see #withFunctionResolver(ScriptFunctionResolver)
	 */
	public ScriptBuilder andFunctionResolver(ScriptFunctionResolver resolver)
	{
		globalResolvers.add(resolver);
		return this;
	}
	
	/**
	 * Adds a function resolver to this builder to be used in the script.
	 * @param namespace the namespace to use for the resolver.
	 * @param resolver the resolver to add.
	 * @return the builder, for chained calls.
	 * @see #withFunctionResolver(ScriptFunctionResolver)
	 */
	public ScriptBuilder andFunctionResolver(String namespace, ScriptFunctionResolver resolver)
	{
		namedResolvers.put(namespace, resolver);
		return this;
	}
	
	/**
	 * Adds a scope to be used in the script, clearing the set of scopes first.
	 * @param name the scope name.
	 * @param resolver the scope resolver to add.
	 * @return the builder, for chained calls.
	 * @see #andScope(String, ScriptVariableResolver)
	 */
	public ScriptBuilder withScope(String name, ScriptVariableResolver resolver)
	{
		scopeResolver.clear();
		return andScope(name, resolver);
	}
	
	/**
	 * Adds a scope to be used in the script.
	 * @param name the scope name.
	 * @param resolver the scope resolver to add.
	 * @return the builder, for chained calls.
	 * @see #withScope(String, ScriptVariableResolver)
	 */
	public ScriptBuilder andScope(String name, ScriptVariableResolver resolver)
	{
		scopeResolver.addScope(name, resolver);
		return this;
	}
	
	/**
	 * Sets the wait handler for the script instance.
	 * @param waitHandler the waiting handler to attach.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withWaitHandler(ScriptWaitHandler waitHandler)
	{
		this.waitHandler = waitHandler;
		return this;
	}
	
	/**
	 * Sets the script environment for this script.
	 * @param environment the environment to use.
	 * @return the builder, for chained calls.
	 */
	public ScriptBuilder withEnvironment(ScriptEnvironment environment)
	{
		this.environment = environment;
		return this;
	}
	
	/**
	 * Gets the instance built from the set components.
	 * The script is compiled through this call. Any parsing errors are thrown as
	 * ScriptBuilderException with the parser error as the cause.
	 * @return a new ScriptInstance created from the characteristics set.
	 * @throws ScriptBuilderException if the instance can't be built.
	 */
	public ScriptInstance get() 
	{
		if (scriptProvider == null)
			throw new ScriptBuilderException("A script or script source.");
		if (stackProvider == null)
			throw new ScriptBuilderException("An instance stack was not set.");

		MultiHostFunctionResolver resolver = new MultiHostFunctionResolver();
		for (ScriptFunctionResolver r : globalResolvers)
			resolver.addResolver(r);
		for (Map.Entry<String, ScriptFunctionResolver> r : namedResolvers.entrySet())
			resolver.addNamedResolver(r.getKey(), r.getValue());
		
		Script script;
		try {
			script = scriptProvider.getScript(
				resolver,
				readerIncluder != null ? readerIncluder : ScriptReader.DEFAULT_INCLUDER, 
				readerOptions != null ? readerOptions : ScriptReader.DEFAULT_OPTIONS
			);
		} catch (Exception e) {
			throw new ScriptBuilderException("An error occurred building a script instance.", e);
		}

		ScriptInstanceStack stack = stackProvider.getStack();
		return new ScriptInstance(script, stack, scopeResolver, waitHandler, environment != null ? environment : ScriptEnvironment.create());
	}
	
	/**
	 * Creates the instance using the set components, then calls an entry point with a set of parameters.
	 * The script is compiled through this call. Any parsing errors are thrown as
	 * ScriptBuilderException with the parser error as the cause.
	 * @param entryName the entry point name.
	 * @param parameters the starting parameters to push onto the stack.
	 * @return a new ScriptInstance created from the characteristics set.
	 * @throws ScriptBuilderException if the instance can't be built.
	 * @throws ScriptExecutionException if the provided amount of parameters do not match the amount of parameters that the script requires, 
	 * 		or the provided entry point does not exist.
	 * @see #get()
	 * @see ScriptInstance#call(String, Object...)
	 */
	public ScriptInstance call(String entryName, Object ... parameters)
	{
		ScriptInstance out = get();
		out.call(entryName, parameters);
		return out;
	}
	
	/**
	 * Creates the instance using the set components, then calls an entry point with a set of parameters.
	 * The script is compiled through this call. Any parsing errors are thrown as
	 * ScriptBuilderException with the parser error as the cause.
	 * <p>
	 * The script instance is lost from this call - only use this if you want to execute and discard the
	 * execution instance.
	 * @param returnType the return type to get from the script.
	 * @param entryName the entry point name.
	 * @param parameters the starting parameters to push onto the stack.
	 * @return a new ScriptInstance created from the characteristics set.
	 * @throws ScriptBuilderException if the instance can't be built.
	 * @throws ScriptExecutionException if the provided amount of parameters do not match the amount of parameters that the script requires, 
	 * 		or the provided entry point does not exist.
	 * @see #get()
	 * @see ScriptInstance#callAndReturnAs(Class, String, Object...)
	 */
	public <T> T callAndReturnAs(Class<T> returnType, String entryName, Object ... parameters)
	{
		return get().callAndReturnAs(returnType, entryName, parameters);
	}
	
}

