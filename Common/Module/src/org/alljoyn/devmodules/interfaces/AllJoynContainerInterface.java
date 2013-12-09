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
package org.alljoyn.devmodules.interfaces;

import java.util.ArrayList;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import org.alljoyn.devmodules.sessionmanager.SessionManager;

//Interface that a developer would implement that contains the container that holds the references to the different services
public interface AllJoynContainerInterface {	
	public static final String SERVICE_NAME = "org.alljoyn.devmodules";
	
	public String getUniqueID();
	
	//legacy will remove
	public SessionManager getSessionManager();
	
	/*
	 * This method will return a sessionId of a group if groups is supported
	 * otherwise it will throw an exception if groups is not supported
	 */
	public int getGroupSessionId(String groupName) throws Exception;
	
	public BusAttachment getBusAttachment(); 
	
	//optional but maybe provide in helper class
	public Status createSession(String sessionName, short sessionPort, SessionPortListener sessionPortListener, SessionOpts sessionOpts);

	//Should probably remove as sessionManager specific
	public ArrayList<String> getParticipants(String uID);
}


