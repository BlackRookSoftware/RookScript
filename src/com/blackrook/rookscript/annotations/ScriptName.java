package com.blackrook.rookscript.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.blackrook.commons.Reflect;

/**
 * Fields and types annotated with this use the {@link #value()} as
 * the name for the converted/mapped field instead of the auto-generated one.
 * @see Reflect#getFieldName(String)
 * @author Matthew Tropiano
 */
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface ScriptName
{
	String value() default "";
}
