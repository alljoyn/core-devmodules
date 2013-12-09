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
package org.alljoyn.aroundme.Adapters;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.R.drawable;
import org.alljoyn.aroundme.R.id;
import org.alljoyn.aroundme.R.layout;
import org.alljoyn.profileloader.MyProfileData;
import org.alljoyn.devmodules.*;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;

import java.util.ArrayList;
import java.util.HashMap;


import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;

import android.util.Log;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;

import android.widget.ImageView;

import android.widget.TextView;


// List Management for profile data




//Adapter to deal with formatted strings
public class ContactAdapter extends BaseAdapter { 


	private static final String TAG = "ContactAdapter";

	// Arrays for holding the data
	// I keep a separate list of names so that they can be looked up by position rather than key
	private static HashMap<String,ProfileDescriptor> mContactList = new HashMap<String,ProfileDescriptor>();
	private static ArrayList<String> mNameList = new ArrayList<String>(); 

	// Context of the current View
	private static Context mContext; 


	private static ContactAdapter _adapter; // the singleton version

	private static boolean viewAttached = false;


	private ContactAdapter() { 
		// no implementation, just private to control usage
	} 


	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// method to return reference to internal data
	public static synchronized ContactAdapter getAdapter() {
		if (_adapter == null) {
			_adapter = new ContactAdapter();
		}
		return _adapter;
	}

	public static synchronized void setContext (Context context){
		mContext = context;
		viewAttached = true;
	}

	public ContactAdapter(Context c) { 
		mContext = c; 
		viewAttached = true;
	} 


	public int getCount() { 
		return mNameList.size(); 
	} 

	public Object getItem(int position) { 
		return position; 
	} 

	public long getItemId(int position) { 
		return position; 
	} 
	// Get the number of items in the list
	public static synchronized int size (){
		return mNameList.size();
	}

	public static synchronized String getProfileId(int position){
		String n = mNameList.get(position);
		return Utilities.checkString (mContactList.get(n).getField("profileid"));
	}

	public static synchronized String[] getNameList(){
		return mNameList.toArray(new String[size()]);
	}

	public static synchronized String getName(int position){
		String n = mNameList.get(position);
		return Utilities.checkString (mContactList.get(n).getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY));
	}

	public static synchronized String getNumber(int position){
		String n = mNameList.get(position);
		return Utilities.checkString (mContactList.get(n).getField(ProfileDescriptor.ProfileFields.PHONE_HOME));
	}

	public static synchronized byte[] getPhoto(int position){
		String n = mNameList.get(position);
		return getPhoto(n);
	}

	public static synchronized byte[] getPhoto(String name){
		byte[] photo ;

		if (name != null){
			try {
				photo = mContactList.get(name).getPhoto();
			} catch (Exception e){
				Log.e(TAG, "getPhoto: Error getting photo for: "+name+" - "+e.toString());
				photo = new byte[0] ; 
			}
		} else {
			Log.e(TAG, "getPhoto: No name supplied");
			photo = new byte[0] ; 
		}
		return photo;
	}



	public static synchronized ProfileDescriptor getProfile (int position){
		String n = mNameList.get(position);
		return mContactList.get(n);	
	}


	public static synchronized ProfileDescriptor getProfile (String name){
		if (name != null){
			return mContactList.get(name);	
		} else {
			return null ;
		}
	}

	public static synchronized void add (ProfileDescriptor profile) {
		if (profile!=null){
			String name = profile.getField("profileid)");
			if (!mNameList.contains(name)){
				Utilities.logMessage(TAG, "Adding profile: "+name);
				mContactList.put(name, profile);
				mNameList.add(name);
				update();
			}
		} else {
			Log.e(TAG, "NULL ProfileDescriptor supplied");
		}
	}

	public static synchronized void remove (String n) {
		if (mContactList.containsKey(n)){
			mContactList.remove(n);
			mNameList.remove(n);
			update();
		} else {
			Log.e(TAG, "Entry not found: "+n);
		}
	}

	public static synchronized void clear(){
		mContactList.clear();
		mNameList.clear();
		update();
	}


	// return the View to be displayed
	public View getView(int position, View convertView, ViewGroup parent) { 

		TextView tv ;


		// Inflate a view template
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.contactitem, parent, false);
		}

		// get the display views
		TextView nameView = (TextView) convertView.findViewById(R.id.contactName);
		TextView numberView = (TextView) convertView.findViewById(R.id.contactNumber);
		ImageView photoIcon = (ImageView) convertView.findViewById(R.id.contactIcon);

		// Populate template
		String name = getName(position);
		if (name.equals(MyProfileData.getProfile().getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY))){
			// If this is the current user then add identifier and change background colour
			name = name + " (Me)" ; 
			convertView.setBackgroundColor(0x2262B2A6); // light green
			nameView.setTypeface(null, Typeface.ITALIC);
		} else {
			// Views are re-used, so can't assume default values
			convertView.setBackgroundColor(0xAAFFFFFF);  // white
			nameView.setTypeface(null, Typeface.NORMAL);
		}
		nameView.setText(name);

		numberView.setText(getNumber(position));

		try {

			// get the photo based on the supplied name
			// Note that it is possible to get a  message from a device that does not have
			// a profile registered , or no photo supplied, so we need the code to check for 
			// null and zero-length photos
			byte[] bphoto = getPhoto(position);
			if ((bphoto!=null) && (bphoto.length>0)){
				Bitmap image = BitmapFactory.decodeByteArray(bphoto, 0, bphoto.length);
				photoIcon.setImageBitmap(image);
			} else {
				photoIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_list_person));			
			}

		} catch (Exception e){
			// error somewhere, just set to default icon
			photoIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_list_person));		
		}

		return convertView;

	} 

	private static void update() {
		if (_adapter == null) {
			_adapter = new ContactAdapter();
		}
		_adapter.notifyDataSetChanged(); // force re-display of list
	}

} // ContactAdapter
