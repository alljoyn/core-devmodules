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
package org.alljoyn.devmodules.debug;

import org.alljoyn.bus.BusException;

///////////////////////////////////////////////////////////////////
// Application (Remote) Debug Callback Handling
// This is for allowing the app to register callbacks for specific events, 
// mostly needed for coordinating startup and (maybe) shutdown
///////////////////////////////////////////////////////////////////

//Class to hold callback functions
public class DebugCallback {

	public DebugCallback(){
		//nothing to do
	}
	
	// called when a new debug service is detected/connected
	// @param service the name of the Debug service detected
	public void onDebugServiceConnected(String service) {
	}

	// called when a debug service is 'lost'
	// @param service the name of the Debug service that has been lost
	public void onDebugServiceDisconnected(String service) {
	}
	
	// called when a debug message is received
	// @param service the name of the Debug service issuing the message
	// @param level   the urgency level (0=lowest) 
	// @param message the (text) contents of the message
    public void onDebugMessageReceived (String service, int level, String message){
    	
    }

}//ClientCallback
