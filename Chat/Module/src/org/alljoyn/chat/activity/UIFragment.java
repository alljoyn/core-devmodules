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
package org.alljoyn.chat.activity;


import org.alljoyn.chat.api.ChatAPI;
import org.alljoyn.chat.api.ChatListener;
import org.alljoyn.devmodules.util.Utility;
import org.alljoyn.storage.ProfileCache;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class UIFragment extends Fragment {
	private final String TAG = "Chat_UIFragment";

	private String mGroupId = "";

	private ChatMessageAdapter mAdapter=null; 
	private EditText    vMessage;
	private Button      vSend;
	private ListView    vList;
	private TextView    vTitle;
	private View        vView;

	private boolean mDisplayed = false;

	private String mMyUserName;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public UIFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!mDisplayed){

			Bundle args = getArguments();

			if (args!=null){
				if (args.containsKey("group")) {
					mGroupId = args.getString("group");
					Log.v(TAG, "Chat UI active for group: "+mGroupId);
				}
			} else {
				Log.e(TAG, "No group specified!!!");
			}

			if(mAdapter == null)
				mAdapter = new ChatMessageAdapter(getActivity());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (!mDisplayed){
			mDisplayed = true;

			mMyUserName = ProfileCache.retrieveName();

			// Layout the UI
			vView = inflater.inflate(Utility.getResourseIdByName(getActivity().getPackageName(),"layout","chat_uifragment"), container, false);

			// Set the title
			vTitle = (TextView) vView.findViewById(Utility.getResourseIdByName(getActivity().getPackageName(),"id","title"));
			if ("".equals(mGroupId)){
				vTitle.setText("Open/Public Messages");
			} else {
				vTitle.setText("Group: "+mGroupId);
			}

			// Message list
			vList = (ListView) vView.findViewById(Utility.getResourseIdByName(getActivity().getPackageName(),"id","msglist"));
			vList.setAdapter(mAdapter); 

			// Send button
			vSend  = (Button) vView.findViewById(Utility.getResourseIdByName(getActivity().getPackageName(),"id","send"));
			vSend.setOnClickListener  (new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					String msg = vMessage.getText().toString();
					addMessage(mGroupId, mMyUserName, msg); // record my own message

					try{
						ChatAPI.SendGroupChat(mGroupId, msg);
					} catch (Exception e){
						Log.e(TAG, "Error Sending Chat: "+e.toString());
					}
				}
			});

			// text input box
			vMessage = (EditText)  vView.findViewById(Utility.getResourseIdByName(getActivity().getPackageName(),"id","text"));

			ChatAPI.RegisterListener(new ChatListener(){

				@Override
				public void onChatRoomFound(String room, String[] users) {

				}

				@Override
				public void onChatRoomLost(String room) {

				}

				@Override
				public void onChatMsg(String room, String user, String msg) {
					addMessage(room, user, msg);
					Log.v(TAG, "ChatMsg. room: "+room+", user: "+user+", msg: "+msg);
				}

				@Override
				public void onGroupChatMsg(String group, String user, String msg) {
					Log.v(TAG, "ChatMsg. group: "+group+", user: "+user+", msg: "+msg);
					if (group.equals(mGroupId)){
						Log.v(TAG, "("+mGroupId+") Adding message: "+msg);
						addMessage(group, user, msg);
					} else {
						Log.v(TAG, "("+mGroupId+") Ignoring message for group: "+group);
					}
				}

			});

		}

		return vView;
	}


	@Override
	public void onDestroyView(){
		super.onDestroyView();
		final ViewParent parent = vView.getParent();
		if (parent != null && parent instanceof ViewGroup)
			((ViewGroup) parent).removeView(vView);  
	}

	public void addMessage(String group, String user, String msg) {
		mAdapter.add(group, user, msg);
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				vView.requestLayout();
			}
		});
	}

	public void setGroupId(String group) {
		this.mGroupId = group;
	}
}
