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

import org.alljoyn.devmodules.remotecontrol.RemoteControlImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

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
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class RemoteControlAPIObject implements RemoteControlAPIInterface, BusObject {
	public static final String OBJECT_PATH = "remotecontrol";
	
	private HandlerThread handlerThread = new HandlerThread(OBJECT_PATH);
	{handlerThread.start();}
	private ConnectorHandler handler = new ConnectorHandler(handlerThread.getLooper());
	private static final int ON_KEY_DOWN = 0;
	private static final int SEND_INTENT = 1;
	
	private static final String TAG = "RemoteControlConnector" ;

	public void onKeyDown(String groupId, String peer, int keyCode)
			throws BusException {
		try{
			Log.i(TAG, "onKeyDown!!! peer:"+peer);
			Message msg = handler.obtainMessage(ON_KEY_DOWN);
			Bundle data = new Bundle();
			data.putString("groupId", groupId);
			data.putString("peer", peer);
			data.putInt("keyCode", keyCode);
			msg.setData(data);
			handler.sendMessage(msg);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendIntent(String groupId, String peer, String intentAction, String intentData)
			throws BusException {
		try{
			Log.i(TAG, "onKeyDown!!! peer:"+peer);
			Message msg = handler.obtainMessage(SEND_INTENT);
			Bundle data = new Bundle();
			data.putString("groupId", groupId);
			data.putString("peer", peer);
			data.putString("intentAction", intentAction);
			data.putString("intentData", intentData);
			msg.setData(data);
			handler.sendMessage(msg);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	
	private class ConnectorHandler extends Handler
    {
		public ConnectorHandler(Looper loop) {
			super(loop);
		}
		public void handleMessage(Message msg) {
			RemoteControlImpl impl = RemoteControlImpl.getInstance();
			Bundle data = msg.getData();
			if(impl == null || data == null) {
				Log.i(TAG,"impl or data is null!");
				return;
			}
			switch(msg.what) {
				case ON_KEY_DOWN:
					impl.sendRemoteKey(data.getString("groupId"), data.getString("peer"), data.getInt("keyCode"));
					break;
				case SEND_INTENT:
					Log.i(TAG, "placing call to sendIntent");
					impl.sendIntent(data.getString("groupId"), data.getString("peer"), data.getString("intentAction"), data.getString("intentData"));
					break;
			}
		}
    }	
}
