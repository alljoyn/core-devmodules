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
package org.alljoyn.devmodules.api.filetransfer;

import org.alljoyn.devmodules.APICoreImpl;

// These are the APIs available to applications for the "Groups" service
public class FileTransferAPI {
	
	
	private static int transactionId = 0;
	
	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	// the interface to the background service
	private static FileTransferAPIInterface apiInterface = null;
	
	// sets up the interface to the background service
	private static void setupInterface() {
		if(apiInterface == null) {
			apiInterface = 	APICoreImpl.getInstance().getProxyBusObject("filetransfer",
					new Class[] {FileTransferAPIInterface.class}).getInterface(FileTransferAPIInterface.class);
		}
	}
		
	// register callback listener
	public static void RegisterListener(FileTransferListener listener) {
		FileTransferCallbackObject.listener = listener;
	}
	
	
	// Service-specific interfaces:
	public static void GetFile(String peer, String path) throws Exception {
		if(apiInterface == null)
			setupInterface();
		apiInterface.getFile(peer,path);
	}

	public static void OfferFile(String file, String path) throws Exception {
		if(apiInterface == null)
			setupInterface();
		apiInterface.offerFile(file, path);
	}
	
	public static void PushFile(String peer, String path) throws Exception {
		if(apiInterface == null)
			setupInterface();
		apiInterface.pushFile(peer,path);
	}
}
