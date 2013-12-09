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
import org.alljoyn.aroundme.Adapters.FragmentPageAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.aroundme.MainApp.TaskControlInterface;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerAPI;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerListener;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ProfileCache;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;



/*
 * Activity to display the details of a user profile
 */
public class NearbyUsersPagerFragment extends Fragment {


	private static final String        TAG = "NearbyUsersPagerFragment";

	private FragmentPageAdapter mAdapter = null;
	private ProfileDescriptor   mProfile ;
	private String              mProfileId = null;

	private View                mDisplayView = null;
	private ViewPager           mViewPager = null;
	private Context             mContext;
	private Bundle              mSavedInstanceState=null;


	// Thread for handling asynchronous stuff
	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private UIhandler mHandler = new UIhandler(handlerThread.getLooper()); 


	private static final String ARG_ITEM_ID = AppConstants.PROFILEID;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mSavedInstanceState = savedInstanceState;

		if (mSavedInstanceState==null){
			// save context for later use
			mContext = getActivity().getBaseContext();

			// create the adapter for holding the fragments for each page
			mAdapter = new FragmentPageAdapter(getFragmentManager(), true);
			mAdapter.setContext(mContext);

			// extract supplied name (optional)
			Bundle args = getArguments();
			if (args!=null){
				if (args.containsKey(ARG_ITEM_ID)) {
					mProfileId = args.getString(ARG_ITEM_ID);
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

		// start the UI Handler
		mHandler.init();

		// register listener for peers
		ProfileManagerAPI.RegisterListener(new ProfileManagerListener() {
			@Override
			public void onProfileFound(String peer) {
				if (mAdapter!=null){
					mHandler.addContact(peer);
				}
			}

			@Override
			public void onProfileLost(String peer) {
				mHandler.removeContact(peer);
			}
		});

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


	/////////////////////////////////////////////////////////////////////////////////
	// UI Handler Message Queue Thread
	// Initiate all UI-related functions through accessor methods for this
	/////////////////////////////////////////////////////////////////////////////////

	private class UIhandler extends Handler{

		public UIhandler(Looper loop) {
			//super(loop);
		}

		// List of UI commands
		private static final int UI_INIT           =  1;  // Initialise
		private static final int UI_STOP           =  2;  // stop processing and quit
		private static final int UI_ERROR          =  3;  // error popup
		private static final int UI_ADD_CONTACT    =  4;  // add contact to list
		private static final int UI_REMOVE_CONTACT =  5;  // remove contact from list


		// Accessor Methods

		public void init() {
			sendEmptyMessage(UI_INIT);
		}

		public void stop() {
			sendEmptyMessage(UI_STOP);
		}

		public void addContact(String peer){
			Message msg = obtainMessage(UI_ADD_CONTACT);
			msg.obj = (String) peer ;
			sendMessage(msg);	
		}

		public void removeContact (String peer){
			Message msg = obtainMessage(UI_REMOVE_CONTACT);
			msg.obj = peer ;
			sendMessage(msg);	
		}

		public void showError(String error){
			Message msg = obtainMessage(UI_ERROR);
			msg.obj = error ;
			sendMessage(msg);	
		}


		@Override
		public void handleMessage(Message msg) {

			String profileId ;
			ProfileDescriptor pdesc;

			switch (msg.what) {

			case UI_INIT: {
				try
				{
					String [] clist = ProfileManagerAPI.GetNearbyUsers();
					int count = clist.length;
					Log.d(TAG, "getNumProfiles() returned: "+count);

					if (count>0){
						// get the list of already detected contacts and add to list
						try {
							if (clist != null){
								for (int i=0; i<clist.length; i++){
									addContact(clist[i]);
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
				//finish();
				break;
			}

			case UI_ADD_CONTACT: {
				try {
					doAddTheUser((String)msg.obj);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}

			case UI_REMOVE_CONTACT: {
				final String id = (String)msg.obj;
				if (mAdapter.contains(id)){
					if(getActivity() != null) {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mAdapter.remove(id);
								mAdapter.notifyDataSetChanged();
								mViewPager.requestLayout();
							}
						});
					}
				}
				break;
			}


			case UI_ERROR: {
				/* Display error string in popup */
				Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_LONG).show();
				break;
			}

			default: {
				Toast.makeText(mContext, "ERR: Unkown UI command", Toast.LENGTH_LONG).show();
				break;
			}
			}//switch
		}
		
		private void doAddTheUser(final String id) {
			if (!mAdapter.contains(id) && getActivity() != null){
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Bundle bundle = new Bundle();
						String key = AppConstants.PROFILEID;
						bundle.putString(key, id);
						ProfileDescriptor profile = ProfileCache.getProfile(id);
						mAdapter.add(id, 
								profile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY), 
								PeerDetailsFragment.class.getName(), 
								bundle, true);
						mViewPager.requestLayout();
					}
				});
			}
		}


	}//UIhandler

} // NearbyUSersPagerFragment
