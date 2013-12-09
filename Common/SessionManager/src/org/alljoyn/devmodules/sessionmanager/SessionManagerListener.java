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

/**
 * The SessionManagerListener is an additional listener that can be used to 
 * listen for the busStopping(), foundAdvertisedname(), lostAdvertisedName(), 
 * nameOwnerChanged(), sessionJoined(), sessionLost(), sessionMemberAdded(), 
 * and sessionMemberRemoved() signals. The desired callback methods need to 
 * be overridden. 
 * 
 */
public class SessionManagerListener {
    
    public void busStopping() {
        return;
    }
    
    public void foundAdvertisedName(String name, short transport, String namePrefix) {
        return;
    }
    
    public void lostAdvertisedName(String name, short transport, String namePrefix) {
        return;
    }
    
    public void nameOwnerChanged(String busName, String previousOwner, String newOwner) {
        return;
    }
    
    public void sessionJoined(short sessionPort, int id, String joiner) {
        return;
    }
    
    public void sessionLost(int sessionId) {
        return;
    }
    
    public void sessionMemberAdded(int sessionId, String uniqueName) {
        return;
    }
    
    public void sessionMemberRemoved(int sessionId, String uniqueName) {
        return;
    }
}
