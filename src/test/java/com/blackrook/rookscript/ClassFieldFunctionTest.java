/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

import java.io.File;

import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.MathFunctions;
import com.blackrook.rookscript.functions.StandardIOFunctions;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.resolvers.function.ClassMemberFunctionResolver;
import com.blackrook.rookscript.struct.Utils;

public class ClassFieldFunctionTest
{
	public static void main(String[] args) throws Exception
	{
		ClassMemberFunctionResolver<File> fileResolver = (new ClassMemberFunctionResolver<>(File.class))
			.addConstructor("file", File.class.getConstructor(String.class))
			.addMethod("fpath", "getPath", false, false)
			.addMethod("fparent", "getParent", false, false)
			.addMethod("fabsolute", "isAbsolute", false, false)
			.addMethod("fisdir", "isDirectory", false, false)
			.addMethod("fhidden", "isHidden", false, false)
			.addMethod("fexists", "exists", false, false)
			.addMethod("flist", "listFiles", false, false);
		
		for (ScriptFunctionType t : fileResolver.getFunctions())
			System.out.println(t);
		
		String fileName;
		if ((fileName = Utils.arrayElement(args, 0)) == null)
		{
			System.out.println("ERROR: No file name for script.");
			System.out.println("Usage: command [filename]");
			System.out.println();
			return;
		}
		
		ScriptInstance instance = ScriptInstance.build()
			.withSource(fileName, Utils.openResource(fileName))
			.withEnvironment(ScriptEnvironment.createStandardEnvironment())
			.withFunctionResolver(CommonFunctions.getResolver())
				.andFunctionResolver(StandardIOFunctions.getResolver())
				.andFunctionResolver(MathFunctions.getResolver())
				.andFunctionResolver(fileResolver)
			.withScriptStack(16, 512)
			.get();
		
		int x = 100;
		while (x-- > 0)
		{
			long nanos = System.nanoTime();
			instance.call("main");
			System.out.println("Script returns: "+instance.popStackValue()+" "+(System.nanoTime()-nanos)+" ns");
		}
	}
}

