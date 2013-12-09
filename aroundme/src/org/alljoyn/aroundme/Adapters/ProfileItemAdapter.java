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


import java.util.ArrayList;
import java.util.HashMap;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.R.id;
import org.alljoyn.aroundme.R.layout;



import android.content.Context;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;

import android.widget.TextView;
import android.widget.Toast;

// List Management for profile fields
// Note that this is *not* a singleton list, so can be created multiple times




//Adapter to deal with formatted strings
public class ProfileItemAdapter extends BaseAdapter { 
	
	private static final String TAG = "ProfileItemAdapter";

	// Arrays for holding the data
	private ArrayList<String> mFieldList ; 
	private ArrayList<String> mValueList ; 

	// Context of the current Activity
	private Context mContext; 
	


	public ProfileItemAdapter(Context c) { 
		mContext = c; 
		mFieldList = new ArrayList<String>(); 
		mValueList = new ArrayList<String>(); 
	} 


	private ProfileItemAdapter() { 
		// no implementation, just private to make sure Context is supplied
	} 


	public int getCount() { 
		return mFieldList.size(); 
	} 

	public Object getItem(int position) { 
		return position; 
	} 

	public long getItemId(int position) { 
		return position; 
	} 

	public synchronized void add (String key, String value) {
		mFieldList.add(key);
		mValueList.add(value);
		update();
	}
	
	public synchronized void clear(){
		mFieldList.clear();
		mValueList.clear();
		update();
	}
	

	// return the View to be displayed
	public View getView(int position, View convertView, ViewGroup parent) { 

		// Inflate a view template
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.profileitem, parent, false);
		}

		// get the display views
		TextView fieldView = (TextView) convertView.findViewById(R.id.pfield);
		TextView valueView = (TextView) convertView.findViewById(R.id.ptext);

		fieldView.setText(mFieldList.get(position)+": "); 
		valueView.setText(mValueList.get(position));

		return convertView;

	} 



	public void update() {
		
		if (mContext != null) {
			notifyDataSetChanged(); // force re-display of list
		}
	}

} // ProfileItemAdapter
