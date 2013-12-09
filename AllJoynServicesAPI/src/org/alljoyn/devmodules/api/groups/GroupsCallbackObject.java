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
package org.alljoyn.devmodules.api.groups;

import java.util.ArrayList;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.bus.annotation.BusSignalHandler;

import org.alljoyn.devmodules.APICore;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerCallbackInterface;
import org.alljoyn.devmodules.callbacks.CallbackObjectBase;
import org.alljoyn.devmodules.callbacks.TransactionHandler;
import org.alljoyn.devmodules.common.MediaIdentifier;

import android.util.Log;

// This class maps signals from the background service to callbacks to the app
// In general, each method handles a Signal from the background service and calls
// the corresponding Listener callback method

public class GroupsCallbackObject extends CallbackObjectBase implements GroupsCallbackInterface, BusObject {

	// list of registered callbacks
	static ArrayList<GroupsListener> mListenerList = null;

	//GroupsListener listener = null; // registered callbacks. Not static so that there can be multiple callbacks

	private static final String TAG = "GroupsCallbackObject";

	public GroupsCallbackObject(){
		Log.v(TAG, "GroupsCallbackObject()");
	}

	/**
	 * Register a listener object which will be called if any of the associated signals are found
	 * @param listener the GroupListener object to register
	 */
	public void registerListener (GroupsListener listener){
		if (mListenerList==null){
			mListenerList = new ArrayList<GroupsListener>();
			mListenerList.clear();
		}

		if (listener != null){
			mListenerList.add(listener);
		} else {
			Log.e(TAG, "registerListener() Null listener supplied");
		}
	}

	// Generic callbacks
	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="CallbackJSON")
	public void CallbackJSON(int transactionId, String module,
			String jsonCallbackData) {
		Log.d(this.getClass().getName(),"callback id("+transactionId+") data: "+jsonCallbackData);
		Integer key = Integer.valueOf(transactionId);
		if(transactionList.containsKey(key)) {
			Log.d(this.getClass().getName(),"calling transactionHandler!!!");
			TransactionHandler th = transactionList.get(key);
			th.HandleTransaction(jsonCallbackData, null, 0, 0);
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="CallbackData")
	public void CallbackData(int transactionId, String module,
			byte[] rawData, int totalParts, int partNumber) {
		Integer key = Integer.valueOf(transactionId);
		if(transactionList.containsKey(key)) {
			TransactionHandler th = transactionList.get(key);
			th.HandleTransaction(null, rawData, totalParts, partNumber);
		}
	}

	@Override
	public String getObjectPath() {
		return GroupsCallbackInterface.OBJECT_PATH;
	}



	// Service-specific signal handlers/callbacks
	// These typically just execute the appropriate registered callback

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupActive")
	public void onGroupActive(String group) {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupActive()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupActive(group);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupInactive")
	public void onGroupInactive(String group) {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupActive()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupInactive(group);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupInvitation")
	public void onGroupInvitation(String group, String originator) {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupInvitation()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupInvitation(group, originator);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupInvitationAccepted")
	public void onGroupInvitationAccepted(String group, String id)throws BusException {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupInvitationAccepted()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupInvitationAccepted(group, id);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupInvitationRejected")
	public void onGroupInvitationRejected(String group, String id)throws BusException {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupInvitationRejected()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupInvitationRejected(group, id);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupInvitationTimeout")
	public void onGroupInvitationTimeout(String group) {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupInvitationTimeout()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupInvitationTimeout(group);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupAdded")
	public void onGroupAdded(String group) {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupAdded()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupAdded(group);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupRemoved")
	public void onGroupRemoved(String group) {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupRemoved()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupRemoved(group);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupMemberJoined")
	public void onGroupMemberJoined(String group, String id)throws BusException {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupMemberJoined()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupMemberJoined(group, id);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupMemberLeft")
	public void onGroupMemberLeft(String group, String id) {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupMemberLeft()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupMemberLeft(group, id);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupEnabled")
	public void onGroupEnabled(String group) {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupEnabled()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupEnabled(group);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}

	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onGroupDisabled")
	public void onGroupDisabled(String group) {
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onGroupDisabled()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onGroupDisabled(group);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}


	@BusSignalHandler(iface=GroupsCallbackInterface.SERVICE_NAME, signal="onTestResult")
	public void onTestResult (String results){
		APICore.getInstance().EnableConcurrentCallbacks();
		if (mListenerList != null){
			Log.v(TAG, "onTestResult()");
			for (GroupsListener listener: mListenerList){
				if (listener!=null) {
					listener.onTestResult(results);
				} else {
					Log.e(TAG, "NULL Listener");
				}
			}
		}
	}
}
