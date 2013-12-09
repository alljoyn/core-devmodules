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

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusSignal;

@BusInterface (name = "org.alljoyn.devmodules.sessionmanager.SessionManagerBusInterface")
interface SessionManagerBusInterface {
    @BusSignal
    public void RequestBreakOut(String uniqueId/*, SessionInfo sessionInfo*/) throws BusException;
    
    @BusSignal
    public void AcceptBreakOut() throws BusException;
    
    @BusSignal
    public void RequestMerge(String str/*, SessionInfo sessionInfo*/) throws BusException;
    
    @BusSignal
    public void AcceptMerge(String str/*, SessionInfo sessionInfo*/) throws BusException;
    
    @BusSignal
    public void MigrateSession(/*SessionInfo sessionInfo*/) throws BusException;
    
    @BusSignal
    public void KickFromSession() throws BusException;
    
    @BusSignal
    public void CreateSignalEmitter(String uniqueId) throws BusException;
}
