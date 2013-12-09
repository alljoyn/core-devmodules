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
package org.alljoyn.groups.api;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.devmodules.interfaces.ModuleAPIInterface;

import android.util.Log;

public class GroupsAPIImpl implements ModuleAPIInterface {
	//callback interface so that specific callback can be invoked
	//Signal emitter - so we have multiple apps that receive callback data

	private static String TAG = "GroupsAPIImpl";
	private static GroupsAPIImpl mInstance;

	class GroupsCallbackObject implements GroupsCallbackInterface, BusObject {

		//These methods are blank and only here because they must be declared
		//in order to invoke signals

		public void CallbackJSON(int transactionId, String module, String jsonCallbackData) {}

		public void CallbackData(int transactionId, String module, byte[] rawData, int totalParts, int partNumber) {}

		public void onGroupInvitation(String group, String originator){}

		public void onGroupInvitationAccepted(String group, String id) {}

		public void onGroupInvitationRejected(String group, String id) {}

		public void onGroupInvitationTimeout(String group) {}

		public void onGroupAdded(String group) {}

		public void onGroupRemoved(String group) {}

		public void onGroupActive(String group) {}

		public void onGroupInactive(String group) {}

		public void onGroupDeactivated(String group) {}

		public void onGroupMemberJoined(String group, String id) {}

		public void onGroupMemberLeft(String group, String id) {}

		public void onGroupEnabled(String group) {}

		public void onGroupDisabled(String group) {}

		public void onTestResult(String results) {}
	}

	private static GroupsCallbackObject mCallbackObject = null;
	private static GroupsAPIObject mAPIObject=null;
	//private static GroupsCallbackInterface mCallbackInterface=null; //look into possibly just folding this in with the regular impl so I don't have to static var this and link it with the ChatImpl
	public static GroupsCallbackInterface mCallbackInterface=null; //look into possibly just folding this in with the regular impl so I don't have to static var this and link it with the ChatImpl

	public GroupsAPIImpl() {
		try{
			Log.v(TAG, "GroupsAPIImpl()");
			mInstance = this;
			if (mCallbackObject==null) mCallbackObject = new GroupsCallbackObject();
			if (mAPIObject==null) mAPIObject = new GroupsAPIObject();
		} catch (Exception e){
			Log.e(TAG, "GroupsAPIImpl() exception: "+e.toString());
		}
	}

	private static int    mSessionId=0;
	private static String mJoiner="";

	@Override
	public void connectCallbackObject(int sessionId, String joiner) {
		try{
			Log.v(TAG, "connectCallbackObject("+sessionId+", "+joiner+")");
			mSessionId = sessionId;
			mJoiner = joiner;
			if (mInstance==null) mInstance = this;
			if (mCallbackObject==null) mCallbackObject = new GroupsCallbackObject();
			if (mCallbackInterface==null){
				//SignalEmitter emitter = new SignalEmitter(mCallbackObject, joiner, sessionId, SignalEmitter.GlobalBroadcast.Off);
				SignalEmitter emitter = new SignalEmitter(mCallbackObject, mJoiner, mSessionId, SignalEmitter.GlobalBroadcast.On);
				mCallbackInterface = emitter.getInterface(GroupsCallbackInterface.class);
				Log.v(TAG, "connectCallbackObject() Created interface for object: "+mCallbackObject.OBJECT_PATH);
				if (mCallbackInterface==null){
					Log.w(TAG, "GroupsCallback interface is NULL");
				}
			}
		} catch (Exception e){
			Log.e(TAG, "connectCallbackObject() error: "+e.toString());
		}
	}

	//Standard do not alter
	@Override
	public BusObject getBusObject() {
		if (mAPIObject==null) mAPIObject = new GroupsAPIObject();
		return mAPIObject;
	}

	@Override
	public String getBusObjectPath() {
		return GroupsAPIObject.OBJECT_PATH;
	}


	@Override
	public BusObject getCallbackBusObject() {
		if (mCallbackObject==null) mCallbackObject = new GroupsCallbackObject();
		return mCallbackObject;
	}

	@Override
	public String getCallbackBusObjectPath() {
		if (mCallbackObject==null) mCallbackObject = new GroupsCallbackObject();
		return mCallbackObject.OBJECT_PATH;
	}

	public static GroupsAPIImpl getInstance(){
		if (mInstance==null){
			Log.w(TAG, "getInstance() mInstance==null !!!!");
		}
		return mInstance;
	}

	public GroupsCallbackInterface getCallbackInterface() {
		if (mCallbackInterface==null) {
			try{
				if (mCallbackObject==null) mCallbackObject = new GroupsCallbackObject();
				SignalEmitter emitter = new SignalEmitter(mCallbackObject, mJoiner, mSessionId, SignalEmitter.GlobalBroadcast.Off);
				mCallbackInterface = emitter.getInterface(GroupsCallbackInterface.class);
				Log.v(TAG, "getCallbackInterface() Created interface for object: "+mCallbackObject.OBJECT_PATH);
			} catch (Exception e){
				Log.e(TAG, "getCallbackInterface() error: "+e.toString());
			}
		}
		return mCallbackInterface;
	}


}
