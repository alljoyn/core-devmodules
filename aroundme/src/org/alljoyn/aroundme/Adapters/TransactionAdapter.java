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
// Holds list of transactions. 
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
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaUtilities;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.TransactionListDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.MediaCache;
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


public class TransactionAdapter extends BaseAdapter { 

	private static final String TAG = "TransactionAdapter";


	// The internal data
	//private  TransactionListDescriptor mTransactionList = null ; 
	private HashMap<String,MediaIdentifier>  mTransactionList = null;
	private ArrayList<String>  mIndex = null;

	// Context of the current Activity
	private Context mContext; 

	// Default icon
	private int mDefaultIcon = R.drawable.ic_dialog_files;

	// Constructor
	public TransactionAdapter(Context c) { 
		mContext = c; 
		mTransactionList = new HashMap<String,MediaIdentifier>();
		mIndex = new ArrayList<String>();
		mTransactionList.clear();
		mIndex.clear();
	} 


	// Set the underlying transaction list
	public void setTransactionType (String trantype){
		if (mTransactionList == null)
			mTransactionList = new HashMap<String,MediaIdentifier>();

		if (mIndex == null)
			mIndex = new ArrayList<String>();

		mTransactionList.clear();
		mIndex.clear();

		String[] tlist = new String[0];
		tlist = MediaTransactionCache.list(trantype);

		if (tlist!=null){
			for (int i=0; i<tlist.length; i++){
				MediaIdentifier mi = new MediaIdentifier();
				mi = MediaTransactionCache.retrieveMediaTransaction(trantype, tlist[i]);
				add(mi);
				//mTransactionList.put(mi.localpath, mi);
				Log.v(TAG, "Add: "+tlist[i]+"("+mi.localpath+")");
			}
		}
	}


	// Set the underlying transaction list
	public void setTransactionList (TransactionListDescriptor tlist){
		mTransactionList.clear();
		for (int i=0; i<tlist.size(); i++){
			MediaIdentifier mi = new MediaIdentifier();
			mi = tlist.get(i);
			add(mi);
			//mTransactionList.put(mi.localpath, mi);
			Log.v(TAG, "Add: "+mi.localpath);
		}
	}


