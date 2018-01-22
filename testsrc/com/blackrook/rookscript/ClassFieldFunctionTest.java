package com.blackrook.rookscript;

import java.io.File;

import com.blackrook.rookscript.ScriptFunctionType.Usage;
import com.blackrook.rookscript.resolver.ClassFieldFunctionResolver;

public class ClassFieldFunctionTest
{
	public static void main(String[] args) throws Exception
	{
		ClassFieldFunctionResolver resolver = new ClassFieldFunctionResolver(File.class);
		for (ScriptFunctionType type : resolver.getFunctions())
		{
			Usage usage = type.getUsage();
			System.out.println(type.name() +": " + usage.getUsageInstructions());
			for (String s : usage.getUsageParameterInstructions())
				System.out.println("\t" + s);
		}
	}
}

