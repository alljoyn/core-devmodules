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
package org.alljoyn.remotecontrol.api;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.*;

@BusInterface(name = RemoteControlCallbackInterface.SERVICE_NAME)
public interface RemoteControlCallbackInterface {
	public static final String SERVICE_NAME = "org.alljoyn.devmodules.api.remotecontrol";
	public static final String OBJECT_PATH = "/remoteControlCallbackObject";
	
	@BusSignal
	public void onKeyDown(String groupId, int keyCode) throws BusException;
	
	@BusSignal
	public void executeIntent(String groupId, String intentAction, String intentData) throws BusException;
}
