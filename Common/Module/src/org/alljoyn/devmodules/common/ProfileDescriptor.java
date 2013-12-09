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
import java.io.FileOutputStream;

import org.alljoyn.storage.ProfileCache;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;


/*
 * Class to hold profile data 
 * The data can be stored/retrieved as a String in JSON format
 * To retrieve a field, use ProfileDescriptor.getField(<name>) (Strings only)
 * This version is based on the CoPS ProfileManager component
 */



public class ProfileDescriptor  {

	private static final String TAG = "ProfileDescriptor";
	public static final String EMPTY_PROFILE_STRING = "{}";

	private String      _JSONstring ;
	private JSONObject  _JSONobject ;

	public static class ProfileFields {
		public static final String _ID = "profileId";
		public static final String EMAIL_HOME = "Email, Home"; 
		public static final String EMAIL_WORK = "Email, Work"; 
		public static final String EMAIL_OTHER = "Email, Other"; 
		public static final String EMAIL_MOBILE = "Email, Mobile"; 
		public static final String EVENT_ANNIVERSARY = "Event, Anniversary"; 
		public static final String EVENT_BIRTHDAY = "Event, Birthday"; 
		public static final String EVENT_OTHER = "Event, Other"; 
		public static final String IM_HOME_AIM = "IM Home, AIM"; 
		public static final String IM_WORK_AIM = "IM Work, AIM";
		public static final String IM_OTHER_AIM = "IM Other, AIM"; 
		public static final String IM_HOME_MSN = "IM Home, MSN"; 
		public static final String IM_WORK_MSN = "IM Work, MSN"; 
		public static final String IM_OTHER_MSN = "IM Other, MSN"; 
		public static final String IM_HOME_YAHOO = "IM Home, Yahoo";
		public static final String IM_WORK_YAHOO = "IM Work, Yahoo"; 
		public static final String IM_OTHER_YAHOO = "IM Other, Yahoo"; 
		public static final String IM_HOME_SKYPE = "IM Home, Skype"; 
		public static final String IM_WORK_SKYPE = "IM Work, Skype"; 
		public static final String IM_OTHER_SKYPE = "IM Other, Skype"; 
		public static final String IM_HOME_QQ = "IM Home, QQ"; 
		public static final String IM_WORK_QQ = "IM Work, QQ";
		public static final String IM_OTHER_QQ = "IM Other, QQ"; 
		public static final String IM_HOME_GOOGLE_TALK = "IM Home, Google Talk"; 
		public static final String IM_WORK_GOOGLE_TALK = "IM Work, Google Talk"; 
		public static final String IM_OTHER_GOOGLE_TALK = "IM Other, Google Talk"; 
		public static final String IM_HOME_ICQ = "IM Home, ICQ";  
		public static final String IM_WORK_ICQ = "IM Work, ICQ"; 
		public static final String IM_OTHER_ICQ = "IM Other, ICQ"; 
		public static final String IM_HOME_JABBER = "IM Home, Jabber"; 
		public static final String IM_WORK_JABBER = "IM Work, Jabber";
		public static final String IM_OTHER_JABBER = "IM Other, Jabber"; 
		public static final String IM_HOME_NETMEETING = "IM Home, NetMeeting"; 
		public static final String IM_WORK_NETMEETING = "IM Work, NetMeeting";
		public static final String IM_OTHER_NETMEETING = "IM Other, NetMeeting";
		public static final String NAME_OTHER = "Name, Other"; 
		public static final String NAME_MAIDEN = "Name, Maiden"; 
		public static final String NAME_SHORT = "Name, Short"; 
		public static final String NAME_INITIALS = "Name, Initials";
		public static final String NOTE = "Note"; 
		public static final String ORG_WORK = "Organization, Work"; 
		public static final String ORG_OTHER = "Organization, Other"; 
		public static final String PHONE_HOME = "Phone, Home"; 
		public static final String PHONE_MOBILE = "Phone, Mobile"; 
		public static final String PHONE_WORK = "Phone, Work"; 
		public static final String PHONE_WORK_MOBILE = "Phone, Work Mobile";
		public static final String PHONE_OTHER = "Phone, Other"; 
		public static final String FAX_HOME = "Fax, Home"; 
		public static final String FAX_WORK = "Fax, Work"; 
		public static final String FAX_OTHER = "Fax, Other"; 
		public static final String PAGER_HOME = "Pager, Home"; 
		public static final String PAGER_WORK = "Pager, Work";
		public static final String PHONE_CALLBACK = "Phone, Callback"; 
		public static final String PHONE_CAR = "Phone, Car"; 
		public static final String PHONE_COMPANY_MAIN = "Phone, Company Main"; 
		public static final String PHONE_ASSISTANT = "Phone, Assistant"; 
		public static final String PHONE_MMS = "Phone, MMS"; 
		public static final String PHOTO_FILE = "Photo, File ID"; 
		public static final String PHOTO_THUMB = "Photo, Thumbnail";
		public static final String SIP_HOME = "SIP Address, Home"; 
		public static final String SIP_WORK = "SIP Address, Work"; 
		public static final String SIP_OTHER = "SIP Address, Other"; 
		public static final String NAME_DISPLAY = "Name, Display"; 
		public static final String NAME_GIVEN = "Name, Given";  
		public static final String NAME_FAMILY = "Name, Family"; 
		public static final String NAME_PREFIX = "Name, Prefix";
		public static final String NAME_MIDDLE = "Name, Middle"; 
		public static final String NAME_SUFFIX = "Name, Suffix";
		public static final String NAME_GIVEN_PHONETIC = "Name, Given Phonetic"; 
		public static final String NAME_MIDDLE_PHONETIC = "Name, Middle Phonetic"; 
		public static final String NAME_FAMILY_PHONETIC = "Name, Family Phonetic"; 
		public static final String ADDRESS_HOME = "Address, Home"; 
		public static final String ADDRESS_WORK = "Address, Work"; 
		public static final String ADDRESS_OTHER = "Address, Other";
		public static final String WEB = "Website";

