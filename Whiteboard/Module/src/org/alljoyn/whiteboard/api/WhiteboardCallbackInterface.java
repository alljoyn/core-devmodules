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
package org.alljoyn.whiteboard.api;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.*;
import org.alljoyn.devmodules.callbacks.CallbackInterface;
import org.alljoyn.devmodules.common.WhiteboardLineInfo;

@BusInterface(name = WhiteboardCallbackInterface.SERVICE_NAME)
public interface WhiteboardCallbackInterface {
	public static final String SERVICE_NAME = "org.alljoyn.devmodules.api.whiteboard";
	public static final String OBJECT_PATH = "/whiteboardCallbackObject";
	
	@BusSignal
	public void onDraw(WhiteboardLineInfo lineInfo) throws BusException;
	
	@BusSignal
	public void onGroupDraw(String group, WhiteboardLineInfo lineInfo) throws BusException;
	
	@BusSignal
	public void onClear() throws BusException;
	
	@BusSignal
	public void onGroupClear(String group) throws BusException;
}
