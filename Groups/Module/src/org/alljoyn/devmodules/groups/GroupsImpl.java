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

package org.alljoyn.devmodules.groups;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alljoyn.bus.*;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.devmodules.sessionmanager.SessionManager;
import org.alljoyn.groups.api.GroupsAPIImpl;
import org.alljoyn.devmodules.common.GroupDescriptor;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.alljoyn.devmodules.interfaces.ModuleInterface;
import org.alljoyn.storage.GroupCache;
import org.alljoyn.storage.ProfileCache;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class GroupsImpl extends BusListener implements ModuleInterface  {
	private final static String TAG = "GroupsImpl";

	private static GroupsImpl mInstance;
	public static GroupsImpl getInstance() { return mInstance; }

	private GroupsObject              mGroupsObject = new GroupsObject();
	private GroupsInterface           mGroupsInterface;

	private AllJoynContainerInterface mAlljoynContainer=null;
	private String                    myWellknownName;
	private String                    mMyId="";
	private int                       mSessionId = 0;

	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private ImplHandler mHandler = new ImplHandler(handlerThread.getLooper()); // mHandler for complex functions

	private static HashMap<String,ProxyBusObject> mRemoteObjectList;
	private static GroupsTestImpl mGroupsTest=null;
	private boolean mSetup=false;



	/**
	 * Constructor - called by Services Framework
	 * @param alljoynContainer Reference to an object containing the main AllJoyn objects (bus attachment, session manager etc.)
	 */
	public GroupsImpl(AllJoynContainerInterface alljoynContainer) {
		try{
			Log.v(TAG, "GroupsImpl()");

			this.mAlljoynContainer = alljoynContainer;
			mInstance = this;

			mMyId = mAlljoynContainer.getUniqueID();

			// make sure Groups cache is initialised
			GroupCache.init();

			// init remote object list
			mRemoteObjectList = new HashMap<String,ProxyBusObject>();
			mRemoteObjectList.clear();
		} catch (Exception e){
			Log.e(TAG, "GroupsImpl() error: "+e.toString());
		}
	}

	public void SetupSession() {


		// register listeners;
		if (mInstance==null) mInstance = this;
		mAlljoynContainer.getBusAttachment().registerBusListener(mInstance);

		//String session = getAdvertisedName();
		String session = getSessionName(getAdvertisedName());
		Log.v(TAG, "SetupSession("+session+")");

		/***
		 * This processing has moved to GroupSessionManager

		SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
		Log.v(TAG, "Creating Session: "+session+", port: "+GroupsConstants.SESSION_PORT);

		if (mAlljoynContainer != null){
			mAlljoynContainer.createSession(session, GroupsConstants.SESSION_PORT, new SessionPortListener() {

				// access approval callback
				public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
					Log.d(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
					if (sessionPort == GroupsConstants.SESSION_PORT) {
						return true;
					}
					return false;
				}

				// session joined callback
				public void sessionJoined(short sessionPort, int id, String joiner) {
					try{
						Log.d(TAG, "sessionJoined(port=" + sessionPort  + ", id=" + id+ ",joiner=" + joiner+")");
						mSessionId = id;
					} catch (Exception e){
						Log.e(TAG, "sessionJoined error: "+e.toString());
					}
				}
			}, sessionOpts);
			Log.d(TAG,"Advertised: "+getAdvertisedName());
		} else {
			Log.e(TAG, "AlljoynContainer not set up yet!!!");
		}
		 ***/
	}

	public BusObject getBusObject() { 
		return mGroupsObject; 
	}

	public GroupsInterface getInterface() { 
		return mGroupsInterface; 
	}

	public String getObjectPath() {
		return GroupsConstants.OBJECT_PATH;
	}

	public String getAdvertisedName() {
		if(myWellknownName == null) {
			myWellknownName = GroupsConstants.SERVICE_NAME+"."+mAlljoynContainer.getUniqueID();
		}
		return myWellknownName;
	}

	public String getServiceName() {
		return GroupsConstants.SERVICE_NAME;
	}

	public void foundAdvertisedName(String name, short transport, String namePrefix) {
		// check that it's the right kind of service, then check that it's not my service
		if(!name.contains(getAdvertisedName()) && name.startsWith(GroupsConstants.NAME_PREFIX))
			Log.v(TAG, "foundAdvertisedName("+name+")");
		name = name.substring(name.lastIndexOf(".")+1);
		try { 
			Log.v(TAG, "Processing groups for: "+name);
			processNewUser(name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void lostAdvertisedName(String name, short transport, String namePrefix) {
		Log.d(TAG,"LostAdvertisedName: "+name);
		if (name.contains(getServiceName())){
			if(!name.contains(getUserid(getAdvertisedName()))) {
				name = name.substring(name.lastIndexOf(".")+1);
				try { 
					processUserLeft(name);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public String getId() {
		return mMyId;
	}

	public void RegisterSignalHandlers() {
		mAlljoynContainer.getBusAttachment().registerSignalHandlers(this);
	}

	@Override
	public void InitAPI(AllJoynContainerInterface coreLogic) {
		try{

			// Initialise the Groups-specific processing
			start();

		} catch (Exception e){
			Log.e(TAG, "InitAPI() error: "+e.toString());
		}
	}

	public void shutdown() {
		mHandler.sendEmptyMessage(SHUTDOWN);
	}

	public SessionManager getSessionManager(){
		return mAlljoynContainer.getSessionManager();
	}


	/* ************************************** */
	/* Module specific Signal Handlers        */
	/* ************************************** */
	@BusSignalHandler(iface=GroupsConstants.NAME_PREFIX, signal="GroupUpdated")
	public void GroupUpdated(String group, String member)  {
		try{
			Log.d(TAG, "GroupUpdated("+group+", "+member+")");

			//mAlljoynContainer.getBusAttachment().enableConcurrentCallbacks();

			if ((mMyId==null)||(mMyId.length()==0)){
				mMyId = mAlljoynContainer.getUniqueID();
			}

			if (member!=mMyId){
				scanGroups(group, member);
			}
		} catch (Exception e){
			Log.d(TAG, "GroupUpdated("+group+", "+member+") - Error: "+e.toString());
		}
	}

	/* ************************************** */
	/* Module specific implementation methods */
	/* ************************************** */

	//Define constants for mHandler events
	private static final int START               = 0; 
	private static final int SHUTDOWN            = 1; 
	private static final int NEW_USER            = 2; 
	private static final int USER_LEFT           = 3; 
	private static final int START_GROUP         = 4; 
	private static final int STOP_GROUP          = 5; 
	private static final int ENABLE_GROUP        = 6; 
	private static final int DISABLE_GROUP       = 7; 
	private static final int INVITE_USER         = 8; 
	private static final int ACCEPT_INVITATION   = 9; 
	private static final int REJECT_INVITATION   = 10; 
	private static final int SCAN_GROUPS         = 11; 
	private static final int GET_GROUP_INFO      = 12; 
	private static final int GET_MEMBER_PROFILE  = 13; 
	private static final int REQUEST_NEW_MEMBERS = 14; 
	private static final int SETUP_GROUP_SESSION = 15; 
	private static final int RUN_GROUPS_TEST     = 16; 


	private void start (){
		mHandler.sendEmptyMessage(START);
	}

	private void processNewUser (String userid){
		Message msg = mHandler.obtainMessage(NEW_USER);
		Bundle data = new Bundle();
		data.putString("userid", getUserid(userid));
		msg.setData(data);
		mHandler.sendMessageDelayed(msg, 100);	
	}

	private void processUserLeft (String userid){
		Message msg = mHandler.obtainMessage(USER_LEFT);
		Bundle data = new Bundle();
		data.putString("userid", getUserid(userid));
		msg.setData(data);
		mHandler.sendMessageDelayed(msg, 100);	
	}

	public void startGroup (String group){
		Message msg = mHandler.obtainMessage(START_GROUP);
		Bundle data = new Bundle();
		data.putString("group", group);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void stopGroup (String group){
		Message msg = mHandler.obtainMessage(STOP_GROUP);
		Bundle data = new Bundle();
		data.putString("group", group);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void enableGroup (String group){
		Message msg = mHandler.obtainMessage(ENABLE_GROUP);
		Bundle data = new Bundle();
		data.putString("group", group);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void disableGroup (String group){
		Message msg = mHandler.obtainMessage(DISABLE_GROUP);
		Bundle data = new Bundle();
		data.putString("group", group);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void inviteUser (String userid, String group){
		Message msg = mHandler.obtainMessage(INVITE_USER);
		Bundle data = new Bundle();
		data.putString("group", group);
		data.putString("userid", getUserid(userid));
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void acceptInvitation (String userid, String group){
		String arg1 = getUserid(userid);
		String arg2 = group;
		Message msg = mHandler.obtainMessage(ACCEPT_INVITATION);
		Bundle data = new Bundle();
		data.putString("userid", arg1);
		data.putString("group", arg2);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void rejectInvitation (String userid, String group){
		String arg1 = getUserid(userid);
		String arg2 = group;
		Message msg = mHandler.obtainMessage(REJECT_INVITATION);
		Bundle data = new Bundle();
		data.putString("userid", arg1);
		data.putString("group", arg2);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void scanGroups (String group, String member){
		scanGroups (getSessionName(member));
	}

	public void scanGroups (String session){
		Message msg = mHandler.obtainMessage(SCAN_GROUPS);
		Bundle data = new Bundle();
		data.putString("session", session); // do not strip prefix
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	private void getGroupInfo (String session, String group){
		Message msg = mHandler.obtainMessage(GET_GROUP_INFO);
		Bundle data = new Bundle();
		data.putString("session", session);
		data.putString("group", group);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	private void getMemberProfile (String session, String group, String userid){
		Message msg = mHandler.obtainMessage(GET_MEMBER_PROFILE);
		Bundle data = new Bundle();
		data.putString("session", session); // do not strip prefix
		data.putString("group", group);
		data.putString("userid", getUserid(userid));
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void setupGroupSession(String session, String group, String userid){
		Message msg = mHandler.obtainMessage(SETUP_GROUP_SESSION);
		Bundle data = new Bundle();
		data.putString("session", session); // do not strip prefix
		data.putString("group", group);
		data.putString("userid", getUserid(userid));
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void requestNewMembers (String session, String group, String[] members){
		Message msg = mHandler.obtainMessage(REQUEST_NEW_MEMBERS);
		Bundle data = new Bundle();
		data.putString("session", session); // do not strip prefix
		data.putString("group", group);
		data.putStringArray("members", members);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void runGroupsTest()  {
		mHandler.sendEmptyMessage(RUN_GROUPS_TEST);	
	}

	// Use this Handler for asynchronous transactions
	// Note that there may be several transactions active in parallel, so don't assume a single user
	private class ImplHandler extends Handler
	{
		public ImplHandler(Looper loop) {
			super(loop);
		}
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			// frequently-used variables
			String userid;
			String group;
			String session;
			String[] members;

			switch(msg.what) {
			case START: {
				Log.d(TAG,"START");
				doStart();
				break;
			}
			case SHUTDOWN: {
				Log.d(TAG,"SHUTDOWN");
				doShutdown();
				break;
			}
			case NEW_USER:
				userid = data.getString("userid");
				Log.d(TAG,"NEW_USER: "+userid);
				doProcessNewUser(userid);
				break;

			case USER_LEFT:
				userid = data.getString("userid");
				Log.d(TAG,"USER_LEFT: "+userid);
				doProcessUserLeft(userid);
				break;

			case START_GROUP:
				group = data.getString("group");
				Log.d(TAG,"START_GROUP: "+group);
				doStartGroup(group);
				break; 

			case STOP_GROUP:
				group = data.getString("group");
				Log.d(TAG,"STOP_GROUP: "+group);
				doStopGroup(group);
				break; 


			case ENABLE_GROUP:
				group = data.getString("group");
				Log.d(TAG,"ENABLE_GROUP: "+group);
				doEnableGroup(group);
				break; 

			case DISABLE_GROUP:
				group = data.getString("group");
				Log.d(TAG,"DISABLE_GROUP: "+group);
				doDisableGroup(group);
				break; 

			case INVITE_USER :
				userid = data.getString("userid");
				group = data.getString("group");
				Log.d(TAG,"INVITE_USER: "+userid);
				doInviteUser(userid, group);
				break;

			case ACCEPT_INVITATION:
				userid = data.getString("userid");
				group = data.getString("group");
				Log.d(TAG,"ACCEPT_INVITATION: "+userid);
				//TODO (if user intervention required)
				break; 

			case  REJECT_INVITATION:
				userid = data.getString("userid");
				group = data.getString("group");
				Log.d(TAG,"REJECT_INVITITATION: "+userid);
				break;

			case  SCAN_GROUPS:{
				session = data.getString("session");
				Log.d(TAG,"SCAN_GROUPS: "+session);
				doScanGroups(session);
				break;
			}

			case  GET_GROUP_INFO:
				session = data.getString("session");
				group = data.getString("group");
				doGetGroupInfo (session, group);
				break;

			case  GET_MEMBER_PROFILE:
				session = data.getString("session");
				group = data.getString("group");
				userid = data.getString("userid");
				doGetMemberProfile (session, group, userid);
				break;

			case  SETUP_GROUP_SESSION:
				session = data.getString("session");
				group = data.getString("group");
				userid = data.getString("userid");
				doSetupGroupSession (session, group, userid);
				break;

			case  REQUEST_NEW_MEMBERS:
				session = data.getString("session");
				group = data.getString("group");
				members = data.getStringArray("members");
				doRequestNewMembers (session, group, members);
				break;

			case  RUN_GROUPS_TEST:
				doRunGroupsTest ();
				break;


			default:
				Log.e(TAG, "ImplHandler unknown msg type: "+msg.what);
				break;

			} // switch
		} // handleMessage

		// Handler function to start 
		private void doStart(){
			try{


				SetupSession();

				//mAlljoynContainer.getBusAttachment().enableConcurrentCallbacks();

				mMyId = mAlljoynContainer.getUniqueID();

				// Set up the group session manager (before registering bus listener)
				GroupSessionManager.init();
				GroupSessionManager.setSessionManager(mAlljoynContainer.getSessionManager());
				GroupSessionManager.setMyId(mMyId);
				GroupSessionManager.registerCallback(mGroupSessionCallback);


				// Crank up the debug level for testing only
				mAlljoynContainer.getBusAttachment().useOSLogging(true);
				//mAlljoynContainer.getBusAttachment().setDaemonDebug("ALLJOYN", 7);
				mAlljoynContainer.getBusAttachment().setDebugLevel("ALLJOYN", 3);
				//mAlljoynContainer.getBusAttachment().setLogLevels("ALL=7");

				// Set up the signal emitter
				if (mGroupsObject==null) mGroupsObject = new GroupsObject();
				SignalEmitter sigEm = new SignalEmitter(mGroupsObject, SignalEmitter.GlobalBroadcast.On);
				mGroupsInterface = sigEm.getInterface(GroupsInterface.class);
				if (mGroupsInterface==null){
					Log.e(TAG, "ummmmm. Null groupsInterface returned...");
				}

				// start the public group
				startGroup(GroupsConstants.PUBLIC_GROUP);

				// start all of the group sessions
				String[] glist = GroupManager.getGroupList();
				for (String g: glist){
					startGroup(g);
				}


				// set up the test mHandlers (need to be ready to handle requests)
				if (mGroupsTest==null) {
					Log.i(TAG, "Creating GroupsTest Implementation");
					mGroupsTest = new GroupsTestImpl(mAlljoynContainer);
				}

				Status status = mAlljoynContainer.getBusAttachment().registerSignalHandlers(mInstance);
				if (status != Status.OK){
					Log.w(TAG, "Error registering signal handlers: "+status.toString());
				}

				mSetup = true;

			} catch (Exception e){
				Log.e(TAG, "doStart() exception: "+e.toString());
				e.printStackTrace();
			}
		}

		// Handler function to do shutdown processing
		private void doShutdown(){
			GroupSessionManager.registerCallback(mGroupSessionCallback);
			String[] glist = GroupManager.getGroupList();
			for (String g: glist){
				stopGroup(g);
			}
			disconnectFromService(getAdvertisedName());
		}

		// Handler function to start a named group
		private void doStartGroup(String group){
			try{
				GroupDescriptor gd = GroupCache.retrieveGroupDetails(group);
				if (gd.isEnabled()){
					if (!GroupManager.isActive(group)){
						if(GroupSessionManager.startSession(group)){
							Log.d(TAG, "Activating group: "+group);
							GroupManager.setActive(group);
							// issue a signal so that other devices will scan
							if (mGroupsInterface!=null){
								Log.v(TAG, "Sending GroupUpdated("+group+", "+mMyId+") signal");
								mGroupsInterface.GroupUpdated(group, mMyId);
							}
							else
								Log.e(TAG, "Null GroupsInterface!");

						} else {
							Log.e(TAG, "Error starting group session for: "+group);
						}
					} else {
						Log.w(TAG, "doEnableGroup() Session already active for group: "+group);
					}
				} else {
					Log.d(TAG, "doStartGroup("+group+") Group is disabled, not starting");
				}
			} catch (Exception e){
				Log.e(TAG, "doEnableGroup() Error: "+e.toString());
				e.printStackTrace();
			}
		}

		// Handler function to enable a named group
		private void doEnableGroup(String group){
			try{
				GroupManager.enable(group); // this is a persistent function
				startGroup(group);
			} catch (Exception e){
				Log.e(TAG, "doEnableGroup() Error: "+e.toString());
				e.printStackTrace();
			}
		}

		// Handler function to stop a named group
		private void doStopGroup(String group){
			if ((group==null) || (group.length()==0)){
				GroupSessionManager.stopSession(GroupsConstants.PUBLIC_GROUP);
			} else {
				if (GroupManager.isActive(group)){
					Log.d(TAG, "Stopping group: "+group);
					GroupSessionManager.stopSession(group);
					GroupManager.setInactive(group);
				}
			}
		}

		// Handler function to disable a named group
		private void doDisableGroup(String group){
			if (group.length()>0){
				Log.d(TAG, "Disabling group: "+group);
				GroupManager.disable(group); // this is a persistent function
				stopGroup(group);
			} else {
				Log.w(TAG, "Cannot disable public group");
			}
		}

		// Handler function to request a user to join a group
		private void doInviteUser(String userid, String group){
			ProxyBusObject proxy = null;
			String session = getSessionName(userid); // ensure format is correct
			String id = getUserid(userid);

			try {
				if (!id.equals(mMyId)){
					if (mRemoteObjectList.containsKey(session)){
						// get the proxy object for this session
						proxy = mRemoteObjectList.get(session);

						if (proxy != null){
							// get the list of groups on the remote device
							Log.v(TAG, "doInviteUser() Getting group list for: "+id);
							GroupDescriptor gd = GroupManager.getGroupDescriptor(group);
							proxy.getInterface(GroupsInterface.class).Invite(mMyId, group, gd.toString());
						} else {
							Log.w(TAG, "doInviteUser() Null proxy object for session: "+session);
						}
						Log.w(TAG, "doInviteUser() Unkown session: "+session);

					}
				}
			} catch (Exception e){
				Log.e(TAG, "doInviteUser() error: "+e.toString());
			}

		}

		// Handler function to process a new user
		private void doProcessNewUser(String userid){
			try{
				if (mSetup){
					String id = getUserid(userid);
					if (!id.equals(mMyId)){
						SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
						Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
						String session = getSessionName(userid);
						String service = getServiceName(userid);
						String sname = session;
						// Check to if we have already processed this user
						//Since only adding from foundAdvertisedName now lets always process because found/lost advertisedName will give us
						//accurate information about the world around us
						//if (!mRemoteObjectList.containsKey(sname)) {

							Log.v(TAG, "Attempting to join session for: "+service);
							// Join the (main) session for the groups service
							/*** moved to GroupSessionManager
							Status status = mAlljoynContainer.getSessionManager().joinSession(sname, GroupsConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());
							//Status status = mAlljoynContainer.getSessionManager().joinSession(getAdvertisedName(), GroupsConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());
							if ((status == Status.OK)){
							***/
							if (GroupSessionManager.joinSession(GroupsConstants.PUBLIC_GROUP, sname, GroupsConstants.SESSION_PORT)){
								Log.v(TAG, "doProcessNewUser("+sname+") SessionID="+sessionId.value);

								Log.v(TAG, "Getting ProxyObject("+service+", "+GroupsConstants.OBJECT_PATH+
										", "+sessionId.value+")");
								// save a reference to the remote object for later use

								ProxyBusObject proxy = mAlljoynContainer.getBusAttachment().getProxyBusObject(service, 
										GroupsConstants.OBJECT_PATH,
										sessionId.value,
										new Class<?>[] { GroupsInterface.class });
								if (proxy != null){
									mRemoteObjectList.put(sname, proxy);

									// initiate synch protocol for groups for this service
									scanGroups(sname);
								} else {
									Log.e(TAG, "doProcessNewUser("+userid+") null proxy object");
								}
							} else {
								//Log.e(TAG, "doProcessNewUser() joinSession failed. Status: "+status.toString());
								Log.e(TAG, "doProcessNewUser() joinSession failed.");
								//DEBUG
								//Log.v(TAG, "All Sessions:    "+mAlljoynContainer.getSessionManager().listSessions());
								Log.v(TAG, "Hosted Sessions: "+mAlljoynContainer.getSessionManager().listHostedSessions());
								Log.v(TAG, "Joined Sessions: "+mAlljoynContainer.getSessionManager().listJoinedSessions());
							}
//						} else {
//							Log.d(TAG, "User("+userid+") already processed. Ignoring");
//						}
					} else {
						Log.i(TAG, "doProcessNewUser("+userid+") Ignoring myself");
					}
				}
			} catch (Exception e){
				Log.e(TAG, "doProcessNewUser() error: "+e.toString());
			}
		}

		// Query remote service for list of groups and process results
		private void doScanGroups(String session){
			ProxyBusObject proxy = null;
			String sname = getSessionName(session); // ensure format is correct
			String userid = getUserid(session);
			//String sname = getServiceName(session);
			try {
				if (!userid.equals(mMyId)){
					if (mRemoteObjectList.containsKey(sname)){
						// get the proxy object for this session
						proxy = mRemoteObjectList.get(sname);

						if (proxy != null){
							Log.v(TAG, "doScanGroups() ProxyObject(Bus:"+proxy.getBusName()+", Path:"+proxy.getObjPath()+")"); //DEBUG

							// get the list of groups on the remote device
							Log.v(TAG, "doScanGroups() Getting group list for: "+sname);
							String[] glist = proxy.getInterface(GroupsInterface.class).GetGroupList();

							Log.v(TAG, "doScanGroups() Found "+glist.length+" groups for: "+sname);
							// scan through groups
							for (String g: glist){
								// If we have the same group, then process
								if (GroupCache.isGroupDetailsPresent(g)){
									GroupDescriptor gd = GroupCache.retrieveGroupDetails(g);

									// check that group is enabled
									if (gd.isEnabled()){

										// Check whether group is private
										if (gd.isPrivate()){
											Log.d(TAG, "doScanGroups() Starting synch of group: "+g);
											getGroupInfo(session, g);
										} else {
											Log.d(TAG, "doScanGroups() S group: '"+g+"'");
											// just establish the session, no need to synch contents of group
											setupGroupSession(sname, g, getUserid(session));
										}
									} else {
										Log.d(TAG, "doScanGroups() Group ("+g+") known but not enabled, ignoring");
									}
								} else {
									Log.d(TAG, "doScanGroups() Ignoring group: '"+g+"'");
								}
							}

							// OK, now scan through my own private groups and see if there any for 
							// which this user is a member, but they don't know it yet
							String [] privlist = GroupManager.getPrivateGroupList();
							List<String> garray = Arrays.asList(glist);
							for (String g: privlist){
								if (!garray.contains(g)){
									inviteUser(getUserid(session), g);
								}
							}
						} else {
							Log.e(TAG, "doScanGroups() Null ProxyObject for: "+sname);
						}
					} else {
						Log.e(TAG, "doScanGroups() No ProxyObject for: "+sname);
					}
				}
			} catch (Exception e){
				Log.e(TAG, "doScanGroups("+sname+") error: "+e.toString());
				//DEBUG
				//Log.v(TAG, "All Sessions:    "+mAlljoynContainer.getSessionManager().listSessions());
				Log.v(TAG, "Hosted Sessions: "+mAlljoynContainer.getSessionManager().listHostedSessions());
				Log.v(TAG, "Joined Sessions: "+mAlljoynContainer.getSessionManager().listJoinedSessions());
			}
		}


		// Handler function to collect group information and process accordingly
		private void doGetGroupInfo (String session, String group){
			Log.d(TAG, "doGetGroupInfo("+session+", "+group+")");

			ProxyBusObject proxy = null;
			String sname = getSessionName(session); // ensure format is correct

			try {
				// Retrieve Proxy object for the supplied session
				if (mRemoteObjectList.containsKey(sname)){
					// get the proxy object for this session
					proxy = mRemoteObjectList.get(sname);

					if (proxy != null){
						// Retrieve the GroupDescriptor from the remote device
						String gstring = proxy.getInterface(GroupsInterface.class).GetGroupDescriptor(group);
						GroupDescriptor remGD = new GroupDescriptor();
						remGD.setString(gstring);

						// Get the local version
						GroupDescriptor lclGD = GroupManager.getGroupDescriptor(group);

						// find the list of users missing on both sides
						Set<String> lclMembers    = new HashSet<String>(); // the list of users already in the local group
						Set<String> remMembers    = new HashSet<String>(); // the list of users already in the remote group
						Set<String> lclNewMembers ;                        // the list of users in the remote group, but not local group
						Set<String> remNewMembers ;                        // the list of users in the local group, but not remote group
						lclMembers.clear();
						remMembers.clear();

						// Populate the sets
						String[] list = lclGD.getMembers();
						String membertxt = "Local Members of group: "+group+" [" ;
						for (int i=0; i<list.length; i++){
							lclMembers.add(list[i]);
							membertxt += list[i]+" ";
						}
						Log.v(TAG, membertxt+"]");

						list = remGD.getMembers();
						membertxt = "Remote Members of group: "+group+" [" ;
						for (int i=0; i<list.length; i++){
							remMembers.add(list[i]);
							membertxt += list[i]+" ";
						}
						Log.v(TAG, membertxt+"]");

						// Get the list of new users for the local list
						lclNewMembers = new HashSet<String>(remMembers);
						lclNewMembers.removeAll(lclMembers);

						// loop through list and retrieve profile info
						for (String member: lclNewMembers){
							getMemberProfile(session, group, member);
						}

						// Add new members to the local group
						if (lclNewMembers.size()>0){
							GroupManager.addMembers(group, lclNewMembers.toArray(new String[lclNewMembers.size()]));
							mGroupsObject.GroupUpdated(group, mAlljoynContainer.getUniqueID());
						} else {
							Log.d(TAG, "No new members for group: "+group);
						}


						//Establish session to remote device for the group
						setupGroupSession(sname, group, getUserid(session));

						/** TODO: request addition to remote list
						 * Is this needed?! The other side will synch too

						// Get the list of new users for the remote list
						remNewMembers = new HashSet(remMembers);
						remNewMembers.removeAll(lclMembers);
						 ***/
					} else {
						Log.e(TAG, "doGetGroupInfo() Null ProxyObject for: "+sname);
					}
				} else {
					Log.e(TAG, "doGetGroupInfo() No ProxyObject for: "+sname);
				}
			} catch (Exception e){
				Log.e(TAG, "doGetGroupInfo("+sname+") error: "+e.toString());
			}

		}


		// Handler function to retrieve and save the profile for member of a group
		private void doGetMemberProfile (String session, String group, String userid){
			Log.d(TAG, "doGetMemberProfile("+session+", "+group+", "+userid+")");


			ProxyBusObject proxy = null;
			String sname = getSessionName(session); // ensure format is correct

			try {
				// check to see if we already have the profile (or it's present, but 'old')
				if((!ProfileCache.isPresent(userid)) || 
						(ProfileCache.isFileOlderThan(ProfileCache.getProfilePath(userid), 10))){
					// Retrieve Proxy object for the supplied session
					if (mRemoteObjectList.containsKey(sname)){
						// get the proxy object for this session
						proxy = mRemoteObjectList.get(sname);

						if (proxy != null){
							Log.d(TAG, "Retrieving profile for: "+userid);
							// Retrieve the ProfileDescriptor from the remote device
							String pstring = proxy.getInterface(GroupsInterface.class).GetMemberProfile(group, userid);
							Log.d(TAG, "Saving profile to cache for: "+userid);
							ProfileDescriptor pd = new ProfileDescriptor();
							pd.setJSONString(pstring);
							if (!pd.isEmpty()){
								ProfileCache.saveProfile(userid, pd);
							} else {
								Log.w(TAG, "Empty profile, not saving");
							}
						} else {
							Log.e(TAG, "doGetMemberProfile() Null ProxyObject for: "+sname);
						}
					} else {
						Log.e(TAG, "doGetMemberProfile() No ProxyObject for: "+sname);
					}
				} else {
					Log.d(TAG, "Profile already stored, ignoring");
				}
			} catch (Exception e){
				Log.e(TAG, "doGetMemberProfile("+sname+") error: "+e.toString());
			}

		}


		// Handler function to setup a group=specific session with a remote device
		private void doSetupGroupSession (String session, String group, String userid){
			Log.d(TAG, "doSetupGroupSession("+session+", "+group+", "+userid+")");

			ProxyBusObject proxy = null;
			String sname = getSessionName(session); // ensure format is correct

			try {
				// check to see if we already have the session set up
				String id = getUserid(userid);
				if (!id.equals(mMyId)){
					if(!GroupSessionManager.isSessionActive(group, userid)){
						// Retrieve Proxy object for the supplied session
						if (mRemoteObjectList.containsKey(sname)){
							// get the proxy object for this session
							proxy = mRemoteObjectList.get(sname);

							if (proxy != null){
								Log.d(TAG, "Retrieving port for: "+userid);
								// Retrieve the port number for this group from the remote device
								short port = 1;
								try{
									port = proxy.getInterface(GroupsInterface.class).GetPortNumber(group);
								}catch(Exception e) {
									e.printStackTrace();
								}
								Log.d(TAG, "Joining session for group: "+group+" on port: "+port);
								GroupSessionManager.joinSession(group, userid, port);
							} else {
								Log.e(TAG, "doSetupGroupSession() Null ProxyObject for: "+sname);
							}
						} else {
							Log.e(TAG, "doSetupGroupSession() No ProxyObject for: "+sname);
						}
					} else {
						Log.d(TAG, "Session already established, ignoring");
					}
				} else{
					Log.i(TAG, "doSetupGroupSession() ignoring my own group");
				}
			} catch (Exception e){
				Log.e(TAG, "doSetupGroupSession("+sname+") Exception: "+e.toString());
				e.printStackTrace();
			}

		}


		// Handler function to request addition of users to a remote group
		private void doRequestNewMembers (String session, String group, String[] members){
			Log.d(TAG, "doRequestNewMembers("+session+", "+group+", ["+members+"])");

			ProxyBusObject proxy = null;
			String sname = getSessionName(session); // ensure format is correct

			try {
				// Retrieve Proxy object for the supplied session
				if (mRemoteObjectList.containsKey(sname)){
					// get the proxy object for this session
					proxy = mRemoteObjectList.get(sname);

					if (proxy != null){

					} else {
						Log.e(TAG, "doRequestNewMembers() Null ProxyObject for: "+sname);
					}
				} else {
					Log.e(TAG, "doRequestNewMembers() No ProxyObject for: "+sname);
				}
			} catch (Exception e){
				Log.e(TAG, "doRequestNewMembers("+sname+") error: "+e.toString());
			}

		}

		// Handler function to clean up after a user leaves
		private void doProcessUserLeft(String userid){
			String session = getSessionName(userid);
			mRemoteObjectList.remove(session);
		}

		// Handler to initiate the Groups self-test
		private void doRunGroupsTest(){
			try{
				if (mGroupsTest==null) mGroupsTest = new GroupsTestImpl(mAlljoynContainer);
				mGroupsTest.runGroupsTest();
			} catch (Exception e){
				Log.e(TAG, "doRunGroupsTest() Exception: "+e.toString());
			}
		}

	} // ImplHandler

	// connect to the Main session offered by a remote Groups session
	private void connectToService(String session) {
		SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
		Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
		mAlljoynContainer.getSessionManager().joinSession(getServiceName(session), GroupsConstants.SESSION_PORT, sessionId, sessionOpts, new SessionListener());
	}

	// disconnect from the Main service offered by a remote Groups session
	private void disconnectFromService(String session) {
		mAlljoynContainer.getSessionManager().leaveSession(getServiceName(session));
	}

	// Utility to build a Groups service name from the unique userid
	private String getServiceName(String userid){
		String svc = userid;
		if (svc.contains(".")) svc = svc.substring(svc.lastIndexOf(".")+1);
		svc = GroupsConstants.NAME_PREFIX + "." + svc;
		return svc;
	}

	// Utility to build a Groups session name from the unique userid
	private String getSessionName(String userid){
		String svc = userid;
		if (svc.contains(".")) svc = svc.substring(svc.lastIndexOf(".")+1);
		svc = GroupsConstants.SERVICE_NAME + "." + svc;
		return svc;
	}

	// Utility to extract the unique ID from a service/session/userid string
	private String getUserid(String userid){
		String id = userid;
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
		return id;
	}

	// Callbacks for group sessions
	private GroupSessionCallback mGroupSessionCallback = new GroupSessionCallback(){

		@Override
		public void onMemberJoined(String group, String member) {

			try {
				// just in case, check that this user has been processed
				//processNewUser(member);
				GroupsAPIImpl.getInstance().getCallbackInterface().onGroupMemberJoined(group, member);
			} catch (BusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(TAG, "onMemberJoined("+group+", "+member+")");		
		}

		@Override
		public void onMemberLeft(String group, String member) {
			try {
				GroupsAPIImpl.getInstance().getCallbackInterface().onGroupMemberLeft(group, member);
			} catch (BusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(TAG, "onMemberLeft("+group+", "+member+")");		
		}

		@Override
		public void onSessionEnded(String group, String member) {
			//GroupsAPIImpl.callback.onGroupInactive(group);
			Log.d(TAG, "onSessionEnded("+group+", "+member+")");		
		}

	};
}
