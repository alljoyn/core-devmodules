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


import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.devmodules.common.FileBuffer;
import org.alljoyn.devmodules.common.FileParameters;

// This interface allows the copying of potentially large files between devices
// It is assumed that the requesting side has knowledge of the file location obtained elsewhere
// The basic sequence is:
// - open the file
// - get the transfer parameters (file size, buffer size etc.)
// - iterate, requesting one buffer at a time
// - close the file

@BusInterface (name = "org.alljoyn.devmodules.mediaquery.filetransfer")
public interface MediaQueryFileTransferInterface {

	// Method to open a file. 
	// Positive return value is a file identifier (in case a client has multiple active transfers)
	// Negative return value indicates an error occurred
	@BusMethod
    public int Open (String filename) throws BusException;

	// Method to get the transfer parameters
	@BusMethod (replySignature = "r")
    public FileParameters GetFileParameters (int fileid) throws BusException;

	// Method to get a buffer. Sequence number is supplied by the caller and should be incremented
	// every time a new buffer is requested.
	// the sender will only process the current (maybe a retransmission) or next sequence number
	@BusMethod (replySignature = "r")
    public FileBuffer GetBuffer(int fileid, int seqnum) throws BusException;

	// Method to close the file being transferred
	@BusMethod
    public int Close(int fileid) throws BusException;

	// Method to abort a file transfer
	@BusMethod
    public void Abort(int fileid) throws BusException;

	// Method to abort all file transfers
	@BusMethod
    public void AbortAll() throws BusException;

}
