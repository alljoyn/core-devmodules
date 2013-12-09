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
import org.alljoyn.aroundme.Adapters.RemoteDebugAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.devmodules.api.debug.DebugAPI;
import org.alljoyn.devmodules.api.debug.DebugListener;
import org.alljoyn.devmodules.common.DebugMessageDescriptor;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.storage.ProfileCache;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


// Simple debug display based on data held in the RemoteDebugListManager for the specified service
/**
 * Fragment to display debug output from remote user
 * NOTE: the Android logcat facility is a little flaky since ICS, so you may need to leave
 *       and re-enter for this to work 
 * @author pprice
 *
 */
public class RemoteDebugFragment extends ListFragment {



	private static final String TAG = "DebugLogFragment";

	private  RemoteDebugAdapter    mAdapter; 
	private  String                mName;


	private  Context           mContext;
	private  ProfileDescriptor mProfile ;
	private static String      mProfileId="";
	private static View        mDisplayView = null;


	// Thread for handling asynchronous stuff
	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private UIhandler mHandler = new UIhandler(handlerThread.getLooper()); 


	/* Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity();

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

		// Layout the overall screen
		mDisplayView = inflater.inflate(R.layout.debug, container, false);

		// extract (debug) data from profile
		mName = mProfile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);

		DebugAPI.RegisterListener(new DebugListener() {

			@Override
			public void onDebugServiceAvailable(String service) {

			}

			@Override
			public void onDebugServiceLost(String service) {
				mHandler.serviceDisconnected(service);
			}

			@Override
			public void onDebugMessage(String service, int level, String message) {
				//if (service.contains(mProfileId)){
				DebugMessageDescriptor dmd = new DebugMessageDescriptor(service, level, message);
				mHandler.addDebugMessage(dmd);
				//} else {
				//	Log.v(TAG, "Ignoring dbg msg for: "+service);
				//}
			}

		});

		// Set up the debug list for this service
		RemoteDebugListManager.addDebugList(mProfileId);

		// Set up list adapter for scrolling text output
		mAdapter = RemoteDebugListManager.get(mProfileId) ;
		mAdapter.setContext(mContext); 
		setListAdapter(mAdapter); 

		mHandler.init();

		// nothing else to do, the UIHandler and ListAdapter handle update of display etc.

		return mDisplayView;
	}

	/////////////////////////////////////////////////////////////////////////////////
	// UI Handler Message Queue Thread
	// Initiate all UI-related functions through accessor methods for this
	/////////////////////////////////////////////////////////////////////////////////

	private class UIhandler extends Handler{

		public UIhandler(Looper loop) {
			super(loop);
		}


		// List of UI commands
		private static final int UI_INIT                 =  1;  // Initialise
		private static final int UI_STOP                 =  2;  // stop processing and quit
		private static final int UI_ERROR                =  3;  // error popup
		private static final int UI_SERVICE_CONNECTED    =  4;  // remote service connected
		private static final int UI_SERVICE_DISCONNECTED =  5;  // remote service disconnected
		private static final int UI_ADD_MESSAGE          =  6;  // add message to list


		// Accessor Methods

		public void init() {
			sendEmptyMessage(UI_INIT);
		}

		public void stop() {
			sendEmptyMessage(UI_STOP);
		}

		public void showError(String error){
			Message msg = obtainMessage(UI_ERROR);
			msg.obj = error ;
			sendMessage(msg);	
		}


		public void serviceConnected (String service){
			Message msg = obtainMessage(UI_SERVICE_CONNECTED);
			msg.obj = service ;
			sendMessage(msg);	
		}

		public void serviceDisconnected (String service){
			Message msg = obtainMessage(UI_SERVICE_DISCONNECTED);
			msg.obj = service ;
			sendMessage(msg);	
		}
		public void addDebugMessage(DebugMessageDescriptor dmd){
			Message msg = obtainMessage(UI_ADD_MESSAGE);
			msg.obj = dmd ;
			sendMessage(msg);	
		}


		@Override
		public void handleMessage(Message msg) {

			String profileId ;
			switch (msg.what) {

			case UI_INIT: {
				try {
					Log.d(TAG, "Initiating connection to: "+mProfileId);
					DebugAPI.Connect(mProfileId,"debug");
				} catch (Exception e) {
					showError(TAG+" Error connecting to Debug Control Service: "+ e.toString());
				}
				break;
			}

			case UI_STOP: {
				RemoteDebugListManager.removeDebugList(mProfileId);
				try {
					DebugAPI.Disconnect(mProfileId,"debug");
				} catch (Exception e) {
					Log.e(TAG, "Error disconnecting from: "+mProfileId);
				}
				if(getActivity() != null && getActivity().getSupportFragmentManager() != null)
					getActivity().getSupportFragmentManager().popBackStack();
				break;
			}

			case UI_SERVICE_CONNECTED: {
				// OK, we're connected to the remote Debug service, so get the list of cached messages
				// and add to the local list

				/*** don't do this for now
					try {
						DebugMessageDescriptor[] mlist = new DebugMessageDescriptor[0];
						mlist = mDebugClient.getMessageList(mProfileId);
						if ((mlist!=null) && (mlist.length>0)){
							for (int i=0; i<mlist.length; i++){
								mAdapter.add(mlist[i]);
							}
						}
					} catch (Exception e) {
						showError(TAG+" Error getting list of messages: "+ e.toString());
					}***/
				break;
			}

			case UI_SERVICE_DISCONNECTED: {
				showError("Remote Debug service disconnected ("+mProfileId+")");
				break;
			}

			case UI_ADD_MESSAGE: {
				Log.v(TAG, "Remote debug msg: "+((DebugMessageDescriptor)msg.obj).message);
				if(getActivity() != null) {
					final DebugMessageDescriptor debugMsg = (DebugMessageDescriptor)msg.obj;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mAdapter.add(debugMsg);
							mDisplayView.requestLayout();
						}
					});
				}
				break;
			}

			case UI_ERROR: {
				/* Display error string in popup */
				Log.e(TAG, (String) msg.obj);
				Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_LONG).show();
				break;
			}

			default: {
				Toast.makeText(mContext, "ERR: Unkown UI command", Toast.LENGTH_LONG).show();
				break;
			}
			}//switch
		}


	}//UIhandler



} // end of Activity
