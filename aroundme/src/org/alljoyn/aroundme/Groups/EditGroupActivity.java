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
package org.alljoyn.aroundme.Groups;


import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.SmallContactAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.GroupCache;
import org.alljoyn.storage.ProfileCache;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.LinearLayout;
import android.widget.Toast;



/*
 * Activity/dialog to add/edit a  group. Returns Group name and description
 */
public class EditGroupActivity extends Activity {

	private static final String TAG = "EditGroupActivity";

	private static EditText       vGroupName;
	private static EditText       vDescription;
	private static CheckBox       vEnable;
	private static CheckBox       vPrivate;
	private static Button         vCreateButton;
	private static Button         vCancelButton;
	private static Button         vMemberButton;
	private Gallery               vGallery;


	private static String         mGroup ;          // name of Group
	private static String         mDescription ;    // Descriptive text
	private static boolean        mEnable;          // true if group is enabled
	private static boolean        mPrivate;         // true if group is private
	private static String[]       mMemberList;      // List of Group Members
	//private GalleryContactAdapter mAdapter; 
	private SmallContactAdapter mAdapter; 

	private Intent                mIntent ;         // return Intent
	private boolean               mChanged = false; // flag indicating that data was changed

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);


		// layout the UI and get references to fields
		setContentView(R.layout.groupedit);
		vGroupName     = (EditText)findViewById(R.id.editGroupName);
		vDescription   = (EditText)findViewById(R.id.editDescription);
		vEnable        = (CheckBox)findViewById(R.id.enableGroup);
		vPrivate       = (CheckBox)findViewById(R.id.privateGroup);
		vGallery       = (Gallery) findViewById(R.id.gallery);
		vCreateButton  = (Button)  findViewById(R.id.create);
		vCancelButton  = (Button)  findViewById(R.id.cancel);
		vMemberButton  = (Button)  findViewById(R.id.members);

		// register button handlers
		vCreateButton.setOnClickListener(mCreateListener);
		vCancelButton.setOnClickListener(mCancelListener);
		vMemberButton.setOnClickListener(mMemberListener);

		// register checkbox handler for handling private/public groups
		vPrivate.setOnCheckedChangeListener(mPrivateListener);

		// Set up Gallery adapter
		vGallery = (Gallery)findViewById(R.id.gallery);
		mAdapter = new SmallContactAdapter(this);
		vGallery.setAdapter(mAdapter); 

		// set default values
		mIntent = new Intent();
		mChanged = false;
		mGroup = "";
		mDescription = "";
		mEnable = true;
		mPrivate = false;

		// If group name specified then pre-populate the fields with existing values
		Intent myIntent = getIntent();
		String group = myIntent.getStringExtra("group");

		if ((group!=null) && (group.length()>0)){
			if (GroupCache.isGroupDetailsPresent(group)){
				GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
				if (gd != null){
					// save initial values for later comparison
					mGroup = gd.getField(GroupDescriptor.GroupFields.NAME);
					mDescription = gd.getField(GroupDescriptor.GroupFields.DESCRIPTION);
					mEnable = gd.isEnabled();
					mPrivate = gd.isPrivate();
					mMemberList = gd.getMembers();
					if ((mMemberList!=null) && (mMemberList.length>0)){
						ProfileDescriptor profile = new ProfileDescriptor();
						for (int i=0; i<mMemberList.length; i++){
							// Get the profile from cache storage (updated on discovery)
							if (ProfileCache.isPresent(mMemberList[i])){
								profile = ProfileCache.getProfile(mMemberList[i]);
							} else {
								Log.e(TAG, "Oops, member profile not found for: "+mMemberList[i]);
							}

							if(profile != null) {
								Log.v(TAG, "Loading profile: "+mMemberList[i]+" ("+
										profile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY)+")");
								mAdapter.add(mMemberList[i], profile);
							}
						}
					} else {
						Log.v(TAG, "No members found");
					}
				}
			} else {
				Log.w(TAG, "Specified group does not exist: "+group);
			}
		}

		// Set display values for fields
		vGroupName.setText(mGroup);
		vDescription.setText(mDescription);
		vEnable.setChecked(mEnable);
		vPrivate.setChecked(mPrivate);
		if (mPrivate){
			showPrivateInfo();
		} else {
			hidePrivateInfo();
		}


	} // onCreate


	@Override
	protected void onDestroy() {
		super.onDestroy();

		// set up the return value
		if (mChanged){
			mIntent.putExtra(GroupDescriptor.GroupFields.NAME, mGroup);
		} else {
			mIntent.putExtra(GroupDescriptor.GroupFields.NAME, "");
		}
		setResult(RESULT_OK, mIntent) ;
		finish();
	} //onDestroy


	// Handler for Save button
	private OnClickListener mCreateListener = new OnClickListener() {
		public void onClick(View v) {
			try{
				String  group       = vGroupName.getText().toString();
				String  description = vDescription.getText().toString();
				boolean enabled     = vEnable.isChecked();
				boolean priv        = vPrivate.isChecked();

				// Did anything change?

				if ((mGroup.equals(group)) && 
						(mDescription.equals(description)) &&
						(mEnable==enabled) &&
						(mPrivate==priv) &&
						!mChanged){
					Log.i(TAG, "No edits made, ignoring");
					// Set flag to indicate no changes
					mChanged = false;
					finish();
				} else {
					//mChanged = true;
					mGroup = group;
					mDescription = description;
					if (mDescription == null) {
						mDescription="";
					}
					mEnable  = enabled;
					mPrivate = priv;

					if ((mGroup != null) && (mGroup.length()>0)){


						// Save the data to cache for later retrieval
						GroupDescriptor gd = new GroupDescriptor();
						gd.setField(GroupDescriptor.GroupFields.NAME, mGroup);
						gd.setField(GroupDescriptor.GroupFields.DESCRIPTION, mDescription);
						String ts = Utilities.getTimestamp();
						String tsc = gd.getField(GroupDescriptor.GroupFields.TIME_CREATED);
						if ((tsc==null)||(tsc.length()==0)){
							gd.setField(GroupDescriptor.GroupFields.TIME_CREATED, ts);
						}
						gd.setField(GroupDescriptor.GroupFields.TIME_MODIFIED, ts);
						gd.setField(GroupDescriptor.GroupFields.ENABLED, (new Boolean(mEnable)).toString());
						gd.setField(GroupDescriptor.GroupFields.PRIVATE, (new Boolean(mPrivate)).toString());
						String pid;
						ProfileDescriptor profile;
						for (int i=0; i<mAdapter.getCount(); i++){
							profile = mAdapter.getProfile(i);
							pid=profile.getField(ProfileDescriptor.ProfileFields._ID);
							gd.addMember(pid);
						}
						String id = ProfileCache.retrieveName();
						gd.addMember(id); // add myself to the list
						Log.v(TAG, "Saving GroupDescriptor: "+gd.toString());
						GroupCache.saveGroupDetails(mGroup, gd);

						showMessage("Saved info for group: "+mGroup);

						// get the list of defined groups
						GroupListDescriptor gld = GroupCache.retrieveGroupList();

						// If it doesn't already exist then add it
						if (!gld.contains(mGroup)){
							gld.add(mGroup);
							GroupCache.saveGroupList(gld);
						}
						mChanged = true;
						mIntent.putExtra(GroupDescriptor.GroupFields.NAME, mGroup);
						setResult(RESULT_OK, mIntent) ;
						finish();
					} else {
						showMessage("Please enter Group Name");
					}
				}
			} catch (Exception e){
				Log.e(TAG, "Error processing group ("+mGroup+"): "+e.toString());
			}
		}

	};

	// Handler for Cancel button
	private OnClickListener mCancelListener = new OnClickListener() {
		public void onClick(View v) {
			mGroup = "";
			mDescription="";
			mChanged = false;
			finish();
		}
	};

	// Handler for Member button
	private OnClickListener mMemberListener = new OnClickListener() {
		public void onClick(View v) {
			Intent myIntent = new Intent();
			myIntent.setAction(AppConstants.INTENT_PREFIX+".SELECTMEMBERS");
			myIntent.putExtra(GroupDescriptor.GroupFields.MEMBERS, mMemberList);
			try {
				Log.d(TAG, "Starting SELECTMEMBERS Activity");
				startActivityForResult(myIntent, 0);
			} catch (Throwable t){
				showMessage(TAG+" Error starting SELECTMEMBERS Activity");
			}
		}
	};


	//  this method is called when the "Choose Members" activity returns
	@Override  
	public void onActivityResult(int reqCode, int resultCode, Intent pIntent) {  
		super.onActivityResult(reqCode, resultCode, pIntent);  

		Log.d(TAG, "Processing member list");
		if (resultCode == Activity.RESULT_CANCELED){
			// just ignore
			Log.v(TAG, "Selection cancelled, ignoring...");
		} else if (resultCode == Activity.RESULT_OK) {
			// check the return intent
			if (pIntent != null){
				// get the updated member list from the intent
				// Note that an empty list is valid
				Bundle b = pIntent.getExtras();
				String[] members = b.getStringArray(GroupDescriptor.GroupFields.MEMBERS);
				//String[] members = pIntent.getStringArrayExtra(GroupDescriptor.GroupFields.MEMBERS);
				if (members!=null){
					Log.v(TAG, members.length+" members returned");
					mAdapter.clear();
					if (members.length>0){
						ProfileDescriptor profile = new ProfileDescriptor();
						for (int i=0; i<members.length; i++){
							profile = ProfileCache.getProfile(members[i]);
							if (profile!=null){
								mAdapter.add(members[i], profile);
							}
						}
					}
					mChanged = true;
				} else {
					Log.w(TAG, "Null Member List returned");
				}
			} else {
				Log.w(TAG, "Null Intent returned");
			}
		} else {
			Log.w(TAG, "Bad resultCode in onActivityResult(): "+resultCode);
		}
	} //onActivityResult

	// Handler for "Private" checkbox
	private OnCheckedChangeListener mPrivateListener = new OnCheckedChangeListener(){

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked){
				showMessage("Press Members button to add/remove members");
				showPrivateInfo();
			} else {
				hidePrivateInfo();
			}
		}
	};

	private void hidePrivateInfo(){
		vGallery.setVisibility(View.GONE);
		vMemberButton.setVisibility(View.GONE);
	}

	private void showPrivateInfo(){
		vGallery.setVisibility(View.VISIBLE);
		vMemberButton.setVisibility(View.VISIBLE);
	}


	// utility to display Toast message
	private void showMessage(String msg){
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}
} // EditGroupActivity
