package com.blackrook.rookscript.resolvers.scope;

import com.blackrook.rookscript.resolvers.ScriptScopeResolver;
import com.blackrook.rookscript.resolvers.ScriptScopeUsage;
import com.blackrook.rookscript.resolvers.variable.DefaultVariableResolver;
import com.blackrook.rookscript.ScriptValue;

import static com.blackrook.rookscript.resolvers.ScriptScopeUsage.type;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public final class DefaultScopeResolverTest 
{
	public static void main(String[] args) 
	{
		DefaultScopeResolver resolver = new DefaultScopeResolver();
		resolver.addScope("test", new DefaultVariableResolver());
		resolver.addScopeUsage("test", () -> ScriptScopeUsage.create()
			.instructions("Test scope.")
			.variable("var", type(ScriptValue.Type.INTEGER, "An integer variable.")
		));
		
		assertNotNull(resolver.getScope("test"));
		assertNotNull(resolver.getScopeUsage("test"));
	}
}
