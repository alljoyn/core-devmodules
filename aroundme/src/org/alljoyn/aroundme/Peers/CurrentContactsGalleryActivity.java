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


/*
 * Displays List of Contacts nearby (Gallery version)
 */



import java.util.HashMap;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.GalleryContactAdapter;
import org.alljoyn.aroundme.Adapters.ProfileItemAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.profileloader.MyProfileData;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerAPI;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerAPICallback;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerListener;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import android.app.ListActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;

import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class CurrentContactsGalleryActivity extends ListActivity {


	private static final String TAG = "CurrentContactsGalleryActivity";


	private GalleryContactAdapter    mAdapter; 
	//private ProfileClient            mProfileClient ;
	private UIhandler                mUI ;
	private Menu                     mMenu;
	private static ProfileDescriptor mProfile ;

	// Display Variables
	private Gallery                  mGallery;
	private static TextView          titleView;
	private static TextView          nameView;
	private static TextView          numberView;
	private static ImageView         photoIcon;

	private static LinearLayout      detailsLayout;
	private String                   mDisplayedUser = "";
	private String                   mName = "";

	private static ProfileItemAdapter mFieldAdapter; 

	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.contactgallery2);


		// Create the UI Handler
		mUI = new UIhandler ();

		// Create the ProfileClient object 
		//mProfileClient = new ProfileClient();

		// initialise the UI variables

		// Gallery display
		mGallery = (Gallery)findViewById(R.id.gallery);

		// Overall "details" layout
		detailsLayout = (LinearLayout)findViewById(R.id.details);

		// header
		nameView    = (TextView)  findViewById(R.id.contactName);
		nameView.setText("(Select person to display details)");
		numberView  = (TextView)  findViewById(R.id.contactNumber);
		photoIcon   = (ImageView) findViewById(R.id.contactIcon);

		// details
		mFieldAdapter = new ProfileItemAdapter(this);
		setListAdapter (mFieldAdapter);

		ProfileManagerAPI.RegisterListener(new ProfileManagerListener() {
			@Override
			public void onProfileFound(String peer) {
				System.out.println("---------------------------");
				System.out.println("peer: "+peer);
				System.out.println("---------------------------");
				mUI.addContact(peer);
			}

			@Override
			public void onProfileLost(String peer) {
				System.out.println("---------------------------");
				System.out.println("peer: "+peer);
				System.out.println("---------------------------");
				mUI.removeContact(peer);
			}
		});

		// Set up list adapter for scrolling text output
		mAdapter = GalleryContactAdapter.getAdapter();
		mAdapter.setContext(this);
		mGallery.setAdapter(mAdapter); 

		// set up click listeners
		mGallery.setOnItemSelectedListener(mProfileSelectedListener);
		detailsLayout.setOnClickListener(mDetailsClickListener);
		detailsLayout.setVisibility(View.INVISIBLE); // don't display anything to start

		mDisplayedUser = "";

		mUI.init();
		// nothing else to do, the UIHandler and Adapter handle update of display etc.

	} //onCreate


	/* Called when the activity is exited. */
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy() called");
		super.onDestroy();

		//mProfileClient = null;

	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause() called");
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume() called");
		super.onResume();
	}


	/////////////////////////////////////////////////////////////////////////
	// Menu/touch action handlers
	/////////////////////////////////////////////////////////////////////////
	void launchActivity (String name) {
		Intent intent = new Intent();

		intent.setAction(AppConstants.INTENT_PREFIX + "." + name);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Toast.makeText(this, "Error starting "+ name +" Activity", Toast.LENGTH_SHORT).show();
		}	
	}


	void launchDetailsActivity(String service) {
		// Launch the Preferences Activity, force update of profile
		Intent intent = new Intent();
		intent.setAction(AppConstants.INTENT_PREFIX + ".DETAILS");
		intent.putExtra(AppConstants.INTENT_PREFIX + ".details.name", service);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Toast.makeText(this, 
					"Error starting Details Activity", 
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
			Toast.makeText(this, 
					"Error starting " + name + " Activity for service: " + service, 
					Toast.LENGTH_SHORT).show();
		}

	}

	// pause thread to give other threads a chance to run
	private void pause(){
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	////////////////////////////////////////////////////////////////
	// List Item Management
	////////////////////////////////////////////////////////////////


	/* When a gallery item is clicked, show the contact details */
	private OnItemSelectedListener mProfileSelectedListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView parent, View view, int position, long id) {

			//get the profile (service) ID
			String profileid = mAdapter.getProfileId(position);

			// Update the detailed info for this user
			if (null!=profileid){
				detailsLayout.setVisibility(View.VISIBLE);
				displayUserDetails(profileid);
			}
			else {
				Utilities.showError(TAG+" Undefined Name");
				detailsLayout.setVisibility(View.INVISIBLE);
			}
		}//onItemSelected

		@Override
		public void onNothingSelected(AdapterView parent) {
			// don't display the details layout
			detailsLayout.setVisibility(View.INVISIBLE);
		}
	};


	private OnItemClickListener mProfileClickListener = new OnItemClickListener(){
		public void onItemClick(AdapterView parent, View v, int position, long id) 
		{   
			//get the profile (service) ID
			String profileid = mAdapter.getProfileId(position);

			// Update the detailed info for this user
			if (null!=profileid){
				displayUserDetails(profileid);
			}
			else {
				Utilities.showError(TAG+" Undefined Name");
			}
		}//onItemClick
	};



	// TODO: Click listener for the "Details" Image
	private OnClickListener mDetailsClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {   
			// Check that something is selected
			if ((mDisplayedUser!=null) && (mDisplayedUser.length()>0)){
				launchServiceActivity("FUNCTIONCHOOSER", mDisplayedUser, mName);
			} 
		}//onClick

	};



	// Update the detailed display items based on the supplied profileID (service name)
	private void displayUserDetails(String profileid){
		if ((profileid==null)){
			Log.e(TAG, "Error: null profileid supplied");
			return;
		}

		Log.d(TAG, "Getting Profile for: "+profileid);

		// OK, retrieve the profile info for the named user
		mProfile = new ProfileDescriptor();

		if (MyProfileData.getSvcName().equals(profileid)){
			// Current user, get data from MyProfileData (may not be added to contact list)
			mProfile = MyProfileData.getProfile();
		} else {
			if (!mAdapter.isEmpty()){
				mProfile = mAdapter.getProfile(profileid);
			} else {
				Log.e(TAG, "no profiles available, quitting");
				return;
			}
		}

		// just check that we have a profile
		if (mProfile==null){
			Log.e(TAG, "Error getting MyProfileData");
			return;
		}


		// Header info
		mName = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
		nameView.setText  (mName);
		numberView.setText(mProfile.getField(ProfileDescriptor.ProfileFields.PHONE_MOBILE));

		try{
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


		// Details - scan through list of available fields and add in some sort of logical order

		// get a map of the available fields
		String[] fieldList = mProfile.getFieldList();
		HashMap<String,String>fieldMap = mapFromArray(fieldList);
		
		int i;
		String key;
		
		// Names
		for (i=0; i<ProfileDescriptor.ProfileFields.NAME_FIELDS.length; i++){
			key = ProfileDescriptor.ProfileFields.NAME_FIELDS[i];
			if (fieldMap.containsKey(key)){
				mFieldAdapter.add(key, mProfile.getField(key));
			}
		}
		
		// Numbers
		for (i=0; i<ProfileDescriptor.ProfileFields.PHONE_FIELDS.length; i++){
			key = ProfileDescriptor.ProfileFields.PHONE_FIELDS[i];
			if (fieldMap.containsKey(key)){
				mFieldAdapter.add(key, mProfile.getField(key));
			}
		}
		
		// Addresses
		for (i=0; i<ProfileDescriptor.ProfileFields.ADDRESS_FIELDS.length; i++){
			key = ProfileDescriptor.ProfileFields.ADDRESS_FIELDS[i];
			if (fieldMap.containsKey(key)){
				mFieldAdapter.add(key, mProfile.getField(key));
			}
		}
		
		// Emails
		for (i=0; i<ProfileDescriptor.ProfileFields.EMAIL_FIELDS.length; i++){
			key = ProfileDescriptor.ProfileFields.EMAIL_FIELDS[i];
			if (fieldMap.containsKey(key)){
				mFieldAdapter.add(key, mProfile.getField(key));
			}
		}
		
		// FAX
		for (i=0; i<ProfileDescriptor.ProfileFields.FAX_FIELDS.length; i++){
			key = ProfileDescriptor.ProfileFields.FAX_FIELDS[i];
			if (fieldMap.containsKey(key)){
				mFieldAdapter.add(key, mProfile.getField(key));
			}
		}
		
		// IM
		for (i=0; i<ProfileDescriptor.ProfileFields.IM_FIELDS.length; i++){
			key = ProfileDescriptor.ProfileFields.IM_FIELDS[i];
			if (fieldMap.containsKey(key)){
				mFieldAdapter.add(key, mProfile.getField(key));
			}
		}
		
		// Other
		for (i=0; i<ProfileDescriptor.ProfileFields.OTHER_FIELDS.length; i++){
			key = ProfileDescriptor.ProfileFields.OTHER_FIELDS[i];
			if (fieldMap.containsKey(key)){
				mFieldAdapter.add(key, mProfile.getField(key));
			}
		}


		mDisplayedUser = profileid;

	}


	// Create a hashmap from a string array to support associative lookup
	// Java doesn't have a simple associative type, so just use hashmap instead
	private HashMap<String,String> mapFromArray(String[] array){
		HashMap<String,String> map = new HashMap<String,String>();
		for (int i=0; i<array.length; i++){
			map.put(array[i], array[i]);
		}
		return map;
	}


	/////////////////////////////////////////////////////////////////////////////////
	// UI Handler Message Queue Thread
	// Initiate all UI-related functions through accessor methods for this
	/////////////////////////////////////////////////////////////////////////////////

	private class UIhandler{


		// List of UI commands
		private static final int UI_INIT           =  1;  // Initialise
		private static final int UI_STOP           =  2;  // stop processing and quit
		private static final int UI_ERROR          =  3;  // error popup
		private static final int UI_ADD_CONTACT    =  4;  // add contact to list
		private static final int UI_REMOVE_CONTACT =  5;  // remove contact from list
		private static final int UI_ADD_THIS_PROFILE = 6; // already have a profile so make UI request to add


		// Accessor Methods

		public void init() {
			MainUIHandler.sendEmptyMessage(UI_INIT);
		}

		public void stop() {
			MainUIHandler.sendEmptyMessage(UI_STOP);
		}

		public void addContact(String peer){
			Message msg = MainUIHandler.obtainMessage(UI_ADD_CONTACT);
			msg.obj = (String) peer ;
			MainUIHandler.sendMessage(msg);	
		}
		
		public void addThisContact(ProfileDescriptor profile) {
			Message msg = MainUIHandler.obtainMessage(UI_ADD_THIS_PROFILE);
			msg.obj = (ProfileDescriptor)profile;
			MainUIHandler.sendMessage(msg);
		}

		public void removeContact (String name){
			Message msg = MainUIHandler.obtainMessage(UI_REMOVE_CONTACT);
			msg.obj = name ;
			MainUIHandler.sendMessage(msg);	
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
				ProfileDescriptor pdesc;

				switch (msg.what) {

				case UI_INIT: {

					//					if (mProfileClient==null){
					//						Log.e(TAG, "ProfileClient not set up");
					//					} else
					try
					{

						// DEBUG: interface check
						//checkControlInterface();
						String [] clist = ProfileManagerAPI.GetNearbyUsers();
						int count = clist.length;
						Log.d(TAG, "getNumProfiles() returned: "+count);

						if (count>0){
							// get the list of already detected contacts and add to list
							try {
								if (clist != null){
									for (int i=0; i<clist.length; i++){
										addContact(clist[i]);
//										pdesc = new ProfileDescriptor();
//										pdesc = ProfileManagerAPI.GetProfileInfo(clist[i]);
//										if (pdesc !=null) {
//											mAdapter.add(pdesc);
//										} else {
//											Log.e(TAG, "Null descriptor returned for: "+clist[i]);
//										}
									}
								}
							} catch (Exception e) {
								//Log.e(TAG, "Error getting list of contacts: "+e.toString());
								Utilities.logException(TAG, "Error getting list of contacts: ", e);
							}
						}
					}catch(Exception e) {
						e.printStackTrace();
					}
					break;
				}

				case UI_STOP: {
					finish();
					break;
				}

				case UI_ADD_CONTACT: {
//					ProfileDescriptor profile = null;
//					try {
//						profile = ProfileManagerAPI.GetProfileInfo((String)msg.obj);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//					if(profile != null) {
//						mAdapter.add(profile);
//					}
					try {
						ProfileManagerAPI.GetProfileInfo((String)msg.obj, new ProfileManagerAPICallback() {
							@Override
							public void onProfileInfoReady(String peer, ProfileDescriptor profile) {
									mUI.addThisContact(profile);
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}

				case UI_ADD_THIS_PROFILE: {
					ProfileDescriptor profile = (ProfileDescriptor) msg.obj;

					if(profile != null) {
						mAdapter.add(profile);
					}
					break;
				}

				case UI_REMOVE_CONTACT: {
					String service = (String)msg.obj;
					mAdapter.remove(service);
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
				}//switch
			}
		};

		// Main UI setup for tabbed display


	}//UIhandler


	/////////////////////////////////////////////////////////////////////////////////
	// Test/Debug
	/////////////////////////////////////////////////////////////////////////////////

	// Debug routine to check out the interfaces for the ProfileClient
	private void checkControlInterface(){
		/*Log.d(TAG, "checkControlInterface()");

		Log.d(TAG, "1. isSet(): " + mProfileClient.isSet());

		String contactId=mProfileClient.getContactId();
		Log.d(TAG, "2. getContactId(): " + contactId);

		Log.d(TAG, "3. setContactId("+contactId+")");
		mProfileClient.setContactId(contactId);

		String profileId=mProfileClient.getProfileId();
		Log.d(TAG, "4. getProfileId():" + profileId);

		ProfileDescriptor pdesc = mProfileClient.getProfile();
		Log.d(TAG, "5. getProfile():" + pdesc.getJSONString());

		ProfileDescriptor pdesc2 = mProfileClient.getProfile(profileId);
		Log.d(TAG, "6. getProfile("+profileId+"):" + pdesc2.getJSONString());

		Log.d(TAG, "7. getNumProfiles(): " + mProfileClient.getNumProfiles());

		Log.d(TAG, "8. getContactList(): ");
		String[] list = mProfileClient.getContactList();
		if ((list!=null) && (list.length>0)){
			for (int i=0; i<list.length; i++){
				Log.d(TAG, "    ["+i+"]: "+list[i]);
			}
		} else {
			Log.d(TAG, "No Contacts found");
		}*/


	}

} // end of Activity

