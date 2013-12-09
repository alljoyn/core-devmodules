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
package org.alljoyn.aroundme.MainApp;


/*
 * Startup activity for the main app.
 * Just displays a splash screen and then starts the top level program for the UI
 */




import java.lang.reflect.Method;

import org.alljoyn.aroundme.R;
import org.alljoyn.devmodules.APICoreCallback;
import org.alljoyn.devmodules.APICoreImpl;
import org.alljoyn.devmodules.common.Utilities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


public class StartupActivity extends Activity {


	private static final String TAG = "StartupActivity";
	private WifiManager.MulticastLock mMulticastLock;
	Context mContext;
	Activity mActivity;
	boolean  mServiceStarted = false;
	boolean  mSplashDone = false;


	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		mActivity = this;
		mServiceStarted = false;
		mSplashDone = false;

		// layout the UI
		Log.d(TAG, "Displaying splashscreen");
		setContentView(R.layout.splash);

		//Check AllJoyn has a transport for external communication
		boolean alljoynConnectionCheck = false;
		WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE); 
		alljoynConnectionCheck = (wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
		if(!alljoynConnectionCheck) {
			Method[] wifiMethods = wifi.getClass().getDeclaredMethods();
			for(Method method: wifiMethods){
				if("isWifiApEnabled".equals(method.getName())) { 
					try {
						alljoynConnectionCheck = (Boolean) method.invoke(wifi);
						break;
					} 
					catch (Exception e) {   
						e.printStackTrace(); 
					}
				} 
			}
		}

		if(!alljoynConnectionCheck) {
			BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter != null) {
				alljoynConnectionCheck = mBluetoothAdapter.isEnabled();
			}
		}

		if(alljoynConnectionCheck)
			Log.d(TAG, "AllJoyn has a connection that it can communicate externally to other devices.");
		else
			Log.e(TAG, "AllJoyn has no connection for external communication.");

		// Acquire WiFi lock
		try{
			WifiManager wm = (WifiManager)getSystemService(WIFI_SERVICE); 
			mMulticastLock = wm.createMulticastLock("AllJoynLock"); 
			mMulticastLock.acquire();
		} catch (Exception e){
			Log.e(TAG, "Error creating multicast lock: "+e.toString());
		}

		splashThread.start();	

	} //onCreate


	// Thread to wait for a while, then start the real processing
	Thread splashThread = new Thread() {
		@Override
		public void run() {
			// start the background service and give it some time to initialise while the splash screen is displaying
			try {
				int waited = 100;
				while (waited < 10000) {
					sleep(100);
					waited += 100;
				}
			} catch (InterruptedException e) {
				// do nothing
			} finally {
				mSplashDone = true;
				Log.v(TAG, "splashThread finished");
				try {
					Log.v(TAG, "Setting up background service...");
					APICoreImpl.StartAllJoynServices(mActivity, new APICoreCallback(){
						@Override
						public void onServiceReady() {
							Log.v(TAG, "onServiceReady()");
							mServiceStarted = true;
							startMainUI();
						}
					});
					Log.v(TAG, "Background service started");
				} catch (Exception e1) {
					Log.e(TAG, "Error starting Framework Services: "+e1.toString());
				}
				// check to see if service started, if not, wait for a while
				if (!mServiceStarted){
					Log.e(TAG, "Splash screen done, but service not started. Waiting a while longer..");
					svcWaitThread.start();
				}
			}
		}
	};

	// Thread to wait for a while, then start the real processing
	Thread svcWaitThread = new Thread() {
		@Override
		public void run() {
			try {
				int waited = 100;
				while ((waited < 10000) && (!mServiceStarted)) {
					sleep(100);
					waited += 100;
				}
			} catch (InterruptedException e) {
				// do nothing
			} finally {
				// do nothing
				Log.v(TAG, "svcWaitThread finished");

				if (!mServiceStarted){
					Log.e(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					Log.e(TAG, "!! Service not started. Stopping App... !!");
					Log.e(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					finish();
				}
			}
		}
	};

	/* Called when the activity is exited. */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Release the WiFi multicast lock
		try{
			mMulticastLock.release();
		} catch (Exception e){
			Log.e(TAG, "Error releasing Multicast lock: "+e.toString());
		}
		finish();
	}

	@Override
	protected void onPause() {
		super.onPause();

		//TODO: code to save display data?
	}
	@Override
	protected void onResume() {
		super.onResume();

		//TODO: code to restore display?
	}


	//  this method is called when the Top-level activity returns
	@Override  
	public void onActivityResult(int reqCode, int resultCode, Intent intent) {  
		//super.onActivityResult(reqCode, resultCode, intent);  

		//result doesn't really matter, so no need to check 
		// just exit...
		Log.v(TAG, "Returned from top-level");
		finish();

	} //onActivityResult


	// utility to start the main UI Activity
	private void startMainUI(){
		Log.v(TAG, "Starting top-level activity");
		Intent intent = new Intent();
		intent.setAction(AppConstants.INTENT_PREFIX + ".TOP");
		intent.putExtra("launchClass", AppConstants.INTENT_PREFIX + ".FRONTPAGE");
		try {
			//startActivityForResult(intent, 0);
			startActivity(intent);
			finish();
		} catch (Exception e){
			Utilities.logException(TAG, "Error starting Top-level activity.", e);
			((Activity) mContext).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), 
							"Error starting Top-level activity", 
							Toast.LENGTH_SHORT).show();
				}
			}); 
		}
	}


} // end of Activity

