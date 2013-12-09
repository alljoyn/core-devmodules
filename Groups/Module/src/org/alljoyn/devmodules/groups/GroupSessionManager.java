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

import java.util.ArrayList;
import java.util.HashMap;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;
import org.alljoyn.devmodules.sessionmanager.SessionManager;
import android.util.Log;



public class GroupSessionManager {

	private static final String TAG = "GroupSessionManager";

	private static GroupSessionManager _groupSessionMgr=null; // the singleton version

	private static SessionManager mSessionMgr = null;
	private static String         mMyId = "";
	private static BusAttachment  mBus = null;

	// Class for holding data associated with a hosted session
	private class SessionData {
		String            host;
		String            group;
		short             port;
		int               id;
		ArrayList<String> members;
	}

	// Lists of data (key is 'session', not 'group')
	private static HashMap<String,SessionData> mHostedSessions=null;
	private static HashMap<String,SessionData> mJoinedSessions=null;

	// List of callbacks (common for all groups)
	private static ArrayList<GroupSessionCallback> mCallbackList=null;

	// Mapping of busId to service name
	private static HashMap<String,String> mBusIdMap = null;

	// Port/SessionId to Group name mapping (for hosted sessions)
	private static HashMap<Short,String> mPortToSessionMap=null;
	private static HashMap<Integer,String> mIdToSessionMap=null;

	/////////////////////////////////////////////////////
	// Singleton class management stuff
	/////////////////////////////////////////////////////

	private GroupSessionManager() { 
		// no implementation, just private to control usage
	} 


	// prevent instantiation via an explicit clone() call
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// method to return reference to internal data
	public static synchronized void init() {
		if (_groupSessionMgr == null) {

			// create singleton object
			_groupSessionMgr = new GroupSessionManager();

			// Initialise lists & variables
			mHostedSessions   = new HashMap<String,SessionData> ();
			mJoinedSessions   = new HashMap<String,SessionData> ();
			mCallbackList     = new ArrayList<GroupSessionCallback> ();
			mPortToSessionMap = new HashMap<Short,String>();
			mIdToSessionMap   = new HashMap<Integer,String>(); // TODO: change to SparseArray<String>
			mBusIdMap         = new HashMap<String,String>();

			mHostedSessions.clear();
			mJoinedSessions.clear();
			mCallbackList.clear();
			mPortToSessionMap.clear();
			mIdToSessionMap.clear();
			mBusIdMap.clear();

			// Initialise the session port pool
			createSessionPortPool();
		}
	}

	/////////////////////////////////////////////////////
	// Utilities
	/////////////////////////////////////////////////////


	// Utilities for formating strings based on groups conventions

	// Extract "ID" portion of typical service/session name
	private static String getMemberID (String service){
		String id = service;
		if (id.contains(".")) id = id.substring(id.lastIndexOf(".")+1);
		return id;
	}

	// Extract "generic" portion of typical bus ID, 
	// e.g. if busId is :wUbSFoQ1.13 then the extracted id is wUbsFoQ1

	private static String getBusID (String fullBusId){
		String id = fullBusId;
		if(id != null) {
			int start = (fullBusId.contains(":")) ? (id.indexOf(":")+1) : 0;
			int end   = (fullBusId.contains(".")) ? (id.indexOf(".")) : fullBusId.length();
			id = id.substring(start, end);
		}
		return id;
	}


	// Extract the group name from the service/session name
	private static String getGroupName(String session){
		String gname = "";
		gname = session;
		String marker = GroupsConstants.SERVICE_NAME+".";
		if (gname.contains(marker)){
			gname = gname.substring(gname.indexOf(marker)+marker.length(), gname.lastIndexOf("."));
		}
		return gname;
	}

	// Utility to create the (hosted) session name from the group
	private static String getSessionName(String group){
		return getSessionName(group, mMyId);
	}

	// Utility to create the session name from the group and member ID
	private static String getSessionName(String group, String member){
		// empty group is a special case (don't add the extra ".")
		String sname = "";
		if ((group==null) || (group.length()==0)) {
			sname = GroupsConstants.SERVICE_NAME + "." + getMemberID(member);
		} else {
			sname = GroupsConstants.SERVICE_NAME + "." + getGroupName(group) + "." + getMemberID(member);
		}
		return sname;
	}

	// Utilities to manage session port numbers
	// TODO: replace with dynamic allocation of ports in create call

	private static ArrayList<Short> mPortPool;
	private static void createSessionPortPool(){
		// allocate port numbers (32 is arbitrary)
		mPortPool = new ArrayList<Short>();
		mPortPool.clear();
		short port ;
		for (short i=0; i<32; i++){
			port = (short) (GroupsConstants.SESSION_PORT + i + 2);
			mPortPool.add(port);
		}
	}

