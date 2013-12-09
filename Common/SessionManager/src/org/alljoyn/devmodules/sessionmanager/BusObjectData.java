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

import org.alljoyn.bus.BusObject;

/**
 * This class is used to store bus objects with their corresponding object paths
 * 
 */
public class BusObjectData {
    private BusObject mBusObject;
    private String mObjectPath;
    
    /**
     * @param busObj  the bus object
     * @param objPath the object path of the bus object
     */
    public BusObjectData (BusObject busObj, String objPath) {
        mBusObject = busObj;
        mObjectPath = objPath;
    }
    
    /**
     * Get the bus object
     * @return the bus object
     */
    public BusObject getBusObject() {
        return mBusObject;
    }
    
    /**
     * Get the object patch of the bus object
     * @return the object path of the bus object
     */
    public String getObjectPath() {
        return mObjectPath;
    }
}