		/** 
		 * An array of the Fields supported by the ProfileManager.
		 */
		public static final String[] FIELDS = {
			_ID,
			EMAIL_HOME,
			EMAIL_WORK,
			EMAIL_OTHER,
			EMAIL_MOBILE,
			EVENT_ANNIVERSARY,
			EVENT_BIRTHDAY,
			EVENT_OTHER,
			IM_HOME_AIM,
			IM_WORK_AIM,
			IM_OTHER_AIM,
			IM_HOME_MSN,
			IM_WORK_MSN,
			IM_OTHER_MSN,
			IM_HOME_YAHOO,
			IM_WORK_YAHOO,
			IM_OTHER_YAHOO,
			IM_HOME_SKYPE,
			IM_WORK_SKYPE,
			IM_OTHER_SKYPE,
			IM_HOME_QQ,
			IM_WORK_QQ,
			IM_OTHER_QQ,
			IM_HOME_GOOGLE_TALK,
			IM_WORK_GOOGLE_TALK,
			IM_OTHER_GOOGLE_TALK,
			IM_HOME_ICQ,
			IM_WORK_ICQ,
			IM_OTHER_ICQ,
			IM_HOME_JABBER,
			IM_WORK_JABBER,
			IM_OTHER_JABBER,
			IM_HOME_NETMEETING,
			IM_WORK_NETMEETING,
			IM_OTHER_NETMEETING,
			NAME_OTHER,
			NAME_MAIDEN,
			NAME_SHORT,
			NAME_INITIALS,
			NOTE,
			ORG_WORK,
			ORG_OTHER,
			PHONE_HOME,
			PHONE_MOBILE,
			PHONE_WORK,
			PHONE_WORK_MOBILE,
			PHONE_OTHER,
			FAX_HOME,
			FAX_WORK,
			FAX_OTHER,
			PAGER_HOME,
			PAGER_WORK,
			PHONE_CALLBACK,
			PHONE_CAR,
			PHONE_COMPANY_MAIN,
			PHONE_ASSISTANT,
			PHONE_MMS,
			PHOTO_FILE,
			PHOTO_THUMB,
			SIP_HOME,
			SIP_WORK,
			SIP_OTHER,
			NAME_DISPLAY,
			NAME_GIVEN,
			NAME_FAMILY,
			NAME_PREFIX,
			NAME_MIDDLE,
			NAME_SUFFIX,
			NAME_GIVEN_PHONETIC,
			NAME_MIDDLE_PHONETIC,
			NAME_FAMILY_PHONETIC,
			ADDRESS_HOME,
			ADDRESS_WORK,
			ADDRESS_OTHER,
			WEB
		};