	private static short allocateSessionPort(){
		short port = -1;
		if (!mPortPool.isEmpty()){
			port = mPortPool.remove(mPortPool.size()-1);
		}
		return port;
	}

	private static void returnSessionPort(short port){
		if (!mPortPool.contains(port)){
			mPortPool.add(port);
		}
	}


	public static void dumpIdMap(){
		Log.v(TAG, "-------------------------------------");
		Log.v(TAG, "Dump of Session ID->Session Name Map for user "+mMyId+":");
		if (mIdToSessionMap.isEmpty()){
			Log.v(TAG, "Empty map!");
		} else {
			for (int id: mIdToSessionMap.keySet()){
				Log.v(TAG, "    ID:"+id+"    Session:"+mIdToSessionMap.get(id));
			}
			Log.v(TAG, "-------------------------------------");
		}
	}

	public static void dumpHostedSessions(){
		Log.v(TAG, "-------------------------------------");
		Log.v(TAG, "Dump of Hosted Sessions Map:");
		if (mHostedSessions.isEmpty()){
			Log.v(TAG, "Empty map!");
		} else {
			SessionData sd;
			for (String s: mHostedSessions.keySet()){
				sd = mHostedSessions.get(s);
				Log.v(TAG, "    ["+s+"]    Group:"+sd.group +
						" Host:" + sd.host + " port:" + sd.port + " id:" + sd.id);
			}
			Log.v(TAG, "-------------------------------------");
		}
	}

	public static void dumpJoinedSessions(){
		Log.v(TAG, "-------------------------------------");
		Log.v(TAG, "Dump of Joined Sessions Map:");
		if (mJoinedSessions.isEmpty()){
			Log.v(TAG, "Empty map!");
		} else {
			SessionData sd;
			for (String s: mJoinedSessions.keySet()){
				sd = mJoinedSessions.get(s);
				Log.v(TAG, "    ["+s+"]    Group:"+sd.group +
						" Host:" + sd.host + " port:" + sd.port + " id:" + sd.id);
			}
			Log.v(TAG, "-------------------------------------");
		}
	}

	/////////////////////////////////////////////////////
	// Track Services coming and going to be able to find busId
	/////////////////////////////////////////////////////

	private static BusListener mBusListener = new BusListener() {
		public void nameOwnerChanged(String busName, String prevOwner, String newOwner) {
//			mBus.enableConcurrentCallbacks();

			// If this is a services name then map IDs
			// busName  is a Service name, but it could be any of the services, so just extract
			//          the ID portion
			// newOwner is the AllJoynID of the bus attachment that owns the name
			//          If empty, then name has been removed
			if (busName.startsWith("org.alljoyn.devmodules")){

				String svcId ;
				String busId ;

				if (mBusIdMap==null){
					mBusIdMap = new HashMap<String,String>();
					mBusIdMap.clear();
				}


				if((newOwner == null)||(newOwner.length()==0)) {
					// only remove if this the actual groups service, not any others
					if (busName.contains("."+GroupsConstants.SERVICE_NAME)){
						// extract the bus attachment piece of the ID, between ":" and "."
						busId = getBusID(prevOwner);
						if (mBusIdMap.containsKey(busId)){
							Log.d(TAG,"NameOwnerChanged("+busName+", "+prevOwner+", "+newOwner);
							mBusIdMap.remove(busId);
							Log.i(TAG,"REMOVE BusId: "+busId);
						}
					}
				}
				else {
					// extract the userID portion of the service name
					svcId = getMemberID(busName);
					// extract the bus attachment piece of the ID, between ":" and "."
					busId = getBusID(newOwner);
					if (!mBusIdMap.containsKey(busId)){
						Log.d(TAG,"NameOwnerChanged("+busName+", "+prevOwner+", "+newOwner);
						Log.i(TAG,"ADD BusId: "+busId+" => "+svcId);
						mBusIdMap.put(busId, svcId);
					}
				}
			}
		}
	};

	/**
	 *  returns the MemberID associated with a BusId
	 * @param busId The AllJoyn bus identifier (unique name)
	 * @return the memberID string
	 */
	public static String getMemberFromBusId(String busId){
		String member = "";
		String id = getBusID(busId);
		if (mBusIdMap.containsKey(id)){
			member = mBusIdMap.get(id);
		}

		return member;
	}

