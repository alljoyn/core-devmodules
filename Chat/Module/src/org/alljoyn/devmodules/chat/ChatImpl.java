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
package org.alljoyn.devmodules.chat;

import java.util.ArrayList;
import java.util.HashMap;
import org.alljoyn.bus.*;
import org.alljoyn.bus.annotation.BusSignalHandler;
import org.alljoyn.chat.api.ChatAPIImpl;
import org.alljoyn.devmodules.groups.GroupSessionManager;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.alljoyn.devmodules.interfaces.ModuleInterface;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ChatImpl extends BusListener implements ModuleInterface {
	private final String TAG = "ChatImpl";

	private static ChatImpl instance;
	public static ChatImpl getInstance() { return instance; }

	private ChatObject chatObject = new ChatObject();
	private AllJoynContainerInterface alljoynContainer;
	private String myWellknownName;

	private ArrayList<String> ChatPeerWellKnowns = new ArrayList<String>();
	private HashMap<String, Integer> roomsToSessionId = new HashMap<String,Integer>();
	private HashMap<Integer,String> sessionToRoomId = new HashMap<Integer,String>();
	private String namePrefix;

	private PostHandler handler = new PostHandler();
	private static final int NOTIFY_JOINED_CHAT = 0;
	private static final int SETUP_SESSION  = 1; 

	private short sessionPort = 1;

	private static boolean mSetup = false;

	public ChatImpl(AllJoynContainerInterface alljoynContainer) {
		this.alljoynContainer = alljoynContainer;
		instance = this;
	}

	public void RegisterSignalHandlers() {
		Status status = alljoynContainer.getBusAttachment().addMatch("sessionless='t'");
		alljoynContainer.getBusAttachment().registerSignalHandlers(this);
		alljoynContainer.getBusAttachment().addMatch("type='signal',interface='"+ChatConstants.NAME_PREFIX+"',member='chat'");
		alljoynContainer.getBusAttachment().addMatch("type='signal',interface='"+ChatConstants.NAME_PREFIX+"',member='groupChat'");
		alljoynContainer.getBusAttachment().addMatch("type='signal',interface='"+ChatConstants.NAME_PREFIX+"',member='chatWithAll'");
	}

	public void SetupSession() {
		if(!mSetup) {
			mSetup = true;
			alljoynContainer.getBusAttachment().registerBusListener(this);
			Log.i(TAG,"Registered signal handlers");
		}
	}

	public BusObject getBusObject() { return chatObject; }

	public String getObjectPath() {
		return ChatConstants.OBJECT_PATH;
	}

	public String getAdvertisedName() {
		if(myWellknownName == null) {
			myWellknownName = ChatConstants.SERVICE_NAME+"."+alljoynContainer.getUniqueID();
		}
		return myWellknownName;
	}

	public String getServiceName() {
		return ChatConstants.SERVICE_NAME;
	}

	public void foundAdvertisedName(String name, short transport, String namePrefix) {
		if(!name.contains(getAdvertisedName()) && name.startsWith(ChatConstants.NAME_PREFIX)){
			name = name.substring(namePrefix.length()+1);
			Log.i(TAG, "Chat FoundAdvName: "+name);
		}
	}

	public void lostAdvertisedName(String name, short transport, String namePrefix) {
		if(!name.contains(getAdvertisedName()) && name.startsWith(ChatConstants.NAME_PREFIX)){
			Log.i(TAG,"LostAdvertisedName: "+name);
			ChatPeerWellKnowns.remove(name);
			try { 
				JSONObject jsonData = new JSONObject();
				jsonData.put("room", name);
				//ModuleAPIManager.callbackInterface.ListenerJSON(getServiceName(), ChatConnectorObject.CHAT_ROOM_LOST, jsonData.toString());
				ChatAPIImpl.chatCallback.onChatRoomLost(name);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void shutdown() {

	}

	public ArrayList<String> getPeers() { return ChatPeerWellKnowns; }

	/* ************************************** */
	/* Module specific implementation methods */
	/* ************************************** */

	public void StartRoom(String room, String[] users) {
		SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
		final String tempRoom = room;
		final String advertisementName =getAdvertisedName()+"_p"+sessionPort;
		alljoynContainer.createSession(advertisementName, sessionPort, new SessionPortListener() {

			public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
				Log.d(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
				//TODO think of an efficient way to remember active ports and the joiner id that should be apart of the session
				//if (sessionPort == ChatConstants.SESSION_PORT)
				{
					return true;
				}
				//return false;
			}

			public void sessionJoined(short sessionPort, int id, String joiner) {
				Log.d(TAG, "User " + joiner + " joined., id=" + id);
				sessionToRoomId.put(new Integer(id), tempRoom);
				roomsToSessionId.put(tempRoom, new Integer(id));
			}

		}, sessionOpts);

		sessionPort++;
		Log.i(TAG,"Started room: "+getAdvertisedName()+"."+room);	
	}

	public void LeaveRoom(String room) {
		alljoynContainer.getSessionManager().leaveSession(room);
		sessionToRoomId.remove(roomsToSessionId.remove(room));
	}
	
	public void Send(String groupId, String msg) {
		//if(groupId != null && groupId.length() != 0) { // null or empty group now means the 'public' group
			//Send regular Signal on a Group
			//place call to get sessionId for group
			try {
				int sessionId = alljoynContainer.getGroupSessionId(groupId);
				Log.d(TAG, "Sending groupChat msg on session: "+sessionId);
				SignalEmitter em = new SignalEmitter(getBusObject(), sessionId, SignalEmitter.GlobalBroadcast.On);
				em.getInterface(ChatInterface.class).groupChat(groupId, msg);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//}
	}

	public void Send(String room, String msg, String user) {
		Log.d(this.getClass().getName(),"user: "+user);
		Log.d(this.getClass().getName(),"room: "+room);
		Log.d(this.getClass().getName(),"msg: "+msg);
		if((room == null)||(room.length()==0)) //This is a message for everyone, all chat signal, send sessionless
		{
			SignalEmitter sigEm = new SignalEmitter(getBusObject(), SignalEmitter.GlobalBroadcast.On);
			sigEm.setSessionlessFlag(true);
			ChatInterface mChatInterface = sigEm.getInterface(ChatInterface.class);
			try {
				mChatInterface.chatWithAll(msg);
			} catch (BusException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Integer sessionId = roomsToSessionId.get(room);
			Log.d(this.getClass().getName(),"sessionId: "+sessionId);
			for(String key:roomsToSessionId.keySet()) {
				Log.d(this.getClass().getName(),"Keys :"+key);
			}
			if(sessionId != null) {
				SignalEmitter sigEm = new SignalEmitter(getBusObject(), sessionId.intValue(), SignalEmitter.GlobalBroadcast.On);
				ChatInterface mChatInterface = sigEm.getInterface(ChatInterface.class);
				try {
					mChatInterface.chatWithAll(msg);
				} catch (BusException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	@BusSignalHandler(iface=ChatConstants.NAME_PREFIX, signal="groupChat")
	public void groupChat(String groupId, String msg) {
		try { 
			String chatUser="";
			alljoynContainer.getBusAttachment().enableConcurrentCallbacks();
			MessageContext ctx = alljoynContainer.getBusAttachment().getMessageContext();
			//ctx.sessionId so that I can correlate to the room and pass that back up to the listener
			if(ctx.sender.equals(alljoynContainer.getBusAttachment().getUniqueName()))
			{
				Log.i(TAG, "ignoring my own signal");
				return;
			}

			//chatUser= alljoynContainer.whoIsBusId(ctx.sender);
			// Temp fix: call GroupSessionManager to get user
			chatUser = GroupSessionManager.getMemberFromBusId(ctx.sender);
			
			//check determine what the room is by reverse looking up sessionId
			if (chatUser==null){
				Log.e(TAG, "Null user returned by getMemberFromBusId("+ctx.sender+")");
			}
			
			Log.i(TAG, "groupChat() group:"+groupId+", user:"+chatUser+", msg:"+msg);
			ChatAPIImpl.chatCallback.onGroupChatMsg(groupId, chatUser, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@BusSignalHandler(iface=ChatConstants.NAME_PREFIX, signal="chatWithAll")
	public void ChatWithAll(String msg) {
		try { 
			String chatRoom="";
			String chatUser="";
			alljoynContainer.getBusAttachment().enableConcurrentCallbacks();
			MessageContext ctx = alljoynContainer.getBusAttachment().getMessageContext();
			//ctx.sessionId so that I can correlate to the room and pass that back up to the listener
			if(ctx.sender.equals(alljoynContainer.getBusAttachment().getUniqueName()))
			{
				Log.i(TAG, "ignoring my own signal");
				return;
			}

			//chatUser= alljoynContainer.whoIsBusId(ctx.sender);
			// Temp fix: call GroupSessionManager to get user
			chatUser = GroupSessionManager.getMemberFromBusId(ctx.sender);
			
			//check determine what the room is by reverse looking up sessionId
			if (chatUser==null){
				Log.e(TAG, "Null user returned by getMemberFromBusId("+ctx.sender+")");
			}
			
			for(String temp:roomsToSessionId.keySet()) { //at this time do not expect large overhead with searching every message
				if(roomsToSessionId.get(temp).intValue() == ctx.sessionId) {	
					chatRoom = temp;
					break;
				}
			}
			Log.i(TAG, "ChatWithAll() room:"+chatRoom+", user:"+chatUser+", msg:"+msg);
			ChatAPIImpl.chatCallback.onChatMsg(chatRoom, chatUser, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void setupSession (){
		handler.sendEmptyMessage(SETUP_SESSION);	
	}

	private class PostHandler extends Handler
	{
		public void handleMessage(Message msg) {
			Bundle data = msg.getData();
			switch(msg.what) {
			case SETUP_SESSION: {
				SetupSession();
				break;
			}
			case NOTIFY_JOINED_CHAT:
				try { 
					SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
					Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
					String wellknown = data.getString("name");
					String UID = data.getString("UID");
					short sessionPort = (short) Integer.parseInt(wellknown.substring(wellknown.indexOf('_')+2,wellknown.lastIndexOf('.')));
					Log.d(this.getClass().getName(),"Going to join on port "+sessionPort);
					Log.d(this.getClass().getName(),"UID: "+UID);
					alljoynContainer.getSessionManager().joinSession(UID, sessionPort, sessionId, sessionOpts, new SessionListener());
					sessionToRoomId.put(new Integer(sessionId.value),data.getString("room"));
					roomsToSessionId.put(data.getString("room"),new Integer(sessionId.value));

					ArrayList<String> rawUsers = alljoynContainer.getParticipants(UID);
					//TODO: somewhat hardcoded cleanup
					String parsedUser = UID.substring(UID.indexOf(".")+1,UID.lastIndexOf("_"));
					rawUsers.add(parsedUser);
					ChatAPIImpl.chatCallback.onChatRoomFound(data.getString("room"), rawUsers.toArray(new String[rawUsers.size()]));

					Log.i(TAG, "Chat should have sent join session: "+wellknown+", port("+sessionPort+")");

				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
		}
	}

	@Override
	public void InitAPI(AllJoynContainerInterface coreLogic) {
		this.setupSession();
	}
}
