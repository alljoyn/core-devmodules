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
package org.alljoyn.aroundme.MainApp;

import org.alljoyn.aroundme.Debug.DebugFunctionsFragment;
import org.alljoyn.aroundme.Peers.NearbyUsersPagerFragment;
import org.alljoyn.aroundme.Peers.PeerDetailsFragment;

import android.os.Bundle;

/**
 * This interface is used by Fragments to interact with other fragments via the main application.
 * This is because fragments can't really communicate directly and Intents don't seem to work consistently
 * @author pprice
 *
 */
public interface TaskControlInterface {
	
	/**
	 * Inner class to hold constants. It's just done this way to make the scope look correct 
	 * and allow multiple files to access the same constants
	 */
	public class Functions {
		public static final int HOME                    =  0 ;
		public static final int ABOUT                   =  1 ;
		public static final int SETTINGS                =  2 ;
		public static final int NEARBY_USERS            =  3 ;
		public static final int USER_DETAILS            =  4 ;
		public static final int USER_SPECIFIC_FUNCTIONS =  5 ;
		public static final int DEBUG_FUNCTIONS         =  6 ;
		public static final int TRANSACTIONS            =  7 ;
		public static final int MANAGE_GROUPS           =  8 ;
		public static final int CHAT                    =  9 ;
		public static final int NOTIFICATIONS           = 10 ;
		public static final int WHITEBOARD              = 11 ;
	}
	
	/**
	 * Start a 'function', which is just a logical thing
	 * @param function the id (see class Functions) that identifies the function
	 * @param args a Bundle containing data to pass to the function (can be null)
	 */
	public void startFunction (int function, Bundle args);
}