	/////////////////////////////////////////////////////
	// the actual Group Management functionality
	// Note: if you see just a 'group' parameter then that typically means the session that this device
	//       is hosting. If you both 'group' and 'member' then it refers to a session hosted on another device
	//       (by member 'member')
	/////////////////////////////////////////////////////

	/**
	 *  Provides a reference to the SessionManager object to use for managing the AllJoyn sessions
	 * @param id id The ID for this service
	 */
	public static void setMyId(String id){
		mMyId = getMemberID(id);
	}

	/**
	 *  Provides the session manager being used. 
	 * @param sm The SessionManager object to use for managing the underlying sessions
	 */
	public static void setSessionManager(SessionManager sm){
		mSessionMgr = sm;
		mBus = mSessionMgr.getBusAttachment();
		mBus.registerBusListener(mBusListener);
	}

	/**
	 *  returns the SessionManagr object in use. Might be needed by something other than the 'owner' of the sessionmanager
	 * @return The SessionManager object to used for managing the underlying sessions
	 */
	public static SessionManager getSessionManager(){
		return mSessionMgr;
	}

	//////////////////////////////////
	// Management of hosted sessions
	//////////////////////////////////

	/**
	 *  The sessionPortListener for hosted sessions. Updates internal records, activates callbacks etc.
	 */
	private static SessionPortListener mHostedSessionPortListener = new SessionPortListener(){

		// access approval callback. Checks port number and membership
		public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
			//mBus.enableConcurrentCallbacks();
			Log.d(TAG, "(Hosted) acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
			boolean result = true;
			short port = 0;
			String busId =  getBusID(joiner);

			// valid group?
			if (mPortToSessionMap.containsKey(sessionPort)){

				if (mBusIdMap.containsKey(busId)){
					String service = mBusIdMap.get(busId);

					String session = mPortToSessionMap.get(sessionPort);
					String group = mHostedSessions.get(session).group;
					String member = getMemberID(service);
					// check that group is open, or joiner is a member of the group
					if ((!GroupManager.isPrivate(group))||(GroupManager.isMember(group, member))){
						result =  true;
					} else {
						Log.w(TAG, "acceptSessionJoiner() Session request rejected. "+member+" not member of group ("+group+")");
						Log.d(TAG, "Dumping group ["+group+"]:");
						Log.d(TAG, GroupManager.getGroupDescriptor(group).toString());
						Log.w(TAG, "acceptSessionJoiner() HACK: accepting for now");
					}
				} else {
					Log.w(TAG, "acceptSessionJoiner() joiner not found: "+busId);
					Log.w(TAG, "acceptSessionJoiner() HACK: accepting for now");
				}
			} else {
				Log.w(TAG, "acceptSessionJoiner() Group not found for port: "+port);
			}
			return result;
		}

		// session joined callback. Only called for first joiner
		public void sessionJoined(short sessionPort, int id, String joiner) {
			try{
				//mBus.enableConcurrentCallbacks();
				Log.d(TAG, "(Hosted) sessionJoined(" + sessionPort+", "+id + ", " + joiner+")");
				// Record this user as active
				String session;
				String group;
				String member;
				String service;
				String busId;

				if (mPortToSessionMap.containsKey(sessionPort)){
					session = mPortToSessionMap.get(sessionPort);
					if (mHostedSessions.containsKey(session)){

						//if (mBusIdMap.containsKey(busId)){
						// strangeness introduced in 3.2, might return unique ID or service name
						if (joiner.contains(":")){
							busId =  getBusID(joiner); // changed to service name in 3.2 ?
							service = mBusIdMap.get(busId);							
						} else{
							service = joiner;
						}

						// update the member list
						member = getMemberID(service);
						group = mHostedSessions.get(session).group;
						if (!mHostedSessions.get(session).members.contains(mMyId)) 
							mHostedSessions.get(session).members.add(mMyId);
						mHostedSessions.get(session).members.add(member);
						mHostedSessions.get(session).id = id;

						// store the ID to Group mapping
						if (!mIdToSessionMap.containsKey(id))
							mIdToSessionMap.put(id,  session);

						/** execute callback from sessionMemberAdded callback instead
						 *  (why the duplication????)
						 */
						// execute callbacks
						for (int i=0; i<mCallbackList.size(); i++){
							mCallbackList.get(i).onMemberJoined(group, member);
						}

						// This callback is only executed for the first joiner
						// register the Session Listener for subsequent members (why is it this way?!)
						mSessionMgr.getBusAttachment().setSessionListener(id, mHostedSessionListener);

						//} else {
						//	Log.w(TAG, "sessionJoined() joiner not found: "+busId);
						//}
					} else {
						Log.e(TAG, "sessionJoined() Oops! session not hosted: ["+session+"]");
						dumpHostedSessions();
					}
				}
			} catch (Exception e){
				Log.e(TAG, "sessionJoined() error: "+e.toString());
			}
		}
	};//SessionPortListener


