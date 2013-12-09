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
package org.alljoyn.devmodules.common;

///////////////////////////////////////////////////////////////////
// Application Callback Handling
// This is for allowing the app to register callbacks for specific events, 
// mostly needed for coordinating startup and (maybe) shutdown
///////////////////////////////////////////////////////////////////

//Class to hold callback functions
public class ClientCallback {
	// This method is called when the client has successfully connected with the background service,
	// and all startup processing has been completed. Apps using the client should wait
	// for this to complete before using the service
	public ClientCallback(){
		//nothing to do
	}
	public void onServiceConnected() {
	}

	// This method is called when the app has disconnected from the service
	public void onServiceDisconnected() {
	}
}//ClientCallback
