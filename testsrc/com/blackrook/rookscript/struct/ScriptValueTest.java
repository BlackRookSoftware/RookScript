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

