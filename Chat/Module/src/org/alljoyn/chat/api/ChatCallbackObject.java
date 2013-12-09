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

import java.util.ArrayList;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.devmodules.APICore;
import org.alljoyn.devmodules.callbacks.CallbackObjectBase;
import org.alljoyn.devmodules.callbacks.TransactionHandler;

import android.util.Log;

public class ChatCallbackObject extends CallbackObjectBase implements ChatCallbackInterface, BusObject {
	//static ChatListener listener;
	// list of registered callbacks
	static ArrayList<ChatListener> mListenerList = new ArrayList<ChatListener>();

	private static final String TAG = "ChatCallbackObject";

	
	/**
	 * Register a listener object which will be called if any of the associated signals are found
	 * @param listener the ChatListener object to register
	 */
	public void registerListener (ChatListener listener){
		if (listener != null){
			mListenerList.add(listener);
		} else {
			Log.e(TAG, "registerListener() Null listener supplied");
		}
	}

	
	@BusSignalHandler(iface=ChatCallbackInterface.SERVICE_NAME, signal="onChatRoomFound")
	public void onChatRoomFound(String room, String[] users) {
		APICore.getInstance().EnableConcurrentCallbacks();
		for (ChatListener listener: mListenerList){
			if (listener!=null) {
				listener.onChatRoomFound(room, users);
			} else {
				Log.e(TAG, "NULL Listener");
			}
		}
	}

	@BusSignalHandler(iface=ChatCallbackInterface.SERVICE_NAME, signal="onChatRoomLost")
	public void onChatRoomLost(String room) {
		APICore.getInstance().EnableConcurrentCallbacks();
		for (ChatListener listener: mListenerList){
			if (listener!=null) {
				listener.onChatRoomLost(room);
			} else {
				Log.e(TAG, "NULL Listener");
			}
		}
	}

	@BusSignalHandler(iface=ChatCallbackInterface.SERVICE_NAME, signal="onChatMsg")
	public void onChatMsg(String room, String user, String msg) {
		APICore.getInstance().EnableConcurrentCallbacks();
		for (ChatListener listener: mListenerList){
			if (listener!=null) {
				listener.onChatMsg(room, user, msg);
			} else {
				Log.e(TAG, "NULL Listener");
			}
		}
	}
	
	@Override
	@BusSignalHandler(iface=ChatCallbackInterface.SERVICE_NAME, signal="onGroupChatMsg")
	public void onGroupChatMsg(String groupId, String user, String msg)
			throws BusException {
		APICore.getInstance().EnableConcurrentCallbacks();
		for (ChatListener listener: mListenerList){
			if (listener!=null) {
				listener.onGroupChatMsg(groupId, user, msg);
			} else {
				Log.e(TAG, "NULL Listener");
			}
		}
	}
	
	@BusSignalHandler(iface=ChatCallbackInterface.SERVICE_NAME, signal="CallbackJSON")
	public void CallbackJSON(int transactionId, String module,
			String jsonCallbackData) {
		APICore.getInstance().EnableConcurrentCallbacks();
		Log.d(this.getClass().getName(),"callback id("+transactionId+") data: "+jsonCallbackData);
		Integer key = Integer.valueOf(transactionId);
		if(transactionList.containsKey(key)) {
			Log.d(this.getClass().getName(),"calling transactionHandler!!!");
			TransactionHandler th = transactionList.get(key);
			th.HandleTransaction(jsonCallbackData, null, 0, 0);
		}
	}
	
	@BusSignalHandler(iface=ChatCallbackInterface.SERVICE_NAME, signal="CallbackData")
	public void CallbackData(int transactionId, String module,
			byte[] rawData, int totalParts, int partNumber) {
		APICore.getInstance().EnableConcurrentCallbacks();
		Integer key = Integer.valueOf(transactionId);
		if(transactionList.containsKey(key)) {
			Log.d(this.getClass().getName(),"calling transactionHandler!!!");
			TransactionHandler th = transactionList.get(key);
			th.HandleTransaction(null, rawData, totalParts, partNumber);
		}
	}
	
	@Override
	public String getObjectPath() {
		return this.OBJECT_PATH;
	}

}
