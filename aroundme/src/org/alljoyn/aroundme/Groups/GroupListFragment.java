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
import org.alljoyn.aroundme.Adapters.GroupAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.devmodules.api.groups.GroupsAPI;
import org.alljoyn.devmodules.api.groups.GroupsListener;
import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.GroupCache;
import org.alljoyn.storage.ProfileCache;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;



public class GroupListFragment extends Fragment {



	private static final String   TAG = "GroupListFragment";
	private static final String   ACTIVE_TAG = "ACTIVE";
	private static final String   INACTIVE_TAG = "INACTIVE";

	private static GroupAdapter   mInactiveGroupAdapter; 
	private static GroupAdapter   mActiveGroupAdapter; 
	private GroupListDescriptor   mInactiveGroupDescriptor ;
	private GroupListDescriptor   mActiveGroupDescriptor ;
	private GroupsAPI             mGroupsAPI;

	private static boolean        mShutdown = false;
	private static Context        mContext;
	private Activity              mActivity;

	private ListView              mInactiveGroupList;
	private ListView              mActiveGroupList;

	private static Button         mAddGroupButton ;
	private  View mDisplayView = null;

	// Thread for handling asynchronous stuff
	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private UIhandler mHandler = new UIhandler(handlerThread.getLooper()); 



	/* Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity().getApplicationContext();
		mActivity = getActivity();
		Log.v(TAG, "onCreate()");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		try{

			mDisplayView = inflater.inflate(R.layout.grouplist, container, false);


			mInactiveGroupList  = (ListView) mDisplayView.findViewById(R.id.inactiveGroupList);
			mActiveGroupList = (ListView) mDisplayView.findViewById(R.id.activeGroupList);
			mAddGroupButton  = (Button)   mDisplayView.findViewById(R.id.addGroup);

			// Set up list adapters for scrolling text output
			mInactiveGroupAdapter = new GroupAdapter(mContext); 
			mInactiveGroupList.setAdapter(mInactiveGroupAdapter); 

			mActiveGroupAdapter = new GroupAdapter(mContext); 
			mActiveGroupList.setAdapter(mActiveGroupAdapter); 

			mInactiveGroupDescriptor = new GroupListDescriptor();
			mActiveGroupDescriptor = new GroupListDescriptor();

			// Handler for clicking on item in either list
			mActiveGroupList.setOnItemClickListener(mListClickListener);
			mInactiveGroupList.setOnItemClickListener(mListClickListener);

			// Set tags on the lists so that can be distinguished in the callbacks
			mActiveGroupList.setTag(ACTIVE_TAG);
			mInactiveGroupList.setTag(INACTIVE_TAG);

			// Set up context menu (long press)
			registerForContextMenu(mActiveGroupList);
			registerForContextMenu(mInactiveGroupList);

			// Handler for Add Group button
			mAddGroupButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					mHandler.editGroup("");
				}
			});

			mShutdown      = false;

			// Start the UI Handler
			mHandler.init();

			// nothing else to do, the ListAdapter handles update of display etc.
		} catch (Exception e){
			Log.e(TAG, "onCreateView() Exception: "+e.toString());
		}
		return mDisplayView;
	}



	/* Called when the activity is exited. */
	@Override
	public void onDestroy() {
		super.onDestroy();
		mShutdown = true;
		mHandler.shutdown();
	}


	/////////////////////////////////////////////////////////////////////////////////
	// Menu Handling
	/////////////////////////////////////////////////////////////////////////////////

	// None, right now

	/////////////////////////////////////////////////////////////////////////////////
	// List Item Selection Handling
	/////////////////////////////////////////////////////////////////////////////////

	OnItemClickListener mListClickListener = new ListView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

			// check which type of list
			String ltype = (String) parent.getTag();


