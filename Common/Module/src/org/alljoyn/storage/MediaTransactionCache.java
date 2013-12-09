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

import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaIdentifierDescriptor;
import org.alljoyn.devmodules.common.TransactionListDescriptor;




import android.os.Environment;
import android.util.Log;

// Class to handle caching of media query transactions data in local storage
// Each 'transaction' is represented by a JSON file that holds the MediaIdentifier for the item
// The directory that holds the fie identifies the list in which it is contained

public class MediaTransactionCache extends BaseCache {

	private static final String TAG                               = "MediaTransactionCache";
	private static final String MEDIATRANSACTION_CACHE_DIR        = "/sdcard/.alljoyn/media/transaction/";
	private static final String MEDIATRANSACTION_EXT              = ".json";

	// constants for types of transaction
	public static final String SENT      = "sent";
	public static final String RECEIVED  = "received";
	public static final String PUBLISHED = "published";
	public static final String APPROVED  = "approved";
	public static final String REJECTED  = "rejected";

	private static final String MEDIATRANSACTION_SENT_LIST        = "sent"      + MEDIATRANSACTION_EXT;
	private static final String MEDIATRANSACTION_RECEIVED_LIST    = "received"  + MEDIATRANSACTION_EXT;
	private static final String MEDIATRANSACTION_PUBLISHED_LIST   = "published" + MEDIATRANSACTION_EXT;
	private static final String MEDIATRANSACTION_APPROVED_LIST    = "approved"  + MEDIATRANSACTION_EXT;
	private static final String MEDIATRANSACTION_REJECTED_LIST    = "rejected"  + MEDIATRANSACTION_EXT;
	private static boolean      mCacheReady                       = false;


	private static String [] subdirs = { SENT, RECEIVED, PUBLISHED, APPROVED, REJECTED } ; 



	// Get the directory location
	protected static String getPath(){
		return MEDIATRANSACTION_CACHE_DIR;
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

					//loop and check the subdirectories too
					for (int i=0; i<subdirs.length; i++){
						File sd = new File (getPath()+subdirs[i]);
						sd.mkdirs();
					}
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
	// MediaTransaction-Specific
	/////////////////////////////////////////////////////////////////////////


	// Form the file prefix based on the transaction type
	public static String getPrefix (String trantype){
		return getPath() + trantype + "/" ;
	}



	// Generate a filename based on the supplied type/name
	public static String getMediaTransactionPath (String trantype, String file){
		if (!mCacheReady) init();
		String fname = getFilename (file);
		if (fname.contains("/"))
			fname = fname.substring(fname.lastIndexOf("/")+1); // remove directories
		if (fname.contains("."))		
			fname = fname.substring(0, fname.lastIndexOf(".")); // remove extension
		return getPrefix(trantype) + fname + MEDIATRANSACTION_EXT;
	}


	// Check to see if media is already saved
	public static boolean isPresent(String trantype, String filename){
		if (!mCacheReady) init();
		return isFilePresent(getMediaTransactionPath(trantype, filename));
	}



	// remove entry from cache
	public static void remove (String trantype, String filename){
		if (!mCacheReady) init();
		if (mCacheReady){
			// Remove the media
			removeFile (getMediaTransactionPath(trantype, filename));
		} else {
			Log.e(TAG, "remove() - Cache not set up");
		}
	}



	// move entry from between lists
	public static void move (String oldtype, String filename, String newtype){
		if (!mCacheReady) init();
		if (mCacheReady){
			// Move the media
			String oldfile = getMediaTransactionPath(oldtype, filename);
			String newfile = getMediaTransactionPath(newtype, filename);
			Log.v(TAG, "Moving: "+oldfile+" -> "+newfile);
			moveFile (oldfile, newfile);
		} else {
			Log.e(TAG, "remove() - Cache not set up");
		}
	}


	// List available transactions
	public static String[] list(String trantype){
		String[] plist = new String[0];
		if (!mCacheReady) init();
		if (mCacheReady){
			// Get the list of profiles
			File dir = new File(getPrefix(trantype));

			// Define filter that only selects the required files
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.endsWith(MEDIATRANSACTION_EXT))
						return true;
					else
						return false;
				}
			};

