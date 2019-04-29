/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.function;

import java.lang.reflect.Field;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.rookscript.ScriptFunctionResolver;
import com.blackrook.rookscript.ScriptFunctionType;
import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;

/**
 * A special kind of host function resolver that wraps an {@link Enum} of {@link ScriptFunctionType}.
 * @author Matthew Tropiano 
 */
public class EnumFunctionResolver implements ScriptFunctionResolver
{
	private CaseInsensitiveHashMap<ScriptFunctionType> map;
	
	/**
	 * Creates a new resolver using a list of enum values.
	 * @param en the list of enum values (usually Enum.values()).
	 */
	@SafeVarargs
	public EnumFunctionResolver(Enum<? extends ScriptFunctionType> ... en)
	{
		this.map = new CaseInsensitiveHashMap<>(10, 1f);
		for (Enum<? extends ScriptFunctionType> e : en)
		{
			Field enumField;
			try {
				enumField = e.getClass().getField(e.name());
			} catch (Exception e1) {
				continue;
			}

			if (enumField.getAnnotation(ScriptIgnore.class) != null)
				continue;
			
			String name;
			ScriptName anno;
			if ((anno = enumField.getAnnotation(ScriptName.class)) != null)
				name = anno.value();
			else
				name = e.name();
				
			map.put(name, (ScriptFunctionType)e);
		}
	}
	
	@Override
	public ScriptFunctionType getFunctionByName(String name)
	{
		return map.get(name);
	}
	
	@Override
	public boolean containsFunctionByName(String name)
	{
		return map.containsKey(name);
	}

	@Override
	public ScriptFunctionType[] getFunctions()
	{
		ScriptFunctionType[] out = new ScriptFunctionType[map.size()];
		int i = 0;
		for (ObjectPair<String, ScriptFunctionType> type : map)
			out[i++] = type.getValue();
		return out;
	}

}