			// get the group name for the selected item
			String name = "";
			if (ltype != null){
				if (ltype.equals(ACTIVE_TAG)){
					name = mActiveGroupAdapter.getName(position);
				} else if (ltype.equals(INACTIVE_TAG)){
					name = mInactiveGroupAdapter.getName(position);
				} else {
					Log.e(TAG, "onItemClick() Unknown tag: "+ltype);
				}
			} else {
				Log.e(TAG, "onItemClick() NULL tag");
			}

			// Edit the appropriate group
			mHandler.editGroup(name);

			/***

			// Launch the selected Activity (if entry is not null)

			if ((null!=name) && (name.length()>0)){
				Intent myIntent = new Intent();
				myIntent.setAction(AppConstants.INTENT_PREFIX+".GROUPTAB");
				myIntent.putExtra(AppConstants.INTENT_PREFIX+".group", name); 

				try {
					startActivity(myIntent);
				} catch (Throwable t){
					mHandler.showMessage(TAG+" Error starting GROUPTAB Activity");
				}
			}
			else {
				mHandler.showMessage(TAG+" Undefined Name");
			}
			 ***/
		}
	};

	/////////////////////////////////////////////////////////////////////////////////
	// Long Press Handling
	/////////////////////////////////////////////////////////////////////////////////


	// Handlers for pop-up context menu on long click
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		// inflate and display the context menu
		MenuInflater inflater = mActivity.getMenuInflater();

		// check which type of list
		String ltype = (String) v.getTag();

		// inflate appropriate layout
		if (ltype.equals(ACTIVE_TAG)){
			inflater.inflate(R.menu.groupsactivecontextmenu, menu);
		} else if (ltype.equals(INACTIVE_TAG)){
			inflater.inflate(R.menu.groupsinactivecontextmenu, menu);
		} else {
			Log.e(TAG, "onCreateContextMenu() Unknown tag: "+ltype);
		}
	}//onCreateContextMenu


	// Called when a listitem is long-clicked
	@Override
	public boolean onContextItemSelected(MenuItem item) {

		// get the selected item
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

		int pos = (int) info.id; // why do listitem and context menu use different fields?!
		ListView v = (ListView) info.targetView.getParent();


		String name = "";
		String ltype = (String) v.getTag();

		// inflate appropriate layout
		if (ltype != null){
			if (ltype.equals(ACTIVE_TAG)){
				name = mActiveGroupAdapter.getName(pos);
			} else if (ltype.equals(INACTIVE_TAG)){
				name = mInactiveGroupAdapter.getName(pos);
			} else {
				Log.e(TAG, "onContextItemSelected() Unknown tag: "+ltype);
			}
		} else {
			Log.e(TAG, "onContextItemSelected() Null View tag");
		}

		if ((name!=null) && (name.length()>0)){
			//process based on the item and function selected
			switch (item.getItemId()) {
			case R.id.edit: {
				Log.v(TAG, "EDIT selected");
				mHandler.editGroup(name);
				break;
			}
			case R.id.enable: {
				Log.v(TAG, "ENABLE selected");
				mHandler.enableGroup(name);
				break;
			}
			case R.id.disable: {
				Log.v(TAG, "DISABLE selected");
				mHandler.disableGroup(name);
				break;
			}
			case R.id.delete: {
				Log.v(TAG, "DELETE selected");
				mHandler.removeGroup(name);
				break;
			}
			default:
				Log.v(TAG, "Unkown menu item value");
				return super.onContextItemSelected(item);
			}
		}else {
			Log.e(TAG, "Invalid/null name provided to context menu");
		}
		return true;
	}

	/////////////////////////////////////////////////////////////////////////////////
	// Activity Handling
	/////////////////////////////////////////////////////////////////////////////////

	//  this method is called when the "Edit Group" activity returns
	@Override  
	public void onActivityResult(int reqCode, int resultCode, Intent pIntent) {  
		super.onActivityResult(reqCode, resultCode, pIntent);  

		Log.d(TAG, "Processing group definition");
		// check the return intent, if group specified then add to list
		if (pIntent != null){
			String group = pIntent.getStringExtra(GroupDescriptor.GroupFields.NAME);
			if ((group!=null) && (group.length()>0)){
				mHandler.addGroup(group);
			} else {
				Log.v(TAG, "No group specified");
			}
		} else {
			Log.w(TAG, "EditGroup Activity returned null intent");
		}
		mHandler.displayActiveGroups();
	} //onActivityResult



	/////////////////////////////////////////////////////////////////////////////////
	// Groups Callback Handling
	/////////////////////////////////////////////////////////////////////////////////

	GroupsListener mGroupsListener = new GroupsListener(){

		@Override
		public void onGroupInvitation(String group, String originator) {
			Log.v(TAG, "onGroupInvitation("+group+", "+originator+")");
			mHandler.displayActiveGroups();
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
			mHandler.displayActiveGroups();
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
		public void onGroupMemberJoined(String group, String id) {
			String name = ProfileCache.getProfile(id).getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
			mHandler.showMessage(id + "joined group: "+group);
		}

		@Override
		public void onGroupMemberLeft(String group, String id) {
			String name = ProfileCache.getProfile(id).getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
			mHandler.showMessage(id + "left group: "+group);
		}

		@Override
		public void onTestResult(String results) {
			Log.v(TAG, "onTestResult()");
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
		private static final int UI_INIT                    =   1; // Initialisation
		private static final int UI_STOP                    =   2; // Shutdown
		private static final int UI_SHOW_MESSAGE            =   3; // Display message
		private static final int UI_DISPLAY_ACTIVE_GROUPS   =   4; // Show the list of active groups
		private static final int UI_DISPLAY_INACTIVE_GROUPS =   5; // Show the list of inactive groups
		private static final int UI_EDIT_GROUP              =   6; // Define/edit a group (UI portion)
		private static final int UI_ADD_GROUP               =   7; // Add a new group
		private static final int UI_REMOVE_GROUP            =   8; // Remove  group
		private static final int UI_ENABLE_GROUP            =   9; // Enable a (disabled) group
		private static final int UI_DISABLE_GROUP           =  10; // Disable a group (stop processing)
		private static final int UI_JOIN_GROUP              =  11; // Join an existing group
		private static final int UI_LEAVE_GROUP             =  12; // Leave a joined group

		/*
		 * TODO:
		 * - Handle Invititation Request
		 * - Handle Invitation Rejected
		 * - Display available groups
		 */

		// Accessor Methods - these just create the appropriate message and send it to the handler thread
		//                    This is one to get processing off the UI thread

		// Initialisation
		private void init(){
			sendEmptyMessage(UI_INIT); 
		}

		// Shutdown
		private void shutdown(){
			sendEmptyMessage(UI_STOP); 
		}

		// Display message
		private void showMessage(String message){
			Message msg = obtainMessage(UI_SHOW_MESSAGE);
			Bundle data = new Bundle();
			data.putString("message", message);
			msg.setData(data);
			sendMessage(msg);	
		}

		// Show the list of active groups
		private void displayActiveGroups(){
			sendEmptyMessage(UI_DISPLAY_ACTIVE_GROUPS); 
		}

		// Show the list of inactive groups
		private void displayInactiveGroups(){
			sendEmptyMessage(UI_DISPLAY_INACTIVE_GROUPS); 
		}

		// Edit/Define a Group definition
		public void editGroup(String group){
			Message msg = obtainMessage(UI_EDIT_GROUP);
			Bundle data = new Bundle();
			data.putString("group", group);
			msg.setData(data);
			sendMessage(msg);	
		}

		// Add a new group
		public void addGroup(String group){
			Message msg = obtainMessage(UI_ADD_GROUP);
			Bundle data = new Bundle();
			data.putString("group", group);
			msg.setData(data);
			sendMessage(msg);	
		}

		// remove an existing group
		public void removeGroup(String group){
			Message msg = obtainMessage(UI_REMOVE_GROUP);
			Bundle data = new Bundle();
			data.putString("group", group);
			msg.setData(data);
			sendMessage(msg);	
		}

		// enable a (disabled) group
		public void enableGroup(String group){
			Message msg = obtainMessage(UI_ENABLE_GROUP);
			Bundle data = new Bundle();
			data.putString("group", group);
			msg.setData(data);
			sendMessage(msg);	
		}

		// Disable an enabled group
		public void disableGroup(String group){
			Message msg = obtainMessage(UI_DISABLE_GROUP);
			Bundle data = new Bundle();
			data.putString("group", group);
			msg.setData(data);
			sendMessage(msg);	
		}

		// Attempt to join a (private) group
		public void joinGroup(String group){
			Message msg = obtainMessage(UI_JOIN_GROUP);
			Bundle data = new Bundle();
			data.putString("group", group);
			msg.setData(data);
			sendMessage(msg);	
		}

		// Leave a (private) group
		public void leaveGroup(String group){
			Message msg = obtainMessage(UI_LEAVE_GROUP);
			Bundle data = new Bundle();
			data.putString("group", group);
			msg.setData(data);
			sendMessage(msg);	
		}


		@Override
		public void handleMessage(Message msg) {

			// variables used a lot so, just declare once
			String group; 
			Bundle data = msg.getData();

			switch (msg.what) {

			case UI_INIT: {
				try {
					doInit();
				} catch (Exception e) {
					Log.e(TAG, "UI_INIT: "+e.toString());
				}
				break;
			}
			case UI_STOP: {
				try {
					doStop();
				} catch (Exception e) {
					Log.e(TAG, "UI_STOP: "+e.toString());
				}
				break;
			}
			case UI_SHOW_MESSAGE: {
				doShowMessage(data.getString("message"));
				break;
			}
			case UI_DISPLAY_ACTIVE_GROUPS: {
				try {
					doDisplayActiveGroups();
				} catch (Exception e) {
					Log.e(TAG, "UI_DISPLAY_ACTIVE_GROUPS: "+e.toString());
				}
				break;
			}
			case UI_DISPLAY_INACTIVE_GROUPS: {
				try {
					doDisplayInactiveGroups();
				} catch (Exception e) {
					Log.e(TAG, "UI_DISPLAY_INACTIVE_GROUPS: "+e.toString());
				}
				break;
			}
			case UI_EDIT_GROUP: {
				group = data.getString("group");
				try {
					doEditGroup(group);
				} catch (Exception e) {
					Log.e(TAG, "UI_EDIT_GROUP("+group+"): "+e.toString());
				}
				break;
			}
			case UI_ADD_GROUP: {
				group = data.getString("group");
				try {
					doAddGroup(group);
				} catch (Exception e) {
					Log.e(TAG, "UI_ADD_GROUP("+group+"): "+e.toString());
				}
				break;
			}
			case UI_REMOVE_GROUP: {
				group = data.getString("group");
				try {
					doRemoveGroup(group);
				} catch (Exception e) {
					Log.e(TAG, "UI_REMOVE_GROUP("+group+"): "+e.toString());
				}
				break;
			}
			case UI_ENABLE_GROUP: {
				group = data.getString("group");
				try {
					doEnableGroup(group);
				} catch (Exception e) {
					Log.e(TAG, "UI_ENABLE_GROUP("+group+"): "+e.toString());
				}
				break;
			}
			case UI_DISABLE_GROUP: {
				group = data.getString("group");
				try {
					doDisableGroup(group);
				} catch (Exception e) {
					Log.e(TAG, "UI_DISABLE_GROUP("+group+"): "+e.toString());
				}
				break;
			}
			case UI_JOIN_GROUP: {
				group = data.getString("group");
				try {
					doJoinGroup(group);
				} catch (Exception e) {
					Log.e(TAG, "UI_JOIN_GROUP("+group+"): "+e.toString());
				}
				break;
			}
			case UI_LEAVE_GROUP: {
				group = data.getString("group");
				try {
					doLeaveGroup(group);
				} catch (Exception e) {
					Log.e(TAG, "UI_LEAVE_GROUP("+group+"): "+e.toString());
				}
				break;
			}
			default: {
				Toast.makeText(mContext, "ERR: Unkown UI command", Toast.LENGTH_LONG).show();
				break;
			}
			}//switch
		}

		//
		// The specific handler routines for each operation supported
		//

		// Initialisation
		private void doInit(){

			// Get connection to Groups service
			mGroupsAPI = new GroupsAPI();
			mGroupsAPI.registerListener(mGroupsListener);

			// TEMP: replace with call to service to get the lists
			// load the group list from the Cache
			GroupCache.init();
			updateLists();
		}

		// Shutdown
		private void doStop(){
		}

		// Display message
		private void doShowMessage(String message){
			Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
			Log.d(TAG, "Displaying: "+message);
		}

		// Show the list of active groups
		private void doDisplayActiveGroups(){
			updateLists();
		}

		// Show the list of inactive groups
		private void doDisplayInactiveGroups(){
			updateLists();
		}

		// Edit/Define a Group definition
		public void doEditGroup(String group){
			launchEditGroup(group);
		}

		// Add a new group
		public void doAddGroup(String group){
			mGroupsAPI.addGroup(group, GroupCache.retrieveGroupDetails(group));
			updateLists();
		}

		// remove an existing group
		public void doRemoveGroup(String group){
			mGroupsAPI.removeGroup(group);
			updateLists();
		}

		// enable a (disabled) group
		public void doEnableGroup(final String group){
			if(getActivity() != null) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (mInactiveGroupAdapter.contains(group)){
							mGroupsAPI.enableGroup(group);
							mInactiveGroupAdapter.remove(group);
							mActiveGroupAdapter.add(group);
						} else {
							doShowMessage("Invalid: Group is not disabled");
						}
					}
				});
			}
		}

		// Disable an enabled group
		public void doDisableGroup(final String group){
			try{
				if(getActivity() != null) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (mActiveGroupAdapter.contains(group)){
								mGroupsAPI.disableGroup(group);
								mActiveGroupAdapter.remove(group);
								mInactiveGroupAdapter.add(group);
							} else {
								doShowMessage("Invalid: Group is not enabled");
							}
						}
					});
				}
			} catch (Exception e){
				e.printStackTrace();
			}
		}

		// Attempt to join a (private) group
		public void doJoinGroup(String group){
		}

		// Leave a (private) group
		public void doLeaveGroup(String group){
		}

		private void updateLists(){
			if (GroupCache.isGroupListPresent()){
				if(getActivity() != null) {
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Log.d(TAG, "Loading Group list");
							mActiveGroupDescriptor = GroupCache.retrieveGroupList();
			
							String group;
							mActiveGroupAdapter.clear();
							mInactiveGroupAdapter.clear();
							for (int i=0; i<mActiveGroupDescriptor.size(); i++){
								group = mActiveGroupDescriptor.get(i);
								if (mGroupsAPI.isGroupActive(group)){
									mActiveGroupAdapter.add(group);
								} else {
									mInactiveGroupAdapter.add(group);
								}
							}
						}			
					});
				}
			}
		}

	}//UIhandler


	private void launchEditGroup(String group){
		Intent myIntent = new Intent();
		myIntent.setAction(AppConstants.INTENT_PREFIX+".EDITGROUP");
		myIntent.putExtra("group", group);
		try {
			Log.d(TAG, "Starting ADDGROUP Activity");
			startActivityForResult(myIntent, 0);
		} catch (Throwable t){
			mHandler.showMessage(TAG+" Error starting ADDGROUP Activity");
		}

	}
} // end of Activity
