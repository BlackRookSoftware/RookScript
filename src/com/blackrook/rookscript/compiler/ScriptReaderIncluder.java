/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.compiler;

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface that allows the user to resolve a resource by path when the
 * {@link ScriptReader} parses it.
 * @author Matthew Tropiano
 */
public interface ScriptReaderIncluder
{

	/**
	 * Returns an open {@link InputStream} for a path when the parser needs a resource.
	 * By default, this attempts to open a file at the provided path.
	 * @param streamName the current name of the stream. This includer may use this to
	 * 		procure a relative path.
	 * @param path the stream path from the include directive.
	 * @return an open {@link InputStream} for the requested resource, or null if not found.
	 * @throws IOException if the next resource to include cannot the opened. 
	 */
	public String getIncludeResourceName(String streamName, String path) throws IOException;

	/**
	 * Returns an open {@link InputStream} for a path when the parser needs a resource.
	 * By default, this attempts to open a file at the provided path.
	 * @param path the stream path.
	 * @return an open {@link InputStream} for the requested resource, or null if not found.
	 * @throws IOException if the next resource to include cannot the opened. 
	 */
	public InputStream getIncludeResource(String path) throws IOException;
	
}
