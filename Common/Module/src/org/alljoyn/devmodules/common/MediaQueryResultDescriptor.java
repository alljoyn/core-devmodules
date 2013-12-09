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
package org.alljoyn.devmodules.common;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;


import org.alljoyn.bus.annotation.Position;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.util.Log;


/*
 * Class to hold profile data 
 * The data can be stored/retrieved as a String in JSON format, or accessed via get/set methods for specific fields
 */


/*
 * The structure is equivalent to an array of MediaIntifier instances
 * MediaIdentifier includes the following fields
 * 
 * path        file path on the hosting device
 * thumbpath   Location of Thumbnail/icon path, if any
 * name        (file) name, without path
 * type        MIME type
 * size        size in bytes
 * title       Descriptive title
 * userid      User name/id of the source
 * mediatype   Media type (see MediaQueryConstants)
 * localpath   Path where file is stored on local device (not remote/source device)
 * 
 */
public class MediaQueryResultDescriptor  {

	private static final String TAG = "MediaQueryResultDescriptor";

	private String      _string ;
	private JSONArray   _list ;
	private JSONObject  _object ;

	// Methods to get/set data (thread safe)

	public MediaQueryResultDescriptor () {
		_string = "";
		_list  = new JSONArray();
		_object = new JSONObject();
	}

	
	// checks whether there is anything in the list
	public synchronized boolean isEmpty (){
		return (_list.length()>0) ? false : true ;
	}

	
	// Get the number of items in the list
	public synchronized int size (){
		return _list.length();
	}
	
	
	// Convert structure to a JSON-encoded String
	public synchronized String toString(){
		return _list.toString();
	}
	
	
	// Build the object based on a JSON-encoded String (the opposite of toString())
	public synchronized void setString(String contents){
		try{
		_list = new JSONArray(contents);
		}catch(Exception e){
			Log.e(TAG, "setString(): Error loading Descriptor");
		}
	}
	
	
	// return the MediaIdentifier at index 'n'
	public synchronized MediaIdentifier get (int n){ 
		MediaIdentifier mi = new MediaIdentifier();
		try {
			JSONObject jobj = new JSONObject();
			jobj = _list.getJSONObject(n);
			mi.path      = jobj.getString("path"); 
			mi.thumbpath = jobj.getString("thumbpath"); 
			mi.name      = jobj.getString("name"); 
			mi.type      = jobj.getString("type"); 
			mi.size      = jobj.getInt   ("size"); 
			mi.title     = jobj.getString("title"); 
			mi.userid    = jobj.getString("userid"); 
			mi.mediatype = jobj.getString("mediatype"); 
			mi.localpath = jobj.getString("localpath"); 
			
		} catch (JSONException e) {
			//e.toString();
			return mi;
		}
		return mi;
	}

	
	// return the entire list as a MediaQueryResult class
	public synchronized MediaQueryResult get (){ 
		MediaQueryResult mqr = new MediaQueryResult();
		try {
			mqr.count = size();
			mqr.media = new MediaIdentifier[mqr.count];
			for (int i=0; i<0; i++){
				mqr.media[i] = get(i);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error getting result list: "+e.toString());
		}
		return mqr;
	}

	
	// add a MediaIdentifier to the list
	public synchronized void add (MediaIdentifier mi){ 		
		try {
			// Build the JSONObject from the MediaIdentifer
			JSONObject jobj = new JSONObject();
			jobj.put("path",      mi.path);
			jobj.put("thumbpath", mi.thumbpath);
			jobj.put("name",      mi.name);
			jobj.put("type",      mi.type);
			jobj.put("size",      mi.size);
			jobj.put("title",     mi.title);
			jobj.put("userid",    mi.userid);
			jobj.put("mediatype", mi.mediatype);
			jobj.put("localpath", mi.localpath);
			
			// Add to the array
			_list.put(jobj);

		} catch (JSONException e) {
			//e.printStackTrace();
		}
	}

	
	// add a MediaQueryResult to the list
	public synchronized void add (MediaQueryResult mqr){ 		
		try {
			for (int i=0; i<mqr.count; i++){
				add (mqr.media[i]);
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}

	
	// save this object to a file
	public synchronized void saveToFile(String path){
		Log.v(TAG, "writeFile: "+path+"()");
		_string = toString();
		if ((_string!=null) && (_string.length()>0)){
			try {
				FileWriter pfile = new FileWriter (path);
				pfile.write(_string);
				pfile.close();
			} catch (Exception e) {
				Log.e(TAG, "saveToFile() Error writing to file");
			}
		} else {
			Log.e(TAG, "saveToFile() no data present");
		}

	}
	
	// populate this object from a file
	public synchronized void loadFromFile(String path){
		Log.v(TAG, "loadFromFile ("+path+")");
		File f = new File (path);
		if (f.exists()){
			try {
				FileReader file = new FileReader (f);
				BufferedReader buf = new BufferedReader(file);
				_string = buf.readLine();
				buf.close();
				setString(_string);
			} catch (Exception e) {
				Log.e(TAG, "loadFromFile() Error reading file: "+f.getPath());
			}
		} else {
			Log.e(TAG, "loadFromFile() file does not exist: "+path);
		}

	}

} // ProfileDescriptor
