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

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.alljoyn.devmodules.callbacks.*;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;
import org.alljoyn.devmodules.sessionmanager.SessionManager;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class APICoreImpl extends APICore {
	static 
    {
        System.loadLibrary("alljoyn_java");
    }
	
	private static final String TAG = "ConnectorCoreImpl";
	
	private static CoreAPIInterface coreConnectorInterface;
	
	private static APICoreCallback startServiceCallback;
	
	//Use this to identify the local service and not another applications service
	private static String serviceId;
	
	public static void StartAllJoynServices(Activity i, APICoreCallback callback) throws Exception {
		Intent intent = new Intent("org.alljoyn.devmodules.ALLJOYNSERVICES");
		intent.setClassName(i,  "org.alljoyn.devmodules.service.CoreService");
		serviceId = ""+System.currentTimeMillis();
		intent.putExtra("serviceId", serviceId);
		i.startService(intent);
		getInstance();
		Log.v(TAG, "Should have sent intent to start the service...");
		startServiceCallback = callback;
		if(instance.isReady)
			startServiceCallback.onServiceReady();

		Log.v(TAG, "Started AllJoynServices");
	}
	
//	public static void StartAllJoynServices(Activity i) throws Exception {
//		Log.w(TAG, "StartAllJoynServices() Old version");
//		i.startService(new Intent("org.alljoyn.devmodules.ALLJOYNSERVICES"));
//		Log.v(TAG, "Should have sent intent to start the service...");
//		//org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(i.getApplicationContext());
//		getInstance();
//	}

	public static void StopAllJoynServices(Activity i) {
		i.stopService(new Intent("org.alljoyn.devmodules.ALLJOYNSERVICES"));
		instance.isReady = false;
		Log.v(TAG, "Stopping AllJoynServices");
	}
	
//	public static String[] GetNearbyDevices() throws Exception {
//		getInstance();
//		if(coreConnectorInterface == null) {
//			Log.d(TAG, "instance == null? "+(instance == null));
//			throw new Exception("Not connected to AllJoyn Service");
//		}
//		return coreConnectorInterface.GetNearbyDevices();
//	}
	
	public static void StartService(String service) throws Exception {
		getInstance();
		if(coreConnectorInterface == null) {
			Log.d(TAG, "instance == null? "+(instance == null));
			throw new Exception("Not connected to AllJoyn Service");
		}
		Log.d(TAG, "about to call start service with with service="+service);
		coreConnectorInterface.StartService(service);
	}
	
	public static String[] GetServices() throws Exception {
		getInstance();
		if(coreConnectorInterface == null) {
			Log.d(TAG, "instance == null? "+(instance == null));
			throw new Exception("Not connected to AllJoyn Service");
		}
		return coreConnectorInterface.GetServices();
	}
	
	public static void StartAllServices() throws Exception {
		getInstance();
		if(coreConnectorInterface == null) {
			Log.d(TAG, "instance == null? "+(instance == null));
			throw new Exception("Not connected to AllJoyn Service");
		}
		String[] services = GetServices();
		Log.d(TAG, "Services: "+services);
		Log.d(TAG, "Services len: "+services.length);
		for(String service: services) {
			Log.d(TAG, "service: "+service);
			StartService(service);
		}
	}
	
	
	//----------------
	
	public static APICore getInstance() {
		if(instance == null) {
			instance = new APICoreImpl();
			BusListener myBusListener = new BusListener() {
				public void foundAdvertisedName(String name, short transport, String namePrefix) {
					String peer = name.substring(namePrefix.length()+1);
					SESSION_NAME = name.substring(namePrefix.length()+1);
					if(name.contains(SERVICE_NAME) && name.contains(serviceId) && transport == SessionOpts.TRANSPORT_LOCAL) {
						SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_PHYSICAL, SessionOpts.TRANSPORT_LOCAL);
						Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
						Status status = instance.sessionManager.joinSession(peer, SERVICE_PORT, sessionId, sessionOpts, new SessionListener() {
							@Override
							public void sessionLost(int sessionId) {
								Log.i(TAG, "SessionLost, This means that the service has terminated.");
							}
						});
						Log.d(TAG, "joinSessionStatus: "+status);
						if (status == Status.OK) {    
							instance.mSessionId = sessionId.value;
							coreConnectorInterface = instance.sessionManager.getBusAttachment().getProxyBusObject(SERVICE_NAME+"."+SESSION_NAME, 
											OBJECT_PATH_PREFIX+CoreAPIInterface.OBJECT_PATH,
											sessionId.value,
											new Class[] {CoreAPIInterface.class}).getInterface(CoreAPIInterface.class);
						}
						instance.isReady = true;
						if(startServiceCallback != null) {
							startServiceCallback.onServiceReady();
						}
					}
				}

				public void lostAdvertisedName(String name, short transport, String namePrefix) {

				}
			};
			//setup individual listeners for each module
			APILoader.LoadAPI(instance);
			Log.d(TAG, "Creating SessionManager ("+SERVICE_NAME+")");
			instance.sessionManager = new SessionManager(SERVICE_NAME, myBusListener, true);
			//instance.sessionManager.setDebug(true);

			for(Object obj:instance.objectList) {
				instance.registerBusObject((BusObject)obj, ((CallbackObjectBase)obj).getObjectPath());
			}
			instance.sessionManager.connectBus();
			for(Object obj:instance.objectList) {
				instance.registerSignalHandler(obj);
			}
			Log.d(TAG, "Done registering everything....");
		} else { 
			Log.d(TAG, "returning singleton instance");
		}
		return instance;
	}
	
	protected void registerBusObject(BusObject object, String path) {
		sessionManager.registerBusObject(object, path);
	}
	
	protected void registerSignalHandler(Object object) {
		sessionManager.registerSignalHandlers(object);
	}
	
	public ProxyBusObject getProxyBusObject(String peer, Class[] interfaces)
	{
//		Log.d(TAG, "SERVICE_NAME.SESSION_NAME: "+SERVICE_NAME+"."+SESSION_NAME);
//		Log.d(TAG, "peer: "+peer);
//		Log.d(TAG, "path: "+OBJECT_PATH_PREFIX + peer);
		ProxyBusObject proxy_object = sessionManager.getBusAttachment().getProxyBusObject(SERVICE_NAME+"."+SESSION_NAME, 
				OBJECT_PATH_PREFIX + peer,
				mSessionId,
				interfaces);
		return proxy_object;
	}
	
	public void EnableConcurrentCallbacks() {
		sessionManager.getBusAttachment().enableConcurrentCallbacks();
	}
	
}
