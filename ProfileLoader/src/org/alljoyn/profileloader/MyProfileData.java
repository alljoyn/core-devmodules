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

package org.alljoyn.profileloader;

import org.alljoyn.devmodules.api.profilemanager.ProfileManagerAPI;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.storage.ProfileCache;


import android.util.Log;


/*
 * Class to hold profile data of the current user
 * This is a Singleton class, i.e. can be accessed from different scopes.
 * It is also thread safe, which is why all variables are accessed via a method
 */
public class MyProfileData  {

	private static final String  TAG = "MyProfileData";

	private static ProfileDescriptor _profile;

	// prevent class from being instantiated via new constructor
	private MyProfileData (){
		// do nothing
	}

	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// method to return reference to internal data
	public static synchronized ProfileDescriptor getProfile() {
		if (_profile == null) {
			try {
				_profile = ProfileManagerAPI.GetMyProfile();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return _profile;
	}

	// method to determine whether the profile has been set up or not
	public static synchronized boolean isSet() {
		if (_profile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY).length()>0){
			return true;
		} else {
			return false;
		}
	}

	// clear/reset the current profile data
	public static synchronized void clear(){

	}

	// Methods to get/set data (thread safe)
	// Note that we should not return null values for AllJoyn methods (how can you send a null value)
	// so the 'checkXYZ' functions make sure that a non-null value is returned

	public static synchronized String getSvcName (){ 
		return _profile.getField("profileid"); 
	}

	public static synchronized void setProfile (ProfileDescriptor profile){ 
		Log.d(TAG, "My Profile set to:\n"+profile.getJSONString());
		_profile = profile; 
		// Also save the profile to storage
		ProfileCache.saveProfile(getSvcName(), _profile);
	}



} // MyProfileData
