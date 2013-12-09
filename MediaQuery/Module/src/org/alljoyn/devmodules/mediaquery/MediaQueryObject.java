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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusInterface;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.devmodules.common.FileBuffer;
import org.alljoyn.devmodules.common.FileParameters;
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaQueryResult;
import org.alljoyn.devmodules.common.MediaUtilities;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;


// Implementation of the "MediaQuery" Service

//@BusInterface (name = "org.alljoyn.devmodules.mediaquery")
public class MediaQueryObject implements MediaQueryInterface, MediaQueryFileTransferInterface, BusObject {


	private static final String TAG = "MediaQueryService";

	private ContentResolver mResolver;
	private Context         mContext;
	private static String   mUserid;
	private static MediaQueryImpl   mMediaQueryImpl = MediaQueryImpl.getInstance();


	public MediaQueryObject (AllJoynContainerInterface alljoynContainer, Context context) {
		mResolver = context.getContentResolver();
		mContext  = context;

		// Get the unique userid for later use in filling out the data structures
		if ((mUserid==null) || (mUserid.length()==0)){
			mUserid = alljoynContainer.getUniqueID();
		}
		
		if (mMediaQueryImpl==null)   mMediaQueryImpl = MediaQueryImpl.getInstance();
	}


	////////////////////////////////////////////////////////////////////////////////////////
	// GENERIC QUERY INTERFACE HANDLERS
	////////////////////////////////////////////////////////////////////////////////////////

	// Method to list photos hosted by this device
	public  MediaQueryResult ListMedia(String mtype) {
		Log.v(TAG, "listMedia("+mtype+")");

//		MediaQueryCache.init();
//		ThumbnailCache.init();

		MediaQueryResult mqr = new MediaQueryResult();
		mqr.count = 0;
		mqr.media = new MediaIdentifier[0];

		try {
			// call appropriate method based on media type
			if (mtype.equals(MediaQueryConstants.PHOTO)){
				mqr = ListPhotos();

			} else if (mtype.equals(MediaQueryConstants.MUSIC)){
				mqr = ListMusic();

			} else if (mtype.equals(MediaQueryConstants.VIDEO)){
				mqr = ListVideos();

			} else if (mtype.equals(MediaQueryConstants.APP)){
				mqr = ListApplications();

			} else if (mtype.equals(MediaQueryConstants.FILE)){
				Log.v(TAG, "Not implemented yet");

			}else{
				Log.e(TAG, "Unkown Media type: "+mtype);
			}
		} catch (Exception e){
			mqr.count = 0;
			mqr.media = new MediaIdentifier[0];
			Log.e(TAG, "Error processing ListMedia("+mtype+"): "+e.toString());
		}

		Log.v(TAG, "ListMedia() Returning "+mqr.count+" items");
		Log.v(TAG, "ListMedia() [ 0] "+mqr.media[0].title);
		Log.v(TAG, "ListMedia() ["+(mqr.count-1)+"] "+mqr.media[(mqr.count-1)].title);

		return mqr;
	}

	
	

	// Method to get a  MediaIdentifier from this device (usually to get the metadata etc.)
    public MediaIdentifier GetMediaIdentifier(String mtype, String filepath){
    	MediaIdentifier mi = new MediaIdentifier();
    	mi = MediaUtilities.getMediaIdentifier(mContext, mtype, filepath);
    	return mi;
	}
    
    

	// Method to get a  thumbnail from this device
	public byte[] GetThumbnail(String mtype, String filepath) {
		byte [] thumb = new byte[0];
		try {
			// call appropriate method based on media type
			if (mtype.equals(MediaQueryConstants.PHOTO)){
				thumb = GetPhotoThumbnail(filepath);

			} else if (mtype.equals(MediaQueryConstants.MUSIC)){
				thumb = GetMusicThumbnail(filepath);

			} else if (mtype.equals(MediaQueryConstants.VIDEO)){
				thumb = GetVideoThumbnail(filepath);

			} else if (mtype.equals(MediaQueryConstants.APP)){
				thumb = GetApplicationThumbnail(filepath);

			} else if (mtype.equals(MediaQueryConstants.FILE)){
				Log.v(TAG, "Not implemented yet");
			}else{
				Log.e(TAG, "Unkown Media type: "+mtype);
			}
		} catch (Exception e){
			Log.e(TAG, "Error processing GetThumbnail("+mtype+"): "+e.toString());
		}

		return thumb;
	}



