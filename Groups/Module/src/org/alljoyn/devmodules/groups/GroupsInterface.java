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

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;


@BusInterface (name = GroupsConstants.NAME_PREFIX)
public interface GroupsInterface {
	
//THIS IS THE SERVICE TO SERVICE (INTER-DEVICE) INTERFACE
	
	@BusMethod
	// Get the list of groups currently supported
	public String [] GetGroupList() throws BusException;
	
	@BusMethod
	// Get the list of groups currently supported (in GroupListDescriptor format)
	public String GetGroupListDescriptor() throws BusException;
	
	@BusMethod
	// Get the descriptor for the specified group
	public String GetGroupDescriptor (String group) throws BusException;
	
	@BusMethod
	// Get the port number for the specified group
	public short GetPortNumber (String group) throws BusException;
	
	@BusMethod
	//	Request activation of an already-defined group
	public void ActivateGroup (String group) throws BusException;
	
	@BusMethod
	// Invite another user to join  a group
	public void Invite(String originator, String group, String descriptor) throws BusException;
	
	@BusMethod
	// Request to be added to a group
	public void AddRequest(String originator, String group) throws BusException;
	
	@BusMethod
	// Request removal from a group
	public void RemoveRequest(String originator, String group) throws BusException;
	
	@BusMethod
	// Request addition of another user to a group
	public void AddMember(String originator, String group, String member) throws BusException;
	
	@BusMethod
	// Request removal of another user from a group (may fail)
	public void RemoveMember(String originator, String group, String member) throws BusException;
	
	@BusMethod
	// Request the profile of a group member
	public String GetMemberProfile(String group, String member) throws BusException;

	
	@BusSignal
	//	Courtesy notification that user has joined group (easier processing for app)
	void JoinedGroup(String group, String member) throws BusException;
	
	@BusSignal
	//	Courtesy notification that user has left group (easier processing for app)
	void LeftGroup(String group, String member) throws BusException;
	
	@BusSignal
	//	Notification that an invitation has been accepted
	void InvitationAccepted(String group, String member) throws BusException;
	
	@BusSignal
	//	Notification that an invitation has been rejected
	void InvitationRejected(String group, String member) throws BusException;
	
	@BusSignal
	//	Notification that a group has been updated
	void GroupUpdated (String group, String member) throws BusException;
	
}
