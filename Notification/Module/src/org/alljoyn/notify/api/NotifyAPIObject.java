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
package org.alljoyn.notify.api;

import org.alljoyn.devmodules.common.NotificationData;
import org.alljoyn.devmodules.notify.NotifyImpl;

import java.util.LinkedList;
import java.util.Queue;

import org.alljoyn.bus.*;
import org.alljoyn.bus.AuthListener.AuthRequest;
import org.alljoyn.bus.AuthListener.PasswordRequest;
import org.alljoyn.bus.AuthListener.UserNameRequest;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.bus.annotation.BusSignalHandler;

import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.hardware.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class NotifyAPIObject implements NotifyAPIInterface, BusObject {
	public static final String OBJECT_PATH = "notify";
	
	private ConnectorHandler handler = new ConnectorHandler();
	private static final int ON_SEND_NOTIFY = 0;
	
	private static final String TAG = "NotifyAPIObject" ;

	public void NotifyAll(NotificationData data)
			throws BusException {
		try{
			Log.d(TAG,"Received request to send a notification");
			Message msg = handler.obtainMessage(ON_SEND_NOTIFY);
			msg.obj = data;
			handler.sendMessage(msg);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void NotifyGroup(NotificationData data, String groupId)
			throws BusException {
		try{
			Log.d(TAG,"Received request to send a notification");
			Message msg = handler.obtainMessage(ON_SEND_NOTIFY);
			msg.obj = data;
			Bundle b = new Bundle();
			b.putString("groupId", groupId);
			msg.setData(b);
			handler.sendMessage(msg);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void NotifyUser(NotificationData data, String userId)
			throws BusException {
		try{
			Log.d(TAG,"Received request to send a notification");
			Message msg = handler.obtainMessage(ON_SEND_NOTIFY);
			msg.obj = data;
			Bundle b = new Bundle();
			b.putString("userId", userId);
			msg.setData(b);
			handler.sendMessage(msg);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private class ConnectorHandler extends Handler
    {
		public void handleMessage(Message msg) {
			NotifyImpl impl = NotifyImpl.getInstance();
			Bundle data = msg.getData();
			if(impl == null) {
				Log.i(TAG,"impl or data is null!");
				return;
			}
			switch(msg.what) {
				case ON_SEND_NOTIFY:
					Log.i(TAG,"placing call to the impl");
					impl.sendNotification((NotificationData)msg.obj,
							data != null ? data.getString("groupId") : null,
									data != null ? data.getString("userId") : null);
					break;
			}
		}
    }	
}
