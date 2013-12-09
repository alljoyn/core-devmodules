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

public class DebugMessageList {


	private final String TAG = "DebugMessageList";
	private final int    MAX_LIST_SIZE = 256;  // List size

	// Arrays for holding the data
	//private CircularArrayList<DebugMessageDescriptor> mMsgList ;
	private ArrayList<DebugMessageDescriptor> mMsgList ;

	public DebugMessageList() { 
		// private to control usage
		checkSetup();
	} 


	// check that list has been set up. If not, create them
	private void checkSetup(){
		if (mMsgList==null)
		{
			//mMsgList = new CircularArrayList<DebugMessageDescriptor>(MAX_LIST_SIZE);	
			mMsgList = new ArrayList<DebugMessageDescriptor>();	
			mMsgList.clear();
		}
	}

	// clear lists
	public synchronized void clear (){
		mMsgList.clear();
	}

	// check whether empty
	public synchronized boolean isEmpty(){
		return mMsgList.isEmpty();
	}
	// add a message to the list
	public synchronized void add (String service, int level, String message){

		checkSetup();

		DebugMessageDescriptor dd = new DebugMessageDescriptor();
		dd.service = service;
		dd.level   = level;
		dd.message = message;
		mMsgList.add(dd);

	}

	// add a message to the list
	public synchronized void add (DebugMessageDescriptor message){

		checkSetup();
		mMsgList.add(message);
		/***
		// if we have reached the maximum size then get rid of the first entry
		if (mMsgList.size()==MAX_LIST_SIZE){
			mMsgList.remove(0);
		}***/
	}

	// get the last item in the list
	public synchronized DebugMessageDescriptor get (){
		checkSetup();
		if (!mMsgList.isEmpty()){
			return mMsgList.get(mMsgList.size());
		} else {
			return null;
		}
	}

	// get the nth item in the list
	public synchronized DebugMessageDescriptor get (int n){
		checkSetup();
		if (!mMsgList.isEmpty()){
			return mMsgList.get(n);
		} else {
			return null;
		}
	}


	// get the number of messages stored
	public synchronized int size(){
		checkSetup();
		return mMsgList.size();
	}


	// return list of debug messages
	public synchronized DebugMessageDescriptor[] getAll() {
		checkSetup();
		if (!mMsgList.isEmpty()){
			return mMsgList.toArray(new DebugMessageDescriptor[mMsgList.size()]);
		} else {
			return new DebugMessageDescriptor[0];
		}
	}

	// return list of debug messages (same as getAll, but matches standard interface for lists
	public synchronized DebugMessageDescriptor[] toArray() {
		checkSetup();
		if (!mMsgList.isEmpty()){
			return mMsgList.toArray(new DebugMessageDescriptor[mMsgList.size()]);
		} else {
			return new DebugMessageDescriptor[0];
		}
	}


} // DebugMessageList
