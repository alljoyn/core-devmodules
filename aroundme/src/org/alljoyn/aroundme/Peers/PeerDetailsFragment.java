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


import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.ContactAdapter;
import org.alljoyn.aroundme.Adapters.ProfileItemAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.aroundme.MainApp.TaskControlInterface;
import org.alljoyn.profileloader.MyProfileData;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ProfileCache;


import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import android.widget.TextView;



/*
 * Activity to display the details of a user profile
 */
public class PeerDetailsFragment extends ListFragment {


	private static final String       TAG = "PeerDetailsFragment";

	private  ProfileItemAdapter mFieldAdapter; 
	private  ProfileDescriptor  mProfile ;
	private  String             mProfileId = null;

	private  View mDisplayView = null;
	private  TextView           nameView;
	private  TextView           numberView;
	private  ImageView          photoIcon;

	private  Context            mContext;



	private  LinearLayout mBackground; // easier selection area

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity().getApplicationContext();

		Bundle args = getArguments();

		if (args!=null){
			if (args.containsKey(AppConstants.PROFILEID)) {
				mProfileId = args.getString(AppConstants.PROFILEID);
			}
		} else {
			Log.e(TAG, "No user specified!!!");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mDisplayView = inflater.inflate(R.layout.details2, container, false);

		Utilities.logMessage(TAG, "Getting Profile for: "+mProfileId);

		// OK, retrieve the profile info for the named user
		mProfile = new ProfileDescriptor();

		// Look up the profile from cache
		if ((mProfileId!=null)&&(mProfileId.length()>0)){
			if (ProfileCache.isPresent(mProfileId)){
				mProfile = ProfileCache.getProfile(mProfileId);
			} else {
				Log.e(TAG, mProfileId+": no profile available, quitting");
				return null;
			}
		} else {
			return null;
		}

		// just check that we have a profile
		if (mProfile==null){
			Log.e(TAG, "Error getting MyProfileData");
			return null;
		}


		// Set up click handler for editing (my) data
		mBackground = (LinearLayout) mDisplayView.findViewById(R.id.background);
		mBackground.setOnClickListener(mBackgroundClickListener);

		// layout the UI header fields
		nameView    = (TextView)  mDisplayView.findViewById(R.id.contactName);
		numberView  = (TextView)  mDisplayView.findViewById(R.id.contactNumber);
		photoIcon   = (ImageView) mDisplayView.findViewById(R.id.contactIcon);


		// Name
		nameView.setText  (mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY));

		// get a map of the available fields
		String[] fieldList = mProfile.getFieldList();
		HashMap<String,String>fieldMap = mapFromArray(fieldList);

		// see if there's a mobile phone number
		if (fieldMap.containsKey(ProfileDescriptor.ProfileFields.PHONE_MOBILE)){
			numberView.setText(mProfile.getField(ProfileDescriptor.ProfileFields.PHONE_MOBILE));
		}
		else if (fieldMap.containsKey(ProfileDescriptor.ProfileFields.PHONE_HOME)){
			numberView.setText(mProfile.getField(ProfileDescriptor.ProfileFields.PHONE_HOME));
		}
		else {
			numberView.setText("");
		}

		// Photo
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



		// Details - scan through list of available fields and add in some sort of logical order

		mFieldAdapter = new ProfileItemAdapter(mContext);
		setListAdapter (mFieldAdapter);

		int i;
		String key;

		// Names
		for (i=0; i<ProfileDescriptor.ProfileFields.NAME_FIELDS.length; i++){
			key = ProfileDescriptor.ProfileFields.NAME_FIELDS[i];
			if (fieldMap.containsKey(key)){
				Log.v(TAG, key+": "+mProfile.getField(key));
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

		return mDisplayView;


	} // onCreateView


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

	/////////////////////////////////////////////////////////

	// Handler for when user clicks on the details. Launch the contact editor

	private OnClickListener mBackgroundClickListener = new OnClickListener(){
		@Override
		public void onClick(View v) {   
			// launch the contact editor for the current user
			try{
				Log.v(TAG, "Stating user-spefici functions Fragment");
				/***
				String contactId = ProfileCache.retrieveContactId();
				Uri contactUri = Uri.parse(contactId);
				Intent intent = new Intent (Intent.ACTION_EDIT, contactUri);
				startActivity(intent);
				 ***/
				Log.v(TAG, "Starting User-specific Functions");
				Bundle bundle = new Bundle();
				bundle.putString(AppConstants.PROFILEID, mProfileId);
				mTaskInterface.startFunction(TaskControlInterface.Functions.USER_SPECIFIC_FUNCTIONS, bundle);
			} catch (Exception e){
				Log.e (TAG, "Error starting User-specific Functions: "+e.toString());
			}
		}//onClick

	};

	// Create a hashmap from a string array to support associative lookup
	// Java doesn't have a simple associative type, so just use hashmap instead
	private HashMap<String,String> mapFromArray(String[] array){
		HashMap<String,String> map = new HashMap<String,String>();
		for (int i=0; i<array.length; i++){
			map.put(array[i], array[i]);
			//Log.v(TAG, "Field: "+array[i]);
		}
		return map;
	}

} // About
