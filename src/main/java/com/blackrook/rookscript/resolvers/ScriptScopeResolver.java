/*******************************************************************************
 * Copyright (c) 2017-2026 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers;

import java.util.List;

import com.blackrook.rookscript.ScriptValue;

/**
 * An interface for structures that map string keys to
 * {@link ScriptVariableResolver}s. As a strict policy, all scope names are CASE-INSENSITIVE.
 * @author Matthew Tropiano
 */
public interface ScriptScopeResolver
{
	/** @since 1.20.0 */
	static final String[] NO_NAMES = new String[0];

	/**
	 * A scope resolver with no scopes.
	 * @since 1.8.0
	 */
	static final ScriptScopeResolver EMPTY = new ScriptScopeResolver()
	{
		@Override
		public ScriptVariableResolver getScope(String name)
		{
			return null;
		}
		
		@Override
		public boolean containsScope(String name)
		{
			return false;
		}

		@Override
		public String[] getScopeNames()
		{
			return NO_NAMES;
		}

		@Override
		public Usage getScopeUsage(String name)
		{
			return null;
		}
		
	};
	
	/**
	 * Gets the corresponding scope for a scope name.
	 * @param name the scope name.
	 * @return the corresponding scope, or <code>null</code> if no corresponding scope.
	 */
	ScriptVariableResolver getScope(String name);
	
	/**
	 * Checks if this contains a scope by its scope name.
	 * @param name the scope name.
	 * @return true if so, false if not.
	 */
	boolean containsScope(String name);
	
	/**
	 * Gets all possible scope names from this resolver.
	 * Each name in the list should return a non-null {@link ScriptVariableResolver} via {@link #getScope(String)}.
	 * @return an array of valid scope names.
	 * @since 1.20.0
	 */
	String[] getScopeNames();
	
	/**
	 * Gets a scope's usage information by its name.
	 * @param name the scope name.
	 * @return the scope's usage. Can return <code>null</code>, always if {@link #getScope(String)} returns <code>null</code>.
	 * @since 1.20.0
	 */
	Usage getScopeUsage(String name);
	
	/**
	 * Scope usage info.
	 * @since 1.20.0
	 */
	interface Usage
	{
		/**
		 * Gets the scope usage instructions.
		 * @return the scope usage instructions. Never returns null.
		 */
		String getInstructions();
		
		/**
		 * Gets the scope variable usage instructions.
		 * @return the scope variable usage instructions. Never returns null.
		 */
		List<VariableUsage> getVariableInstructions();
		
		/**
		 * A single variable's usage instructions.
		 */
		interface VariableUsage
		{
			/**
			 * @return the name of the variable.
			 */
			String getVariableName();

			/**
			 * Gets a list of possible variable types for this parameter.
			 * Other types may result in an error.
			 * @return the list of accepted types and usages. Never returns null.
			 */
			List<TypeUsage> getTypes();

		}

		/**
		 * Per-relevant-type usage.
		 */
		interface TypeUsage
		{
			/**
			 * @return the script value type. If this returns null, it means "any type."
			 */
			ScriptValue.Type getType();
			
			/**
			 * @return the subtype. Usually a class name if OBJECTREF or the contents of a list/map. Can return null for no subtype.
			 */
			String getSubType();
			
			/**
			 * @return the description.
			 */
			String getDescription();
			
		}
		
	}
	
}
