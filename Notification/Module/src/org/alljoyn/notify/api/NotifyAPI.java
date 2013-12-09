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
package org.alljoyn.notify.api;

import org.alljoyn.devmodules.APICore;
import org.alljoyn.devmodules.callbacks.CallbackObjectBase;
import org.alljoyn.devmodules.callbacks.TransactionHandler;
import org.alljoyn.devmodules.common.NotificationData;
import org.alljoyn.devmodules.notify.NotifyConstants;
import org.json.JSONObject;

import android.util.Log;

public class NotifyAPI {
	
	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	private static NotifyAPIInterface apiInterface = null;
	
	private static void setupInterface() {
		if(apiInterface == null) {
			apiInterface = APICore.getInstance().getProxyBusObject(NotifyAPIObject.OBJECT_PATH,
				new Class[] {NotifyAPIInterface.class}).getInterface(NotifyAPIInterface.class);
		}
	}
	
    /**
     * Assign a listener to be triggered when events occur from remote applications
     * 
     * @param listener		The implemented callback class that will be triggered
     */
	public static void RegisterListener(NotifyListener listener) {
		NotifyCallbackObject.listener = listener;
	}

	/**
	 * Sends a SessionLess signal that contains the NotificationData
	 * 
	 * @param data			Contains the notification information
	 * @throws Exception
	 */
	public static void SendGlobalNotification(NotificationData data) throws Exception {
		setupInterface();
		apiInterface.NotifyAll(data);
	}
	
	/**
	 * Sends a Signal to a supplied group
	 * 
	 * @param data			Contains the notification information
	 * @param groupId		The groupId that should receive the notification
	 * @throws Exception
	 */
	public static void SendGroupNotification(NotificationData data, String groupId) throws Exception {
		setupInterface();
		apiInterface.NotifyGroup(data, groupId);
	}
	
	/**
	 * Send a Notification to a specified user
	 * 
	 * @param data			Contains the notification information
	 * @param userId		The name of the user that should receive the Notification
	 * @throws Exception
	 */
	public static void SendUserNotification(NotificationData data, String userId) throws Exception {
		setupInterface();
		apiInterface.NotifyUser(data, userId);
	}
}
