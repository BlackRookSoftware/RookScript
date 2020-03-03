/*******************************************************************************
 * Copyright (c) 2017-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.rookscript;

/**
 * The wait handler for running scripts.
 * This determines how to 
 * @author Matthew Tropiano
 */
public interface ScriptWaitHandler
{
	/**
	 * Called to determine if this waiting script can continue.
	 * @param scriptInstance the script instance to test.
	 * @param waitType the waiting type.
	 * @param waitParameter the waiting type parameter.
	 * @return true if this script can continue on in a RUNNING state, false if not.
	 */
	public boolean waitingScriptCanContinue(ScriptInstance scriptInstance, Object waitType, Object waitParameter);
	
	/**
	 * Called when {@link #waitingScriptCanContinue(ScriptInstance, Object, Object)} returns false,
	 * in case this script's waiting state needs updating.
	 * @param scriptInstance the script instance to update.
	 * @param waitType the current waiting type.
	 * @param waitParameter the current waiting type parameter.
	 */
	public void waitingScriptUpdate(ScriptInstance scriptInstance, Object waitType, Object waitParameter);

}
