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
import org.alljoyn.aroundme.R.drawable;
import org.alljoyn.aroundme.R.id;
import org.alljoyn.aroundme.R.layout;
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


public class ApprovalAdapter extends BaseAdapter { 

	private static final String TAG = "ApprovalAdapter";


	// Items in internal list
	private class ListItem {
		boolean         isChecked;
		MediaIdentifier mi;

		ListItem(){
			isChecked = false;
			mi = new MediaIdentifier();
		}
	}

	// The internal data
	//private  TransactionListDescriptor mTransactionList = null ; 
	private ArrayList<ListItem>         mTransactionList = null;
	private ArrayList<ListItem>         mCopyList = null;

	// Context of the current Activity
	private Context mContext; 

	// Default icon
	private int mDefaultIcon = R.drawable.ic_dialog_files;

	// Constructor
	public ApprovalAdapter(Context c) { 
		mContext = c; 
		//mTransactionList = new TransactionListDescriptor();
		mTransactionList = new ArrayList<ListItem>();
		mTransactionList.clear();
	} 


	// Set the underlying transaction list
	public void setTransactionType (String trantype){
		if (mTransactionList == null)
			mTransactionList = new ArrayList<ListItem>();
		mTransactionList.clear();

		String[] tlist = new String[0];
		tlist = MediaTransactionCache.list(trantype);

		if (tlist!=null){
			for (int i=0; i<tlist.length; i++){
				ListItem li = new ListItem();
				li.isChecked = false;
				li.mi = MediaTransactionCache.retrieveMediaTransaction(trantype, tlist[i]);
				mTransactionList.add(li);
				Log.v(TAG, "Add: "+tlist[i]+"("+li.mi.localpath+")");
			}
			notifyDataSetChanged(); // force re-display of list
		}
	}


	// Set the underlying transaction list
	public void setTransactionList (TransactionListDescriptor tlist){
		mTransactionList.clear();
		for (int i=0; i<tlist.size(); i++){
			ListItem li = new ListItem();
			li.isChecked = false;
			li.mi = tlist.get(i);
			mTransactionList.add(li);
			Log.v(TAG, "Add: "+li.mi.localpath);
		}
		notifyDataSetChanged();
	}


	// Get the underlying transaction list
	public TransactionListDescriptor getTransactionList (){
		TransactionListDescriptor tlist = new TransactionListDescriptor();
		for (int i=0; i<mTransactionList.size(); i++){
			MediaIdentifier mi = new MediaIdentifier();
			mi = mTransactionList.get(i).mi;
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
		if (mTransactionList!=null){
			return mTransactionList.size(); 
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
			ListItem li = new ListItem();
			li.isChecked = false;
			li.mi = new MediaIdentifier();
			li.mi = mi;
			mTransactionList.add(li);
			notifyDataSetChanged(); // force re-display of list
		}
	}

	public synchronized void remove (int pos) {
		if ((mTransactionList!=null) &&(pos<mTransactionList.size())){
			mTransactionList.remove(pos);
			notifyDataSetChanged(); 
		}
	}

	public synchronized MediaIdentifier get (int pos) {
		if ((mTransactionList!=null) &&(pos<mTransactionList.size())){
			return mTransactionList.get(pos).mi;
		} else {
			return new MediaIdentifier();
		}
	}


	// accessors for underlying data

	public synchronized String getTitle(int position){
		String s = mTransactionList.get(position).mi.title;
		if ((s==null) || (s.length()<=0)){
			String path = mTransactionList.get(position).mi.localpath;
			if (path.contains("/")){
				s = path.substring(path.lastIndexOf("/")+1);
			} else {
				s = path;
			}
		}
		return s;
	}

	public synchronized String getPath(int position){
		String s = mTransactionList.get(position).mi.localpath;
		if (s==null) s = "(?)";
		return s;
	}

	public synchronized String getThumbPath(int position){
		String s = mTransactionList.get(position).mi.thumbpath;
		if (s==null) s = "(?)";
		return s;
	}

	public synchronized String getMediaType(int position){
		String s = mTransactionList.get(position).mi.mediatype;
		if (s==null) s = "(?)";
		return s;
	}

	public synchronized String getTimestamp(int position){
		long time = mTransactionList.get(position).mi.timestamp;
		return Utilities.getTimestamp(time, "yyyy-MMM-dd hh:mm");
	}

	// returns the human readable form of the userid (sender or destination)
	public synchronized String getUserid(int position){
		String name = "";
		try {
			String userid = mTransactionList.get(position).mi.userid;
			// look up profile from cache
			ProfileDescriptor prof = new ProfileDescriptor();
			prof = ProfileCache.getProfile (userid);
			name =  prof.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
		}catch (Exception e){
			Log.e(TAG, "Error getting UserID: "+e.toString());
		}
		return name;
	}


	public void removeCheckedItems(){
		// Copy unchecked items into new array and replace
		mCopyList = new ArrayList<ListItem>();
		mCopyList.clear();
		for (int i=0; i<mTransactionList.size();i++){
			if (!isChecked(i)){
				mCopyList.add(mTransactionList.get(i));
			} else {
				Log.v(TAG, "Dropping: "+mTransactionList.get(i).mi.title);
			}
		}
		mTransactionList.clear();
		mTransactionList = mCopyList;
		mCopyList = null;
		notifyDataSetChanged(); // force re-display of list
	}

	public synchronized boolean isChecked (int position){
		return mTransactionList.get(position).isChecked;
	}


	public synchronized void check (int position){
		mTransactionList.get(position).isChecked = true;
	}


	public synchronized void toggleSelection (int position){
		mTransactionList.get(position).isChecked = !mTransactionList.get(position).isChecked;
	}


	public synchronized void toggleAll (){
		for (int i=0; i<mTransactionList.size(); i++){
			toggleSelection(i);
		}
		notifyDataSetChanged(); // force re-display of list
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
		//TEMPHACK: re-using contact layout
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
		String filename = getTitle(position);
		if (filename.contains("_")) 
			filename = filename.substring(filename.lastIndexOf("_")+1);
		line1View.setText(filename);
		line2View.setText(getUserid(position));
		line3View.setText(getTimestamp(position));

		checkView.setChecked(isChecked(position));

		// Set up the File image thumbnail
		try {

			//String thumbpath = MediaUtilities.getThumbnailPath(getPath(position));
			// Note: thumbnails are already compressed, so just load
			String thumbpath = getThumbPath(position);
			Bitmap image = BitmapFactory.decodeFile(thumbpath,new BitmapFactory.Options());
			imageIcon.setImageBitmap(image);

		} catch (Exception e){
			// error somewhere, just set to default icon
			imageIcon.setImageDrawable(mContext.getResources().getDrawable(mDefaultIcon));		
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


		// Overall line
		Button itemTouched = (Button) convertView.findViewById(R.id.listItemButton);
		itemTouched.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				toggleSelection(loc);
				checkView.setChecked(isChecked(loc));
			}
		});


		// Thumbnail
		imageIcon.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				String filepath = getPath(loc);
				Intent intent = new Intent(Intent.ACTION_VIEW);

				// figure out the MIME type from the file extension
				String ext = filepath.substring(filepath.lastIndexOf(".")+1);
				String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);		
				intent.setDataAndType(Uri.fromFile(new File(filepath)), mimetype);

				// Start the viewer for this file and MIME type
				mContext.startActivity(intent);

			}
		});

		return convertView;
	} 


} // ApprovalAdapter
