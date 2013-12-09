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
package org.alljoyn.whiteboard.activity;

import org.alljoyn.whiteboard.api.WhiteboardAPI;
import org.alljoyn.whiteboard.api.WhiteboardListener;
import org.alljoyn.devmodules.APICoreCallback;
import org.alljoyn.devmodules.APICoreImpl;
import org.alljoyn.devmodules.common.WhiteboardLineInfo;

import com.example.whiteboardmoduleunittest.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class MainActivity extends FragmentActivity {
	
	private static final String TAG = "WhiteboardMainActivity";
	private org.alljoyn.whiteboard.activity.UIFragment whiteboardFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    try {
			Log.v(TAG, "Setting up background service...");
			APICoreImpl.StartAllJoynServices(this, new APICoreCallback() {
				@Override
				public void onServiceReady() {				
					Log.v(TAG, "Background service started");
					try {
						Thread.sleep(5000);
						APICoreImpl.StartAllServices();
					} catch (Exception e) {
						e.printStackTrace();
					}
					WhiteboardAPI.RegisterListener(new WhiteboardListener(){

						@Override
						public void onWhiteboardServiceAvailable(String service) {
							
						}

						@Override
						public void onWhiteboardServiceLost(String service) {
							
						}

						@Override
						public void onRemoteDraw(WhiteboardLineInfo lineInfo) {
							whiteboardFragment.draw(lineInfo);
						}

						@Override
						public void onClear() {
							whiteboardFragment.clear();
						}

						@Override
						public void onRemoteGroupDraw(String group,
								WhiteboardLineInfo lineInfo) {
							whiteboardFragment.draw(lineInfo);
						}

						@Override
						public void onGroupClear(String group) {
							whiteboardFragment.clear();
						}

					});
				}
			});
		} catch (Exception e1) {
			Log.e(TAG, "Error starting Framework Services: "+e1.toString());
		}
	    setContentView(R.layout.test);
	    whiteboardFragment = (org.alljoyn.whiteboard.activity.UIFragment) getSupportFragmentManager().findFragmentById(R.id.whiteboardfragment);
	}	
}
