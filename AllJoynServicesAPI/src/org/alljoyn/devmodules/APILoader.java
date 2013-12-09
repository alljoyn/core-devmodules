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
package org.alljoyn.devmodules;

import org.alljoyn.remotecontrol.api.RemoteControlCallbackObject;
import org.alljoyn.devmodules.api.debug.DebugCallbackObject;
import org.alljoyn.devmodules.api.mediaquery.MediaQueryCallbackObject;
import org.alljoyn.devmodules.api.profilemanager.ProfileManagerCallbackObject;
import org.alljoyn.devmodules.api.filetransfer.FileTransferCallbackObject;
import org.alljoyn.devmodules.api.groups.GroupsCallbackObject;
import org.alljoyn.whiteboard.api.WhiteboardCallbackObject;
import org.alljoyn.chat.api.*;
import org.alljoyn.notify.api.*;

public class APILoader {
	private static final String TAG = "APILoader";
	
	//----------------
	
	public static void LoadAPI(APICore instance) {
		instance.objectList.clear();
		instance.objectList.add(new ProfileManagerCallbackObject());
		instance.objectList.add(new FileTransferCallbackObject());
		instance.objectList.add(new ChatCallbackObject());
		instance.objectList.add(new MediaQueryCallbackObject());
		instance.objectList.add(new GroupsCallbackObject());
		instance.objectList.add(new RemoteControlCallbackObject());
		instance.objectList.add(new DebugCallbackObject());
		instance.objectList.add(new NotifyCallbackObject());
		instance.objectList.add(new WhiteboardCallbackObject());
	}
	
}
