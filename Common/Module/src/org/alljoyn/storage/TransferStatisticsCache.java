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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;

import org.alljoyn.devmodules.common.TransferStatistics;
import org.alljoyn.devmodules.common.TransferStatisticsDescriptor;



import android.os.Environment;
import android.util.Log;

// Class to handle caching of media query transactions data in local storage
// Each 'transaction' is represented by a JSON file that holds the TransferStatistics for the item
// The directory that holds the fie identifies the list in which it is contained

public class TransferStatisticsCache extends BaseCache {

	private static final String TAG                           = "TransferStatisticsCache";
	private static final String TRANSFER_STATISTICS_CACHE_DIR = "/sdcard/.alljoyn/media/stats/";
	private static final String TRANSFER_STATISTICS_EXT       = ".json";

	private static final String TRANSFER_STATISTICS_LIST      = "sent"      + TRANSFER_STATISTICS_EXT;
	private static boolean      mCacheReady                   = false;



	// Get the directory location
	protected static String getPath(){
		return TRANSFER_STATISTICS_CACHE_DIR;
	}

	protected static String getLogTag() {
		return TAG;
	}



	/////////////////////////////////////////////////////////////////////////
	// Generic stuff (MediaTransaction-independent)
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
					Log.d(TAG, "MediaTransaction cache directory set up: "+getPath());
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
	// Statistics-Specific
	/////////////////////////////////////////////////////////////////////////


	// Form the file prefix based on the transaction type
	public static String getPrefix (){
		return getPath()  ;
	}



	// Get the stored file path
	public static String getStatisticsPath (){
		if (!mCacheReady) init();
		String fname = TRANSFER_STATISTICS_LIST;
		if (fname.contains("/"))
			fname = fname.substring(fname.lastIndexOf("/")+1); // remove directories
		if (fname.contains("."))		
			fname = fname.substring(0, fname.lastIndexOf(".")); // remove extension
		return getPrefix() + fname + TRANSFER_STATISTICS_EXT;
	}


	// Check to see if media is already saved
	public static boolean isPresent(){
		if (!mCacheReady) init();
		return isFilePresent(getStatisticsPath());
	}



	// remove entry from cache
	public static void remove (){
		if (!mCacheReady) init();
		if (mCacheReady){
			// Remove the media
			removeFile (getStatisticsPath());
		} else {
			Log.e(TAG, "remove() - Cache not set up");
		}
	}



	// save the Statistics List to cache
	public static void saveList (TransferStatisticsDescriptor statsd){
		if (!mCacheReady) init();
		// Just convert to string and overwrite the file
		writeTextFile(getPath()+TRANSFER_STATISTICS_LIST, statsd.toString());
	}

	// retrieve the List from cache
	public static TransferStatisticsDescriptor retrieveList (){
		if (!mCacheReady) init();
		TransferStatisticsDescriptor statsd = new TransferStatisticsDescriptor();
		String f = getPath()+TRANSFER_STATISTICS_LIST;
		if (isFilePresent(f)){
			String list = readTextFile (f);
			statsd.setString(list);
		}
		return statsd;
	}

	// add an entry to the list (and replace the old one)
	public static void add(TransferStatistics stats){
		if (!mCacheReady) init();
		TransferStatisticsDescriptor tsd = retrieveList();
		tsd.add(stats);
		remove();
		saveList(tsd);
		stats.dump(); 
	}

}//TransferStatisticsCache
