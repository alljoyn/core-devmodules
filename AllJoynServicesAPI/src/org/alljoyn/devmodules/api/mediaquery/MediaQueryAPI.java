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
package org.alljoyn.devmodules.api.mediaquery;

import org.alljoyn.devmodules.APICoreImpl;
import org.alljoyn.devmodules.api.groups.GroupsCallbackObject;
import org.alljoyn.devmodules.callbacks.TransactionHandler;
import android.util.Log;

public class MediaQueryAPI {
	
	public  static final String PHOTO      = "photo";
	public  static final String MUSIC      = "music";
	public  static final String VIDEO      = "video";
	public  static final String APP        = "app";
	public  static final String FILE       = "file";
	
	private static int transactionId = 0;
	//private static CallbackHandler handler = new CallbackHandler();
	
	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	// the callback object
	private static MediaQueryCallbackObject mMediaQueryCallbackObject = null;

	private static MediaQueryAPIInterface mediaQueryInterface = null;
	
	private static void setupInterface() {
		if (mMediaQueryCallbackObject == null) mMediaQueryCallbackObject = new MediaQueryCallbackObject();
		if(mediaQueryInterface == null) {
			mediaQueryInterface = 	APICoreImpl.getInstance().getProxyBusObject("mediaquery",
					new Class[] {MediaQueryAPIInterface.class}).getInterface(MediaQueryAPIInterface.class);
		}
	}
	
	public static int CollectMediaList(String peer, String mtype) throws Exception {
		setupInterface();
		mediaQueryInterface.CollectMediaList(transactionId++, peer, mtype);
		return transactionId;
	}
	
	public static int CollectMyMediaList(String mtype) throws Exception {
		setupInterface();
		mediaQueryInterface.CollectMyMediaList(transactionId++, mtype);
		return transactionId;
	}
	
	public static int requestMedia(String service, String mtype, String path) throws Exception {
		setupInterface();
		mediaQueryInterface.RequestMedia(transactionId++, service, mtype, path);
		return transactionId;
	}
	
	public static boolean requestActivation(String peer, String mtype, String path) throws Exception {
		TransactionHandler th = new TransactionHandler();
		int transactionId = MediaQueryCallbackObject.AddTransaction(th);
		mediaQueryInterface.RequestActivation(transactionId, peer, mtype, path);
		Log.i("MediaQueryConnector", "waiting for activation result: "+path);
		th.latch.await();
		return th.getJSONData().getBoolean("return");
	}
	
	// Request transfer of file to remote peer
	public static void sendFileRequest(String peer, String mtype, String path) throws Exception {
		setupInterface();
		mediaQueryInterface.SendFileRequest(peer, mtype, path);
	}	
	
	//static MediaQueryListener listener;
	
	public static final int MEDIAQUERY_SERVICE_AVAILABLE = 1;
	public static final int MEDIA_ITEM_READY = 2;
	public static final int MEDIA_QUERY_COMPLETE = 3;
	
	public static void RegisterListener(MediaQueryListener listener) {
		setupInterface();
		mMediaQueryCallbackObject.registerListener(listener);
	}
	
//	public static void HandleListener(int methodId,
//			JSONObject jsonData, MediaIdentifier item) {
//		try {
//			if(jsonData != null)
//				Log.i("MediaQueryConnector", "jsonData: "+jsonData.toString());
//			else
//				Log.i("MediaQueryConnector", "Got media!!!!");
//			switch(methodId) {
//				case MEDIAQUERY_SERVICE_AVAILABLE:
//					listener.onMediaQueryServiceAvailable(jsonData.getString("service"));
//					break;
//				case MEDIA_ITEM_READY:
//					//listener.onItemAvailable("media", item);
////					Message msg = handler.obtainMessage(methodId);
////					MSGObj obj = new MSGObj();
////					obj.jsonData = jsonData;
////					obj.item = item;
////					msg.obj = obj;
////					handler.sendMessage(msg);
//					item.localpath = "/sdcard/.alljoyn/"+item.type+"/"+item.name;///mediaFile.filename;
//					MediaQueryConnector.listener.onItemAvailable("media", item);
//					break;
//				case MEDIA_QUERY_COMPLETE:
//					MediaQueryConnector.listener.onTransferComplete("media", "", MediaQueryConnector.PHOTO, "");
//					break;
//			}
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//	}
}
//class MSGObj {
//	public JSONObject jsonData;
//	public MediaIdentifier item;
//}
//class CallbackHandler extends Handler
//{
//	public void handleMessage(Message msg) {
//		MSGObj obj = (MSGObj) msg.obj;
//		String peerName = obj.item.userid;
//		try {
//			if(obj.jsonData != null)
//				Log.i("MediaQueryConnector", "jsonData: "+obj.jsonData.toString());
//			else
//				Log.i("MediaQueryConnector", "Got media!!!!");
//			switch(msg.what) {
//				case MediaQueryConnector.MEDIA_ITEM_READY:
////					ArrayList<FileDescriptor> files = FileTransferConnector.requestFileList(peerName);
////					FileDescriptor mediaFile = null;
////					for(FileDescriptor file: files) {
////						Log.d(this.getClass().getName(),"Peer("+peerName+") file: "+file.filename);
////						if(obj.item.path.endsWith(file.filename)) {
////							mediaFile = file;
////						}
////					}
////					if(mediaFile != null) {
////						FileTransferConnector.setDefaultSaveDirectory("/sdcard/.alljoyn/"+obj.item.type+"/");
////						FileTransferConnector.requestFile(peerName, mediaFile.fileId, mediaFile.filename);
////					}
//					obj.item.localpath = "/sdcard/.alljoyn/"+obj.item.type+"/"+obj.item.name;///mediaFile.filename;
//					MediaQueryConnector.listener.onItemAvailable("media", obj.item);
//					break;
//			}
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//	}
//}	
