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

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.devmodules.interfaces.ModuleAPIInterface;

public class RemoteControlAPIImpl implements ModuleAPIInterface {
	//callback interface so that specific callback can be invoked
	//Signal emitter - so we have multiple apps that receive callback data
	
	class RemoteControlCallbackObject implements RemoteControlCallbackInterface, BusObject {
		public void onKeyDown(String peer, int keyCode) throws BusException {
			
		}

		public void executeIntent(String groupId, String intentAction, String intentData) throws BusException {
			
		}		
	}
	
	private RemoteControlCallbackObject remoteControlCallbackObject = new RemoteControlCallbackObject();
	public static RemoteControlCallbackInterface remoteControlCallback; //look into possibly just folding this in with the regular impl so I don't have to static var this and link it with the ChatImpl
	
	public RemoteControlAPIImpl() {
	}

	@Override
	public void connectCallbackObject(int sessionId, String joiner) {
		SignalEmitter emitter = new SignalEmitter(remoteControlCallbackObject, sessionId, SignalEmitter.GlobalBroadcast.Off);
		remoteControlCallback = emitter.getInterface(RemoteControlCallbackInterface.class);
	}

	@Override
	public BusObject getBusObject() {
		return new RemoteControlAPIObject();
	}

	@Override
	public String getBusObjectPath() {
		return RemoteControlAPIObject.OBJECT_PATH;
	}

	@Override
	public BusObject getCallbackBusObject() {
		// TODO Auto-generated method stub
		return remoteControlCallbackObject;
	}

	@Override
	public String getCallbackBusObjectPath() {
		// TODO Auto-generated method stub
		return remoteControlCallbackObject.OBJECT_PATH;
	}
	
}
