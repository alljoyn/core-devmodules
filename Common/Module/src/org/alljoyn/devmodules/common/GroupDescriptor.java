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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;


import org.alljoyn.storage.ProfileCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;



public class GroupDescriptor  extends BaseDescriptor {

	private static final String TAG = "GroupDescriptor";

	private ArrayList<String> _members;

	/**
	 * Class to manage Group List and Group Descriptors
	 * @author pprice
	 *
	 */
	public static class GroupFields {
		public static final String NAME          = "name"; 
		public static final String DESCRIPTION   = "description"; 
		public static final String TIME_CREATED  = "timeCreated"; 
		public static final String TIME_MODIFIED = "timeModified"; 
		public static final String MEMBERS       = "members"; 
		public static final String PRIVATE       = "private"; 
		public static final String ENABLED       = "enabled"; 
	}//GroupFields


	public GroupDescriptor () {
		super();
		_members = new ArrayList<String>();
		_members.clear();
	}

	/**
	 * Converts Object to a string
	 * @return the object in string (JSON) format
	 */
	public String toString() {
		try {
			// Add the member array and convert to String
			for (int i=0; i<_members.size(); i++){
				_list.put(i, _members.get(i));
			}
			_object.put(GroupDescriptor.GroupFields.MEMBERS, _list);
		} catch (Exception e) {
			Log.e(TAG, "Error creating JSON String: "+e.toString());
		}

		return _object.toString();
	}

	//
	// GET accessors
	//
	// use generic getField(key) for simple values

	/**
	 *  Get the list of group members
	 * @return String array of members
	 */
	public String[] getMembers(){
		return _members.toArray(new String[_members.size()]);
	}

	/**
	 *  get whether group is private
	 * @return true if private, false if open
	 */
	public boolean isPrivate(){
		boolean result = false; // open by default
		try{
			result = _object.getBoolean(GroupFields.PRIVATE);
		} catch (Exception e){
			// do nothing
		}
		return result;
	}

	/**
	 *  get whether group is enabled (automatic processing)
	 * @return true if enabled false if not
	 */
	public boolean isEnabled(){
		boolean result = true; // enabled by default
		try{
			result = _object.getBoolean(GroupFields.ENABLED);
		} catch (Exception e){
			// do nothing
		}
		return result;
	}

	//
	// SET methods
	//

	// use setField(key, value) for simple fields

	/**
	 *  Set the list of group members
	 * @param members Array of member IDs
	 */
	public void setMembers(String[] members){

		try{
			for (int i=0; i<members.length; i++){
				_members.add(members[i]);
			}
		} catch (Exception e){
			Log.e(getTag(), "Error creating member list: "+e.toString());
		}
	}

	/**
	 *  Add a single member to the existing list
	 * @param member Member ID to be added
	 */
	public void addMember(String member){
		if (!_members.contains(member)){
			_members.add(member);
		}
	}
	
	/**
	 *  Remove a single member from the list
	 * @param member ID of member to be removed
	 */
	public void removeMember(String member){
		if (_members.contains(member)){
			_members.remove(member);
		}
	}
	

	/**
	 *  get whether group is private (true) or not (false)
	 * @param privateflag true for private, false for open
	 */
	public void setPrivacy(boolean privateflag){
		try{
			_object.put(GroupFields.PRIVATE, privateflag);
		} catch (Exception e){
			// do nothing
		}
	}

	/**
	 *  flag group as being private
	 */
	public void setPrivate(){
		setPrivacy(true);
	}
	
	/**
	 *  flag group as being open (public)
	 */
	public void setPublic(){
		setPrivacy(false);
	}
	

	/**
	 *  flag group as enabled(process automatically)
	 */
	public void enable(){
		try{
			_object.put(GroupFields.ENABLED, true);
		} catch (Exception e){
			// do nothing
		}
	}

	/**
	 *  flag group as disabled(no processing)
	 */
	public void disable(){
		try{
			_object.put(GroupFields.ENABLED, false);
		} catch (Exception e){
			// do nothing
		}
	}
	
	@Override
	/**
	 *  sets the internal String, which is then expanded into an object
	 *  @param contents The JSON-formatted string that contains the contents to be expanded
	 */
	public void setString(String contents) {
		setJSONString(contents);
	}

	/**
	 *  Reset the entire object with the specified (JSON) string
	 * @param json The JSON-formatted string that contains the contents to be expanded
	 */
	public synchronized void setJSONString (String json){
		try {
			_string = json;
			_object = new JSONObject (_string);
			// Update the member array from the JSON list
			try{
				_list   = _object.getJSONArray(GroupDescriptor.GroupFields.MEMBERS);
			} catch (Exception e1){
				// No members, just create empty list
				_list = new JSONArray();
			}
			for (int i=0; i<_list.length(); i++){
				_members.add(_list.getString(i));
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Set/replace the internal JSON object
	 * @param jsonObj the JSON object. Overwrites current object
	 */
	public synchronized void setJSONObject (JSONObject jsonObj){
		_object = jsonObj;
		_string = jsonObj.toString();
	}
} // GroupDescriptor
