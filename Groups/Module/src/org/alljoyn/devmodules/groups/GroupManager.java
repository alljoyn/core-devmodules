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
package org.alljoyn.devmodules.groups;

import java.util.ArrayList;
import java.util.HashMap;

import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.GroupCache;

import android.util.Log;



public class GroupManager {

	private static final String TAG = "GroupManager";

	private static GroupManager _groupMgr; // the singleton version

	// Data Structures for holding info on groups
	// NOTE: for now, we assume that if a user is active, they are active in all relevant groups
	private static ArrayList<String> mActiveGroups;
	private static ArrayList<String> mInactiveGroups;
	private static HashMap<String,Integer> mActiveUsers; // int is a reference count of groups
	private static ArrayList<String> mInactiveUsers;


	/////////////////////////////////////////////////////
	// Singleton class management stuff
	/////////////////////////////////////////////////////

	private GroupManager() { 
		// no implementation, just private to control usage
	} 


	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	private static void init(){
		if (_groupMgr == null) {

			// create singleton object
			_groupMgr = new GroupManager();

			// initialise arrays
			mActiveGroups    = new ArrayList<String>();
			mInactiveGroups  = new ArrayList<String>();
			mActiveUsers     = new HashMap<String,Integer>();
			mInactiveUsers   = new ArrayList<String>();
			mActiveGroups.clear();
			mInactiveGroups.clear();
			mActiveUsers.clear();
			mInactiveUsers.clear();

			// load initial data from cache
			GroupListDescriptor gld = new GroupListDescriptor();
			for (int i=0; i<gld.size(); i++){
				mInactiveGroups.add(gld.get(i));
			}
		}
	}

	/**
	 *  method to return reference to internal data
	 * @return the singleton instance
	 */
	public static synchronized GroupManager getInstance() {
		init();
		return _groupMgr;
	}

	/////////////////////////////////////////////////////
	// the actual Group Management functionality
	/////////////////////////////////////////////////////

	/**
	 *  Return the list of known groups, in GroupListDescriptor form
	 * @return GroupListDescriptor form of group list
	 */
	public static GroupListDescriptor getGroupListDescriptor(){
		init();
		GroupListDescriptor gld = new GroupListDescriptor();
		try{
			// load the group list from the Cache
			if (GroupCache.isGroupListPresent()){
				Log.d(TAG, "Loading Group list");
				gld = GroupCache.retrieveGroupList();
			}
		} catch (Exception e){
			Log.e(TAG, "Error in getGroupList: "+e.toString());
		}
		return gld;
	}


	/**
	 *  Return the list of known groups, in String[] form
	 * @return String array of group names
	 */
	public static String[] getGroupList(){
		init();
		String[] glist = new String[0];
		GroupListDescriptor gld = new GroupListDescriptor();
		try{
			// load the group list from the Cache
			if (GroupCache.isGroupListPresent()){
				Log.d(TAG, "Loading Group list");
				gld = GroupCache.retrieveGroupList();
				glist = gld.get();
			}
		} catch (Exception e){
			Log.e(TAG, "Error in getGroupList: "+e.toString());
		}
		return glist;
	}


	/**
	 *  Returns the list of (enabled) public groups
	 * @return Array of names of (enabled) public groups
	 */
	public static String[] getPublicGroupList(){
		ArrayList<String> list;
		GroupDescriptor gd;
		init();
		
		// get the full group list
		String [] glist = getGroupList();
		
		// if private, copy the name to the public list
		list = new ArrayList<String>();
		list.clear();
		for (int i=0; i<glist.length; i++){
			gd = GroupCache.retrieveGroupDetails(glist[i]);
			if ((!gd.isPrivate()) && (gd.isEnabled())){
				list.add(glist[i]);
			}
		}	
		return list.toArray(new String[list.size()]);
	}


	/**
	 *  Returns the list of (enabled) private groups
	 * @return Array of names of (enabled) private groups
	 */
	public static String[] getPrivateGroupList(){
		ArrayList<String> list;
		GroupDescriptor gd;
		init();
		
		// get the full group list
		String [] glist = getGroupList();
		
		// if private, copy the name to the private list
		list = new ArrayList<String>();
		list.clear();
		for (int i=0; i<glist.length; i++){
			gd = GroupCache.retrieveGroupDetails(glist[i]);
			if ((gd.isPrivate()) && (gd.isEnabled())){
				list.add(glist[i]);
			}
		}	
		return list.toArray(new String[list.size()]);
	}

