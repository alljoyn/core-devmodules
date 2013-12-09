/******************************************************************************
* Copyright (c) 2013, AllSeen Alliance. All rights reserved.
*
*    Permission to use, copy, modify, and/or distribute this software for any
*    purpose with or without fee is hereby granted, provided that the above
*    copyright notice and this permission notice appear in all copies.
*
*    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
*    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
*    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
*    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
*    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
*    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
*    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
******************************************************************************/
package org.alljoyn.whiteboard.api;

import org.alljoyn.devmodules.APICore;
import org.alljoyn.devmodules.callbacks.CallbackObjectBase;
import org.alljoyn.devmodules.callbacks.TransactionHandler;
import org.alljoyn.devmodules.common.WhiteboardLineInfo;
import org.json.JSONObject;

import android.util.Log;

public class WhiteboardAPI {
	
	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	
	private static WhiteboardAPIInterface whiteboardInterface = null;
	
	private static void setupInterface() {
		if(whiteboardInterface == null) {
			whiteboardInterface = APICore.getInstance().getProxyBusObject("whiteboard",
				new Class[] {WhiteboardAPIInterface.class}).getInterface(WhiteboardAPIInterface.class);
		}
	}

	/**
	 * This method will send a line part to a group or peer
	 * 
	 * @param groupId		the name of the group
	 * @param peer			the name of the peer, if groupId is supplied it takes precedence
	 * @param lineInfo		the line information
	 * @throws Exception
	 */
	public static void Draw(String groupId, String peer, WhiteboardLineInfo lineInfo) throws Exception
	{
		setupInterface();
		if(lineInfo.sender == null)
			lineInfo.sender = "";
		whiteboardInterface.Draw(groupId == null ? "" : groupId,
							peer == null ? "" : peer,
									lineInfo);
	}
    
	/**
	 * This method will clear all the lines on a supplied group or peer.
	 * 
	 * @param groupId		the name of the group
	 * @param peer			the name of the peer, if groupId is supplied it takes precedence
	 * @throws Exception
	 */
    public static void Clear(String groupId, String peer) throws Exception
	{
    	setupInterface();
		whiteboardInterface.Clear(groupId, peer);
	}
    
    /**
     * Assign a listener to be triggered when events occur from remote applications
     * 
     * @param listener		The implemented callback class that will be triggered
     */
	public static void RegisterListener(WhiteboardListener listener) {
		WhiteboardCallbackObject.registerListener(listener);
	}
	
	/**
     * Remove all listeners that are triggered when events occur from remote applications
     * 
     * @param listener		The implemented callback class that will be triggered
     */
	public static void UnregisterListener(WhiteboardListener listener) {
		WhiteboardCallbackObject.unregisterListener(listener);
	}
	
	/**
     * Remove all listeners that are triggered when events occur from remote applications
     * 
     * @param listener		The implemented callback class that will be triggered
     */
	public static void UnregisterAllListeners() {
		WhiteboardCallbackObject.unregisterAllListener();
	}  
}
