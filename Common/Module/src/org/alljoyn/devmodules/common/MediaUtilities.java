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
package org.alljoyn.devmodules.common;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.alljoyn.storage.ProfileCache;
import org.alljoyn.storage.ThumbnailCache;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

// Set of general purpose utilities related to managing media transfers
public class MediaUtilities {

	private static final String TAG = "MediaUtilities";


	//////////////////////////////////////////////////////////////////////////////
	// THUMBNAIL UTILITIES
	//////////////////////////////////////////////////////////////////////////////

	private static final int THUMB_APPROX_SIZE = 160;

	// Utility to get a scaled thumbnail from an existing file
	// @param mediatype Media Type, as defined in MediaQueryConstants
	public static byte[] getThumbnailFromFile(Context context, String mediatype, String filepath){
		byte[] thumb = new byte[0];
		if (mediatype.equals(MediaTypes.PHOTO)){
			thumb = getImageThumbnail(filepath);

		} else if (mediatype.equals(MediaTypes.MUSIC)){
			thumb = getAudioThumbnail(context.getContentResolver(), filepath);

		} else if (mediatype.equals(MediaTypes.VIDEO)){
			thumb = getVideoThumbnail(filepath);

		} else if (mediatype.equals(MediaTypes.APP)){
			thumb = getAppThumbnail(context, filepath);

		} else if (mediatype.equals(MediaTypes.FILE)){
			Log.v(TAG, "Not implemented yet");

		} else {
			Log.e(TAG, "Unknown media type: "+mediatype);
		}
		return thumb;
	}


	// included for backwards compatibility
	public static byte[] getThumbnailFromFile(String filepath){
		return getImageThumbnail(filepath);
	}


	// Utility to get a scaled thumbnail from an existing image file
	public static byte[] getImageThumbnail(String filepath){
		byte[] pthumb = new byte[0];

		BitmapFactory.Options opts = new BitmapFactory.Options();
		try {

			// get the image size
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filepath, opts);
			//Log.d(TAG, "Size: "+opts.outWidth+"x"+opts.outHeight);

			// scale down based on longest side
			opts.inJustDecodeBounds = false;
			int len = Math.max(opts.outWidth, opts.outHeight);
			int ratio = 1;
			if (len > THUMB_APPROX_SIZE) {

				ratio = findRatio(len, THUMB_APPROX_SIZE);
				Log.d(TAG, "GetThumbnail(). Scaling down ("+opts.outWidth+"x"+opts.outHeight+") ratio: "+ratio);
			}
			opts.inSampleSize = ratio;

			Bitmap image = BitmapFactory.decodeFile(filepath,opts);

			ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
			try{
				image.compress(Bitmap.CompressFormat.JPEG, 50, baos);  
			} catch (Exception ec){
				// just ignore compression errors
			}
			pthumb = baos.toByteArray();

			// force recycling of bitmap and stream (they can cause out of memory errors)
			if (image != null) image.recycle();
			baos = null;
			opts = null;
			//System.gc();
		} catch (Exception e){
			Log.e (TAG, "Error processing thumbnail: "+e.toString());
			//Utilities.logException(TAG, "Error processing thumbnail: "+filepath, e);
		}

