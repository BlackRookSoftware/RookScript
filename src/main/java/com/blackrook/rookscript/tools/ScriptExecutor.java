/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import com.blackrook.rookscript.ScriptAssembler;
import com.blackrook.rookscript.ScriptEnvironment;
import com.blackrook.rookscript.ScriptInstance;
import com.blackrook.rookscript.exception.ScriptExecutionException;
import com.blackrook.rookscript.functions.MathFunctions;
import com.blackrook.rookscript.functions.RegexFunctions;
import com.blackrook.rookscript.functions.ZipFunctions;
import com.blackrook.rookscript.functions.CommonFunctions;
import com.blackrook.rookscript.functions.DateFunctions;
import com.blackrook.rookscript.functions.FileSystemFunctions;
import com.blackrook.rookscript.functions.IOFunctions;
import com.blackrook.rookscript.functions.PrintFunctions;

/**
 * A class for executing scripts from command line.
 * @author Matthew Tropiano
 * @since [NOW]
 */
public final class ScriptExecutor
{
	private static final String SWITCH_HELP1 = "--help";
	private static final String SWITCH_HELP2 = "-h";
	private static final String SWITCH_ENTRY1 = "--entry";
	private static final String SWITCH_DISASSEMBLE1 = "--disassemble";
	private static final String SWITCH_ACTIVATIONDEPTH1 = "--activation-depth";
	private static final String SWITCH_STACKDEPTH1 = "--stack-depth";
	private static final String SWITCH_SEPARATOR = "--";
	
	private enum Mode
	{
		EXECUTE,
		DISASSEMBLE,
		HELP;
	}
	
	private Mode mode;
	private File scriptFile;
	private String entryPoint;
	private Integer activationDepth;
	private Integer stackDepth;
	private List<String> argList;
	
	private ScriptExecutor()
	{
		this.mode = Mode.EXECUTE;
		this.scriptFile = null;
		this.entryPoint = "main";
		this.activationDepth = 256;
		this.stackDepth = 2048;
		this.argList = new LinkedList<>();
	}

	private static void doDisassemble(ScriptInstance instance)
	{
		StringWriter sw = new StringWriter();
		try {
			ScriptAssembler.disassemble(instance.getScript(), new PrintWriter(sw));
		} catch (IOException e) {
			// Do nothing.
		}
		System.out.print(sw);
	}

	private int execute()
	{
		if (mode == Mode.HELP)
		{
			printHelp();
			return 0;
		}
		
		if (scriptFile == null)
		{
			System.err.println("ERROR: Bad script file.");
			return 4;
		}
		if (!scriptFile.exists())
		{
			System.err.println("ERROR: Script file does not exist: " + scriptFile);
			return 4;
		}

		ScriptInstance instance = ScriptInstance.createBuilder()
			.withSource(scriptFile)
			.withEnvironment(ScriptEnvironment.createStandardEnvironment())
			.withFunctionResolver(CommonFunctions.createResolver())
				.andFunctionResolver(IOFunctions.createResolver())
				.andFunctionResolver(DateFunctions.createResolver())
				.andFunctionResolver(FileSystemFunctions.createResolver())
				.andFunctionResolver(MathFunctions.createResolver())
				.andFunctionResolver(PrintFunctions.createResolver())
				.andFunctionResolver(RegexFunctions.createResolver())
				.andFunctionResolver(ZipFunctions.createResolver())
			.withScriptStack(activationDepth, stackDepth)
			.createInstance();
		
		if (mode == Mode.DISASSEMBLE)
		{
			System.out.println("Disassembly of \"" + scriptFile + "\":");
			doDisassemble(instance);
			return 0;
		}

		if (mode == Mode.EXECUTE)
		{
			if (entryPoint == null)
			{
				System.err.println("ERROR: Bad entry point.");
				return 4;
			}
			if (activationDepth == null)
			{
				System.err.println("ERROR: Bad activation depth.");
				return 4;
			}
			if (stackDepth == null)
			{
				System.err.println("ERROR: Bad stack depth.");
				return 4;
			}
			
			if (instance.getScript().getScriptEntry(entryPoint) == null)
			{
				System.err.println("ERROR: Entry point not found: " + entryPoint);
				return 5;
			}
			
			Object[] args = new Object[argList.size()];
			argList.toArray(args);
			try {
				Integer ret = instance.callAndReturnAs(Integer.class, entryPoint, new Object[]{args});
				return ret != null ? ret : 0;
			} catch (ScriptExecutionException e) {
				System.err.println("Script ERROR: " + e.getLocalizedMessage());
				return 6;
			} catch (ClassCastException e) {
				System.err.println("Script return ERROR: Could not " + e.getLocalizedMessage());
				return 6;
			}
		}
		
		System.err.println("ERROR: Bad mode - INTERNAL ERROR.");
		return -1;
	}

