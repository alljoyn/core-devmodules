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
package org.alljoyn.devmodules.api.debug;

import java.util.ArrayList;

import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.devmodules.APICore;
import org.alljoyn.devmodules.api.groups.GroupsListener;
import org.alljoyn.devmodules.callbacks.CallbackObjectBase;
import org.alljoyn.devmodules.callbacks.TransactionHandler;

import android.util.Log;

public class DebugCallbackObject extends CallbackObjectBase implements DebugCallbackInterface, BusObject {
	static DebugListener mListener;
	
	public static final String TAG = "DebugCallbackObject";
	
	/**
	 * Register a mListener object which will be called if any of the associated signals are found
	 * @param mListener the GroupListener object to register
	 */
	public void registerListener (DebugListener listener){
		Log.i(TAG, "Debug Listener registered");
		mListener = listener;
	}
	
	@BusSignalHandler(iface=DebugCallbackInterface.SERVICE_NAME, signal="onDebugServiceAvailable")
	public void onDebugServiceAvailable(String service) {
		APICore.getInstance().EnableConcurrentCallbacks();
		mListener.onDebugServiceAvailable(service);
	}
	
	@BusSignalHandler(iface=DebugCallbackInterface.SERVICE_NAME, signal="onDebugServiceLost")
    public void onDebugServiceLost(String service) {
		APICore.getInstance().EnableConcurrentCallbacks();
		mListener.onDebugServiceLost(service);
	}
	
	@BusSignalHandler(iface=DebugCallbackInterface.SERVICE_NAME, signal="onDebugMessage")
    public void onDebugMessage(String service, int level, String message) {
		Log.v(TAG, "onDebugMessage("+message+")");
		APICore.getInstance().EnableConcurrentCallbacks();
		mListener.onDebugMessage(service, level, message);
	}

	@BusSignalHandler(iface=DebugCallbackInterface.SERVICE_NAME, signal="CallbackJSON")
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
	
	@BusSignalHandler(iface=DebugCallbackInterface.SERVICE_NAME, signal="CallbackData")
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
