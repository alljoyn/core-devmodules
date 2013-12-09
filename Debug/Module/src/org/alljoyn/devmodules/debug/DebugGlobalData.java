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
package org.alljoyn.devmodules.debug;

// Class for holding global data for the Debug service
public class DebugGlobalData {

	// Flag indicating whether service is enabled or not
	public static boolean   mIsEnabled  = false;
	
	// Filter string for logging (OS-specific)
	public static String    mFilter     = "AndroidRuntime:E dalvikvm:E *:D";
	
	// Object for accessing Debug implementation
	public static DebugImpl mDebugImpl  = null;
	
	// The list of (local) debug information
	public static LocalDebugList mDebugList = null;
	
	// The list of (remote) debug information
	public static RemoteDebugList mRemoteDebugList = null;
	
	// Name of service being advertised
	public static String    mServiceName = "";
	
}
