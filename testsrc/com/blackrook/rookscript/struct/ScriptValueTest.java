/*******************************************************************************
 * Copyright (c) 2017-2018 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.struct;

import com.blackrook.commons.Common;
import com.blackrook.commons.math.PairGroup;
import com.blackrook.rookscript.struct.ScriptValue;

public class ScriptValueTest
{
	public static void main(String[] args)
	{
		PairGroup pg = PairGroup.box(0, 0, 1, 1);
		ScriptValue value = ScriptValue.create(pg);
		value.set(5);
		Common.noop();
	}
}