	// SessionListener implementation for monitoring established sessions
	private static SessionListener mHostedSessionListener = new SessionListener(){

		// called when a session is lost
		public void sessionLost(int sessionId) {
			try{
				mBus.enableConcurrentCallbacks();
				Log.i(TAG, "(Hosted) sessionLost(" + sessionId + ")");

				// clean up all entries for this session
				String session;
				String group;
				String member;

				// Get group from ID and check it's hosted
				if (mIdToSessionMap.containsKey(sessionId)){
					session = mIdToSessionMap.get(sessionId);
					if (mHostedSessions.containsKey(session)){
						short port = mHostedSessions.get(session).port;
						int id = mHostedSessions.get(session).id;
						group = mHostedSessions.get(session).group;
						member = mHostedSessions.get(session).host;

						// execute callbacks
						for (int i=0; i<mCallbackList.size(); i++){
							mCallbackList.get(i).onSessionEnded(group, member);
						}

						// Clean up
						returnSessionPort(port);
						mHostedSessions.remove(session);
						if (mPortToSessionMap.containsKey(port)) mPortToSessionMap.remove(port);
						if (mIdToSessionMap.containsKey(id))     mIdToSessionMap.remove(id);

					} else {
						Log.e(TAG, "sessionLost() Oops! group not hosted: "+session);
					}
				}

			} catch (Exception e){
				Log.e(TAG, "sessionLost() error: "+e.toString());
			}
		}

		// called when someone joins an already-established session
		public void sessionMemberAdded(int sessionId, String joiner) {
			try{
				mBus.enableConcurrentCallbacks();
				Log.i(TAG, "(Hosted) sessionMemberAdded(" + sessionId + ", " + joiner + ")");

				String session;
				String group;
				String member;
				String busId =  getBusID(joiner);
				if (mIdToSessionMap.containsKey(sessionId)){
					session = mIdToSessionMap.get(sessionId);
					if (mHostedSessions.containsKey(session)){
						if (mBusIdMap.containsKey(busId)){
							String service = mBusIdMap.get(busId);
							// update the member list
							group = mHostedSessions.get(session).group;
							member = getMemberID(service);
							mHostedSessions.get(session).members.add(member);

							// execute callbacks
							for (int i=0; i<mCallbackList.size(); i++){
								mCallbackList.get(i).onMemberJoined(group, member);
							}
						} else {
							Log.w(TAG, "sessionMemberAdded() joiner not found: "+busId);
						}
					} else {
						Log.e(TAG, "sessionMemberAdded() Oops! group not hosted: "+session);
						dumpHostedSessions();
					}
				}
			} catch (Exception e){
				Log.e(TAG, "sessionMemberAdded() error: "+e.toString());
			}
		}

		// Called when someone leaves a hosted session
		public void sessionMemberRemoved(int sessionId, String joiner) {
			try{
				mBus.enableConcurrentCallbacks();
				Log.i(TAG, "(Hosted) sessionMemberRemoved(" + sessionId + ", " + joiner + ")");

				// Remove the participant from the list of participants for the session

				String session;
				String group;
				String member;
				String busId =  getBusID(joiner);

				// Get group from ID and check it's hosted
				if (mIdToSessionMap.containsKey(sessionId)){
					session = mIdToSessionMap.get(sessionId);
					if (mHostedSessions.containsKey(session)){
						if (mBusIdMap.containsKey(busId)){
							String service = mBusIdMap.get(busId);
							// update the member list
							group = mHostedSessions.get(session).group;
							member = getMemberID(service);
							mHostedSessions.get(session).members.remove(member);

							// execute callbacks
							for (int i=0; i<mCallbackList.size(); i++){
								mCallbackList.get(i).onMemberLeft(group, member);
							}
						} else {
							Log.w(TAG, "joiner not found: "+busId);
						}
					} else {
						Log.e(TAG, "Oops! group not hosted: "+session);
					}
				}
			} catch (Exception e){
				Log.e(TAG, "sessionMemberRemoved error: "+e.toString());
			}
		}
	}; // mHostedSessionListener


