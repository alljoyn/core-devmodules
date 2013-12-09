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
package org.alljoyn.aroundme.Peers;


import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.GalleryContactAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.aroundme.MainApp.TaskControlInterface;
import org.alljoyn.aroundme.RemoteMedia.RemoteDebugFragment;
import org.alljoyn.aroundme.RemoteMedia.RemoteKeyFragment;
import org.alljoyn.aroundme.RemoteMedia.RemoteMediaPagerFragment;
import org.alljoyn.aroundme.RemoteMedia.RemoteMediaQueryFragment;
import org.alljoyn.aroundme.RemoteMedia.SendMediaFragment;
import org.alljoyn.profileloader.MyProfileData;
import org.alljoyn.devmodules.common.MediaTypes;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ProfileCache;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;



/*
 * Fragment that displays the options available for user-specific functions
 */
public class PeerFunctionsSelectionFragment extends Fragment {


	private static final String TAG = "PeerFunctionsSelectionFragment";
	private static String   mProfileId="";
	private static String   mName="";
	private static View mDisplayView = null;

	private  Context            mContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity().getApplicationContext();

		Bundle args = getArguments();

		if (args!=null){
			if (args.containsKey(AppConstants.PROFILEID)) {
				mProfileId = args.getString(AppConstants.PROFILEID);
			} else {
				Log.e(TAG, "No user specified!!!");
			}
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Layout the overall screen, then populate the main page
		mDisplayView = inflater.inflate(R.layout.functionlist, container, false);
		setupOptions();
		return mDisplayView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	} //onDestroy



	///////////////////////////////////////////////////////////////
	// Handling for the different buttons
	///////////////////////////////////////////////////////////////


	// Setup the appearance and actions for the buttons
	private void setupOptions(){

		final String postfix = "." + mProfileId; // extract the unique ID (including ".")
		Log.i(TAG,"postfix: "+postfix);

		LinearLayout  lv1   = (LinearLayout) mDisplayView.findViewById (R.id.btn01);
		TextView      txt1  = (TextView)  mDisplayView.findViewById(R.id.btn01_text);
		ImageView     icon1 = (ImageView) mDisplayView.findViewById(R.id.btn01_icon);
		txt1.setText("View Remote Media");
		icon1.setBackgroundResource(R.drawable.ic_dialog_applications);
		lv1.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Bundle bundle = new Bundle();
				bundle.putString(AppConstants.PROFILEID, mProfileId);
				bundle.putString(AppConstants.NAME,      mName);
				startFragment (new RemoteMediaPagerFragment(), bundle);
			}	
		});


		LinearLayout  lv2   = (LinearLayout) mDisplayView.findViewById (R.id.btn02);
		TextView      txt2  = (TextView)  mDisplayView.findViewById(R.id.btn02_text);
		ImageView     icon2 = (ImageView) mDisplayView.findViewById(R.id.btn02_icon);
		txt2.setText("View Remote Debug Log");
		icon2.setBackgroundResource(R.drawable.ic_dialog_debug);
		lv2.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Bundle bundle = new Bundle();
				bundle.putString(AppConstants.PROFILEID, mProfileId);
				bundle.putString(AppConstants.NAME,      mName);
				startFragment (new RemoteDebugFragment(), bundle);
			}		
		});

		LinearLayout  lv3   = (LinearLayout) mDisplayView.findViewById (R.id.btn03);
		TextView      txt3  = (TextView)  mDisplayView.findViewById(R.id.btn03_text);
		ImageView     icon3 = (ImageView) mDisplayView.findViewById(R.id.btn03_icon);
		txt3.setText("Send (Media) Files");
		icon3.setBackgroundResource(R.drawable.ic_dialog_files);
		lv3.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Bundle bundle = new Bundle();
				bundle.putString(AppConstants.PROFILEID, mProfileId);
				startFragment (new SendMediaFragment(), bundle);
			}		
		});

		LinearLayout  lv4   = (LinearLayout) mDisplayView.findViewById (R.id.btn04);
		lv4.setVisibility(View.GONE);
//		TextView      txt4  = (TextView)  mDisplayView.findViewById(R.id.btn04_text);
//		ImageView     icon4 = (ImageView) mDisplayView.findViewById(R.id.btn04_icon);
//		txt4.setText("Inject Remote Key Events");
//		icon4.setBackgroundResource(R.drawable.ic_dialog_unpack);
//		lv4.setOnClickListener(new OnClickListener(){
//			@Override
//			public void onClick(View v) {
//				/***
//				String service = "remotekey"+postfix;
//				launchServiceActivity ("KEYEVENTS", service, mName);
//				***/
//				Bundle bundle = new Bundle();
//				bundle.putString(AppConstants.PROFILEID, mProfileId);
//				startFragment (new RemoteKeyFragment(), bundle);
//			}		
//		});

		LinearLayout  lv5   = (LinearLayout) mDisplayView.findViewById (R.id.btn05);
		lv5.setVisibility(View.GONE);

	}//setupOptions



	/**
	 * Utility to launch the supplied fragment in the display frame
	 * @param fragment The fragment to start
	 * @param bundle Arguments to pass to the fragment. Set to null if not arguments are needed
	 */
	private void startFragment(Fragment fragment, Bundle bundle){
		try{
			if (bundle!=null){
				fragment.setArguments(bundle);
			}
			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.user_display, fragment);
			transaction.addToBackStack(null);
			transaction.commit();
		} catch (Exception e){
			Log.e(TAG, "Exception starting fragment: "+e.toString());
		}
	}

	void launchServiceActivity(String activity, String service, String name) {
		// Launch a Service-specific Activity, setting the service name as a parameter
		Intent intent = new Intent();
		intent.setAction(AppConstants.INTENT_PREFIX + "." +      activity);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".service", service);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".user",    name);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Toast.makeText(mContext, 
					"Error starting " + activity + " Activity for service: " + service, 
					Toast.LENGTH_SHORT).show();
		}

	}

} // PeerFunctionsSelectionFragment
