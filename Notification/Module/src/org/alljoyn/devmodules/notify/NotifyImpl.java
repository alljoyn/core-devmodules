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
package org.alljoyn.devmodules.notify;

import java.util.ArrayList;
import org.alljoyn.bus.*;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.notify.api.NotifyAPIImpl;
import org.alljoyn.devmodules.common.NotificationData;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.alljoyn.devmodules.interfaces.ModuleInterface;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NotifyImpl extends BusListener implements ModuleInterface  {
	private final String TAG = "NotifyImpl";

	private static NotifyImpl instance;
	public static NotifyImpl getInstance() { return instance; }

	private NotifyObject notifyObject = new NotifyObject();
	private AllJoynContainerInterface alljoynContainer;
	private String myWellknownName;

	private ArrayList<String> remotekeyPeerWellKnowns = new ArrayList<String>();
	private Context mContext;
	private String namePrefix;

	public NotifyImpl(AllJoynContainerInterface alljoynContainer, Context context) {
		this.alljoynContainer = alljoynContainer;
		instance = this;
		mContext = context;
		SetupSession();
	}

	public void RegisterSignalHandlers() {
		Status status = alljoynContainer.getBusAttachment().addMatch("sessionless='t'");
		Log.i(NotifyConstants.COMPONENT_NAME,"added sessionless match: "+status);
		
		status = alljoynContainer.getBusAttachment().registerSignalHandlers(this);
		if(status == Status.OK) {
			Log.i(NotifyConstants.COMPONENT_NAME, "registered signal hanlders");
		} else {
			Log.i(NotifyConstants.COMPONENT_NAME, "ERROR! register signal hanlders: "+status);
		}
	}

	public void SetupSession() {
		
	}

	public BusObject getBusObject() { return notifyObject; }

	public String getObjectPath() {
		return NotifyConstants.OBJECT_PATH;
	}

	public String getAdvertisedName() {
		if(myWellknownName == null) {
			myWellknownName = NotifyConstants.SERVICE_NAME+"."+alljoynContainer.getUniqueID();
		}
		return myWellknownName;
	}

	public String getServiceName() {
		return NotifyConstants.SERVICE_NAME;
	}

	public void foundAdvertisedName(String name, short transport, String namePrefix) {

	}

	public void lostAdvertisedName(String name, short transport, String namePrefix) {

	}

	public void shutdown() {

	}

	public ArrayList<String> getPeers() { return remotekeyPeerWellKnowns; }

	/* ************************************** */
	/* Module specific implementation methods */
	/* ************************************** */
	private NotifyInterface signalSender;
	public void sendNotification(NotificationData data, String groupId, String userId) {
		try {
			if(groupId != null) {
				//Send regular Signal on a Group
				//place call to get sessionId for group
				int sessionId = alljoynContainer.getGroupSessionId(groupId);
				SignalEmitter em = new SignalEmitter(getBusObject(), sessionId, SignalEmitter.GlobalBroadcast.On);
				em.getInterface(NotifyInterface.class).sendNotification(data);
			} else if(userId != null) {
				
			}
			else {
				if(signalSender == null) {
					SignalEmitter em = new SignalEmitter(getBusObject(), SignalEmitter.GlobalBroadcast.On);
					em.setSessionlessFlag(true);
					signalSender = em.getInterface(NotifyInterface.class);
				}
				Log.d(TAG, "sending the notification: "+data.msg);
				signalSender.sendNotification(data);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@BusSignalHandler(iface=NotifyConstants.NAME_PREFIX, signal="sendNotification")
	public void onReceivedNotification(NotificationData data) {
		try{
			alljoynContainer.getBusAttachment().enableConcurrentCallbacks();
			MessageContext ctx = alljoynContainer.getBusAttachment().getMessageContext();
			Log.d(TAG, "Received a notification: "+data.msg);
			//if(!ctx.sender.equals(alljoynContainer.getBusAttachment().getUniqueName())) {
				NotifyAPIImpl.callback.onNotification(ctx.sender, data);
				Log.d(TAG, "Did I send up through framework to callback object? sender:"+ctx.sender);
			//} else {
			//	Log.d(TAG, "This was sent by me");
			//}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void InitAPI(AllJoynContainerInterface coreLogic) {
		// TODO Auto-generated method stub
		setupSession();
	}


	/* ****************************************************************** */
	/* Module specific implementation methods (running on handler thread) */
	/* ****************************************************************** */

	//Define constants for Handler events
	private static final int SETUP_SESSION  = 1; 


	private void setupSession (){
		mHandler.sendEmptyMessage(SETUP_SESSION);	
	}


	// Use this Handler for asynchronous transactions
	// Note that there may be several transactions active in parallel, so don't assume a single user

	private ImplHandler mHandler = new ImplHandler(); // Handler for complex functions

	private class ImplHandler extends Handler
	{
		public void handleMessage(Message msg) {
			// frequently-used variables
			String service;
			Bundle data = msg.getData();

			switch(msg.what) {
			case SETUP_SESSION: {
				SetupSession();
				break;
			}
			default:
				Log.e(TAG, "ImplHandler unknown msg type: "+msg.what);
				break;

			} // switch
		} // handleMessage
	}// ImplHandler

}
