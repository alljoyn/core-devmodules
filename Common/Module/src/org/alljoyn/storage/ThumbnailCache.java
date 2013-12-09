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


import android.os.Environment;
import android.util.Log;

// Class to handle caching of media data in local storage
// File extensions etc are not modified, so we handle potential conflicts by using the 'unique name' 
// of the supplying service as a prefix, i.e. you can the same files form multiple users and they will be stored 
// separately
public class ThumbnailCache extends BaseCache{

	private static final String TAG                       = "ThumbnailCache";
	private static final String THUMBNAIL_CACHE_DIR       = "/sdcard/.alljoyn/media/thumbs/";
	private static final String THUMBNAIL_THUMBNAIL_TAG   = "th-";
	private static final String THUMBNAIL_THUMBNAIL_EXT   = ".jpg";
	private static boolean      mCacheReady               = false;



	// Get the directory location
	public static String getPath(){
		return THUMBNAIL_CACHE_DIR;
	}

	protected static String getLogTag() {
		return TAG;
	}
 
	/////////////////////////////////////////////////////////////////////////
	// File naming conventions (up front so you see this first)
	/////////////////////////////////////////////////////////////////////////

	// We use the "userid" to store files, which is the last (unique) part of the service name
	// It is safe to specify either the full service name or just the userid, either works
	
	// return the userid from the full service name
	private static String getUserid (String service){
		String userid = service;
		if (service.contains(".")){
			userid = service.substring(service.lastIndexOf(".")+1);
		} 
		return userid;
	}


	/////////////////////////////////////////////////////////////////////////
	// Generic stuff (Thumbnail-independent)
	/////////////////////////////////////////////////////////////////////////


	// Run initial checks, make sure directories are there and writeable etc.
	public static void init(){
		if(mCacheReady)
			return;
		mCacheReady = false;

		try {
			// check that storage is accessible
			if (checkStorage()){
				// make sure directory exists
				File lclpath = new File(THUMBNAIL_CACHE_DIR);

				// Make sure the  directory exists.
				lclpath.mkdirs();
				if (lclpath.exists()){
					mCacheReady = true;
					Log.d(TAG, "Thumbnail cache directory set up: "+THUMBNAIL_CACHE_DIR);
				} else {
					Log.e(TAG, "Unkown error, cahce not set up");
				}

			} else {
				Log.e(TAG, "*** External Storage not available, cannot save medias!!! ***");
			}
		} catch (Exception e) {
			Log.e(TAG, "Error setting up media cache ("+THUMBNAIL_CACHE_DIR+"): "+e.toString());
		}
	}//init


	// List available thumbnails
	public static String[] list(){
		String[] plist=new String[0];
		if (mCacheReady){
			// Get the list of medias
			File dir = new File(THUMBNAIL_CACHE_DIR);

			// Define filter that only selects the required files
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.startsWith(THUMBNAIL_THUMBNAIL_TAG))
						return true;
					else
						return false;
				}
			};

			plist = dir.list(filter);
			if (plist == null) {
				Log.d(TAG, "list() No thumbnails found");
			}

		} else {
			Log.e(TAG, "list() - Cache not set up");
		}
		return plist;
	}

	/////////////////////////////////////////////////////////////////////////
	// Thumbnail-Specific
	// There are generally two versions, one with a userid, one without
	// The userid is used as a prefix to help sort out files from different sources with the same name
	/////////////////////////////////////////////////////////////////////////

	// Generate a filename based on the supplied path/name
	// any existing path data will be replaced
	public static String getThumbnailPath (String filename){
		String thumbfile = filename;
		// Extract just the filename
		//filename=filename.replaceAll("\\", "/");
		if (filename.contains("/")){
			thumbfile = filename.substring(filename.lastIndexOf("/")+1);
		} 
		
		// make sure we don't double-process
		String prefix = THUMBNAIL_THUMBNAIL_TAG ;
		if(!thumbfile.startsWith(prefix))
			thumbfile = prefix + thumbfile;
		
		// make sure it's a .jpg file
		if (!thumbfile.endsWith(THUMBNAIL_THUMBNAIL_EXT)){
			thumbfile = thumbfile + THUMBNAIL_THUMBNAIL_EXT;
		}
		
		return  THUMBNAIL_CACHE_DIR + thumbfile;
	}

	// Generate a filename based on the supplied path/name and unique userid
	// Use this version if you need to prefix the filename. Any existing path data will be replaced
	public static String getThumbnailPath (String userid, String filename){
		String thumbfile = filename;
		// Extract just the filename
		//filename=filename.replaceAll("\\", "/");
		if (filename.contains("/")){
			thumbfile = filename.substring(filename.lastIndexOf("/")+1);
		}		
		
		// make sure we don't double-process
		String prefix = THUMBNAIL_THUMBNAIL_TAG + getUserid(userid) + "_";
		if(!thumbfile.startsWith(prefix))
			thumbfile = prefix + thumbfile;

		
		// make sure it's a .jpg file
		if (!thumbfile.endsWith(THUMBNAIL_THUMBNAIL_EXT)){
			thumbfile = thumbfile + THUMBNAIL_THUMBNAIL_EXT;
		}

		return  THUMBNAIL_CACHE_DIR + thumbfile;
	}


	// Check to see if media is already saved
	public static boolean isPresent(String name){
		return isFilePresent(getThumbnailPath(name));
	}

	// Check to see if media is already saved
	public static boolean isPresent(String userid, String name){
		return isFilePresent(getThumbnailPath(userid, name));
	}

	// save the thumbnail to cache with the supplied name
	// note: binary data, so not the same as the text files
	public static void saveThumbnail (String name, byte[] thumbdata){
		if (!mCacheReady) init();
		writeBinaryFile (getThumbnailPath(name), thumbdata);
	}

	// save the photo to cache with the supplied name
	// note: binary data, so not the same as the text files
	public static void saveThumbnail (String userid, String name, byte[] thumbdata){
		if (!mCacheReady) init();
		String lclpath = getThumbnailPath(userid, name);
		Log.v(TAG, "saveThumbnail("+thumbdata.length+"):"+lclpath);
		writeBinaryFile (lclpath, thumbdata);
	}


	// get the media photo from cache
	public static byte[] getThumbnail (String name){
		return readBinaryFile (getThumbnailPath(name));
	}

	// get the media photo from cache
	public static byte[] getThumbnail (String userid, String name){
		return readBinaryFile (getThumbnailPath(userid, name));
	}


	// remove entry from cache
	public static void remove (String name){
		if (mCacheReady){
			// Remove the media
			removeFile (getThumbnailPath(name));
		} else {
			Log.e(TAG, "remove() - Cache not set up");
		}
	}

	// remove entry from cache
	public static void remove (String userid, String name){
		if (mCacheReady){
			// Remove the media
			removeFile (getThumbnailPath(userid, name));
		} else {
			Log.e(TAG, "remove() - Cache not set up");
		}
	}

}//ThumbnailCache
