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
import org.alljoyn.aroundme.R.styleable;
import org.alljoyn.devmodules.*;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;

import java.util.ArrayList;
import java.util.HashMap;


import android.content.Context;
import android.content.res.TypedArray;

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
public class GalleryContactAdapter extends BaseAdapter { 


	private static final String TAG = "GalleryContactAdapter";

	// Arrays for holding the data
	// I keep a separate list of names so that they can be looked up by position rather than key
	private static HashMap<String,ProfileDescriptor> mContactList = new HashMap<String,ProfileDescriptor>();
	private static ArrayList<String> mNameList = new ArrayList<String>(); 

	// Context of the current View
	private static Context mContext; 
	private int mGalleryItemBackground;


	private static GalleryContactAdapter _adapter; // the singleton version

	private static boolean viewAttached = false;


	private GalleryContactAdapter() { 
		// no implementation, just private to control usage

	} 


	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// method to return reference to internal data
	public static synchronized GalleryContactAdapter getAdapter() {
		if (_adapter == null) {
			_adapter = new GalleryContactAdapter();
		}
		return _adapter;
	}

	public static synchronized void setContext (Context context){
		mContext = context;
		viewAttached = true;
	}

	public GalleryContactAdapter(Context c) { 
		mContext = c; 
		viewAttached = true;

		// set the attributes of he selected item (why not in XML?!)
		TypedArray attr = mContext.obtainStyledAttributes(R.styleable.galleryBackground);
		mGalleryItemBackground = attr.getResourceId(R.styleable.galleryBackground_android_galleryItemBackground, 0);
		attr.recycle();
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

	// utility for stripping the name to be just the unique id
	// allows for use of full/partial service name or directory-style names
	private static String extractId(String name){
		String id = name;
		if (id.contains(".")){ id = id.substring(id.lastIndexOf(".")+1); }
		if (id.contains("/")){ id = id.substring(id.lastIndexOf("/")+1); }
		return id;
	}
	
	
	public static synchronized String getProfileId(int position){
		String n = mNameList.get(position);
		//return Utilities.checkString (mContactList.get(n).getField(ProfileDescriptor.ProfileFields._ID));
		return n;
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
			String id = extractId(name);

			try {
				photo = mContactList.get(id).getPhoto();
			} catch (Exception e){
				Log.e(TAG, "getPhoto(): Error decoding photo ("+id+")"+e.toString());
				photo = new byte[0] ; 
			}
		} else {
			Log.e(TAG, "getPhoto(): No name supplied");
			photo = new byte[0] ; 
		}
		return photo;
	}



	public static synchronized boolean contains (String name){
		return (mNameList.contains(name)) ? true : false ;
	}

	
	public static synchronized ProfileDescriptor getProfile (int position){
		String n = mNameList.get(position);
		return mContactList.get(n);	
	}


	public static synchronized ProfileDescriptor getProfile (String name){
		ProfileDescriptor profile = new ProfileDescriptor();
		if (name != null){
			String id = extractId(name);

			if (mNameList.contains(id)){
				profile = mContactList.get(id);	
			} else {
				Log.w(TAG, "getProfile("+id+"): Profile entry not found");
				profile.setField(ProfileDescriptor.ProfileFields.NAME_DISPLAY, "(unknown)");
			}
		}
		return profile;
	}

	// Add an entry using the supplied name
	public static synchronized void add (String name, ProfileDescriptor profile) {
		if (profile!=null){
			String id = extractId(name);
			if (!mNameList.contains(id)){
				Utilities.logMessage(TAG, "Adding profile: "+id);
				profile.setField("profileid", id);
				profile.setField(ProfileDescriptor.ProfileFields._ID, id);
				mContactList.put(id, profile);
				mNameList.add(id);
				update();
			}
		} else {
			Log.e(TAG, "NULL ProfileDescriptor supplied");
		}
	}

	// add an entry, extracting the name from the profile
	public static synchronized void add (ProfileDescriptor profile) {
		if (profile!=null){
			String id = profile.getField("profileid");
			if ((id!=null) && (id.length()>0)){
				id = extractId(id);
				if (!mNameList.contains(id)){
					Utilities.logMessage(TAG, "Adding profile: "+id);
					mContactList.put(id, profile);
					mNameList.add(id);
					update();
				}
			} else {
				Log.e(TAG, "profileid field not set");
			}
		} else {
			Log.e(TAG, "NULL ProfileDescriptor supplied");
		}
	}

	public static synchronized void remove (String name) {
		String id = extractId(name);
		if (mContactList.containsKey(id)){
			mContactList.remove(id);
			mNameList.remove(id);
			update();
		} else {
			Log.e(TAG, "Entry not found: "+id);
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
			convertView = inflater.inflate(R.layout.griditem, parent, false);
		}

		// get the display views
		TextView nameView = (TextView) convertView.findViewById(R.id.grid_item_text);
		ImageView photoIcon = (ImageView) convertView.findViewById(R.id.grid_item_image);

		// Populate name
		String name = getName(position);
		nameView.setText(name);

		// display icon, if specified
		try {

			// get the photo based on the supplied name
			// Note that it is possible to get a  message from a device that does not have
			// a profile registered , or no photo supplied, so we need the code to check for 
			// null and zero-length photos

			byte[] photo = getPhoto(position);
			if ((photo!=null) && photo.length>0){
				Bitmap image = BitmapFactory.decodeByteArray(photo, 0, photo.length);
				photoIcon.setImageBitmap(image);
			} else {
				photoIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_list_person));		
			}

		} catch (Exception e){
			// error somewhere, just set to default icon
			photoIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_list_person));		
		}

		// set the background attribute of the selection
		//convertView.setBackgroundResource(mGalleryItemBackground);
		convertView.setBackgroundResource(R.drawable.gallerycurrentitem_selector);

		return convertView;

	} 

	private static void update() {
		if (_adapter == null) {
			_adapter = new GalleryContactAdapter();
		}
		_adapter.notifyDataSetChanged(); // force re-display of list
	}

} // GalleryContactAdapter
