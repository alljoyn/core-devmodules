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

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.devmodules.common.WhiteboardLineInfo;
import org.alljoyn.devmodules.interfaces.ModuleAPIInterface;

public class WhiteboardAPIImpl implements ModuleAPIInterface {
	//callback interface so that specific callback can be invoked
	//Signal emitter - so we have multiple apps that receive callback data
	
	class ThisWhiteboardCallbackObject implements WhiteboardCallbackInterface, BusObject {
		public void CallbackJSON(int transactionId, String module,
				String jsonCallbackData) {
			
		}

		public void CallbackData(int transactionId, String module,
				byte[] rawData, int totalParts, int partNumber) {
			
		}

	
		public void onDraw(WhiteboardLineInfo lineInfo) {
			
		}
		
		public void onGroupDraw(String groupId, WhiteboardLineInfo lineInfo) {
			
		}

		public void onClear() {
			
		}		
		
		public void onGroupClear(String groupId) {
			
		}
	}
	
	private ThisWhiteboardCallbackObject callbackObject = new ThisWhiteboardCallbackObject();
	public static WhiteboardCallbackInterface callback; //look into possibly just folding this in with the regular impl so I don't have to static var this and link it with the WhiteboardImpl
	
	public WhiteboardAPIImpl() {
	}

	@Override
	public void connectCallbackObject(int sessionId, String joiner) {
		//System.out.println("Connected callback object so we should be able to broadcast a callback signal");
		SignalEmitter emitter = new SignalEmitter(callbackObject, sessionId, SignalEmitter.GlobalBroadcast.Off);
		callback = emitter.getInterface(WhiteboardCallbackInterface.class);
	}

	@Override
	public BusObject getBusObject() {
		return new WhiteboardAPIObject();
	}

	@Override
	public String getBusObjectPath() {
		return WhiteboardAPIObject.OBJECT_PATH;
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
