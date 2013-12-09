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

package org.alljoyn.devmodules.mediaquery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.bus.annotation.Position;
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaQueryResult;


@BusInterface (name = "org.alljoyn.devmodules.mediaquery")
public interface MediaQueryInterface {

	//Generic Query Interface
	
	// Method to list photos hosted by this device
	@BusMethod (replySignature = "r")
    public  MediaQueryResult ListMedia(String mtype) throws BusException;

	// Method to get a  thumbnail from this device
	@BusMethod
    public byte[] GetThumbnail(String mtype, String filepath) throws BusException;

	// Method to get a  MediaIdentifier from this device (usually to get the metadata etc.)
	@BusMethod
    public MediaIdentifier GetMediaIdentifier(String mtype, String filepath) throws BusException;
	

	// Method to request activation of a media item on the hosting device
	@BusMethod
	public boolean RequestActivation (String requestor, String mtype, String filepath) throws BusException;
	
	// Method to 'send' a file to another device (really a request for it to copy the file)
	@BusMethod
	public void SendFileRequest (String requestor, String mtype, String filepath) throws BusException;
	
}