	/**
	 *  Returns the list of active groups
	 * @return Array of names of active groups
	 */
	public static String[] getActiveGroupList(){
		String[] list;
		init();
		int count = mActiveGroups.size();
		if (count>0){
			list = mActiveGroups.toArray(new String[count]);
		} else {
			list = new String[0];
		}
		return list;
	}

	/**
	 *  Returns the list of inactive groups
	 * @return Array of names of (enabled but) inactive groups
	 */
	public static String[] getInactiveGroupList(){
		String[] list;
		init();
		int count = mInactiveGroups.size();
		if (count>0){
			list = mInactiveGroups.toArray(new String[count]);
		} else {
			list = new String[0];
		}
		return list;
	}

	/**
	 *  Checks whether group is defined or not
	 * @param group The name of the group to check
	 * @return true if already defined, false otherwise
	 */
	public static boolean isDefined (String group){
		init();
		return (GroupCache.isGroupDetailsPresent(group)) ? true : false ;
	}

	/**
	 *  Checks whether group is active or not
	 * @param group The name of the group to check
	 * @return
	 */
	public static boolean isActive (String group){
		init();
		return (mActiveGroups.contains(group)) ? true : false ;
	}

	/**
	 * Returns the list of all (active and inactive) users in a group
	 * @param group The name of the group
	 */
	public static String[] getGroupMembers(String group){
		String[] list= new String[0];
		init();
		try{
			if (GroupCache.isGroupDetailsPresent(group)){
				// TODO: cache information in memory instead of retrieving from file every time?
				//       Probably 'safer' to read every time though
				GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
				list = gd.getMembers();
			}
		} catch (Exception e){
			list = new String[0];
		}
		return list;
	}


	/**
	 * Returns the list of active users in a group
	 * @param group The name of the group
	 */
	public static String[] getActiveGroupMembers(String group){
		String[] list= new String[0];
		init();
		try{
			if (isActive(group)){
				ArrayList<String> userlist = new ArrayList<String>();
				String[] members = getGroupMembers(group);
				if (members.length>0){
					for (int i=0; i<members.length;i++){
						if (mActiveUsers.containsKey(members[i])){
							userlist.add(members[i]);
						}
					}
					list = userlist.toArray(new String[userlist.size()]);
				}
			}
		} catch (Exception e){
			list = new String[0];
		}
		return list;
	}


	/**
	 * Returns the list of inactive users in a group
	 * @param group The name of the group
	 */
	public static String[] getInactiveGroupMembers(String group){
		String[] list= new String[0];
		init();
		try{
			if (isActive(group)){
				ArrayList<String> userlist = new ArrayList<String>();
				String[] members = getGroupMembers(group);
				if (members.length>0){
					for (int i=0; i<members.length;i++){
						if (!mActiveUsers.containsKey(members[i])){
							userlist.add(members[i]);
						}
					}
					list = userlist.toArray(new String[userlist.size()]);
				}
			}
		} catch (Exception e){
			list = new String[0];
		}
		return list;
	}


	/*
	 * Get the group descriptor
	 * @param group The name of the group
	 */
	public static GroupDescriptor getGroupDescriptor (String group){
		init();
		GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
		return gd;
	}


	/*
	 * add a new group (will overwrite existing group)
	 * @param group The name of the group
	 * @param descriptor The GroupDescriptor that defines the group
	 */
	public static void addGroup (String group, GroupDescriptor descriptor){
		GroupCache.saveGroupDetails(group, descriptor);

		// Update the group list
		GroupListDescriptor gld = GroupCache.retrieveGroupList();
		gld.add(group);
		GroupCache.saveGroupList(gld);
	}


	/**
	 * Remove a group
	 * @param group The name of the group
	 */
	public static void removeGroup (String group){
		GroupCache.removeGroupDetails(group);

		// Update the group list
		GroupListDescriptor gld = GroupCache.retrieveGroupList();
		gld.remove(group);
		GroupCache.saveGroupList(gld);
		//TODO: delete descriptor file?
	}


	/**
	 * enable a group. Note that this is a persistent operation. 
	 * It does not initiate any further processing
	 * @param group The name of the group
	 */
	public static void enable (String group){
		init();
		if (GroupCache.isGroupDetailsPresent(group)){
			GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
			gd.setField(GroupDescriptor.GroupFields.TIME_MODIFIED, Utilities.getTimestamp());
			gd.enable();
			GroupCache.saveGroupDetails(group, gd);
		} else {
			Log.e(TAG, "Attempt to enable non-existent group ("+group+")");
		}
	}



