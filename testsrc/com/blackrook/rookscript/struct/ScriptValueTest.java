package com.blackrook.rookscript.struct;

import com.blackrook.commons.math.PairGroup;
import com.blackrook.commons.math.Triple;
import com.blackrook.rookscript.ScriptValue;

public class ScriptValueTest
{
	public static void main(String[] args)
	{
		ScriptValue sv = ScriptValue.create(false);
		sv.set((Character)'f');
		sv.set((Float)0.78f);
		for (int i = 0; i < 5000; i++)
		{
			long nanos = System.nanoTime();
			sv.set(new Triple(34, -23, 45));
			System.out.println((System.nanoTime() - nanos)+"ns");
		}
		sv.setObjectRef(PairGroup.box(0, 0, 5, 5));
		System.out.println("asdfasdf");
	}
}
