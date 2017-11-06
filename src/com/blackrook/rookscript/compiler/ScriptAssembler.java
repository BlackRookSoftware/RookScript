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
	 * @param writer the output writer.
	 * @throws IOException if the writer cannot be written to.
	 */
	public static void disassemble(Script script, Writer writer) throws IOException
	{
		int commandCount = script.getCommandCount();
		for (int i = 0; i < commandCount; i++)
		{
			dumpLineLabels(script, writer, i);
			ScriptCommand command = script.getCommand(i);
			writer.write("\t");
			writer.write(command.toString());
			writer.write("\n");
			writer.flush();
		}
		
		dumpLineLabels(script, writer, commandCount);
	}

	
	
	// Dumps the line labels.
	private static void dumpLineLabels(Script script, Writer out, int line) throws IOException
	{
		Iterable<String> labels = script.getLabelsAtIndex(line);
		if (!Common.isEmpty(labels)) for (String label : labels)
		{
			out.write(label);
			out.write(":");
			out.write("\n");
			out.flush();
		}
	}

}

