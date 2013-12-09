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
package org.alljoyn.api.filetransfer;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.devmodules.interfaces.ModuleAPIInterface;

public class FileTransferAPIImpl implements ModuleAPIInterface {
	//callback interface so that specific callback can be invoked
	//Signal emitter - so we have multiple apps that receive callback data
	
	class FileTransferCallbackObject implements FileTransferCallbackInterface, BusObject {

		public void CallbackJSON(int transactionId, String module,
				String jsonCallbackData) {
			
		}

		public void CallbackData(int transactionId, String module,
				byte[] rawData, int totalParts, int partNumber) {
			
		}
		//These methods are blank and only used because they must be added
		//in order to invoke signals

		public void onTransferComplete(String service, String path,
				String mtype, String localpath) throws BusException {
			
		}

		public void onFileOffered(String peer, String filename, String path)
				throws BusException {
			
		}
	}
	
	private FileTransferCallbackObject callbackObject = new FileTransferCallbackObject();
	public static FileTransferCallbackInterface callback; //look into possibly just folding this in with the regular impl so I don't have to static var this and link it with the ChatImpl
	
	public FileTransferAPIImpl() {
	}

	@Override
	public void connectCallbackObject(int sessionId, String joiner) {
		SignalEmitter emitter = new SignalEmitter(callbackObject, sessionId, SignalEmitter.GlobalBroadcast.Off);
		callback = emitter.getInterface(FileTransferCallbackInterface.class);
	}

	//Standard do not alter
	@Override
	public BusObject getBusObject() {
		return new FileTransferAPIObject();
	}

	@Override
	public String getBusObjectPath() {
		return FileTransferAPIObject.OBJECT_PATH;
	}

	@Override
	public BusObject getCallbackBusObject() {
		// TODO Auto-generated method stub
		return callbackObject;
	}

	@Override
	public String getCallbackBusObjectPath() {
		// TODO Auto-generated method stub
		return callbackObject.OBJECT_PATH;
	}
	
}
