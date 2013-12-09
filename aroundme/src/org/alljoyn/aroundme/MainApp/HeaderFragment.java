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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.GridContactAdapter;
import org.alljoyn.profileloader.MyProfileData;
import org.alljoyn.remotecontrol.api.RemoteControlAPI;
import org.alljoyn.remotecontrol.api.RemoteControlListener;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerAPI;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerListener;
import org.alljoyn.devmodules.api.groups.GroupsAPI;

import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ProfileCache;

/**
 * A fragment that populates the header frame
 */
public class HeaderFragment extends Fragment {


	private static View mHeaderView;
	private static Context mContext = null;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public HeaderFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	} // onCreate


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

	private Menu                 mMenu;

	private static final String TAG = "HeaderFragment";

	// UI display variables for contact info
	private static TextView   mNameView;
	private static TextView   mIdView;
	private static TextView   mNumberView;
	private static ImageView  mPhotoIcon;
	private static RelativeLayout mHeaderArea;

	// GridView of nearby users
	private GridView          mGrid;

	// Info on users, groups, wifi
	private static TextView   mUserCountView;
	private static TextView   mGroupCountView;
	private static TextView   mSSIDView;

	// general UI variables

	private static ProfileDescriptor mProfile ;
	private static String     mName="";
	private GridContactAdapter mAdapter; 
	private GroupsAPI          mGroupsAPI;
	private WifiManager        mWifiManager = null;

	// Thread for handling asynchronous stuff
	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private UIhandler mHandler = new UIhandler(handlerThread.getLooper()); // mHandler for complex functions

	// function called when display view is ready for update
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


		try{

			mContext = this.getActivity();

			// layout the UI
			mHeaderView = inflater.inflate(R.layout.header, container, false);

			// display the various sections of the header
			displayContactInfo();
			displayUserList();
			displayIndicators();

			mHandler.init();


		} catch (Exception e){
			Log.e(TAG, "Error in onCreateView: "+e.toString());
		}
		return mHeaderView;
	}


	//////////////////////////////////////////////////////
	// MAIN HEADER PROCESSING
	//////////////////////////////////////////////////////


	private void displayContactInfo(){
		try{
			// Get the appropriate layout variables
			// Contact info
			mHeaderArea  = (RelativeLayout) mHeaderView.findViewById(R.id.header);
			mNameView    = (TextView)  mHeaderView.findViewById(R.id.contactName);
			mIdView      = (TextView)  mHeaderView.findViewById(R.id.contactId);
			mNumberView  = (TextView)  mHeaderView.findViewById(R.id.contactNumber);
			mPhotoIcon   = (ImageView) mHeaderView.findViewById(R.id.contactIcon);

			mProfile = new ProfileDescriptor();
			mProfile = MyProfileData.getProfile();

			// just check that we have a profile
			if (mProfile==null){
				Log.e(TAG, "Error getting MyProfileData");
				return ;
			}

			try {
				mName = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
			} catch (Exception e){
				Log.e(TAG, "Error getting My Profile Name!");
			}
			if (mName==null) mName="";

			Utilities.logMessage(TAG, "Displaying Header for: "+mName);

			String id = "";
			id = mProfile.getProfileId();
			if (id==null) id = "";

			String number="";
			number = mProfile.getField(ProfileDescriptor.ProfileFields.PHONE_MOBILE);
			if ((number==null) || (number.length()==0)){
				number = mProfile.getField(ProfileDescriptor.ProfileFields.PHONE_HOME);
			}
			Log.v(TAG, "Name:"+mName+", Id:"+id+", No:"+number);

			mNameView.setText  (mName);
			mIdView.setText(id);
			mNumberView.setText(number);

			// set the photo, if present
			try {
				byte[] bphoto = mProfile.getPhoto();
				if ((bphoto!=null) && (bphoto.length>0)){
					Bitmap image = BitmapFactory.decodeByteArray(bphoto, 0, bphoto.length);
					mPhotoIcon.setImageBitmap(image);
				} else {
					mPhotoIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_list_person));			
				}
			} catch (Exception e){
				mPhotoIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_list_person));			
			}


			// add handler for touching the general header area
			mHeaderArea.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					//launchActivity("DETAILS");
					Bundle bundle = new Bundle();
					String key = AppConstants.PROFILEID;
					bundle.putString(key, mProfile.getProfileId());
					mTaskInterface.startFunction(TaskControlInterface.Functions.USER_DETAILS, bundle);
				}});

		} catch (Exception e2){
			Log.e(TAG, "Exception in displayContactInfo(): "+e2.toString());
			e2.printStackTrace();
		}
	}

	// sets up/displays the list of nearby users
	public void displayUserList(){

		// User Grid display
		mGrid = (GridView)mHeaderView.findViewById(R.id.usersGrid);

		// add gallery of contacts and register call backs for displaying info on contacts and file transfers

		//mGrid.setOnItemSelectedListener(mProfileSelectedListener);
		mGrid.setOnItemClickListener(mProfileClickedListener);



		ProfileManagerAPI.RegisterListener(new ProfileManagerListener() {
			@Override
			public void onProfileFound(String peer) {
				Log.d(TAG, "---------------------------");
				Log.d(TAG, "Found Peer: "+peer);
				Log.d(TAG, "---------------------------");
				mHandler.addContact(peer);
			}

			@Override
			public void onProfileLost(String peer) {
				Log.d(TAG, "---------------------------");
				Log.d(TAG, "Lost Peer: "+peer);
				Log.d(TAG, "---------------------------");
				mHandler.removeContact(peer);
			};
		});

		try{
			String[] users = ProfileManagerAPI.GetNearbyUsers();
			for(String peer:users) {
				mHandler.addContact(peer);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		RemoteControlAPI.RegisterListener(new RemoteControlListener() {

			@Override
			public void onKeyDown(String groupId, int keyCode) {
				try {
					Process process = Runtime.getRuntime().exec(new String[]{"input", "keyevent", ""+keyCode});
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onExecuteIntent(String groupId, String intentAction,
					String intentData) {
				Intent i = new Intent(intentAction, Uri.parse(intentData));
				mContext.startActivity(i);
			}
		});

		// Set up list adapter for scrolling text output
		mAdapter = GridContactAdapter.getAdapter();
		mAdapter.setContext(mContext);
		mGrid.setAdapter(mAdapter); 

	}

	public void displayIndicators(){


		// Indicators
		mUserCountView  = (TextView)mHeaderView.findViewById(R.id.userCount);
		mGroupCountView = (TextView)mHeaderView.findViewById(R.id.groupCount);
		mSSIDView       = (TextView)mHeaderView.findViewById(R.id.wifiSSID);

		// get handle to Groups API
		mGroupsAPI = new GroupsAPI();

		// update displayed indicators
		mHandler.updateUserCount();
		mHandler.updateGroupCount();
		mHandler.updateSSID();

		// register for broadcast WiFi events
		IntentFilter intFilter = new IntentFilter();
		intFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		mContext.registerReceiver(mWiFiMonitor, intFilter);

	}


	// BroadcastReceiver for detecting network changes
	BroadcastReceiver mWiFiMonitor = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
				mHandler.updateSSID();
			}
		}
	};


	void launchActivity(String activity) {
		// Launch a Service-specific Activity, setting the service name as a parameter
		Intent intent = new Intent();
		String service = MyProfileData.getProfile().getProfileId();
		intent.setAction(AppConstants.INTENT_PREFIX + "." + activity);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".service", service);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".details.name",    service);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Log.e(TAG, "Error starting " + activity + " Activity for service: " + service);
			Log.e(TAG, "Exception: "+t.toString());
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
			Log.e(TAG, "Error starting " + name + " Activity for service: " + service);
			Log.e(TAG, "Exception: "+t.toString());
		}

	}


	////////////////////////////////////////////////////////////////
	// List Item Management
	////////////////////////////////////////////////////////////////




	public  OnItemClickListener mProfileClickedListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView parent, View view, int position, long id) {

			//get the profile (service) ID
			String profileid = mAdapter.getProfileId(position);
			// Check that something is selected
			if ((profileid!=null) && (profileid.length()>0)){
				Bundle bundle = new Bundle();
				bundle.putString(AppConstants.PROFILEID, profileid);
				mTaskInterface.startFunction(TaskControlInterface.Functions.NEARBY_USERS, bundle);
			} else {
				Log.e(TAG, "null Profile returned");
			}

		}//onItemClick
	};


	/////////////////////////////////////////////////////////////////////////////////
	// UI Handler Message Queue Thread
	// Initiate all UI-related functions through accessor methods for this
	/////////////////////////////////////////////////////////////////////////////////


	private class UIhandler extends Handler{

		public UIhandler(Looper loop) {
			super(loop);
		}


		// List of UI commands
		private static final int UI_INIT              =  1;  // Initialise
		private static final int UI_STOP              =  2;  // stop processing and quit
		private static final int UI_MESSAGE           =  3;  // message popup
		private static final int UI_ERROR             =  4;  // error popup
		private static final int UI_ADD_CONTACT       =  5;  // add contact to list
		private static final int UI_REMOVE_CONTACT    =  6;  // remove contact from list
		private static final int UI_UPDATE_USERCOUNT  =  7;  // update display of user count
		private static final int UI_UPDATE_GROUPCOUNT =  8;  // update display of group count
		private static final int UI_UPDATE_SSID       =  9;  // update display of SSID name


		// Accessor Methods

		public void init() {
			MainUIHandler.sendEmptyMessage(UI_INIT);
		}

		public void stop() {
			MainUIHandler.sendEmptyMessage(UI_STOP);
		}

		public void addContact(String peer){
			Message msg = MainUIHandler.obtainMessage(UI_ADD_CONTACT);
			msg.obj = peer ;
			MainUIHandler.sendMessage(msg);	
		}

		public void removeContact (String name){
			Message msg = MainUIHandler.obtainMessage(UI_REMOVE_CONTACT);
			msg.obj = name ;
			MainUIHandler.sendMessage(msg);	
		}

		public void showMessage(String text){
			Message msg = MainUIHandler.obtainMessage(UI_MESSAGE);
			msg.obj = text ;
			MainUIHandler.sendMessage(msg);	
		}

		public void showError(String error){
			Message msg = MainUIHandler.obtainMessage(UI_ERROR);
			msg.obj = error ;
			MainUIHandler.sendMessage(msg);	
		}

		public void updateUserCount(){
			MainUIHandler.sendEmptyMessage(UI_UPDATE_USERCOUNT);	
		}

		public void updateGroupCount(){
			MainUIHandler.sendEmptyMessage(UI_UPDATE_GROUPCOUNT);	
		}

		public void updateSSID(){
			MainUIHandler.sendEmptyMessage(UI_UPDATE_SSID);	
		}

		// Main UI Handler
		private Handler MainUIHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				try{
					String profileId ;
					ProfileDescriptor pdesc;

					switch (msg.what) {

					case UI_INIT: {

						//					if (mProfileClient==null){
						//						Log.e(TAG, "ProfileClient not set up");
						//					} else
						{
							String[] clist = null;
							try {
								clist = ProfileManagerAPI.GetNearbyUsers();
							} catch (Exception e) {
								e.printStackTrace();
							}
							Log.d(TAG, "getNumProfiles() returned: "+clist.length);

							if (clist != null && clist.length>0){
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
						}
						break;
					}

					case UI_STOP: {
						return;
					}

					case UI_ADD_CONTACT: {
						final ProfileDescriptor profile;
						final String profileid = (String)msg.obj;
						if (!mAdapter.contains(profileid)){

							// Get the profile from cache storage (updated on discovery)
							if (ProfileCache.isPresent(profileid)){
								profile = ProfileCache.getProfile(profileid);
							} else {
								profile = null;
								Log.w(TAG, "Profile not present in cache: "+profileid);
							}

							if(profile != null) {
								profile.setField("profileId", profileid);
								getActivity().runOnUiThread(new Runnable() {
									@Override
									public void run() {
										mAdapter.add(profileid, profile);
										showMessage("Contact found: "+profile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY));
										updateUserCount();
										updateGroupCount();
		
										// force display to be visible (default is last item is selected)
										mGrid.setSelection(0);
										mGrid.smoothScrollToPosition(0);
										mGrid.scrollTo(0, 0);
									}
								});
							} else {
								Log.e(TAG, "Null Profile");
							}
						} else {
							Log.v(TAG, "ADD_CONTACT: ignoring "+profileid);
						}
						break;
					}

					case UI_REMOVE_CONTACT: {
						final String service = (String)msg.obj;
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								try{
									showMessage("Contact left: "+mAdapter.getProfile(service).getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY));
								} catch(Exception e) {
									e.printStackTrace();
								}
								mAdapter.remove(service);
								updateUserCount();
								updateGroupCount();
		
								// force display to be visible (default is last item is selected)
								if (mAdapter.getCount()>0){
									mGrid.setSelection(0);
									mGrid.smoothScrollToPosition(0);
								}
							}
						});
						break;
					}

					case UI_UPDATE_USERCOUNT: {
						int count = mAdapter.getCount();
						if (mUserCountView!=null) 
							mUserCountView.setText(""+count);
						break;
					}

					case UI_UPDATE_GROUPCOUNT: {
						int count = mGroupsAPI.getGroupList().length;
						//TODO: make this the active group count
						if (mGroupCountView!=null) 
							mGroupCountView.setText(""+count);
						break;
					}

					case UI_UPDATE_SSID: {
						String ssid = "(none)";
						if (mWifiManager==null){
							mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
						}

						WifiInfo wifi = mWifiManager.getConnectionInfo();
						if (wifi != null) {
							ssid = wifi.getSSID();
						}
						mSSIDView.setText(ssid);
						break;
					}

					case UI_MESSAGE: {
						/* Display  string in popup */
						String txt = (String) msg.obj;
						Toast.makeText(mContext, txt, Toast.LENGTH_SHORT).show();
						Log.d(TAG, txt);
						break;
					}


					case UI_ERROR: {
						/* Display error string in popup */
						String txt = (String) msg.obj;
						Toast.makeText(mContext, txt, Toast.LENGTH_LONG).show();
						Log.e(TAG, txt);
						break;
					}

					default: {
						Toast.makeText(mContext, "ERR: Unkown UI command", Toast.LENGTH_LONG).show();
						break;
					}
					}//switch
				} catch (Exception e){
					Log.e(TAG, "Exception in HandleMessage: "+e.toString());
				}
			}
		};


	}//UIhandler

}