	private static void printHelp()
	{
		PrintStream out = System.out;
		out.println("Arguments: [filename] [switches] -- [scriptargs]");
		out.println();
		out.println("[filename]:");
		out.println("    The script filename.");
		out.println();
		out.println("[switches]:");
		out.println("    --help, -h                   Prints this help.");
		out.println("    --disassemble                Prints the disassembly for this script");
		out.println("                                     and exits.");
		out.println("    --entry [name]               Use a different entry point named [name].");
		out.println("                                     Default: \"main\"");
		out.println("    --activation-depth [num]     Sets the activation depth to [num].");
		out.println("                                     Default: 256");
		out.println("    --stack-depth [num]          Sets the stack value depth to [num].");
		out.println("                                     Default: 2048");
		out.println("    --                           Pass parameters as-is after this token");
		out.println("                                     to the script.");
	}

	private int parseCommandLine(ScriptExecutor executor, String[] args)
	{
		final int STATE_START = 0;
		final int STATE_ARGS = 1;
		final int SWITCHES = 10;
		final int STATE_SWITCHES_ENTRY = SWITCHES + 0;
		final int STATE_SWITCHES_ACTIVATION = SWITCHES + 1;
		final int STATE_SWITCHES_STACK = SWITCHES + 2;
		int state = STATE_START;
		
		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			switch (state)
			{
				case STATE_START:
				{
					if (SWITCH_HELP1.equalsIgnoreCase(arg) || SWITCH_HELP2.equalsIgnoreCase(arg))
					{
						mode = Mode.HELP;
						return 0;
					}
					else if (SWITCH_DISASSEMBLE1.equalsIgnoreCase(arg))
						mode = Mode.DISASSEMBLE;
					else if (SWITCH_ENTRY1.equalsIgnoreCase(arg))
						state = STATE_SWITCHES_ENTRY;
					else if (SWITCH_ACTIVATIONDEPTH1.equalsIgnoreCase(arg))
						state = STATE_SWITCHES_ACTIVATION;
					else if (SWITCH_STACKDEPTH1.equalsIgnoreCase(arg))
						state = STATE_SWITCHES_STACK;
					else if (SWITCH_SEPARATOR.equalsIgnoreCase(arg))
						state = STATE_ARGS;
					else
						scriptFile = new File(arg);
				}
				break;
				
				case STATE_SWITCHES_ENTRY:
				{
					arg = arg.trim();
					entryPoint = arg.length() > 0 ? arg : null;
				}
				break;
				
				case STATE_SWITCHES_ACTIVATION:
				{
					int n;
					try {
						n = Integer.parseInt(arg);
						activationDepth = n > 0 ? n : null;
					} catch (NumberFormatException e) {
						activationDepth = null;
						return 2;
					}
					state = STATE_START;
				}
				break;
				
				case STATE_SWITCHES_STACK:
				{
					int n;
					try {
						n = Integer.parseInt(arg);
						stackDepth = n > 0 ? n : null;
					} catch (NumberFormatException e) {
						stackDepth = null;
						return 2;
					}
					state = STATE_START;
				}
				break;
				
				case STATE_ARGS:
				{
					argList.add(arg);
				}
				break;			
			}
		}
		
		if (state == STATE_SWITCHES_ENTRY)
		{
			System.err.println("ERROR: Expected entry point name after switches.");
			return 3;
		}
		if (state == STATE_SWITCHES_ACTIVATION)
		{
			System.err.println("ERROR: Expected number after activation depth switch.");
			return 3;
		}
		if (state == STATE_SWITCHES_STACK)
		{
			System.err.println("ERROR: Expected number after stack depth switch.");
			return 3;
		}
		
		return 0;
	}
	
	public static void main(String[] args) throws Exception
	{
		if (args.length == 0)
		{
			System.out.println("RookScript Executor (C) 2020 Black Rook Software");
			System.out.println("Use switch `--help` for help.");
			System.exit(-1);
			return;
		}
		
		ScriptExecutor executor = new ScriptExecutor();
		int out;
		if ((out = executor.parseCommandLine(executor, args)) > 0)
			System.exit(out);
		else
			System.exit(executor.execute());
	}
	
}

