/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import com.blackrook.rookscript.struct.ScriptInstanceStack;

public class ScriptInstanceStackTest
{
	public static void main(String[] args)
	{
		ScriptInstanceStack stack = new ScriptInstanceStack(16, 128);
		stack.setValue("x", 5);
		System.out.println(stack.getValue("x"));
		stack.pushLocalScope();
		stack.setValue("x", 10);
		System.out.println(stack.getValue("x"));
		
		stack.pushStackValue(10);
		stack.pushStackValue(20.0);
		stack.pushStackValue("Butt");
		stack.pushStackValue(true);

		stack.setCommandIndex(20);
		stack.pushCommandIndex(10);
		stack.pushCommandIndex(30);

		stack.reset();
	}
}

