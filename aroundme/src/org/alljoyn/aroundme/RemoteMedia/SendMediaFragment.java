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
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.aroundme.R.drawable;
import org.alljoyn.aroundme.R.id;
import org.alljoyn.aroundme.R.layout;
import org.alljoyn.devmodules.api.mediaquery.MediaQueryAPI;
import org.alljoyn.devmodules.api.mediaquery.MediaQueryListener;
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaTypes;
import org.alljoyn.devmodules.common.MediaUtilities;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ProfileCache;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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
import android.provider.MediaStore;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


// Activity to allow the user to choose  a media file and send it to another user
public class SendMediaFragment extends Fragment {



	private static final String TAG = "SendMediaFragment";

	private  String            mName;

	// Media Type buttons
	private static Button      mBtnPhotos;
	private static Button      mBtnMusic;
	private static Button      mBtnVideos;
	private static Button      mBtnApps;

	// Action buttons
	private static Button      mBtnSend;
	private static Button      mBtnCancel;

	// Info on the selected media item
	private static TextView    mMediaLine1;
	private static TextView    mMediaLine2;
	private static TextView    mMediaLine3;
	private static ImageView   mMediaIcon;
	private        String      mSelectedFile = "";
	private        String      mMediaType = "";

	private  Context           mContext;
	private  ProfileDescriptor mProfile ;
	private static String      mProfileId="";
	private static View       mDisplayView = null;

	// Default icon
	private int mDefaultIcon = R.drawable.ic_dialog_files;



	// Thread for handling asynchronous stuff
	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private UIhandler mHandler = new UIhandler(handlerThread.getLooper()); 



	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity();

		Bundle args = getArguments();

		if (args!=null){
			if (args.containsKey(AppConstants.PROFILEID)) {
				mProfileId = args.getString(AppConstants.PROFILEID);

				// OK, retrieve the profile info for the named user
				mProfile = new ProfileDescriptor();

				// Look up the profile from cache
				if ((mProfileId!=null)&&(mProfileId.length()>0)){
					if (ProfileCache.isPresent(mProfileId)){
						mProfile = ProfileCache.getProfile(mProfileId);
					} else {
						Log.e(TAG, mProfileId+": no profile available");
					}
				}
			} else {
				Log.e(TAG, "No user specified!!!");
			}
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Layout the overall screen
		mDisplayView = inflater.inflate(R.layout.sendfile, container, false);

		// extract (debug) data from profile
		mName = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);

		// set up the buttons
		setupButtons();

		// Callbacks for the MediaQuery service
		setupMediaListener();


		// OK UI is set up, start processing
		Log.d(TAG, "Send Media to: "+mProfileId + "(" + mName + ")");

		// nothing else to do, everything else is button-driven