		/**
		 * An array of the name fields
		 */
		public static final String[] STRUCTURED_NAMES = {
			NAME_DISPLAY,
			NAME_PREFIX,
			NAME_GIVEN,
			NAME_MIDDLE,
			NAME_FAMILY,
			NAME_SUFFIX,
			NAME_GIVEN_PHONETIC,
			NAME_MIDDLE_PHONETIC,
			NAME_FAMILY_PHONETIC
		};

		/*
		 * The following arrays are just intended to make it a little easier to present 
		 * fields that are related to each, e.g. different names or addresses
		 */
		// (Helper) Array of Name-related fields
		public static final String[] NAME_FIELDS = {
			NAME_DISPLAY,
			NAME_SHORT,
			NAME_PREFIX,
			NAME_GIVEN,
			NAME_MIDDLE,
			NAME_FAMILY,
			NAME_SUFFIX,
			NAME_OTHER,
			NAME_MAIDEN,
			NAME_INITIALS,
			NAME_GIVEN_PHONETIC,
			NAME_MIDDLE_PHONETIC,
			NAME_FAMILY_PHONETIC
		};

		// (Helper) Array of Phone-related fields
		public static final String[] PHONE_FIELDS = {
			PHONE_HOME,
			PHONE_MOBILE,
			PHONE_WORK,
			PHONE_WORK_MOBILE,
			PHONE_OTHER,
			PHONE_CALLBACK,
			PHONE_CAR,
			PHONE_COMPANY_MAIN,
			PHONE_ASSISTANT,
			PHONE_MMS
		};

		// (Helper) Array of Email-related fields
		public static final String[] EMAIL_FIELDS = {
			EMAIL_HOME,
			EMAIL_WORK,
			EMAIL_OTHER,
			EMAIL_MOBILE
		};

		// (Helper) Array of Address-related fields
		public static final String[] ADDRESS_FIELDS = {
			ADDRESS_HOME,
			ADDRESS_WORK,
			ADDRESS_OTHER
		};

		// (Helper) Array of FAX-related fields
		public static final String[] FAX_FIELDS = {
			FAX_HOME,
			FAX_WORK,
			FAX_OTHER
		};

		// (Helper) Array of Event-related fields
		public static final String[] EVENT_FIELDS = {
			EVENT_ANNIVERSARY,
			EVENT_BIRTHDAY,
			EVENT_OTHER,
		};

		// (Helper) Array of IM-related fields
		public static final String[] IM_FIELDS = {
			IM_HOME_AIM,
			IM_WORK_AIM,
			IM_OTHER_AIM,
			IM_HOME_MSN,
			IM_WORK_MSN,
			IM_OTHER_MSN,
			IM_HOME_YAHOO,
			IM_WORK_YAHOO,
			IM_OTHER_YAHOO,
			IM_HOME_SKYPE,
			IM_WORK_SKYPE,
			IM_OTHER_SKYPE,
			IM_HOME_QQ,
			IM_WORK_QQ,
			IM_OTHER_QQ,
			IM_HOME_GOOGLE_TALK,
			IM_WORK_GOOGLE_TALK,
			IM_OTHER_GOOGLE_TALK,
			IM_HOME_ICQ,
			IM_WORK_ICQ,
			IM_OTHER_ICQ,
			IM_HOME_JABBER,
			IM_WORK_JABBER,
			IM_OTHER_JABBER,
			IM_HOME_NETMEETING,
			IM_WORK_NETMEETING,
			IM_OTHER_NETMEETING
		};


