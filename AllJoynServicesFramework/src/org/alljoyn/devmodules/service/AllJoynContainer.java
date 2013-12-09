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
package org.alljoyn.devmodules.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import org.alljoyn.devmodules.sessionmanager.BusObjectData;
import org.alljoyn.devmodules.sessionmanager.SessionManager;
import org.alljoyn.devmodules.api.ModuleAPIManager;
import org.alljoyn.devmodules.groups.GroupSessionManager;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.alljoyn.devmodules.interfaces.ModuleInterface;
import org.alljoyn.devmodules.loader.ContainerLoader;
import org.alljoyn.storage.ProfileCache;

import android.content.Context;
import android.util.Log;

public class AllJoynContainer implements AllJoynContainerInterface {
	private static final String TAG = "AllJoynContainer";
	static 
    {
        System.loadLibrary("alljoyn_java");
    }

	private static AllJoynContainer instance = null;
	
	public SessionManager  mSessionManager;

//	//legacy remove
//	public static HashMap<String,HashMap<String,Boolean>> remoteServiceMap = new HashMap<String,HashMap<String,Boolean>>();
//	
	private HashMap<String, String> busIdMap = new HashMap<String,String>();
	
	//new way
	public Vector<ModuleInterface> modules = new Vector<ModuleInterface>();
	
	//���private FileTransferImpl fileTransferImpl;

	private String uniqueId;
	
	public AllJoynContainer() 
	{
	}
	
	public String whoIsBusId(String busId) {
		return busIdMap.get(busId);
	}
	
	public String whoOwnsSession(String session) {
		Log.d(TAG, "Looking for session mapping to "+session);
		for(String key:busIdMap.keySet())
			Log.d(TAG, "keys: "+key);
		for(String vals:busIdMap.values())
			Log.d(TAG, "vals: "+vals);
		for(String busId:busIdMap.keySet()) {
			if(busIdMap.get(busId).endsWith(session))
				return busId;
		}
		return session;
	}

	public void free() {
		// call the shutdown() method of each entry
		for(ModuleInterface mod : modules)
		{
			mod.shutdown();
		}
	}
	
	private static BusObjectData createBusObjectData(ModuleInterface sdi) {
		return new BusObjectData(sdi.getBusObject(), sdi.getObjectPath());
	}
	
	public String getUniqueID() {
		return instance.uniqueId;
	}
	
	public SessionManager getSessionManager() {
		return instance.mSessionManager;
	}
	
	public int getGroupSessionId(String groupName) throws Exception {
		return GroupSessionManager.getSessionId(groupName);
	}

