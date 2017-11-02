/*******************************************************************************
 * Copyright (c) 2017 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.compiler;

import java.io.IOException;
import java.io.Writer;

import com.blackrook.commons.Common;
import com.blackrook.rookscript.Script;
import com.blackrook.rookscript.ScriptCommand;

/**
 * The assembler part of the script factories.
 * @author Matthew Tropiano
 */
public final class ScriptAssembler
{
	/**
	 * Disassembles a script into a text representation.
	 * The writer is not closed.
	 * @param script the compiled script.
	 * @param out the output stream.
	 */
	public static void disassemble(Script script, Writer out) throws IOException
	{
		int commandCount = script.getCommandCount();
		for (int i = 0; i < commandCount; i++)
		{
			dumpLineLabels(script, out, i);
			ScriptCommand command = script.getCommand(i);
			out.write("\t");
			out.write(command.toString());
			out.write("\n");
			out.flush();
		}
		
		dumpLineLabels(script, out, commandCount);
	}

	
	
	// Dumps the line labels.
	private static void dumpLineLabels(Script script, Writer out, int line) throws IOException
	{
		Iterable<String> labels = script.getLabelsAtIndex(line);
		if (!Common.isEmpty(labels)) for (String label : labels)
		{
			if (label.startsWith(Script.LABEL_MAIN) || label.startsWith(Script.LABEL_ENTRY_PREFIX) || label.startsWith(Script.LABEL_FUNCTION_PREFIX))
				out.write("\n");
			
			out.write(label);
			out.write(":");
			out.write("\n");
			out.flush();
		}
	}

}

