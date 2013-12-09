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


import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.R.id;
import org.alljoyn.aroundme.R.layout;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ChatCache;

import java.util.ArrayList;
import java.util.HashMap;


import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;

import android.widget.ImageView;

import android.widget.TextView;


// Manages the list of Chat Rooms
// Note that it also loads the stored list from cache and updates the cache as
// changes are made. This is a singleton Adapter, so can be accessed from multiple
// Activities.
// You must get a handle to it via ChatRoomAdapter.getAdapter() to use it.




//Adapter to deal with formatted strings
public class ChatRoomAdapter extends BaseAdapter { 


	private static final String TAG = "ChatRoomAdapter";

	// Arrays for holding the data
	//private static ArrayList<String> mGroupList = new ArrayList<String>(); 
	private static GroupListDescriptor mGroupList = null;

	// Context of the current View
	private static Context mContext; 


	private static ChatRoomAdapter _adapter; // the singleton version


	private ChatRoomAdapter() { 
		// no implementation, just private to control usage
	} 


	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// method to return reference to internal data
	public static synchronized ChatRoomAdapter getAdapter() {
		// If first time through, then run through initialisation
		if (_adapter == null) {
			// create singleton version
			_adapter = new ChatRoomAdapter();
			
			mGroupList = new GroupListDescriptor();
		}
		return _adapter;
	}

	public static synchronized void setContext (Context context){
		mContext = context;
	}

	public ChatRoomAdapter(Context c) { 
		mContext = c; 
	} 


	public int getCount() { 
		return mGroupList.size(); 
	} 

	public Object getItem(int position) { 
		return position; 
	} 

	public long getItemId(int position) { 
		return position; 
	} 

	public static synchronized String getRoom(int position){
		return mGroupList.get(position);
	}

	public static synchronized void add (String room) {
		if (!mGroupList.contains(room)){
			Utilities.logMessage(TAG, "Adding room: "+room);
			mGroupList.add(room);
			update();
		}
	}

	public static synchronized void remove (String room) {
		if (mGroupList.contains(room)){
			mGroupList.remove(room);
			update();
			// update saved version
			int pos = mGroupList.indexOf(room);
			mGroupList.remove(pos);
		} else {
			Log.e(TAG, "Entry not found: "+room);
		}
	}

	public static synchronized void clear(){
		mGroupList.clear();
		update();
	}


	// return the View to be displayed
	public View getView(int position, View convertView, ViewGroup parent) { 

		TextView tv ;


		// Inflate a view template
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.simpleitem, parent, false);
		}

		// get the display views
		TextView roomView = (TextView) convertView.findViewById(R.id.itemText);

		// Populate template
		roomView.setText(getRoom(position));

		return convertView;

	} 

	private static void update() {
		if (_adapter == null) {
			_adapter = new ChatRoomAdapter();
		}
		_adapter.notifyDataSetChanged(); // force re-display of list
	}

} // ChatRoomAdapter
