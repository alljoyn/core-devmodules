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
package org.alljoyn.aroundme.Debug.LogOutput;




import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.DebugAdapter;
import org.alljoyn.aroundme.R.layout;

import android.app.ListActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;



public class DebugLogActivity extends ListActivity {



	private static final String TAG = "DebugListActivity";

	private static DebugAdapter   mAdapter; 
	private static boolean        mShutdown = false;

	private HandlerThread handlerThread = new HandlerThread(TAG);
	{handlerThread.start();}
	private UIhandler mHandler = new UIhandler(handlerThread.getLooper()); // mHandler for complex functions


	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug);


		// Set up list adapter for scrolling text output
		mAdapter = DebugAdapter.getAdapter(); 
		DebugAdapter.setContext(this); 
		setListAdapter(mAdapter); 

		mShutdown      = false;

		// Initiate reading of debug log
		mHandler.getDebugInfo();

		// nothing else to do, the ListAdapter handles update of display etc.
	}



	/* Called when the activity is exited. */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mShutdown = true;
	}



	private void readDebugInfo(){


		Thread logThread = new Thread(){
			@Override
			public void run() {
				BufferedReader bufferedReader=null;
				Process process = null; 
				InputStreamReader dbgstream = null;
				try {

					// versions of Android after ICS don't handle this well, so just dump everything
					process = Runtime.getRuntime().exec(new String[]{"logcat", "-d"});
					//process = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-v", "time", "-s"});
					/****
			process = Runtime.getRuntime().exec(new String[]
			                                    {"logcat", "-d", "AndroidRuntime:W dalvikvm:W *:D" });
					 ***/
					//{"logcat", "-d", "AndroidRuntime:E dalvikvm:E *:D" });// Debug filter
					/* -d dumps the log file and exits
					 * Check different filters available at 
					 * http://developer.android.com/guide/developing/tools/logcat.html , 
					 * Options section 
					 */
					Log.i(TAG, "Opening logcat stream");
					dbgstream = new InputStreamReader(process.getInputStream());
					bufferedReader = new BufferedReader(dbgstream);

					String line;
					String lvl;
					while  (!mShutdown && ((line = bufferedReader.readLine()) != null)){
						if      (line.startsWith("D/")) lvl = "d";
						else if (line.startsWith("E/")) lvl = "e";
						else if (line.startsWith("I/")) lvl = "i";
						else if (line.startsWith("W/")) lvl = "w";
						else if (line.startsWith("V/")) lvl = "v";
						else lvl = "i";
						final String theLine = line;
						final String theLvl = lvl;
						DebugLogActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mAdapter.add(theLine.toString(), theLvl);
							}
						});

					}// while buffer !null

				} catch (IOException e) {
					Log.e(TAG, "Error getting logcat output: "+e.toString());
					mShutdown = true;
				} finally {
					//Log.d(TAG, "exiting logcat loop");
					if (bufferedReader != null)
						try {
							bufferedReader.close();
						}catch (IOException e){
							Log.e(TAG, "Error closing buffered reader");
						}
				}

				// clean up
				try {
					if (bufferedReader != null) bufferedReader.close();
					if (dbgstream != null)      dbgstream.close();
					if (process != null)        process.destroy();
				}catch (IOException e){
					Log.e(TAG, "Error closing log buffers: "+e.toString());
				}	
			}
		};

		if(!logThread.isAlive()) {
			logThread.run();
		}

	}




	/////////////////////////////////////////////////////////////////////////////////
	// UI Handler Message Queue Thread
	// Initiate all UI-related functions through accessor methods for this
	/////////////////////////////////////////////////////////////////////////////////

	private class UIhandler extends Handler{

		public UIhandler(Looper loop) {
			super(loop);
		}

		// List of UI commands
		private static final int UI_GETDEBUGINFO =  1;

		// Accessor Methods

		public void getDebugInfo(){
			Message msg = obtainMessage(UI_GETDEBUGINFO);
			sendMessageDelayed(msg, 500);	
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case UI_GETDEBUGINFO: {
				readDebugInfo();
				if (!mShutdown){
					// keep sending request to get debug info
					getDebugInfo();						
				} else {
					Log.d(TAG, "Exiting debug loop");
				}
				break;
			}

			default: {
				Toast.makeText(getApplicationContext(), "ERR: Unkown UI command", Toast.LENGTH_LONG).show();
				break;
			}
			}//switch
		}


	}//UIhandler

} // end of Activity
