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
import org.alljoyn.bus.annotation.*;
import org.alljoyn.devmodules.common.GroupDescriptor;

// AllJoyn interface from app to background service
@BusInterface(name = "org.alljoyn.api.devmodules.groups")
public interface GroupsAPIInterface {
	
	@BusMethod
	// Get the list of groups currently supported
	public String [] GetGroupList() throws BusException;
	
	@BusMethod
	// Get the list of groups currently supported (in GroupListDescriptor format)
	public String GetGroupListDescriptor() throws BusException;
	
	
	@BusMethod
	public String GetGroupDescriptor(String group) throws BusException;

	@BusMethod
	public void AddGroup(String group, String descriptor) throws BusException;
	
	@BusMethod
	public void RemoveGroup(String group) throws BusException;
	
	@BusMethod
	public void SaveGroup(String group) throws BusException;
	
	@BusMethod
	public void DeleteGroup(String group) throws BusException;
	
	@BusMethod
	public void EnableGroup(String group) throws BusException;
	
	@BusMethod
	public void DisableGroup(String group) throws BusException;
	
	@BusMethod
	public boolean IsGroupEnabled(String group) throws BusException;
	
	@BusMethod
	public boolean IsGroupActive(String group) throws BusException;
	
	@BusMethod
	public boolean IsGroupDefined(String group) throws BusException;
	
	@BusMethod
	public void AddMembers(String group, String[] members) throws BusException;
	
	@BusMethod
	public void RemoveMembers(String group, String[] members) throws BusException;
	
	@BusMethod
	public String[] GetAllMembers(String group) throws BusException;
	
	@BusMethod
	public String[] GetActiveMembers(String group) throws BusException;
	
	@BusMethod
	public String[] GetInactiveMembers(String group) throws BusException;
	
	@BusMethod
	public String[] GetRemovedMembers(String group) throws BusException;

	@BusMethod
	public boolean IsMemberActive(String group, String member) throws BusException;
	
	@BusMethod
	public void InviteMembers(String group, String[] members) throws BusException;
	
	@BusMethod
	public void AcceptInvitation(String group) throws BusException;
	
	@BusMethod
	public void RejectInvitation(String group) throws BusException;
	
	@BusMethod
	public void IgnoreInvitation(String group) throws BusException;
	
	
	
	/**
	 * Runs verification tests on groups
	 * Result returned via callback interface
	 */
	@BusMethod
	public void RunGroupsTest() throws BusException;

}
