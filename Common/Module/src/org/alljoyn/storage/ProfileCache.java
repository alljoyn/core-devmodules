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

import org.alljoyn.devmodules.common.ProfileDescriptor;


import android.os.Environment;
import android.util.Log;

// Class to handle caching of profile data in local storage
public class ProfileCache extends BaseCache {

	private static final String TAG                     = "ProfileCache";
	private static final String PROFILE_CACHE_DIR       = "/sdcard/.alljoyn/profile/";
	private static final String PROFILE_DESCRIPTOR_EXT  = ".json";
	private static final String PROFILE_PHOTO_EXT       = ".jpg";
	private static final String PROFILE_CONTACT_EXT     = ".id";
	private static final String PROFILE_NAME_EXT        = ".wkn";
	private static final String PROFILE_CURRENT_CONTACT = PROFILE_CACHE_DIR + "default" + PROFILE_CONTACT_EXT;
	private static final String PROFILE_CURRENT_NAME    = PROFILE_CACHE_DIR + "default" + PROFILE_NAME_EXT;
	private static boolean      mCacheReady = false;



	// Get the directory location
	public static String getPath(){
		return PROFILE_CACHE_DIR;
	}

	protected static String getLogTag() {
		return TAG;
	}


	// We use the "userid" to store files, which is the last (unique) part of the service name
	// It is safe to specify either the full service name or just the userid, either works
	
	// return the userid from the full service name
	private static String getUserid (String service){
		String userid = service;
		if (userid.contains(PROFILE_DESCRIPTOR_EXT)){ userid = userid.substring(0, userid.lastIndexOf("."));} 		
		if (userid.contains(".")){ userid = userid.substring(userid.lastIndexOf(".")+1); } 
		if (userid.contains("/")){ userid = userid.substring(userid.lastIndexOf("/")+1); } 
		return userid;
	}


	// get the actual path of the photo (it does not have to exist, so you can use it in creating the files)
	public static String getPhotoPath (String name){
		return 	PROFILE_CACHE_DIR + getUserid(name) + PROFILE_PHOTO_EXT ;

	}


	// get the actual path of the profile (it does not have to exist, so you can use it in creating the files)
	public static String getProfilePath (String name){
		return 	PROFILE_CACHE_DIR + getUserid(name) + PROFILE_DESCRIPTOR_EXT ;

	}

	
	// Run initial checks, make sure directories are there and writeable etc.
	public static void init(){

		mCacheReady = false;

		try {
			// check that storage is accessible
			if (checkStorage()){
				// make sure directory exists
				File lclpath = new File(PROFILE_CACHE_DIR);

				// Make sure the  directory exists.
				lclpath.mkdirs();
				if (lclpath.exists()){
					mCacheReady = true;
					Log.d(TAG, "Profile cache directory set up: "+PROFILE_CACHE_DIR);
				} else {
					Log.e(TAG, "Unkown error, cahce not set up");
				}

			} else {
				Log.e(TAG, "*** External Storage not available, cannot save profiles!!! ***");
			}
		} catch (Exception e) {
			Log.e(TAG, "Error setting up profile cache ("+PROFILE_CACHE_DIR+"): "+e.toString());
		}
	}//init


	// List available profiles
	public static String[] listProfiles(){
		String[] plist=null;
		if (mCacheReady){
			// Get the list of profiles
			File dir = new File(PROFILE_CACHE_DIR);

			// Define filter that only selects the required files
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.endsWith(PROFILE_DESCRIPTOR_EXT))
						return true;
					else
						return false;
				}
			};

			plist = dir.list(filter);
			if (plist == null) {
				Log.d(TAG, "listProfiles() No profiles found");
			}

		} else {
			Log.e(TAG, "listProfiles() - Cache not set up");
		}
		return plist;
	}


	// test whether current contact has been defined
	public static boolean isContactDefined(){
		return isFilePresent(PROFILE_CURRENT_CONTACT);
	}


	// save the local contact id for the current user
	public static void saveContactId(String contactId){
		Log.d(TAG, "saveContactId("+contactId+")");
		writeTextFile(PROFILE_CURRENT_CONTACT, contactId);
	}


	// retrieve the contact id for the current user. Returns empty string if not found
	public static String retrieveContactId(){
		String contactId = readTextFile(PROFILE_CURRENT_CONTACT);
		Log.d(TAG, "retrieveContactId("+contactId+")");
		return contactId;
	}


	// test whether current contact has been defined
	public static boolean isNameDefined(){
		return isFilePresent(PROFILE_CURRENT_NAME);
	}


	// save the local contact id for the current user
	public static void saveName(String name){
		Log.d(TAG, "saveName("+name+")");
		writeTextFile(PROFILE_CURRENT_NAME, name);
	}


	// retrieve the contact id for the current user. Returns empty string if not found
	public static String retrieveName(){
		String name = readTextFile(PROFILE_CURRENT_NAME);
		Log.d(TAG, "retrieveName("+name+")");
		return name;
	}



	// Check to see if profile is already saved
	public static boolean isPresent(String name){
		return isFilePresent(getProfilePath(name));
	}


	// save the profile to cache with the supplied name
	public static void saveProfile (String name, ProfileDescriptor profile){
		writeTextFile(getProfilePath(name), profile.getJSONString());
	}


	// save the photo to cache with the supplied name
	// note: binary data, so not the same as the text files
	public static void savePhoto (String name, byte[] photo){
		if ((name!=null) && (name.length()>0)){
			writeBinaryFile(getPhotoPath(name), photo);
		} else {
			Log.e(TAG, "savePhoto() invalid name supplied: "+name);
		}
	}


	// get the profile descriptor from cache
	public static ProfileDescriptor getProfile (String name){
		ProfileDescriptor pdescr = null;
		if (isFilePresent(getProfilePath(name))){
			String pstring = readTextFile(getProfilePath(name));
			pdescr = new ProfileDescriptor();
			pdescr.setJSONString(pstring);
		} else {
			Log.e(TAG, "getProfile() Profile does not exist for: "+name);
		}
		return pdescr;
	}


	// get the profile photo from cache
	public static byte[] getPhoto (String name){
		return readBinaryFile (getPhotoPath(name));
	}


	// remove entry from cache
	public static void remove (String name){
		removeFile(getProfilePath(name));
		removeFile(getPhotoPath(name));
	}

}//ProfileCache
