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
import java.util.ArrayList;


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
 * Class to hold Media Identifier data 
 * The data can be stored/retrieved as a String in JSON format, or accessed via get/set methods for specific fields
 */


/*
 * The structure is equivalent to an array of MediaIntifier instances
 * MediaIdentifier includes the following fields
 * 
 * service        file path on the hosting device
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
public class TransactionListDescriptor  {

	private static final String TAG = "TansactionListDescriptor";

	private String      _string ;
	private JSONArray   _list ;
	private ArrayList<MediaIdentifier> _milist;
	private JSONObject  _object ;

	// Methods to get/set data (thread safe)

	public TransactionListDescriptor () {
		_string = "";
		_list  = new JSONArray();
		_object = new JSONObject();
		_milist = new ArrayList<MediaIdentifier>();
	}


	// checks whether there is anything in the list
	public synchronized boolean isEmpty (){
		return (_milist.size()>0) ? false : true ;
	}


	// Get the number of items in the list
	public synchronized int size (){
		return _milist.size();
	}


	// Convert structure to a JSON-encoded String
	public synchronized String toString(){
		// Build the JSON list then dump to String
		for (int i=0; i<_milist.size();i++){
			try {
				// Build the JSONObject from the MediaIdentifer
				JSONObject jobj = new JSONObject();
				jobj.put("path",      _milist.get(i).path);
				jobj.put("thumbpath", _milist.get(i).thumbpath);
				jobj.put("name",      _milist.get(i).name);
				jobj.put("type",      _milist.get(i).type);
				jobj.put("size",      _milist.get(i).size);
				jobj.put("title",     _milist.get(i).title);
				jobj.put("userid",    _milist.get(i).userid);
				jobj.put("mediatype", _milist.get(i).mediatype);
				jobj.put("localpath", _milist.get(i).localpath);
				jobj.put("timestamp", _milist.get(i).timestamp);

				// Add to the array
				_list.put(jobj);

			} catch (JSONException e) {
				//e.printStackTrace();
			}
		}
		return _list.toString();
	}


	// Build the object based on a JSON-encoded String (the opposite of toString())
	public synchronized void setString(String contents){
		try{
			_list = new JSONArray(contents);
			_milist.clear();
			for (int i=0; i<_list.length(); i++){
				// Extract the Media Identifier
				MediaIdentifier mi = new MediaIdentifier();
				try {
					JSONObject jobj = new JSONObject();
					jobj = _list.getJSONObject(i);
					mi.path      = jobj.getString("path"); 
					mi.thumbpath = jobj.getString("thumbpath"); 
					mi.name      = jobj.getString("name"); 
					mi.type      = jobj.getString("type"); 
					mi.size      = jobj.getInt   ("size"); 
					mi.title     = jobj.getString("title"); 
					mi.userid    = jobj.getString("userid"); 
					mi.mediatype = jobj.getString("mediatype"); 
					mi.localpath = jobj.getString("localpath"); 
					mi.timestamp = jobj.getLong  ("timestamp"); 
					
					// Add the the array
					_milist.add(mi);
				} catch (JSONException e) {
					//e.toString();
				}

			}
		}catch(Exception e){
			Log.e(TAG, "setString(): Error loading Descriptor");
		}
	}


	// return the MediaIdentifier at index 'n'
	public synchronized MediaIdentifier get (int n){ 

		return _milist.get(n);
	}


	// return the entire list as a TansactionList class
	public synchronized MediaIdentifier[] get (){ 
		return _milist.toArray(new MediaIdentifier[size()]);
	}


	// add a MediaIdentifier to the list
	public synchronized void add (MediaIdentifier mi){ 	
		_milist.add(mi);
	}


	// remove an item from the list
	public synchronized void remove (int pos){ 		
		if (_milist.size()>pos){
			_milist.remove(pos);
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
