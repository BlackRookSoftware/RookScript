/*******************************************************************************
 * Copyright (c) 2017-2018 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
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