			//TODO: sort by date (newest first)
			plist = dir.list(filter);
			if (plist == null) {
				Log.d(TAG, "list() No files found");
			}

		} else {
			Log.e(TAG, "list() - Cache not set up");
		}
		return plist;
	}


	// retrieve the query from cache with the supplied name (String version)
	public static MediaIdentifier retrieveMediaTransaction (String trantype, String filename){
		if (!mCacheReady) init();
		MediaIdentifier mi = new MediaIdentifier();
		String miString = readTextFile (getMediaTransactionPath(trantype, filename));
		MediaIdentifierDescriptor mid = new MediaIdentifierDescriptor ();
		mid.setString(miString);
		mi = mid.get();
		return mi;
	}


	// save the query to cache with the supplied name (String version)
	public static void saveMediaTransaction (String trantype, String filename, MediaIdentifier mi){
		if (!mCacheReady) init();
		MediaIdentifierDescriptor mid = new MediaIdentifierDescriptor ();
		mid.add(mi);
		String tfile = getMediaTransactionPath(trantype, filename);
		Log.v(TAG, "Save Transaction to: "+tfile);
		Log.v(TAG, "MediaIdentifier: "+mid.toString());
		writeTextFile (tfile, mid.toString());
	}



	// save the Sent List to cache
	public static void saveSentList (TransactionListDescriptor transd){
		if (!mCacheReady) init();
		// Just convert to string and overwrite the file
		writeTextFile(getPath()+MEDIATRANSACTION_SENT_LIST, transd.toString());
	}

	// retrieve the Sent List from cache
	public static TransactionListDescriptor retrieveSentList (){
		if (!mCacheReady) init();
		TransactionListDescriptor tld = new TransactionListDescriptor();
		String f = getPath()+MEDIATRANSACTION_SENT_LIST;
		if (isFilePresent(f)){
			String list = readTextFile (f);
			tld.setString(list);
		}
		return tld;
	}


	// save the Received List to cache
	public static void saveReceivedList (TransactionListDescriptor transd){
		if (!mCacheReady) init();
		// Just convert to string and overwrite the file
		writeTextFile(getPath()+MEDIATRANSACTION_RECEIVED_LIST, transd.toString());
	}

	// retrieve the Received List from cache
	public static TransactionListDescriptor retrieveReceivedList (){
		if (!mCacheReady) init();
		TransactionListDescriptor tld = new TransactionListDescriptor();
		String f = getPath()+MEDIATRANSACTION_RECEIVED_LIST;
		if (isFilePresent(f)){
			String list = readTextFile (f);
			tld.setString(list);
		}
		return tld;
	}


	// save the Published List to cache
	public static void savePublishedList (TransactionListDescriptor transd){
		if (!mCacheReady) init();
		// Just convert to string and overwrite the file
		writeTextFile(getPath()+MEDIATRANSACTION_PUBLISHED_LIST, transd.toString());
	}

	// retrieve the Published List from cache
	public static TransactionListDescriptor retrievePublishedList (){
		if (!mCacheReady) init();
		TransactionListDescriptor tld = new TransactionListDescriptor();
		String f = getPath()+MEDIATRANSACTION_PUBLISHED_LIST;
		if (isFilePresent(f)){
			String list = readTextFile (f);
			tld.setString(list);
		}
		return tld;
	}


	// save the Approved List to cache
	public static void saveApprovedList (TransactionListDescriptor transd){
		if (!mCacheReady) init();
		// Just convert to string and overwrite the file
		writeTextFile(getPath()+MEDIATRANSACTION_APPROVED_LIST, transd.toString());
	}

	// retrieve the Approved List from cache
	public static TransactionListDescriptor retrieveApprovedList (){
		if (!mCacheReady) init();
		TransactionListDescriptor tld = new TransactionListDescriptor();
		String f = getPath()+MEDIATRANSACTION_APPROVED_LIST;
		if (isFilePresent(f)){
			String list = readTextFile (f);
			tld.setString(list);
		}
		return tld;
	}


	// save the Rejected List to cache
	public static void saveRejectedList (TransactionListDescriptor transd){
		if (!mCacheReady) init();
		// Just convert to string and overwrite the file
		writeTextFile(getPath()+MEDIATRANSACTION_REJECTED_LIST, transd.toString());
	}

	// retrieve the Rejected List from cache
	public static TransactionListDescriptor retrieveRejectedList (){
		if (!mCacheReady) init();
		TransactionListDescriptor tld = new TransactionListDescriptor();
		String f = getPath()+MEDIATRANSACTION_REJECTED_LIST;
		if (isFilePresent(f)){
			String list = readTextFile (f);
			tld.setString(list);
		}
		return tld;
	}



}//MediaTransactionCache