	/**
	 *  starts a hosted session for the supplied group
	 * @param group The name of the group
	 * @return true if successful, otherwise false
	 */
	public static synchronized boolean startSession(String group){
		boolean result = false;
		short port=0;
		try{
			//mBus.enableConcurrentCallbacks();
			init();
			String session = getSessionName(group);
			if (!mHostedSessions.containsKey(session)){
				if ((group==null) || (group.length()==0)){
					port = GroupsConstants.SESSION_PORT;
				} else {
					port = allocateSessionPort();
				}
				//TODO: deal with exhaustion of ports

				mPortToSessionMap.put(port, session);
				SessionData sd = _groupSessionMgr.new SessionData();
				sd.host = mMyId;
				sd.group = group;
				sd.port = port;
				sd.id   = 0;
				sd.members = new ArrayList<String>();
				sd.members.clear();
				mHostedSessions.put(session, sd);

				Log.v(TAG, "Creating Session: "+session+", port: "+port);

				Status status = mSessionMgr.createSession(session, port, mHostedSessionPortListener);
				if (status==Status.OK){
					result = true;
					Log.d(TAG, "Saved hosted session info. session:"+session+" host:"+sd.host+" group:"+sd.group+
							" port:"+sd.port+" id:"+sd.id);
				} else {
					Log.e(TAG, "Error creating session ("+session+"): "+status.toString());
					mHostedSessions.remove(session);
				}
			} else {
				Log.w(TAG, "Session already active for group: "+group);
			}
		} catch (Exception e){
			Log.e(TAG, "Error starting session:"+e.toString());
			if ((group!=null) && (group.length()!=0)){
				returnSessionPort(port);
			} else {
				port = GroupsConstants.SESSION_PORT;
			}
			if (mPortToSessionMap.containsKey(port)) mPortToSessionMap.remove(port);
		}
		return result;
	}

	/**
	 *  Stops an active hosted session
	 * @param group The name of the group
	 * @return true if successful, otherwise false
	 */
	public static boolean stopSession(String group){
		boolean result = false;
		try{
			//mBus.enableConcurrentCallbacks();
			init();
			String session = getSessionName(group);

			// take down the hosted session
			if (mHostedSessions.containsKey(session)){

				mSessionMgr.destroySession(session);
				short port = mHostedSessions.get(session).port;
				int id = mHostedSessions.get(session).id;
				returnSessionPort(port);
				mHostedSessions.remove(session);
				if (mPortToSessionMap.containsKey(port)) mPortToSessionMap.remove(port);
				if (mIdToSessionMap.containsKey(id))     mIdToSessionMap.remove(id);
				result = true;
				Log.d(TAG, "stopSession("+session+")");

				// take down the remote sessions, if any
				String[] glist = getAllJoinedSessions(group);
				for (String s: glist){
					leaveSession(group, getMemberID(s));
				}
			} else {
				Log.w(TAG, "Attempt to stop non-existent group (ignored): "+session);
			}
		} catch (Exception e){
			Log.e(TAG, "Error stopping session:"+e.toString());
		}
		return result;
	}

	/**
	 *  Returns the port number used for a (hosted) group session
	 * @param group The name of the group
	 * @return the port number for the session
	 */
	public static short getSessionPort(String group){
		//mBus.enableConcurrentCallbacks();
		init();
		String session = getSessionName(group);
		if (mHostedSessions.containsKey(session)){
			return mHostedSessions.get(session).port;
		}else {
			Log.d(TAG, "getSessionPort() No entry for group("+group+")");
			return -1;
		}
	}

	/**
	 *  Returns the session ID used for a (hosted) group session.  Use this for sending signals and registering BusObjects
	 * @param group The name of the group
	 * @return The ID of the Session. 0 if not active
	 */
	public static int getSessionId(String group){
		int id=0;
		init();
		String session = getSessionName(group);
		if (mHostedSessions.containsKey(session)){
			id = mHostedSessions.get(session).id;
			Log.v(TAG, "SessionID("+session+")="+id);
		}
		return id;
	}

	///////////////////////////////////////////////////
	// Management of joined (remotely hosted) sessions
	///////////////////////////////////////////////////