		// (Helper) Array of filed names for everything else
		public static final String[] OTHER_FIELDS = {
			NOTE,
			ORG_WORK,
			ORG_OTHER,
			PAGER_HOME,
			PAGER_WORK,
			SIP_HOME,
			SIP_WORK,
			SIP_OTHER,
			WEB
		};

	}//ProfileFields



	// Methods to get/set data (thread safe)

	public ProfileDescriptor () {
		_JSONstring = EMPTY_PROFILE_STRING;
		_JSONobject = new JSONObject();
	}

	public String toString() {
		return getJSONString();
	}

	public synchronized String getJSONString (){ 
		return _JSONobject.toString() ; 
	}

	public synchronized JSONObject getJSONObject (){ 
		return _JSONobject ; 
	}


	public synchronized boolean isEmpty(){
		boolean result = true;
		try {
			if (_JSONobject.names().length()>0) {
				result = false;
			}
		} catch (Exception e){
			Log.w(TAG, "isEmpty() exception: "+e.toString());
			result = true;
		}
		return result;
	}

	// Routine to retrieve the Profile photo from the JSON string (Base64 encoded)
	public synchronized byte[] getPhoto (){ 

		byte[] pthumb = null; // returned image (null if not defined)
		try{
			String pString = getField(ProfileFields.PHOTO_THUMB);
			if (pString.length()>0){
				pthumb = Base64.decode(pString, Base64.DEFAULT); 
			}
		} catch (Exception e){
			Log.e(TAG, "Error decoding photo");
		}
		return pthumb ;
	}

	// Utility to export the Photo to a JPEG file at the specified path
	public synchronized void exportPhoto(String path){

		byte[] photo = null; 
		try{
			// extract the photo encoded string
			String pString = getField(ProfileFields.PHOTO_THUMB);

			// If present, then convert to bitmap and write to file
			if (pString.length()>0){
				photo = Base64.decode(pString, Base64.DEFAULT); 

				// Decode the image's byte array into a bitmap
				Bitmap photoData = BitmapFactory.decodeByteArray(photo, 0, photo.length);

				// Create the file and write the data out to it
				File file = new File (path);
				FileOutputStream fos = new FileOutputStream(file);
				try{
					boolean success = photoData.compress(CompressFormat.JPEG, 100, fos);
				} catch (Exception e){
					// ignore errors, image may already be compressed
				}
				fos.flush();
				fos.close();

			} else {
				Log.v(TAG, "exportPhoto(): No photo in profile");
			}
		} catch (Exception e){
			Log.e(TAG, "Error exporting photo: "+e.toString());
		}

	}

	// generic method to retrieve the list of field names present (varies by profile)
	public synchronized String[] getFieldList(){
		JSONArray keylist = _JSONobject.names();
		String[] list = new String[keylist.length()];
		for (int i=0; i<keylist.length(); i++){
			try{
				list[i] = keylist.getString(i);
			} catch (Exception e) {
				// ignore
			}
		}
		return list;
	}

	// generic method to return a named (String) field. This allows app-defined fields to be retrieved
	public synchronized String getField (String field){ 		
		try {
			return _JSONobject.getString(field);
		} catch (JSONException e) {
			//e.printStackTrace();
			//Log.e(TAG, "Error getting field("+field+"): "+e.toString());
			return "";
		}
	}

	/**
	 * Get the ProfileId (Unique name used for indexing)
	 * @return the profile ID string
	 */
	public synchronized String getProfileId (){ 		
		return getField(ProfileFields._ID);
	}



	// SET methods

