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




import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.ApprovalAdapter;
import org.alljoyn.aroundme.Adapters.MemberAdapter;
import org.alljoyn.aroundme.R.id;
import org.alljoyn.aroundme.R.layout;
import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaUtilities;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.TransactionListDescriptor;
import org.alljoyn.storage.MediaTransactionCache;
import org.alljoyn.storage.ProfileCache;


import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;

import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;



public class SelectMembersActivity extends ListActivity {



	private final String TAG = "SelectMembersActivity";

	private MemberAdapter           mAdapter; 

	private Button                    saveButton;
	private Button                    cancelButton;
	private ListView                  mListView ;
	private Context                   mContext;
	private static String[]           mMembers;
	private static Intent             mIntent ;


	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.memberlist);

		mContext = this;
		Intent intent = getIntent();
		String[] members = intent.getStringArrayExtra(GroupDescriptor.GroupFields.MEMBERS);
		mMembers = members; // default return list
		mIntent = new Intent(); // Intent for returning results

		if ((members==null)){
			Log.e(TAG, "New group");
			members = new String[0];
		}

		// Load the current member list and set up the adapter
		mAdapter = new MemberAdapter(this); 
		setListAdapter(mAdapter); 

		// add the supplied names and tag as selected
		ProfileDescriptor profile;
		String id="";
		for (int i=0; i<members.length; i++){
			profile = ProfileCache.getProfile(members[i]);
			id = profile.getField(ProfileDescriptor.ProfileFields._ID);
			// fix backwards compatibility issue, ID not set in earlier versions
			if ((id==null) || (id.length()==0)){
				Log.v(TAG, "Fixing Profile ID: "+id);
				id = members[i];
				if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
				profile.setField(ProfileDescriptor.ProfileFields._ID, id);
				ProfileCache.saveProfile(id, profile);
			}
			mAdapter.add(profile, true);
		}

		// add all other known profiles and tag as unselected
		String[] plist = ProfileCache.listProfiles(); // returns list of files (ID+".json")
		for (int i=0; i<plist.length; i++){
			id = plist[i];
			if (id.contains(".json")) plist[i] = id.substring(0,id.lastIndexOf(".")); // remove extension
		}

		// create a List from the String[] data for easier indexing
		List<String> mlist = Arrays.asList(members);

		// add any profiles that are not already in the member list
		for (int i=0; i<plist.length; i++){
			if (plist[i].length()>0){
				if (!mlist.contains(plist[i])){
					profile = ProfileCache.getProfile(plist[i]);
					id = profile.getField(ProfileDescriptor.ProfileFields._ID);
					// fix backwards compatibility issue
					if ((id==null) || (id.length()==0) || (id.contains(".json"))){
						id = plist[i];
						if (id.contains(".json")) id = id.substring(0,id.lastIndexOf(".")); // remove extension
						Log.v(TAG, "Fixing Profile ID: "+id);
						profile.setField(ProfileDescriptor.ProfileFields._ID, id);
						ProfileCache.saveProfile(id, profile);
					}
					Log.v(TAG, "Adding: "+plist[i]);
					mAdapter.add(ProfileCache.getProfile(plist[i]), false);
				}
			}
		}

		// Set up list for multiple selection
		mListView = getListView();
		mListView.setItemsCanFocus(false);
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		// Set up click listeners for Approve/Reject Buttons
		saveButton = (Button) findViewById(R.id.saveButton);
		cancelButton = (Button) findViewById(R.id.cancelButton);

		saveButton.setOnClickListener (mSaveListener);
		cancelButton.setOnClickListener (mCancelListener);

	}

	/* Called when the activity is exited. */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		try{
			// copy the list of selected members to the return intent
			//Log.v(TAG, "onDestroy() returning "+mMembers.length+" members");
			//mIntent.putExtra(GroupDescriptor.GroupFields.MEMBERS, mMembers);
		} catch (Exception e){
			Log.e(TAG, "onDestroy() exception: "+e.toString());
		}
		finish();
	}



	// Handler for Accept button
	private OnClickListener mSaveListener = new OnClickListener() {
		public void onClick(View v) {

			try{
				// update member list
				mMembers = mAdapter.getSelectedMembers();
				Bundle b = new Bundle();
				b.putStringArray(GroupDescriptor.GroupFields.MEMBERS, mMembers);
				mIntent.putExtras(b);

				if (getParent()==null){
					setResult(Activity.RESULT_OK, mIntent) ;
				} else {
					getParent().setResult(Activity.RESULT_OK, mIntent) ;
				}
				
				finish();
			} catch (Exception e){
				Log.e(TAG, "Error saving: "+e.toString());
			}
		}
	};


	// Handler for Cancel button
	private OnClickListener mCancelListener = new OnClickListener() {
		public void onClick(View v) {
			try{
				// just leave member list alone (same as list provided)
				if (getParent()==null){
					setResult(Activity.RESULT_CANCELED, mIntent) ;
				} else {
					getParent().setResult(Activity.RESULT_CANCELED, mIntent) ;
				}
				finish();
			} catch (Exception e){
				Log.e(TAG, "Error in Cancel: "+e.toString());
			}

		}
	};

} // end of Activity