	// SessionListener implementation for monitoring joined sessions
	private static SessionListener mJoinedSessionListener = new SessionListener(){

		// called when a session is lost
		public void sessionLost(int sessionId) {
			try{
				mBus.enableConcurrentCallbacks();
				Log.i(TAG, "(Joined) sessionLost(" + sessionId + ")");

				// clean up all entries for this session
				String session;
				String group;
				String member;

				// Get group from ID and check it's joined
				if (mIdToSessionMap.containsKey(sessionId)){
					session = mIdToSessionMap.get(sessionId);
					if (mJoinedSessions.containsKey(session)){
						short port = mJoinedSessions.get(session).port;
						int id = mJoinedSessions.get(session).id;
						group = mJoinedSessions.get(session).group;
						member = mJoinedSessions.get(session).host;

						// execute callbacks
						for (int i=0; i<mCallbackList.size(); i++){
							mCallbackList.get(i).onSessionEnded(group, member);
						}

						// Clean up
						returnSessionPort(port);
						mJoinedSessions.remove(session);
						if (mPortToSessionMap.containsKey(port)) mPortToSessionMap.remove(port);
						if (mIdToSessionMap.containsKey(id))     mIdToSessionMap.remove(id);

					} else {
						Log.e(TAG, "Oops! Joined session not active: ["+session+"] (ID="+sessionId+")");
						dumpJoinedSessions();
					}
				} else {
					Log.w(TAG, "SessionID not found: "+sessionId);
					dumpIdMap();
				}

			} catch (Exception e){
				Log.e(TAG, "sessionLost error: "+e.toString());
			}
		}

		// called when someone joins an already-established session
		public void sessionMemberAdded(int sessionId, String joiner) {
			try{
				mBus.enableConcurrentCallbacks();
				Log.i(TAG, "(Joined) sessionMemberAdded(" + sessionId + ", " + joiner + ")");

				String session;
				String group;
				String member;
				String busId =  getBusID(joiner);

				if (mIdToSessionMap.containsKey(sessionId)){
					session = mIdToSessionMap.get(sessionId);
					if (mJoinedSessions.containsKey(session)){
						if (mBusIdMap.containsKey(busId)){
							String service = mBusIdMap.get(busId);
							// update the member list
							group = mJoinedSessions.get(session).group;
							member = getMemberID(service);
							mJoinedSessions.get(session).members.add(member);

							// execute callbacks
							for (int i=0; i<mCallbackList.size(); i++){
								mCallbackList.get(i).onMemberJoined(group, member);
							}
						} else {
							Log.w(TAG, "joiner not found: "+busId);
						}
					} else {
						Log.w(TAG, "Oops! Joined session not active: ["+session+"] (ID="+sessionId+")");
						dumpJoinedSessions();
					}
				} else {
					Log.w(TAG, "SessionID not found: "+sessionId);
					dumpIdMap();
				}
			} catch (Exception e){
				Log.e(TAG, "sessionMemberAdded error: "+e.toString());
			}
		}

		// Called when someone leaves a hosted session
		public void sessionMemberRemoved(int sessionId, String joiner) {
			try{
				mBus.enableConcurrentCallbacks();
				Log.i(TAG, "(Joined) sessionMemberRemoved(" + sessionId + ", " + joiner + ")");

				// Remove the participant from the list of participants for the session

				String session;
				String group;
				String member;
				String busId =  getBusID(joiner);

				// Get group from ID and check it's hosted
				if (mIdToSessionMap.containsKey(sessionId)){
					session = mIdToSessionMap.get(sessionId);
					if (mJoinedSessions.containsKey(session)){
						if (mBusIdMap.containsKey(busId)){
							String service = mBusIdMap.get(busId);
							// update the member list
							group = mJoinedSessions.get(session).group;
							member = getMemberID(service);
							mJoinedSessions.get(session).members.remove(member);

							// execute callbacks
							for (int i=0; i<mCallbackList.size(); i++){
								mCallbackList.get(i).onMemberLeft(group, member);
							}
						} else {
							Log.w(TAG, "joiner not found: "+busId);
						}
					} else {
						Log.e(TAG, "Oops! Joined session not active: ["+session+"](ID="+sessionId+")");
						dumpIdMap();
					}
				}
			} catch (Exception e){
				Log.e(TAG, "sessionMemberRemoved error: "+e.toString());
			}
		}

	}; // mJoinedSessionListener


