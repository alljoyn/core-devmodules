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
package org.alljoyn.devmodules.loader;


import org.alljoyn.api.filetransfer.FileTransferAPIImpl;
import org.alljoyn.chat.api.ChatAPIImpl;
import org.alljoyn.groups.api.GroupsAPIImpl;
import org.alljoyn.mediaquery.api.MediaQueryAPIImpl;
import org.alljoyn.notify.api.NotifyAPIImpl;
import org.alljoyn.profilemanager.api.ProfileManagerAPIImpl;
import org.alljoyn.remotecontrol.api.RemoteControlAPIImpl;
import org.alljoyn.devmodules.filetransfer.FileTransferImpl;
import org.alljoyn.devmodules.api.ModuleAPIManager;
import org.alljoyn.devmodules.debug.api.DebugAPIImpl;
import org.alljoyn.devmodules.chat.ChatImpl;
import org.alljoyn.devmodules.debug.DebugImpl;
import org.alljoyn.devmodules.groups.GroupsImpl;
import org.alljoyn.devmodules.mediaquery.MediaQueryImpl;
import org.alljoyn.devmodules.notify.NotifyImpl;
import org.alljoyn.devmodules.profilemanager.ProfileManagerImpl;
import org.alljoyn.devmodules.remotecontrol.RemoteControlImpl;
import org.alljoyn.devmodules.service.AllJoynContainer;
import org.alljoyn.devmodules.whiteboard.WhiteboardImpl;
import org.alljoyn.whiteboard.api.WhiteboardAPIImpl;

import android.content.Context;

public class ContainerLoader {
	private static final String TAG = "ContainerLoader";
	
	//----------------
	
	public static void LoadAPIImpl(ModuleAPIManager instance) {
		instance.moduleList.add(new GroupsAPIImpl());
	    instance.moduleList.add(new ProfileManagerAPIImpl());
	    instance.moduleList.add(new FileTransferAPIImpl());
	    instance.moduleList.add(new MediaQueryAPIImpl());
		instance.moduleList.add(new ChatAPIImpl());
	    instance.moduleList.add(new RemoteControlAPIImpl());
	    instance.moduleList.add(new DebugAPIImpl());
	    instance.moduleList.add(new NotifyAPIImpl());
	    instance.moduleList.add(new WhiteboardAPIImpl());
	}
	
	public static void LoadImpl(AllJoynContainer instance, Context context) {
		instance.modules.add(new GroupsImpl(instance));
		instance.modules.add(new ProfileManagerImpl(instance, context));
		instance.modules.add(new FileTransferImpl(instance));
		instance.modules.add(new MediaQueryImpl(instance, context));
		instance.modules.add(new ChatImpl(instance));
		instance.modules.add(new RemoteControlImpl(instance));
		instance.modules.add(new DebugImpl(instance, context));
		instance.modules.add(new NotifyImpl(instance, context));
		instance.modules.add(new WhiteboardImpl(instance));
	}
	
}
