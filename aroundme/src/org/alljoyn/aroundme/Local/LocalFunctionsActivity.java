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
package org.alljoyn.aroundme.Local;


import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.GalleryContactAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.profileloader.MyProfileData;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;



/*
 * Simple  application that will the functions available locally on this device
 */
public class LocalFunctionsActivity extends Activity {

	private Menu                 mMenu;

	private static final String TAG = "LocalFunctionsActivity";

	// UI display variables for header
	private static TextView   nameView;
	private static TextView   numberView;
	private static ImageView  photoIcon;
	private static Button[]   buttonList;

	private static ProfileDescriptor mProfile ;
	private static GalleryContactAdapter mAdapter ;
	private static String   mName="";
	private static String   mService="";



	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);


		mProfile = MyProfileData.getProfile();

		// just check that we have a profile
		if (mProfile==null){
			Log.e(TAG, "Error getting Profile Data");
			finish();
		} else {


			mService = MyProfileData.getSvcName();
			mName = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
			Utilities.logMessage(TAG, "Displaying Function Page for: "+mName);


			// layout the UI (re-using front page layout, just for a different user)
			setContentView(R.layout.frontpage);

			// Set up the Header info
			nameView    = (TextView)  findViewById(R.id.contactName);
			numberView  = (TextView)  findViewById(R.id.contactNumber);
			photoIcon   = (ImageView) findViewById(R.id.contactIcon);

			nameView.setText  (mName);
			numberView.setText(mProfile.getField(ProfileDescriptor.ProfileFields.PHONE_HOME));

			try {
				byte[] bphoto = mProfile.getPhoto();
				if ((bphoto!=null) && (bphoto.length>0)){
					Bitmap image = BitmapFactory.decodeByteArray(bphoto, 0, bphoto.length);
					photoIcon.setImageBitmap(image);
				} else {
					photoIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_list_person));			
				}
			} catch (Exception e){
				photoIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_list_person));			
			}

			setupOptions();
		}
	} // onCreate


	@Override
	protected void onDestroy() {
		super.onDestroy();
		finish();
	} //onDestroy




	///////////////////////////////////////////////////////////////
	// Handling for the different buttons
	///////////////////////////////////////////////////////////////


	// Setup the appearance and actions for the selection options
	private void setupOptions(){

		Gallery gallery = (Gallery)findViewById(R.id.gallery);
		gallery.setVisibility(View.GONE);

		LinearLayout  lv1   = (LinearLayout) findViewById (R.id.btn01);
		TextView      txt1  = (TextView)  findViewById(R.id.btn01_text);
		ImageView     icon1 = (ImageView) findViewById(R.id.btn01_icon);
		txt1.setText("About");
		icon1.setBackgroundResource(R.drawable.ic_dialog_help);
		lv1.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {  
				launchServiceActivity ("ABOUT", mService, mName);
			}	
		});


		LinearLayout  lv2   = (LinearLayout) findViewById (R.id.btn02);
		TextView      txt2  = (TextView)  findViewById(R.id.btn02_text);
		ImageView     icon2 = (ImageView) findViewById(R.id.btn02_icon);
		txt2.setText("My Profile");
		icon2.setBackgroundResource(R.drawable.ic_dialog_card);
		lv2.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) { launchServiceActivity("DETAILS", mService, mName); }	
		});


		LinearLayout  lv3   = (LinearLayout) findViewById (R.id.btn03);
		TextView      txt3  = (TextView)  findViewById(R.id.btn03_text);
		ImageView     icon3 = (ImageView) findViewById(R.id.btn03_icon);
		txt3.setText("Debug");
		icon3.setBackgroundResource(R.drawable.ic_dialog_debug);
		lv3.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) { launchServiceActivity("DEBUG", mService, mName); }	
		});


		LinearLayout  lv4   = (LinearLayout) findViewById (R.id.btn04);
		TextView      txt4  = (TextView)  findViewById(R.id.btn04_text);
		ImageView     icon4 = (ImageView) findViewById(R.id.btn04_icon);
		txt4.setText("Transactions");
		icon4.setBackgroundResource(R.drawable.ic_dialog_files);
		lv4.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) { launchServiceActivity("TRANSACTIONS", mService, mName); }	
		});


		LinearLayout  lv5   = (LinearLayout) findViewById (R.id.btn05);
		lv5.setVisibility(View.INVISIBLE);

	}//setupOptions

	void launchServiceActivity(String activity, String service, String name) {
		// Launch a Service-specific Activity, setting the service name as a parameter
		Intent intent = new Intent();
		intent.setAction(AppConstants.INTENT_PREFIX + "." +           activity);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".service",      service);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".user",         name);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".details.name", service);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Toast.makeText(this, 
					"Error starting " + activity + " Activity for service: " + service, 
					Toast.LENGTH_SHORT).show();
		}

	}

} // LocalFunctionsActivity
