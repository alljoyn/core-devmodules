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
package org.alljoyn.aroundme.RemoteMedia;

import java.util.ArrayList;
import java.util.HashMap;

import org.alljoyn.aroundme.Adapters.RemoteDebugAdapter;

import android.content.Context;

// Singleton class for managing the list of debug messages for all of the remote connected debug services

public class RemoteDebugListManager {


	private static final String TAG = "RemoteDebugListManager";


	// Hash Map for holding the data - one for each connected debug service

	private static HashMap<String,RemoteDebugAdapter> mRemDebugList     = new HashMap<String,RemoteDebugAdapter>();

	private static RemoteDebugListManager          _manager;               // the singleton version

	// Context of the current Application
	private static Context mContext; 

	private RemoteDebugListManager() { 
		// no implementation, just private to control usage
	} 


	public static synchronized void setContext (Context context){
		mContext = context;
	}

	// prevent instantiation via an implicit or explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// method to return reference to internal data
	public static synchronized RemoteDebugListManager getAdapter() {
		if (_manager == null) {
			_manager = new RemoteDebugListManager();
		}
		return _manager;
	}

	// methods to add and get the various types of lists

	public static synchronized void addDebugList (String service){
		if (!mRemDebugList.containsKey(service)){
			RemoteDebugAdapter rda = new RemoteDebugAdapter (mContext);
			mRemDebugList.put(service, rda);
		}
	}

	public static synchronized void removeDebugList (String service){
		if (mRemDebugList.containsKey(service)){
			mRemDebugList.remove(service);
		}
	}

	// get the adapter for a specified service
	public static synchronized RemoteDebugAdapter get(String service) {
		if (contains(service)){
			return mRemDebugList.get(service);
		} else {
			return null;
		}
	}

	// check to see whether service is present in list
	public static synchronized boolean contains(String service) {
		return (mRemDebugList.containsKey(service));
	}

	// check to see whether list is empty
	public static synchronized boolean isEmpty() {
		return (mRemDebugList.isEmpty());
	}

	// return the number of services present
	public static synchronized int size() {
		return (mRemDebugList.size());
	}


} // RemoteDebugListManager
