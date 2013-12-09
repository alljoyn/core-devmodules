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
package org.alljoyn.aroundme.MainApp;

import org.alljoyn.aroundme.R;
import org.alljoyn.profileloader.MyProfileData;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


/**
 * A fragment that implements the "front page" of the app. It displays an intro to the app 
 * and a summary of current activity
 */
public class FrontPageFragment extends Fragment {


	private static View       mFragView = null; // the View occupied by the Fragment
	private static Context    mContext = null;  // the context of the calling Application


	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public FrontPageFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		// save the app context for use in inner routines
		mContext = this.getActivity();

		// load the display layout
		mFragView = inflater.inflate(R.layout.frontpage, container, false);

		// populate the buttons (replace later?)
		setupButtons();
		
		return mFragView;
	}


	///////////////////////////////////////////////////////////////
	// Handling for the different buttons
	///////////////////////////////////////////////////////////////


	// Setup the appearance and actions for the selection options
	private void setupButtons(){

		// For this version, we don't use the buttons, so just make sure 
		// it is not layed out
		LinearLayout  lv1   = (LinearLayout) mFragView.findViewById (R.id.btn01);
		LinearLayout  lv2   = (LinearLayout) mFragView.findViewById (R.id.btn02);
		LinearLayout  lv3   = (LinearLayout) mFragView.findViewById (R.id.btn03);
		LinearLayout  lv4   = (LinearLayout) mFragView.findViewById (R.id.btn04);
		LinearLayout  lv5   = (LinearLayout) mFragView.findViewById (R.id.btn05);
		lv1.setVisibility(View.GONE);
		lv2.setVisibility(View.GONE);
		lv3.setVisibility(View.GONE);
		lv4.setVisibility(View.GONE);
		lv5.setVisibility(View.GONE);

	}//setupOptions


	void launchActivity(String activity) {
		// Launch a Service-specific Activity, setting the service name as a parameter
		Intent intent = new Intent();
		String service = MyProfileData.getSvcName();
		intent.setAction(AppConstants.INTENT_PREFIX + "." + activity);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".service", service);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".details.name",    service);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Toast.makeText(mContext, 
					"Error starting " + activity + " Activity for service: " + service, 
					Toast.LENGTH_SHORT).show();
		}

	}




	// launch a service-specific activity
	void launchServiceActivity(String name, String service, String user) {
		// Launch a Service-specific Activity, setting the service name as a parameter
		Intent intent = new Intent();
		intent.setAction(AppConstants.INTENT_PREFIX + "." + name);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".service", service);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".user", user);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Toast.makeText(mContext, 
					"Error starting " + name + " Activity for service: " + service, 
					Toast.LENGTH_SHORT).show();
		}

	}

}
