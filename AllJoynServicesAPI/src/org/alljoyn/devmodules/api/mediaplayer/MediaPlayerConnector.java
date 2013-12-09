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
package org.alljoyn.devmodules.api.mediaplayer;

import org.alljoyn.devmodules.APICoreImpl;
import org.alljoyn.devmodules.callbacks.CallbackObjectBase;
import org.alljoyn.devmodules.callbacks.TransactionHandler;
import org.json.JSONObject;

import org.alljoyn.devmodules.common.SongInfo;

import android.util.Log;

public class MediaPlayerConnector {
	
	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	private static MediaPlayerConnectorInterface mediaPlayerInterface = null;
	
	private static void setupInterface() {
		if(mediaPlayerInterface == null) {
			mediaPlayerInterface = APICoreImpl.getInstance().getProxyBusObject("mediaplayer",
				new Class[] {MediaPlayerConnectorInterface.class}).getInterface(MediaPlayerConnectorInterface.class);
		}
	}
	
	public static void PlaySong(String peer, SongInfo si) throws Exception {
		setupInterface();
		Log.i("MediaPlayerConnector","placing call to onKeyDown!!!");
		mediaPlayerInterface.startStreaming(si);
	}

//	public static void PlaySong(String peer, SongInfo si) throws Exception {
//		setupInterface();
//		Log.i("MediaPlayerConnector","placing call to onKeyDown!!!");
//		//TransactionHandler th = new TransactionHandler();
//		//int transactionId = CallbackObject.AddTransaction(th);
//		mediaPlayerInterface.startStreaming(si);
//		//Log.i("RemoteKeyConnector", "waiting for onKeyDown result");
//		//th.latch.await();
//		//return th.getJSONData().getBoolean("return");
//	}
	
//	public static void HandleListener(int methodId,
//	String jsonCallbackData, byte[] rawData, int totalParts, int partNumber) {
//		try {
//			Log.i("RemoteKeyConnector", "jsonData: "+jsonCallbackData);
//			JSONObject jobj = new JSONObject(jsonCallbackData); 
//			switch(methodId) {
//				case 0:
//					Process process = Runtime.getRuntime().exec(new String[]{"input", "keyevent", ""+jobj.getInt("keycode")});
//					break;
//			}
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//	}
}