	// Get the underlying transaction list
	public TransactionListDescriptor getTransactionList (){
		TransactionListDescriptor tlist = new TransactionListDescriptor();
		for (int i=0; i<mTransactionList.size(); i++){
			MediaIdentifier mi = new MediaIdentifier();
			mi = get(i);
			tlist.add(mi);
		}
		return tlist;
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
		if (mIndex!=null){
			return mIndex.size(); 
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


	public synchronized void add (MediaIdentifier mi) {
		if (mTransactionList!=null){
			mIndex.add(mi.localpath);
			mTransactionList.put(mi.localpath, mi);
			notifyDataSetChanged(); // force re-display of list
		} else {
			Log.e(TAG, "add(): null list");
		}
	}


	public synchronized void remove (int pos) {
		if ((mTransactionList!=null) &&(pos<mTransactionList.size())){
			String p = mIndex.get(pos);
			mTransactionList.remove(p);
			mIndex.remove(pos);
			notifyDataSetChanged(); 
		}
	}


	public synchronized void remove (String path) {
		if (mTransactionList!=null){
			if (mTransactionList.containsKey(path)){
				int pos = mIndex.indexOf(path);
				remove(pos);
			} else {
				Log.e(TAG, "remove() - Unknown item: "+path);
			}
		}
	}


	public synchronized MediaIdentifier get (int pos) {
		MediaIdentifier mi = new MediaIdentifier();
		try{
			if ((mTransactionList!=null) &&(pos<mTransactionList.size())){
				String p = mIndex.get(pos);
				mi = mTransactionList.get(p);
			} 
		} catch (Exception e){
			Log.e(TAG, "getTitle() Error: "+e.toString());
		}
		return mi;
	}


	public synchronized MediaIdentifier get (String path) {
		MediaIdentifier mi = new MediaIdentifier();
		try{
			if ((mTransactionList!=null) && (mTransactionList.containsKey(path))){
				mi = mTransactionList.get(path);
			} else {
				Log.e(TAG,"get("+path+"): unknown");
			}
		} catch (Exception e){
			Log.e(TAG, "getTitle() Error: "+e.toString());
		}
		return mi;
	}



	// accessors for underlying data

	public synchronized String getTitle(int position){
		String s = "";
		try{
			String p = mIndex.get(position);
			s = mTransactionList.get(p).title;
			if ((s==null) || (s.length()<=0)){
				String path = mTransactionList.get(p).localpath;
				if (path.contains("/")){
					s = path.substring(path.lastIndexOf("/")+1);
				} else {
					s = path;
				}
			}
		} catch (Exception e){
			Log.e(TAG, "getTitle() Error: "+e.toString());
		}
		return s;
	}

	public synchronized String getPath(int position){
		String s = "";
		try{
			String p = mIndex.get(position);
			s = mTransactionList.get(p).localpath;
			if (s==null) s = "";
		} catch (Exception e){
			Log.e(TAG, "getPath() Error: "+e.toString());
		}
		return s;
	}

	public synchronized String getThumbPath(int position){
		String s = "";
		try{
			String p = mIndex.get(position);
			s = mTransactionList.get(p).thumbpath;
			if (s==null) s = "";
		} catch (Exception e){
			Log.e(TAG, "getThumbPath() Error: "+e.toString());
		}
		return s;
	}

	public synchronized String getMediaType(int position){
		String s = "";
		try{
			String p = mIndex.get(position);
			s = mTransactionList.get(p).mediatype;
			if (s==null) s = "";
		} catch (Exception e){
			Log.e(TAG, "getMediaType() Error: "+e.toString());
		}
		return s;
	}

	public synchronized String getTimestamp(int position){
		long time = 0;
		try{
			String p = mIndex.get(position);
			time = mTransactionList.get(p).timestamp;
		} catch (Exception e){
			Log.e(TAG, "getTimestamp() Error: "+e.toString());
		}
		return Utilities.getTimestamp(time, "yyyy-MMM-dd hh:mm");
	}

	// returns the human readable form of the userid (sender or destination)
	public synchronized String getUserid(int position){
		String userid = "";
		try {
			String p = mIndex.get(position);
			userid = mTransactionList.get(p).userid;

		}catch (Exception e){
			Log.e(TAG, "Error getting UserID: "+e.toString());
		}
		return userid;
	}

	// returns the human readable form of the userid (sender or destination)
	public synchronized String getUsername(int position){
		String name = "";
		String userid = "";
		try {
			String p = mIndex.get(position);
			userid = mTransactionList.get(p).userid;
			// look up profile from cache
			ProfileDescriptor prof = new ProfileDescriptor();
			prof = ProfileCache.getProfile (userid);
			if (prof!=null){
				name =  prof.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
			} else {
				name = userid;
			}
		}catch (Exception e){
			Log.e(TAG, "Error getting UserID: "+e.toString());
			name = userid;
		}
		return name;
	}


	public synchronized byte[] getThumbnail(int position){
		byte[] photo = new byte[0];
		String photopath = getPath(position);

		try {
			String thumbpath = MediaUtilities.getThumbnailPath(photopath);
			photo = ThumbnailCache.getThumbnail(thumbpath);
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
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.transitem, parent, false);
		}

		// get the display views
		ImageView      imageIcon = (ImageView) convertView.findViewById(R.id.imageIcon);
		TextView       line1View = (TextView)  convertView.findViewById(R.id.line1);
		TextView       line2View = (TextView)  convertView.findViewById(R.id.line2);
		TextView       line3View = (TextView)  convertView.findViewById(R.id.line3);

		// Populate fields
		String filename = getTitle(position);
		String userid = getUserid(position);

		// Remove userid prefix, if present
		if (filename.contains("_")) 
			filename = filename.substring(filename.lastIndexOf("_")+1);
		line1View.setText(filename);
		line2View.setText(getUsername(position));
		line3View.setText(getTimestamp(position));

		// Set up the File image thumbnail
		Bitmap image=null;
		boolean imageset = false;
		try {

			//String thumbpath = MediaUtilities.getThumbnailPath(getPath(position));
			// Note: thumbnails are already compressed, so just load
			String thumbpath = getThumbPath(position);
			if ((thumbpath!=null) && (thumbpath.length()>0)){
				// double-check that thumbnail is in the cache, if not then create it from the file
				String tpath = ThumbnailCache.getThumbnailPath(userid,thumbpath);
				if (!ThumbnailCache.isPresent(tpath)){
					byte[] thumb = MediaUtilities.getThumbnailFromFile(mContext, 
							getMediaType(position), 
							getPath(position));
					if ((thumb!=null) && (thumb.length>0)){
						ThumbnailCache.saveThumbnail(userid, tpath, thumb);
					}
				}
				imageset = true;
				image = BitmapFactory.decodeFile(tpath,new BitmapFactory.Options());
				if (image==null) imageset=false;
				//imageIcon.setImageBitmap(image);
			} 

		} catch (Exception e){
			// error somewhere, just set to default icon
			imageset = false;
			Log.e(TAG, "Error creating thumbnail: "+e.toString());
		}
		if (imageset){
			imageIcon.setImageBitmap(image);		
		} else {
			imageIcon.setImageDrawable(mContext.getResources().getDrawable(mDefaultIcon));					
		}


		// Set up listener for image thumbnail

		final int loc = position;

		// Thumbnail
		imageIcon.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				String filepath = getPath(loc);

				try {
					// if no filepath, try to reconstruct
					if ((filepath==null) || (filepath.length()<=0)){
						Log.e(TAG, "Null filepath for index: "+loc);
						String path = MediaCache.getMediaPath(mTransactionList.get(loc).userid, 
								mTransactionList.get(loc).mediatype, 
								mTransactionList.get(loc).name) ;
						filepath = path;
					}

					Intent intent = new Intent(Intent.ACTION_VIEW);

					// figure out the MIME type from the file extension
					String ext = filepath.substring(filepath.lastIndexOf(".")+1);
					String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);		
					intent.setDataAndType(Uri.fromFile(new File(filepath)), mimetype);

					// Start the viewer for this file and MIME type
					mContext.startActivity(intent);
				} catch (Exception e){
					Log.e(TAG, "Error viewing media ("+filepath+"): "+e.toString());
					Toast.makeText(mContext, "Oops, error viewing media", Toast.LENGTH_SHORT);
				}
			}
		});

		return convertView;
	} 


} // TransactionAdapter
