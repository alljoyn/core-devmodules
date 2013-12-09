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
package org.alljoyn.chat.api;

import org.alljoyn.devmodules.APICore;

import android.util.Log;

public class ChatAPI {

	private static final String TAG = "ChatAPI";

	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	private static ChatAPIInterface ChatInterface = null;

	// the callback object
	private static ChatCallbackObject mChatCallbackObject = null;

	/**
     * Assign a listener to be triggered when events occur from remote applications
     * 
     * @param listener		The implemented callback class that will be triggered
     */
	public static void RegisterListener(ChatListener listener) {
		setupInterface();
		mChatCallbackObject.registerListener(listener);
	}

	private static void setupInterface() {
		if (mChatCallbackObject == null) {
			mChatCallbackObject = new ChatCallbackObject();
		}

		if(ChatInterface == null) {
			ChatInterface = APICore.getInstance().getProxyBusObject("chat",
					new Class[] {ChatAPIInterface.class}).getInterface(ChatAPIInterface.class);
		}
	}

	/**
	 * Opens a chat room.
	 * Deprecated at this time, use SendGroupChat instead and leverage the UIFragment example
	 * 
	 * @param users		List of users to invite
	 * @return			The name of the chat room created
	 * @throws Exception
	 */
	public static String createChatRoom(String[] users) throws Exception {
		String room = "_";
		for(int i = 0; i < 32; i++)
			room += (char)('A'+(int)(Math.random()*26));
		CreateChatRoom(room, users);
		return room;
	}

	/**
	 * Opens a chat room named with the supplied string
 	 * Deprecated at this time, use SendGroupChat instead and leverage the UIFragment example
	 * 
	 * @param room		The name of the chat room
	 * @param users		List of the users to invite
	 * @throws Exception
	 */
	public static void CreateChatRoom(String room, String[] users) throws Exception {
		setupInterface();
		Log.i(TAG,"placing call to start chat room!!!");
		ChatInterface.createChat(room, users);
	}

	/**
	 * Send a chat message to all the users in a group
	 * 
	 * @param groupId		The group id to receive the message
	 * @param chatMsg		Message that will be sent
	 * @throws Exception
	 */
	public static void SendGroupChat(String groupId, String chatMsg) throws Exception {
		try{
			setupInterface();
			Log.i(TAG,"SendGroupChat() placing call to group send chat message!!!");
			ChatInterface.sendGroup(groupId, chatMsg, "");
		}catch(Exception e) {
			Log.e(TAG, "SendGroupChat() Error sending message. "+e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Send a chat message to a room
	 * Deprecated at this time, use SendGroupChat instead and leverage the UIFragment example
	 * 
	 * @param room			The room to receive the message
	 * @param chatMsg		Message that will be sent
	 * @throws Exception
	 */
	public static void Send(String room, String chatMsg) throws Exception {
		try{
			setupInterface();
			Log.i(TAG,"Send() placing call to signal chat message!!!");
			ChatInterface.send(room, chatMsg, "");
		}catch(Exception e) {
			Log.e(TAG, "Send() Error sending message. "+e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Stops a chat room and leaves it.
	 * Deprecated at this time, use SendGroupChat instead and leverage the UIFragment example
	 * 
	 * @param room
	 * @throws Exception
	 */
	public static void LeaveChat(String room) throws Exception {
		setupInterface();
		ChatInterface.leaveChat(room);
	}
}