	/**
	 *  Join a session hosted by another device, identified by 'member' (the unique ID or service name)
	 * @param group The name of the group
	 * @param member The ID of the user (the unique ID or service name)
	 * @param port The port used by the remote session
	 * @return true if successful, otherwise false
	 */
	public static synchronized boolean joinSession(String group, String member, short port){
		boolean result = false;
		try{
			//mBus.enableConcurrentCallbacks();
			init();
			String session = getSessionName(group, member);
			if (!mJoinedSessions.containsKey(session)){

				Log.v(TAG, "joinSession("+group+", "+member+", "+port+")");

				// check port. Errors happen, plus there are windows where the session may not be quite ready
				if (port>=0){
					SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
					Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
					Log.d(TAG, "Joining session: "+session);
					if (mJoinedSessionListener==null){
						Log.e(TAG, "Null SessionListener!");
					}
					Log.v(TAG, "=======================================================");

					// save session data. Need to do this *before* calling joinSession so that it is set up for the SessionMemberAdded callback
					SessionData sd = _groupSessionMgr.new SessionData();
					sd.host = member;
					sd.group = group;
					sd.port = port;
					sd.id   = sessionId.value;
					sd.members = new ArrayList<String>();
					sd.members.clear();
					sd.members.add(getMemberID(member));
					sd.members.add(getMemberID(mMyId));
					mJoinedSessions.put(session, sd);
					Status status = mSessionMgr.joinSession(session, port, sessionId, sessionOpts, mJoinedSessionListener);
					Log.d(TAG, "mSessionMgr.joinSession("+session+", "+port+",...): "+status.toString());

					if (status == Status.OK){
						Log.d(TAG, "Joined Session: "+session+" ID: "+sessionId.value);

						// save ID to Session mapping
						mJoinedSessions.get(session).id   = sessionId.value;
						mIdToSessionMap.put(sessionId.value, session);
						Log.d(TAG, "Saved joined session info. session:"+session+" host:"+sd.host+" group:"+sd.group+
								" port:"+sd.port+" id:"+sd.id);

						result = true;

					} else {
						Log.e(TAG, "joinSession failed with error: "+status.toString());
						mJoinedSessions.remove(session);
					}
				} else {
					Log.e(TAG, "joinSession() invalid port:"+port);
				}
			} else {
				Log.w(TAG, "Oops! session already joined: "+session);
			}
		} catch (Exception e){
			Log.e(TAG, "Error in joinSession:"+e.toString());
			e.printStackTrace();
		}

		// clean up if problems encountered
		if (!result){

		}
		return result;
	}

	/**
	 * Leave a session hosted by another device, identified by 'member' (the unique ID or service name)
	 * @param group The name of the group
	 * @param member The ID of the user (the unique ID or service name)
	 * @return true if successful, otherwise false
	 */
	public static synchronized boolean leaveSession(String group, String member){
		boolean result = false;
		String session = getSessionName(group, member);
		try{
			Log.v(TAG, "leaveSession("+group+", "+member+")");
			//mBus.enableConcurrentCallbacks();
			init();
			if (mJoinedSessions.containsKey(session)){
				Status status = mSessionMgr.leaveSession(session);
				if (status == Status.OK){
					int id = mJoinedSessions.get(session).id;
					mJoinedSessions.remove(session);
					if (mIdToSessionMap.containsKey(id))     mIdToSessionMap.remove(id);
					result = true;
				} else {
					Log.e(TAG, "leaveSession failed with error: "+status.toString());
				}
			} else {
				Log.w(TAG, "Attempt to leave inactive group (ignored): "+session);
			}
		} catch (Exception e){
			Log.e(TAG, "Exception leaving session ("+session+"):"+e.toString());
			e.printStackTrace();
		}
		return result;
	}


	/**
	 * Get the Session ID associated with a (joined) group. Use this for ProxyBusObjects
	 * @param group The name of the group
	 * @param member The ID of the user
	 * @return The ID of the Session. 0 if not active
	 */
	public static int getSessionId(String group, String member){
		init();
		String session = getSessionName(group, member);
		if (mJoinedSessions.containsKey(session)){
			return mJoinedSessions.get(session).id;
		}else {
			return 0;
		}
	}


	/**
	 * Get the list of all joined sessions associated with a group
	 * @param group The name of the group
	 * @return Array containing the list of session names
	 */
	public static String[] getAllJoinedSessions (String group){
		String[] slist;
		try{
			ArrayList<String> sarray = new ArrayList<String>();

			// scan through the joined session list
			for (String session: mJoinedSessions.keySet()){
				// check group name. Prepend "groups." in case group name is a substring of other groups
				if (session.contains("groups."+group)){
					// make sure session is actually set up
					if (mJoinedSessions.get(session).id != 0){
						sarray.add(session);
					}
				}
			}

			// copy to output array
			slist = sarray.toArray(new String[sarray.size()]);

		} catch (Exception e){
			Log.e(TAG, "getAllSessions("+group+") Exception: "+e.toString());
			slist = new String[0];
		}
		return slist;
	}

	/**
	 * Returns a list of all (hosted & joined) sessions
	 * @return the list of session names
	 */
	public static String[] listAllSessions(){
		init();
		int hsize = mHostedSessions.size();
		int jsize = mJoinedSessions.size();
		String[] list = new String [hsize+jsize];
		int i=0;
		SessionData sd;
		for (String k: mHostedSessions.keySet()){
			sd = mHostedSessions.get(k);
			list[i] = getSessionName(sd.group, sd.host);
			i++;
		}
		for (String k: mJoinedSessions.keySet()){
			sd = mHostedSessions.get(k);
			list[i] = getSessionName(sd.group, sd.host);
			i++;
		}
		return list;
	}

