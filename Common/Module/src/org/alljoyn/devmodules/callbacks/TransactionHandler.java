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
package org.alljoyn.devmodules.callbacks;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class TransactionHandler {	
	public CountDownLatch latch = new CountDownLatch(1);
	private JSONObject jsonData;
	private Timer timer = new Timer();
	private TimerTask timeoutTask;
	
	public void HandleTransaction(String jsonCallbackData, byte[] rawData, int totalParts, int partNumber) {
		synchronized(latch) {
			try {
				jsonData = new JSONObject(jsonCallbackData);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			latch.countDown();
			if(timeoutTask != null)
				timeoutTask.cancel();
		}
	}
	
	public JSONObject getJSONData() {
		return jsonData;
	}
	
	public void startTimeout() {
		timeoutTask = new TimerTask() {
			@Override
			public void run() {
				synchronized(latch) {
					jsonData = new JSONObject();
					try {
						Log.d("TRANSACTIONHANDLER", "ERROR TIMEOUT!!!!");
						jsonData.put("error", "timeout");
					} catch (Exception e) {
						e.printStackTrace();
					}
					latch.countDown();
				}
			}
		};
		timer.schedule(timeoutTask, 30000);
	}
}
