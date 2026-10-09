package com.blackrook.rookscript.resolvers;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import com.blackrook.rookscript.ScriptValue;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.resolvers.ScriptScopeResolver.Usage;

/**
 * Scope usage builder.
 * @author Matthew Tropiano
 * @since 1.20.0
 */
public class ScriptScopeUsage implements Usage
{
	/** Instructions for scope use. */
	private String instructions;
	/** Instructions for each scope variable. */
	private List<VariableUsage> variables;

	private ScriptScopeUsage()
	{
		this.instructions = "";
		this.variables = new LinkedList<VariableUsage>();
	}
	
	/**
	 * @return a new usage description.
	 */
	public static ScriptScopeUsage create()
	{
		return new ScriptScopeUsage();
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
	 * @return this scope usage.
	 */
	public ScriptScopeUsage instructions(String instructions)
	{
		this.instructions = Objects.requireNonNull(instructions);
		return this;
	}
	
	/**
	 * Adds a variable's usage.
	 * @param name the variable name.
	 * @param usages each variable type and its description.
	 * @return this scope usage.
	 */
	public ScriptScopeUsage variable(String name, TypeUsage ... usages)
	{
		Objects.requireNonNull(name);
		variables.add(new Variable(name, usages));
		return this;
	}
	
	@Override
	public String getInstructions()
	{
		return instructions;
	}

	@Override
	public List<VariableUsage> getVariableInstructions()
	{
		return variables;
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
	
	private static class Variable implements VariableUsage
	{
		private String name;
		private List<TypeUsage> types;

		private Variable(String name, TypeUsage ... typeUsages)
		{
			this.name = name;
			this.types = new LinkedList<TypeUsage>();
			for (TypeUsage tu : typeUsages)
				this.types.add(tu);
		}
		
		@Override
		public String getVariableName()
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
