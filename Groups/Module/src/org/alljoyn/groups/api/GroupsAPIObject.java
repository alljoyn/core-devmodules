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

package org.alljoyn.groups.api;

import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.devmodules.groups.GroupManager;
import org.alljoyn.devmodules.groups.GroupsImpl;
import org.alljoyn.storage.GroupCache;

import org.alljoyn.bus.*;
import org.alljoyn.bus.annotation.BusMethod;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class GroupsAPIObject implements GroupsAPIInterface, BusObject {
	public static final String OBJECT_PATH = "groups";

	private static final String TAG = "GroupsAPIObject" ;

	// Group manager instance
	private static GroupManager mGroupManager = GroupManager.getInstance();

	// Handler for long transactions or asynchronous operations
	private HandlerThread handlerThread = new HandlerThread(OBJECT_PATH);
	{handlerThread.start();}
	private ConnectorHandler handler = new ConnectorHandler(handlerThread.getLooper());


	// Template for methods that need to go off-device
	//	public void GroupsMETHOD(int transactionId, String data)
	//			throws BusException {
	//		try{
	//			Log.i(TAG, "onKeyDown!!! peer:"+peer);
	//			Message msg = handler.obtainMessage(ON_KEY_DOWN);
	//			Bundle data = new Bundle();
	//			data.putInt("transactionId", transactionId);
	//			data.putString("data", data);
	//			msg.setData(data);
	//			handler.sendMessage(msg);
	//		}catch(Exception e) {
	//			e.printStackTrace();
	//		}
	//	}

	GroupsImpl mGroupsImpl = GroupsImpl.getInstance();

	/**
	 * Set up internal variables
	 */
	private void init(){
		if (mGroupsImpl==null) mGroupsImpl = GroupsImpl.getInstance();
	}

	// Use this handler for transactions that need to go off-device
	private class ConnectorHandler extends Handler
	{
		public ConnectorHandler(Looper loop) {
			super(loop);
		}
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			if(mGroupsImpl == null || data == null) {
				Log.i(TAG,"mGroupsImpl or data is null!");
				return;
			}
			//switch(msg.what) {
			//case ON_EVENT:
			//Log.i(TAG,"placing call to the mGroupsImpl");
			//mGroupsImpl.<method>(data.getInt("transactionId"), data.getString("data"));
			//break;
			//}
		}
	}

	@Override
	public String[] GetGroupList() throws BusException {
		return GroupManager.getGroupList();
	}

	@Override
	public String GetGroupListDescriptor () throws BusException {
		return GroupManager.getGroupListDescriptor().toString();
	}

	@Override
	public String GetGroupDescriptor(String group) throws BusException {
		return GroupManager.getGroupDescriptor(group).toString();
	}

	@Override
	public void AddGroup(String group, String descriptor) throws BusException {
		try {
			init();
			// Save the group definition
			GroupDescriptor gd = new GroupDescriptor();
			gd.setString(descriptor);

			GroupManager.addGroup(group, gd);

			// Start processing
			if(mGroupsImpl != null) {
				if (gd.isEnabled()){
					mGroupsImpl.enableGroup(group);
				}
			} else {
				Log.e(TAG,"Removeroup() mGroupsImpl is null!");
			}

			// Issue signal to notify interested apps
			GroupsAPIImpl.getInstance().getCallbackInterface().onGroupAdded(group);

		} catch (Exception e){
			Log.e(TAG, "AddGroup() error: "+e.toString());
		}
	}

	@Override
	public void RemoveGroup(String group) throws BusException {
		try {
			Log.v(TAG, "RemoveGroup("+group+")");
			// make sure it exists first
			if (GroupCache.isGroupDetailsPresent(group)){

				// Stop any activity
				if(mGroupsImpl != null) {
					mGroupsImpl.disableGroup(group);
				} else {
					Log.e(TAG,"Removeroup() mGroupsImpl is null!");
				}
				// remove the group definition
				GroupManager.removeGroup(group);

				// Issue signal to notify interested apps
				GroupsAPIImpl.getInstance().getCallbackInterface().onGroupRemoved(group);
			} else {
				Log.w(TAG, "RemoveGroup() - group not found: "+group);
				Log.v(TAG, "Group List: "+GroupManager.getGroupDescriptor(group).toString());
			}

		} catch (Exception e){
			Log.e(TAG, "RemoveGroup() error: "+e.toString());
		}
	}

	@Override
	public void SaveGroup(String group) throws BusException {
		try {
			// ?? nothing to do. Remove this?
		} catch (Exception e){
			Log.e(TAG, "SaveGroup() error: "+e.toString());
		}
	}

	@Override
	public void DeleteGroup(String group) throws BusException {
		try {
			// Is this any different than RemoveGroup?!
			RemoveGroup (group);
		} catch (Exception e){
			Log.e(TAG, "DeleteGroup() error: "+e.toString());
		}
	}

	@Override
	public void EnableGroup(String group) throws BusException {
		try {
			init();
			// Tell the Service controller to activate the group
			GroupsImpl mGroupsImpl = GroupsImpl.getInstance();
			if(mGroupsImpl != null) {
				mGroupsImpl.enableGroup(group);
			} else {
				Log.e(TAG,"mGroupsImpl is null!");
				return;
			}
		} catch (Exception e){
			Log.e(TAG, "EnableGroup() error: "+e.toString());
		}
	}

	@Override
	public void DisableGroup(String group) throws BusException {
		try {
			init();
			// Tell the Service controller to deactivate the group
			GroupsImpl mGroupsImpl = GroupsImpl.getInstance();
			if(mGroupsImpl != null) {
				mGroupsImpl.disableGroup(group);
			} else {
				Log.e(TAG,"mGroupsImpl is null!");
				return;
			}
		} catch (Exception e){
			Log.e(TAG, "DisableGroup() error: "+e.toString());
		}
	}

	@Override
	public boolean IsGroupActive(String group) throws BusException {
		return GroupManager.isActive(group);
	}

	@Override
	public boolean IsGroupDefined(String group) throws BusException {
		return GroupCache.isGroupDetailsPresent(group);
	}

	@Override
	public void AddMembers(String group, String[] members) throws BusException {
		GroupManager.addMembers(group, members);
	}

	@Override
	public void RemoveMembers(String group, String[] members) throws BusException {
		GroupManager.removeMembers(group, members);
	}

	@Override
	public String[] GetAllMembers(String group) throws BusException {
		return GroupManager.getGroupMembers(group);
	}

	@Override
	public String[] GetActiveMembers(String group) throws BusException {
		return GroupManager.getActiveGroupMembers(group);
	}

	@Override
	public String[] GetInactiveMembers(String group) throws BusException {
		return GroupManager.getInactiveGroupMembers(group);
	}

	@Override
	public String[] GetRemovedMembers(String group) throws BusException {
		try {
			//TODO
		} catch (Exception e){
			Log.e(TAG, "GetRemovedMembers() error: "+e.toString());
		}
		return null;
	}

	@Override
	public boolean IsMemberActive(String group, String member) throws BusException {
		return GroupManager.isMemberActive(group, member) ;
	}

	@Override
	public void InviteMembers(String group, String[] members) throws BusException {
		try {
			//TODO
		} catch (Exception e){
			Log.e(TAG, "InviteMembers() error: "+e.toString());
		}
	}

	@Override
	public void AcceptInvitation(String group) throws BusException {
		try {
			//TODO
		} catch (Exception e){
			Log.e(TAG, "AcceptInvitation() error: "+e.toString());
		}
	}

	@Override
	public void RejectInvitation(String group) throws BusException {
		try {
			//TODO
		} catch (Exception e){
			Log.e(TAG, "RejectInvitation() error: "+e.toString());
		}
	}

	@Override
	public void IgnoreInvitation(String group) throws BusException {
		try {
			//TODO
		} catch (Exception e){
			Log.e(TAG, "IgnoreInvitation() error: "+e.toString());
		}
	}


	@Override
	public void RunGroupsTest() throws BusException {
		try{
			init();
			mGroupsImpl.runGroupsTest();
		} catch (Exception e){
			Log.e(TAG, "RunGroupsTest() exception: "+e.toString());
		}
	}

	@Override
	public boolean IsGroupEnabled(String group) throws BusException {
		GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
		return gd.isEnabled();
	}	
}