		return mDisplayView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	} //onDestroy


	public void setupButtons() {

		// Set up the buttons
		mBtnPhotos = (Button)  mDisplayView.findViewById(R.id.btnPhotos);
		mBtnMusic  = (Button)  mDisplayView.findViewById(R.id.btnMusic);
		mBtnVideos = (Button)  mDisplayView.findViewById(R.id.btnVideos);
		mBtnApps   = (Button)  mDisplayView.findViewById(R.id.btnApps);
		mBtnSend   = (Button)  mDisplayView.findViewById(R.id.btnSend);
		mBtnCancel = (Button)  mDisplayView.findViewById(R.id.btnCancel);

		// Disable the Send button until file selected
		mBtnSend.setClickable(false);

		// set up the button listeners
		mBtnPhotos.setOnClickListener(photoListener);
		mBtnMusic.setOnClickListener(musicListener);
		mBtnVideos.setOnClickListener(videoListener);
		mBtnApps.setOnClickListener(appsListener);
		mBtnSend.setOnClickListener(sendListener);
		mBtnCancel.setOnClickListener(cancelListener);

		// Set up the variables for the selected media item
		mMediaLine1 = (TextView)  mDisplayView.findViewById(R.id.line1);
		mMediaLine2 = (TextView)  mDisplayView.findViewById(R.id.line2);
		mMediaLine3 = (TextView)  mDisplayView.findViewById(R.id.line3);
		mMediaIcon  = (ImageView) mDisplayView.findViewById(R.id.imageIcon);

		mMediaLine1.setText("");
		mMediaLine2.setText("");
		mMediaLine3.setText("");
	}

	private void setupMediaListener(){
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
				// callback not used
			}

			@Override
			public void onItemAvailable(String service, MediaIdentifier item) {
				// callback not used
			}

			@Override
			public void onTransferComplete(String service, String path, String mtype, String localpath) {
				// callback not used
			}

			@Override
			public void onTransferError(int transaction, String service, String mtype, String path) {
				mHandler.showError("onTransferError() for: "+path);
			}

		});
	}

	private void displaySelectedFile(){
		String name = mSelectedFile;
		if (name.contains("/"))
			name = name.substring(name.lastIndexOf("/")+1);
		mMediaLine1.setText(name);
		mMediaLine2.setText(mSelectedFile);
		mMediaLine3.setText("Type: "+mMediaType);
		// Set up the File image thumbnail
		try {
			BitmapDrawable icon = null;
			ByteArrayInputStream rawIcon = null;

			byte[] thumb = new byte[0];
			//thumb = MediaUtilities.getThumbnailFromFile(mSelectedFile);
			thumb = MediaUtilities.getThumbnailFromFile(mContext, mMediaType, mSelectedFile);
			if ((thumb!=null) && (thumb.length>0)){	
				rawIcon = new ByteArrayInputStream(thumb);
				icon = new BitmapDrawable (rawIcon);
				mMediaIcon.setImageDrawable(icon);
			} else {
				mMediaIcon.setImageDrawable(mContext.getResources().getDrawable(mDefaultIcon));		
			}

		} catch (Exception e){
			// error somewhere, just set to default icon
			mMediaIcon.setImageDrawable(mContext.getResources().getDrawable(mDefaultIcon));		
		}

	}

	/////////////////////////////////////////////////////////////////////////////////
	// Click Listener(s)
	/////////////////////////////////////////////////////////////////////////////////

	// Constants for different selection/pick actions
	private static final int PICK_PHOTO = 0;
	private static final int PICK_MUSIC = 1;
	private static final int PICK_VIDEO = 2;
	private static final int PICK_APP   = 3;
	private static final int PICK_FILE  = 4;

	private OnClickListener photoListener = new OnClickListener(){
		public void onClick(View v){
			try {
				Intent intent = new Intent(Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(intent, PICK_PHOTO); 

			} catch (Exception e){
				Log.e(TAG, "Error activating photo picker: "+e.toString());
			}

		}//onClick
	};


	private OnClickListener musicListener = new OnClickListener(){
		public void onClick(View v){
			try {

				Intent intent = new Intent(Intent.ACTION_PICK,
						android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(intent, PICK_MUSIC); 

			} catch (Exception e){
				Log.e(TAG, "Error activating music picker: "+e.toString());
			}
		}//onClick
	};


	private OnClickListener videoListener = new OnClickListener(){
		public void onClick(View v){
			try {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("video/*");
				/***
				Intent intent = new Intent(Intent.ACTION_PICK,
						android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
				 ***/
				startActivityForResult(intent, PICK_VIDEO); 

			} catch (Exception e){
				Log.e(TAG, "Error activating video picker: "+e.toString());
			}
		}//onClick
	};


	private OnClickListener appsListener = new OnClickListener(){
		public void onClick(View v){
			try {
				//Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				//String mimetype = "application/vnd.android.package-archive";
				//intent.setType(mimetype);
				//intent.setType("application/*");
				Intent intent = new Intent();
				intent.setAction(AppConstants.INTENT_PREFIX + ".SELECTAPP");
				startActivityForResult(intent, PICK_APP); 
			} catch (Exception e){
				Log.e(TAG, "Error activating app picker: "+e.toString());
			}
		}//onClick
	};


	private OnClickListener fileListener = new OnClickListener(){
		public void onClick(View v){
			mHandler.showMessage("Sorry, not ready yet!");
		}//onClick
	};


	private OnClickListener sendListener = new OnClickListener(){
		public void onClick(View v){
			mHandler.sendFile (mProfileId, mMediaType, mSelectedFile);
		}//onClick
	};


	private OnClickListener cancelListener = new OnClickListener(){
		public void onClick(View v){
			getActivity().getSupportFragmentManager().popBackStack();
		}//onClick
	};


	/////////////////////////////////////////////////////////////////////////////////
	// Handler for return of selection activities
	/////////////////////////////////////////////////////////////////////////////////
	@Override  
	public void onActivityResult(int reqCode, int resultCode, Intent intent) {  
		super.onActivityResult(reqCode, resultCode, intent);  

		Uri uri ;
		String file;

		if (resultCode == Activity.RESULT_OK) {  
			uri = intent.getData(); 

			switch (reqCode){
			case PICK_PHOTO: {

				file = MediaUtilities.getImagePathFromURI(mContext.getContentResolver(), uri);

				// setup variables for selected item
				mSelectedFile = file;
				mMediaType = MediaTypes.PHOTO;
				mBtnSend.setClickable(true);

				// update the selected item display
				displaySelectedFile();
				break;
			}
			case PICK_MUSIC: {
				file = MediaUtilities.getAudioPathFromURI(mContext.getContentResolver(), uri);

				// setup variables for selected item
				mSelectedFile = file;
				mMediaType = MediaTypes.MUSIC;
				mBtnSend.setClickable(true);

				// update the selected item display
				displaySelectedFile();
				break;
			}
			case PICK_VIDEO: {
				file = MediaUtilities.getVideoPathFromURI(mContext.getContentResolver(), uri);

				// setup variables for selected item
				mSelectedFile = file;
				mMediaType = MediaTypes.VIDEO;
				mBtnSend.setClickable(true);

				// update the selected item display
				displaySelectedFile();
				break;
			}
			case PICK_APP: {
				file = uri.getEncodedPath();
				/***
				if (file.contains("/")){
					file = file.substring(file.lastIndexOf("/")+1);
				}
				 ***/

				// setup variables for selected item
				mSelectedFile = file;
				mMediaType = MediaTypes.APP;
				mBtnSend.setClickable(true);

				// update the selected item display
				displaySelectedFile();


				break;
			}
			default: {
				mHandler.showError("Unkown result: "+reqCode);
			}
			}

		} else {
			Log.e (TAG, "Activity returned with error code: "+resultCode);
		}

	} //onActivityResult


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
		private static final int UI_SEND_FILE            =  7;  // handle notification that query has finished


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

		public void sendFile(String service, String mtype, String path){
			Message msg = obtainMessage(UI_SEND_FILE);
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
				/***
					if (mMediaQueryClient==null){
						Log.e(TAG, "MediaQueryClient not set up");
					} else {
						Log.i(TAG, "UI ready");
					}
				 ***/
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


			case UI_SEND_FILE: {
				doSendFile(msg.getData().getString("service"), 
						msg.getData().getString("mtype"),
						msg.getData().getString("path"));
				break;
			}


			default: {
				Toast.makeText(mContext, "ERR: Unkown UI command", Toast.LENGTH_LONG).show();
				break;
			}
			}//switch
		}


		// Send a request to copy a local file to a remote device
		void doSendFile(String service, String mtype, String path){
			try {
				MediaQueryAPI.sendFileRequest(service, mtype, path);
				showMessage("Sent Request to transfer file...");
			} catch (Exception e){
				showError ("Error sending file: "+e.toString());
			}
		}

	}//Handler



} // end of Activity
