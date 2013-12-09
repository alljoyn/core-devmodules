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
package org.alljoyn.devmodules.api.groups;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.devmodules.APICoreImpl;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerAPIInterface;
import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.GroupCache;
import org.alljoyn.storage.ProfileCache;

import android.util.Log;

// These are the APIs available to applications for the "Groups" service
// This is designed for multiple clients, so you need to instantiate a GroupsAPI object before using it
public class GroupsAPI {


	private static int transactionId = 0;
	public static final String TAG = "GroupsAPI";
	
	// the callback object
	private static GroupsCallbackObject mGroupsCallbackObject = null;

	// the interface to the background service
	private GroupsAPIInterface groupsInterface = null;

	/**
	 * Constructor for the GroupsAPI
	 */
	public GroupsAPI(){
		
	}
	
	
	// sets up the interface to the background service
	private void setupInterface() {
		boolean ready = APICoreImpl.getInstance().isReady;
		if (ready){
			
			if (mGroupsCallbackObject == null) mGroupsCallbackObject = new GroupsCallbackObject();

			if(groupsInterface == null) {
				try {
					groupsInterface = 	APICoreImpl.getInstance().getProxyBusObject("groups",
							new Class[] {GroupsAPIInterface.class}).getInterface(GroupsAPIInterface.class);
					if (groupsInterface!=null){
						Log.d(TAG, "Established interface to Groups service");
					} else {
						Log.w(TAG, "Null Interface to Groups service");
					}
				} catch (Exception e){
					Log.e(TAG, "setupInterface(): Error getting groupsInterface");
				}
			} 
		} else {
			Log.w(TAG, "setupInterface(): Connector core not ready!");
		}

	}



	// Service-specific interfaces:

	/**
	 * register callback listener
	 * @param listener An object with interface GroupsListener for handling Groups callbacks
	 */
	public void registerListener(GroupsListener listener) {
		setupInterface();
		mGroupsCallbackObject.registerListener(listener);
	}

	/**
	 *  Get the list of defined groups
	 * @return Array of groups names
	 */
	public String[] getGroupList() {
		setupInterface();
		String[] glist = new String[0];
		try {
			// List is returned in JSON string format
			glist = groupsInterface.GetGroupList();
		} catch (BusException e) {
			Log.e(TAG, "Error calling GetGroupList: "+e.toString());
			glist = new String[0];
		}
		return glist;
	}

	/**
	 *  Get the list of defined groups
	 * @return the list of groups in GroupListdescriptor format
	 */
	public GroupListDescriptor getGroupListDescriptor() {
		setupInterface();
		GroupListDescriptor gld = new GroupListDescriptor();
		try {
			// List is returned in JSON string format
			String list = groupsInterface.GetGroupListDescriptor();
			gld.setString(list);
		} catch (BusException e) {
			Log.e(TAG, "Error calling GetGroupList: "+e.toString());
		}
		return gld;
	}


	/**
	 * Get the GroupDescriptor object for the named group
	 * @param group The name of the group
	 */
	public GroupDescriptor getGroupDescriptor(String group) {
		setupInterface();
		GroupDescriptor gd = new GroupDescriptor();
		try {
			String descr = groupsInterface.GetGroupDescriptor(group);
			gd.setString(descr);
		} catch (BusException e) {
			Log.e(TAG, "Error calling GetGroupDescriptor: "+e.toString());
		}
		return gd;
	}

	/**
	 * Add a new group
	 * @param group The name of the group
	 * @param gd The GroupDescriptor that describes the group parameters
	 */
	public void addGroup(String group, GroupDescriptor gd) {
		setupInterface();
		try {
			String descr = gd.toString();
			groupsInterface.AddGroup(group, descr);
		} catch (BusException e) {
			Log.e(TAG, "Error calling AddGroup: "+e.toString());
		}
	}


