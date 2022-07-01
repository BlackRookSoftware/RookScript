/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.lang;

import java.util.LinkedList;
import java.util.List;

import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;

/**
 * Function usage builder.
 * @author Matthew Tropiano
 */
public final class ScriptFunctionUsage implements ScriptFunctionType.Usage
{
	/** Instructions for function use. */
	private String instructions;
	/** List of each parameter. */
	private List<ParameterUsage> parameters;
	/** List of each return type. */
	private List<TypeUsage> returnTypes;
	
	private ScriptFunctionUsage()
	{
		this.instructions = "";
		this.parameters = new LinkedList<ParameterUsage>();
		this.returnTypes = new LinkedList<TypeUsage>();
	}
	
	/**
	 * @return a new usage description.
	 */
	public static ScriptFunctionUsage create()
	{
		return new ScriptFunctionUsage();
	}
	
	/**
	 * Creates a single type usage.
	 * @param description its description.
	 * @return a new type usage.
	 */
	public static TypeUsage type(String description)
	{
		return new TypeInfo(null, null, description);
	}

	/**
	 * Creates a single type usage.
	 * @param type the script value type.
	 * @param description its description.
	 * @return a new type usage.
	 */
	public static TypeUsage type(ScriptValue.Type type, String description)
	{
		return new TypeInfo(type, null, description);
	}

	/**
	 * Creates a single type usage.
	 * @param type the script value type.
	 * @param objectRefType if type is {@link Type#OBJECTREF}, then this is the expected Object type.
	 * @param description its description.
	 * @return a new type usage.
	 */
	public static TypeUsage type(ScriptValue.Type type, String objectRefType, String description)
	{
		return new TypeInfo(type, objectRefType, description);
	}

	/**
	 * Sets the instructions.
	 * @param instructions the new instructions.
	 * @return itself.
	 */
	public ScriptFunctionUsage instructions(String instructions)
	{
		this.instructions = instructions;
		return this;
	}
	
	/**
	 * Adds a parameter's usage.
	 * @param name the parameter name.
	 * @param usages each accepted type and its description.
	 * @return itself.
	 */
	public ScriptFunctionUsage parameter(String name, TypeUsage ... usages)
	{
		parameters.add(new Parameter(name, usages));
		return this;
	}
	
	/**
	 * Adds return types.
	 * @param typeUsages each returned type and its description.
	 * @return itself.
	 */
	public ScriptFunctionUsage returns(TypeUsage ... typeUsages)
	{
		for (TypeUsage tu : typeUsages)
			returnTypes.add(tu);
		return this;
	}
	
	@Override
	public String getInstructions()
	{
		return instructions;
	}

	@Override
	public List<ParameterUsage> getParameterInstructions() 
	{
		return parameters;
	}

	@Override
	public List<TypeUsage> getReturnTypes() 
	{
		return returnTypes;
	}
	
	private static class TypeInfo implements TypeUsage
	{
		private ScriptValue.Type type;
		private String objectRefType;
		private String description;

		private TypeInfo(ScriptValue.Type type, String objectRefType, String description)
		{
			this.type = type;
			this.objectRefType = objectRefType;
			this.description = description;
		}
		
		@Override
		public Type getType()
		{
			return type;
		}

		@Override
		public String getSubType() 
		{
			return objectRefType;
		}
		
		@Override
		public String getDescription() 
		{
			return description;
		}
		
	}
	
	private static class Parameter implements ParameterUsage
	{
		private String name;
		private List<TypeUsage> types;

		private Parameter(String name, TypeUsage ... typeUsages)
		{
			this.name = name;
			this.types = new LinkedList<TypeUsage>();
			for (TypeUsage tu : typeUsages)
				this.types.add(tu);
		}
		
		@Override
		public String getParameterName()
		{
			return name;
		}

		@Override
		public List<TypeUsage> getTypes() 
		{
			return types;
		}
		
	}
	
}

