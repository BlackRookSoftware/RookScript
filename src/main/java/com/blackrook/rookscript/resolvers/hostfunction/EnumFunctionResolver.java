/*******************************************************************************
 * Copyright (c) 2017-2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.resolvers.hostfunction;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.blackrook.rookscript.annotations.ScriptIgnore;
import com.blackrook.rookscript.annotations.ScriptName;
import com.blackrook.rookscript.lang.ScriptFunctionType;
import com.blackrook.rookscript.resolvers.ScriptFunctionResolver;

/**
 * A special kind of host function resolver that wraps an {@link Enum} of {@link ScriptFunctionType}.
 * @author Matthew Tropiano 
 */
public class EnumFunctionResolver implements ScriptFunctionResolver
{
	private Map<String, ScriptFunctionType> map;
	private ScriptFunctionType[] list;
	
	/**
	 * Creates a new resolver using a list of enum values.
	 * @param en the list of enum values (usually Enum.values()).
	 */
	@SafeVarargs
	public EnumFunctionResolver(Enum<? extends ScriptFunctionType> ... en)
	{
		this.map = new HashMap<>(10, 1f);
		List<ScriptFunctionType> funcs = new LinkedList<ScriptFunctionType>();
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
				
			map.put(name.toLowerCase(), (ScriptFunctionType)e);
			funcs.add((ScriptFunctionType)e);
		}
		this.list = new ScriptFunctionType[funcs.size()];
		funcs.toArray(this.list);
	}
	
	@Override
	public ScriptFunctionType getFunction(String name)
	{
		return map.get(name.toLowerCase());
	}
	
	@Override
	public boolean containsFunction(String name)
	{
		return map.containsKey(name.toLowerCase());
	}

	@Override
	public ScriptFunctionType[] getFunctions()
	{
		return list;
	}

}