	////////////////////////////////////////////////////////////////////////////////////////
	// PHOTOS
	////////////////////////////////////////////////////////////////////////////////////////

	// Method to query the list of photos on this device
	public  MediaQueryResult ListPhotos() {
		Log.v(TAG,"ListPhotos()");

		MediaQueryResult mqr = new MediaQueryResult();
		Cursor cursor=null;
		//ArrayList<String> mlist = new ArrayList<String>();
		ArrayList<MediaIdentifier> mlist = new ArrayList<MediaIdentifier>();
		mlist.clear();
		MediaIdentifier mi ;

		try{
			//TODO BS: Cleanup so that simplier to request FileTransferService
			//FileTransferService ftService = ((FileTransferImpl)CoreLogic.getInstance().getObject(FileTransferConnectorObject.OBJECT_PATH)).getFileTransferService();
			mqr.count=0;

			// Query for all images on external storage     
			//String[] projection = { MediaStore.Images.Media._ID };
			String[] projection = {MediaStore.Images.ImageColumns._ID, 
					MediaStore.Images.ImageColumns.DISPLAY_NAME,
					MediaStore.Images.ImageColumns.MIME_TYPE,
					MediaStore.Images.ImageColumns.SIZE,
					MediaStore.Images.ImageColumns.TITLE,
					//MediaStore.Images.ImageColumns.IS_PRIVATE,
					MediaStore.Images.Media.DATA};
			String selection = "";
			String [] selectionArgs = null;


			Log.d(TAG, "Getting list of Photos");

			//cursor = HandlerList.mUIHandler.getActivity().managedQuery( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 
			cursor = mResolver.query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 

			// Loop through retrieved data and extract photo path
			if ( cursor != null ) {
				//Utilities.dumpCursor(cursor);
				cursor.moveToFirst();
				while (cursor.isAfterLast()==false){
					int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
					String photoFilePath = cursor.getString(columnIndex);
					//Log.d(TAG, "Found: "+photoFilePath);
					// Only add images from the Camera directory
					//TODO: anything on sdcard?
					if (photoFilePath.contains("DCIM")){
						mi = new MediaIdentifier();
						mi.path  = photoFilePath;
						mi.name  = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
						mi.type  = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE));
						mi.size  = cursor.getInt   (cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE));
						mi.title = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.TITLE));
						mi.thumbpath = photoFilePath;
						mi.userid = mUserid;
						mi.mediatype = MediaQueryConstants.PHOTO;
						mi.transactionId = 0;
						
						//checkValues for null
						if(mi.path == null) mi.path = "";
						if(mi.name == null) mi.name = "";
						if(mi.type == null) mi.type = "";
						if(mi.title == null) mi.title = "";
						if(mi.thumbpath == null) mi.thumbpath = "";

