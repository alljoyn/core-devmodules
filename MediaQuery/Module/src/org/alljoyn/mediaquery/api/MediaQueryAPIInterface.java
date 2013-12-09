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
package org.alljoyn.mediaquery.api;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.*;

@BusInterface(name = "org.alljoyn.api.devmodules.mediaquery")
public interface MediaQueryAPIInterface {
	// Initiate (background) collection of media available. The return value is the transaction ID
	@BusMethod
    public  void CollectMediaList(int transactionId, String service, String mtype) throws BusException;
 
	@BusMethod
    public  void CollectMyMediaList(int transactionId, String mtype) throws BusException;
	
    // Request copying of file from remote device
	@BusMethod
    public  int RequestMedia(int transactionId, String service, String mtype, String path) throws BusException;
	
	@BusMethod
    public  int RequestMyMedia(int transactionId, String service, String mtype, String path) throws BusException;
   
    // Request Activation of file on remote device
	@BusMethod
	public void RequestActivation (int transactionId, String peer, String mtype, String path) throws BusException;
	
	// Method to 'send' a file to another device (really a request for it to copy the file)
	@BusMethod
	public void SendFileRequest (String service, String mtype, String filepath) throws BusException;

}
