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
package org.alljoyn.aroundme.Debug;


import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.GalleryContactAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.aroundme.MainApp.TaskControlInterface;
import org.alljoyn.profileloader.MyProfileData;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
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
 * Simple  application that will display the About dialog
 * Not really much to do except setup up the UI and handle the Quit menu option
 */
public class DebugFunctionsFragment extends Fragment {

	private Menu                 mMenu;

	private static final String TAG = "DebugFunctionsActivity";
	private static Button[]   buttonList;

	private static ProfileDescriptor mProfile ;
	private static GalleryContactAdapter mAdapter ;
	private static Context mContext;
	private static View mView = null;
	private static View mFrame = null;



	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity().getApplicationContext();

	} // onCreate


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// layout the UI (re-using front page layout, just for a different user)
		mView = inflater.inflate(R.layout.debugpage, container, false);
		//mFrame = inflater.inflate(R.layout.subframe, container, false);

		setupOptions();
		
		return mView;
	}//onCreateView


	@Override
	public void onDestroy() {
		super.onDestroy();
	} //onDestroy


	public TaskControlInterface mTaskInterface; // provides access to containing app

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		try{
			mTaskInterface = (TaskControlInterface)activity;
		} catch (Exception e){
			Log.e(TAG, "Exception getting Task Control Interface: "+e.toString());
		}
	}

	///////////////////////////////////////////////////////////////
	// Handling for the different buttons
	///////////////////////////////////////////////////////////////


	// Setup the appearance and actions for the buttons
	private void setupOptions(){


		LinearLayout  lv1   = (LinearLayout) mView.findViewById (R.id.btn01);
		TextView      txt1  = (TextView)  mView.findViewById(R.id.btn01_text);
		ImageView     icon1 = (ImageView) mView.findViewById(R.id.btn01_icon);
		txt1.setText("Debug Log");
		icon1.setBackgroundResource(R.drawable.ic_dialog_debug);
		lv1.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				//startFragment(new DebugLogFragment(), null);
				launchActivity ("DEBUGLOG");
			}	
		});


		LinearLayout  lv2   = (LinearLayout) mView.findViewById (R.id.btn02);
		TextView      txt2  = (TextView)  mView.findViewById(R.id.btn02_text);
		ImageView     icon2 = (ImageView) mView.findViewById(R.id.btn02_icon);
		txt2.setText("Service Browser");
		icon2.setBackgroundResource(R.drawable.ic_dialog_unpack);
		lv2.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				launchActivity ("SVCBROWSER");
			}		
		});


		LinearLayout  lv3   = (LinearLayout) mView.findViewById (R.id.btn03);
		lv3.setVisibility(View.INVISIBLE);

		LinearLayout  lv4   = (LinearLayout) mView.findViewById (R.id.btn04);
		lv4.setVisibility(View.INVISIBLE);

		LinearLayout  lv5   = (LinearLayout) mView.findViewById (R.id.btn05);
		lv5.setVisibility(View.INVISIBLE);

	}//setupOptions


	void launchActivity(String activity) {
		// Launch a Service-specific Activity, setting the service name as a parameter
		Intent intent = new Intent();
		intent.setAction(AppConstants.INTENT_PREFIX + "." + activity);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Toast.makeText(mContext, "Error starting " + activity, Toast.LENGTH_SHORT).show();
		}

	}


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
			//transaction.replace(R.id.subframe, fragment);
			transaction.replace(R.id.user_display, fragment);
			//transaction.addToBackStack(null);
			transaction.setTransition(FragmentTransaction.TRANSIT_ENTER_MASK);
			transaction.commitAllowingStateLoss();
		} catch (Exception e){
			Log.e(TAG, "Exception starting fragment: "+e.toString());
		}
	}

} // DebugFunctionsActivity
