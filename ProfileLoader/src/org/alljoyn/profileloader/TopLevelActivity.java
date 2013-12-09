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

package org.alljoyn.profileloader;


// Activity to coordinate initial startup.
// Checks to see if profile has been defined. If not, it starts the profile setup activity
// Once set, it will start the background services and fire up the main display activity 
// for this user


import java.util.ArrayList;
import java.util.HashMap;

import org.alljoyn.devmodules.APICore;
import org.alljoyn.devmodules.APICoreImpl;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerAPI;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.devmodules.util.Utility;
import org.alljoyn.storage.ContactLoader;
import org.alljoyn.storage.ProfileCache;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class TopLevelActivity extends Activity {


	private static final String  TAG = "TopLevel";
	private Menu                 mMenu;
	private UIhandler            mUI ;
	private Activity             mActivity;

	private boolean restart=false;
	private boolean shutdown=false;

	// Profile-related variables
	private ProfileDescriptor  mProfile = null;
	private String             mProfileString;
	
	private String mLaunchClassString;

	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(Utility.getResourseIdByName(this.getPackageName(),"layout","top"));
		
		mLaunchClassString = this.getIntent().getStringExtra("launchClass");

		mActivity = this;

		shutdown = false;

		// Create the UI Handler
		mUI = new UIhandler ();
		mUI.init();

	} //onCreate

	/* Called when the activity is exited. */
	@Override
	protected void onDestroy() {

		if (!shutdown)
			shutdown();

		System.gc();		

		// Give things a chance to finish
		pause();

		super.onDestroy();
		finish();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause()");

		//TODO: code to save display data?
	}
	@Override
	protected void onResume() {
		super.onResume();

		Log.i(TAG, "onResume()");
	}


	@Override
	public void onBackPressed() {
		// do nothing on back. User must press Quit button to exit
		return;
	}


	/* Menu Options setup and processing */
	/* Called when the menu button is pressed. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(Utility.getResourseIdByName(this.getPackageName(),"menu","simplemenu"), menu);
		this.mMenu = menu;
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true; //must return true for options menu to display
	}

	/* Called when a menu item is selected */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* Handle item selection
		 */
		if(item.getItemId() == Utility.getResourseIdByName(this.getPackageName(),"id","quit")) {
			Toast.makeText(this, " Bye ", Toast.LENGTH_SHORT).show();
			Utilities.logMessage(TAG, "Quitting application");
			APICoreImpl.StopAllJoynServices(this);
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	// pause thread to give other threads a chance to run
	private void pause(){
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/////////////////////////////////////////////////////////////////////////
	// Menu action handlers
	/////////////////////////////////////////////////////////////////////////

	void launchProfileActivity(String action) {
		// Launch the Preferences Activity, force update of profile
		Intent intent = new Intent(this, ProfileLoaderActivity.class);
		intent.putExtra("action", action);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Toast.makeText(this, 
					"Error starting Profile Activity", 
					Toast.LENGTH_SHORT).show();
		}

	}

	// utility to shutdown UI processing
	private void shutdown () {
		shutdown=true;
		mUI.stop();
		// TODO: shutdown bus processing
		// delay for a while to give shutdown processing a chance to run
		Thread delayThread = new Thread() {
			@Override
			public void run() {
				try {
					int waited = 0;
					while (waited < 1000) {
						sleep(100);
						waited += 100;
					}
				} catch (InterruptedException e) {
					// do nothing
				} finally {
					finish();
				}
			}
		};

		delayThread.start();
	}//shutdown()


	/////////////////////////////////////////////////////////////////////////////////
	// UI Handler Message Queue Thread
	// Initiate all UI-related functions through accessor methods for this
	/////////////////////////////////////////////////////////////////////////////////

	private class UIhandler{


		// List of UI commands
		private static final int UI_INIT                 =  1;  // Initialise
		private static final int UI_STOP                 =  2;  // stop processing and quit
		private static final int UI_ERROR                =  3;  // error popup
		private static final int UI_CHECK_PROFILE        =  4;  // check whether profile is already defined or not
		private static final int UI_LOAD_PROFILE         =  5;  // load the profile
		private static final int UI_CHOOSE_CONTACT       =  6;  // launch contact picker
		private static final int UI_START_PROFILE_CLIENT =  7;  // start the profile client
		private static final int UI_SETUP_DISPLAY        =  8;  // setup main tabs


		// Accessor Methods

		public void init() {
			MainUIHandler.sendEmptyMessage(UI_INIT);
		}

		public void stop() {
			MainUIHandler.sendEmptyMessage(UI_STOP);
		}

		public void checkProfile(){
			MainUIHandler.sendEmptyMessageDelayed(UI_CHECK_PROFILE, 10);
		}

		public void loadProfile(){
			MainUIHandler.sendEmptyMessage(UI_LOAD_PROFILE);
		}

		public void chooseContact(){
			MainUIHandler.sendEmptyMessage(UI_CHOOSE_CONTACT);
		}

		public void startProfileClient(){
			MainUIHandler.sendEmptyMessage(UI_START_PROFILE_CLIENT);
		}

		public void setupDisplay (){
			MainUIHandler.sendEmptyMessageDelayed(UI_SETUP_DISPLAY, 300);
			//MainUIHandler.sendEmptyMessage(UI_SETUP_DISPLAY);
		}

		public void showError(String error){
			Message msg = MainUIHandler.obtainMessage(UI_ERROR);
			msg.obj = error ;
			MainUIHandler.sendMessage(msg);	
		}

		// Main UI Handler
		private Handler MainUIHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				String profileId ;
				//Log.v(TAG, "UIhandler handle message: "+msg.what);
				switch (msg.what) {

				case UI_INIT: {
					if(!shutdown) {
						Log.v(TAG, "init()");
						// initialise Profile Cache
						ProfileCache.init();
	
						pause();
	
						//transition to next state
						checkProfile();
					}
					break;
				}

				case UI_STOP: {
					finish();
					break;
				}

				case UI_CHECK_PROFILE: {
					doCheckProfile();
					break;
				}

				case UI_LOAD_PROFILE: {
					doLoadProfile();
					break;
				}

				case UI_CHOOSE_CONTACT: {
					doLaunchContactPicker();
					break;
				}

				case UI_START_PROFILE_CLIENT: {					
					// start the profile client
					doStartProfileClient();
					break;
				}

				case UI_SETUP_DISPLAY: {
					doSetupDisplay();
					break;
				}


				case UI_ERROR: {
					/* Display error string in popup */
					Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
					break;
				}

				default: {
					Toast.makeText(getApplicationContext(), "ERR: Unkown UI command", Toast.LENGTH_LONG).show();
					break;
				}
				}
			}
		};



	}//UIhandler



	/*
	 * ===============================================================================
	 * Action handlers for UI states
	 * ===============================================================================
	 */

	// launch the built-in contact picker activity
	void doLaunchContactPicker() {

		// popup a quick set of instructions

		final String profileIntro = 
				"\nTo use this app, you need to first define a Profile that will be used as your identity." + 
						"It does not have to really be you, it can be any contact. The name, number, photo etc." +
						"will be taken from the contact entry.\n\n" + 
						"Please select a contact from the list that appears when you leave this dialog\n"+
						"\n";

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(profileIntro)
		.setCancelable(false)
		.setIcon(Utility.getResourseIdByName(this.getPackageName(),"drawable","icon"))
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				//Start the contact picker activity
				try {
					// start Activity to choose a contact
					// return is handled in onActivityResult
					Intent intent = new Intent (Intent.ACTION_PICK, Contacts.CONTENT_URI);
					//Intent intent = new Intent ("org.alljoyn.profileconfig.MAIN");
					startActivityForResult (intent, 0);
				} catch (Exception e){
					Log.e(TAG, "Error launching Profile Configuration app");

				}
			} //onClick
		});
		AlertDialog alert = builder.create();
		alert.show();


	}


	//  this method is called when the Contact Picker (0) or CurrentContacts (1) activity returns
	@Override  
	public void onActivityResult(int reqCode, int resultCode, Intent pIntent) {  
		super.onActivityResult(reqCode, resultCode, pIntent);  
		if (reqCode==0){ // Contact Picker
			if (resultCode == Activity.RESULT_OK) {  
				if (pIntent!=null){
					// get the selected contact and save it as the default
					Uri contactURI = pIntent.getData();
					ProfileCache.saveContactId(contactURI.toString());
				}
			}

			// result doesn't matter, just loop back to check profile state
			mUI.checkProfile();
			
		} else if (reqCode==1){ // User quit the main app
			shutdown=true;
			mUI.stop();
		}else {
			Log.e(TAG, "onActivityResult() Unknown reqCode: "+reqCode);
		}
	} //onActivityResult

	private int mSetupCount = 0;

	// Check to see if the profile has already been set up, launch loader activity or set up UI
	private void doCheckProfile() {
		Log.i(TAG,"doCheckProfile()");

		try{
			if (!restart) {

				shutdown=false;


				try{

					// retrieve the default contact id (if any)
					String contactid = ProfileCache.retrieveContactId();

					if ((contactid!=null) && (contactid.length()>0)){

						Log.d(TAG, "Loading data for contactid: "+contactid);
						
						// Load the contact info
						ContactLoader loader = new ContactLoader(mActivity);
						mProfileString = loader.retrieveProfile(contactid);
						mProfile = new ProfileDescriptor();
						mProfile.setJSONString(mProfileString);
						mProfile.setProfileId(getGUID());

						// Save the contact info for use by other activities
						ProfileCache.saveProfile(getGUID(), mProfile);

						// Set up "My" profile data for use in various activities
						MyProfileData.setProfile(mProfile);

						// start the background service
						doStartService();
						// Allow threading some time to run
						pause();

					} else {
						Log.w(TAG, "Default contact not defined!");
						mSetupCount=0;
						// Default values
						mProfileString = ProfileDescriptor.EMPTY_PROFILE_STRING;
						mProfile = null;
					}
				} catch (Exception e){
					Log.e(TAG, "Exception loading profile: "+e.toString());
					mSetupCount=0;
					// Default values
					mProfileString = ProfileDescriptor.EMPTY_PROFILE_STRING;
					mProfile = null;
				}




				if(mProfile != null){
					Log.d(TAG, "doCheckProfile() Profile defined, starting load...");
					mUI.setupDisplay();
				} else {

					// Profile not already set up, so try to get one defined
					Log.d(TAG, "doCheckProfile() Profile not set up");

					// First try having the user set up a contact
					if (mSetupCount==0){
						mSetupCount++;

						// Launch contact picker. Return handled in onActivityResult()
						Log.d(TAG, "doCheckProfile() Contact not defined, starting contact picker...");
						mUI.chooseContact();
					} 

					// Second attempt, see if there is already a profile defined in the cache
					else if (mSetupCount==1){
						Log.w(TAG, "Hmmm, problems loading profile. Loading from cache instead of database...");
						mSetupCount++;
						if (ProfileCache.isNameDefined()){
							String name = ProfileCache.retrieveName();
							Log.d(TAG, "Loading profile from cached data for: "+name);
							mProfile = ProfileCache.getProfile(name);

							if (mProfile != null){
								// Get the GUID
								String guid = getGUID();
								mProfile.setProfileId(guid);

								// Set up "My" profile data for use in various activities
								MyProfileData.setProfile(mProfile);

								// start the background service
								doStartService();
							}
						}
					} 

					// First two attempts failed, give up
					else {
						//just quit if not defined
						mUI.showError("Profile not set up. Quitting...");
						mUI.stop();
					}
				}
			}
		} catch (Exception e){
			Utilities.logException(TAG, "Error getting profile: ", e);
			mUI.showError("Error getting Profile. Quitting...");
			mUI.stop();
		}

	}//checkProfile()


	// Load profile data into static data class (MyProfileData) 
	private void doLoadProfile(){
		Log.d(TAG, "doLoadProfile() Setting up local profile...");
		try {
			MyProfileData.setProfile(ProfileManagerAPI.GetMyProfile());
		} catch (Exception e) {
			Log.e(TAG, "Oops, error setting My Profile: "+e.toString());
			mUI.showError("Error setting Profile. Quitting...");
			mUI.stop();
			//e.printStackTrace();
		}
	}


	private void doStartService(){
		Log.d(TAG, "doStartService()");
		// fire up the activity that sets up the background service
		try {
			// Moved generic startup activity
			//Utilities.logMessage(TAG, "Setting up background service");
			//APICoreImpl.StartAllJoynServices(this);

			pause();
			try {
				Utilities.logMessage(TAG, "Starting Profile service");
				APICoreImpl.StartService("profile");
				pause();
				String[] services = APICoreImpl.GetServices();
				for(String service:services) {
					Log.d(TAG, "Service found: "+service);
				}

				Utilities.logMessage(TAG, "Starting all services");
				APICoreImpl.StartAllServices();
			} catch (Exception e) {
				Utilities.showError(TAG, "Error starting Services: "+e.toString());
				//e.printStackTrace();
			}

		} catch (Exception e2){
			Utilities.showError(TAG, "Error starting Background Services: "+e2.toString());
			e2.printStackTrace();
			mUI.stop();
		}
	}

	private void doStartProfileClient(){
		Log.d(TAG, "doStartProfileClient()");

	}



	// Main UI setup for tabbed display
	private void doSetupDisplay() {

		Log.d(TAG, "doSetupDisplay()");

		Intent intent = new Intent();
		intent.setAction(mLaunchClassString);
		try {
			//startActivityForResult (intent, 1);
			startActivity (intent);
			finish();
		} catch (Throwable t){
			Toast.makeText(this, 
					"Error starting Display Activity", 
					Toast.LENGTH_SHORT).show();
			mUI.stop();
		}

	}

	private  String getGUID(){
		// check to see if the name has been saved. If so, retrieve, otherwise define and save
		String guid;
		if (ProfileCache.isNameDefined()){
			guid = ProfileCache.retrieveName();
		} else {
			guid = "";
			for(int i = 0; i < 15; i++)
				guid += (char)('A'+(int)(Math.random()*26));
		}

		// save name to cache
		ProfileCache.saveName(guid);

		return guid;
	}

	/*
	 * ===============================================================================
	 * DEBUG/TEST STUFF
	 * ===============================================================================
	 */

	private void createTestData() {

		// FOR DEBUG ONLY: POPULATE SOME LISTS JUST TO SORT OUT THE UI.


	}
} // end of Activity

