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

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.bus.annotation.BusSignalHandler;

import org.alljoyn.devmodules.APICore;
import org.alljoyn.devmodules.callbacks.CallbackObjectBase;
import org.alljoyn.devmodules.callbacks.TransactionHandler;
import org.alljoyn.devmodules.common.MediaIdentifier;

import android.util.Log;

// This class maps signals from the background service to callbacks to the app
// In general, each method handles a Signal from the background service and calls
// the corresponding Listener callback method

public class FileTransferCallbackObject extends CallbackObjectBase implements FileTransferCallbackInterface, BusObject {
	static FileTransferListener listener;

	// Generic callbacks
	@BusSignalHandler(iface=FileTransferCallbackInterface.SERVICE_NAME, signal="CallbackJSON")
	public void CallbackJSON(int transactionId, String module,
			String jsonCallbackData) {
		APICore.getInstance().EnableConcurrentCallbacks();
		Integer key = Integer.valueOf(transactionId);
		if(transactionList.containsKey(key)) {
			TransactionHandler th = transactionList.get(key);
			th.HandleTransaction(jsonCallbackData, null, 0, 0);
		}
	}
	
	@BusSignalHandler(iface=FileTransferCallbackInterface.SERVICE_NAME, signal="CallbackData")
	public void CallbackData(int transactionId, String module,
			byte[] rawData, int totalParts, int partNumber) {
		APICore.getInstance().EnableConcurrentCallbacks();
		Integer key = Integer.valueOf(transactionId);
		if(transactionList.containsKey(key)) {
			TransactionHandler th = transactionList.get(key);
			th.HandleTransaction(null, rawData, totalParts, partNumber);
		}
	}
	
	@Override
	public String getObjectPath() {
		return this.OBJECT_PATH;
	}

	
	// Service-specific signal handlers/callbacks:
	@BusSignalHandler(iface=FileTransferCallbackInterface.SERVICE_NAME, signal="onTransferComplete")
	public void onTransferComplete(String service, String path, String mtype, String localpath) throws BusException {
		APICore.getInstance().EnableConcurrentCallbacks();
		if(listener != null)
			listener.onTransferComplete(service, path, mtype, localpath);
	}

	@BusSignalHandler(iface=FileTransferCallbackInterface.SERVICE_NAME, signal="onFileOffered")
	public void onFileOffered(String peer, String filename, String path)
			throws BusException {
		APICore.getInstance().EnableConcurrentCallbacks();
		if(listener != null)
			listener.onFileOffered(peer, filename, path);
	}	
}
