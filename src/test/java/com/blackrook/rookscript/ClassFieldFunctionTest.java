/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
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
import com.blackrook.rookscript.resolvers.hostfunction.ClassMemberFunctionResolver;
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
		
		ScriptInstance instance = ScriptInstance.createBuilder()
			.withSource(fileName, Utils.openResource(fileName))
			.withEnvironment(ScriptEnvironment.createStandardEnvironment())
			.withFunctionResolver(CommonFunctions.createResolver())
				.andFunctionResolver(StandardIOFunctions.createResolver())
				.andFunctionResolver(MathFunctions.createResolver())
				.andFunctionResolver(fileResolver)
			.withScriptStack(16, 512)
			.createInstance();
		
		int x = 100;
		ScriptValue out = ScriptValue.create(null);
		while (x-- > 0)
		{
			long nanos = System.nanoTime();
			instance.call("main");
			nanos = System.nanoTime() - nanos;
			instance.popStackValue(out);
			System.out.println("Script returns: " + out + " " + nanos + " ns");
		}
	}
}