	// Reset the entire object with the specified (JSON) string
	public synchronized void setJSONString (String json){
		try {
			//Log.v(TAG, "+++++++ json: "+json);
			_JSONstring = json;
			_JSONobject = new JSONObject (_JSONstring);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void setJSONObject (JSONObject jsonObj){
		_JSONobject = jsonObj;
		_JSONstring = jsonObj.toString();
	}

	// Set named field
	public synchronized void setField (String field, String value){
		try {
			_JSONobject.put(field, value);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Log.e(TAG, "Error setting field("+field+"): "+e.toString());
		}
	}

	public synchronized void setProfileId(String profileId) {
		try {
			_JSONobject.put(ProfileFields._ID, profileId);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Log.e(TAG, "Error setting field(profileId): "+e.toString());
		}
	}

	// Utility function for generating acceptable id for use in an AllJoyn service
	public static String makeServiceName (String s){

		//Utility to convert generic string into something usable as a service name
		String rs = s;
		rs = s.replaceAll("\\W", "");
		Log.v(TAG, "makeServiceName() old: ("+s+") new: ("+rs+")");
		return rs ;
	}

	// Function to populate the profile from a specified ContactID
	// In this case, this is Android-specific and loads the data from the Contact Database using the URI supplied
	// The data is loaded directly into the internal JSON object 
	// Note: - need content resolver because this code doesn't know what is calling it, and it could be an 
	//         Activity or a Service
	//       - profileId is a problem because it may not be defined initially, so build from name field

	public boolean loadFromContact (ContentResolver cr, String contactId) {
		boolean result ;
		String  lookupId;
		byte[] bPhoto = null;

		result = false;

		Log.d(TAG, "Loading profile for: "+contactId);

		setProfileId(contactId);
		// do not set ProfileID, as this is independent of the contactID
		//setProfileId ("");

		Uri uri = Uri.parse(contactId);
		//setURI (contactId);

		//ContentResolver cr = ProfileGlobalData.mApp.getContentResolver();

		Cursor c = cr.query(uri, null, null, null, null);  
		if (c.moveToFirst()) {  

			lookupId = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));
			setField("lookupId",lookupId);

			//Get the name
			setField(ProfileFields.NAME_DISPLAY, c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));

			//Get the number
			if (Integer.parseInt(c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
				Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
						null, 
						ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", 
						new String[]{lookupId}, null);
				if (pCur.moveToFirst()) {
					setField(ProfileFields.PHONE_MOBILE, pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));

				}
				/** TODO: cope with multiple phone numbers. Should probably just make this an array field
				while (pCur.moveToNext()) {
					// Do something with phones
				} 
				 **/
				pCur.close();
			}

			// TEMP: dumping cursor
			//Utilities.dumpCursor(c);



			// Find Email Addresses
			Cursor emails = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, 
					null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + lookupId, 
					null, null);
			if (emails.moveToFirst()) 
			{
				//TODO: deal with multiple emails
				setField(ProfileFields.EMAIL_HOME, emails.getString(emails.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)));
			}
			emails.close();

			Cursor address = cr.query(
					ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
					null,
					ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = " + lookupId,
					null, null);
			if (address.moveToFirst()) 
			{ 
				// These are all private class variables, don't forget to create them.
				setField(ProfileFields.ADDRESS_OTHER, address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POBOX)));
				setField(ProfileFields.ADDRESS_HOME, address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)));
				//setCity       (address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)));
				//setState      (address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)));
				//setPostalCode (address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)));
				//setCountry    (address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)));
				//setType       (address.getString(address.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)));
			}  //address.moveToNext()      

			result = true;

		}
		c.close();


		// OK, photos are a little complicated, since the thumbnail is stored directly in the database,
		// and I haven't figured out how to get the filename of the image used
		// So, get the thumbnail, and store it to the profile cache instead

		//		bPhoto = getContactPhoto(cr, lookupId);
		//		// and finally, save to the Profile Cache
		//		
		//		// We should use the profileId as a name, but we may not know what that is at startup, so use name field
		//		// (can't have a contact without a name (I think)
		//		
		//		if (ProfileConfigCache.isReady()){
		//			String name=getName();
		//			name=makeServiceName(name);
		//			if ((name==null) || (name.length()<=0)){
		//				Log.e(TAG, "Oops, null name determined for photo");
		//			}
		//			//ProfileCache.saveProfile(name, this);
		//			ProfileConfigCache.savePhoto(name, bPhoto);
		//			setPhotoPath(ProfileConfigCache.getPhotoPath(name)); // adds location and extension
		//		} else {
		//			Log.e(TAG, "*** Oops, cache is not ready, cannot save");
		//		}

		return result ;
	}




} // ProfileDescriptor
