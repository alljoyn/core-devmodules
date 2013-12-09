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

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.groups.api.GroupsAPIImpl;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

// Class to manage testing of Group Sessions
// This is really just to test that the group session can be successfully used by another BusObject
public class GroupsTestImpl extends BusListener{

	private final static String TAG = "GroupsTestImpl";

	// class to hold the data for active group tests
	private class PingData{
		String              group;
		boolean             result;
		ArrayList<String>   responders;
		GroupsTestInterface testInterface;

		PingData(){
			group = "";
			result = false;
			responders = new ArrayList<String>();
			testInterface = null;
		}
	}

	private TestHandler mHandler = new TestHandler(); // mHandler for executing test state machine

	private static HashMap<String,PingData> mTestData;
	private static GroupsTestObject         mTestObject = null;
	private static GroupsTestInterface      mTestInterface = null;

	private AllJoynContainerInterface    mAlljoynContainer = null;
	private String                       mResultString="";
	private String                       mMyId="";
	private final static long            TEST_TIME = 20000; // time allowed for test (msecs)
	private static GroupsTestImpl         mListener;

	/**
	 * Constructor. Note that AllJoyn setup must already be done before calling
	 * @param container An instance of AllJoynContainerInterface that provides access to AllJoyn functions/instances
	 */
	public GroupsTestImpl(AllJoynContainerInterface container){	
		try{
			Status status;

			mAlljoynContainer = container;
			mMyId = mAlljoynContainer.getUniqueID();

			mListener = this;  // for use in inner class


			// register bus listener (is this needed?)
			mAlljoynContainer.getBusAttachment().registerBusListener(this);

			// Set up test infrastructure, in case someone starts testing against this user
			init();

		} catch (Exception e){
			Log.e(TAG, "GroupsImpl() error: "+e.toString());
		}
	}

	// prevent use of default constructor
	private GroupsTestImpl(){
	}


	/* ************************************** */
	/* Module specific Signal Handlers        */
	/* ************************************** */
	@BusSignalHandler(iface=GroupsConstants.NAME_PREFIX + ".test", signal="PingRequest")
	public void PingRequest(String group, String userid, String pingString){
		try{
			Log.d(TAG, "PingRequest("+group+", "+userid+", "+pingString+"))");
			if (!userid.equals(mMyId)){
				handlePingRequest(group, userid, pingString);
			}
		} catch (Exception e){
			Log.d(TAG, "PingRequest("+group+", "+userid+", "+pingString+")) - Error: "+e.toString());
		}
	}

	@BusSignalHandler(iface=GroupsConstants.NAME_PREFIX + ".test", signal="PingResponse")
	public void PingResponse(String group, String userid, String pingString) {
		try{
			Log.d(TAG, "PingResponse("+group+", "+userid+", "+pingString+"))");
			if (!userid.equals(mMyId)){
				handlePingResponse(group, userid, pingString);
			}
		} catch (Exception e){
			Log.d(TAG, "PingResponse("+group+", "+userid+", "+pingString+")) - Error: "+e.toString());
		}
	}

	/* ************************************** */
	/* Module specific implementation methods */
	/* ************************************** */

	//Define constants for mHandler events
	private static final int INIT                 = 0; 
	private static final int START_TEST           = 1; 
	private static final int SHUTDOWN             = 2; 
	private static final int START_GROUP_TEST     = 3; 
	private static final int HANDLE_PING_REQUEST  = 4; 
	private static final int HANDLE_PING_RESPONSE = 5; 
	private static final int END_TEST             = 6; 
	private static final int SEND_RESULTS         = 7; 

	/* ************************************** */
	/* External (Accessor) functions          */
	/* ************************************** */

	public void runGroupsTest(){
		startTest();
	}

	private void init(){
		mHandler.sendEmptyMessage(INIT);
	}

	private void startTest(){
		mHandler.sendEmptyMessage(START_TEST);
	}

	private void shutdown(){
		mHandler.sendEmptyMessage(SHUTDOWN);
	}

