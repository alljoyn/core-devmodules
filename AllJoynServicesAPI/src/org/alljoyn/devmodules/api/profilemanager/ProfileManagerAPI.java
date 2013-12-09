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
package org.alljoyn.devmodules.api.profilemanager;

import org.alljoyn.devmodules.APICoreImpl;
import org.alljoyn.devmodules.api.groups.GroupsCallbackObject;
import org.alljoyn.devmodules.callbacks.TransactionHandler;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import android.util.Log;

public class ProfileManagerAPI {

	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public static final String OBJECT = "profilemanager";

	private static ProfileManagerAPIInterface profileMgrInterface = null;

	// the callback object
	private static ProfileManagerCallbackObject mProfileManagerCallbackObject = null;

	private static void setupInterface() {
		boolean ready = APICoreImpl.getInstance().isReady;
		if (ready){
			if (mProfileManagerCallbackObject == null) mProfileManagerCallbackObject = new ProfileManagerCallbackObject();
			if(profileMgrInterface == null) {
				try {
					profileMgrInterface = APICoreImpl.getInstance().getProxyBusObject(OBJECT,
							new Class[] {ProfileManagerAPIInterface.class}).getInterface(ProfileManagerAPIInterface.class);
					Log.d(OBJECT, "Established interface to ProfileManagerConnector");
				} catch (Exception e){
					Log.e(OBJECT, "setupInterface(): Error getting profileMgrInterface");
				}
			}
		} else {
			Log.w(OBJECT, "setupInterface(): Connector core not ready!");
		}

		if (profileMgrInterface==null){
			Log.w(OBJECT, "setupInterface(): null profileMgrInterface");
		}
	}

	/* ---------- Listener Section ----------- */
	public static final int PROFILE_FOUND = 0;
	public static final int PROFILE_LOST = 1;

	public static void RegisterListener(ProfileManagerListener listener) {
		setupInterface();
		mProfileManagerCallbackObject.registerListener(listener);
	}

	/* ---------- API Section ----------- */

	public static ProfileDescriptor GetMyProfile() throws Exception {
		setupInterface();
		String jsonData = profileMgrInterface.GetMyProfile();
		if("".equals(jsonData)) {
			return null;
		}
		ProfileDescriptor ret = new ProfileDescriptor();
		ret.setJSONString(profileMgrInterface.GetMyProfile());
		return ret;
	}
	
	public static void SetMyProfile(ProfileDescriptor profile) throws Exception {
		setupInterface();
		profileMgrInterface.SetMyProfile(profile.getJSONString());
	}


	public static String[] GetNearbyUsers() throws Exception {
		setupInterface();
		return profileMgrInterface.GetNearbyUsers();
	}

	public static ProfileDescriptor GetProfileInfo(String peer) throws Exception {
		setupInterface();
		Log.d(OBJECT, "ProfileManagerConnector: placing call to getProfile("+peer+")");
		TransactionHandler th = new TransactionHandler();
		int transactionId = ProfileManagerCallbackObject.AddTransaction(th);
		try{
			profileMgrInterface.GetProfile(transactionId, peer);
		} catch (Exception e){
			Log.e(OBJECT, "Error calling GetProfile: "+e.toString());
		}
		th.latch.await();
		ProfileDescriptor ret = new ProfileDescriptor();
		if(th.getJSONData() != null)
			ret.setJSONObject(th.getJSONData());
		ret.setField("profileid", peer.substring(peer.lastIndexOf('.')+1));
		Log.d(OBJECT, "============================");
		Log.d(OBJECT, "============================");
		Log.d(OBJECT, "============================");

		return ret;
	}
	
	public static void GetProfileInfo(final String peer, final ProfileManagerAPICallback callback) throws Exception {
		setupInterface();
		Thread bgRequest = new Thread(new Runnable() {

			@Override
			public void run() {
				try{
					Log.d(OBJECT, "ProfileManagerConnector: placing call to getProfile("+peer+")");
					TransactionHandler th = new TransactionHandler();
					int transactionId = ProfileManagerCallbackObject.AddTransaction(th);
					try{
						profileMgrInterface.GetProfile(transactionId, peer);
					} catch (Exception e){
						Log.e(OBJECT, "Error calling GetProfile: "+e.toString());
					}
					th.latch.await();
					ProfileDescriptor ret = new ProfileDescriptor();
					if(th.getJSONData() != null)
						ret.setJSONObject(th.getJSONData());
					ret.setField("profileid", peer.substring(peer.lastIndexOf('.')+1));
					callback.onProfileInfoReady(peer, ret);
					Log.d(OBJECT, "============================");
					Log.d(OBJECT, "============================");
					Log.d(OBJECT, "============================");
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
			
		});
		bgRequest.start();
	}
}
