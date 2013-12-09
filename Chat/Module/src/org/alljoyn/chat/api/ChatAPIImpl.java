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
package org.alljoyn.chat.api;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.devmodules.interfaces.ModuleAPIInterface;

public class ChatAPIImpl implements ModuleAPIInterface {
	//callback interface so that specific callback can be invoked
	//Signal emitter - so we have multiple apps that receive callback data
	
	class ChatCallbackObject implements ChatCallbackInterface, BusObject {
		public void onChatRoomFound(String room, String[] users) {	}

		public void onChatRoomLost(String room) {	}

		public void onChatMsg(String room, String user, String msg) {	}
		
		public void onGroupChatMsg(String groupId, String user, String msg) {	}

		public void CallbackJSON(int transactionId, String module,
				String jsonCallbackData) {
			
		}

		public void CallbackData(int transactionId, String module,
				byte[] rawData, int totalParts, int partNumber) {
			
		}	
	}
	
	private ChatCallbackObject chatCallbackObject = new ChatCallbackObject();
	public static ChatCallbackInterface chatCallback; //look into possibly just folding this in with the regular impl so I don't have to static var this and link it with the ChatImpl
	
	public ChatAPIImpl() {
	}

	@Override
	public void connectCallbackObject(int sessionId, String joiner) {
		SignalEmitter emitter = new SignalEmitter(chatCallbackObject, sessionId, SignalEmitter.GlobalBroadcast.Off);
		chatCallback = emitter.getInterface(ChatCallbackInterface.class);
	}

	@Override
	public BusObject getBusObject() {
		return new ChatAPIObject();
	}

	@Override
	public String getBusObjectPath() {
		return ChatAPIObject.OBJECT_PATH;
	}

	@Override
	public BusObject getCallbackBusObject() {
		// TODO Auto-generated method stub
		return chatCallbackObject;
	}

	@Override
	public String getCallbackBusObjectPath() {
		// TODO Auto-generated method stub
		return chatCallbackObject.OBJECT_PATH;
	}
	
}