	/**
	 * Disable a group. Note that this is a persistent operation. 
	 * It does not initiate any further processing
	 * @param group The name of the group
	 */
	public static void disable (String group){
		init();
		if (GroupCache.isGroupDetailsPresent(group)){
			GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
			gd.setField(GroupDescriptor.GroupFields.TIME_MODIFIED, Utilities.getTimestamp());
			gd.disable();
			GroupCache.saveGroupDetails(group, gd);
		} else {
			Log.e(TAG, "Attempt to disable non-existent group ("+group+")");
		}
	}

	/**
	 * flag a group as 'active'
	 * @param group The name of the group
	 */
	public static void setActive (String group){
		init();
		if (!mActiveGroups.contains(group)){
			mInactiveGroups.remove(group);
			mActiveGroups.add(group);
			Log.d(TAG, "Activated group: "+group);
		}
	}


	/*
	 * flag a group as 'inactive'
	 * @param group The name of the group
	 */
	public static void setInactive (String group){
		init();
		if (mActiveGroups.contains(group)){
			mActiveGroups.remove(group);
			mInactiveGroups.add(group);
		}
	}


	/**
	 * Add members to a group
	 * @param group The name of the group
	 * @param members The list of member IDs
	 */
	public static void addMembers (String group, String[] members){
		init();
		if ((members!=null) && (members.length>0)){
			if (GroupCache.isGroupDetailsPresent(group)){
				GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
				String id;
				for (int i=0; i<members.length; i++){
					id = members[i];
					if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
					gd.addMember(id);
					mInactiveUsers.add(id);
				}
				gd.setField(GroupDescriptor.GroupFields.TIME_MODIFIED, Utilities.getTimestamp());
				GroupCache.saveGroupDetails(group, gd);
			} else {
				Log.e(TAG, "Attempt to add members to non-existent group ("+group+")");
			}
		}
	}


	/**
	 * Remove members from a group
	 * @param group The name of the group
	 * @param members The list of member IDs
	 */
	public static void removeMembers (String group, String[] members){
		init();
		if ((members!=null) && (members.length>0)){
			if (GroupCache.isGroupDetailsPresent(group)){
				GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
				String id;
				for (int i=0; i<members.length; i++){
					id = members[i];
					if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
					gd.removeMember(id);
					// if not active in any other groups, move user to inactive
					if (mActiveGroups.contains(group)){
						if (mActiveUsers.containsKey(id)){
							if (mActiveUsers.get(id)==1){
								mActiveUsers.remove(id);
								mInactiveUsers.add(id);
							}
						}
					}
				}
				gd.setField(GroupDescriptor.GroupFields.TIME_MODIFIED, Utilities.getTimestamp());
				GroupCache.saveGroupDetails(group, gd);
			} else {
				Log.e(TAG, "Attempt to add members to non-existent group ("+group+")");
			}
		}
	}


	/**
	 * Flag a user as active
	 * @param group The name of the group
	 * @param member The ID of the user
	 */
	public static void setMemberActive (String group, String member){
		init();
		String id = member;
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);

		int count = 1;
		if (mActiveUsers.containsKey(id)){
			count = mActiveUsers.get(id)+1;
		} else {
			if (mInactiveUsers.contains(id)){
				mInactiveUsers.remove(id);
			}
		}
		mActiveUsers.put(id, count);
	}


	/**
	 * Flag a user as inactive
	 * @param group The name of the group
	 * @param member The ID of the user
	 */
	public static void setMemberInactive (String group, String member){
		init();
		String id = member;
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);

		int count;
		if (mActiveUsers.containsKey(id)){
			count = mActiveUsers.get(id)-1;
			if (count <=0){
				mActiveUsers.remove(id);
				mInactiveUsers.add(id);
			} else {
				mActiveUsers.put(id, count);
			}
		} else {
			mInactiveUsers.add(id);
		}
	}


	/**
	 * Check whether a user as active
	 * @param group The name of the group
	 * @param member The ID of the user
	 */
	public static boolean isMemberActive (String group, String member){
		init();
		String id = member;
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
		return (mActiveUsers.containsKey(id)) ? true : false;
	}


	/**
	 * Check whether a user is a member of a group
	 * @param group The name of the group
	 * @param member The ID of the user
	 */
	public static boolean isMember (String group, String member){
		init();
		boolean found;
		String id = member;
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
		String[] members = getGroupMembers(group);
		found = false;
		for (int i=0; i<members.length; i++){
			if (members[i].equals(id)){
				found = true;
				break;
			}
		}
		return found;
	}

	/**
	 * Check whether a group is private or not
	 * @param group The name of the group
	 * @return true if private, false if open
	 */
	public static boolean isPrivate (String group){
		init();
		GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
		return gd.isPrivate();
	}

} // GroupManager
