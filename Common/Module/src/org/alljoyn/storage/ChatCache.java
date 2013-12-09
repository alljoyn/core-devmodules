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
package org.alljoyn.storage;

import java.io.File;
import org.alljoyn.devmodules.common.MessageListDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.Utilities;



import android.os.Environment;
import android.util.Log;

// Class to handle caching of chat message data in local storage
// There is a list of Messages for each Room. Use Groups for finding the rooms

public class ChatCache extends BaseCache {

	private static final String TAG            = "ChatCache";
	private static final String CHAT_CACHE_DIR = Environment.getExternalStorageDirectory().getPath() + "/.alljoyn/chat/";
	private static final String CHAT_EXT       = ".json";

	private static boolean      mCacheReady    = false;



	// Get the directory location
	protected static String getPath(){
		return CHAT_CACHE_DIR;
	}

	protected static String getLogTag() {
		return TAG;
	}



	/////////////////////////////////////////////////////////////////////////
	// Generic stuff (Type-independent)
	/////////////////////////////////////////////////////////////////////////


	// Run initial checks, make sure directories are there and writeable etc.
	public static void init(){

		mCacheReady = false;

		try {
			// check that storage is accessible
			if (checkStorage()){
				// make sure directory exists
				File lclpath = new File(getPath());

				// Make sure the  directory exists.
				lclpath.mkdirs();
				if (lclpath.exists()){
					mCacheReady = true;
					Log.d(TAG, "Chat cache directory set up: "+getPath());
				} else {
					Log.e(TAG, "Unkown error, cache not set up");
				}
			} else {
				Log.e(TAG, "*** External Storage not available, cannot save media transactions!!! ***");
			}
		} catch (Exception e) {
			Log.e(TAG, "Error setting up media cache ("+getPath()+"): "+e.toString());
		}
	}//init



	/////////////////////////////////////////////////////////////////////////
	// Chat-Specific
	/////////////////////////////////////////////////////////////////////////


	// Form the file prefix 
	public static String getPrefix (){
		return getPath()  ; // (fixed in this case, but left here for generic use)
	}


	// Extract the file name out of the path (and remove non alphanumeric characters)
	public static String getFilename (String path){
		String fname = path;
		if (fname.contains("/"))
			fname = fname.substring(fname.lastIndexOf("/")+1); // remove directories
		if (fname.contains("."))		
			fname = fname.substring(0, fname.lastIndexOf(".")); // remove extension
		fname = Utilities.makeServiceName(fname); // strip any 'invalid' characters and whitespace
		return fname;
	}


	// Message List Management
	// -----------------------

	// Get the stored file path for the list of Messages
	public static String getMessageListPath (String room){
		if (!mCacheReady) init();
		return getPrefix() + getFilename(room) + CHAT_EXT;
	}

	// Check to see if Message List file is already saved
	public static boolean isMessageListPresent(String room){
		if (!mCacheReady) init();
		return isFilePresent(getMessageListPath(room));
	}


	// save the Message List to cache
	public static void saveMessageList (String room, MessageListDescriptor rld){
		if (!mCacheReady) init();
		// Just convert to string and overwrite the file
		writeTextFile(getMessageListPath(room), rld.toString());
	}

	// retrieve the Message List from cache
	public static MessageListDescriptor retrieveMessageList (String room){
		if (!mCacheReady) init();
		MessageListDescriptor rld = new MessageListDescriptor();
		String f = getMessageListPath(room);
		if (isFilePresent(f)){
			String list = readTextFile(f);
			rld.setString(list);
		}
		return rld;
	}

	// remove entry from cache
	public static void removeMessageList (String room){
		if (!mCacheReady) init();
		if (mCacheReady){
			// Remove the file
			removeFile (getMessageListPath(room));
		} else {
			Log.e(TAG, "remove() - Cache not set up");
		}
	}


}//ChatCache
