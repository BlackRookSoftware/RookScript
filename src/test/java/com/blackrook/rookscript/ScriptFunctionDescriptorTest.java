/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.io.PrintStream;
import java.util.List;

import com.blackrook.rookscript.functions.MathFunctions;
import com.blackrook.rookscript.functions.RegexFunctions;
import com.blackrook.rookscript.ScriptValue.Type;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.StandardIOFunctions;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.lang.ScriptFunctionType.Usage;
import com.blackrook.rookscript.lang.ScriptFunctionType.Usage.ParameterUsage;
import com.blackrook.rookscript.lang.ScriptFunctionType.Usage.TypeUsage;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;

public class ScriptFunctionDescriptorTest
{
	public static void printHeader(PrintStream out, String string)
	{
		out.println("=================================================================");
		out.println("==== " + string);
		out.println("=================================================================");
		out.println();
	}
	
	public static void printUsages(PrintStream out, ScriptFunctionResolver resolver)
	{
		for (ScriptFunctionType sft : resolver.getFunctions())
		{
			Usage usage = sft.getUsage();
			if (usage != null)
				printUsage(out, sft.name(), usage);
			else
				out.println(sft.name() + "(...)");
			out.println();
		}
		out.println();
	}
	
	public static void printUsage(PrintStream out, String name, Usage usage)
	{
		out.append(name).append('(');
		List<ParameterUsage> pul = usage.getParameterInstructions();
		for (int i = 0; i < pul.size(); i++)
		{
			out.append(pul.get(i).getParameterName());
			if (i < pul.size() - 1)
				out.append(", ");
		}
		out.append(')').print('\n');
		
		out.append("    ").println(usage.getInstructions());
		if (!pul.isEmpty())
		{
			out.append("    ").println("Parameters:");
			for (ParameterUsage pu : pul)
			{
				out.append("        ").append(pu.getParameterName()).println(":");
				for (TypeUsage tu : pu.getTypes())
				{
					out.append("            ").println(tu.getType() != null 
						? (tu.getType().name() + (tu.getType() == Type.OBJECTREF ? ":" + tu.getObjectRefType() : "")) 
						: "ANY"
					);
					out.append("                ").println(tu.getDescription());
				}
			}
		}
		out.append("    ").println("Returns:");
		for (TypeUsage tu : usage.getReturnTypes())
		{
			out.append("        ").println(tu.getType() != null 
				? (tu.getType().name() + (tu.getType() == Type.OBJECTREF ? ":" + tu.getObjectRefType() : "")) 
				: "ANY"
			);
			out.append("            ").println(tu.getDescription());
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		printHeader(System.out, "Common");
		printUsages(System.out, CommonFunctions.createResolver());
		printHeader(System.out, "Math");
		printUsages(System.out, MathFunctions.createResolver());
		printHeader(System.out, "I/O");
		printUsages(System.out, StandardIOFunctions.createResolver());
		printHeader(System.out, "RegEx");
		printUsages(System.out, RegexFunctions.createResolver());
	}
	
}

