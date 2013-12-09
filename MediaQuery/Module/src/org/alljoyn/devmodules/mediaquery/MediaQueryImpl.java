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
package org.alljoyn.devmodules.mediaquery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.alljoyn.bus.*;
import org.alljoyn.mediaquery.api.MediaQueryAPIImpl;
import org.alljoyn.devmodules.common.FileBuffer;
import org.alljoyn.devmodules.common.FileParameters;
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaQueryResult;
import org.alljoyn.devmodules.common.MediaQueryResultDescriptor;
import org.alljoyn.devmodules.common.MediaUtilities;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.devmodules.filetransfer.FileTransferImpl;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.alljoyn.devmodules.interfaces.ModuleInterface;
import org.alljoyn.storage.MediaCache;
import org.alljoyn.storage.MediaTransactionCache;
import org.alljoyn.storage.ThumbnailCache;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

public class MediaQueryImpl extends BusListener implements ModuleInterface {	

	private static final String TAG = "MediaQueryImpl";

	private MediaQueryObject mediaQueryObject;

	private String myWellknownName;
	private String namePrefix = "org.alljoyn.devmodules";

	private static MediaQueryImpl instance;
	public static MediaQueryImpl getInstance() { return instance; }
	public static AllJoynContainerInterface getAllJoynContainer() { return instance.alljoynContainer; }

	private AllJoynContainerInterface alljoynContainer;
	private Context mContext;
	private FileTransferImpl mFileTransfer = null;

	private int mySessionId = -1;

	public MediaQueryImpl(AllJoynContainerInterface alljoynContainer, Context context) {
		this.alljoynContainer = alljoynContainer;
		instance = this;
		mediaQueryObject = new MediaQueryObject(alljoynContainer,context);
		mContext = context;
	}

	public void RegisterSignalHandlers() {
		alljoynContainer.getBusAttachment().registerSignalHandlers(this);
	}

	public void SetupSession() {
		SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
		alljoynContainer.createSession(getAdvertisedName(), MediaQueryConstants.SESSION_PORT, new SessionPortListener() {
			public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
				Log.d(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
				if (sessionPort == MediaQueryConstants.SESSION_PORT) {
					return true;
				}
				return false;
			}

			public void sessionJoined(short sessionPort, int id, String joiner) {
				Log.d(TAG, "User " + joiner + " joined., id=" + id);
				mySessionId = id;
			}
		}, sessionOpts);
		Log.i(TAG,"Advertised: "+getAdvertisedName());
	}

	public BusObject getBusObject() {
		return mediaQueryObject;
	}

	public String getObjectPath() {
		return MediaQueryConstants.OBJECT_PATH ;
	}

	public String getAdvertisedName() {
		if(myWellknownName == null) {
			myWellknownName = MediaQueryConstants.SERVICE_NAME+"."+alljoynContainer.getUniqueID();
		}
		return myWellknownName;
	}

	public String getServiceName() {
		return MediaQueryConstants.SERVICE_NAME;
	}

