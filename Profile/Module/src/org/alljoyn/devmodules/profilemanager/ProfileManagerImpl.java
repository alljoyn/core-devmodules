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
package org.alljoyn.devmodules.profilemanager;

import java.util.ArrayList;
import org.alljoyn.bus.*;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.profilemanager.api.ProfileManagerAPIImpl;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.alljoyn.devmodules.interfaces.ModuleInterface;
import org.alljoyn.storage.ProfileCache;
import org.alljoyn.storage.ContactLoader;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class ProfileManagerImpl extends BusListener implements ModuleInterface  {	
	private final String TAG = "ProfileManagerImpl";
	private final String SESSION_NAME = "AllJoynServices" ;

	private ProfileManagerObject  mProfileManagerObject = new ProfileManagerObject();
	private String                mWellknownName;

	private AllJoynContainerInterface mAlljoynContainer;

	private static ProfileManagerImpl instance;
	public static ProfileManagerImpl getInstance() { return instance; }

	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private ImplHandler mHandler = new ImplHandler(handlerThread.getLooper()); // Handler for complex functions

	private ArrayList<String> mPeerList = new ArrayList<String>();
	private String namePrefix;

	private String            mProfileString;
	private ProfileDescriptor mProfile;
	private Context           mContext;


	// constructor
	public ProfileManagerImpl(AllJoynContainerInterface alljoynContainer, Context context) {

		Log.v(TAG, "Constructor()");
		ProfileCache.init();
		mAlljoynContainer = alljoynContainer;
		mContext = context;
		instance = this;
		mAlljoynContainer.getBusAttachment().registerBusListener(this);

		loadDefaultProfile();

	}

	public void RegisterSignalHandlers() {

	}

	private boolean hasDoneSetup = false;

	public void SetupSession() {
		if(hasDoneSetup)
			return;
		hasDoneSetup = true;

		Log.v(TAG, "SetupSession()");

		try{
			SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
			mAlljoynContainer.createSession(getAdvertisedName(), ProfileManagerConstants.SESSION_PORT, new SessionPortListener() {
				public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
					Log.d(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
					if (sessionPort == ProfileManagerConstants.SESSION_PORT) {
						return true;
					}
					return false;
				}

				public void sessionJoined(short sessionPort, int id, String joiner) {
					Log.d(TAG, "User: " + joiner + " joined., id=" + id);
					//processUserFound(name); // how do we find name (add tracking to CoreService)?
					scanUsers(); // remove this when above is fixed
				}
			}, sessionOpts);

			Log.i(TAG,"Advertised: "+getAdvertisedName());
		} catch (Exception e){
			Log.e(TAG, "SestupSession() exception: "+e.toString());
		}
	}


	public BusObject getBusObject() { 
		return mProfileManagerObject; 
	}

	public String getObjectPath() {
		return ProfileManagerConstants.OBJECT_PATH;
	}

	public String getAdvertisedName() {
		if(mWellknownName == null) {
			mWellknownName = ProfileManagerConstants.SERVICE_NAME+"."+mAlljoynContainer.getUniqueID();
		}
		return mWellknownName;
	}

	public String getServiceName() {
		return ProfileManagerConstants.SERVICE_NAME;
	}

	@Override
	public void foundAdvertisedName(String name, short transport, String namePrefix) {
		if(!name.contains(getAdvertisedName()) && name.startsWith(ProfileManagerConstants.NAME_PREFIX))
		{
			Log.i(TAG,"foundAdvertisedName: "+name);
			this.namePrefix = namePrefix;
			processUserFound(name);
		}
	}

	@Override
	public void lostAdvertisedName(String name, short transport, String namePrefix) {
		Log.i(TAG,"LostAdvertisedName: "+name);
		if(!name.contains(getAdvertisedName()) && transport != SessionOpts.TRANSPORT_LOCAL
				&& name.startsWith(ProfileManagerConstants.NAME_PREFIX))
		{
			processUserLeft(name);
		}
	}
	
	@Override
	public void nameOwnerChanged(String busName, String prevOwner, String newOwner) {

	}

	public void shutdown() {
		mAlljoynContainer.getSessionManager().destroySession(getAdvertisedName());
	}

	/* ************************************** */
	/* Module specific implementation methods */
	/* ************************************** */


	private void loadDefaultProfile(){
		// Default values
		mProfileString = ProfileDescriptor.EMPTY_PROFILE_STRING;
		mProfile = new ProfileDescriptor();

		try{

			// retrieve the default contact id (if any)
			String contactid = ProfileCache.retrieveContactId();
			Log.d(TAG, "contactId: "+contactid);
			
			if ((contactid!=null) && (contactid.length()>0)){

				// Load the contact info
				ContactLoader loader = new ContactLoader(mContext);
				mProfileString = loader.retrieveProfile(contactid);
				mProfile = new ProfileDescriptor();
				mProfile.setJSONString(mProfileString);
				mProfile.setProfileId(mAlljoynContainer.getUniqueID());

				// Save the contact info for use by other activities
				ProfileCache.saveProfile(mAlljoynContainer.getUniqueID(), mProfile);
			} else {
				Log.w(TAG, "loadDefaultProfile() Default contact not defined!");
			}
		} catch (Exception e){
			Log.e(TAG, "loadDefaultProfile() exception: "+e.toString());
		}
	}

	// return JSON String version of currently active profile
	public String getMyProfile(){
		try{
			if ((mProfileString==null) || (mProfileString.length()==0)){
				loadDefaultProfile();
			}
			Log.d(TAG, "getMyProfile(): mProfileString: "+mProfileString);
			return mProfileString;
		}catch(Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	public void setProfile(ProfileDescriptor profile) {

		if ((profile!=null)&&(!profile.isEmpty())){
			Log.d(TAG, "setting profile!!!! "+profile.getJSONString());
			String profName = profile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
			Log.d(TAG, "ProfName: "+profName);

			mProfile = profile;
			mProfile.setProfileId(mAlljoynContainer.getUniqueID());
			mProfileString = mProfile.getJSONString();

			// Save the contact info for use by other activities
			ProfileCache.saveProfile(mAlljoynContainer.getUniqueID(), mProfile);
		} else {
			Log.w(TAG, "setProfile() Empty profile provided!");
		}
	}

	// return list of currently active/nearby peers
	public ArrayList<String> getPeers() { 
		return mPeerList; 
	}

	// return the JSON profile data of the requested peer
	public String getProfileData(String peer) {

		Log.v(TAG, "getProfileData("+peer+")");
		String profileData = ProfileDescriptor.EMPTY_PROFILE_STRING;
		try {

			String service = getServiceName(peer);
			String session = getSessionName(peer);
			String id      = getUserid(peer);
			SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
			Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
			Status status = mAlljoynContainer.getSessionManager().joinSession(session, 
					ProfileManagerConstants.SESSION_PORT, 
					sessionId, 
					sessionOpts, 
					getProfileSessionListener);
			Log.i(TAG, "service: "+service+", session: "+session+", id: "+sessionId.value + ", status: "+status);
			if (sessionId.value==0){
				Log.w(TAG, "Session not set up for: "+session);
			} else {
				ProxyBusObject mProxyObj =  mAlljoynContainer.getBusAttachment().getProxyBusObject(service, 
						ProfileManagerConstants.OBJECT_PATH,
						sessionId.value,
						new Class<?>[] { ProfileManagerInterface.class });
				try {
					profileData = mProxyObj.getInterface(ProfileManagerInterface.class).GetPublicProfile();
					Log.d(TAG, "getProfileData() profileData:"+profileData);
					// Store profile for later use by apps (when peer may have gone)
					ProfileDescriptor profile = new ProfileDescriptor();
					profile.setJSONString(profileData);
					if (!profile.isEmpty()){
						profile.setProfileId(id); // make sure ID is saved (used as index)
						ProfileCache.saveProfile(id, profile);
						//DEBUG
						String name = profile.getField(ProfileDescriptor.ProfileFields.NAME_DISPLAY);
						Log.d(TAG, "Saved profile for: "+id+"("+name+")");
					} else {
						Log.e(TAG, "getProfileData() Empty profile returned, not saving");
						Log.w(TAG, "getProfileData() profileData: "+profileData);
					}
				} catch (Exception e) {
					Log.e (TAG, "getProfileData("+peer+") Exception: "+e.toString());
					e.printStackTrace();
				}
				mAlljoynContainer.getBusAttachment().leaveSession(sessionId.value);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error requesting private info ("+peer+"): "+e.toString());
			profileData=ProfileDescriptor.EMPTY_PROFILE_STRING;
		}

		if (profileData.equals(ProfileDescriptor.EMPTY_PROFILE_STRING)){
			profileData = getProfileFromCache(peer);
		}
		return profileData;
	}

	// handler for when the session setup has completed
	private SessionListener getProfileSessionListener = new SessionListener(){
		// called when the session is lost
		public void sessionLost(int sessionId) {
			Log.d(TAG, "getProfileSessionListener.sessionLost("+sessionId+")");
		}

		// called when someone joins an already-established session
		public void sessionMemberAdded(int sessionId, String joiner) {
			Log.d(TAG, "getProfileSessionListener.sessionMemberAdded("+sessionId+", "+joiner+")");
		}

		// Called when someone leaves a hosted session
		public void sessionMemberRemoved(int sessionId, String joiner) {
			Log.d(TAG, "getProfileSessionListener.sessionMemberRemoved("+sessionId+", "+joiner+")");
		}
	};


	// Utility to retrieve a profile from Cache and convert to String form
	public String getProfileFromCache(String peer){
		String pstring = ProfileDescriptor.EMPTY_PROFILE_STRING;
		if (ProfileCache.isPresent(peer)){
			pstring = ProfileCache.getProfile(peer).toString();
		} else {
			Log.w(TAG, "Profile not found in cache. Returning empty profile");
		}
		return pstring;
	}


	// called by Core Service framework
	@Override
	public void InitAPI(AllJoynContainerInterface coreLogic) {
		setupProfileSession();
	}


	/* ****************************************************************** */
	/* Module specific implementation methods (running on handler thread) */
	/* ****************************************************************** */

	//Define constants for Handler events
	private static final int SETUP_SESSION  = 1; 
	private static final int SCAN_USERS     = 2; 
	private static final int USER_FOUND     = 3; 
	private static final int GET_PROFILE    = 4; 
	private static final int USER_LEFT      = 5; 


	private void setupProfileSession (){
		mHandler.sendEmptyMessage(SETUP_SESSION);	
	}

	private void scanUsers (){
		mHandler.sendEmptyMessage(SCAN_USERS);	
	}

	private void processUserFound (String service){
		Message msg = mHandler.obtainMessage(USER_FOUND);
		Bundle data = new Bundle();
		data.putString("service", service);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	private void getProfile (String service){
		Message msg = mHandler.obtainMessage(GET_PROFILE);
		Bundle data = new Bundle();
		data.putString("service", service);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	private void processUserLeft (String service){
		Message msg = mHandler.obtainMessage(USER_LEFT);
		Bundle data = new Bundle();
		data.putString("service", service);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}


	// Use this Handler for asynchronous transactions
	// Note that there may be several transactions active in parallel, so don't assume a single user
	private class ImplHandler extends Handler
	{
		public ImplHandler(Looper loop) {
			super(loop);
		}
		public void handleMessage(Message msg) {
			// frequently-used variables
			String service;
			Bundle data = msg.getData();

			switch(msg.what) {
			case SETUP_SESSION: {
				SetupSession();
				scanUsers();
				break;
			}
			case SCAN_USERS: {
				doScanUsers();
				break;
			}
			case USER_FOUND: {
				service = data.getString("service");
				doUserFound(service);
				break;
			}
			case GET_PROFILE: {
				service = data.getString("service");
				doGetProfile(service);
				break;
			}
			case USER_LEFT: {
				service = data.getString("service");
				doUserLeft(service);
				break;
			}
			default:
				Log.e(TAG, "ImplHandler unknown msg type: "+msg.what);
				break;

			} // switch
		} // handleMessage


		// handler for scanning list of currently known users
		private void doScanUsers(){
			Log.v(TAG, "doScanUsers()");
			try {
				DBusProxyObj dobj = mAlljoynContainer.getBusAttachment().getDBusProxyObj();
				// get the list of known services
				String[] svclist = dobj.ListNames();
				for (String s: svclist){
					// if it's not this service, and it's a profile service, then process it
					if(!s.contains(getAdvertisedName()) && s.startsWith(ProfileManagerConstants.NAME_PREFIX)){
						getProfile(s);
					}
				}
			} catch (BusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}



		// handler for when new user is found
		private void doUserFound(String service){
			doGetProfile(service);
		}

		// handler for getting profile from remote user
		private void doGetProfile(String service){
			String id = getUserid(service);
			// retrieve the (string form) of the profile (this also saves it to cache)
			String pstring = getProfileData(service);

			// notify clients that profile is available
			notifyProfileFound(service);
		}


		// handler for when user leaves
		private void doUserLeft(String service){
			String name = getUserid(service);
			try { 
				mPeerList.remove(name);
				if (ProfileManagerAPIImpl.profileCallback != null)
					ProfileManagerAPIImpl.profileCallback.onProfileLost(name);
			} catch (Exception e) {
				Log.e(TAG, "Error sending onProfileLost("+name+")");
				e.printStackTrace();
			}
		}

	}// ImplHandler


	// Notify the clients that a new user has been found
	private void notifyProfileFound(String service){
		String name = getUserid(service);
		mPeerList.add(name);
		try { 
			if (ProfileManagerAPIImpl.profileCallback != null){
				Log.v(TAG, "notifyProfileFound("+name+")");
				ProfileManagerAPIImpl.profileCallback.onProfileFound(name);
			} else {
				Log.e(TAG, "notifyProfileFound() NULL profileCallback !!!");
			}
		} catch (Exception e) {
			Log.e(TAG, "Error sending onProfileFound("+name+")");
			e.printStackTrace();
		}
	}

	// Utility to extract the unique ID from a service/session/userid string
	private String getUserid(String userid){
		String id = userid;
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
		return id;
	}

	// Utility to build a Profile service name from the unique userid
	private String getServiceName(String userid){
		String svc = userid;
		if (svc.contains(".")) svc = svc.substring(svc.lastIndexOf(".")+1);
		svc = ProfileManagerConstants.NAME_PREFIX + "." + svc;
		return svc;
	}

	// Utility to build a Groups session name from the unique userid
	private String getSessionName(String userid){
		String svc = userid;
		if (svc.contains(".")) svc = svc.substring(svc.lastIndexOf(".")+1);
		svc = ProfileManagerConstants.SERVICE_NAME + "." + svc;
		return svc;
	}


}//ProfileManagerImpl

