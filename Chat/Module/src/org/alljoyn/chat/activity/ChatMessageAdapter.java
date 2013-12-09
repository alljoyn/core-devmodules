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
package org.alljoyn.chat.activity;

//
// List of Chat messages for a group
// Note: unlike most other adapters, there can be multiple instances of this type 
//       (because we need multiple message lists)
//       This means that access to the "correct" list has to be managed externally

import java.util.ArrayList;
import org.alljoyn.devmodules.common.MessageIdentifier;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.devmodules.util.Utility;
import org.alljoyn.storage.ProfileCache;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class ChatMessageAdapter extends BaseAdapter { 
	
	private static final String TAG = "ChatMessageAdapter";
	
	// Arrays for holding the data
	private ArrayList<MessageIdentifier> mMsgList = null; 

	// Context of the current Activity
	private Activity mActivity; 
	
	private  ProfileDescriptor  mProfile ;

	private static Drawable mDefaultThumb;

	// make default constructor private, to force provision of Context
	private ChatMessageAdapter() { 
	} 

	public ChatMessageAdapter(Activity activity) { 
		mActivity = activity; 
		mMsgList = new ArrayList<MessageIdentifier>();
		mProfile = new ProfileDescriptor();
		mDefaultThumb = mActivity.getResources().getDrawable(Utility.getResourseIdByName(activity.getPackageName(),"drawable","person"));
	} 


	public synchronized void setContext (Activity activity){
		mActivity = activity;
	}

	public int getCount() { 
		return mMsgList.size(); 
	} 

	public Object getItem(int position) { 
		return position; 
	} 

	public long getItemId(int position) { 
		return position; 
	} 

	public synchronized void add (String room, String sender, String contents) {
		MessageIdentifier mi = new MessageIdentifier();
		mi.room      = room;
		mi.sender    = sender;
		mi.contents  = contents;
		mi.timestamp = Utilities.getTime();
		add(mi);
	}

	public synchronized void add (final MessageIdentifier mi) {
		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mMsgList.add(0, mi); // insert at head of list
				notifyDataSetChanged(); // force re-display of list
			}
		});
		Log.v(TAG, "add("+mi.room+", "+mi.sender+", "+mi.contents+")");
	}

	public synchronized MessageIdentifier get (int pos) {
		if (pos<mMsgList.size()){
			return mMsgList.get(pos);
		} else {
			return new MessageIdentifier();
		}
	}


	public synchronized void clear(){
		mMsgList.clear();
		notifyDataSetChanged(); // force re-display of list
	}
	
	// return the View to be displayed
	public View getView(int position, View convertView, ViewGroup parent) { 
		// Inflate a view template
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mActivity.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(Utility.getResourseIdByName(mActivity.getPackageName(),"layout","chatitem"), parent, false);
		}

		// get the display views
		ImageView vThumbnail = (ImageView)convertView.findViewById(Utility.getResourseIdByName(mActivity.getPackageName(),"id","thumbnail"));
		TextView  vTimestamp = (TextView) convertView.findViewById(Utility.getResourseIdByName(mActivity.getPackageName(),"id","timestamp"));
		TextView  vSender    = (TextView) convertView.findViewById(Utility.getResourseIdByName(mActivity.getPackageName(),"id","sender"));
		TextView  vContents  = (TextView) convertView.findViewById(Utility.getResourseIdByName(mActivity.getPackageName(),"id","contents"));

		String room = get(position).room;
		String name = get(position).sender;
		String contents = get(position).contents;

		Log.v(TAG, "Display chat ("+room+","+name+","+contents+")");
		
		// retrieve the profile from cache
		if (!ProfileCache.isPresent(ProfileCache.getProfilePath(name))){
			Log.e(TAG, "Profile not found for: "+name);
		}
		mProfile = ProfileCache.getProfile(name);
		if(mProfile == null) {
			String ts = Utilities.getTimestamp(get(position).timestamp, "hh:mm");
			vSender.setText("ERROR");
			vTimestamp.setText(ts);
			vContents.setText(get(position).contents);
			vThumbnail.setImageDrawable(mDefaultThumb);
			return convertView;
		}
		String fullname = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
		if (fullname.length()==0) fullname="("+name+")";
		if (room.length()>0) fullname += "("+room+")";
		
		String ts = Utilities.getTimestamp(get(position).timestamp, "hh:mm");
		
		vTimestamp.setText(ts);
		vSender.setText(fullname);
		vContents.setText(get(position).contents);

		// Process the photo
		try {
			byte[] bphoto ;
			bphoto = mProfile.getPhoto();
			if ((bphoto!=null) && (bphoto.length>0)){
				Bitmap image = BitmapFactory.decodeByteArray(bphoto, 0, bphoto.length);
				vThumbnail.setImageBitmap(image);
			} else {
				vThumbnail.setImageDrawable(mDefaultThumb);			
			}
		} catch (Exception e){
			vThumbnail.setImageDrawable(mDefaultThumb);			
		}

		return convertView;
	} 


} // ChatMessageAdapter
