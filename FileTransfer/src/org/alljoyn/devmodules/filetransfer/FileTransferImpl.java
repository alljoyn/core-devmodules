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
package org.alljoyn.devmodules.filetransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import org.alljoyn.api.filetransfer.FileTransferAPIImpl;
import org.alljoyn.bus.*;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.alljoyn.devmodules.interfaces.ModuleInterface;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FileTransferImpl extends BusListener implements ModuleInterface  {
	private final String TAG = "FileTransferImpl";

	private static FileTransferImpl instance;
	public static FileTransferImpl getInstance() { return instance; }

	private FileTransferObject busObject;
	private AllJoynContainerInterface alljoynContainer;
	private String myWellknownName;

	private ArrayList<String> remotekeyPeerWellKnowns = new ArrayList<String>();
	private String namePrefix;


	public FileTransferImpl(AllJoynContainerInterface alljoynContainer) {
		this.alljoynContainer = alljoynContainer;
		instance = this;
		busObject = new FileTransferObject(alljoynContainer);
	}

	public void RegisterSignalHandlers() {
		alljoynContainer.getBusAttachment().registerSignalHandlers(this);
	}

	public void SetupSession() {
		SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
		alljoynContainer.createSession(getAdvertisedName(), FileTransferConstants.SESSION_PORT, new SessionPortListener() {
			public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
				Log.d(FileTransferConstants.COMPONENT_NAME, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
				if (sessionPort == FileTransferConstants.SESSION_PORT) {
					return true;
				}
				return false;
			}

			public void sessionJoined(short sessionPort, int id, String joiner) {
				Log.d(FileTransferConstants.COMPONENT_NAME, "User " + joiner + " joined., id=" + id);
				//            	mProxyObjHash.put(joiner,DebugImpl.sessionManager.getBusAttachment().getProxyBusObject(joiner, 
				//    					ProfileConstants.OBJECT_PATH,
				//    					id,
				//    					new Class<?>[] { DebugInterface.class }));
			}
		}, sessionOpts);
		Log.i(FileTransferConstants.COMPONENT_NAME,"Advertised: "+getAdvertisedName());
	}

	public BusObject getBusObject() { return busObject; }

	public String getObjectPath() {
		return FileTransferConstants.OBJECT_PATH;
	}

	public String getAdvertisedName() {
		if(myWellknownName == null) {
			myWellknownName = FileTransferConstants.SERVICE_NAME+"."+alljoynContainer.getUniqueID();
		}
		return myWellknownName;
	}

	public String getServiceName() {
		return FileTransferConstants.SERVICE_NAME;
	}

	public void foundAdvertisedName(String name, short transport, String namePrefix) {
		if(!name.contains(getAdvertisedName()) && name.contains(FileTransferConstants.NAME_PREFIX)) {
			Log.i(TAG,"foundAdvertisedName: "+name);
			name = name.substring(namePrefix.length()+1);
			remotekeyPeerWellKnowns.add(name);
			this.namePrefix = namePrefix;
			try { 
				JSONObject jsonData = new JSONObject();
				jsonData.put("name", name);
				//ConnectorCore.callbackInterface.ListenerJSON(getServiceName(), ProfileConnectorObject.PROFILE_FOUND, jsonData.toString());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void lostAdvertisedName(String name, short transport, String namePrefix) {
		if(!name.contains(getAdvertisedName()) && name.contains(FileTransferConstants.NAME_PREFIX)) {
			Log.i(TAG,"LostAdvertisedName: "+name);
			remotekeyPeerWellKnowns.remove(name);
			try { 
				JSONObject jsonData = new JSONObject();
				jsonData.put("name", name);
				//ConnectorCore.callbackInterface.ListenerJSON(getServiceName(), ProfileConnectorObject.PROFILE_LOST, jsonData.toString());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void InitAPI(AllJoynContainerInterface coreLogic) {
		setupFileSession(); // has to be done on Handler thread
	}

	public void shutdown() {

	}

	public ArrayList<String> getPeers() { return remotekeyPeerWellKnowns; }

	/* ************************************** */
	/* Module specific implementation methods */
	/* ************************************** */

	public void getFile(String peer, String path) {
		String wellKnownName = peer;
		SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
		Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
		alljoynContainer.getSessionManager().joinSession(wellKnownName, FileTransferConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());
		Log.e(TAG, "Joined the session: "+sessionId.value);
		Log.e(TAG, "object name: "+namePrefix+"."+peer);
		ProxyBusObject mProxyObj =  alljoynContainer.getBusAttachment().getProxyBusObject(namePrefix+"."+peer, 
				FileTransferConstants.OBJECT_PATH,
				sessionId.value,
				new Class<?>[] { FileTransferInterface.class });

		FileTransferInterface filetransfer = mProxyObj.getInterface(FileTransferInterface.class);
		try {
			filetransfer.getFile(path);
		} catch (BusException e) {
			e.printStackTrace();
		}
		alljoynContainer.getBusAttachment().leaveSession(sessionId.value);
	}

	@BusSignalHandler(iface=FileTransferConstants.NAME_PREFIX, signal="fileData")
	public void fileDataReceived(String fileName, String origPath, byte[] data) {
		Log.d(TAG, "Received raw file data: "+fileName+", size:"+data.length);
		MessageContext ctx = alljoynContainer.getBusAttachment().getMessageContext();
		String localPath = Environment.getExternalStorageDirectory().getPath()+"/data/received/"+fileName;
		if(data.length == 0) {
			try{
				FileTransferAPIImpl.callback.onTransferComplete(ctx.sender, fileName, "TYPE", localPath);
			}catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			//Write data to file
			File root = new File(Environment.getExternalStorageDirectory().getPath()+"/data/received/", "");
			if(!root.exists())
				root.mkdirs();
			File received = new File(localPath);
			FileOutputStream out;
			try {
				out = new FileOutputStream(received,true);
				out.write(data);
				out.flush();
				out.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	private FileTransferInterface globalSender = null;
	public void offerFile(String filename, String path) {
		if(globalSender == null) {
			SignalEmitter sem = new SignalEmitter(busObject, SignalEmitter.GlobalBroadcast.On);
			globalSender = sem.getInterface(FileTransferInterface.class);
		}
		try {
			globalSender.offerFile(filename, path);
		} catch (BusException e) {
			e.printStackTrace();
		}
	}

	@BusSignalHandler(iface=FileTransferConstants.NAME_PREFIX, signal="offerFile")
	public void fileOffered(String filename, String path) {
		MessageContext ctx = alljoynContainer.getBusAttachment().getMessageContext();
		try{
			FileTransferAPIImpl.callback.onFileOffered(ctx.sender, filename, path);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void pushFile(String peer, String path) {
		//TODO: No need to join because we are already connected to everyone on the Bus
		//		String wellKnownName = peer;
		//		SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
		//        Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
		//        alljoynContainer.getSessionManager().joinSession(wellKnownName, FileTransferConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());
		//		Log.e(TAG, "Joined the session: "+sessionId.value);
		//		Log.e(TAG, "object name: "+namePrefix+"."+peer);
		try {
			//This is a complete hack because currently we are using a global broadcast
			busObject.sendFile(path);
		} catch (BusException e) {
			e.printStackTrace();
		}
	}



	/* ****************************************************************** */
	/* Module specific implementation methods (running on handler thread) */
	/* ****************************************************************** */

	//Define constants for Handler events
	private static final int SETUP_SESSION  = 1; 


	private void setupFileSession (){
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
