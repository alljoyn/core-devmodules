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
package org.alljoyn.aroundme.RemoteMedia;


import java.io.ByteArrayInputStream;
import java.io.File;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.MediaAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.devmodules.api.mediaquery.MediaQueryAPI;
import org.alljoyn.devmodules.api.mediaquery.MediaQueryListener;
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaTypes;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ProfileCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;


/** Displays a grid of media items available on a remote device
 * The profile ID (Unique name) and media type are passed as arguments
 * 
 * @author pprice
 *
 */

public class RemoteMediaQueryFragment extends Fragment {



	private static final String TAG = "RemoteMediaQueryFragment";

	private MediaAdapter        mAdapter; 
	private ProfileDescriptor   mProfile ;
	private String              mProfileId="";
	private String              mName;
	private boolean             mReady = false;

	private int                 mTransaction=-1;
	private String              mTitle="";
	private GridView            mGridview;
	private Context             mContext;

	private String              mMediaType = "";
	private String              mLocalFunction="";
	private View                mDisplayView = null;

	// Default icon
	private static final int    mDefaultIcon = R.drawable.ic_dialog_files;
	private  int                mMediaIcon = R.drawable.ic_dialog_files;


	// Thread for handling asynchronous stuff
	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private UIhandler mHandler = new UIhandler(handlerThread.getLooper()); 

