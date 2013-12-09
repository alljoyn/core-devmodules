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

//
// Holds list of transactions waiting to be approved/rejected
// Note: unlike most other adapters, there can be multiple instances of this type, so no static arrays etc.
//       This means that access to the "correct" list has to be managed externally

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.aroundme.R.drawable;
import org.alljoyn.aroundme.R.id;
import org.alljoyn.aroundme.R.layout;
import org.alljoyn.devmodules.common.ProfileDescriptor;

import org.alljoyn.devmodules.common.MediaUtilities;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.TransactionListDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.MediaTransactionCache;
import org.alljoyn.storage.ProfileCache;
import org.alljoyn.storage.ThumbnailCache;



import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MemberAdapter extends BaseAdapter { 

	private static final String TAG = "MemberAdapter";


	// Items in internal list
	private class ListItem {
		boolean         isChecked;
		ProfileDescriptor profile;

		ListItem(){
			isChecked = false;
			profile = new ProfileDescriptor();
		}
	}

	// The internal data
	private ArrayList<ListItem>         mProfileList = null;
	private ArrayList<ListItem>         mCopyList = null;

	// Context of the current Activity
	private Context mContext; 

	// Default icon
	private int mDefaultIcon = R.drawable.ic_list_person;

	// Constructor
	public MemberAdapter(Context c) { 
		mContext = c; 
		//mProfileList = new TransactionListDescriptor();
		mProfileList = new ArrayList<ListItem>();
		mProfileList.clear();
	} 

	// Allow caller to set the default icon
	public void setDefaultIcon (int resource){
		mDefaultIcon = resource;
	}

	public int getDefaultIcon (){
		return mDefaultIcon ;
	}

	public synchronized void setContext (Context context){
		mContext = context;
	}

	public int getCount() { 
		if (mProfileList!=null){
			return mProfileList.size(); 
		} else {
			return 0;
		}
	} 

	public Object getItem(int position) { 
		return position; 
	} 

	public long getItemId(int position) { 
		return position; 
	} 

	public synchronized void add (ProfileDescriptor profile, boolean selected) {
		if (mProfileList!=null){
			ListItem li = new ListItem();
			li.isChecked = selected;
			li.profile = profile;
			mProfileList.add(li);
			notifyDataSetChanged(); // force re-display of list
		}
	}

	public synchronized void remove (int pos) {
		if ((mProfileList!=null) &&(pos<mProfileList.size())){
			mProfileList.remove(pos);
			notifyDataSetChanged(); 
		}
	}

	public synchronized ProfileDescriptor get (int pos) {
		if ((mProfileList!=null) &&(pos<mProfileList.size())){
			return mProfileList.get(pos).profile;
		} else {
			return new ProfileDescriptor();
		}
	}


	// accessors for underlying data

	public synchronized String getDisplayName(int position){
		ProfileDescriptor profile = get(position);
		return profile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
	}

	public synchronized String getID(int position){
		// HACK: ID storage has changed in various versions, so check for various locations
		String id = "";
		ProfileDescriptor profile = get(position);
		id = profile.getField(ProfileDescriptor.ProfileFields._ID);
		if ((id==null) || (id.length()==0)){
			id = profile.getField("userid");
			if ((id==null) || (id.length()==0)){
				id = profile.getField("profileid");
			}
			
			// If found, then save it back to cache
			if ((id==null) || (id.length()==0)){
				Log.v(TAG, "Fixing Profile ID: "+id);
				if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
				profile.setField(ProfileDescriptor.ProfileFields._ID, id);
				ProfileCache.saveProfile(id, profile);
			} else {
				Log.e(TAG, "Could not determine ID of profile: "+profile);
			}
		}
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);

		return id;
	}


	public synchronized byte[] getPhoto(int position){
		return get(position).getPhoto();
	}



	/**
	 * Returns the list of member IDs selected
	 * @return String array containing the list of selected members. Zero length if none selected.
	 */
	public String[] getSelectedMembers(){

		// figure out how many entries
		int mcount = 0;
		for (int i=0; i<mProfileList.size();i++){
			if (isChecked(i)){
				mcount++;
			}
		}

		// Create return list of correct size and copy IDs to it
		String [] mlist = new String[mcount];
		int j = 0;
		for (int i=0; i<mProfileList.size();i++){
			if (isChecked(i)){
				mlist[j] = getID(i);
				j++;
			}
		}
		return mlist;
	}

	public synchronized boolean isChecked (int position){
		return mProfileList.get(position).isChecked;
	}


	public synchronized void check (int position){
		mProfileList.get(position).isChecked = true;
	}


	public synchronized void toggleSelection (int position){
		mProfileList.get(position).isChecked = !mProfileList.get(position).isChecked;
	}


	public synchronized void toggleAll (){
		for (int i=0; i<mProfileList.size(); i++){
			toggleSelection(i);
		}
		notifyDataSetChanged(); // force re-display of list
	}





	// return the View to be displayed
	public View getView(int position, View convertView, ViewGroup parent) { 


		// Inflate a view template
		//TEMPHACK: re-using transaction checked item layout
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.checkeditem, parent, false);
		}

		// get the display views
		ImageView      imageIcon = (ImageView) convertView.findViewById(R.id.imageIcon);
		TextView       line1View = (TextView)  convertView.findViewById(R.id.line1);
		TextView       line2View = (TextView)  convertView.findViewById(R.id.line2);
		TextView       line3View = (TextView)  convertView.findViewById(R.id.line3);
		final CheckBox checkView = (CheckBox) convertView.findViewById(R.id.itemSelect);

		// Populate fields
		line1View.setText(getDisplayName(position));
		line2View.setText(getID(position));  // Show something else, e.g. Number?
		line3View.setVisibility(View.GONE);  // not using for now

		checkView.setChecked(isChecked(position));

		// Set up the profile thumbnail
		try {

			// get the photo based on the profile
			// Note that it is possible to get a  profile from a device that does not have
			// a profile registered , or no photo supplied, so we need the code to check for 
			// null and zero-length photos

			byte[] photo = getPhoto(position);
			if ((photo!=null) && photo.length>0){
				Bitmap image = BitmapFactory.decodeByteArray(photo, 0, photo.length);
				imageIcon.setImageBitmap(image);
			} else {
				imageIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_list_person));		
			}

		} catch (Exception e){
			// error somewhere, just set to default icon
			imageIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_list_person));		
		}


		// Set up listeners for image thumbnail, checkbox and the rest of the line

		final int loc = position;

		// Checkbox
		checkView.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				toggleSelection(loc);
				checkView.setChecked(isChecked(loc));
			}
		});


		// Overall line - act like the checkbox was touched
		Button itemTouched = (Button) convertView.findViewById(R.id.listItemButton);
		itemTouched.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				toggleSelection(loc);
				checkView.setChecked(isChecked(loc));
			}
		});


		// Thumbnail - launch profile viewer activity
		imageIcon.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				String id = getID(loc);
				Intent intent = new Intent();
				intent.setAction(AppConstants.INTENT_PREFIX + ".DETAILS");
				intent.putExtra(AppConstants.INTENT_PREFIX + ".details.name", id);

				// Start the viewer for this profile
				try{
					mContext.startActivity(intent);
				} catch (Throwable t){
					Toast.makeText(mContext, "Error starting Details Activity", Toast.LENGTH_SHORT).show();
				}

			}
		});

		return convertView;
	} 


} // MemberAdapter