	private void startGroupTest(String group){
		Message msg = mHandler.obtainMessage(START_GROUP_TEST);
		Bundle data = new Bundle();
		data.putString("group", group);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void handlePingRequest(String group, String userid, String pingString)
	{
		Message msg = mHandler.obtainMessage(HANDLE_PING_REQUEST);
		Bundle data = new Bundle();
		data.putString("group", group);
		data.putString("userid", userid);
		data.putString("pingstring", pingString);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	public void handlePingResponse(String group, String userid, String pingString)
	{
		Message msg = mHandler.obtainMessage(HANDLE_PING_RESPONSE);
		Bundle data = new Bundle();
		data.putString("group", group);
		data.putString("userid", userid);
		data.putString("pingstring", pingString);
		msg.setData(data);
		mHandler.sendMessage(msg);	
	}

	private void startTimer(){
		mHandler.sendEmptyMessageDelayed(END_TEST, TEST_TIME);
	}

	private void sendResults(){
		mHandler.sendEmptyMessage(SEND_RESULTS);
	}


	// Use this Handler for asynchronous transactions
	// Note that there may be several transactions active in parallel, so don't assume a single test
	private class TestHandler extends Handler
	{
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			// frequently-used variables
			String group;
			String userid;
			String pingString;

			switch(msg.what) {
			case INIT: {
				Log.d(TAG,"INIT");
				doInit();
				break;
			}
			case START_TEST: {
				Log.d(TAG,"START_TEST");
				doStartTest();
				break;
			}
			case SHUTDOWN: {
				Log.d(TAG,"SHUTDOWN");
				doShutdown();
				break;
			}

			case START_GROUP_TEST: {
				group = data.getString("group");
				Log.d(TAG,"START_GROUP_TEST: "+group);
				doStartGroupTest(group);
				break;
			}
			case HANDLE_PING_REQUEST: {
				group = data.getString("group");
				userid = data.getString("userid");
				pingString = data.getString("pingstring");
				Log.d(TAG,"HANDLE_PING_REQUEST: "+group+", "+userid+", "+pingString);
				doHandlePingRequest(group, userid, pingString);
				break;
			}
			case HANDLE_PING_RESPONSE: {
				group = data.getString("group");
				userid = data.getString("userid");
				pingString = data.getString("pingstring");
				Log.d(TAG,"HANDLE_PING_RESPONSE: "+group+", "+userid+", "+pingString);
				doHandlePingResponse(group, userid, pingString);
				break;
			}
			case END_TEST: {
				Log.d(TAG,"END_TEST");
				doEndTest();
				break;
			}
			case SEND_RESULTS: {
				Log.d(TAG,"SEND_RESULTS");
				doSendResults();
				break;
			}

			default:
				Log.e(TAG, "TestHandler unknown msg type: "+msg.what);
				break;

			} // switch
		} // handleMessage


		// Initialise such that another user can ping this user
		private void doInit(){
			try{
				GroupSessionManager.init();
				if (mTestData==null) mTestData = new HashMap<String,PingData>();

				// Crank up the debug level for testing only
				mAlljoynContainer.getBusAttachment().useOSLogging(true);
				//mAlljoynContainer.getBusAttachment().setDaemonDebug("ALLJOYN", 7);
				mAlljoynContainer.getBusAttachment().setDebugLevel("ALLJOYN", 7);
				mAlljoynContainer.getBusAttachment().setLogLevels("ALL=7");

				// set up the test object
				mTestObject = new GroupsTestObject();
				Status status = mAlljoynContainer.getBusAttachment().registerBusObject(mTestObject, "/groupsTest");
				if (status != Status.OK){
					Log.e(TAG, "doInit() registerBusObject() failed: "+status);
				} else {
					Log.d(TAG, mMyId+": Registered GroupsTestObject at /groupsTest");
				}

				// Set up a generic (non session-specific)
				SignalEmitter sigEm = new SignalEmitter(mTestObject, SignalEmitter.GlobalBroadcast.On);
				mTestInterface = sigEm.getInterface(GroupsTestInterface.class);

				status = mAlljoynContainer.getBusAttachment().registerSignalHandlers(mListener);
				if (status != Status.OK){
					Log.w(TAG, "Error registering signal handlers: "+status.toString());
				}

				// Group-specific setup is done elsewhere
			} catch (Exception e){
				Log.e(TAG, "doInit() exception: "+e.toString());
				e.printStackTrace();
			}
		}

		// Handler function to start testing all active groups
		private void doStartTest(){
			// Initialise variables, arrays etc.
			mResultString = "";
			if (mTestData==null) mTestData = new HashMap<String,PingData>();
			mTestData.clear();


			// Dump the list of sessions, for debug purposes
			GroupSessionManager.dumpIdMap();

			// Start the timer
			startTimer();

			// Start test on each group
			//String[] glist = GroupManager.getActiveGroupList();
			String[] glist = GroupSessionManager.listActiveGroups();
			Log.d(TAG, "Found "+glist.length+" hosted sessions");
			for (int i=0; i<glist.length;i++){
				// make sure someone is on the session
				if (GroupSessionManager.isSessionActive(glist[i])){
					startGroupTest(glist[i]);
				} else {
					Log.d(TAG, "No user on group: "+glist[i]);
				}
			}
		}

		// Handler function to do shutdown processing
		private void doShutdown(){
		}

		// handler to start testing a specified group
		private void doStartGroupTest(String group){
			try{

				// Set up the test entry
				int session;

				if (GroupManager.isActive(group)){
					session = GroupSessionManager.getSessionId(group);
					if (session==0){
						Log.w(TAG, "doStartGroupTest() Oops. Session==0 (MyId="+mMyId+")");
						GroupSessionManager.dumpIdMap();
					} else {
						if (mTestData.containsKey(group)){
							mTestData.remove(group);
						}
						PingData pdata = new PingData();
						pdata.responders = new ArrayList<String>();
						pdata.responders.clear();
						pdata.group = group;
						Log.v(TAG, "Sending PingRequest on session: "+session);
						SignalEmitter sigEm = new SignalEmitter(mTestObject, session, SignalEmitter.GlobalBroadcast.On);
						//SignalEmitter sigEm = new SignalEmitter(mTestObject, SignalEmitter.GlobalBroadcast.On);
						pdata.testInterface = sigEm.getInterface(GroupsTestInterface.class);

						mTestData.put(group, pdata);

						// OK, send the request
						if (pdata.testInterface != null){
							pdata.testInterface.PingRequest(group, mMyId, "PING!");
							GroupsAPIImpl.getInstance().getCallbackInterface().onTestResult("Testing group:"+group);


							// HACK HACK HACK - send it on the open session to see if it works
							//mTestInterface.PingRequest(group, mMyId, "PING!");

						} else {
							Log.e(TAG, "doStartGroupTest() Oops. Null signal interface!!!");
						}
					}
				} else {
					Log.w(TAG, "doStartGroupTest() No session set up for group: "+group);
				}

			} catch (Exception e){
				Log.e(TAG, "doStartGroupTest() Exception: "+e.toString());
				e.printStackTrace();
				Log.v(TAG, "MyID: "+mMyId);
				GroupSessionManager.dumpIdMap();
			}
		}


		// handler to respond to a ping request
		private void doHandlePingRequest(String group, String userid, String pingString){
			try{

				if (userid != mMyId){
					// Check that Session is active
					if (GroupSessionManager.isSessionActive(group, userid)){
						int session = GroupSessionManager.getSessionId(group, userid);
						// if entry not set up then do so 
						if (!mTestData.containsKey(group)){
							mTestData.put(group, new PingData());
							mTestData.get(group).group = group;
							SignalEmitter sigEm = new SignalEmitter(mTestObject, session, SignalEmitter.GlobalBroadcast.On);
							mTestData.get(group).testInterface = sigEm.getInterface(GroupsTestInterface.class);
						}

						// OK, send a response
						if (mTestData.get(group).testInterface != null){
							Log.d(TAG, "Sending PingResponse for group ("+group+") on session: "+session);
							mTestData.get(group).testInterface.PingResponse(group, mMyId, pingString);

							// HACK HACK HACK - send it on the open session to see if it works
							//mTestInterface.PingResponse(group, mMyId, pingString);
						} else {
							Log.e(TAG, "doHandlePingRequest() Oops. Null signal interface!!!");
						}

					} else {
						Log.e(TAG, "doHandlePingRequest() Signal received on inactive session !!!");
					}
				}
			} catch (Exception e){
				Log.e(TAG, "doHandlePingRequest() Exception: "+e.toString());
				e.printStackTrace();
			}
		}


		// handler for Ping Response
		private void doHandlePingResponse(String group, String userid, String pingString){
			try{
				// Check that Session is active
				if (GroupSessionManager.isSessionActive(group, userid)){
					// update test entry
					if (mTestData.containsKey(group)){
						if (!mTestData.get(group).responders.contains(userid)){
							mTestData.get(group).responders.add(userid);
							GroupsAPIImpl.getInstance().getCallbackInterface().onTestResult("Response from:"+userid);
						} else {
							Log.w(TAG, "Received duplicate response for group ("+group+") from user: "+userid);
						}
					} else {
						Log.e(TAG, "doHandlePingResponse() PingResponse received for inactive test");
					}
				} else {
					Log.e(TAG, "doHandlePingResponse() Signal received on inactive session !!!");
				}
			} catch (Exception e){
				Log.e(TAG, "doHandlePingResponse() Exception: "+e.toString());
				e.printStackTrace();
			}
		}


		private void doEndTest(){
			sendResults();
		}


		private void doSendResults(){
			try {

				int failCount = 0;
				mResultString = "<b>Groups Test Results:</b><br><br>";


				// Scan through the list of groups and build result table

				if (mTestData.isEmpty()){
					Log.w(TAG, "Oops, no test data recorded!");
				}

				for (String group: mTestData.keySet()){
					PingData data = mTestData.get(group);
					mResultString += "<font color=\"yellow\">Group:</font>&nbsp;&nbsp;" + group+"<br>";

					mResultString += "<font color=\"yellow\">Responders:</font><br>";
					for (int i=0;i<data.responders.size();i++){
						mResultString += data.responders.get(i)+"<br>";
					}

					// Check whether we got the expected responses
					String[] mlist = GroupSessionManager.getAllJoinedSessions(group);
					Log.d(TAG, "Group:"+group+" Expected:"+mlist.length+" Received:"+data.responders.size());
					if (mlist.length == data.responders.size()){
						data.result=true;
						Log.d(TAG, "Test for group "+group+" passed");
					} else {
						data.result=false;
						failCount++;
						Log.d(TAG, "Test for group "+group+" failed");
					}
					mResultString += "<font color=\"yellow\">Pass ?:</font>&nbsp;&nbsp;";
					mResultString += data.result+"<br><br>";
				}

				// If no data, jsut add an empty row (looks better)
				if (mTestData.isEmpty()){
					mResultString += "<br>(none)<br>"  ;
				}

				if (failCount>0){
					mResultString += "Result: <b><font color=\"red\">FAILED</font></b>";
				} else {
					if (mTestData.isEmpty()){
						mResultString += "Result: <b><font color=\"blue\">NO GROUP MEMBERS</font></b>";
					} else {
						mResultString += "Result: <b><font color=\"green\">PASSED</font></b>";
					}
				}


				Log.d(TAG, mResultString);

				// probably shouldn't call directly, but hey it's test code!
				if (GroupsAPIImpl.getInstance().getCallbackInterface() != null){
					Log.v(TAG, "sending onTestResult signal");
					GroupsAPIImpl.getInstance().getCallbackInterface().onTestResult(mResultString);
				} else {
					Log.w(TAG, "doSendResults(). Null GroupsAPIImpl");
				}
			} catch (BusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	} // TestHandler
}
