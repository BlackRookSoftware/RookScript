package com.blackrook.rookscript.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Fields and types annotated with this are ignored in object-to-map
 * conversions and creating scopes and function resolvers.
 * @author Matthew Tropiano
 */
@Retention(RUNTIME)
@Target({FIELD, METHOD})
public @interface ScriptIgnore
{
	// empty
}