	/* Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity();

		// Process the arguments
		Bundle args = getArguments();


		if (args!=null){
			Log.v(TAG, "Args: "+args.toString());

			// handle the ProfileID (unique id) name and look up associated data
			if (args.containsKey(AppConstants.PROFILEID)) {
				mProfileId = args.getString(AppConstants.PROFILEID);

				// OK, retrieve the profile info for the named user
				mProfile = new ProfileDescriptor();

				// Look up the profile from cache
				if ((mProfileId!=null)&&(mProfileId.length()>0)){
					if (ProfileCache.isPresent(mProfileId)){
						mProfile = ProfileCache.getProfile(mProfileId);
						mName = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
					} else {
						Log.e(TAG, mProfileId+": no profile available");
					}
				}
			} else {
				Log.e(TAG, "No user specified!!!");
				mReady = false;
				getActivity().getSupportFragmentManager().popBackStack();
			}

			// get the media type and set up appropriately
			if (args.containsKey(AppConstants.MEDIATYPE)) {
				mMediaType = args.getString(AppConstants.MEDIATYPE);
				setupMediaType(mMediaType);
				mReady = true;
			} else {
				Log.e(TAG, "No media type specified!!! Quitting fragment...");
				Log.w(TAG, "args: "+args.toString());
				mReady = false;
				getActivity().getSupportFragmentManager().popBackStack();
			}

		} else {
			Log.e(TAG, "No args provided! Quitting...");
			mReady = false;
			getActivity().getSupportFragmentManager().popBackStack();
		}
	}



	// Called when Fragment is attached to a View on the display
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (mReady){
			// Layout the overall screen
			mDisplayView = inflater.inflate(R.layout.grid, container, false);

			Log.d(TAG, "Loading remote media for service: "+mProfileId + "(" + mName + ", "+ mMediaType +")");


			// Set up list adapter for list display
			mAdapter = new MediaAdapter(mContext);
			mAdapter.setContext(mContext); 
			mAdapter.setDefaultIcon(mMediaIcon);

			mGridview = (GridView) mDisplayView.findViewById(R.id.gridview);
			mGridview.setAdapter(mAdapter);
			mGridview.setOnItemClickListener(mClickListener);

			// Start the UI Handler
			mHandler.init();


			// Register listener for MediaQuery callbacks
			MediaQueryAPI.RegisterListener(new MediaQueryListener() {
				@Override
				public void onMediaQueryServiceAvailable(String service) {
					mHandler.init();
				}

				@Override
				public void onMediaQueryServiceLost(String service) {
					mHandler.stop();
				}

				@Override
				public void onQueryComplete(String service, String mtype) {

					// just update the header
					mHandler.updateHeader();

				}

				@Override
				public void onItemAvailable(String service, MediaIdentifier item) {
					//Log.i(TAG,"ITEM AVAILABLE!!!!");
					mHandler.itemAvailable(mTransaction, service, item);
				}

				@Override
				public void onTransferComplete(String service, String path, String mtype, String localpath) {
					mHandler.fileAvailable(service, mtype, localpath);
					Log.d(TAG, "Transfer Complete: "+service+", "+path+", "+mtype);
				}

				@Override
				public void onTransferError(int transaction, String service, String mtype, String path) {
					Log.d(TAG, "Transfer Error: "+service+", "+path+", "+mtype);
				}

			});


			// nothing else to do, the UIHandler and ListAdapter handle update of display etc.

		} else {
			Log.e(TAG, "onCreateView() Not ready...");
		}
		return mDisplayView;
	} //onCreateView


	/* Called when the activity is exited. */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}


	/**
	 * Utility function to set up internal variables based on the media type
	 */
	private void setupMediaType(String mediatype){
		if (mediatype.equals(MediaTypes.PHOTO)){
			mMediaIcon = R.drawable.ic_list_photos;
			mLocalFunction = "Copy & View";
		} else if (mediatype.equals(MediaTypes.MUSIC)){
			mMediaIcon = R.drawable.ic_list_music;
			mLocalFunction = "Stream";
		} else if (mediatype.equals(MediaTypes.VIDEO)){
			mMediaIcon = R.drawable.ic_list_movies;
			mLocalFunction = "Stream";
		} else if (mediatype.equals(MediaTypes.APP)){
			mMediaIcon = R.drawable.ic_list_applications;
			mLocalFunction = "Copy & Run";
		} else {
			Log.e (TAG, "Unknown Media Type: "+mediatype);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////
	// Click Listener(s)
	/////////////////////////////////////////////////////////////////////////////////


	private OnItemClickListener mClickListener = new OnItemClickListener(){
		public void onItemClick(AdapterView parent, View v, int position, long id) 
		{   
			//get the item details and display
			MediaIdentifier mi = new MediaIdentifier();
			mi = mAdapter.get(position);
			displayMediaItemOptions(mi);
		}//onItemClick
	};



	// launch the built-in contact picker activity
	void displayMediaItemOptions(MediaIdentifier mi) {

		// popup a quick set of instructions

		String text = "\n\n"+
				"Title:     "+mi.title+"\n" +
				"File:      "+mi.name+"\n"+
				"Size:      "+mi.size+"\n"+
				"\n\n";


		BitmapDrawable icon = null;
		Drawable       picture = null;
		ByteArrayInputStream rawIcon = null;
		try {

			String ppath = mi.thumbpath;
			if ((ppath!=null) && (ppath.length()>0)){	
				//Log.v(TAG, "Loading icon: "+ppath);
				Bitmap image = BitmapFactory.decodeFile(ppath);
				icon = new BitmapDrawable (image);
				picture = icon;		
			} else {
				picture = mContext.getResources().getDrawable(mMediaIcon);		
			}

		} catch (Exception e){
			// error somewhere, just set to default icon
			picture = mContext.getResources().getDrawable(mMediaIcon);		
		}


		final String mtype = mMediaType;
		final String path  = mi.path;
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(text)
		.setTitle("Options for "+mi.name)
		.setCancelable(true)
		.setIcon(picture)
		.setPositiveButton(mLocalFunction, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if ((mMediaType.equals(MediaTypes.PHOTO)||(mMediaType.equals(MediaTypes.APP)))){
					mHandler.requestFile(mProfileId, mMediaType, path);
				} else if ((mMediaType.equals(MediaTypes.MUSIC))||((mMediaType.equals(MediaTypes.VIDEO)))){
					mHandler.requestStream(mProfileId, mMediaType, path);
				} else {
					Log.e (TAG, "Unknown Media Type: "+mMediaType);
				}

			} //onClick
		})
		.setNeutralButton("Activate Remotely", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				mHandler.requestActivation(mProfileId, mtype, path);
			} //onClick
		})
		.setNegativeButton("Cancel\n", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				mHandler.showMessage("Cancel");
			} //onClick
		});

		AlertDialog alert = builder.create();
		alert.requestWindowFeature(Window.FEATURE_LEFT_ICON);

		alert.show();

	}//displayMediaItem

	/////////////////////////////////////////////////////////////////////////////////
	// UI Handler Message Queue Thread
	// Initiate all UI-related functions through accessor methods for this
	/////////////////////////////////////////////////////////////////////////////////

	private class UIhandler extends Handler{

		public UIhandler(Looper loop) {
			super(loop);
		}


		// List of UI commands
		private static final int UI_INIT                 =  1;  // Initialise
		private static final int UI_STOP                 =  2;  // stop processing and quit
		private static final int UI_ERROR                =  3;  // error popup
		private static final int UI_MESSAGE              =  4;  // message popup
		private static final int UI_SERVICE_CONNECTED    =  5;  // remote service connected
		private static final int UI_SERVICE_DISCONNECTED =  6;  // remote service disconnected
		private static final int UI_QUERY_COMPLETE       =  7;  // handle notification that query has finished
		private static final int UI_ITEM_AVAILABLE       =  8;  // handle notification that media is available
		private static final int UI_ADD_MEDIA            =  9;  // add media to list
		private static final int UI_UPDATE_HEADER        = 10;  // update header text
		private static final int UI_REQUEST_FILE         = 11;  // request copy of remote file
		private static final int UI_FILE_AVAILABLE       = 12;  // notification that file is available
		private static final int UI_REQUEST_STREAM       = 13;  // request stream of remote file
		private static final int UI_STREAM_AVAILABLE     = 14;  // notification that stream is available
		private static final int UI_REQUEST_ACTIVATION   = 15;  // request activation of file on remote device


		// Accessor Methods

		public void init() {
			sendEmptyMessage(UI_INIT);
		}

		public void stop() {
			sendEmptyMessage(UI_STOP);
		}

		public void showError(String error){
			Message msg = obtainMessage(UI_ERROR);
			msg.obj = error ;
			sendMessage(msg);	
		}

		public void showMessage(String text){
			Message msg = obtainMessage(UI_MESSAGE);
			msg.obj = text ;
			sendMessage(msg);	
		}

		public void queryComplete(int transaction, String service, String mtype){
			Message msg = obtainMessage(UI_QUERY_COMPLETE);
			Bundle data = new Bundle();
			data.putInt("transaction", transaction);
			data.putString("service", service);
			data.putString("mtype", mtype);
			msg.setData(data);
			sendMessageDelayed(msg, 500);	
		}

		public void itemAvailable(int transaction, String service, MediaIdentifier item){
			Message msg = obtainMessage(UI_ITEM_AVAILABLE);
			Bundle data = new Bundle();
			data.putInt("transaction", transaction);
			data.putString("service", service);
			msg.setData(data);
			msg.obj = item;
			sendMessage(msg);	
		}


		public void addItem(MediaIdentifier mi){
			Message msg = obtainMessage(UI_ADD_MEDIA);
			msg.obj = mi ;
			sendMessage(msg);	
		}


		public void updateHeader(){
			sendEmptyMessageDelayed(UI_UPDATE_HEADER, 500);
		}

		public void requestFile (String service, String mtype, String path){
			Message msg = obtainMessage(UI_REQUEST_FILE);
			Bundle data = new Bundle();
			data.putString("service", service);
			data.putString("mtype", mtype);
			data.putString("path", path);
			msg.setData(data);
			sendMessage(msg);			
		}

		public void fileAvailable (String service, String mtype, String path){
			Message msg = obtainMessage(UI_FILE_AVAILABLE);
			Bundle data = new Bundle();
			data.putString("service", service);
			data.putString("mtype", mtype);
			data.putString("path", path);
			msg.setData(data);
			sendMessage(msg);			
		}

		public void requestStream (String service, String mtype, String path){
			Message msg = obtainMessage(UI_REQUEST_STREAM);
			Bundle data = new Bundle();
			data.putString("service", service);
			data.putString("mtype", mtype);
			data.putString("path", path);
			msg.setData(data);
			sendMessage(msg);			
		}

		//TODO: Need extra parameters, not sure what
		public void streamAvailable (String service, String mtype, String path){
			Message msg = obtainMessage(UI_STREAM_AVAILABLE);
			Bundle data = new Bundle();
			data.putString("service", service);
			data.putString("mtype", mtype);
			data.putString("path", path);
			msg.setData(data);
			sendMessage(msg);			
		}

		public void requestActivation (String service, String mtype, String path){
			Message msg = obtainMessage(UI_REQUEST_ACTIVATION);
			Bundle data = new Bundle();
			data.putString("service", service);
			data.putString("mtype", mtype);
			data.putString("path", path);
			msg.setData(data);
			sendMessage(msg);			
		}


		@Override
		public void handleMessage(Message msg) {

			String profileId ;
			ProfileDescriptor pdesc;

			switch (msg.what) {

			case UI_INIT: {
				//
				//					if (mMediaQueryClient==null){
				//						Log.e(TAG, "MediaQueryClient not set up");
				//					} else
				{

					// OK, we are connected to the Media Client, so connect to the supplied service name

					try {
						Log.d(TAG, "Getting Media ("+mMediaType+") for: "+mProfileId);
						MediaQueryAPI.CollectMediaList(mProfileId, mMediaType);
					} catch (Exception e) {
						showError(TAG+" Error getting Media list: "+ e.toString());
					}
				}
				break;
			}

			case UI_QUERY_COMPLETE: {
				processQueryResult(msg.getData().getInt("transaction"),
						msg.getData().getString("service"), 
						msg.getData().getString("mtype"));
				break;
			}

			case UI_ITEM_AVAILABLE: {
				//Log.i(TAG, "UI ITEM AVAILABLE!!!!");
				processItemAvailable(msg.getData().getInt("transaction"), 
						msg.getData().getString("service"), 
						(MediaIdentifier) msg.obj);
				break;
			}

			case UI_ADD_MEDIA: {
				final MediaIdentifier mi = (MediaIdentifier)msg.obj;
				// add to the displayed list
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mAdapter.add(mi);
						mAdapter.notifyDataSetChanged();
					}
				});
				break;
			}
			case UI_UPDATE_HEADER: {
				doUpdateHeader();
				break;
			}

			case UI_REQUEST_FILE: {
				doRequestFile(msg.getData().getString("service"),
						msg.getData().getString("mtype"), 
						msg.getData().getString("path"));
				break;
			}

			case UI_FILE_AVAILABLE: {
				doFileAvailable(msg.getData().getString("service"),
						msg.getData().getString("mtype"), 
						msg.getData().getString("path"));
				break;
			}

			case UI_REQUEST_STREAM: {
				doRequestStream(msg.getData().getString("service"),
						msg.getData().getString("mtype"), 
						msg.getData().getString("path"));
				break;
			}

			case UI_STREAM_AVAILABLE: {
				doStreamAvailable(msg.getData().getString("service"),
						msg.getData().getString("mtype"), 
						msg.getData().getString("path"));
				break;
			}

			case UI_REQUEST_ACTIVATION: {
				doRequestActivation(msg.getData().getString("service"),
						msg.getData().getString("mtype"), 
						msg.getData().getString("path"));
				break;
			}


			case UI_STOP: {
				getActivity().getSupportFragmentManager().popBackStack();
				break;
			}

			case UI_MESSAGE: {
				/* Display generic message */
				Log.i(TAG, (String) msg.obj);
				Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_LONG).show();
				break;
			}

			case UI_ERROR: {
				/* Display error string in popup */
				Log.e(TAG, (String) msg.obj);
				Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_LONG).show();
				break;
			}

			default: {
				Toast.makeText(mContext, "ERR: Unkown UI command", Toast.LENGTH_LONG).show();
				break;
			}
			}//switch
		}


		// Handle a returned MediaQueryResult
		void processQueryResult(int transaction, String service, String mtype){

			if (transaction == mTransaction){
				// Only process if this is a notification for this service
				if (mtype.equals(mMediaType)) {
					Log.v(TAG, "processQueryResult()");
					updateHeader();


					// Retrieve the data from the cache
					//					if (MediaQueryCache.isPresent(service, mtype)){
					//						String mqstring = MediaQueryCache.retrieveMediaQuery(service, mtype);
					//
					//						// build a descriptor and convert to MediaIdentifiers and add to display
					//						MediaQueryResultDescriptor mqrd = new MediaQueryResultDescriptor();
					//						mqrd.setString(mqstring);
					//						Log.v(TAG, "Adding "+mqrd.size()+" items");
					//						for (int i=0; i<mqrd.size(); i++){
					//							MediaIdentifier mi = mqrd.get(i);
					//							addItem(mi);
					//						}
					//					} else
					//{
					//	Log.e(TAG, "Query results not found, ignoring");
					//}
				}
			}
		}


		// Handle a returned MediaIdentifier
		void processItemAvailable(int transaction, String service, MediaIdentifier item){

			//if (transaction == mTransaction)
			{
				// Only process if this is a notification for this service
				if ((item.mediatype.equals(mMediaType)) /*&& 
						(service.equals(mProfileId))*/) {
					Log.v(TAG, "processMediaItem("+service+", "+item.mediatype+", "+item.name+")");
					//Log.v(TAG, "processMediaItem("+service+", "+item.mediatype+", "+item.name+")");

					// OK, it's for us, add it to the list
					if (item != null){
						addItem(item);
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// need to find more efficient way to do this
								mGridview.setSelection(0);
								mGridview.requestLayout();
							}
						});

					} else {
						Log.e(TAG, "Null item, ignoring");
					}
					/***
				} else {
					Log.v(TAG, "Ignoring. Got: "+item.mediatype+", Expected: "+mMediaType);
					 ***/
				}
			}
		}


		// Update the header text
		void doUpdateHeader(){

			/**
			// Just update the title
			int count = mAdapter.getCount();
			mTitle = "Media for: " + mName + "   (" + count + ")";
			TextView   titleView;
			titleView = (TextView)  mDisplayView.findViewById(R.id.title);
			titleView.setText(mTitle);
			 **/

			// force a final update of the displayed list
			mAdapter.notifyDataSetChanged();
			mGridview.setSelection(0);
			mGridview.requestLayout();
		}


		public void doRequestFile (String service, String mtype, String rempath){
			try{
				//mTransaction = mMediaQueryClient.requestMedia(service, mtype, rempath);
				//mHandler.showError("RequestFile: not implemented yet!!!");
				Log.v(TAG, "Requesting remote file: "+rempath);
				mTransaction = MediaQueryAPI.requestMedia(service, mtype, rempath);
				mHandler.showMessage("Requested file: "+rempath);

			} catch (Exception e){
				Log.e(TAG, "doRequestFile() exception:" + e.toString());
				mHandler.showError("Error requesting file");
			}


			// Start a progress dialog
			//mProgressDialog.show (mContext, "", "Copying file...", true);
		}

		public void doFileAvailable (String service, String mtype, String lclpath){
			try {
				// Only process if this is a notification for this service
				if (mtype.equals(mMediaType)) {
					//TODO: show "Working..." dialog. Cancellation doesn't seem to work though
					//mProgressDialog.dismiss();
					Log.v(TAG, "Displaying file: "+lclpath);
					Intent intent = new Intent(Intent.ACTION_VIEW);

					// figure out the MIME type from the file extension
					String ext = lclpath.substring(lclpath.lastIndexOf(".")+1);
					String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);

					// Start the viewer for this file and MIME type
					//intent.setDataAndType(Uri.fromFile(new File(lclpath)), "image/*");
					intent.setDataAndType(Uri.fromFile(new File(lclpath)), mimetype);
					startActivity(intent);
				}
			} catch (Exception e){
				mHandler.showError ("Error displaying attachment "+lclpath);
			}
		}

		public void doRequestStream (String service, String mtype, String path){
			// No implementation yet...
		}

		//TODO: Need extra parameters, not sure what
		public void doStreamAvailable (String service, String mtype, String path){
		}

		public void doRequestActivation (String service, String mtype, String path){
			try {
				Log.v(TAG, "Activation remote file: "+path);
				if (MediaQueryAPI.requestActivation(service, mtype, path)){
					mHandler.showMessage("Activation requested");
				} else
				{
					mHandler.showError ("Error activating remote file: "+path);					
				}

			} catch (Exception e){
				e.printStackTrace();
				mHandler.showError ("Error activating remote file: "+path);
			}

		}


	}//Handler



} // end of Activity
