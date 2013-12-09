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
package org.alljoyn.devmodules.sessionmanager;

import java.util.ArrayList;

import org.alljoyn.bus.AuthListener;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;

public interface SessionManagerInterface {
    
    public Status createSession (String sessionName, short sessionPort, SessionPortListener sessionPortListener);

    public Status createSession (String sessionName, short sessionPort, 
            SessionPortListener sessionPortListener, SessionOpts sessionOpts);
    
    public void destroySession (String sessionName);
    
    public Status joinSession (String sessionName, short sessionPort, Mutable.IntegerValue sessionId,
            SessionOpts sessionOpts, SessionListener sessionListener);
    
    public Status leaveSession (String sessionName);
    
    public JoinOrCreateReturn joinOrCreateSession (String sessionName, short sessionPort, 
            Mutable.IntegerValue sessionId, SessionOpts sessionOpts, 
            SessionListener sessionListener, SessionPortListener sessionPortListener);
    
    public Status registerBusObject(BusObject busObject, String objectPath);
    
    public Status registerBusObjects(ArrayList<BusObjectData> busObjects);
    
    public void unregisterBusObject(BusObject busObject);
    
    public void unregisterBusObjects(ArrayList<BusObjectData> busObjects);
    
    public void unregisterAllBusObjects();
    
    public ArrayList<String> listSessions ();
    
    public ArrayList<String> listHostedSessions ();
    
    public ArrayList<String> listJoinedSessions ();
    
    public ArrayList<String> getParticipants (String sessionName);
    
    public int getNumParticipants(String sessionName);
    
    public int getSessionId(String sessionName);
    
    public BusAttachment getBusAttachment();
    
    public Status registerSignalHandlers(Object classWithSignalHandlers);
    
    public void addSessionManagerListener(SessionManagerListener sessionMgrListener);
    
    public boolean isBusConnected();
    
    public Status registerAuthListener(String authMechanisms, AuthListener listener,
            String keyStoreFileName, boolean isShared);
    
    public Status registerAuthListener(String authMechanisms, AuthListener listener,
            String keyStoreFileName);
    
    public Status registerAuthListener(String authMechanisms, AuthListener listener);
    
    public Status addAlias(String sessionName, String masterSessionName, SessionOpts sessionOpts);
    
    public void removeAlias(String sessionName);
    
    public ArrayList<String> listAliases();
    
    public String getSessionName(String wellKnownName);
    
    // TODO: Implement methods below
//    public void breakOutSession (String[] ids, int waitTime);
//    
//    public void mergeSessions (String sessionName, String[] sessions);
//    
//    public void kickParticipants (String sessionName, String[] ids);
//    
//    public void banParticipants (String[] ids);
//    
//    public void unbanParticipants (String[] ids);

    
}
