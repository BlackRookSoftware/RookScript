/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
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
import com.blackrook.rookscript.functions.DataIOFunctions;
import com.blackrook.rookscript.functions.FileFunctions;
import com.blackrook.rookscript.functions.FileIOFunctions;
import com.blackrook.rookscript.functions.StandardIOFunctions;
import com.blackrook.rookscript.functions.StreamFunctions;
import com.blackrook.rookscript.functions.common.BufferFunctions;
import com.blackrook.rookscript.functions.common.ErrorFunctions;
import com.blackrook.rookscript.functions.common.ListFunctions;
import com.blackrook.rookscript.functions.common.MapFunctions;
import com.blackrook.rookscript.functions.common.MiscFunctions;
import com.blackrook.rookscript.functions.common.StringFunctions;
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
			for (ParameterUsage pu : pul)
			{
				out.append("    ").append(pu.getParameterName()).println(":");
				for (TypeUsage tu : pu.getTypes())
				{
					out.append("        (").append(tu.getType() != null 
						? (tu.getType().name() + (tu.getSubType() != null ? ":" + tu.getSubType() : "")) 
						: "ANY"
					).append(") ").println(tu.getDescription());
				}
			}
		}
		out.append("    ").println("Returns:");
		for (TypeUsage tu : usage.getReturnTypes())
		{
			out.append("        (").append(tu.getType() != null 
				? (tu.getType().name() + (tu.getSubType() != null ? ":" + tu.getSubType() : "")) 
				: "ANY"
			).append(") ").println(tu.getDescription());
			
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		printHeader(System.out, "Common");
		printUsages(System.out, MiscFunctions.createResolver());
		printHeader(System.out, "Standard I/O");
		printUsages(System.out, StandardIOFunctions.createResolver());
		printHeader(System.out, "String");
		printUsages(System.out, StringFunctions.createResolver());
		printHeader(System.out, "List / Set");
		printUsages(System.out, ListFunctions.createResolver());
		printHeader(System.out, "Map");
		printUsages(System.out, MapFunctions.createResolver());
		printHeader(System.out, "Buffer");
		printUsages(System.out, BufferFunctions.createResolver());
		printHeader(System.out, "Error");
		printUsages(System.out, ErrorFunctions.createResolver());
		printHeader(System.out, "Math");
		printUsages(System.out, MathFunctions.createResolver());
		printHeader(System.out, "RegEx");
		printUsages(System.out, RegexFunctions.createResolver());
		printHeader(System.out, "Files");
		printUsages(System.out, FileFunctions.createResolver());
		printHeader(System.out, "File I/O");
		printUsages(System.out, FileIOFunctions.createResolver());
		printHeader(System.out, "Stream I/O");
		printUsages(System.out, StreamFunctions.createResolver());
		printHeader(System.out, "Data I/O");
		printUsages(System.out, DataIOFunctions.createResolver());
	}
	
}

