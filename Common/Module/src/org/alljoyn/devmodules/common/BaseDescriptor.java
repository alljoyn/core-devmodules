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


abstract public class BaseDescriptor  {

	private static final String TAG = "BaseDescriptor";

	protected String      _string ;
	protected JSONArray   _list ;
	protected JSONObject  _object ;

	public BaseDescriptor () {
		_string = "";
		_list  = new JSONArray();
		_object = new JSONObject();
	}



	protected static String getTag() {
		return TAG;
	}

	
	// Convert structure to a JSON-encoded String
	public abstract String toString();


	// Build the object based on a JSON-encoded String (the opposite of toString())
	public abstract void setString(String contents);


	// Set named field
	public synchronized void setField (String field, String value){
		try {
			_object.put(field, value);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Log.e(TAG, "Error setting field("+field+"): "+e.toString());
		}
	}

	// generic method to retrieve the list of field names present (not all have to be there)
	public synchronized String[] getFieldList(){
		JSONArray keylist = _object.names();
		String[] list = new String[keylist.length()];
		for (int i=0; i<keylist.length(); i++){
			try{
				list[i] = keylist.getString(i);
			} catch (Exception e) {
				// ignore
			}
		}
		return list;
	}

	// generic method to return a named (String) field. This allows app-defined fields to be retrieved
	public synchronized String getField (String field){ 		
		try {
			return _object.getString(field);
		} catch (JSONException e) {
			//e.printStackTrace();
			//Log.e(TAG, "Error getting field("+field+"): "+e.toString());
			return "";
		}
	}


	// save this object to a file
	public void saveToFile(String path){
		Log.v(getTag(), "writeFile: "+path+"()");
		_string = toString();
		if ((_string!=null) && (_string.length()>0)){
			try {
				FileWriter pfile = new FileWriter (path);
				pfile.write(_string);
				pfile.close();
			} catch (Exception e) {
				Log.e(getTag(), "saveToFile() Error writing to file");
			}
		} else {
			Log.e(getTag(), "saveToFile() no data present");
		}

	}

	// populate this object from a file
	public void loadFromFile(String path){
		Log.v(getTag(), "loadFromFile ("+path+")");
		File f = new File (path);
		if (f.exists()){
			try {
				FileReader file = new FileReader (f);
				BufferedReader buf = new BufferedReader(file);
				_string = buf.readLine();
				buf.close();
				setString(_string);
			} catch (Exception e) {
				Log.e(getTag(), "loadFromFile() Error reading file: "+f.getPath());
			}
		} else {
			Log.e(getTag(), "loadFromFile() file does not exist: "+path);
		}

	}

} // ProfileDescriptor
