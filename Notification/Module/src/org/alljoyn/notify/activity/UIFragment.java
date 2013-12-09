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
package org.alljoyn.notify.activity;

import org.alljoyn.notify.api.NotifyAPI;
import org.alljoyn.notify.api.NotifyListener;
import org.alljoyn.notifymodule.R;
import org.alljoyn.devmodules.common.NotificationData;
import org.alljoyn.devmodules.util.Utility;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class UIFragment extends Fragment {
	private NotificationManager notificationManager;
	private int notificationId = 1;
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public UIFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		View view = inflater.inflate(Utility.getResourseIdByName(getActivity().getPackageName(),"layout","notify_uifragment"), container, false);
		Button b = (Button)view.findViewById(Utility.getResourseIdByName(getActivity().getPackageName(),"id","button1"));
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				try {
					NotificationData data = new NotificationData();
					data.msg = "test";
					data.deviceName = "NotifyTest";
					data.eventType = 1;
					NotifyAPI.SendGlobalNotification(data);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		NotifyAPI.RegisterListener(new NotifyListener() {
			@Override
			public void onNotification(String peer, NotificationData msg) {
				try{
					Notification notification = new Notification.Builder(getActivity().getApplicationContext())
			         .setContentTitle(msg.msg)
			         .setContentText("From:"+ msg.deviceName)
			         .setSmallIcon(getActivity().getPackageManager().getApplicationInfo(getActivity().getPackageName(), 0).icon)
			         .build();
					notificationManager.notify(notificationId++, notification);
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		notificationManager = (NotificationManager) getActivity().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        return view;
    }
	
	
}