	/**
	 * Remove a group (does not delete definition)
	 * @param group The name of the group
	 */
	public void removeGroup(String group) {
		setupInterface();
		try {
			groupsInterface.RemoveGroup(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling RemoveGroup: "+e.toString());
		}
	}


	/**
	 *  Save a group to persistent storage (must already be defined)
	 * @param group The name of the group
	 */
	public void saveGroup(String group) {
		setupInterface();
		try {
			groupsInterface.SaveGroup(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling SaveGroup: "+e.toString());
		}
	}


	/**
	 *  Delete a group from persistent storage
	 * @param group The name of the group
	 */
	public void deleteGroup(String group) {
		setupInterface();
		try {
			groupsInterface.DeleteGroup(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling DeleteGroup: "+e.toString());
		}
	}


	/**
	 *  Enable a (dormant) group. Must already be defined
	 * @param group The name of the group
	 */
	public void enableGroup(String group) {
		setupInterface();
		try {
			groupsInterface.EnableGroup(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling EnableGroup: "+e.toString());
		}
	}


	/**
	 *  Disable a group
	 * @param group The name of the group
	 */
	public void disableGroup(String group) {
		setupInterface();
		try {
			groupsInterface.DisableGroup(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling DisableGroup: "+e.toString());
		}
	}


	/**
	 *  Check whether a group is currently enabled
	 * @param group The name of the group
	 * @return
	 */
	public boolean isGroupEnabled(String group) {
		setupInterface();
		boolean result = false;
		try {
			result = groupsInterface.IsGroupEnabled(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling IsGroupEnabled: "+e.toString());
		}
		return result;
	}


	/**
	 *  Check whether a group is currently active
	 * @param group The name of the group
	 * @return
	 */
	public boolean isGroupActive(String group) {
		setupInterface();
		boolean result = false;
		try {
			result = groupsInterface.IsGroupActive(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling IsGroupActive: "+e.toString());
		}
		return result;
	}


	/**
	 *  Check whether a group has already been defined
	 * @param group The name of the group
	 * @return
	 */
	public boolean isGroupDefined(String group) {
		setupInterface();
		boolean result = false;
		try {
			result = groupsInterface.IsGroupDefined(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling IsGroupDefined: "+e.toString());
		}
		return result;
	}


	/**
	 * Add members to an existing group
	 * @param group The name of the group
	 * @param members The list of members (IDs) to add
	 */
	public void addMembers(String group, String[] members) {
		setupInterface();
		try {
			groupsInterface.AddMembers(group, members);
		} catch (BusException e) {
			Log.e(TAG, "Error calling AddMembers: "+e.toString());
		}
	}


	/**
	 * Remove members from an existing group
	 * @param group The name of the group
	 * @param members The list of members (IDs) to remove
	 */
	public void removeMembers(String group, String[] members) {
		setupInterface();
		try {
			groupsInterface.RemoveMembers(group, members);
		} catch (BusException e) {
			Log.e(TAG, "Error calling RemoveMembers: "+e.toString());
		}
	}



	/**
	 *  Get all the members (active and inactive) of a group
	 * @param group The name of the group
	 * @return
	 */
	public String[] getAllMembers(String group) {
		setupInterface();
		String[] mlist = new String[0];
		try {
			mlist = groupsInterface.GetAllMembers(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling GetAllMembers: "+e.toString());
		}
		return mlist;
	}


	/**
	 *  Get the active members of a group
	 * @param group The name of the group
	 * @return
	 */
	public String[] getActiveMembers(String group) {
		setupInterface();
		String[] mlist = new String[0];
		try {
			mlist = groupsInterface.GetActiveMembers(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling GetActiveMembers: "+e.toString());
		}
		return mlist;
	}

	/**
	 *  Get the inactive members of a group
	 * @param group The name of the group
	 * @return
	 */
	public String[] getInactiveMembers(String group) {
		setupInterface();
		String[] mlist = new String[0];
		try {
			mlist = groupsInterface.GetInactiveMembers(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling GetInactiveMembers: "+e.toString());
		}
		return mlist;
	}

	/**
	 *  Get the members that have been removed (usually to prevent re-addition)
	 * @param group The name of the group
	 * @return
	 */
	public String[] getRemovedMembers(String group) {
		setupInterface();
		String[] mlist = new String[0];
		try {
			mlist = groupsInterface.GetRemovedMembers(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling GetRemovedMembers: "+e.toString());
		}
		return mlist;
	}


	/**
	 * Check to see whether a member is already active in a group
	 * @param group The name of the group
	 * @param member The member (IDs) to check
	 */
	public boolean isMemberActive(String group, String member) {
		setupInterface();
		boolean result = false;
		try {
			result = groupsInterface.IsMemberActive(group, member);
		} catch (BusException e) {
			Log.e(TAG, "Error calling IsMemberActive: "+e.toString());
		}
		return result;
	}


	/**
	 * Invite a list of members to join a group
	 * @param group The name of the group
	 * @param members The list of members (IDs) to invite
	 */
	public void inviteMembers(String group, String[] members) {
		setupInterface();
		try {
			groupsInterface.InviteMembers(group, members);
		} catch (BusException e) {
			Log.e(TAG, "Error calling InviteMembers: "+e.toString());
		}
	}


	/**
	 *  Accept a received invitation to join a group
	 * @param group The name of the group
	 */
	public void acceptInvitation(String group) {
		setupInterface();
		try {
			groupsInterface.AcceptInvitation(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling AcceptInvitation: "+e.toString());
		}
	}


	/**
	 *  Reject a received invitation to join a group
	 * @param group The name of the group
	 */
	public void rejectInvitation(String group) {
		setupInterface();
		try {
			groupsInterface.RejectInvitation(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling RejectInvitation: "+e.toString());
		}
	}


	/**
	 *  Ignore a received invitation to join a group
	 * @param group The name of the group
	 */
	public void ignoreInvitation(String group) {
		setupInterface();
		try {
			groupsInterface.IgnoreInvitation(group);
		} catch (BusException e) {
			Log.e(TAG, "Error calling IgnoreInvitation: "+e.toString());
		}
	}


	/**
	 * Runs verification tests on groups
	 * Result returned via callback interface
	 */
	public void runGroupsTest(){
		try{
			groupsInterface.RunGroupsTest();
		} catch (Exception e){
			Log.e(TAG, "runGroupsTest() Error: "+e.toString());
		}
	}
}
