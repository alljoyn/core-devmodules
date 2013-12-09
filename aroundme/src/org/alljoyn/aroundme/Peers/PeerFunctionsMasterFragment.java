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
import org.alljoyn.profileloader.MyProfileData;
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
 * This is the 'master fragment for user-specific functions
 * It lays out the general UI, displays the header for the
 * currently selected peer/user and invokes the fragment that displays 
 * the available options
 */
public class PeerFunctionsMasterFragment extends Fragment {


	private static final String TAG = "PeerFunctionsActivity";

	// UI display variables for header
	private static TextView   nameView;
	private static TextView   idView;
	private static TextView   numberView;
	private static ImageView  photoIcon;
	private static View       mDisplayView = null;

	private static ProfileDescriptor mProfile ;
	private static String   mName="";
	private static String   mNumber="";
	private static String   mProfileId="";

	private  Context            mContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity().getApplicationContext();

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

		// Layout the overall screen, then populate the main page
		mDisplayView = inflater.inflate(R.layout.peerfunctions, container, false);
		setupContactHeader();
		setupOptions();
		return mDisplayView;
	}

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



	private void setupContactHeader(){

		// Set up the Header info
		nameView    = (TextView)  mDisplayView.findViewById(R.id.peerName);
		idView      = (TextView)  mDisplayView.findViewById(R.id.peerId);
		numberView  = (TextView)  mDisplayView.findViewById(R.id.peerNumber);
		photoIcon   = (ImageView) mDisplayView.findViewById(R.id.peerIcon);

		mName = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
		nameView.setText(mName);
		
		idView.setText (mProfileId);
		
		mNumber = mProfile.getField(ProfileDescriptor.ProfileFields.PHONE_MOBILE);
		if ((mNumber==null)||mNumber.length()==0){
			mNumber = mProfile.getField(ProfileDescriptor.ProfileFields.PHONE_HOME);
		}
		numberView.setText(mNumber);

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
	} // setupContactHeader


	///////////////////////////////////////////////////////////////
	// Handling for the different buttons
	///////////////////////////////////////////////////////////////


	// Start the fragment for displaying available options
	private void setupOptions(){
		try{
			Fragment fragment = new PeerFunctionsSelectionFragment();
			Bundle bundle = new Bundle();
			bundle.putString(AppConstants.PROFILEID, mProfileId);
			bundle.putString(AppConstants.NAME,      mName);

			fragment.setArguments(bundle);

			FragmentTransaction transaction = getFragmentManager().beginTransaction();
			transaction.replace(R.id.user_display, fragment);
			//transaction.addToBackStack(null);
			transaction.commit();
		} catch (Exception e){
			Log.e(TAG, "Exception starting fragment: "+e.toString());
		}

	}//setupOptions



} // PeerFunctionsActivity
