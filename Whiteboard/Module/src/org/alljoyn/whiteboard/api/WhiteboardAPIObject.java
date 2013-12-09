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


import org.alljoyn.devmodules.common.WhiteboardLineInfo;
import org.alljoyn.devmodules.whiteboard.WhiteboardImpl;

import org.alljoyn.bus.*;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

public class WhiteboardAPIObject implements WhiteboardAPIInterface, BusObject {
	public static final String OBJECT_PATH = "whiteboard";
	
	private HandlerThread handlerThread = new HandlerThread(OBJECT_PATH);
	{handlerThread.start();}
	private ConnectorHandler handler = new ConnectorHandler(handlerThread.getLooper());
	private static final int DRAW = 0;
	private static final int CLEAR = 1;
	
	private static final String TAG = "WhiteboardAPIObject" ;

	public void Join(String peer) throws BusException {
		
	}

	public void Leave(String peer) throws BusException {
		
	}
	
	public void Draw(String groupId, String peer, WhiteboardLineInfo lineInfo) throws BusException {
		try{
			Message msg = handler.obtainMessage(DRAW, lineInfo);
			Bundle data = new Bundle();
			data.putString("groupId", groupId);
			data.putString("peer", peer);
			msg.setData(data);
			handler.sendMessage(msg);
		}catch(Exception e) {
			e.printStackTrace();
		}	
	}

	public void Clear(String groupId, String peer) throws BusException {
		try{
			Message msg = handler.obtainMessage(CLEAR);
			Bundle data = new Bundle();
			data.putString("groupId", groupId);
			data.putString("peer", peer);
			msg.setData(data);
			handler.sendMessage(msg);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}	
	
	private class ConnectorHandler extends Handler
    {
		public ConnectorHandler(Looper loop) {
			super(loop);
		}
		public void handleMessage(Message msg) {
			WhiteboardImpl impl = (WhiteboardImpl)WhiteboardImpl.getInstance();
			Bundle data = msg.getData();
			if(impl == null || data == null)
				return;
			switch(msg.what) {
				case DRAW:
					impl.Draw(data.getString("groupId"),(WhiteboardLineInfo)msg.obj);
					break;
				case CLEAR:
					impl.Clear(data.getString("groupId"),data.getString("peer"));
					break;
			}
		}
    }
}
