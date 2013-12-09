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
package org.alljoyn.devmodules.debug.api;

import org.alljoyn.devmodules.debug.*;

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

public class DebugAPIObject implements DebugAPIInterface, BusObject {
	public static final String OBJECT_PATH = "debug";
	
	private HandlerThread handlerThread = new HandlerThread(OBJECT_PATH);
	{handlerThread.start();}
	private ConnectorHandler handler = new ConnectorHandler(handlerThread.getLooper());
	
	////////////////////////////////////////////////
	// Control of Local Service 
	////////////////////////////////////////////////
	
	private static final String TAG = "DebugConnector" ;

	// Get current filter string (OS-Specific)
	public String GetFilter() {
		return "";//DebugObject.mFilter;
	}

	// Set filter string (OS-Specific)
	public void SetFilter(String filter) {
		//DebugObject.mFilter = filter;
	}
	
	////////////////////////////////////////////////
	// Interaction with Remote Debug Services
	////////////////////////////////////////////////
	
	// Connect to a particular service (just use prefix for all services)
	public void Connect (String device, String service) {
		Log.d(TAG, "Connect("+device+","+service+")");	
		handler.sendMessage(handler.obtainMessage(0, device));
	}
	
	
	// Disconnect from a particular service
	public void Disconnect(String device, String service) {
		Log.d(TAG, "Disconnect("+device+","+service+")");	
		handler.sendMessage(handler.obtainMessage(1, device));
	}

	// List connected services
	public String[] GetServiceList() {
		Log.d(TAG, "GetServiceList()");
		DebugImpl impl = DebugImpl.getInstance();
		return impl.getDebugSessions();
	}
	
	// Get the list of messages received from a particular service
	public DebugMessageDescriptor[] GetMessageList (String service) {
		Log.d(TAG, "GetMessageList("+service+")");
//		return DebugGlobalData.mRemoteDebugList.getAll(service);
		return null;
	}
	
	private class ConnectorHandler extends Handler
    {
		public ConnectorHandler(Looper loop) {
			super(loop);
		}
		public void handleMessage(Message msg) {
			DebugImpl impl = DebugImpl.getInstance();
			switch(msg.what) {
				case 0:
					try {
						impl.ConnectToDevice((String)msg.obj);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case 1:
					try {
						impl.DisconnectFromDevice((String)msg.obj);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
			}
		}
    }

	public boolean IsEnabled() throws BusException {
		// TODO Auto-generated method stub
		return false;
	}

	public void Enable() throws BusException {
		// TODO Auto-generated method stub
		
	}

	public void Disable() throws BusException {
		// TODO Auto-generated method stub
		
	}
}
