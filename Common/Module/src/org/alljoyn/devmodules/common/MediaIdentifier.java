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
package org.alljoyn.devmodules.common;

import org.alljoyn.bus.annotation.Position;

// Class that represents media description data

public class MediaIdentifier {

	// size in bytes
	@Position(0)
	public int    size;

	// file path on the hosting device
	@Position(1)
	public String path;

	// Location of Thumbnail/icon path, if any
	@Position(2)
	public String thumbpath;

	// (file) name, without directories
	@Position(3)
	public String name;

	// MIME type
	@Position(4)
	public String type;

	// Descriptive title (use this for UIs, not 'name')
	@Position(5)
	public String title;

	// User name/id of the source
	@Position(6)
	public String userid;

	// Media type (see MediaQueryConstants)
	@Position(7)
	public String mediatype;

	// path where file is stored on local device (not remote/source device)
	@Position(8)
	public String localpath;

	// Timestamp associated with Item action
	@Position(9)
	public long timestamp;
	
	@Position(10)
	public int transactionId;

	
	public MediaIdentifier(){
		size = 0;
		path = "";
		thumbpath = "";
		name = "";
		type = "";
		title = "";
		userid = "";
		mediatype = "";
		localpath = "";
		timestamp = 0;
		transactionId = 0;
	}
}