		return Utilities.checkByteArray(pthumb) ;

	}

	/* Returns the image associated with an audio file
	 * @param filepath The location of the audio file. The thumbnail is created from this, scaled and returned
	 * @retval byte array of the thumbnail
	 */
	public static byte[] getAudioThumbnail(ContentResolver resolver, String filepath) {

		byte[] thumb = new byte[0];
		Cursor cursor;

		try {
			String[] projection = {MediaStore.Audio.AudioColumns._ID, 
					MediaStore.Audio.AudioColumns.ALBUM_ID,
					MediaStore.Audio.Media.DATA};
			//String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";
			String selection = MediaStore.Audio.Media.DATA + "=\"" + filepath + "\"";
			String [] selectionArgs = null;

			Log.d(TAG, "Getting list of Music");

			//cursor = HandlerList.mUIHandler.getActivity().managedQuery( MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 
			cursor = resolver.query( MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 

			// Loop through retrieved data and extract audio data
			if ( cursor != null ) {
				//Utilities.dumpCursor(cursor);
				if (cursor.moveToFirst()){
					int columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
					String musicFilePath = cursor.getString(columnIndex);

					// Get the album art location
					Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
					Uri uri = ContentUris.withAppendedId(sArtworkUri, Long.parseLong(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))));
					String thumbpath = getImagePathFromURI(resolver, uri);
					thumb = getThumbnailFromFile(thumbpath);
				}
			}
		} catch (Exception e){
			Log.e (TAG, "Error getting audio thumbnail: "+e.toString());
		}

		return Utilities.checkByteArray(thumb) ;

	}


	/* Returns the image associated with a video file
	 * @param filepath The location of the video file. The thumbnail is created from this, scaled and returned
	 * @retval byte array of the thumbnail
	 */
	public static byte[] getVideoThumbnail(String filepath) {

		byte[] thumb = new byte[0];

		try {
			Bitmap image = ThumbnailUtils.createVideoThumbnail(filepath, MediaStore.Video.Thumbnails.MICRO_KIND);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();  
			image.compress(Bitmap.CompressFormat.JPEG, 50, baos);   
			thumb = baos.toByteArray();

			// force recycling of bitmap and stream (they can cause out of memory errors)
			image.recycle();
			baos = null;
		} catch (Exception e){
			Log.e (TAG, "Error getting video thumbnail: "+e.toString());
		}

		return Utilities.checkByteArray(thumb) ;

	}


	/* Returns the image associated with an app file
	 * @param filepath The location of the app file. The thumbnail is retrieved from this, scaled and returned
	 * @retval byte array of the thumbnail
	 */
	public static byte[] getAppThumbnail(Context context, String filepath) {

		byte[] thumb = new byte[0];

		try {

			// remove path, if any
			String fpath = filepath;
			/***
			if (fpath.contains("/")){
				fpath = fpath.substring(fpath.lastIndexOf("/")+1);
			}
			***/

			Log.v(TAG, "Loading app icon for: "+fpath);
			Drawable icon ;
			PackageManager pm = context.getPackageManager();
			PackageInfo pack;

			if (fpath.contains(".apk")){
				pack = pm.getPackageArchiveInfo(fpath, 0);
				fpath = pack.packageName;
			} 
			pack = pm.getPackageInfo(fpath, 0);

			
			if (pack != null){
				icon = pack.applicationInfo.loadIcon(pm);
				if (icon != null){
					Bitmap image = ((BitmapDrawable) icon).getBitmap();
					ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
					try {
						image.compress(Bitmap.CompressFormat.JPEG, 50, baos);  
					} catch (Exception ec){
						// just ignore
					}
					thumb = baos.toByteArray();

					// force recycling of bitmap and stream (they can cause out of memory errors)
					image.recycle();
					baos = null;
				}
			} else{
				Log.d(TAG, "icon not found for: "+fpath);
			}
		} catch (Exception e){
			Log.e (TAG, "Error getting app thumbnail: "+e.toString());
		}

		return Utilities.checkByteArray(thumb) ;

	}

	// Utility to calculate scaling ratio based on desired approximate size. Returns the nearest power of 2
	public static int findRatio (int size, int desire){
		int ratio = 1;
		if (size > desire) {
			//ratio = (size+desire-1) / desire;
			ratio = size / desire;
			// calculate nearest power of 2 for efficient scaling
			int v = ratio;
			v--;
			v |= v >> 1;
			v |= v >> 2;
			v |= v >> 4;
			v |= v >> 8;
			v |= v >> 16;
			v++;
			ratio = v;
		}
		return ratio;
	}


	//////////////////////////////////////////////////////////////////////////////
	// URI UTILITIES
	//////////////////////////////////////////////////////////////////////////////


	// convert media URI into filepath
	public static String getMediaPathFromURI(ContentResolver resolver, String mtype, Uri uri) { 
		String path = "";
		try {
			if (mtype.equals(MediaTypes.PHOTO)){
				path = getImagePathFromURI(resolver, uri);

			} else if (mtype.equals(MediaTypes.MUSIC)){
				path = getAudioPathFromURI(resolver, uri);

			} else if (mtype.equals(MediaTypes.VIDEO)){
				path = getVideoPathFromURI(resolver, uri);

			} else if (mtype.equals(MediaTypes.APP)){
				path = uri.getEncodedPath();

			} else if (mtype.equals(MediaTypes.FILE)){
				path = uri.getEncodedPath();

			} else {
				Log.e(TAG, "Unknown media type: "+mtype);
			}
		} catch (Exception e){
			Log.e(TAG, "Error getting path from URI: "+uri+"   "+e.toString());
		}
		return path;
	}


	// convert (image) URI into filepath
	public static String getImagePathFromURI(ContentResolver resolver, Uri imageUri) {    
		String path = "";
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = null;
		try{
			cursor = resolver.query(imageUri, proj, null, null, null);
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


	// convert (audio) URI into filepath
	public static String getAudioPathFromURI(ContentResolver resolver, Uri audioUri) {    
		String path = "";
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = null;
		try{
			cursor = resolver.query(audioUri, proj, null, null, null);
			if (cursor !=null){
				int column_index = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
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


	// convert (video) URI into filepath
	public static String getVideoPathFromURI(ContentResolver resolver, Uri videoUri) {    
		String path = "";
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = null;
		try{
			cursor = resolver.query(videoUri, proj, null, null, null);
			if (cursor !=null){
				int column_index = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
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


	//////////////////////////////////////////////////////////////////////////////
	// MEDIA IDENTIFIER (META DATA) UTILITIES
	//////////////////////////////////////////////////////////////////////////////

	private static String mUserid=null;

	// Return MediaIdentifier (metadata) for supplied file/type
	public static MediaIdentifier getMediaIdentifier(Context context, String mtype, String filepath){
		MediaIdentifier mi = new MediaIdentifier();


		// Get the unique userid for later use in filling out the data structures
		if ((mUserid==null) || (mUserid.length()==0)){
			mUserid = ProfileCache.retrieveName();
		}


		try {

			/****
			// first, see if the MediaIdentifier is already stored. If so, just use it
			if (MediaTransactionCache.isPresent(MediaTransactionCache.SENT, filepath)){
				mi = MediaTransactionCache.retrieveMediaTransaction(MediaTransactionCache.SENT, filepath);

			} else if (MediaTransactionCache.isPresent(MediaTransactionCache.RECEIVED, filepath)){
				mi = MediaTransactionCache.retrieveMediaTransaction(MediaTransactionCache.RECEIVED, filepath);

			} else {
			 ****/

			// OK, MediaIdentifier not already stored so try to build it from the filepath

			if (mtype.equals(MediaTypes.PHOTO)){
				mi = getPhotoMediaIdentifier(context, filepath);

			} else if (mtype.equals(MediaTypes.MUSIC)){
				mi = getAudioMediaIdentifier(context, filepath);

			} else if (mtype.equals(MediaTypes.VIDEO)){
				mi = getVideoMediaIdentifier(context, filepath);

			} else if (mtype.equals(MediaTypes.APP)){
				mi = getAppMediaIdentifier(context, filepath);

			} else if (mtype.equals(MediaTypes.FILE)){
				mi = getFileMediaIdentifier(context, filepath);

			} else {
				Log.e(TAG, "Unknown media type: "+mtype);
			}
			/****
			}
			 ****/

		} catch (Exception e){
			Log.e(TAG, "Exception getting MediaIdentifier for: "+filepath+" ("+mtype+"): "+e.toString());
			mi = getDefaultMediaIdentifier(filepath);
		}

		// Make sure Userid reflects this device (not destination)
		mi.userid = mUserid;

		// set timestamp
		mi.timestamp = Utilities.getTime();

		return mi;
	}


	// Return MediaIdentifier (metadata) for supplied image file
	public static MediaIdentifier getPhotoMediaIdentifier(Context context, String filepath){
		MediaIdentifier mi = new MediaIdentifier();
		try {
			Cursor cursor=null;
			ContentResolver resolver = context.getContentResolver();

			// Data to be returned    
			String[] projection = {MediaStore.Images.ImageColumns._ID, 
					MediaStore.Images.ImageColumns.DISPLAY_NAME,
					MediaStore.Images.ImageColumns.MIME_TYPE,
					MediaStore.Images.ImageColumns.SIZE,
					MediaStore.Images.ImageColumns.TITLE,
					MediaStore.Images.Media.DATA};

			// Matching query (WHERE clause)
			String selection = MediaStore.Images.Media.DATA + "=\"" + filepath + "\"";
			String [] selectionArgs = null;


			cursor = resolver.query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 

			// Loop through retrieved data and extract photo path
			if ( cursor != null ) {
				//Utilities.dumpCursor(cursor);
				cursor.moveToFirst();
				int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
				String photoFilePath = cursor.getString(columnIndex);
				//Log.d(TAG, "Found: "+photoFilePath);
				mi = new MediaIdentifier();
				mi.path  = photoFilePath;
				mi.localpath = photoFilePath;
				mi.name  = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME));
				mi.type  = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.MIME_TYPE));
				mi.size  = cursor.getInt   (cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE));
				mi.title = cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.TITLE));
				mi.thumbpath = photoFilePath;
				mi.mediatype = MediaTypes.PHOTO;

				Log.v(TAG, "Found: "+mi.path+"("+mi.name+", "+mi.title+", "+mi.thumbpath+")");
			} else {
				Log.d(TAG, "No Photos found.");   
				// Just set up what we can from the file name alone
				mi = getDefaultMediaIdentifier(filepath);
			} 

			if (cursor!=null)
				cursor.close();

		} catch (Exception e){
			Log.e(TAG, "Error getting MediaIdentifier for: "+filepath);
			mi = getDefaultMediaIdentifier(filepath);
		}
		return mi;
	}


	// Return MediaIdentifier (metadata) for supplied audio file
	public static MediaIdentifier getAudioMediaIdentifier(Context context, String filepath){
		MediaIdentifier mi = new MediaIdentifier();
		try {
			Cursor cursor=null;
			ContentResolver resolver = context.getContentResolver();

			// URI to query
			Uri audioUri;
			audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			//audioUri = Uri.fromFile(new File(filepath));

			// Data to be returned    
			String[] projection = {MediaStore.Audio.AudioColumns._ID, 
					MediaStore.Audio.AudioColumns.DISPLAY_NAME,
					MediaStore.Audio.AudioColumns.MIME_TYPE,
					MediaStore.Audio.AudioColumns.SIZE,
					MediaStore.Audio.AudioColumns.TITLE,
					MediaStore.Audio.AudioColumns.ALBUM_ID,
					MediaStore.Audio.Media.DATA};

			// Matching query (WHERE clause)
			String selection = MediaStore.Audio.Media.DATA + "=\"" + filepath + "\"";
			//String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";

			// Qualifiers (SORT etc.)
			String [] selectionArgs = null;

			Log.d(TAG, "Querying data for: "+audioUri);
			cursor = resolver.query( MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 
			//cursor = resolver.query( audioUri, projection, null, null, null ); 

			// extract audio data
			if ( cursor != null ) {
				//Utilities.dumpCursor(cursor);
				cursor.moveToFirst();
				int columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
				String musicFilePath = cursor.getString(columnIndex);
				mi.path  = musicFilePath;
				mi.localpath  = musicFilePath;
				mi.name  = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME));
				mi.type  = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.MIME_TYPE));
				mi.size  = cursor.getInt   (cursor.getColumnIndex(MediaStore.Audio.AudioColumns.SIZE));
				mi.title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
				mi.mediatype = MediaTypes.MUSIC;

				// Get the album art location
				Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
				Uri uri = ContentUris.withAppendedId(sArtworkUri, Long.parseLong(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID))));
				//mi.thumbpath = uri.getPath();
				mi.thumbpath = getImagePathFromURI(resolver, uri);
				mi.userid = mUserid;
				Log.v(TAG, "Found: "+mi.path+"("+mi.name+", "+mi.title+", "+mi.thumbpath+")");
			} else {
				Log.d(TAG, "No music found.");     
				// Just set up what we can from the file name alone
				mi = getDefaultMediaIdentifier(filepath);
			} 


			if (cursor!=null)
				cursor.close();

		} catch (Exception e){
			Log.e(TAG, "Error getting MediaIdentifier for: "+filepath);
			mi = getDefaultMediaIdentifier(filepath);
		}
		return mi;
	}


	// Return MediaIdentifier (metadata) for supplied video file
	public static MediaIdentifier getVideoMediaIdentifier(Context context, String filepath){
		MediaIdentifier mi = new MediaIdentifier();
		try {
			Cursor cursor=null;
			ContentResolver resolver = context.getContentResolver();


			// Data to be returned
			String[] projection = {MediaStore.Video.VideoColumns._ID, 
					MediaStore.Video.VideoColumns.DISPLAY_NAME,
					MediaStore.Video.VideoColumns.MIME_TYPE,
					MediaStore.Video.VideoColumns.SIZE,
					MediaStore.Video.VideoColumns.TITLE,
					MediaStore.Video.Media.DATA};

			// Matching query (WHERE clause)
			String selection = MediaStore.Video.Media.DATA + "=\"" + filepath + "\"";

			String [] selectionArgs = null;

			cursor = resolver.query( MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null ); 

			if ( cursor != null ) {
				//Utilities.dumpCursor(cursor);
				cursor.moveToFirst();
				int columnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
				String videoFilePath = cursor.getString(columnIndex);
				Log.d(TAG, "Found: "+videoFilePath);

				// Don't know why, but some mp3 songs end up in the video database
				mi.path  = videoFilePath;
				mi.localpath  = videoFilePath;
				mi.name  = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME));
				mi.type  = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.MIME_TYPE));
				mi.size  = cursor.getInt   (cursor.getColumnIndex(MediaStore.Video.VideoColumns.SIZE));
				mi.title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.VideoColumns.TITLE));
				mi.thumbpath = videoFilePath;
				mi.mediatype = MediaTypes.VIDEO;

				Log.v(TAG, "Found: "+mi.path+"("+mi.name+", "+mi.title+", "+mi.thumbpath+")");
			} else {
				Log.d(TAG, "Video not found: "+filepath);
				// Just set up what we can from the file name alone
				mi = getDefaultMediaIdentifier(filepath);

			}

			if (cursor!=null)
				cursor.close();

		} catch (Exception e){
			Log.e(TAG, "Error getting MediaIdentifier for: "+filepath);
			mi = getDefaultMediaIdentifier(filepath);
		}
		return mi;
	}


	// Return MediaIdentifier (metadata) for supplied app file
	public static MediaIdentifier getAppMediaIdentifier(Context context, String filepath){
		MediaIdentifier mi = new MediaIdentifier();
		try {
			// Scan through the list of installed packages
			// TODO: find more efficient algorithm
			PackageManager pm = context.getPackageManager();

			List<PackageInfo> packs = pm.getInstalledPackages(0);
			for(int i=0;i<packs.size();i++) {
				PackageInfo p = packs.get(i);

				// ignore system packages
				if (p.versionName != null){
					String loc = p.applicationInfo.publicSourceDir;

					if (!loc.contains("/system")){

						// OK, see if path matches
						if (filepath.equals(p.applicationInfo.publicSourceDir)){
							mi.title = p.applicationInfo.loadLabel(pm).toString();
							mi.name  = p.packageName;
							mi.path  = p.applicationInfo.publicSourceDir;
							mi.localpath  = p.applicationInfo.publicSourceDir;
							mi.type  = "application/*";
							mi.size  = 0;
							mi.thumbpath = mi.name;
							mi.userid = mUserid;
							mi.mediatype = MediaTypes.APP;

							Log.v(TAG, "Found: "+mi.path+"("+mi.name+", "+mi.title+", "+mi.thumbpath+")");
						}
					}
				}
			}  //for

		} catch (Exception e){
			Log.e(TAG, "Error getting MediaIdentifier for: "+filepath);
			mi = getDefaultMediaIdentifier(filepath);
		}
		return mi;
	}


	// Return MediaIdentifier (metadata) for supplied generic file
	public static MediaIdentifier getFileMediaIdentifier(Context context, String filepath){
		MediaIdentifier mi = new MediaIdentifier();
		try {
			String mtype = getMediaType(filepath);

			// if type is known, then re-scan based on media type (be careful calling this routine!)
			if (!mtype.equals(MediaTypes.FILE)){
				mi = getMediaIdentifier(context, mtype, filepath);
			} else {
				mi = getDefaultMediaIdentifier(filepath);
			}

		} catch (Exception e){
			Log.e(TAG, "Error getting MediaIdentifier for: "+filepath);
			mi = getDefaultMediaIdentifier(filepath);
		}
		return mi;
	}


	// Return MediaIdentifier (metadata) when we have no additional information, just the filename
	public static MediaIdentifier getDefaultMediaIdentifier(String filepath){
		MediaIdentifier mi = new MediaIdentifier();
		try {
			File f = new File (filepath);

			// for now, just set up fields derived from path
			String fname = filepath;

			// remove path prefix (if any)
			if (fname.contains("/")){
				fname = fname.substring(fname.lastIndexOf("/")+1);
			}
			// remove userid prefix (if any)
			if (fname.contains("_")){
				fname = fname.substring(fname.indexOf("_")+1);
			}

			mi.name  = fname;
			mi.path  = filepath;

			// set title to filename minus extension
			mi.title = fname;
			if (mi.title.contains("."))
				mi.title = mi.title.substring(mi.title.lastIndexOf(".")+1);

			mi.type  = "file/*";
			mi.thumbpath = mi.name;
			mi.mediatype = getMediaType (filepath);
			mi.size  = (int)f.length();

			// check to see if there is a thumbnail for this file
			String thumbpath = ThumbnailCache.getThumbnailPath(mUserid, filepath);
			if (ThumbnailCache.isFilePresent(thumbpath)){
				mi.thumbpath = thumbpath;
			}

		} catch (Exception e){
			Log.e(TAG, "Error getting MediaIdentifier for: "+filepath);
		}
		return mi;
	}



	/////////////////////////////////////////////////////////////////////////////////
	// MEDIA REGISTRATION UTILITIES
	/////////////////////////////////////////////////////////////////////////////////

	// Register media with the 'system' based on data in Media Identifier
	public static void registerMedia(Context context, MediaIdentifier mi){

		try {

			String mtype = mi.mediatype;
			if (mtype.equals(MediaTypes.PHOTO)){
				registerPhoto(context, mi);

			} else if (mtype.equals(MediaTypes.MUSIC)){
				registerAudio(context, mi);

			} else if (mtype.equals(MediaTypes.VIDEO)){
				registerVideo(context, mi);

			} else if (mtype.equals(MediaTypes.APP)){
				registerApp(context, mi);

			} else if (mtype.equals(MediaTypes.FILE)){
				registerFile(context, mi);

			} else {
				Log.e(TAG, "Unknown media type: "+mtype);
			}

		} catch (Exception e){
			Log.e(TAG, "Error registering Media ("+mi.name+"): "+e.toString());
		}

	}




	// Register image with the 'system' based on data in Media Identifier
	public static void registerPhoto(Context context, MediaIdentifier mi){

		// for images, just need to scan the file
		try {
			scanMediaFile (context, mi.localpath);

			/***
			// update the content provider
			ContentValues values = new ContentValues();
			values.put(MediaStore.MediaColumns.DATA,         mi.localpath);  
			values.put(MediaStore.MediaColumns.TITLE,        mi.title);  
			values.put(MediaStore.MediaColumns.MIME_TYPE,    mi.type);  
			values.put(MediaStore.MediaColumns.DISPLAY_NAME, mi.name);  
			values.put(MediaStore.MediaColumns.DATE_ADDED,   Utilities.getTime());  
			values.put(MediaStore.MediaColumns.SIZE,         mi.size);  

			values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, mi.name);  
			values.put(MediaStore.Images.ImageColumns.TITLE,        mi.title);  
			values.put(MediaStore.Images.ImageColumns.MIME_TYPE,    mi.type);  
			values.put(MediaStore.Images.ImageColumns.SIZE,         mi.size);  

			ContentResolver resolver = context.getContentResolver();
			resolver.insert(MediaStore.Images.Media.getContentUriForPath(mi.localpath), values);
			***/
			MediaStore.Images.Media.insertImage(context.getContentResolver(), mi.localpath, mi.name, mi.title);
		} catch (Exception e){
			Log.e(TAG, "Error registering Media ("+mi.name+"): "+e.toString());
		}
	}



	// Register audio with the 'system' based on data in Media Identifier
	// Audio is a little different because of the artwork etc.
	public static void registerAudio(Context context, MediaIdentifier mi){


		try {
			// scan the media file (this is asynchronous)
			scanMediaFile (context, mi.localpath);

			// update the content provider
			ContentValues values = new ContentValues();
			values.put(MediaStore.MediaColumns.DATA,         mi.localpath);  
			values.put(MediaStore.MediaColumns.TITLE,        mi.title);  
			values.put(MediaStore.MediaColumns.MIME_TYPE,    mi.type);  
			values.put(MediaStore.MediaColumns.DISPLAY_NAME, mi.name);  
			values.put(MediaStore.MediaColumns.DATE_ADDED,   Utilities.getTime());  
			values.put(MediaStore.MediaColumns.SIZE,         mi.size);  

			values.put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, mi.name);  
			values.put(MediaStore.Audio.AudioColumns.TITLE,        mi.title);  
			values.put(MediaStore.Audio.AudioColumns.MIME_TYPE,    mi.type);  
			values.put(MediaStore.Audio.AudioColumns.SIZE,         mi.size);  
			//values.put(MediaStore.Audio.Media.ARTIST, mi.artist);  
			values.put(MediaStore.Audio.Media.IS_RINGTONE,         false);  
			values.put(MediaStore.Audio.Media.IS_NOTIFICATION,     true);  
			values.put(MediaStore.Audio.Media.IS_ALARM,            false);  
			values.put(MediaStore.Audio.Media.IS_MUSIC,            true); 

			//TODO: deal with album art
			// long aid = ???;
			//values.put(MediaStore.Audio.AudioColumns.ALBUM_ID, aid);
			// Apparently, album art was never stored
			// Removed in Android JB
			//values.put(MediaStore.Audio.AudioColumns.ALBUM_ART, 
			//		MediaStore.Audio.Media.getContentUriForPath(mi.thumbpath).toString());
			ContentResolver resolver = context.getContentResolver();
			resolver.insert(MediaStore.Audio.Media.getContentUriForPath(mi.localpath), values);


		} catch (Exception e){
			Log.e(TAG, "Error registering Audio ("+mi.name+"): "+e.toString());
			Utilities.logException(TAG, "Full Error: ", e);
		}
	}



	// Register image with the 'system' based on data in Media Identifier
	public static void registerVideo(Context context, MediaIdentifier mi){

		// for images, just need to scan the file
		try {
			scanMediaFile (context, mi.localpath);
			// update the content provider
			ContentValues values = new ContentValues();
			values.put(MediaStore.MediaColumns.DATA,         mi.localpath);  
			values.put(MediaStore.MediaColumns.TITLE,        mi.title);  
			values.put(MediaStore.MediaColumns.MIME_TYPE,    mi.type);  
			values.put(MediaStore.MediaColumns.DISPLAY_NAME, mi.name);  
			values.put(MediaStore.MediaColumns.DATE_ADDED,   Utilities.getTime());  
			values.put(MediaStore.MediaColumns.SIZE,         mi.size);  

			values.put(MediaStore.Video.VideoColumns.DISPLAY_NAME, mi.name);  
			values.put(MediaStore.Video.VideoColumns.TITLE,        mi.title);  
			values.put(MediaStore.Video.VideoColumns.MIME_TYPE,    mi.type);  
			values.put(MediaStore.Video.VideoColumns.SIZE,         mi.size);  

			ContentResolver resolver = context.getContentResolver();
			resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
		} catch (Exception e){
			Log.e(TAG, "Error registering Media ("+mi.name+"): "+e.toString());
		}
	}



	// Register app with the 'system' based on data in Media Identifier
	// This will likely cause the app to be installed
	public static void registerApp(Context context, MediaIdentifier mi){

		// for images, just need to scan the file
		try {
			scanMediaFile (context, mi.localpath);
		} catch (Exception e){
			Log.e(TAG, "Error registering Media ("+mi.name+"): "+e.toString());
		}
	}



	// Register generic file with the 'system' (may not do anything)
	public static void registerFile(Context context, MediaIdentifier mi){

		// attempt to scan the file. There may be something installed to handle the type of media
		try {
			scanMediaFile (context, mi.localpath);
		} catch (Exception e){
			Log.e(TAG, "Error registering Media ("+mi.name+"): "+e.toString());
		}
	}



	// Register image with the 'system' based on data in Media Identifier
	public static void scanMediaFile(Context context, String path){

		try {

			// MediaScanner not working in Android 4.x, try Intent method
			
			Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			intent.setData(Uri.fromFile(new File(path)));
			context.sendBroadcast(intent);

				
			/*** This stopped working in ANdroid JellyBean...
			// Tell the media scanner about the new file so that it is available to the user.
			Log.d(TAG, "Registering media file: "+path);
			MediaScannerConnection.scanFile(context, new String[] { path }, null,
					new MediaScannerConnection.OnScanCompletedListener() {
				public void onScanCompleted(String path, Uri uri) {
					Log.i("ExternalStorage", "Scanned " + path + ":");
					Log.i("ExternalStorage", "-> uri=" + uri);
				}
			});
			***/

		} catch (Exception e){
			Log.e(TAG, "Error registering Media ("+path+"): "+e.toString());
			Utilities.logException(TAG, "Full Error: ", e);
		}

	}


	/////////////////////////////////////////////////////////////////////////////////
	// MISCELLANEOUS MEDIA-RELATED UTILITIES
	/////////////////////////////////////////////////////////////////////////////////

	// Return the Media Type from the file extension
	public static String getMediaType (String filename){
		String mtype = MediaTypes.FILE;
		// look up the mimetype and check for media type
		String mimetype = getMimeType (filename);
		if (mimetype !=null){
			if (mimetype.contains("image")){
				mtype = MediaTypes.PHOTO;

			} else if (mimetype.contains("audio")){
				mtype = MediaTypes.MUSIC;	

			} else if (mimetype.contains("video")){
				mtype = MediaTypes.MUSIC;	

			} else if (mimetype.contains("application")){
				mtype = MediaTypes.APP;				
			}
		}
		return mtype;
	}


	// Return the MIME Type from the file extension
	public static String getMimeType (String filename){
		String mimetype = "file/*";

		try {
			// figure out the MIME type from the file extension
			String ext = filename.substring(filename.lastIndexOf(".")+1);
			mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);		

		} catch (Exception e){
			mimetype = "file/*";
		}
		return mimetype;
	}



	// Returns the recommended external storage location based on the supplied media tpe
	public static String getStorageLocation (String mtype){
		File path;

		// Default value:
		path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

		try {
			if (mtype.equals(MediaTypes.PHOTO)){
				path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

			} else if (mtype.equals(MediaTypes.MUSIC)){
				path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

			} else if (mtype.equals(MediaTypes.VIDEO)){
				path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);

			} else if (mtype.equals(MediaTypes.APP)){
				path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

			} else if (mtype.equals(MediaTypes.FILE)){
				path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

			} else {
				Log.e(TAG, "Unknown media type: "+mtype);
			}
		} catch (Exception e){
			Log.e(TAG, "Error getting path for type: "+mtype+"   "+e.toString());
		}

		return path.getPath();
	}


	// retrieve the path for stored thumbnail associated with a media file
	// (assumes this is a downloaded file)
	public static String getThumbnailPath(String file){
		String path = "";
		//TODO: doesn't work: userid?!
		path = ThumbnailCache.getThumbnailPath(file);
		Log.v(TAG, "Thumbnail ("+file+"): "+path);
		return path;
	}


}
