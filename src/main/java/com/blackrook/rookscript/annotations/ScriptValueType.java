/*******************************************************************************
 * Copyright (c) 2017-2021 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.annotations;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.blackrook.rookscript.ScriptValue;

/**
 * Fields and method annotated with this are read into the script
 * as a specific type.
 * @author Matthew Tropiano
 */
@Retention(RUNTIME)
@Target({FIELD, METHOD, CONSTRUCTOR})
public @interface ScriptValueType
{
	/** @return the value to convert to on read. */
	ScriptValue.Type value();
}
