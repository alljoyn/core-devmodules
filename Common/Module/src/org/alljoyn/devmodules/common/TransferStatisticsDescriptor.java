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
 * Class to hold File Transfer Statistics 
 * The data can be stored/retrieved as a String in JSON format, or accessed via get/set methods for specific fields
 */


/*
 * The structure is equivalent to an array of TransferStatistics instances
 * TransferStatistics includes the following fields
 * 
 * name        (file) name, without path
 * size        size in bytes
 * time        time (msec) to transfer file
 * mean        mean rate (bits/sec)
 * stddev      standard deviation
 * result      pass/fail (boolean)
 * 
 */
public class TransferStatisticsDescriptor  {

	private static final String TAG = "TransferStatisticsDescriptor";

	private String      _string ;
	private JSONArray   _list ;
	private ArrayList<TransferStatistics> _statslist;
	private JSONObject  _object ;

	// Methods to get/set data (thread safe)

	public TransferStatisticsDescriptor () {
		_string = "";
		_list  = new JSONArray();
		_object = new JSONObject();
		_statslist = new ArrayList<TransferStatistics>();
	}


	// checks whether there is anything in the list
	public synchronized boolean isEmpty (){
		return (_statslist.size()>0) ? false : true ;
	}


	// Get the number of items in the list
	public synchronized int size (){
		return _statslist.size();
	}


	// Convert structure to a JSON-encoded String
	public synchronized String toString(){
		// Build the JSON list then dump to String
		for (int i=0; i<_statslist.size();i++){
			try {
				// Build the JSONObject from the MediaIdentifer
				JSONObject jobj = new JSONObject();
				jobj.put("name",   _statslist.get(i).getFilename());
				jobj.put("size",   _statslist.get(i).getFileSize());
				jobj.put("time",   _statslist.get(i).getFileTxTime());
				jobj.put("mean",   _statslist.get(i).getMeanBufferRate());
				jobj.put("stddev", _statslist.get(i).getStdDev());
				jobj.put("result", _statslist.get(i).getResult());

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
			_statslist.clear();
			for (int i=0; i<_list.length(); i++){
				// Extract the Transfer Statistics
				try {
					JSONObject jobj = new JSONObject();
					jobj = _list.getJSONObject(i);
					TransferStatistics stats = new TransferStatistics(jobj.getString("name"));
					stats.setFileSize (jobj.getInt("size"));
					stats.setStartTime(0.0);
					stats.setEndTime  (jobj.getLong("time"));
					stats.setMeanRate (jobj.getLong("mean"));
					stats.setStdDev   (jobj.getLong("stddev"));
					stats.setResult   (jobj.getBoolean("result"));
					
					// Add the the array
					_statslist.add(stats);
				} catch (JSONException e) {
					//e.toString();
				}

			}
		}catch(Exception e){
			Log.e(TAG, "setString(): Error loading Descriptor");
		}
	}


	// return the TransferStatistics at index 'n'
	public synchronized TransferStatistics get (int n){ 

		return _statslist.get(n);
	}


	// return the entire list as a TansactionList class
	public synchronized TransferStatistics[] get (){ 
		return _statslist.toArray(new TransferStatistics[size()]);
	}


	// add a TransferStatistics to the list
	public synchronized void add (TransferStatistics stats){ 	
		_statslist.add(stats);
	}


	// remove an item from the list
	public synchronized void remove (int pos){ 		
		if (_statslist.size()>pos){
			_statslist.remove(pos);
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
