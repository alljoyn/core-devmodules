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

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.GalleryContactAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.aroundme.R.id;
import org.alljoyn.aroundme.R.layout;
import org.alljoyn.remotecontrol.api.RemoteControlAPI;
import org.alljoyn.devmodules.api.debug.DebugAPI;
import org.alljoyn.devmodules.api.debug.DebugListener;
import org.alljoyn.devmodules.common.DebugMessageDescriptor;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ProfileCache;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import android.widget.TextView;



/*
 * Simple  application that will display the About dialog
 * Not really much to do except setup up the UI and handle the Quit menu option
 */
public class RemoteKeyFragment extends Fragment {


	private static final String TAG = "RemoteKeyActivity";

	private static Button[]   buttonList;

	private static String   mName="";


	private  Context           mContext;
	private  ProfileDescriptor mProfile ;
	private static String      mProfileId="";
	private static View        mDisplayView = null;

	/* Called when the activity is first created. */

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
		mDisplayView = inflater.inflate(R.layout.frontpage, container, false);

		// extract (debug) data from profile
		mName = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);

		Utilities.logMessage(TAG, "Displaying Remote Key Page for: "+mName);

		setupButtons();

		return mDisplayView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	} //onDestroy




	///////////////////////////////////////////////////////////////
	// Handling for the different buttons
	///////////////////////////////////////////////////////////////


	private class buttonDescriptor {
		buttonDescriptor (int r, String t, String i){
			resource = r;
			text     = t;
			intent   = i;
		}
		public int    resource;
		public String text;
		public String intent;
	}


	private buttonDescriptor[] buttonConfig = {
			new buttonDescriptor(R.id.btn01, "Back",        ""),
			new buttonDescriptor(R.id.btn02, "Volume up",   ""),
			new buttonDescriptor(R.id.btn03, "Volume down", ""),
			new buttonDescriptor(R.id.btn04, "Menu",        ""),
			new buttonDescriptor(R.id.btn05, "",            ""),
			new buttonDescriptor(R.id.btn06, "",            ""),
			new buttonDescriptor(R.id.btn07, "",            ""),
			new buttonDescriptor(R.id.btn08, "",            ""),
			new buttonDescriptor(R.id.btn09, "exit",        "")
	};

	// routine to launch an activity


	// Setup the appearance and actions for the buttons
	private void setupButtons(){

		buttonList = new Button[buttonConfig.length];
		Log.i(TAG,"mProfileId: "+mProfileId);
		/***
		String postfix = mProfileId;
		if (postfix.contains(".")){
			postfix = mProfileId.substring(mProfileId.lastIndexOf('.')); // extract the unique ID (including ".")
		}
		Log.i(TAG,"postfix: "+postfix);
		***/

		for (int i=0; i<buttonConfig.length; i++){
			buttonList[i] = (Button)  mDisplayView.findViewById(buttonConfig[i].resource);
			buttonList[i].setText(buttonConfig[i].text);
		}

		// can't dynamically create index-based handlers in a loop, so just declare each one
		buttonList[0].setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				try {
					RemoteControlAPI.SendKey(mProfileId, KeyEvent.KEYCODE_BACK);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	
		});

		buttonList[1].setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				try {
					RemoteControlAPI.SendKey(mProfileId, KeyEvent.KEYCODE_VOLUME_UP);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	
		});

		buttonList[2].setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				try {
					RemoteControlAPI.SendKey(mProfileId, KeyEvent.KEYCODE_VOLUME_DOWN);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	
		});

		buttonList[3].setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				try {
					RemoteControlAPI.SendKey(mProfileId, KeyEvent.KEYCODE_MENU);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}	
		});

		buttonList[8].setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				getActivity().getSupportFragmentManager().popBackStack();
			}	
		});


	}//setupButtons

} // LocalFunctionsActivity
