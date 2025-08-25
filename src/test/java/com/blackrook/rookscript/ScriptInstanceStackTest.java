/*******************************************************************************
 * Copyright (c) 2017-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

public class ScriptInstanceStackTest
{
	public static void main(String[] args)
	{
		ScriptInstanceStack stack = new ScriptInstanceStack(16, 128);
		ScriptValue value = ScriptValue.create(null);
		stack.pushFrame(0);
		stack.setValue("x", ScriptValue.create(5));
		stack.getValue("x", value);
		System.out.println(value);
		stack.setValue("x", ScriptValue.create(5));
		stack.getValue("x", value);
		System.out.println(value);
		
		stack.pushStackValue(10);
		stack.pushStackValue(20.0);
		stack.pushStackValue("Butt");
		stack.pushStackValue(true);

		stack.setCommandIndex(20);

		stack.reset();
	}
}

