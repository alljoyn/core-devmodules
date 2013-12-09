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
 * Class to hold Message List data 
 * The data can be stored/retrieved as a String in JSON format, or accessed via get/set methods for specific fields
 */


/*
 * The following fields are supported for each entry:
 * 
 * timestamp   Timestamp of message
 * sender      ID of the sender
 * content     The text of the message
 * 
 */
public class MessageListDescriptor  {

	private static final String TAG = "TansactionListDescriptor";

	private String      _string ;
	private JSONArray   _list ;
	private ArrayList<MessageIdentifier> _msglist;
	private JSONObject  _object ;

	// Methods to get/set data (thread safe)

	public MessageListDescriptor () {
		_string = "";
		_list  = new JSONArray();
		_object = new JSONObject();
		_msglist = new ArrayList<MessageIdentifier>();
	}


	// checks whether there is anything in the list
	public synchronized boolean isEmpty (){
		return (_msglist.size()>0) ? false : true ;
	}


	// Get the number of items in the list
	public synchronized int size (){
		return _msglist.size();
	}


	// Convert structure to a JSON-encoded String
	public synchronized String toString(){
		// Build the JSON list then dump to String
		for (int i=0; i<_msglist.size();i++){
			try {
				// Build the JSONObject from the MessageIdentifier
				JSONObject jobj = new JSONObject();
				jobj.put("timestamp", _msglist.get(i).timestamp);
				jobj.put("room",      _msglist.get(i).room);
				jobj.put("sender",    _msglist.get(i).sender);
				jobj.put("contents",  _msglist.get(i).contents);
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
			_msglist.clear();
			for (int i=0; i<_list.length(); i++){
				// Extract the Message List
				MessageIdentifier mi = new MessageIdentifier();
				try {
					JSONObject jobj = new JSONObject();
					jobj = _list.getJSONObject(i);
					mi.timestamp = jobj.getLong("timestamp"); 
					mi.room      = jobj.getString("room");
					mi.sender    = jobj.getString("sender");
					mi.contents  = jobj.getString("contents");
					_msglist.add(mi);
				} catch (JSONException e) {
					//e.toString();
				}

			}
		}catch(Exception e){
			Log.e(TAG, "setString(): Error loading Descriptor");
		}
	}


	// return the MessageIdentifier at index 'n'
	public synchronized MessageIdentifier get (int n){ 

		return _msglist.get(n);
	}


	// return the entire list as a TansactionList class
	public synchronized MessageIdentifier[] get (){ 
		return _msglist.toArray(new MessageIdentifier[size()]);
	}


	// add a MessageIdentifier to the list
	public synchronized void add (MessageIdentifier mi){ 	
		_msglist.add(mi);
	}


	// remove an item from the list
	public synchronized void remove (int pos){ 		
		if (_msglist.size()>pos){
			_msglist.remove(pos);
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
