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
import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.Utilities;



import android.os.Environment;
import android.util.Log;

// Class to handle caching of group lists in local storage

public class GroupCache extends BaseCache {

	private static final String TAG            = "GroupCache";
	private static final String GROUP_CACHE_DIR = Environment.getExternalStorageDirectory().getPath() + "/.alljoyn/groups/";
	private static final String GROUP_EXT       = ".json";

	private static final String GROUP_LIST = "groups" + GROUP_EXT;
	private static boolean      mCacheReady    = false;




	/////////////////////////////////////////////////////////////////////////
	// Generic stuff (Type-independent)
	/////////////////////////////////////////////////////////////////////////


	// Run initial checks, make sure directories are there and writeable etc.
	public static void init(){

		mCacheReady = false;
		setTag(TAG);
		setCacheDir(GROUP_CACHE_DIR);

		try {
			// check that storage is accessible
			if (checkStorage()){
				// make sure directory exists
				File lclpath = new File(getPath());

				// Make sure the  directory exists.
				lclpath.mkdirs();
				if (lclpath.exists()){
					mCacheReady = true;
					Log.d(TAG, "Group cache directory set up: "+getPath());
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
	// Group-Specific
	/////////////////////////////////////////////////////////////////////////


	// Form the file prefix 
	public static String getPrefix (){
		return getPath();
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


	// Group List Management
	// --------------------

	// Get the stored file path for the list of groups
	public static String getGroupListPath (){
		if (!mCacheReady) init();
		return getPrefix() + GROUP_LIST;
	}


	// Check to see if Group List file is already saved
	public static boolean isGroupListPresent(){
		if (!mCacheReady) init();
		return isFilePresent(getGroupListPath());
	}


	// save the Group List to cache
	public static void saveGroupList (GroupListDescriptor gld){
		if (!mCacheReady) init();
		// Just convert to string and overwrite the file
		writeTextFile(getGroupListPath(), gld.toString());
	}

	// retrieve the Group List from cache
	public static GroupListDescriptor retrieveGroupList (){
		if (!mCacheReady) init();
		GroupListDescriptor gld = new GroupListDescriptor();
		String f = getGroupListPath();
		if (isFilePresent(f)){
			String list = readTextFile(f);
			gld.setString(list);
		}
		return gld;
	}

	// remove entry from cache
	public static void removeGroupList (){
		if (!mCacheReady) init();
		if (mCacheReady){
			// Remove the file
			removeFile (getGroupListPath());
		} else {
			Log.e(TAG, "remove() - Cache not set up");
		}
	}

	// Individual Group Definition Management
	// --------------------------------------

	// Get the stored file path for the list of Messages
	public static String getGroupDetailsPath (String group){
		if (!mCacheReady) init();
		return getPrefix() + getFilename(group) + GROUP_EXT;
	}

	// Check to see if Message List file is already saved
	public static boolean isGroupDetailsPresent(String group){
		if (!mCacheReady) init();
		return isFilePresent(getGroupDetailsPath(group));
	}


	// save the Message List to cache
	public static void saveGroupDetails (String group, GroupDescriptor gld){
		if (!mCacheReady) init();
		// Just convert to string and overwrite the file
		writeTextFile(getGroupDetailsPath(group), gld.toString());
	}

	// retrieve the Message List from cache
	public static GroupDescriptor retrieveGroupDetails (String group){
		if (!mCacheReady) init();
		GroupDescriptor gld = new GroupDescriptor();
		String f = getGroupDetailsPath(group);
		if (isFilePresent(f)){
			String list = readTextFile(f);
			gld.setString(list);
		}
		return gld;
	}

	// remove entry from cache
	public static void removeGroupDetails (String group){
		if (!mCacheReady) init();
		if (mCacheReady){
			// Remove the file
			removeFile (getGroupDetailsPath(group));
		} else {
			Log.e(TAG, "remove() - Cache not set up");
		}
	}


}//GroupCache
