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
package org.alljoyn.remotecontrol.api;

import org.alljoyn.devmodules.APICore;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class RemoteControlAPI {
	
	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	private static RemoteControlAPIInterface remoteControlInterface = null;
	
	private static void setupInterface() {
		if(remoteControlInterface == null) {
			remoteControlInterface = APICore.getInstance().getProxyBusObject("remotecontrol",
				new Class[] {RemoteControlAPIInterface.class}).getInterface(RemoteControlAPIInterface.class);
		}
	}

	/**
	 * Send a keyDown event to a specific user
	 * 
	 * @param peer			The name of the peer to send a keycode to
	 * @param keyCode		the value of the key pressed
	 * @throws Exception
	 */
	public static void SendKey(String peer, int keyCode) throws Exception {
		setupInterface();
		remoteControlInterface.onKeyDown("", peer, keyCode);
	}
	
	/**
	 * Broadcast a keyDown event to a group
	 * 
	 * @param group			The name of the group to send a keycode to
	 * @param keyCode		the value of the key pressed
	 * @throws Exception
	 */
	public static void SendGroupKey(String group, int keyCode) throws Exception {
		setupInterface();
		remoteControlInterface.onKeyDown(group, "", keyCode);
	}
	
	/**
	 * Send an intent to a remote peer
	 * 
	 * @param peer			The name of the peer to send an intent to
	 * @param i				The intent that will be sent.  The action and dataString are only pulled out of the intent at this time.
	 * @throws Exception
	 */
	public static void SendIntent(String peer, Intent i) throws Exception {
		setupInterface();
		String intentAction = i.getAction();
		String intentData = i.getDataString();
		remoteControlInterface.sendIntent("", peer, intentAction, intentData == null ? "" : intentData);
	}
	
	/**
	 * Send an intent to a group
	 * 
	 * @param group			The name of the group to broadcast the supplied intent
	 * @param i				The intent that will be sent.  The action and dataString are only pulled out of the intent at this time.
	 * @throws Exception
	 */
	public static void SendGroupIntent(String group, Intent i) throws Exception {
		String intentAction = i.getAction();
		String intentData = i.getDataString();
		remoteControlInterface.sendIntent(group, "", intentAction, intentData == null ? "" : intentData);
	}
	
    /**
     * Assign a listener to be triggered when events occur from remote applications
     * 
     * @param listener		The implemented callback class that will be triggered
     */
	public static void RegisterListener(RemoteControlListener listener) {
		RemoteControlCallbackObject.registerListener(listener);
	}   
}
