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
package org.alljoyn.devmodules.debug;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.alljoyn.bus.*;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.devmodules.sessionmanager.SessionManagerListener;

import org.alljoyn.devmodules.debug.api.DebugAPIImpl;
//import org.alljoyn.devmodules.filetransfer.*;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.alljoyn.devmodules.interfaces.ModuleInterface;
import org.json.JSONObject;
//import org.alljoyn.devmodules.profile.ProfileCache;
//import org.alljoyn.devmodules.profile.ProfileUtilities;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class DebugImpl extends BusListener implements ModuleInterface {	

	////////////////////////////////////////////////////////////////////////////
	// Initialization process
	////////////////////////////////////////////////////////////////////////////

	private static final String TAG = "DebugImpl";

	private static DebugImpl instance;
	public static DebugImpl getInstance() { return instance; }

	private AllJoynContainerInterface alljoynContainer;
	static Context context;

	public DebugObject debugObject = new DebugObject();
	//public static Map<String,ProxyBusObject> mProxyObjHash = new HashMap<String,ProxyBusObject>();
	private ArrayList<String> debugSessions = new ArrayList<String>();
	public DebugInterface mDebugInterface;
	public String myWellknownName;

	private static int mClientCount = 0;
	private String mFilter = "AndroidRuntime:E dalvikvm:E *:D";

	private int mySessionId = -1;

	public DebugImpl(AllJoynContainerInterface alljoynContainer, Context context) {
		this.alljoynContainer = alljoynContainer;
		this.context = context;
		instance = this;
		this.alljoynContainer.getSessionManager().addSessionManagerListener(new SessionManagerListener() {
			public void sessionLost(int sessionId) { 
				if(mySessionId == sessionId) {
					Log.i(TAG,"sessionLost: "+sessionId);
					mClientCount = 0;
				}
			}
			public void sessionMemberAdded(int sessionId, String uniqueName) { 
				if(mySessionId == sessionId) {
					Log.i(TAG,"Member Added! uniqueName: "+uniqueName);
				}
			} 
			public void sessionMemberRemoved(int sessionId, String uniqueName) {
				if(sessionId == mySessionId) {
					Log.i(TAG,"Member left!!! uniqueName: "+uniqueName);
					mClientCount--;
				}
			}
		});
	}

	public void RegisterSignalHandlers() {
		alljoynContainer.getBusAttachment().registerSignalHandlers(this);
		alljoynContainer.getBusAttachment().addMatch("type='signal',interface='"+DebugConstants.NAME_PREFIX+"',member='DebugMessage'");
	}

	public void SetupSession() {
		SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, false, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
		alljoynContainer.getSessionManager().createSession(getSessionName(getAdvertisedName()), DebugConstants.SESSION_PORT, new SessionPortListener() {
			public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
				Log.d(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
				if (sessionPort == DebugConstants.SESSION_PORT) {
					return true;
				}
				return false;
			}

			public void sessionJoined(short sessionPort, int id, String joiner) {
				Log.d(TAG, "User " + joiner + " joined., id=" + id);
				mySessionId = id;
				SignalEmitter emitter = new SignalEmitter(debugObject, id, SignalEmitter.GlobalBroadcast.On);
				mDebugInterface = emitter.getInterface(DebugInterface.class);
				try {
					mDebugInterface.DebugMessage(getAdvertisedName(), 0, "Starting Remote logging");
					Thread.sleep(100);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mClientCount = mClientCount > 0 ? mClientCount+1 : 1;
				Message msg = mHandler.obtainMessage(SEND_LOG_MSG);
				Bundle data = new Bundle();
				data.putString("service", "all");
				data.putInt("level", 0);
				data.putString("msg", "Not supported at this time");
				msg.setData(data);
				mHandler.sendMessage(msg);
//				if(!loggingThread.isAlive()) {
//					loggingThread.run();
//				}
			}
		}, sessionOpts);
		Log.i(TAG,"Advertised: "+getAdvertisedName());
	}

	public BusObject getBusObject() { return debugObject; }

	public String getObjectPath() {
		return DebugConstants.OBJECT_PATH;
	}

	public String getAdvertisedName() {
		if(myWellknownName == null) {
			myWellknownName = DebugConstants.SERVICE_NAME+"."+alljoynContainer.getUniqueID();
		}
		return myWellknownName;
	}

	public String getServiceName() {
		return DebugConstants.SERVICE_NAME;
	}

	public void foundAdvertisedName(String name, short transport, String namePrefix) {

		if(!name.contains(getAdvertisedName()) && name.contains(DebugConstants.NAME_PREFIX))
		{
			Log.i(TAG,"foundAdvertisedName: "+name);
			name = name.substring(namePrefix.length()+1);
			debugSessions.add(name);
		}
	}

	public void lostAdvertisedName(String name, short transport, String namePrefix) {
		if(!name.contains(getAdvertisedName()) && transport != SessionOpts.TRANSPORT_LOCAL) {
			Log.i(TAG, "lostAdvertisedName: "+name);
			debugSessions.remove(name);
		}
	}

	public String[] getDebugSessions() {
		return debugSessions.toArray(new String[debugSessions.size()]);
	}

	public void shutdown() {

	}

	/////////////////////////////////////////////////////////////////////////////////
	// Utilities for figuring out service names etc. from the ProfileID (unique name)
	/////////////////////////////////////////////////////////////////////////////////

	// Extract "ID" portion of typical service/session name
	private static String getUserID (String service){
		String id = service;
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
		return id;
	}

	// Utility to create the service name from the session or user ID
	private static String getServiceName(String session){
		return DebugConstants.NAME_PREFIX + "." + getUserID(session);
	}

	// Utility to create the service name from the service, session or user ID
	private static String getSessionName(String service){
		return DebugConstants.SERVICE_NAME + "." + getUserID(service);
	}


	/* ************************************** */
	/* Module specific implementation methods */
	/* ************************************** */

	public void ConnectToDevice(String device) {
		SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, false, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
		Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
		alljoynContainer.getSessionManager().joinSession(getSessionName(device), DebugConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());
	}

	public void DisconnectFromDevice(String device) {
		Log.d(TAG, "Should be leaving the session...");
		alljoynContainer.getSessionManager().leaveSession(getSessionName(device));
	}

	Thread loggingThread = new Thread() {
		@Override
		public void run() {
			BufferedReader bufferedReader=null;
			try {
				// check filter string 
				do {
					//Clear the log first
//					Process processClear = Runtime.getRuntime().exec("logcat -c");
//					Thread.sleep(2000);
					//Now get the latest info and then read the stream
					Process process = Runtime.getRuntime().exec("logcat -b events");
					/* Check different filters available at 
					 * http://developer.android.com/guide/developing/tools/logcat.html , 
					 */

					bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					String line;
					int    lvl;
					while (((line = bufferedReader.readLine()) != null) && mClientCount > 0) {
						if      (line.startsWith("V/")) lvl = 0;
						else if (line.startsWith("D/")) lvl = 1;
						else if (line.startsWith("I/")) lvl = 2;
						else if (line.startsWith("W/")) lvl = 3;
						else if (line.startsWith("E/")) lvl = 4;
						else lvl = 0;
					
						// Issue Signal
						if (mDebugInterface != null){
							Message msg = mHandler.obtainMessage(SEND_LOG_MSG);
							Bundle data = new Bundle();
							data.putString("service", "all");
							data.putInt("level", lvl);
							data.putString("msg", line);
							msg.setData(data);
							mHandler.sendMessage(msg);
						}
						//					else {
						//						Log.e(TAG, "Debug Interface not set up!!!");
						//					}
					}
					bufferedReader.close();
				} while(mClientCount > 0);
			} catch (Exception e) {
				e.printStackTrace();
				//Utilities.logException(TAG, "Exception thrown in logging thread: ", e);
			} finally {
				Log.d(TAG, "Logging thread exiting");
			}
		}//run
	};//Thread

	////////////////////////////////////////////////////////////////////////////
	// Implementations of Signal Handlers
	////////////////////////////////////////////////////////////////////////////

	@BusSignalHandler(iface=DebugConstants.NAME_PREFIX, signal="DebugMessage")
	public void DebugMessageSignal(String service, int level, String message) {
		MessageContext ctx = alljoynContainer.getBusAttachment().getMessageContext();
		if(ctx.sender.equals(alljoynContainer.getBusAttachment().getUniqueName()))
		{
			//Log.i(TAG, "ignoring my own signal");
			return;
		} else {
			Log.i(TAG, "Remote: "+message);
			Message msg = mHandler.obtainMessage(RECEIVED_LOG_MSG);
			Bundle data = new Bundle();
			data.putString("service", service);
			data.putInt("level", level);
			data.putString("msg", message);
			msg.setData(data);
			mHandler.sendMessage(msg);
		}
	}


	/* ****************************************************************** */
	/* Module specific implementation methods (running on handler thread) */
	/* ****************************************************************** */

	//Define constants for Handler events
	private static final int SETUP_SESSION  = 1; 
	private static final int SEND_LOG_MSG = 2;
	private static final int RECEIVED_LOG_MSG = 3;


	private void setupSession (){
		mHandler.sendEmptyMessage(SETUP_SESSION);	
	}


	// Use this Handler for asynchronous transactions
	// Note that there may be several transactions active in parallel, so don't assume a single user

	private HandlerThread handlerThread = new HandlerThread("DebugImpl");
	{handlerThread.start();}
	private ImplHandler mHandler = new ImplHandler(handlerThread.getLooper()); // Handler for complex functions

	private class ImplHandler extends Handler
	{
		public ImplHandler(Looper looper) {
			super(looper);
		}
		public void handleMessage(Message msg) {
			// frequently-used variables
			Bundle data = msg.getData();

			switch(msg.what) {
			case SETUP_SESSION: {
				SetupSession();
				break;
			}
			case SEND_LOG_MSG: {
				try { 
					mDebugInterface.DebugMessage(data.getString("service"), data.getInt("level"), data.getString("msg"));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
			case RECEIVED_LOG_MSG: {
				try { 
					DebugAPIImpl.debugCallback.onDebugMessage(data.getString("service"), data.getInt("level"), data.getString("msg"));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
			default:
				Log.e(TAG, "ImplHandler unknown msg type: "+msg.what);
				break;

			} // switch
		} // handleMessage
	}// ImplHandler

	@Override
	public void InitAPI(AllJoynContainerInterface coreLogic) {
		this.setupSession();
	}
} //DebugImpl
