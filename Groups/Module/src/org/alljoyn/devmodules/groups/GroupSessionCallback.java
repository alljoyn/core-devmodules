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

import org.alljoyn.devmodules.sessionmanager.SessionManager;
import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.GroupListDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.GroupCache;

import android.util.Log;



public interface GroupSessionCallback {
	
	// called when another member joins the hosted group session 
	// @param group The name of the group
	// @param member The ID of the user that joined
	public void onMemberJoined (String group, String member);
	
	// called when another member leaves the hosted group session 
	// @param group The name of the group
	// @param member The ID of the user that left
	public void onMemberLeft   (String group, String member);
	
	// Called when a group session that we had joined ends (probably unexpectedly)
	// @param group The name of the group
	// @param member The ID of the user hosting the session
	public void onSessionEnded (String group, String member);
} // GroupSessionCallback
