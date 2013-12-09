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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;



import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

// General utility functions used in various files/activities
public class Utilities {

	private static final String TAG = "Utilities";



	private Utilities() { 
		// no implementation, just private to make sure no one 
		// accidentally creates an object
	} 


	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}


	private static String _emptyString = null ;
	private static byte[] _emptyByteArray = null ;


	// checks a String. If it is null, then replace with an empty string, else return the argument
	public static String checkString (String s){
		if (null==s) {
			// Argument is null, replace with empty string
			if (null==_emptyString){
				// double-check that _emptyString exists, if not allocate it
				_emptyString = new String("");
			}
			return _emptyString ;
		} else {
			// argument is OK, just return what was passed in the argument
			return s ;
		}
	}


	// checks a byte[] argument. If it is null, then replace with an empty array, else return the argument
	public static byte[] checkByteArray (byte[] bytes){
		if (null==bytes) {
			// Argument is null, replace with empty byte array
			if (null==_emptyByteArray){
				// double-check that _emptyByteArray exists, if not allocate it
				_emptyByteArray = new byte[0];
			}
			return _emptyByteArray ;
		} else {
			return bytes;
		}
	}

	public static String makeServiceName (String s){
		return makeValidName(s);
	}

	public static String makeValidName (String s){

		//Utility to convert generic string into something usable as a service name
		String rs = s;
		rs = rs.replaceAll("-", "_");
		rs = rs.replaceAll("@", "_");
		rs = rs.replaceAll("\\W", "");
		return rs ;
	}



	// returns alphanumeric hash based on input string
	// May be 31 or 32 characters long. First character is always alpha
	public static String generateHash(String seed){
		String hashString;
		try{
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(seed.getBytes());
			byte messageDigest[] = algorithm.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i=0;i<messageDigest.length;i++) {
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			}
			String foo = messageDigest.toString();
			hashString=hexString+"";
		} catch(Exception e){
			// Error, just generate 'weak' hash
			Log.e(TAG, "generateHash() Error generating hash string for: ("+seed+")")  ;  
			hashString = "";
			for(int i = 0; i < 15; i++)
				hashString += (char)('A'+(int)(Math.random()*26));
		}

		// Make sure first character is a letter
		if (!Character.isLetter(hashString.charAt(0))){
			hashString = (char)('A'+(int)(Math.random()*26)) + hashString.substring(1);
		}
		return hashString;
	}

	

	/////////////////////////////////////////////////////////////
	// TIMESTAMP UTILITIES
	/////////////////////////////////////////////////////////////
	public static final String TIMESTAMP_FORMAT = "yyMMddhhmmss";
	public static final String NULL_TIMESTAMP   = "000000000000";

	// get the current date & time as a string. The argument specifies the format
	// format can be any variant of yyyyMMddhhmmss, with other characters
	// Common forms are: yy/MM/dd yyyy/MM/dd hh:mm hh:mm:ss etc. etc.
	// Refer to the standard Java class SimpleDateFormat for details
	public static String getTimestamp (String format){
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(cal.getTime());
	}

	// get the current timestamp in the default format (yyMMddhhmmss)
	public static String getTimestamp (){
		return getTimestamp(TIMESTAMP_FORMAT);
	}


	// get the timestamp string for the given time in the given format
	public static String getTimestamp (long time, String format){
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(time);
	}


	// get the timestamp string for the given time in the default format (yyMMddhhmmss)
	public static String getTimestamp (long time){
		return getTimestamp (time, TIMESTAMP_FORMAT);
	}


	// get the current timestamp in binary format. Note this will be UTC time
	public static long getTime(){
		return (new Date()).getTime();
	}

	/////////////////////////////////////////////////////////////
	// CURSOR UTILITIES
	/////////////////////////////////////////////////////////////

	// Debug utility to dump the fields and data of a cursor
	// Helps work around the lack of Android documentation ;-)
	public static void dumpCursor (Cursor c){
		int i, j, count ;

		count=c.getColumnCount();

		for(i=0;i<count;i++){
			Log.d(TAG, "Column "+i+": "+c.getColumnName(i)+",");
		}
		Log.d(TAG, "");

		String str;
		c.moveToFirst();
		while(true){
			for(j=0;j<count;j++){
				try {
					str = c.getString(j);
				}catch (Exception e){
					str = "Error converting to string";
				}
				Log.d(TAG, "Content("+j+"):"+str+",");
			}
			Log.d(TAG, "");
			if(c.isLast()){
				break;
			}else{
				c.moveToNext();
			}
		}
	}//dumpCursor



	/////////////////////////////////////////////////////////////
	// DEBUG UTILITIES
	/////////////////////////////////////////////////////////////


	// some simple logging routines to help with portability
	// These are the Android versions
	public static void logMessage (String msg) {
		Log.d ("", msg);
	}

	public static void logMessage (String tag, String msg) {
		Log.d (tag, msg);
	}

	public static void logError(String msg) {
		Log.e ("", msg);
	}

	public static void logError(String tag, String msg) {
		Log.e (tag, msg);
	}

	public static void showError(String msg) {
		Log.e ("", msg);
	}

	public static void showError(String tag, String msg) {
		Log.e (tag, msg);
	}

	/*
	 * print the status or result to the Android log. If the result is the expected
	 * result only print it to the log.  Otherwise print it to the error log and
	 * Sent a Toast to the users screen. 
	 */
	public static void logStatus(String msg, Object status, Object passStatus) {
		String log = String.format("%s: %s", msg, status);
		if (status == passStatus) {
			logMessage(log);
		} else {
			logError(log);
		}
	}

	/*
	 * When an exception is thrown use this to Toast the name of the exception 
	 * and send a log of the exception to the Android log.
	 */
	public static void logException(String msg, Exception e) {
		logException("", e);
	}

	public static void logException(String tag, String msg, Exception e) {
		String log = String.format("*** %s:\n %s", msg, e);
		logError(tag, log);
		// write full dump to log file (but not to display)
		// why is this such a pain to generate?!
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		logError(tag, "Exception details: "+errors.toString());
	}

} //Utilities
