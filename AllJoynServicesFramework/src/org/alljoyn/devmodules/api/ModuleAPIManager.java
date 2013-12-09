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
package org.alljoyn.devmodules.api;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import org.alljoyn.devmodules.sessionmanager.SessionManager;
import org.alljoyn.devmodules.groups.GroupSessionManager;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.alljoyn.devmodules.interfaces.ModuleAPIInterface;
import org.alljoyn.devmodules.loader.ContainerLoader;

import android.util.Log;

public class ModuleAPIManager implements AllJoynContainerInterface {
	static 
    {
        System.loadLibrary("alljoyn_java");
    }
	
	private static final String TAG = "ModuleAPIManager";
	
	private SessionManager sessionManager;
	private static final String SERVICE_NAME = "org.alljoyn.api.devmodules";
	private static final short SERVICE_PORT = 24;
	private static final String SESSION_NAME = "api";
	private static final String OBJECT_PATH_PREFIX = "/devmodulesapi/";
	private int mSessionId;
	
	public Vector<ModuleAPIInterface> moduleList = new Vector<ModuleAPIInterface>();
	
	//public static CallbackInterface callbackInterface;
	
	private static ModuleAPIManager instance;
	
	public ModuleAPIManager() {
		
	}
	
	public static ModuleAPIManager getInstance(String apiServiceId) {
		if(instance == null) {
			instance = new ModuleAPIManager();
		    instance.sessionManager = new SessionManager(SERVICE_NAME, true);
		    //instance.sessionManager.setDebug(true);
		    
		    instance.sessionManager.registerBusObject(new CoreAPIObject(), OBJECT_PATH_PREFIX+CoreAPIObject.OBJECT_PATH);

		    ContainerLoader.LoadAPIImpl(instance);
		    
		    Log.d(TAG, "Added objects to vector, now going to register their busObjects");
		    for(ModuleAPIInterface module:instance.moduleList) {
		    	Log.d(TAG, "module: "+module.getBusObject());
		    	instance.sessionManager.registerBusObject(module.getBusObject(), OBJECT_PATH_PREFIX+module.getBusObjectPath());
		    	Log.d(TAG, "callback : "+module.getCallbackBusObject());
		    	Log.d(TAG, "callback : "+module.getCallbackBusObjectPath());
		    	instance.sessionManager.registerBusObject(module.getCallbackBusObject(), module.getCallbackBusObjectPath());
		    }
		    Log.d(TAG, "ConnectBus...");
		    instance.sessionManager.connectBus();
		    
		    instance.sessionManager.createSession(SESSION_NAME+"._"+instance.sessionManager.getBusAttachment().getGlobalGUIDString()+"._"+apiServiceId, SERVICE_PORT, new SessionPortListener() {
		    	public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
	                Log.d(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
	        		if (sessionPort == SERVICE_PORT) {
	        			return true;
	        		}
	        		return false;
	            }
	            
	            public void sessionJoined(short sessionPort, int id, String joiner) {
	            	Log.d(TAG, "User " + joiner + " joined., id=" + id);
	            	//TODO:Change this to signal emitter!!!!
//	            	callbackInterface = instance.sessionManager.getBusAttachment().getProxyBusObject(joiner, 
//	            			CallbackInterface.OBJECT_PATH,
//	    					id,
//	    					new Class<?>[] { CallbackInterface.class }).getInterface(CallbackInterface.class);
	            	//Setup the callbacks for each module
	            	for(ModuleAPIInterface module:instance.moduleList) {
	            		module.connectCallbackObject(id, joiner);
	            	}
	            }
		    }, new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_PHYSICAL, SessionOpts.TRANSPORT_LOCAL));
		    Log.i(TAG, "Did this advertise????");
		}
		return instance;
	}

	public String getUniqueID() {
		return null;
	}

	public SessionManager getSessionManager() {
		return instance.sessionManager;
	}
	
	public int getGroupSessionId(String groupName) throws Exception {
		throw new Exception("Not Supported");
	}

	public BusAttachment getBusAttachment() {
		return instance.sessionManager.getBusAttachment();
	}

	public Status createSession(String sessionName, short sessionPort,
			SessionPortListener sessionPortListener, SessionOpts sessionOpts) {
		return instance.sessionManager.createSession(sessionName, sessionPort, sessionPortListener, sessionOpts);
	}

	public ArrayList<String> getParticipants(String uID) {
		return null;
	}

	public String whoIsBusId(String sender) {
		// TODO Auto-generated method stub
		return null;
	}
}