	public static AllJoynContainer getInstance(Context context, String apiServiceId) {
		ModuleAPIManager.getInstance(apiServiceId);
		Log.i(TAG, "checking instance == null: "+(instance == null));
		if(instance == null) {
			instance = new AllJoynContainer();
			instance.uniqueId=instance.getGUID();
			
			BusListener myBusListener = new BusListener() {
				public void foundAdvertisedName(String name, short transport, String namePrefix) {

	        	}
	    		
	    		public void lostAdvertisedName(String name, short transport, String namePrefix) {
	    			
	    		}
	    		
	    		public void nameOwnerChanged(String busName, String prevOwner, String newOwner) {
	    			//instance.mSessionManager.getBusAttachment().enableConcurrentCallbacks();
	    			Log.d(TAG, "NameOwnerChanged("+busName+", "+prevOwner+", "+newOwner);
	    			if(busName.startsWith(SERVICE_NAME)) {
	    				String session = busName.substring(SERVICE_NAME.length()+1);
		        		Log.i(TAG, "nameOwnerChanged: "+session);
		        		String module = session.substring(0,session.indexOf('.'));
		        		Log.i(TAG, "nameOwnerChanged: "+module);
		        		//TODO hardcoded this for now!
		        		if(busName.contains(".chat.")) {
		        			if(newOwner == null) {
		    					instance.busIdMap.remove(prevOwner);
		    					//Log.d(TAG, "removed mapping for "+prevOwner);
		    				}
		    				else {
		    					//Log.d(TAG, "mapping "+newOwner+" to "+busName.substring(busName.lastIndexOf('.')+1));
		    					instance.busIdMap.put(newOwner, busName.substring(busName.lastIndexOf('.')+1, busName.lastIndexOf("_")));
		    				}
		        		}
		        		else
		        		{
		    				if(newOwner == null) {
		    					instance.busIdMap.remove(prevOwner);
		    					//Log.d(TAG, "removed mapping for "+prevOwner);
		    				}
		    				else {
		    					//Log.d(TAG, "mapping "+newOwner+" to "+busName.substring(busName.lastIndexOf('.')+1));
		    					instance.busIdMap.put(newOwner, busName.substring(busName.lastIndexOf('.')+1));
		    				}
		        		}
	    			}
	    		}
			};
			instance.mSessionManager = new SessionManager(SERVICE_NAME,myBusListener);
			//instance.mSessionManager.setDebug(true);
			ContainerLoader.LoadImpl(instance, context);
			for(ModuleInterface module:instance.modules) { //TODO: Do not like this change soon
				if(module.getBusObject() != null) {
					instance.mSessionManager.registerBusObject(module.getBusObject(), module.getObjectPath());
					Log.d(TAG,"registered module: "+module);
				}
			}
			instance.mSessionManager.connectBus();
			//you must first connect to the bus prior to registering signal handlers
			for(ModuleInterface module:instance.modules) {
				module.RegisterSignalHandlers();
			}
	//		for(ModuleInterface module:instance.modules) {
	//			module.InitAPI(instance);
	//		}	
		}
		return instance;
	}
	
	public static void StartService(String service) {
		for(ModuleInterface module: instance.modules) {
			if(service.equals(module.getServiceName())) {
				module.InitAPI(instance);
				return;
			}
		}
	}
	
	public static String[] GetServices() {
		int len = instance.modules.size();
		String[] services = new String[len];
		Log.d(TAG,"len: "+len);
		for(int i = 0; i < len; i++)
			services[i] = instance.modules.get(i).getServiceName();
		return services;
	}
	
	// some services do not always run at startup, so double-check and start if necessary
	public void checkServices (){
		// Profile can be set up after service has started, so load if not already there
		//if (!instance.serviceMap.containsKey("Profile")){
//			if (ProfileCache.isContactDefined()){
//				serviceMap.put("Debug",       new DebugImpl());
//				serviceMap.put("Profile",     new ProfileImpl());
//				serviceMap.put("MediaQuery",  new MediaQueryImpl());
//				serviceMap.put("FileManager", new FileManagerImpl());
//			}
		//}
		
	}	

	private String getGUID(){
		// check to see if the name has been saved. If so, retrieve, otherwise define and save
		String guid;
		if (ProfileCache.isNameDefined()){
			guid = ProfileCache.retrieveName();
		} else {
			guid = "";
			for(int i = 0; i < 15; i++)
				guid += (char)('A'+(int)(Math.random()*26));
		}

		// save name to cache
		ProfileCache.saveName(guid);

		return guid;
	}

	public BusAttachment getBusAttachment() {
		return instance.mSessionManager.getBusAttachment();
	}

	public Status createSession(String sessionName, short sessionPort,
			SessionPortListener sessionPortListener, SessionOpts sessionOpts) {
		return instance.mSessionManager.createSession(sessionName, sessionPort, sessionPortListener, sessionOpts);
	}

	public ArrayList<String> getParticipants(String uID) {
		ArrayList<String> participants = instance.mSessionManager.getParticipants(uID);
		int len = participants.size();
		String busId;
		String mapped;
		for(int i=0; i < len; i++)
		{
			busId = participants.get(i);
			if( !this.getBusAttachment().getUniqueName().equals(busId) ){
				mapped = instance.whoIsBusId(busId);
				if(mapped != null)
					participants.set(i, mapped);
			}
		}
		return participants;
	}
}

