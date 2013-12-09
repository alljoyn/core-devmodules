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

import java.util.ArrayList;
import java.util.HashMap;


import android.content.Context;
import android.util.Log;

public class LocalDebugList {


	private static final String TAG = "DebugList";

	// Arrays for holding the data
	private static DebugMessageList mDebugList ;


	private static LocalDebugList _instance; // the singleton version

	private LocalDebugList() { 
		// private to control usage
		checkSetup();
	} 


	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// method to return reference to internal data
	public static synchronized LocalDebugList getInstance() {
		if (_instance == null) {
			_instance = new LocalDebugList();
			checkSetup();
		}
		return _instance;
	}

	// check that list has been set up. If not, create them
	private static void checkSetup(){
		if (mDebugList==null)
		{
			mDebugList = new DebugMessageList();	
			mDebugList.clear();
		}
	}

	// clear lists
	public static synchronized void clear (){
		mDebugList.clear();
	}

	// add a profile to the list
	public static synchronized void add (int level, String message){

		checkSetup();

		DebugMessageDescriptor dd = new DebugMessageDescriptor();
		dd.level = level;
		dd.message = message;
		mDebugList.add(dd);

	}

	// get the last item in the list
	public static synchronized DebugMessageDescriptor get (){
		checkSetup();
		if (!mDebugList.isEmpty()){
			return mDebugList.get(mDebugList.size());
		} else {
			return null;
		}
	}


	// get the number of messages stored
	public static synchronized int size(){
		checkSetup();
		return mDebugList.size();
	}


	// return list of profiles
	public static synchronized DebugMessageDescriptor[] getAll() {
		checkSetup();
		if (!mDebugList.isEmpty()){
			return mDebugList.getAll();
		} else {
			return new DebugMessageDescriptor[0];
		}

	}


} // DebugList
