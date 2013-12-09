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
package org.alljoyn.storage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


import org.alljoyn.devmodules.common.ProfileDescriptor;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;

/**
 * ContactLoader
 * This class provides a means to load a contact from the native database and format
 * the data into JSON representation
 *
 */
public class ContactLoader {
	private static final String TAG = "ContactLoader";
	private static Context mContext;
	Map<String,String> mContactFields ;  // used to accumulate results

	/**
	 * Constructor
	 * @param context the Application context
	 */
	public ContactLoader(Context context) {
		mContext = context;
		mContactFields = new TreeMap<String,String>();
	}

	private ContactLoader() {
		// private to prevent use of this form
	}



	public ContactLoader clone() {
		return new ContactLoader(mContext);
	}

	/*-----------------------------------------------------------------------*
	 * ProfileManager APIs
	 *-----------------------------------------------------------------------*/

	/**
	 * Retrieves the data associated with the provided contact ID
	 * @param contactid ID (URI) used to identify contact in local database
	 * @return Profile data in JSON format (see ProfileDescriptor)
	 */
	public String retrieveProfile(String contactid) {
		String pstring = "{}";
		String [] projection ;
		String id="";
		String fvalue="";
		String ftype="";
		int    itype;

		try{
			Log.d(TAG, "getProfile("+contactid+")");
			if (mContext != null){

				ContentResolver cr = mContext.getContentResolver();
				mContactFields.clear();
				Uri uri = Uri.parse(contactid);
				// get the lookup ID
				Cursor c = mContext.getContentResolver().query(uri, null, null, null, null);
				//dumpCursor(c);
				if (c.moveToFirst()) {  
					id = c.getString(c.getColumnIndex(ContactsContract.Contacts._ID));

					// run various queries to retrieve data from the Contact database
					// these all update mContactFields
					getNames(cr, id);
					// Only query if number is defined
					if (Integer.parseInt(c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
						getNumbers(cr, id);
					}
					getEmails(cr, id);
					getAddresses(cr, id);
					getOrgs(cr, id);
					getIMs(cr, id);
					getNotes(cr, id);
					getWeb(cr, id);
					getPhoto(cr, id);

					// OK, convert map of fields to JSON format
					pstring = toJSON(mContactFields);
				} else {
					Log.w (TAG, "No data for contact: "+contactid);
				}
				if (!c.isClosed()) c.close();
			} else {
				Log.e(TAG, "Null context");
			}
		} catch (Exception e){
			Log.e(TAG, "getProfile("+contactid+") Exception: "+e.toString());
			e.printStackTrace();
		}
		return pstring;
	}


	/*-----------------------------------------------------------------------*
	 * Utilities for retrieving various fields
	 * A little naughty, the global list mContactFields is modified by each of these routines
	 *-----------------------------------------------------------------------*/

	/**
	 * Retrieves the Display Name field and adds it to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getDisplayName (ContentResolver cr, String id){
		String ftype;
		String fvalue;
		String [] projection = new String[] {
				ContactsContract.Contacts.DISPLAY_NAME
		};

		Cursor c=null;

		try{
			c = cr.query(ContactsContract.Contacts.CONTENT_URI,
					projection, 
					ContactsContract.Contacts._ID + "=?",    // filter entries on the basis of the contact id
					new String[]{String.valueOf(id)},    // the parameter to which the contact id column is compared toProfile._ID +" = "+id, 
					null);

			if (c.moveToFirst()){
				//dumpCursor(names);
				ftype = ProfileDescriptor.ProfileFields.NAME_DISPLAY;
				fvalue = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Identity.DISPLAY_NAME));
				mContactFields.put(ftype, fvalue);
			}
			c.close();
		} catch (Exception e){
			Log.e(TAG, "getNames() Exception: "+e.toString());
			if ((c!=null) && (!c.isClosed())) {
				c.close();
			}
		}
	}

	/**
	 * Retrieves the structured name fields and adds them to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getNames (ContentResolver cr, String id){
		int    itype;
		String ftype;
		String fvalue;

		Cursor c=null;

		try{
			// projection
			String[] projection = new String[] {
					ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
					ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, 
					ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, 
					ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
					ContactsContract.CommonDataKinds.StructuredName.PREFIX,
					ContactsContract.CommonDataKinds.StructuredName.SUFFIX
			};


			String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
			String[] whereParameters = new String[]{id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE};

			c = cr.query(ContactsContract.Data.CONTENT_URI, projection, where, whereParameters, null);

			String [][] fieldMap = {
					{ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, ProfileDescriptor.ProfileFields.NAME_DISPLAY},
					{ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,  ProfileDescriptor.ProfileFields.NAME_FAMILY},
					{ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,   ProfileDescriptor.ProfileFields.NAME_GIVEN},
					{ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,  ProfileDescriptor.ProfileFields.NAME_MIDDLE},
					{ContactsContract.CommonDataKinds.StructuredName.PREFIX,       ProfileDescriptor.ProfileFields.NAME_PREFIX},
					{ContactsContract.CommonDataKinds.StructuredName.SUFFIX,       ProfileDescriptor.ProfileFields.NAME_SUFFIX}
			};

			String field="";
			if (c.moveToFirst()) {
				for (int i=0; i<fieldMap.length; i++){ 
					field = c.getString(c.getColumnIndex(fieldMap[i][0]));
					if ((field!=null) && (field.length()>0)){
						mContactFields.put(fieldMap[i][1], field);
					}
				}
			}
			c.close();
		} catch (Exception e){
			Log.e(TAG, "getNames() Exception: "+e.toString());
			e.printStackTrace();
			if ((c!=null) && (!c.isClosed())) {
				c.close();
			}
		}

		// This doesn't always work, so check and do simpler query if necessary
		if (!mContactFields.containsKey(ProfileDescriptor.ProfileFields.NAME_DISPLAY)){
			getDisplayName(cr, id);
		}
	}



	/**
	 * Retrieves the phone number fields (including FAX) and adds them to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getNumbers (ContentResolver cr, String id){
		int    itype;
		String ftype;
		String fvalue;

		Cursor c=null;

		try{

			String[] projection = new String[] {
					ContactsContract.CommonDataKinds.Phone.TYPE,
					ContactsContract.CommonDataKinds.Phone.NUMBER
			};
			String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
			String[] whereParameters = new String[]{id, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE};

			c = cr.query(ContactsContract.Data.CONTENT_URI, projection, where, whereParameters, null);

			while (c.moveToNext()) {
				// Convert to correct format and ad to list
				itype = Integer.parseInt(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)));
				fvalue = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				ftype = getPhoneType(itype);
				if (ftype.length()>0){
					mContactFields.put(ftype, fvalue);
				}
			}
			c.close();
		} catch (Exception e){
			Log.e(TAG, "getNumbers() Exception: "+e.toString());
			if ((c!=null) && (!c.isClosed())) {
				c.close();
			}
		}
	}



	/**
	 * Retrieves the email fields and adds them to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getEmails (ContentResolver cr, String id){
		int    itype;
		String ftype;
		String fvalue;

		Cursor c=null;

		try{
			// Get the emails
			String[] projection = new String[] {
					ContactsContract.CommonDataKinds.Email.TYPE,
					ContactsContract.CommonDataKinds.Email.ADDRESS
			};
			String where = ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
			String[] whereParameters = new String[]{id, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE};

			c = cr.query(ContactsContract.Data.CONTENT_URI, projection, where, whereParameters, null);

			String s="";
			while (c.moveToNext()) {
				// Convert to correct format and add to list
				s=c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
				if ((s!=null) && (s.length()>0)){ 
					itype = Integer.parseInt(s);
				} else {
					itype = ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
				}
				fvalue = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));

				ftype = getEmailType(itype);
				if (ftype.length()>0){
					mContactFields.put(ftype, fvalue);
				}
				//Log.v(TAG, "Email: "+fvalue+", type: "+itype);
			} 
			c.close();

		} catch (Exception e){
			Log.e(TAG, "getEmails() Exception: "+e.toString());
			if ((c!=null) && (!c.isClosed())) {
				c.close();
			}
		}
	}



	/**
	 * Retrieves the address fields and adds them to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getAddresses (ContentResolver cr, String id){
		int    itype;
		String ftype;
		String fvalue;

		Cursor c=null;

		try{
			String[] projection = new String[] {
					ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
					ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
			};
			String where = ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
			String[] whereParameters = new String[]{id, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE};

			c = cr.query(ContactsContract.Data.CONTENT_URI, projection, where, whereParameters, null);

			while (c.moveToNext()) {
				// Convert to correct format and add to list
				itype = Integer.parseInt(c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)));
				fvalue = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));

				ftype = getAddressType(itype);
				if (ftype.length()>0){
					mContactFields.put(ftype, fvalue);
				}
				//Log.v(TAG, "Address: "+fvalue+", type: "+itype);
			} 
			c.close();
		} catch (Exception e){
			Log.e(TAG, "getAddresses() Exception: "+e.toString());
			if ((c!=null) && (!c.isClosed())) {
				c.close();
			}
		}
	}


	/**
	 * Retrieves the organization fields and adds them to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getOrgs (ContentResolver cr, String id){
		int    itype;
		String ftype;
		String fvalue;
		Cursor c=null;

		try{
			String[] projection = new String[] {
					ContactsContract.CommonDataKinds.Organization.DATA,
					ContactsContract.CommonDataKinds.Organization.COMPANY,
					ContactsContract.CommonDataKinds.Organization.DEPARTMENT,
					ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION,
					ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION,
					ContactsContract.CommonDataKinds.Organization.TITLE,
					ContactsContract.CommonDataKinds.Organization.TYPE
			};
			String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
			String[] whereParameters = new String[]{id, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE};

			c = cr.query(ContactsContract.Data.CONTENT_URI, projection, where, whereParameters, null);


			//Note: don't know why, but this seems to return the same data twice
			// It doesn't really matter, the second will just overwrite the first

			fvalue="";
			String s="";
			while (c.moveToNext()) {
				// Note that any of these fields can be null, so be sure to check

				// DATA and COMPANY appear to be duplicates
				//s = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DATA));
				//Log.v(TAG, "Org.DATA:"+s);
				//if ((s!=null) && (s.length()>0)) fvalue = s;

				/** Not sure if these fields are worth processing **/
				s = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY));
				//Log.v(TAG, "Org.COMPANY:"+s);
				if ((s!=null) && (s.length()>0)) fvalue = s;
				//if ((s!=null) && (s.length()>0)) fvalue += ", "+s;

				s = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT));
				//Log.v(TAG, "Org.DEPARTMENT:"+s);
				if ((s!=null) && (s.length()>0)) fvalue += ", "+s;

				s = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION));
				//Log.v(TAG, "Org.JOB_DESCRIPTION:"+s);
				if ((s!=null) && (s.length()>0)) fvalue += ", "+s;

				s = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION));
				//Log.v(TAG, "Org.OFFICE_LOCATION:"+s);
				if ((s!=null) && (s.length()>0)) fvalue += ", "+s;

				/** **/

				s = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
				//Log.v(TAG, "Org.TITLE:"+s);
				if ((s!=null) && (s.length()>0)) fvalue += ", "+s;


				s= c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TYPE));
				if ((s!=null) && (s.length()>0)){ 
					itype = Integer.parseInt(s);
				} else {
					itype = ContactsContract.CommonDataKinds.Organization.TYPE_OTHER;
				}
				ftype = getOrgType(itype);
				if ((ftype.length()>0) && (fvalue.length()>0)){
					mContactFields.put(ftype, fvalue);
				}
			}

			c.close();
		} catch (Exception e){
			Log.e(TAG, "getOrgs() Exception: "+e.toString());
			e.printStackTrace();
			if ((c!=null) && (!c.isClosed())) {
				c.close();
			}
		}
	}

	/**
	 * Retrieves the IM fields and adds them to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getIMs (ContentResolver cr, String id){
		int    itype;
		String ftype;
		String fvalue;
		Cursor c=null;

		try{
			String[] projection = new String[] {
					ContactsContract.CommonDataKinds.Im.DATA,
					ContactsContract.CommonDataKinds.Im.PROTOCOL,
					ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL,
					ContactsContract.CommonDataKinds.Im.TYPE
			};
			String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
			String[] whereParameters = new String[]{id, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE};

			c = cr.query(ContactsContract.Data.CONTENT_URI, projection, where, whereParameters, null);

			fvalue="";
			String protocol = "";
			String cprotocol = "";
			String ptype = "";
			while (c.moveToNext()) {
				fvalue = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA));
				protocol = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL));
				cprotocol = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL));
				ptype = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Im.TYPE));
				if (protocol==null)protocol = cprotocol; // if null, it's supposed to be in the CUSTOM_PROTOCOL field
				ftype = getIMType(ptype, protocol);
				if ((ftype.length()>0) && (fvalue.length()>0)){
					mContactFields.put(ftype, fvalue);
				} else {
					Log.w(TAG, "getIMs() Don't know how to resolve: "+fvalue);
				}
			}

			c.close();
		} catch (Exception e){
			Log.e(TAG, "getIMs() Exception: "+e.toString());
			e.printStackTrace();
			if ((c!=null) && (!c.isClosed())) {
				c.close();
			}
		}
	}


	/**
	 * Retrieves the Notes fields and adds them to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getNotes (ContentResolver cr, String id){
		int    itype;
		String ftype;
		String fvalue;
		Cursor c=null;

		try{
			String[] projection = new String[] {
					ContactsContract.CommonDataKinds.Note.NOTE
			};
			String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
			String[] whereParameters = new String[]{id, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE};

			c = cr.query(ContactsContract.Data.CONTENT_URI, projection, where, whereParameters, null);

			fvalue="";
			if (c.moveToFirst()) {
				fvalue = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));

				if ((fvalue!=null) && (fvalue.length()>0)){
					mContactFields.put(ProfileDescriptor.ProfileFields.NOTE, fvalue);
				}
			}

			c.close();
		} catch (Exception e){
			Log.e(TAG, "getNotes() Exception: "+e.toString());
			e.printStackTrace();
			if ((c!=null) && (!c.isClosed())) {
				c.close();
			}
		}
	}


	/**
	 * Retrieves the Web field and adds it to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getWeb (ContentResolver cr, String id){
		int    itype;
		String ftype;
		String fvalue;
		Cursor c=null;

		try{
			String[] projection = new String[] {
					ContactsContract.CommonDataKinds.Website.URL
			};
			String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
			String[] whereParameters = new String[]{id, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE};

			c = cr.query(ContactsContract.Data.CONTENT_URI, null, where, whereParameters, null);

			fvalue="";
			if (c.moveToFirst()) {
				fvalue = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL));

				if ((fvalue!=null) && (fvalue.length()>0)){
					mContactFields.put(ProfileDescriptor.ProfileFields.WEB, fvalue);
				}
			}

			c.close();
		} catch (Exception e){
			Log.e(TAG, "getWeb() Exception: "+e.toString());
			e.printStackTrace();
			if ((c!=null) && (!c.isClosed())) {
				c.close();
			}
		}
	}


	/**
	 * Retrieves the Contact photo, Base64 encodes it and adds it to mContactFields
	 * @param cr ContentResolver for this context
	 * @param id Lookup ID of the contact
	 */
	private void getPhoto(ContentResolver cr, String id) {
		String ftype;
		String fvalue;
		long   uriId;

		try{
			// get the contact photo
			uriId = Long.parseLong(id);
			Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, uriId);
			InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri);
			if (input != null) {
				Bitmap bitmap = BitmapFactory.decodeStream(input);
				if (bitmap != null){
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					try{
						bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);  
					} catch (Exception ec){
						// just ignore compression errors, likely already compressed
					}
					byte[] barray = stream.toByteArray();
					//BASE64 encode the data
					fvalue = Base64.encodeToString(barray, Base64.DEFAULT); 

					if ((fvalue!=null) && (fvalue.length()>0)){
						mContactFields.put(ProfileDescriptor.ProfileFields.PHOTO_THUMB, fvalue);
					}
				} else {
					Log.w(TAG, "getPhoto() Null bitmap for contact photo");
				}

			}
		} catch (Exception e){
			Log.e(TAG, "getPhoto() Exception: "+e.toString());
			e.printStackTrace();
		}
	}


	/*
	 * Type Conversion utilities
	 */


	/**
	 * Convert Android (numeric string) phone type to profile field id
	 * @param type Android (numeric string) phone type
	 * @return The equivalent field identifier for use in ProfileDescriptors. Empty string if unknown
	 */
	private String getPhoneType(int type){
		String ptype="";
		switch (type) {
		case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
			ptype = ProfileDescriptor.ProfileFields.PHONE_HOME;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
			ptype = ProfileDescriptor.ProfileFields.PHONE_MOBILE;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
			ptype = ProfileDescriptor.ProfileFields.PHONE_WORK;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
			ptype = ProfileDescriptor.ProfileFields.FAX_WORK;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
			ptype = ProfileDescriptor.ProfileFields.FAX_HOME;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
			ptype = ProfileDescriptor.ProfileFields.PAGER_WORK;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
			ptype = ProfileDescriptor.ProfileFields.PHONE_OTHER;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK:
			ptype = ProfileDescriptor.ProfileFields.PHONE_CALLBACK;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_CAR:
			ptype = ProfileDescriptor.ProfileFields.PHONE_CAR;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
			ptype = ProfileDescriptor.ProfileFields.PHONE_COMPANY_MAIN;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_ISDN:
			ptype = "";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_MAIN:
			ptype = "";
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX:
			ptype = ProfileDescriptor.ProfileFields.FAX_OTHER;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
			ptype = ProfileDescriptor.ProfileFields.PHONE_WORK_MOBILE;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER:
			ptype = ProfileDescriptor.ProfileFields.PAGER_WORK;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT:
			ptype = ProfileDescriptor.ProfileFields.PHONE_ASSISTANT;
			break;
		case ContactsContract.CommonDataKinds.Phone.TYPE_MMS:
			ptype = ProfileDescriptor.ProfileFields.PHONE_MMS;
			break;
		}
		return ptype;
	}


	/**
	 * Convert Android (numeric string) Email type to profile field id
	 * @param type Android (numeric string) email type
	 * @return The equivalent field identifier for use in ProfileDescriptors. Empty string if unknown
	 */
	private String getEmailType(int type){
		String ptype="";
		switch (type) {
		case ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM:
			ptype = "";
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
			ptype = ProfileDescriptor.ProfileFields.EMAIL_HOME;
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
			ptype = ProfileDescriptor.ProfileFields.EMAIL_WORK;
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
			ptype = ProfileDescriptor.ProfileFields.EMAIL_OTHER;
			break;
		case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
			ptype = ProfileDescriptor.ProfileFields.EMAIL_MOBILE;
			break;
		}
		return ptype;
	}


	/**
	 * Convert Android (numeric string) Address type to profile field id
	 * @param type Android (numeric string) address type
	 * @return The equivalent field identifier for use in ProfileDescriptors. Empty string if unknown
	 */
	private String getAddressType(int type){
		String ptype="";
		switch (type) {
		case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM:
			ptype = "";
			break;
		case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME:
			ptype = ProfileDescriptor.ProfileFields.ADDRESS_HOME;
			break;
		case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK:
			ptype = ProfileDescriptor.ProfileFields.ADDRESS_WORK;
			break;
		case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER:
			ptype = ProfileDescriptor.ProfileFields.ADDRESS_OTHER;
			break;
		}
		return ptype;
	}


	/**
	 * Convert Android (numeric string) Org type to profile field id
	 * @param type Android (numeric string) organization type
	 * @return The equivalent field identifier for use in ProfileDescriptors. Empty string if unknown
	 */
	private String getOrgType(int type){
		String ptype="";
		switch (type) {
		case ContactsContract.CommonDataKinds.Organization.TYPE_CUSTOM:
			ptype = "";
			break;
		case ContactsContract.CommonDataKinds.Organization.TYPE_WORK:
			ptype = ProfileDescriptor.ProfileFields.ORG_WORK;
			break;
		case ContactsContract.CommonDataKinds.Organization.TYPE_OTHER:
			ptype = ProfileDescriptor.ProfileFields.ORG_OTHER;
			break;
		}
		return ptype;
	}



	// static map used to translate IM Type & protocol to return value
	private static final Map<String,String> IM_MAP = new HashMap<String,String>() {{
		// PROTOCOL_AIM
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_HOME)+ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM), 
				ProfileDescriptor.ProfileFields.IM_HOME_AIM);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_WORK)+ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM), 
				ProfileDescriptor.ProfileFields.IM_WORK_AIM);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_OTHER)+ContactsContract.CommonDataKinds.Im.PROTOCOL_AIM), 
				ProfileDescriptor.ProfileFields.IM_OTHER_AIM);
		// PROTOCOL_MSN
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_HOME)+ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN), 
				ProfileDescriptor.ProfileFields.IM_HOME_MSN);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_WORK)+ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN), 
				ProfileDescriptor.ProfileFields.IM_WORK_MSN);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_OTHER)+ContactsContract.CommonDataKinds.Im.PROTOCOL_MSN), 
				ProfileDescriptor.ProfileFields.IM_OTHER_MSN);
		// PROTOCOL_YAHOO
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_HOME)+ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO), 
				ProfileDescriptor.ProfileFields.IM_HOME_YAHOO);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_WORK)+ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO), 
				ProfileDescriptor.ProfileFields.IM_WORK_YAHOO);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_OTHER)+ContactsContract.CommonDataKinds.Im.PROTOCOL_YAHOO), 
				ProfileDescriptor.ProfileFields.IM_OTHER_YAHOO);
		// PROTOCOL_SKYPE
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_HOME)+ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE), 
				ProfileDescriptor.ProfileFields.IM_HOME_SKYPE);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_WORK)+ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE), 
				ProfileDescriptor.ProfileFields.IM_WORK_SKYPE);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_OTHER)+ContactsContract.CommonDataKinds.Im.PROTOCOL_SKYPE), 
				ProfileDescriptor.ProfileFields.IM_OTHER_SKYPE);
		// PROTOCOL_QQ
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_HOME)+ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ), 
				ProfileDescriptor.ProfileFields.IM_HOME_QQ);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_WORK)+ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ), 
				ProfileDescriptor.ProfileFields.IM_WORK_QQ);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_OTHER)+ContactsContract.CommonDataKinds.Im.PROTOCOL_QQ), 
				ProfileDescriptor.ProfileFields.IM_OTHER_QQ);
		// PROTOCOL_GOOGLE_TALK
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_HOME)+ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK), 
				ProfileDescriptor.ProfileFields.IM_HOME_GOOGLE_TALK);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_WORK)+ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK), 
				ProfileDescriptor.ProfileFields.IM_WORK_GOOGLE_TALK);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_OTHER)+ContactsContract.CommonDataKinds.Im.PROTOCOL_GOOGLE_TALK), 
				ProfileDescriptor.ProfileFields.IM_OTHER_GOOGLE_TALK);
		// PROTOCOL_ICQ
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_HOME)+ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ), 
				ProfileDescriptor.ProfileFields.IM_HOME_ICQ);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_WORK)+ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ), 
				ProfileDescriptor.ProfileFields.IM_WORK_ICQ);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_OTHER)+ContactsContract.CommonDataKinds.Im.PROTOCOL_ICQ), 
				ProfileDescriptor.ProfileFields.IM_OTHER_ICQ);
		// PROTOCOL_JABBER
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_HOME)+ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER), 
				ProfileDescriptor.ProfileFields.IM_HOME_JABBER);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_WORK)+ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER), 
				ProfileDescriptor.ProfileFields.IM_WORK_JABBER);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_OTHER)+ContactsContract.CommonDataKinds.Im.PROTOCOL_JABBER), 
				ProfileDescriptor.ProfileFields.IM_OTHER_JABBER);
		// PROTOCOL_NETMEETING
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_HOME)+ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING), 
				ProfileDescriptor.ProfileFields.IM_HOME_NETMEETING);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_WORK)+ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING), 
				ProfileDescriptor.ProfileFields.IM_WORK_NETMEETING);
		put((String.valueOf(ContactsContract.CommonDataKinds.Im.TYPE_OTHER)+ContactsContract.CommonDataKinds.Im.PROTOCOL_NETMEETING), 
				ProfileDescriptor.ProfileFields.IM_OTHER_NETMEETING);

	}};

	/**
	 * Convert Android (numeric string) IM type to profile field id
	 * @param type Android (numeric string) IM type
	 * @param provider Android IM provider type
	 * @return The equivalent field identifier for use in ProfileDescriptors. Empty string if unknown
	 */

	//TODO: translate provider type
	private String getIMType(String type, String protocol){

		String ptype="";
		String key = type + protocol;
		if (IM_MAP.containsKey(key)){
			ptype = IM_MAP.get(key);
		} else {
			Log.w(TAG, "getIMType("+type+", "+protocol+") Unknown");
		}

		return ptype;
	}



	// Debug utility to dump the fields and data of a cursor
	// Helps work around the lack of Android documentation ;-)
	public static void dumpCursor (Cursor cursor){
		int i, j, count ;

		Cursor c=cursor;
		count=c.getColumnCount();

		/**
		for(i=0;i<count;i++){
			Log.d(TAG, "Column "+i+": "+c.getColumnName(i)+",");
		}
		Log.d(TAG, "");
		 **/

		String str;
		c.moveToFirst();
		while(true){
			for(j=0;j<count;j++){
				try {
					str = c.getString(j);
				}catch (Exception e){
					str = "Error converting to string";
				}
				if (str!=null){
					Log.d(TAG, "Content("+c.getColumnName(j)+"):"+str+",");
				}
			}
			Log.d(TAG, "");
			if(c.isLast()){
				break;
			}else{
				c.moveToNext();
			}
		}
	}//dumpCursor




	/**
	 * Conversion to JSON Sting form
	 */

	String toJSON(Map<String,String> profileData) {
		//JSONObject json = new JSONObject();
		String jsonStr="{";
		try {
			String nkey = ProfileDescriptor.ProfileFields.NAME_DISPLAY;
			String tkey = ProfileDescriptor.ProfileFields.PHOTO_THUMB;
			String name=null;
			String thumbnail=null;
			int n = profileData.size();

			// make sure display name goes first and photo thumbnail goes last
			if (profileData.containsKey(nkey)){
				name = profileData.get(nkey);
				profileData.remove(nkey);
				jsonStr += "\"" + nkey + "\":\"" + name + "\"";
				if (n>1) jsonStr += ",";
				n--;
			}

			if (profileData.containsKey(tkey)){
				thumbnail = profileData.get(tkey);
				profileData.remove(tkey);
				n--;
			}

			// Loop over the rest of the keys of the data set and build the JSON string
			// (Done manually to control order)
			int i = 0;
			for(String key : profileData.keySet()) {
				//json.put(key, profileData.get(key));
				jsonStr += "\"" + key + "\":\"" + profileData.get(key) + "\"";
				i++;
				if (i<n) jsonStr += ",";
			}

			Log.v(TAG, "toJSON() profile string without photo: "+jsonStr+"}");

			// Add back the photo thumbnail
			if (thumbnail!=null){
				//json.put(ProfileDescriptor.ProfileFields.PHOTO_THUMB, thumbnail);
				jsonStr += ",\"" + tkey + "\":\"" + thumbnail + "\"";
			}

			jsonStr += "}";

		} catch (Exception e) {
			e.printStackTrace();
		}
		//return json.toString();
		return jsonStr;
	}

} // ContactLoader
