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
package org.alljoyn.devmodules;

import java.util.ArrayList;

import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.devmodules.sessionmanager.SessionManager;

import android.app.Activity;
import android.util.Log;


public abstract class APICore {
	
	protected SessionManager sessionManager;
	protected static final String SERVICE_NAME = "org.alljoyn.api.devmodules";
	protected static final short SERVICE_PORT = 24;
	protected static String SESSION_NAME = "";
	protected static final String OBJECT_PATH_PREFIX = "/devmodulesapi/";
	protected int mSessionId;
	protected static APICore instance;
	public boolean isReady = false;
	
	ArrayList<Object> objectList = new ArrayList<Object>(); 
	
	public static void StartAllJoynServices(Activity i) throws Exception {
		Log.w("APICore", "StartAllJoynServices in base class. Shouldn't be here!!!");
		if(instance != null) {
			instance.StartAllJoynServices(i);
		}
	}
	
	public static void StopAllJoynServices(Activity i) {
		Log.w("APICore", "StopAllJoynServices in base class. Shouldn't be here!!!");
		if(instance != null) {
			instance.StopAllJoynServices(i);
		}
	}
	
	public static String[] GetNearbyDevices() throws Exception {
		Log.w("APICore", "GetNearbyDevices in base class. Shouldn't be here!!!");
		if(instance != null) {
			return instance.GetNearbyDevices();
		}
		return null;
	}
	
	public static void StartService(String service) throws Exception {
		Log.w("APICore", "StartService in base class. Shouldn't be here!!!");
		if(instance != null) {
			instance.StartService(service);
		}
	}
	
	public static String[] GetServices() throws Exception {
		Log.w("APICore", "GetServices in base class. Shouldn't be here!!!");
		if(instance != null) {
			return instance.GetServices();
		}
		return null;
	}
	
	public static void StartAllServices() throws Exception {
		Log.w("APICore", "StartAllServices in base class. Shouldn't be here!!!");
		if(instance != null) {
			instance.StartAllServices();
		}
	}

	//----------------
	
	public static APICore getInstance() {
		return instance;
	}
	
	protected void registerBusObject(BusObject object, String path) {
		Log.w("APICore", "registerBusObject in base class. Shouldn't be here!!!");
		if(instance != null) {
			instance.registerBusObject(object, path);
		}
	}
	
	protected void registerSignalHandler(Object object) {
		Log.w("APICore", "registerSignalHandler in base class. Shouldn't be here!!!");
		if(instance != null) {
			instance.registerSignalHandler(object);
		}
	}
	
	public ProxyBusObject getProxyBusObject(String peer, Class[] interfaces)
	{
		Log.w("APICore", "getProxyBusObject in base class. Shouldn't be here!!!");
		if(instance != null) {
			return instance.getProxyBusObject(peer, interfaces);
		}
		return null;
	}	
	
	public void EnableConcurrentCallbacks() {
	}
}

