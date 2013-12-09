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
// Holds list of detected peers
// Note: unlike most other adapters, there can be multiple instances of this type, so no static arrays etc.
//       This means that access to the "correct" list has to be managed externally

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.R.drawable;
import org.alljoyn.aroundme.R.id;
import org.alljoyn.aroundme.R.layout;
import org.alljoyn.profileloader.MyProfileData;
import org.alljoyn.devmodules.common.MediaIdentifier;

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
import android.graphics.drawable.Drawable;
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


public class UserSelectionAdapter extends BaseAdapter { 

	private static final String TAG = "UserSelectionAdapter";

	// Items in internal list
	private class ListItem {
		boolean isChecked;
		String  peer;

		ListItem(){
			isChecked = false;
			peer = "";
		}
	}


	// The internal data
	private ArrayList<ListItem>        mSelectedList = null;
	private ArrayList<ListItem>        mCopyList = null;
	private static ContactAdapter     mAdapter ;
	private static ProfileDescriptor  mProfile ;
	private static Drawable           mDefaultThumb;

	// Context of the current Activity
	private Context mContext; 


	// Constructor
	private UserSelectionAdapter() {
		// no implementation, just private to prevent it being called
	}

	public UserSelectionAdapter(Context c) { 
		mContext = c; 
		mSelectedList = new ArrayList<ListItem>();
		mSelectedList.clear();
		
		mProfile = new ProfileDescriptor();
		mDefaultThumb = mContext.getResources().getDrawable(R.drawable.ic_list_person);

	} 


	public synchronized void setContext (Context context){
		mContext = context;
	}

	public int getCount() { 
		if (mSelectedList!=null){
			return mSelectedList.size(); 
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

	public synchronized void add (String peer) {
		if (mSelectedList!=null){
			ListItem li = new ListItem();
			li.isChecked = false;
			li.peer      = peer;
			mSelectedList.add(li);
			notifyDataSetChanged(); // force re-display of list
		}
	}

	public synchronized void remove (int pos) {
		if ((mSelectedList!=null) &&(pos<mSelectedList.size())){
			mSelectedList.remove(pos);
			notifyDataSetChanged(); 
		}
	}

	public synchronized ListItem get (int pos) {
		if ((mSelectedList!=null) &&(pos<mSelectedList.size())){
			return mSelectedList.get(pos);
		} else {
			return new ListItem();
		}
	}


	// accessors for underlying data


	// returns the full profile of the peer
	public synchronized ProfileDescriptor getProfile(int position){
		ProfileDescriptor prof = new ProfileDescriptor();
		try {
			String userid = mSelectedList.get(position).peer;
			// look up profile from cache
			prof = ProfileCache.getProfile (ProfileCache.getProfilePath(userid));
		}catch (Exception e){
			Log.e(TAG, "Error getting profile: "+e.toString());
		}
		return prof;
	}

	// returns the human readable form of the peer
	public synchronized String getName(int position){
		String name = "";
		try {
			String userid = mSelectedList.get(position).peer;
			// look up profile from cache
			ProfileDescriptor prof = new ProfileDescriptor();
			prof = ProfileCache.getProfile (ProfileCache.getProfilePath(userid));
			name =  prof.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
		}catch (Exception e){
			Log.e(TAG, "Error getting Name: "+e.toString());
		}
		return name;
	}

	// return the profile thumbnail
	public synchronized byte[] getThumbnail(int position){
		byte[] photo = new byte[0];

		try {
			String userid = mSelectedList.get(position).peer;
			// look up profile from cache
			ProfileDescriptor prof = new ProfileDescriptor();
			prof = ProfileCache.getProfile (ProfileCache.getProfilePath(userid));
			photo = prof.getPhoto();
		} catch (Exception e){
			Log.e(TAG, "getThumbnail() Error: "+e.toString());
			photo = new byte[0] ; 
		}
		return photo;
	}

	
	// Utility methods for managing selection of item

	public void removeCheckedItems(){
		// Copy unchecked items into new array and replace
		mCopyList = new ArrayList<ListItem>();
		mCopyList.clear();
		for (int i=0; i<mSelectedList.size();i++){
			if (!isChecked(i)){
				mCopyList.add(mSelectedList.get(i));
			} else {
				Log.v(TAG, "Dropping: "+mSelectedList.get(i).peer);
			}
		}
		mSelectedList = mCopyList;
		mCopyList = null;
		notifyDataSetChanged(); // force re-display of list
	}

	public synchronized boolean isChecked (int position){
		return mSelectedList.get(position).isChecked;
	}


	public synchronized void check (int position){
		mSelectedList.get(position).isChecked = true;
	}

	public synchronized void clear (int position){
		mSelectedList.get(position).isChecked = false;
	}


	public synchronized void toggleSelection (int position){
		mSelectedList.get(position).isChecked = !mSelectedList.get(position).isChecked;
	}


	public synchronized void clearAll (){
		for (int i=0; i<mSelectedList.size(); i++){
			clear(i);
		}
		notifyDataSetChanged(); // force re-display of list
	}

	public synchronized void setAll (){
		for (int i=0; i<mSelectedList.size(); i++){
			check(i);
		}
		notifyDataSetChanged(); // force re-display of list
	}

	public synchronized void toggleAll (){
		for (int i=0; i<mSelectedList.size(); i++){
			toggleSelection(i);
		}
		notifyDataSetChanged(); // force re-display of list
	}





	// return the View to be displayed
	public View getView(int position, View convertView, ViewGroup parent) { 

		TextView tv ;


		// Inflate a view template
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
		line1View.setText(getName(position));
		checkView.setChecked(isChecked(position));

		// Process the profile photo thumbnail
		try {
			byte[] bphoto ;

			bphoto = getThumbnail(position);
			if ((bphoto!=null) && (bphoto.length>0)){
				Bitmap image = BitmapFactory.decodeByteArray(bphoto, 0, bphoto.length);
				imageIcon.setImageBitmap(image);
			} else {
				imageIcon.setImageDrawable(mDefaultThumb);			
			}
		} catch (Exception e){
			imageIcon.setImageDrawable(mDefaultThumb);			
		}


		// Set up listeners for image checkbox and the rest of the line

		final int loc = position;

		// Checkbox
		checkView.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				toggleSelection(loc);
				checkView.setChecked(isChecked(loc));
			}
		});


		// Overall line (not so easy hit the checkbox directly)
		Button itemTouched = (Button) convertView.findViewById(R.id.listItemButton);
		itemTouched.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				toggleSelection(loc);
				checkView.setChecked(isChecked(loc));
			}
		});


		return convertView;
	} 


} // UserSelectionAdapter
