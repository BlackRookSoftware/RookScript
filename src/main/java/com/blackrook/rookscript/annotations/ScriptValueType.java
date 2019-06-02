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
	/** The value to convert to on read. */
	ScriptValue.Type value();
}