	/**
	 * Returns a list of all active (hosted & joined, with members) sessions
	 * @return the list of session names
	 */
	public static String[] listActiveSessions(){
		init();
		int asize = mIdToSessionMap.size();
		String[] list = new String [asize];
		int i=0;
		for (Integer id: mIdToSessionMap.keySet()){
			list[i] = mIdToSessionMap.get(id);
			i++;
		}
		return list;
	}

	/**
	 * Returns a list of all active (hosted & joined, with members) groups
	 * @return the list of group names (no prefix, no member id)
	 */
	public static String[] listActiveGroups(){
		init();
		ArrayList<String> activeList = new ArrayList<String>();
		activeList.clear();
		String group;
		for (Integer id: mIdToSessionMap.keySet()){
			group = getGroupName(mIdToSessionMap.get(id));
			if (!activeList.contains(group)){
				activeList.add(group);
			}
		}
		return activeList.toArray(new String[activeList.size()]);
	}


	/**
	 * returns a list of the currently hosted sessions
	 * @return the list of hosted session names
	 */
	public static String[] listHostedSessions(){
		init();
		int hsize = mHostedSessions.size();
		String[] list = new String [hsize];
		int i=0;
		SessionData sd;
		for (String k: mHostedSessions.keySet()){
			sd = mHostedSessions.get(k);
			list[i] = getSessionName(sd.group, sd.host);
			i++;
		}
		return list;
	}

	/**
	 * returns a list of the currently joined sessions
	 * @return the list of session names that have been joined
	 */
	public static String[] listJoinedSessions(){
		init();
		int jsize = mJoinedSessions.size();
		String[] list = new String [jsize];
		int i=0;
		SessionData sd;
		for (String k: mJoinedSessions.keySet()){
			sd = mHostedSessions.get(k);
			list[i] = getSessionName(sd.group, sd.host);
			i++;
		}
		return list;
	}


	/**
	 * returns a list of members for a hosted group
	 * @param group The name of the group
	 * @return the list of member IDs (Unique IDs) of apps/devices that have joined the group
	 */
	public static String[] listParticipants(String group){
		init();
		String session = getSessionName(group);
		int hsize ;
		String[] list;

		if (mHostedSessions.containsKey(session)){
			hsize = mHostedSessions.get(session).members.size();
			list = new String [hsize];
			for (int i=0; i<hsize; i++){
				list[i] = mHostedSessions.get(session).members.get(i);
			}
		} else {
			list = new String[0];
		}
		return list;
	}


	/**
	 * returns a list of members for a joined (remote) group
	 * @param group The name of the group
	 * @return the list of member IDs (Unique IDs) of apps/devices that have joined the group
	 */
	public static String[] listParticipants(String group, String member){
		init();
		String session = getSessionName(group, member);
		int hsize ;
		String[] list;

		if (mJoinedSessions.containsKey(session)){
			hsize = mJoinedSessions.get(session).members.size();
			list = new String [hsize];
			for (int i=0; i<hsize; i++){
				list[i] = mJoinedSessions.get(session).members.get(i);
			}
		} else {
			list = new String[0];
		}
		return list;
	}


	/**
	 * register a callback for notification group changes. Applies to all groups
	 * @param callback A callback interface for providing notifications back to the calling app
	 */
	public static void registerCallback(GroupSessionCallback callback){
		init();
		mCallbackList.add(callback);
	}

	/**
	 * Check whether a (remote) session is active
	 * @param group The name of the group
	 * @param member The ID of the user  (the unique ID or service name)
	 */
	public static boolean isSessionActive(String group, String member){
		init();
		boolean result = false;
		try{
			String session = getSessionName(group, member);
			if (mJoinedSessions.containsKey(session)){
				if (mJoinedSessions.get(session).id!=0)
					result = true;
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return result;
	}


	/**
	 * Check whether a (hosted) session is active
	 * Check to see whether a group is active, i.e. has participants
	 * @param group The name of the group
	 */
	public static boolean isSessionActive(String group){
		init();
		boolean result = false;
		String session = getSessionName(group);
		if (mHostedSessions.containsKey(session)){
			if (mHostedSessions.get(session).id!=0){
				result = true;
			} else {
				Log.v(TAG, "Session ID not set up yet");
			}
		} else {
			Log.v(TAG, "Session not known: "+session);
		}
		return result;
	}

} // GroupSessionManager