	public void foundAdvertisedName(String name, short transport, String namePrefix) {
		if(!name.contains(getAdvertisedName()) && name.contains(MediaQueryConstants.NAME_PREFIX)){
			Log.i(TAG, "MediaQuery service found: "+name);
			this.namePrefix = namePrefix;
			try { 
				JSONObject jsonData = new JSONObject();
				jsonData.put("service", name);
				MediaQueryAPIImpl.mediaQueryCallback.onMediaQueryServiceAvailable(name);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void lostAdvertisedName(String name, short transport, String namePrefix) {
		if(!name.contains(getAdvertisedName()) && name.contains(MediaQueryConstants.NAME_PREFIX)){
			alljoynContainer.getSessionManager().leaveSession(name); //Need to look at if this is needed
		}
	}

	public void shutdown() {

	}


	/* ************************************** */
	/* Module specific implementation methods */
	/* ************************************** */

	private void setupFileTransfer(){
		if (mFileTransfer == null){
			mFileTransfer = FileTransferImpl.getInstance();
		}
	}

	//////////////////////////////////////////////////////////////////////////////
	// Accessor Functions for interacting with remote services
	//////////////////////////////////////////////////////////////////////////////


	// counter for assigning transaction IDs
	private static int mTidCounter = 0;

	// Get transaction ID for action
	private static int getTransactionId(){
		int tid = mTidCounter;
		mTidCounter++;         // increment counter
		mTidCounter &= 0x7FFF; //  and wrap
		return tid;
	}


	// Collect the list of media from a remote service
	public int collectMediaList(int transactionId, String peer, String mtype){
		Log.v(TAG,"collectMediaList(). peer: "+peer);
		doLoadRemoteMedia(transactionId, peer, mtype);

		return transactionId;
	}

	public int collectMyMediaList(int transactionId, String mtype){
		//ThumbnailCache.init();
		Log.d(this.getClass().getName(),"Here in collectMyMediaList!!!!!!!!");
		MediaQueryResult mqr = new MediaQueryResult();
		MediaQueryResultDescriptor mqrd = new MediaQueryResultDescriptor();
		try{
			mqr = mediaQueryObject.ListMedia(mtype);

			Log.v(TAG, "doLoadRemoteMedia(): "+mqr.count+" found");
			if (mqr.count>0){
				Log.v(TAG, "[0]"+mqr.media[0].name);

				//int numfiles = (mqr.count>5) ? 5 : mqr.count;
				int numfiles = mqr.count;

				// for each file, copy and save the thumbnail, modify the location in the results and save		
				for (int i=0; i<numfiles; i++){
					MediaIdentifier mi = mqr.media[i];
					String tpath = mi.thumbpath;
					if ((tpath!=null) && (tpath.length()>0)){
						byte[] thumb = new byte[0];
						String lclpath = ThumbnailCache.getThumbnailPath("LOCAL", tpath);
						mi.thumbpath=lclpath;
						// Only retrieve if it's not already in the cache
						//TODO: check age of thumbnail?
						if (!ThumbnailCache.isPresent(lclpath)){
							try{
								thumb = mediaQueryObject.GetThumbnail(mtype, tpath);
								ThumbnailCache.saveThumbnail("LOCAL", lclpath, thumb);
							} catch (Exception e){
								Log.e(TAG, "Error getting thumbnail");
								mi.thumbpath = "";
							}
						}
					}
					mi.mediatype = mtype;

					// Set the path of where he media file would be saved, if it is copied anyway
					//mi.localpath = MediaCache.getMediaPath(peer, mtype, mi.name);

					// add to the descriptor
					mqrd.add(mi);

					// signal availability
					//mMediaQueryControlInterface.MediaItemAvailable(transaction, service, mtype, mi);
					//REFACTOR MOD ModuleAPIManager.callbackInterface.MediaItemReady(transactionId, mi);
					mi.localpath = "/sdcard/.alljoyn/"+mi.type+"/"+mi.name;
					MediaQueryAPIImpl.mediaQueryCallback.onItemAvailable("media", mi);
					Log.v(TAG, "Sent: MediaItemAvailable("+mi.title+")");
				}

				// save the list to local cache
				Log.v(TAG, "Saving query results");

				// Send a Signal notifying availability
				//MediaQueryCache.saveMediaQuery(service, mtype, mqrd.toString());
				//mMediaQueryControlInterface.MediaQueryComplete(transaction, service, mtype);
				//REFACTOR MOD ModuleAPIManager.callbackInterface.MediaQueryComplete(transactionId);
				MediaQueryAPIImpl.mediaQueryCallback.onTransferComplete("media", "", mtype, "");
				Log.v(TAG, "Sent MediaQueryComplete Signal");
			}
		} catch (Exception e){
			Log.e(TAG, "Error getting photo list: "+e.toString());
			e.printStackTrace();
			//Utilities.logException(TAG, "Full error: ", e);
		}
		return transactionId;
	}


	// Request a media file from a remote service
	public int requestMedia(String service, String mtype, String filepath){
		int tid = -1;

		tid = getTransactionId();
		Log.v(TAG,"requestMedia(). TID: "+tid);

		// Initiate the collection and return
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putInt("action", REQUEST_FILE);
		data.putInt("transaction", tid);
		data.putString("service", service);
		data.putString("mtype",   mtype);
		data.putString("path",   filepath);
		msg.setData(data);

		customAction(msg);

		return tid;
	}


	// Request activation of a media file on a remote device
	public void requestActivation(int transactionId, String service, String mtype, String filepath){
		// Initiate the collection and return
		doRequestActivation(transactionId,
				service, 
				mtype,
				filepath);
	}


	// Request sending of a media file to a remote device
	public void sendFileRequest(String service, String mtype, String filepath){
		// Initiate the send and return
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putInt("action", REQUEST_SEND_FILE);
		data.putString("service", service);
		data.putString("mtype",   mtype);
		data.putString("path",   filepath);
		msg.setData(data);

		customAction(msg);
	}

	private void setupSession (){
		Message msg = new Message();
		Bundle data = new Bundle();
		data.putInt("action", SETUP_SESSION);
		msg.setData(data);
		customAction(msg);
	}

	// List of custom actions that need to run in the Bus Handler thread
	private static final int SETUP_SESSION      = 1; 
	private static final int COLLECT_MEDIA      = 2;
	private static final int REQUEST_FILE       = 3;
	private static final int REQUEST_ACTIVATION = 4;
	private static final int REQUEST_SEND_FILE  = 5;


	// method for handling custom actions that need to run in the Bus Handler Thread
	protected void customAction (Message message){
		mHandler.sendMessage(message);
	}


	// Use this Handler for asynchronous transactions
	// Note that there may be several transactions active in parallel, so don't assume a single user
	private ImplHandler mHandler = new ImplHandler();

	private class ImplHandler extends Handler
	{
		public void handleMessage(Message message) {
			int action = message.getData().getInt("action");
			switch (action) {
			case SETUP_SESSION: {
				SetupSession();
				break;
			}
			case COLLECT_MEDIA:
				doLoadRemoteMedia(message.getData().getInt("transaction"), 
						message.getData().getString("peer"), 
						message.getData().getString("mtype"));
				break;

			case REQUEST_FILE:
				doRequestFile(message.getData().getInt("transaction"), 
						message.getData().getString("service"), 
						message.getData().getString("mtype"),
						message.getData().getString("path"));
				break;

			case REQUEST_ACTIVATION:
				doRequestActivation(message.getData().getInt("transactionId"),
						message.getData().getString("service"), 
						message.getData().getString("mtype"),
						message.getData().getString("path"));
				break;

			case REQUEST_SEND_FILE:
				doSendFileRequest(message.getData().getString("service"), 
						message.getData().getString("mtype"),
						message.getData().getString("path"));
				break;

			default:
				Log.w(TAG, "handleMessage() - Unkown action: "+action);
				break;
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////
	// Loading of remote media
	////////////////////////////////////////////////////////////////////////////

	// collect media lists and save to cache
	private void doLoadRemoteMedia(int transaction, String peer, String mtype){
		Log.v(TAG, "doLoadRemoteMedia("+peer+", "+mtype+")");

		//MediaQueryCache.init();
		ThumbnailCache.init();


		MediaQueryResult mqr = new MediaQueryResult();
		MediaQueryResultDescriptor mqrd = new MediaQueryResultDescriptor();

		try {

			//check to see if results for this query are already present and not older than 10 minutes
			//			String qFile = MediaQueryCache.getMediaQueryPath(service, mtype);
			//			if ((MediaQueryCache.isFilePresent(qFile)) && (!MediaQueryCache.isFileOlderThan(qFile, 30))){
			//				// Query results already present and less than 10 minutes old
			//
			//				// Load query results from file
			//				String qString = MediaQueryCache.retrieveMediaQuery(service, mtype);
			//				mqrd.setString(qString);
			//
			//				// Loop through entries and send Signal for each
			//				for (int i=0; i<mqrd.size(); i++){
			//					// get item
			//					MediaIdentifier mi = mqrd.get(i);
			//
			//					// signal availability
			//					mMediaQueryControlInterface.MediaItemAvailable(transaction, service, mtype, mi);
			//					Log.v(TAG, "Sent: MediaItemAvailable("+mi.title+")");
			//				}
			//
			//
			//				mMediaQueryControlInterface.MediaQueryComplete(transaction, service, mtype);
			//				Log.v(TAG, "Sent MediaQueryComplete Signal");
			//
			//			} else 
			{
				// Query results not present or too old

				// Query the remote device for the list of media items
				String wellKnownName = getServiceName(peer);
				
				SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
				Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
				alljoynContainer.getSessionManager().joinSession(wellKnownName, MediaQueryConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());
				Log.e(TAG, "Joined the session: "+sessionId.value);
				Log.e(TAG, "object name: "+wellKnownName+" "+MediaQueryConstants.OBJECT_PATH);
				ProxyBusObject mProxyObj =  alljoynContainer.getBusAttachment().getProxyBusObject(wellKnownName, 
						MediaQueryConstants.OBJECT_PATH,
						sessionId.value,
						new Class<?>[] { MediaQueryInterface.class });
				MediaQueryInterface mediaquery = mProxyObj.getInterface(MediaQueryInterface.class);

				if (mediaquery==null){
					Log.e(TAG, "*** NULL mediaquery interface for service: "+peer);
					return;
				}

				// DEBUG: allocate space
				//mqr.media = new MediaIdentifier[100];

				// call the remote method
				mqr = mediaquery.ListMedia(mtype);

				Log.v(TAG, "doLoadRemoteMedia(): "+mqr.count+" found");
				if (mqr.count>0){
					Log.v(TAG, "[0]"+mqr.media[0].name);

					//int numfiles = (mqr.count>5) ? 5 : mqr.count;
					int numfiles = mqr.count;

					// for each file, copy and save the thumbnail, modify the location in the results and save		
					for (int i=0; i<numfiles; i++){
						MediaIdentifier mi = mqr.media[i];
						String tpath = mi.thumbpath;
						if ((tpath!=null) && (tpath.length()>0)){
							byte[] thumb = new byte[0];
							String lclpath = ThumbnailCache.getThumbnailPath(peer, tpath);
							mi.thumbpath=lclpath;
							// Only retrieve if it's not already in the cache
							//TODO: check age of thumbnail?
							if (!ThumbnailCache.isPresent(lclpath)){
								try{
									thumb = mediaquery.GetThumbnail(mtype, tpath);
									ThumbnailCache.saveThumbnail(peer, lclpath, thumb);
								} catch (Exception e){
									Log.e(TAG, "Error getting thumbnail");
									mi.thumbpath = "";
								}
							}
						}
						mi.mediatype = mtype;

						// Set the path of where he media file would be saved, if it is copied anyway
						//mi.localpath = MediaCache.getMediaPath(peer, mtype, mi.name);

						// add to the descriptor
						mqrd.add(mi);

						// signal availability
						//mMediaQueryControlInterface.MediaItemAvailable(transaction, service, mtype, mi);
						MediaQueryAPIImpl.mediaQueryCallback.onItemAvailable("media", mi);
						Log.v(TAG, "Sent: MediaItemAvailable("+mi.title+")");

					}

					// save the list to local cache
					Log.v(TAG, "Saving query results");

					// Send a Signal notifying availability
					//MediaQueryCache.saveMediaQuery(service, mtype, mqrd.toString());
					//mMediaQueryControlInterface.MediaQueryComplete(transaction, service, mtype);
					//MediaQueryAPIImpl.mediaQueryCallback.onTransferComplete("media", "", mtype, "");
					//Log.v(TAG, "Sent MediaQueryComplete Signal");
					alljoynContainer.getBusAttachment().leaveSession(sessionId.value);
				}
			}
		} catch (Exception e){
			Log.e(TAG, "Error getting media list: "+e.toString());
			Utilities.logException(TAG, "Full error: ", e);
		}


	}



	////////////////////////////////////////////////////////////////////////////
	// Copying of remote media
	////////////////////////////////////////////////////////////////////////////


	// get remote file and send signal when complete
	private void doRequestFile(int transaction, String service, String mtype, String path){
		Log.v(TAG, "doRequestFile("+service+", "+mtype+", "+path+")");

		// make sure cache is initialised
		MediaCache.init();

		// get path where file will be stored locally
		String localpath = MediaCache.getMediaPath(service, mtype, path);


		try {
			if ((MediaCache.isFilePresent(localpath)) && !(MediaCache.isFileOlderThan(localpath, 30))){
				Log.d(TAG, "File already present, not re-copying from remote device");
				MediaQueryAPIImpl.mediaQueryCallback.onTransferComplete(service, path, mtype, localpath);
				//mMediaQueryControlInterface.MediaFileTransferAvailable(transaction, service, mtype, path, localpath);
				Log.v(TAG, "sending onTransferComplete("+service+", "+mtype+")");

			} else {

				// setup a session to the remote service
				String wellKnownName = getServiceName(service);
				SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
				Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
				Log.d(TAG, "About to join session"+wellKnownName+" ...");
				alljoynContainer.getSessionManager().joinSession(wellKnownName, MediaQueryConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());

				// get the proxy object and interfaces
				ProxyBusObject mProxyObj =  alljoynContainer.getBusAttachment().getProxyBusObject(wellKnownName, 
						MediaQueryConstants.OBJECT_PATH,
						sessionId.value,
						new Class<?>[] { MediaQueryInterface.class, MediaQueryFileTransferInterface.class });

				MediaQueryInterface             mediaquery   = mProxyObj.getInterface(MediaQueryInterface.class);
				MediaQueryFileTransferInterface filetransfer = mProxyObj.getInterface(MediaQueryFileTransferInterface.class);

				// get the file from the remote device
				if (getFile(filetransfer, service, path, localpath)){

					// Get the MediaIdentifier for this file
					MediaIdentifier lclmi = new MediaIdentifier();
					MediaIdentifier remmi = new MediaIdentifier();

					// Query the remote device for the list of media items
					try {
						Log.d(TAG, "Getting MediaIdentifier for: "+path);

						// call the remote method
						remmi = mediaquery.GetMediaIdentifier(mtype, path);

						// reset the path to the local location
						lclmi = remmi;
						lclmi.localpath = localpath;

						// get the thumbnail
						String tpath = lclmi.thumbpath;
						if ((tpath!=null) && (tpath.length()>0)){
							byte[] thumb = new byte[0];
							String lcltpath = ThumbnailCache.getThumbnailPath(service, tpath);
							lclmi.thumbpath=lcltpath;
							// Only retrieve if it's not already in the cache
							//TODO: check age of thumbnail?
							if (!ThumbnailCache.isPresent(lcltpath)){
								try{
									thumb = mediaquery.GetThumbnail(mtype, tpath);
									if (thumb.length>0){
										ThumbnailCache.saveThumbnail(service, lcltpath, thumb);
									}
								} catch (Exception e){
									Log.e(TAG, "Error getting thumbnail");
									lclmi.thumbpath = "";
								}
							}

							// For music, the album art is distinct from the audio file, so in that case also
							// copy the retrieved thumbnail to map it to the audio file
							// This is a bit of a hack, but it makes subsequent thumbnail processing
							// consistent across all media types

							if (mtype.equals(MediaQueryConstants.MUSIC)){
								String altthumb = ThumbnailCache.getThumbnailPath(service, localpath);
								if (!ThumbnailCache.isPresent(altthumb)){
									try{
										ThumbnailCache.copyFile(lcltpath, altthumb);
									} catch (Exception e){
										Log.e(TAG, "Error copying music thumbnail");
									}
								}
							}
						}


					} catch (Exception e2){
						Log.w(TAG, "Exception getting media details: "+e2.toString());

						// query didn't work, so just set what we can
						lclmi.mediatype = mtype;
						lclmi.localpath = localpath;
						lclmi.userid = service.substring(service.lastIndexOf(".")+1);
						lclmi.timestamp = Utilities.getTime();

						if (localpath.contains("/")){
							lclmi.title = localpath.substring(localpath.lastIndexOf("/")+1);
						} else {
							lclmi.title = localpath;
						}
						lclmi.name = lclmi.title;
						String ext = lclmi.name.substring(lclmi.name.lastIndexOf(".")+1);
						lclmi.type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);		

					}

					// Record the transaction in the received log
					MediaTransactionCache.saveMediaTransaction(MediaTransactionCache.RECEIVED, localpath, lclmi);
					
					// Success, send Signal to announce availability
					MediaQueryAPIImpl.mediaQueryCallback.onTransferComplete(service, path, mtype, localpath);
					Log.v(TAG, "sending onTransferComplete("+service+", "+mtype+")");

					// Notify User
					Toast.makeText(mContext, "Received file: "+lclmi.title, Toast.LENGTH_SHORT);


				} else {
					// Something happened, send signal to announce error
					Log.e(TAG, "Error getting remote file: "+path);
					MediaQueryAPIImpl.mediaQueryCallback.onTransferError(transaction, service, mtype, localpath);
				}

				// leave the session
				alljoynContainer.getBusAttachment().leaveSession(sessionId.value);

			}
		} catch (Exception e){
			// Something happened, send signal to announce error
			Log.e(TAG, "Error getting remote file ("+path+"): "+e.toString());
			Utilities.logException(TAG, "Fulle error:", e);
			try {
				MediaQueryAPIImpl.mediaQueryCallback.onTransferError(transaction, service, mtype, localpath);
			} catch (Exception e2){
				Log.e(TAG, "Error issuing signal: "+e2.toString());
			}
		}
	}


	////////////////////////////////////////////////////////////////////////////
	// Activation of remote media
	////////////////////////////////////////////////////////////////////////////

	public void doRequestActivation (int transactionId, String peer, String mtype, String path) {
		boolean status = true;
		try {
			String wellKnownName = getServiceName(peer);
			SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
			Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
			Log.d(TAG, "About to join session...");
			alljoynContainer.getSessionManager().joinSession(wellKnownName, MediaQueryConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());
			ProxyBusObject mProxyObj =  alljoynContainer.getBusAttachment().getProxyBusObject(wellKnownName, 
					MediaQueryConstants.OBJECT_PATH,
					sessionId.value,
					new Class<?>[] { MediaQueryInterface.class });
			MediaQueryInterface mediaquery = mProxyObj.getInterface(MediaQueryInterface.class);
			
			Log.i(TAG, "vals: "+alljoynContainer.getUniqueID()+", "+mtype+", "+path);
			boolean success = mediaquery.RequestActivation(alljoynContainer.getUniqueID(), mtype, path);
			JSONObject ret = new JSONObject();
			ret.put("return", success);
			
			MediaQueryAPIImpl.mediaQueryCallback.CallbackJSON(transactionId, getServiceName(), ret.toString());
			alljoynContainer.getBusAttachment().leaveSession(sessionId.value);

		} catch (Exception e){
			e.printStackTrace();
			status = false;
			Log.e(TAG, "Error activating remote file ("+path+"): "+e.toString());
		}

	}


	////////////////////////////////////////////////////////////////////////////
	// Request to Send media to remote device
	////////////////////////////////////////////////////////////////////////////

	public void doSendFileRequest (String service, String mtype, String path) {
		boolean status = true;
		try {
			// setup a session to the remote service
			String wellKnownName = getServiceName(service);
			String session = getSessionName(service);
			SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
			Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
			Log.d(TAG, "About to join session: "+session+" ...");
			alljoynContainer.getSessionManager().joinSession(session, MediaQueryConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());

			// get the proxy object and interface
			Log.v(TAG, "Getting proxy object from: "+wellKnownName);
			ProxyBusObject mProxyObj =  alljoynContainer.getBusAttachment().getProxyBusObject(wellKnownName, 
					MediaQueryConstants.OBJECT_PATH,
					sessionId.value,
					new Class<?>[] { MediaQueryInterface.class });
			MediaQueryInterface mediaquery = mProxyObj.getInterface(MediaQueryInterface.class);

			// issue the send file request
			// NOTE: don't directly transfer using FileTransfer service because of associated media type processing required
			mediaquery.SendFileRequest(getAdvertisedName(), mtype, path);

			// leave the session
			alljoynContainer.getBusAttachment().leaveSession(sessionId.value);

			// Record the transaction in the received log
			MediaIdentifier mi = new MediaIdentifier();
			mi = MediaUtilities.getMediaIdentifier(mContext, mtype, path);

			MediaTransactionCache.saveMediaTransaction(MediaTransactionCache.SENT, path, mi);

		} catch (Exception e){
			e.printStackTrace();
			status = false;
			Log.e(TAG, "Error sending file ("+path+"): "+e.toString());
		}

	}

	@Override
	public void InitAPI(AllJoynContainerInterface coreLogic) {
		// TODO Auto-generated method stub
		this.setupSession();
	}


	// Utilities for formating strings based on mediaquery naming conventions

	// Extract "ID" portion of typical service/session name
	private static String getUserID (String service){
		String id = service;
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
		return id;
	}

	// Utility to create the service name from the session or user ID
	private static String getServiceName(String session){
		return MediaQueryConstants.NAME_PREFIX + "." + getUserID(session);
	}

	// Utility to create the service name from the service, session or user ID
	private static String getSessionName(String service){
		return MediaQueryConstants.SERVICE_NAME + "." + getUserID(service);
	}



	// method to retrieve a full (multi-buffer) file and store in the provided location
	public boolean getFile (MediaQueryFileTransferInterface filetransfer, String service, String remotePath, String localPath){
		boolean status = false;

		boolean fileopen = false;
		int fileid = 0;

		try {
			// Open the remote file
			fileid = filetransfer.Open(remotePath);
			Utilities.logMessage ("Opened transaction for file: "+remotePath+", id: "+fileid);

			FileParameters params = filetransfer.GetFileParameters(fileid);
			if (params.length > 0){
				fileopen = true;
				OutputStream out = new FileOutputStream(localPath);
				Utilities.logMessage ("File "+remotePath+", id: "+fileid+", length: "+params.length);
				long len = 1;
				int seqnum = 0;
				FileBuffer filebuf = new FileBuffer() ;
				filebuf.eof = 0;

				// iterate, getting buffers until end of file indicated
				while (filebuf.eof==0) {
					filebuf = filetransfer.GetBuffer(fileid, seqnum);
					if (filebuf.length>0) {
						//Utilities.logMessage ("File  id: "+fileid+", buffer: "+seqnum+" writen");
						out.write(filebuf.buffer);
						if (seqnum!=filebuf.seqnum){
							Utilities.logMessage("Oops, expected seqnum: "+seqnum+", received: "+filebuf.seqnum);
						}
					}
					seqnum++;
				}
				out.close();
				fileopen = false;
				Utilities.logMessage(localPath+" written.");
			} else {
				Utilities.showError ("zero-length file");
			}

			// close the remote file
			filetransfer.Close(fileid);
			status = true;

		} catch (Exception ex){
			Utilities.logException ("Error getting file: "+remotePath, ex);
			if (fileopen){
				try {
					filetransfer.Close(fileid);
					File file = new File (localPath);
					if (file.exists()){
						file.delete();
					}
					fileopen = false;
				} catch (Exception e){
					//just ignore;
				}
			}
		}

		return status ;
	}//getFile

} //MediaQueryImpl
