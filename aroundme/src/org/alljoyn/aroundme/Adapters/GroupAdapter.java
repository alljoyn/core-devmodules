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
import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ChatCache;
import org.alljoyn.storage.GroupCache;

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
import android.widget.Gallery;

import android.widget.ImageView;

import android.widget.TextView;


// Manages the list of Groups. *Not* a singleton object, since you may need multiple lists



//Adapter to deal with formatted strings
public class GroupAdapter extends BaseAdapter { 


	private static final String TAG = "GroupAdapter";

	// Arrays for holding the data
	//private static ArrayList<String> mGroupList = new ArrayList<String>(); 
	private  GroupListDescriptor mGroupList = null;

	// Context of the current View
	private  Context mContext; 


	private GroupAdapter() { 
		// no implementation, just private to control usage
	} 


	public synchronized void setContext (Context context){
		mContext = context;
	}

	public GroupAdapter(Context c) { 
		mGroupList = new GroupListDescriptor();
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

	public String getName(int position){
		return mGroupList.get(position);
	}

	public boolean contains (String group) {
		return mGroupList.contains(group);
	}

	public void add (String group) {
		if (!mGroupList.contains(group)){
			Utilities.logMessage(TAG, "Adding group: "+group);
			mGroupList.add(group);
			update();
		}
	}

	public void remove (String group) {
		if (mGroupList.contains(group)){
			mGroupList.remove(group);
			update();
		} else {
			Log.e(TAG, "Entry not found: "+group);
		}
	}

	public void clear(){
		mGroupList.clear();
		update();
	}


	// return the View to be displayed
	public View getView(int position, View convertView, ViewGroup parent) { 

		TextView vText;
		Gallery  vGallery;
		String[] members;      // List of Group Members


		// Inflate a view template
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
			//convertView = inflater.inflate(R.layout.groupitem, parent, false);
			convertView = inflater.inflate(R.layout.simpleitem, parent, false);
		}

		// get the display views
		vText    = (TextView) convertView.findViewById(R.id.itemText);
		vGallery = (Gallery) convertView.findViewById(R.id.gallery);

		// Populate template
		String group = getName(position);
		
		// get the associated GroupDescriptor and update with current group parameters
		GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
		
		vText.setTypeface(null, Typeface.NORMAL);
		vText.setTextColor(mContext.getResources().getColor(R.color.green_light));
		
		// if private then append "(private)" and change colour
		if (gd.isPrivate()){
			group += " (private)";
			vText.setTextColor(mContext.getResources().getColor(R.color.blue_light));
		}
		
		// If disabled, then set typeface to italic, append "(disabled)"
		if (!gd.isEnabled()){
			group += " (disabled)";
			vText.setTypeface(null, Typeface.ITALIC);
			vText.setTextColor(mContext.getResources().getColor(R.color.grey));
		}
		
		vText.setText(group);

		return convertView;

	} 

	private void update() {

		this.notifyDataSetChanged(); // force re-display of list
	}

} // GroupAdapter
