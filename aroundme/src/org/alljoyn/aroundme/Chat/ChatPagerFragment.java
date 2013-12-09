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
package org.alljoyn.aroundme.Chat;


import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.FragmentPageAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.aroundme.MainApp.TaskControlInterface;
import org.alljoyn.chat.api.ChatAPI;
import org.alljoyn.chat.api.ChatListener;
import org.alljoyn.devmodules.api.groups.GroupsAPI;
import org.alljoyn.devmodules.api.groups.GroupsListener;
import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.storage.GroupCache;
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
public class ChatPagerFragment extends Fragment {


	private static final String        TAG = "ChatPagerFragment";

	private FragmentPageAdapter mAdapter = null;

	private View                mDisplayView = null;
	private ViewPager           mViewPager = null;
	private Context             mContext;
	private Bundle              mSavedInstanceState=null;
	private GroupsAPI           mGroupsAPI;
	private ChatAPI             mChatAPI;
	private boolean             mDisplayed = false;


	// Thread for handling asynchronous stuff
	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private UIhandler mHandler = new UIhandler(handlerThread.getLooper()); 


	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);


		mSavedInstanceState = savedInstanceState;

		if (mSavedInstanceState==null){
			// save context for later use
			mContext = getActivity().getBaseContext();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (!mDisplayed){
			mDisplayed = true;
			// inflate the layout
			mDisplayView = inflater.inflate(R.layout.viewpager, container, false);

			// get the ViewPager object
			mViewPager = (ViewPager)mDisplayView.findViewById(R.id.viewpager);

			// create the adapter for holding the fragments for each page
			//mAdapter = new FragmentPageAdapter(getFragmentManager(), true);
			mAdapter = new FragmentPageAdapter(getActivity().getSupportFragmentManager(), true);
			mAdapter.setContext(mContext);

			// associate the adapter to the ViewPager
			mViewPager.setAdapter(mAdapter);
			mViewPager.setOffscreenPageLimit(10);

			// start the UI Handler
			mHandler.init();
		}

		return mDisplayView;
	} // onCreateView


	@Override
	public void onDestroy() {
		super.onDestroy();
		mHandler.stop();
		mHandler = null;
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
	// Groups Callback Handling
	/////////////////////////////////////////////////////////////////////////////////

	private GroupsListener mGroupsListener = new GroupsListener(){

		@Override
		public void onGroupInvitation(String group, String originator) {
			Log.v(TAG, "onGroupInvitation("+group+", "+originator+")");
			//mHandler.displayActiveGroups();
		}

		@Override
		public void onGroupInvitationAccepted(String group, String id) {
			Log.v(TAG, "onGroupInvitationAccepted("+group+", "+id+")");
			mHandler.displayActiveGroups();
		}

		@Override
		public void onGroupInvitationRejected(String group, String id) {
			Log.v(TAG, "onGroupInvitationRejected("+group+", "+id+")");
		}

		@Override
		public void onGroupInvitationTimeout(String group) {
			Log.v(TAG, "onGroupInvitationTimeout("+group+")");
		}

		@Override
		public void onGroupAdded(String group) {
			Log.v(TAG, "onGroupAdded("+group+")");
			mHandler.displayActiveGroups();
		}

		@Override
		public void onGroupRemoved(String group) {
			Log.v(TAG, "onGroupRemoved("+group+")");
			mHandler.removeGroup(group);
		}

		@Override
		public void onGroupActive(String group) {
			Log.v(TAG, "onGroupActive("+group+")");
			mHandler.displayActiveGroups();
		}

		@Override
		public void onGroupInactive(String group) {
			Log.v(TAG, "onGroupInactive("+group+")");
			mHandler.displayActiveGroups();
		}

		@Override
		public void onGroupEnabled(String group) {
			Log.v(TAG, "onGroupEnabled("+group+")");
			mHandler.displayActiveGroups();
		}

		@Override
		public void onGroupDisabled(String group) {
			Log.v(TAG, "onGroupDisabled("+group+")");
			mHandler.displayActiveGroups();
		}


		@Override
		public void onGroupMemberJoined(String group, String id) {}

		@Override
		public void onGroupMemberLeft(String group, String id) {}

		@Override
		public void onTestResult(String results) {}

	};



	/////////////////////////////////////////////////////////////////////////////////
	// Chat Callback Handling - Hack to force display update when message received
	/////////////////////////////////////////////////////////////////////////////////
	ChatListener mChatListener = new ChatListener(){

		@Override
		public void onChatRoomFound(String room, String[] users) {}

		@Override
		public void onChatRoomLost(String room) {}

		@Override
		public void onChatMsg(String room, String user, String msg) {}

		@Override
		public void onGroupChatMsg(String group, String user, String msg) {
			mHandler.showChatMsg(msg);
		}

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
		private static final int UI_INIT            =  1;  // Initialise
		private static final int UI_STOP            =  2;  // stop processing and quit
		private static final int UI_ERROR           =  3;  // error popup
		private static final int UI_ADD_GROUP       =  4;  // add contact to list
		private static final int UI_REMOVE_GROUP    =  5;  // remove contact from list
		private static final int UI_UPDATE_GROUPS   =  6;  // Show the list of active groups
		private static final int UI_CHAT_MESSAGE    =  7;  // Handle received chat message


		// Accessor Methods

		public void init() {
			sendEmptyMessage(UI_INIT);
		}

		public void stop() {
			//sendEmptyMessage(UI_STOP);
			handlerThread.quit();
			handlerThread = null;
		}

		public void addGroup(String group){
			Message msg = obtainMessage(UI_ADD_GROUP);
			msg.obj = (String) group ;
			sendMessage(msg);	
		}

		public void removeGroup (String group){
			Message msg = obtainMessage(UI_REMOVE_GROUP);
			msg.obj = group ;
			sendMessage(msg);	
		}

		// Show the list of active groups
		private void displayActiveGroups(){
			sendEmptyMessage(UI_UPDATE_GROUPS); 
		}

		// Show a chat message
		private void showChatMsg(String txt){
			Message msg = obtainMessage(UI_CHAT_MESSAGE);
			msg.obj = txt ;
			sendMessageDelayed(msg,100);	
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
					// Get connection to Groups service
					mGroupsAPI = new GroupsAPI();
					mGroupsAPI.registerListener(mGroupsListener);

					// Get connection to Chat service
					mChatAPI = new ChatAPI();
					mChatAPI.RegisterListener(mChatListener);

					mHandler.addGroup(""); // explicitly add the 'open' (public) group
					doUpdateGroups();

				} catch(Exception e) {
					e.printStackTrace();
				}
				break;
			}

			case UI_STOP: {
				//finish();
				break;
			}

			case UI_ADD_GROUP: {
				try {
					final String group = (String)msg.obj;
					if (!mAdapter.contains(group)){
						Log.v(TAG, "Adding Group: "+group);
						
						
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Bundle bundle = new Bundle();
								String key = AppConstants.GROUP;
								bundle.putString(key, group);

								String id = group;
								if (group.length()==0) id = "Public";
								mAdapter.add(id, 
										id, 
										org.alljoyn.chat.activity.UIFragment.class.getName(), 
										bundle);
								mAdapter.notifyDataSetChanged();
								mViewPager.requestLayout();
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}

			case UI_REMOVE_GROUP: {
				String group = (String)msg.obj;
				if (mAdapter.contains(group)){
					mAdapter.remove(group);
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mViewPager.requestLayout();
						}
					});
				}
				break;
			}

			case UI_UPDATE_GROUPS: {
				try {
					doUpdateGroups();
				} catch (Exception e) {
					Log.e(TAG, "UI_UPDATE_GROUPS: "+e.toString());
				}
				break;
			}

			case UI_CHAT_MESSAGE: {
				try {
					String txt = (String)msg.obj;
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mViewPager.requestLayout();
						}
					});
					Log.v(TAG, "onGroupChatMsg() Message: "+txt);
					Toast.makeText(mContext, "Message: "+txt, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					Log.e(TAG, "UI_CHAT_MESSAGE: "+e.toString());
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



		private void doUpdateGroups(){

			// get the list of currently active groups and add them
			String group;
			if (GroupCache.isGroupListPresent()){
				GroupListDescriptor gld = GroupCache.retrieveGroupList();
				GroupDescriptor gd = null;
				Log.v(TAG, gld.size()+" groups found");
				for (int i=0; i<gld.size(); i++){
					group = gld.get(i);
					gd = GroupCache.retrieveGroupDetails(group);
					if (mGroupsAPI.isGroupActive(group)){
						Log.v(TAG, "Active: "+group);
						addGroup(group);
					} else {
						Log.v(TAG, "Inactive: "+group);
						removeGroup(group);
					}
				}
			}
		}


	}//UIhandler

} // WhiteboardPagerFragment
