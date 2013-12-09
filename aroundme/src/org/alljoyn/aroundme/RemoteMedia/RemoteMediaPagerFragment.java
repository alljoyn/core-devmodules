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
package org.alljoyn.aroundme.RemoteMedia;


import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.FragmentPageAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.aroundme.MainApp.TaskControlInterface;
import org.alljoyn.aroundme.Peers.PeerDetailsFragment;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerAPI;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerListener;
import org.alljoyn.devmodules.common.MediaTypes;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ProfileCache;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;



/*
 * Fragment that builds the pages for viewing different types of remote media
 */
public class RemoteMediaPagerFragment extends Fragment {


	private static final String        TAG = "RemoteMediaPagerFragment";

	private FragmentPageAdapter mAdapter = null;
	private ProfileDescriptor   mProfile = null;
	private String              mProfileId = null;
	private String              mName = null;

	private View                mDisplayView = null;
	private ViewPager           mViewPager = null;
	private Context             mContext;
	private Bundle              mSavedInstanceState=null;



	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mSavedInstanceState = savedInstanceState;

		if (mSavedInstanceState==null){
			// save context for later use
			mContext = getActivity().getBaseContext();

			// create the adapter for holding the fragments for each page
			mAdapter = new FragmentPageAdapter(getFragmentManager());
			mAdapter.setContext(mContext);

			// extract supplied ID (optional)
			Bundle args = getArguments();
			if (args!=null){
				if (args.containsKey(AppConstants.PROFILEID)) {
					mProfileId = args.getString(AppConstants.PROFILEID);
					mProfile = ProfileCache.getProfile(mProfileId);
					mName = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
				}
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// inflate the layout
		mDisplayView = inflater.inflate(R.layout.viewpager, container, false);

		// get the ViewPager object
		mViewPager = (ViewPager)mDisplayView.findViewById(R.id.viewpager);

		// associate the adapter to the ViewPager
		mViewPager.setAdapter(mAdapter);
		mViewPager.setOffscreenPageLimit(4);

		// add the pages. Note: re-using the bundle doesn't seem to work
		Bundle bundle1 = new Bundle();
		bundle1.putString(AppConstants.PROFILEID, mProfileId);
		bundle1.putString(AppConstants.MEDIATYPE, MediaTypes.PHOTO);
		//bundle1.putString(AppConstants.NAME,      mName);
		mAdapter.add(MediaTypes.PHOTO, "IMAGES", RemoteMediaQueryFragment.class.getName(), bundle1);
		
		Bundle bundle2 = new Bundle();
		bundle2.putString(AppConstants.PROFILEID, mProfileId);
		//bundle2.putString(AppConstants.NAME,      mName);
		bundle2.putString(AppConstants.MEDIATYPE, MediaTypes.MUSIC);
		mAdapter.add(MediaTypes.MUSIC, "MUSIC", RemoteMediaQueryFragment.class.getName(), bundle2);
		
		Bundle bundle3 = new Bundle();
		bundle3.putString(AppConstants.PROFILEID, mProfileId);
		//bundle3.putString(AppConstants.NAME,      mName);
		bundle3.putString(AppConstants.MEDIATYPE, MediaTypes.VIDEO);
		mAdapter.add(MediaTypes.VIDEO, "VIDEOS", RemoteMediaQueryFragment.class.getName(), bundle3);
		
		Bundle bundle4 = new Bundle();
		bundle4.putString(AppConstants.PROFILEID, mProfileId);
		//bundle4.putString(AppConstants.NAME,      mName);
		bundle4.putString(AppConstants.MEDIATYPE, MediaTypes.APP);
		mAdapter.add(MediaTypes.APP, "APPS", RemoteMediaQueryFragment.class.getName(), bundle4);
		
		mAdapter.notifyDataSetChanged();
		
		return mDisplayView;
	} // onCreateView


	@Override
	public void onDestroy() {
		super.onDestroy();
	} //onDestroy


} // RemoteMediaPagerFragment
