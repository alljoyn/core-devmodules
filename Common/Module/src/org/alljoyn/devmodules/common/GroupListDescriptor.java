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
 * Class to hold Group List data 
 * The data can be stored/retrieved as a String in JSON format, or accessed via get/set methods for specific fields
 */


/*
 * The structure is equivalent to an array of key/value pairs
 * 
 * name        group name
 * <TBD>       should probably track creation time, last modified etc.
 * 
 */
public class GroupListDescriptor  {

	private static final String TAG = "GroupListDescriptor";

	private String      _string ;
	private JSONArray   _list ;
	private ArrayList<String> _grouplist;
	private JSONObject  _object ;

	// Methods to get/set data (thread safe)

	public GroupListDescriptor () {
		_string = "";
		_list  = new JSONArray();
		_object = new JSONObject();
		_grouplist = new ArrayList<String>();
		_grouplist.clear();
	}


	// checks whether there is anything in the list
	public synchronized boolean isEmpty (){
		return (_grouplist.size()>0) ? false : true ;
	}


	// Get the number of items in the list
	public synchronized int size (){
		return _grouplist.size();
	}

	// find if an entry is present
	public synchronized boolean contains (String group){
		return _grouplist.contains(group);
	}

	// find the index of an entry
	public synchronized int indexOf (String group){
		return _grouplist.indexOf(group);
	}

	// Convert structure to a JSON-encoded String
	public synchronized String toString(){
		// Build the JSON list then dump to String
		_list  = new JSONArray();
		for (String g: _grouplist){
			try {
				// Build the JSONObject from the MediaIdentifer
				JSONObject jobj = new JSONObject();
				jobj.put("name", g);
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
			_grouplist.clear();
			for (int i=0; i<_list.length(); i++){
				// Extract the Group List from the JSON Array, convert and save
				// into group list
				String name;
				try {
					JSONObject jobj = new JSONObject();
					jobj = _list.getJSONObject(i);
					name      = jobj.getString("name"); 
					if (!_grouplist.contains(name)){ // filter out duplicates
						_grouplist.add(name);
					}
				} catch (JSONException e) {
					//e.toString();
				}

			}
		}catch(Exception e){
			Log.e(TAG, "setString(): Error loading Descriptor");
		}
	}


	// return the String at index 'n'
	public synchronized String get (int n){ 

		return _grouplist.get(n);
	}


	// return the entire list as a a string array
	public synchronized String[] get (){ 
		return _grouplist.toArray(new String[size()]);
	}


	// add a String to the list
	public synchronized void add (String group){ 	
		if (!_grouplist.contains(group)){
			_grouplist.add(group);
		}
	}


	// remove an item from the list, by position
	public synchronized void remove (int pos){ 		
		if (_grouplist.size()>pos){
			_grouplist.remove(pos);
		}
	}


	// remove an item from the list, by name
	public synchronized void remove (String group){ 	
		if (_grouplist.contains(group)){
			int pos = _grouplist.indexOf(group);
			remove(pos);
		} else {
			Log.w(TAG, "Attempt to delete non-existent group: "+group);
		}
	}

	// clear the entire list
	public synchronized void clear (){ 	
		_grouplist.clear();
	}



	// save this object to a file
	public synchronized void saveToFile(String path){
		Log.v(TAG, "saveToFile: "+path+"()");
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

} // GroupDescriptor
