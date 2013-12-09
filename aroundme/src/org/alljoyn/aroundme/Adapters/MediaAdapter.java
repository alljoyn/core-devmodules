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
// Holds list of media (photos, videos etc.)
// Note: unlike most other adapters, there can be multiple instances of this type, so no static arrays etc.
//       This means that access to the "correct" list has to be managed externally

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import org.alljoyn.aroundme.R;
import org.alljoyn.devmodules.common.MediaIdentifier;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class MediaAdapter extends BaseAdapter { 

	private static final String TAG = "MediaAdapter";

	// Arrays for holding the data
	private  ArrayList<MediaIdentifier> mMediaList = new ArrayList<MediaIdentifier>(); 
	private  ArrayList<String> mNameList = new ArrayList<String>(); 

	// Context of the current Activity
	private Context mContext; 

	// Default icon
	private int mDefaultIcon = R.drawable.ic_list_person;


	// Constructor
	public MediaAdapter(Context c) { 
		mContext = c; 
		if (mMediaList==null) mMediaList = new ArrayList<MediaIdentifier>(); 
		mMediaList.clear();
		mNameList.clear();
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
		return mMediaList.size(); 
	} 

	public Object getItem(int position) { 
		return position; 
	} 

	public long getItemId(int position) { 
		return position; 
	} 

	public synchronized void add (MediaIdentifier mi) {
		if (!mNameList.contains(mi.path)){
			mMediaList.add(mi);
			mNameList.add(mi.path);
			notifyDataSetChanged(); // force re-display of list
		}
	}

	public synchronized MediaIdentifier get (int pos) {
		if (pos<mMediaList.size()){
			return mMediaList.get(pos);
		} else {
			return new MediaIdentifier();
		}
	}


	public synchronized void clear(){
		mMediaList.clear();
		mNameList.clear();
		notifyDataSetChanged(); // force re-display of list
	}

	// accessors for underlying data

	public synchronized String getName(int position){
		return mMediaList.get(position).name;
	}

	public synchronized String getTitle(int position){
		return mMediaList.get(position).title;
	}

	public synchronized String getPath(int position){
		return mMediaList.get(position).path;
	}

	public synchronized String getThumbnailPath(int position){
		return mMediaList.get(position).thumbpath;
	}


	public synchronized byte[] getThumbnail(int position){
		byte[] photo = new byte[0];
		String photopath = mMediaList.get(position).thumbpath;

		try {
			if ((photopath!=null) && (photopath.length()>0)){
				// load from file
				Log.d(TAG, "Loading photo from: "+photopath);
				Bitmap image = BitmapFactory.decodeFile(photopath);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();  
				image.compress(Bitmap.CompressFormat.JPEG, 100, baos);   
				photo = baos.toByteArray();

				// force recycling of bitmap and stream (they can cause out of memory errors)
				image.recycle();
				baos = null;
			}
		} catch (Exception e){
			Log.e(TAG, "getPhoto: Error decoding file ("+photopath+")"+e.toString());
			photo = new byte[0] ; 
		}
		return photo;
	}



	// return the View to be displayed
	public View getView(int position, View convertView, ViewGroup parent) { 

		TextView tv ;


		// Inflate a view template
		//TEMPHACK: re-using contact layout
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
			//convertView = inflater.inflate(R.layout.contactitem, parent, false);
			convertView = inflater.inflate(R.layout.griditem, parent, false);
		}

		// get the display views
		TextView  nameView   = (TextView)  convertView.findViewById(R.id.grid_item_text);
		//TextView  numberView = (TextView)  convertView.findViewById(R.id.contactNumber);
		ImageView photoIcon  = (ImageView) convertView.findViewById(R.id.grid_item_image);

		// Populate template
		String name = getTitle(position);
		nameView.setText(name);

		//numberView.setText(getPath(position));

		try {
			BitmapDrawable icon = null;
			ByteArrayInputStream rawIcon = null;

			String ppath = getThumbnailPath(position);
			if ((ppath!=null) && (ppath.length()>0)){	
				//Log.v(TAG, "Loading icon: "+ppath);
				Bitmap image = BitmapFactory.decodeFile(ppath);
				icon = new BitmapDrawable (image);
				photoIcon.setImageDrawable(icon);
			} else {
				photoIcon.setImageDrawable(mContext.getResources().getDrawable(mDefaultIcon));		
			}

		} catch (Exception e){
			// error somewhere, just set to default icon
			photoIcon.setImageDrawable(mContext.getResources().getDrawable(mDefaultIcon));		
		}

		return convertView;
	} 


} // MediaAdapter
