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

import android.util.Log;

public class MediaQueryConstants {

	public static String COMPONENT_NAME = "MediaQuery";
	public static final short SESSION_PORT = 621;
	public static final String SERVICE_NAME = "mediaquery";
	public static final String NAME_PREFIX = "org.alljoyn.devmodules."+SERVICE_NAME;
	public static final String OBJECT_PATH = "/"+SERVICE_NAME;
	
	// Constants for media "types"
	public  static final String PHOTO      = "photo";
	public  static final String MUSIC      = "music";
	public  static final String VIDEO      = "video";
	public  static final String APP        = "app";
	public  static final String FILE       = "file";

}
