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

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.*;
import org.alljoyn.devmodules.callbacks.CallbackInterface;

/**
* Callback interface definition from background group to app context
*/
@BusInterface(name = GroupsCallbackInterface.SERVICE_NAME)
public interface GroupsCallbackInterface extends CallbackInterface {
	public static final String SERVICE_NAME = "org.alljoyn.devmodules.api.groups";
	public static final String OBJECT_PATH = "/groupsCallbackObject";
	

	/**
	* Notification that we have received an invitation to join a group
	* @param group The name of the group
	* @param originator The ID of the person originating the request (can be used to look up Profile etc.)
	*/
	@BusSignal
	public void onGroupInvitation (String group, String originator) throws BusException;
	
	
	/**
	* Notification that a user has accepted the invitation to join a group
	* @param group The name of the group
	* @param id The ID of the person accepting the request (can be used to look up Profile etc.)
	*/
	@BusSignal
	public void onGroupInvitationAccepted (String group, String id) throws BusException;
	
	/**
	* Notification that a user has rejected the invitation to join a group
	* @param group The name of the group
	* @param originator The ID of the person rejecting the request (can be used to look up Profile etc.)
	*/
	@BusSignal
	public void onGroupInvitationRejected (String group, String id) throws BusException;
	
	/**
	* Notification that we an issued invitation has timed out
	* @param group The name of the group
	*/
	@BusSignal
	public void onGroupInvitationTimeout (String group) throws BusException;
	
	/*
	* Notification that a new group has been added
	* @param group The name of the group
	*/
	@BusSignal
	public void onGroupAdded (String group) throws BusException;
	
	/*
	* Notification that a group has been removed
	* @param group The name of the group
	*/
	@BusSignal
	public void onGroupRemoved (String group) throws BusException;
	
	/**
	* Notification that a group has become Active (i.e. someone joined the group)
	* @param group The name of the group
	*/
	@BusSignal
	public void onGroupActive (String group) throws BusException;
	
	/**
	* Notification that a group has become empty, i.e. all other users have left
	* @param group The name of the group
	*/
	@BusSignal
	public void onGroupInactive (String group) throws BusException;
	
	/**
	* Notification that a group has been enabled (i.e. available for use)
	* @param group The name of the group
	*/
	@BusSignal
	public void onGroupEnabled (String group) throws BusException;
	
	/**
	* Notification that a group has been disabled (i.e. still defined but not used)
	* @param group The name of the group
	*/
	@BusSignal
	public void onGroupDisabled (String group) throws BusException;
	
	/**
	* Notification that someone has joined an active group
	* @param group The name of the group
	* @param originator The ID of the person that joined (can be used to look up Profile etc.)
	*/
	@BusSignal
	public void onGroupMemberJoined(String group, String id) throws BusException;
	
	/**
	* Notification that someone has left an active group
	* @param group The name of the group
	* @param originator The ID of the person that left (can be used to look up Profile etc.)
	*/
	@BusSignal
	public void onGroupMemberLeft (String group, String id) throws BusException;
	
	/**
	 * Signal indicating that Groups Test has completed
	 * @param results HTML-formatted string with results of test
	 * @throws BusException
	 */
	@BusSignal
	public void onTestResult (String results) throws BusException;
 }
