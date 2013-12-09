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
package org.alljoyn.devmodules.api.profilemanager;

import java.util.ArrayList;

import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.bus.annotation.BusSignalHandler;

import org.alljoyn.devmodules.APICore;
import org.alljoyn.devmodules.api.groups.GroupsListener;
import org.alljoyn.devmodules.callbacks.CallbackObjectBase;
import org.alljoyn.devmodules.callbacks.TransactionHandler;

import android.util.Log;

public class ProfileManagerCallbackObject extends CallbackObjectBase implements ProfileManagerCallbackInterface, BusObject {

	// list of registered callbacks
	static ArrayList<ProfileManagerListener> mListenerList = new ArrayList<ProfileManagerListener>();
	//static ProfileManagerListener listener;
	private static final String TAG = "ProfileManagerCallbackObject";


	/**
	 * Register a listener object which will be called if any of the associated signals are found
	 * @param listener the GroupListener object to register
	 */
	public void registerListener (ProfileManagerListener listener){
		if (mListenerList==null){
			mListenerList.clear();
		}

		if (listener != null){
			mListenerList.add(listener);
		} else {
			Log.e(TAG, "registerListener() Null listener supplied");
		}
	}
	@BusSignalHandler(iface=ProfileManagerCallbackInterface.SERVICE_NAME, signal="onProfileFound")
	public void onProfileFound(String peer) {
		APICore.getInstance().EnableConcurrentCallbacks();
		Log.v(TAG, "onProfileFound("+peer+")");
		for (ProfileManagerListener listener: mListenerList){
			if (listener!=null) {
				listener.onProfileFound(peer);
			}
		}
	}

	@BusSignalHandler(iface=ProfileManagerCallbackInterface.SERVICE_NAME, signal="onProfileLost")
	public void onProfileLost(String peer) {
		APICore.getInstance().EnableConcurrentCallbacks();
		for (ProfileManagerListener listener: mListenerList){
			if (listener!=null) {
				listener.onProfileLost(peer);
			}
		}
	}

	@BusSignalHandler(iface=ProfileManagerCallbackInterface.SERVICE_NAME, signal="CallbackJSON")
	public void CallbackJSON(int transactionId, String module,
			String jsonCallbackData) {
		APICore.getInstance().EnableConcurrentCallbacks();
		Integer key = Integer.valueOf(transactionId);
		if(transactionList.containsKey(key)) {
			TransactionHandler th = transactionList.get(key);
			th.HandleTransaction(jsonCallbackData, null, 0, 0);
		}
	}

	@BusSignalHandler(iface=ProfileManagerCallbackInterface.SERVICE_NAME, signal="CallbackData")
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
}