						mlist.add(mi);
						//ftService.shareFile(photoFilePath);
						Log.v(TAG, "Found: "+mi.path+"("+mi.name+", "+mi.title+", "+mi.thumbpath+")");
					} else {
						Log.d(TAG, "ListPhotos() Ignoring: "+photoFilePath);
					}
					cursor.moveToNext();
				}
			} else {
				Log.d(TAG, "No Photos found.");     
			} 

		} catch (Exception e){
			Log.e(TAG, "Error processing query: "+e.toString());
		}

		if (cursor!=null)
			cursor.close();

		if (!mlist.isEmpty()){
			Log.d(TAG, "Found "+mlist.size()+" images");
			mqr.count = mlist.size();
			mqr.media = new MediaIdentifier[mlist.size()];
			for (int i=0; i<mlist.size();i++){
				mqr.media[i] = new MediaIdentifier();
				mqr.media[i]=mlist.get(i);
			}
		}

		return mqr;
	}//ListPhotos


	// Called when someone wants to retrieve a photo thumbnail from this device
	/*
	 * @param filepath The location of the photo file. The thumbnail is generated from the file and scaled
	 */
	public byte[] GetPhotoThumbnail(String filepath) {
		// Note: Android thumbnail cache doesn't work often, so just generate from the image file
		return MediaUtilities.getThumbnailFromFile(filepath);
	}




	////////////////////////////////////////////////////////////////////////////////////////
	// MUSIC
	////////////////////////////////////////////////////////////////////////////////////////

	// Method to query the list of music on this device
	public  MediaQueryResult ListMusic() {
		MediaQueryResult mqr = new MediaQueryResult();
		Cursor cursor=null;
		//ArrayList<String> mlist = new ArrayList<String>();
		ArrayList<MediaIdentifier> mlist = new ArrayList<MediaIdentifier>();
		mlist.clear();
		MediaIdentifier mi;

		try{
			mqr.count=0;
			// Query for all images on external storage     
			//String[] projection = { MediaStore.Images.Media._ID };
			//String[] projection = {MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.Media.DATA};
			String[] projection = {MediaStore.Audio.AudioColumns._ID, 
					MediaStore.Audio.AudioColumns.DISPLAY_NAME,
					MediaStore.Audio.AudioColumns.MIME_TYPE,
					MediaStore.Audio.AudioColumns.SIZE,
					MediaStore.Audio.AudioColumns.TITLE,
					MediaStore.Audio.AudioColumns.ALBUM_ID,
					MediaStore.Audio.Media.DATA};
			String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";
			String [] selectionArgs = null;

			Log.d(TAG, "Getting list of Music");

			//cursor = HandlerList.mUIHandler.getActivity().managedQuery( MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 
			cursor = mResolver.query( MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 

			// Loop through retrieved data and extract audio data
			if ( cursor != null ) {
				//Utilities.dumpCursor(cursor);
				cursor.moveToFirst();
				while (cursor.isAfterLast()==false){
					int columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
					String musicFilePath = cursor.getString(columnIndex);
					mi = new MediaIdentifier();
					mi.path  = musicFilePath;
					mi.name  = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME));
					mi.type  = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE));
					mi.size  = cursor.getInt   (cursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE));
					mi.title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
					//mi.mediatype = MediaQueryConstants.MUSIC;

					// Get the album art location
					Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
					Uri uri = ContentUris.withAppendedId(sArtworkUri, Long.parseLong(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))));
					//mi.thumbpath = uri.getPath();
					mi.thumbpath = getRealPathFromURI(uri);
					mi.userid = mUserid;
					Log.v(TAG, "Found: "+mi.path+"("+mi.name+", "+mi.title+", "+mi.thumbpath+")");

					mlist.add(mi);
					cursor.moveToNext();
				}
			} else {
				Log.d(TAG, "No music found.");     
			} 


		} catch (Exception e){
			//Log.e(TAG, "Error processing query: "+e.toString());
			Utilities.logException(TAG, "Error processing query: ", e);
		}

		if (cursor!=null)
			cursor.close();

		if (!mlist.isEmpty()){
			Log.d(TAG, "Found "+mlist.size()+" audio files");
			mqr.count = mlist.size();
			mqr.media = new MediaIdentifier[mlist.size()];
			for (int i=0; i<mlist.size();i++){
				mqr.media[i]=mlist.get(i);
			}
		}

		return mqr;

	}//ListMusic

	// convert (image) URI into filepath
	public String getRealPathFromURI(Uri contentUri) {    
		String path = "";
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = null;
		try{
			cursor = mResolver.query(contentUri, proj, null, null, null);
			if (cursor !=null){
				int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
				if (cursor.moveToFirst()){
					path = cursor.getString(column_index);
				}
			}
		} catch (Exception e){
			Log.e (TAG, "Error retrieving URI path: "+e.toString());
		}
		if (cursor != null) cursor.close();

		return path;
	}

	// Called when someone wants to retrieve a music thumbnail from this device
	/*
	 * @param filepath The location of the album art file. The file is used to generate a scaled bitmap image
	 */
	public byte[] GetMusicThumbnail(String filepath) {
		return MediaUtilities.getThumbnailFromFile(filepath);
	}




	////////////////////////////////////////////////////////////////////////////////////////
	// VIDEOS
	////////////////////////////////////////////////////////////////////////////////////////

	// Method to query the list of videos on this device
	public  MediaQueryResult ListVideos() {
		MediaQueryResult mqr = new MediaQueryResult();
		Cursor cursor=null;
		//ArrayList<String> mlist = new ArrayList<String>();
		ArrayList<MediaIdentifier> mlist = new ArrayList<MediaIdentifier>();
		mlist.clear();
		MediaIdentifier mi;

		try{
			mqr.count=0;
			// Query for all images on external storage     
			//String[] projection = { MediaStore.Images.Media._ID };
			//String[] projection = {MediaStore.Video.VideoColumns._ID, MediaStore.Video.Media.DATA};
			String[] projection = {MediaStore.Video.VideoColumns._ID, 
					MediaStore.Video.VideoColumns.DISPLAY_NAME,
					MediaStore.Video.VideoColumns.MIME_TYPE,
					MediaStore.Video.VideoColumns.SIZE,
					MediaStore.Video.VideoColumns.TITLE,
					MediaStore.Video.Media.DATA};
			String selection = "";
			String [] selectionArgs = null;


			Log.d(TAG, "Getting list of Videos");

			//cursor = HandlerList.mUIHandler.getActivity().managedQuery( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 
			cursor = mResolver.query( MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 
			// Loop through retrieved data and extract photo path
			if ( cursor != null ) {
				//Utilities.dumpCursor(cursor);
				cursor.moveToFirst();
				while (cursor.isAfterLast()==false){
					int columnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
					String videoFilePath = cursor.getString(columnIndex);
					Log.d(TAG, "Found: "+videoFilePath);

					// Don't know why, but some mp3 songs end up in the video database
					if (!videoFilePath.contains(".mp3")){
						mi = new MediaIdentifier();
						mi.path  = videoFilePath;
						mi.name  = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME));
						mi.type  = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.MIME_TYPE));
						mi.size  = cursor.getInt   (cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE));
						mi.title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE));
						mi.thumbpath = videoFilePath;
						mi.userid = mUserid;
						mi.mediatype = MediaQueryConstants.VIDEO;

						Log.v(TAG, "Found: "+mi.path+"("+mi.name+", "+mi.title+", "+mi.thumbpath+")");

						mlist.add(mi);
					}
					cursor.moveToNext();
				}
			} else {
				Log.d(TAG, "No videos found.");     
			} 

		} catch (Exception e){
			Log.e(TAG, "Error processing query: "+e.toString());
		}

		if (cursor!=null)
			cursor.close();

		if (!mlist.isEmpty()){
			Log.d(TAG, "Found "+mlist.size()+" video files");
			mqr.count = mlist.size();
			mqr.media = new MediaIdentifier[mlist.size()];
			for (int i=0; i<mlist.size();i++){
				mqr.media[i]=mlist.get(i);
			}
		}

		return mqr;

	}//ListVideos


	// Called when someone wants to retrieve a Video thumbnail from this device
	/*
	 * @param filepath The location of the video file. The thumbnail is created from this, scaled and returned
	 */
	public byte[] GetVideoThumbnail(String filepath) {

		byte[] thumb = new byte[0];

		try {
			//TODO: cache thumbnails
			Bitmap image = ThumbnailUtils.createVideoThumbnail(filepath, MediaStore.Video.Thumbnails.MICRO_KIND);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();  
			image.compress(Bitmap.CompressFormat.JPEG, 100, baos);   
			thumb = baos.toByteArray();

			// force recycling of bitmap and stream (they can cause out of memory errors)
			image.recycle();
			baos = null;
		} catch (Exception e){
			Log.e (TAG, "Error processing thumbnail: "+e.toString());
		}

		return Utilities.checkByteArray(thumb) ;

	}




	////////////////////////////////////////////////////////////////////////////////////////
	// APPLICATIONS
	////////////////////////////////////////////////////////////////////////////////////////

	// Method to query the list of (user-installed) applications on this device
	public  MediaQueryResult ListApplications() {
		MediaQueryResult mqr = new MediaQueryResult();
		ArrayList<MediaIdentifier> mlist = new ArrayList<MediaIdentifier>();
		mlist.clear();
		mqr.count = 0;
		mqr.media = new MediaIdentifier[0];

		// Scan through the list of installed packages
		PackageManager pm = mContext.getPackageManager();
		List<PackageInfo> packs = pm.getInstalledPackages(0);
		for(int i=0;i<packs.size();i++) {
			PackageInfo p = packs.get(i);

			// ignore system packages
			if (p.versionName != null){
				String loc = p.applicationInfo.publicSourceDir;

				if (!loc.contains("/system")){

					MediaIdentifier mi = new MediaIdentifier();
					mi.title = p.applicationInfo.loadLabel(pm).toString();
					mi.name  = p.packageName;
					mi.path  = p.applicationInfo.publicSourceDir;
					mi.type  = "application/*";
					mi.size  = 0;
					mi.thumbpath = mi.name;
					mi.userid = mUserid;
					mi.mediatype = MediaQueryConstants.APP;

					Log.v(TAG, "Found: "+mi.path+"("+mi.name+", "+mi.title+", "+mi.thumbpath+")");

					mlist.add(mi);
				}
			}
		}  //for

		// build the return list
		if (!mlist.isEmpty()){
			Log.d(TAG, "Found "+mlist.size()+" app files");
			mqr.count = mlist.size();
			mqr.media = new MediaIdentifier[mlist.size()];
			for (int i=0; i<mlist.size();i++){
				mqr.media[i]=mlist.get(i);
			}
		}

		return mqr;
	}//ListApplications



	// Called when someone wants to retrieve an application thumbnail from this device
	/*
	 * @param filepath The location of the file. This is going to be platform specific but likely doesn't matter since 
	 * the path was retrieved from ListApplications() anyway
	 */
	public byte[] GetApplicationThumbnail(String filepath) {
		byte[] thumb = new byte[0];

		try {
			Drawable icon ;
			PackageManager pm = mContext.getPackageManager();
			PackageInfo pack = pm.getPackageInfo(filepath, 0);
			if (pack != null){
				icon = pack.applicationInfo.loadIcon(pm);
				if (icon != null){
					Bitmap image = ((BitmapDrawable) icon).getBitmap();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();  
					image.compress(Bitmap.CompressFormat.JPEG, 100, baos);   
					thumb = baos.toByteArray();

					// force recycling of bitmap and stream (they can cause out of memory errors)
					image.recycle();
					baos = null;
				}
			}
		} catch (Exception e){
			Log.e (TAG, "Error processing thumbnail: "+e.toString());
		}

		return Utilities.checkByteArray(thumb) ;
	}


	////////////////////////////////////////////////////////////////////////////////////////
	// REMOTE ACTIVATION
	////////////////////////////////////////////////////////////////////////////////////////
	public boolean RequestActivation (String requestor, String mtype, String filepath) {
		boolean status = true;
		String mimetype = "*/*";

		try {
			Log.v(TAG,"_+_+_+_+_+_+_+_+_+_+_+_+_+_+");
			Log.v(TAG, "requestor: "+requestor);
			Log.v(TAG, "mtype: "+mtype);
			Log.v(TAG, "Displaying file: "+filepath);
			Log.v(TAG,"_+_+_+_+_+_+_+_+_+_+_+_+_+_+");
			Intent intent = new Intent();
			// For apps, use the package manager, for other types, let the system use the MIME type
			if (mtype.equals(MediaQueryConstants.APP)){
				PackageManager pm = mContext.getPackageManager();
				PackageInfo pack = pm.getPackageInfo(filepath, 0);
				if (pack != null){
					String pname = pack.packageName;
					intent = pm.getLaunchIntentForPackage(pname);
				}
			} else {
				intent = new Intent(Intent.ACTION_VIEW);

				// figure out the MIME type from the file extension
				String ext = filepath.substring(filepath.lastIndexOf(".")+1);
				mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);		
				intent.setDataAndType(Uri.fromFile(new File(filepath)), mimetype);
			}

			// Specify that activity should run as a separate task
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			// Start the viewer for this file and MIME type
			if (intent != null){
				mContext.startActivity(intent);
			} else {
				Log.e(TAG, "Don't know how to handle request for: "+filepath+" ("+mtype+")");
			}

		} catch (Exception e){
			Log.e(TAG, "Error activating: "+filepath+" ("+mimetype+") "+e.toString());
			status = false;
		}

		return status;

	}


	////////////////////////////////////////////////////////////////////////////////////////
	// SEND FILE REQUEST
	////////////////////////////////////////////////////////////////////////////////////////

	// Request to send a file to this device. Really, a request for this device to copy the file
	public void SendFileRequest(String requestor, String mtype, String filepath) throws BusException {

		// Just call the MediaQuery service implementation to get the file
		Log.v(TAG, "SendFileRequest("+requestor+", "+mtype+", "+filepath+")");
		int tid = mMediaQueryImpl.requestMedia(requestor, mtype, filepath);
		//int tid = MediaQueryGlobalData.mMediaQueryImpl.requestMedia(requestor, mtype, filepath);
	}


	////////////////////////////////////////////////////////////////////////////////////////
	// FILE TRANSFER INTERFACE HANDLERS (SHOULD BE REPLACED BY 'REAL' FILE TRANSFER)
	////////////////////////////////////////////////////////////////////////////////////////
	private final int           _BUFSIZE = 12*1024; // Size of data buffer
	private final int           _MAX_FILEID = 1023; // point where fileid wraps

	// Class to hold transaction data for a particular file transfer transaction
	private class FileTransaction {
		String              _file ;          // current filepath
		int                 _length ;        // size (in bytes) of file
		int                 _fileid ;        // id of current file
		int                 _seqnum = 0 ;    // current sequence number
		InputStream         _in ;            // Input Stream for current file
		BufferedInputStream _bin ;           // Buffered Input Stream for current file
		byte []             _buffer ;        // Buffer to hold data
		FileBuffer          _filebuf ;       // buffer returned to caller

	}

	private static HashMap<Integer,FileTransaction> mTransactionList = new HashMap<Integer,FileTransaction>();
	private static int mFileCount = 0; // used to generate fileid

	// Method to open a file. 
	// Positive return value is a file identifier (in case a client has multiple active transfers)
	// Negative return value indicates an error occurred
	public int Open (String filename) {
		int fileid = -1;

		try {
			// do some checks to make sure we can process the file
			File file = new File (filename);
			if (!file.exists()){
				Utilities.showError ("File dos not exist: "+filename);
				fileid = -1;
			} else {
				if (!file.isFile()){
					Utilities.showError ("File is a directory: "+filename);
					fileid = -2;
				} else {
					if (!file.canRead()){
						Utilities.showError ("File not readable: "+filename);
						fileid = -3;
					} else {
						// parameters OK, set up transaction and open the stream
						FileTransaction ftx = new FileTransaction();
						ftx._file = filename ;
						ftx._filebuf = new FileBuffer();
						ftx._filebuf.buffer = new byte[_BUFSIZE];

						try {
							ftx._in = new FileInputStream (filename);
						} catch (FileNotFoundException e) {
							fileid = -4;
							Utilities.logException ("Error creating input steam", e);
						}
						ftx._fileid = mFileCount;
						mFileCount = (mFileCount+1) % _MAX_FILEID ; // next file id, with wraparound
						ftx._bin = new BufferedInputStream (ftx._in);
						ftx._seqnum = 0;
						ftx._length = (int) file.length();
						if (mTransactionList == null){
							mTransactionList = new HashMap<Integer,FileTransaction>();
						}

						if (mTransactionList.containsKey(ftx._fileid)){
							Utilities.logMessage(TAG, "Oops, file transaction already exists! ("+ftx._fileid+")");
						} else {
							fileid = ftx._fileid;
							mTransactionList.put(ftx._fileid, ftx);
							Utilities.logMessage(TAG, "Opened stream for: "+filename+", id: "+ftx._fileid);
						}
					}
				}
			}
		} catch (Exception e){
			Utilities.showError(TAG, "Error Opening file: "+filename);
			fileid = -5;
		}
		return fileid ;
	}

	// Method to get the transfer parameters
	public FileParameters GetFileParameters (int fileid) {
		FileParameters params = new FileParameters();
		params.bufsize = _BUFSIZE;
		if (mTransactionList.containsKey(fileid)){
			FileTransaction ftx = mTransactionList.get(fileid);
			params.curseq  = ftx._seqnum;
			params.length  = ftx._length;
		} else {
			Utilities.logMessage(TAG, "File Transaction not found: "+fileid);
			params.curseq  = 0;
			params.length  = 0;
		}
		return params;
	}

	// Method to get a buffer. Sequence number is supplied by the caller and should be incremented
	// every time a new buffer is requested.
	// the sender will only process the current (maybe a retransmission) or next sequence number
	public FileBuffer GetBuffer(int fileid, int seqnum) {

		FileTransaction ftx ;
		if (mTransactionList.containsKey(fileid)){
			ftx = mTransactionList.get((Integer)fileid);

			//check if this is a repeated request (and not first time through)
			if ((seqnum !=0) && (seqnum<ftx._seqnum)){
				Utilities.logMessage(TAG, "Retransmitting sequnce: "+seqnum);
			} else {
				if (seqnum!=ftx._seqnum){
					Utilities.logMessage(TAG, "Warning: expected seqnum:"+ftx._seqnum+", received:"+seqnum);
				}
				try {
					//TODO: should cope with end of file partial block
					// need to return actual length read along with data
					ftx._filebuf.length = ftx._in.read(ftx._filebuf.buffer);
					Utilities.logMessage(TAG, "GetBuffer() - id: "+ftx._fileid+", segment: "+ftx._seqnum);
					if (ftx._filebuf.length < _BUFSIZE){
						Utilities.logMessage (TAG, "EOF detected after "+(seqnum+1)+" buffers");
						ftx._filebuf.eof = 1;
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					Utilities.logException ("Error reading file.", (Exception) e);
				}
				ftx._filebuf.seqnum = ftx._seqnum;
				ftx._seqnum++;
			}
		} else {
			Utilities.logMessage(TAG, "Transaction not found: "+fileid);
			FileBuffer filebuf = new FileBuffer();
			filebuf.length=0;
			filebuf.eof=1;
			return filebuf;
		}
		return ftx._filebuf;
	}

	// Method to close the identified file
	public int Close(int fileid) {
		int s=-1;
		if (mTransactionList.containsKey(fileid)){
			FileTransaction ftx = mTransactionList.get((Integer)fileid);
			try {
				ftx._bin.close();
				ftx._in.close();
				s = 0;
				mTransactionList.remove(fileid);
				Utilities.logMessage(TAG, "Closed transaction for file: "+ftx._file+", id: "+ftx._fileid);
			} catch (IOException e) {
				Utilities.logException ("Error closing file. ", e);
				s = -1;
			}
		} else {
			Utilities.logMessage(TAG, "Transaction not found: "+fileid);
			s = -2;
		}
		return s;

	}
	
	// Method to abort an active transaction
	public void Abort(int fileid){
		if (mTransactionList.containsKey(fileid)){
			Close(fileid);
		} else {
			Log.w(TAG, "Attempt to abort unkown fileid: "+fileid);
		}
	}
	
	// Method to abort all active transfers
	public void AbortAll(){
		for (Integer fid: mTransactionList.keySet()){
			Close(fid);
		}
	}
	
}//MediaQueryService
