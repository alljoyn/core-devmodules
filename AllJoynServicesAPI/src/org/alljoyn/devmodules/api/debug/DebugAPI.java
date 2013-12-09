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
package org.alljoyn.devmodules.api.debug;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.devmodules.APICoreImpl;
import org.alljoyn.devmodules.api.groups.GroupsCallbackObject;
import org.alljoyn.devmodules.callbacks.CallbackObjectBase;
import org.alljoyn.devmodules.callbacks.TransactionHandler;
import org.json.JSONObject;

import org.alljoyn.devmodules.common.DebugMessageDescriptor;

import android.util.Log;

public class DebugAPI {
	
	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	// the callback object
	private static DebugCallbackObject mDebugCallbackObject = null;
	
	private static DebugAPIInterface mDebugInterface = null;
	
	private static void setupInterface() {
		if(mDebugInterface == null) {
			mDebugInterface = APICoreImpl.getInstance().getProxyBusObject("debug",
				new Class[] {DebugAPIInterface.class}).getInterface(DebugAPIInterface.class);
		}
		
		
		if (mDebugCallbackObject == null) mDebugCallbackObject = new DebugCallbackObject();

	}
	
	public static final int DEBUG_SERVICE_AVAILABLE = 1;
	public static final int DEBUG_SERVICE_LOST = 2;
	public static final int DEBUG_SERVICE_MESSAGE = 3;
	
	public static void RegisterListener(DebugListener listener) {
		setupInterface();
		mDebugCallbackObject.registerListener(listener);
	}
	
	public static boolean IsEnabled() throws Exception {
		setupInterface();
		return mDebugInterface.IsEnabled();
	}

    public static void Enable() throws Exception {
    	setupInterface();
		mDebugInterface.Enable();
	}

	// Disable Logging
    public static void Disable() throws Exception {
		mDebugInterface.Disable();
	}

	// Get current filter string (OS-Specific)
    public static String GetFilter() throws Exception {
    	setupInterface();
		return mDebugInterface.GetFilter();
	}

	// Set filter string (OS-Specific)
    public static void SetFilter(String filter) throws Exception {
    	setupInterface();
		mDebugInterface.SetFilter(filter);
	}
	
	////////////////////////////////////////////////
	// Interaction with Remote Debug Services
	////////////////////////////////////////////////
	
	// Connect to a particular service (just use prefix for all services)
	public static void Connect (String device, String service) throws Exception {
    	setupInterface();
		Log.d(DebugAPI.class.getName(),"Calling Connect in the connector.");
		mDebugInterface.Connect(device, service);
	}
	
	// Disconnect from a particular service
	public static void Disconnect (String device, String service) throws Exception {
    	setupInterface();
		mDebugInterface.Disconnect(device, service);
	}

	// List connected services
	public static String[] GetServiceList () throws Exception {
    	setupInterface();
//		TransactionHandler th = new TransactionHandler() {
//			public void HandleTransaction(String jsonCallbackData,
//					byte[] rawData, int totalParts, int partNumber) {
//				latch.countDown();
//				
//			}
//		};
//		int transactionId = CallbackObject.AddTransaction(th);
//		th.latch.await();
		return mDebugInterface.GetServiceList();
	}
	
	// Get the list of messages received from a particular service
	public static DebugMessageDescriptor[] GetMessageList (String service) throws Exception {
    	setupInterface();
		return mDebugInterface.GetMessageList(service);
	}
}
