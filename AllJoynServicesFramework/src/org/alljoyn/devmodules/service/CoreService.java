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
package org.alljoyn.devmodules.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.alljoyn.bus.annotation.BusSignalHandler;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import android.widget.LinearLayout;

public class CoreService extends Service {	

	private static final String TAG = "CoreService";
	
	private static CoreService instance;
	
//	public static CoreService getInstance() {
//		return instance;
//	}

    private AllJoynContainer mAllJoynContainer=null;

    
    @Override
    public void onCreate() {
    	Log.i(TAG, "onCreate()");
    }

    @Override
    public void onDestroy() {
    	Log.i(TAG, "onDestroy()");
    	if(mAllJoynContainer != null)
    		mAllJoynContainer.free();
    	mAllJoynContainer = null;
    	/** Lets sleep for 5000ms so that the UDP packets can go out */
    	try{
    		Thread.sleep(5000);
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	android.os.Process.killProcess(android.os.Process.myPid());
    }
    
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        // onCreate() is not always called
    	if(instance == null) {
    		Log.i(TAG, "Creating instance...");
    		instance = this;

			// Let the AllJoyn daemon know our application context (needed for network queries)
			//org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
    		mAllJoynContainer = AllJoynContainer.getInstance(this,
    				intent != null && intent.getExtras() != null && intent.getExtras().containsKey("serviceId")
    					? intent.getExtras().getString("serviceId") : "");
    	} else {
    		Log.d(TAG, "instance != null");
    	}
    	
    	// check services to see if any of them need to be started
    	Log.d(TAG, "About to check mCoreLogic var");
    	if (mAllJoynContainer != null){
    		mAllJoynContainer.checkServices();
    	}
    	Log.i(TAG, "mCoreLogic started?");
    	
    	startForeground();
    	
        return START_STICKY;
    }
    
    private void startForeground() {
    	CharSequence title = "AllJoyn Services";
        CharSequence message = "Service running";
    	Notification notification = new Notification();
    	try{
//    		notification = new Notification.Builder(this.getApplicationContext())
//            .setContentTitle(title)
//            .setContentText(message)
//            .build();
//	        notification.contentIntent = PendingIntent.getService(this, 0, new Intent(), 0);
//	        notification.flags |= Notification.DEFAULT_SOUND | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
        Log.i(TAG, "onCreate(): startForeground()");
        startForeground(NOTIFICATION_ID, notification);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
    private static final int NOTIFICATION_ID = 0xdefabcde;

}
