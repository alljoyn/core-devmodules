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

import org.alljoyn.bus.Status;

/**
 * This class is returned by the joinOrCreate() method of the session manager
 * 
 */
public class JoinOrCreateReturn {
    private Status status;
    private boolean joined;
    
    public JoinOrCreateReturn(Status status, boolean hasJoined) {
        this.status = status;
        this.joined = hasJoined;
    }
    
    /**
     * Get the AllJoyn status return from calling joinOrCreateSession()
     * @return the AllJoyn status return from joinOrCreateSession()
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Get a flag denoting whether the user joined or created the session
     * @return true if the caller joined the session, false if the called created the session
     */
    public boolean hasJoined() {
        return joined;
    }
}
