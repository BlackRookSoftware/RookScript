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
import com.blackrook.rookscript.resolvers.function.ClassMemberFunctionResolver;
import com.blackrook.rookscript.util.Utils;

public class ClassFieldFunctionTest
{
	public static void main(String[] args) throws Exception
	{
		ClassMemberFunctionResolver<File> fileResolver = new ClassMemberFunctionResolver<>(File.class);
		fileResolver.addConstructor("file", File.class.getConstructor(String.class), null);
		fileResolver.addGetterMethod("fpath", File.class.getMethod("getPath"), null);
		fileResolver.addGetterMethod("fparent", File.class.getMethod("getParent"), null);
		fileResolver.addGetterMethod("fabsolute", File.class.getMethod("isAbsolute"), null);
		fileResolver.addGetterMethod("fisdir", File.class.getMethod("isDirectory"), null);
		fileResolver.addGetterMethod("fhidden", File.class.getMethod("isHidden"), null);
		fileResolver.addMethod("fexists", File.class.getMethod("exists"), false, false, null);
		fileResolver.addMethod("flist", File.class.getMethod("listFiles"), false, false, null);
		
		String fileName;
		if ((fileName = Utils.arrayElement(args, 0)) == null)
		{
			System.out.println("ERROR: No file name for script.");
			System.out.println("Usage: command [filename]");
			System.out.println();
			return;
		}
		
		ScriptInstance instance = ScriptInstance.build()
			.withSource(new File(fileName))
			.withFunctionResolver(CommonFunctions.getResolver())
				.andFunctionResolver(StandardIOFunctions.getResolver())
				.andFunctionResolver(MathFunctions.getResolver())
				.andFunctionResolver(fileResolver)
			.withScriptStack(16, 512)
			.get();
		
		int x = 1;
		while (x-- > 0)
		{
			long nanos = System.nanoTime();
			instance.call("main");
			System.out.println("Script returns: "+instance.popStackValue()+" "+(System.nanoTime()-nanos)+" ns");
		}
	}
}

