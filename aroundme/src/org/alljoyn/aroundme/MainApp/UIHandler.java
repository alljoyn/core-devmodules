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

/////////////////////////////////////////////////////////////////////////////
//  This is the Main UI logic of the application. It is the only UI guaranteed to be running, so
// most general UI functions such as adding items to lists, notifying the user etc.  are done here.
// The actuall display activities and user input handling are done elsewhere, but this is the only thread
// that can update UI components that span multiple activities and threads
/////////////////////////////////////////////////////////////////////////////

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.alljoyn.aroundme.Adapters.ContactAdapter;
import org.alljoyn.aroundme.Adapters.DebugAdapter;
import org.alljoyn.devmodules.common.ProfileDescriptor;

//import org.alljoyn.devmodules.profile.ProfileDescriptor;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;



public class UIHandler {


	// List of UI commands
	private static final int UI_INIT                   =  1;  // Initialise
	private static final int UI_START_PROCESSING       =  2;  // Start the processing
	private static final int UI_LOG                    =  3;  // log message in debug window
	private static final int UI_ERROR                  =  4;  // error popup
	private static final int UI_ADD_CONTACT            =  5;  // Add a contact
	private static final int UI_REMOVE_CONTACT         =  6;  // Remove a contact
	private static final int UI_STOP                   = 16;  // stop processing and quit


	// Colour values
	private static final int UI_COLOUR_BACKGROUND  = 0xFFFFFFFF ;
	private static final int UI_COLOUR_TEXT        = 0xFF0900E5 ;
	private static final int UI_COLOUR_ERROR       = 0xFFFF0000 ;


	private static final String TAG = "UIHandler";

	private static boolean mFirstTime = true; // flag indicating first activation
	private static boolean running = false;   // flag indicating UI is active (filters multiple requests)

	private static Activity       mActivity; 
	private static ContactAdapter mContacts ;
	private static DebugAdapter   mDebug ; 

	//## private static BusHandler mBusHandler; // handle to main bus-processing thread




	// Constructor - save the current View context for later use
	public UIHandler(Activity activity) { 
		mActivity = activity ; 
		
		// some lists are used before main processing starts, so initialise them here (not in INIT)
		
		// debug list
		mDebug = DebugAdapter.getAdapter(); 
		DebugAdapter.setContext(mActivity);
		DebugAdapter.clear();

		// contact list
		mContacts  = ContactAdapter.getAdapter();
		ContactAdapter.setContext(mActivity);
		ContactAdapter.clear();

	} 

	////////////////////////////////////////////////////
	// Utilities for sending messages to the UI
	// (Note that these are called from the context of other activities, not the UI thread.
	//  This is why they send messages rather than doing anything directly)
	////////////////////////////////////////////////////

	public void init (){
		MainUIHandler.sendEmptyMessage(UI_INIT);
		// Naughty, but register activity globally
		HandlerList.mUIHandler = this;

		running = true;
	}

	public void stop (){
		if (running){
			MainUIHandler.sendEmptyMessage(UI_STOP);
			running = false;
		}
	}

	public void addContact (ProfileDescriptor profile){
		Message msg = MainUIHandler.obtainMessage(UI_ADD_CONTACT);
		msg.obj = profile ;
		MainUIHandler.sendMessage(msg);
	}

	public ProfileDescriptor getContact (String name){
		return ContactAdapter.getProfile(name);
	}

	public void removeContact (String name){
		Message msg = MainUIHandler.obtainMessage(UI_REMOVE_CONTACT);
		msg.obj = name ;
		MainUIHandler.sendMessage(msg);		
	}

	public void displayError (String error){
		Message msg = MainUIHandler.obtainMessage(UI_ERROR);
		msg.obj = error ;
		MainUIHandler.sendMessage(msg);
	}

	public void logDebugText (String text){
		Message msg = MainUIHandler.obtainMessage(UI_LOG);
		msg.obj = text ;
		MainUIHandler.sendMessage(msg);
	}

/***##
	// method to set the bus handler reference
	public void setBusHandler (BusHandler bh){
		mBusHandler = bh;
	}
	##**/

	// display a short pop-up text box
	public void popup(String msg){
		Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show();
	}

	// Update list adapters
	public void updateLists() {

	}

	////////////////////////////////////////////////////////////////////////////////////////
	//  MAIN UI MESSAGE LOOP
	////////////////////////////////////////////////////////////////////////////////////////
	/*
	 * UI Message Queue - updates to the UI should be done by sending a message to this loop
	 * using mUIHandler.sendMessage()
	 */
	public Handler MainUIHandler = new Handler() {


		@Override
		public void handleMessage(Message msg) {
			String stxt;
			String [] tmp = {"?", "?", "?"};

			switch (msg.what) {
			case UI_INIT: {



				// Start Bus processing
				//## mBusHandler.sendEmptyMessage(BusHandler.INIT);
				break;
			}

			case UI_START_PROCESSING: {
				// for future use, in case we ever need a two-step init process
				break;
			}
			case UI_ADD_CONTACT: {
				//TODO: check if entry already exists; if so, replace
				ProfileDescriptor profile = (ProfileDescriptor)msg.obj;
				ContactAdapter.add(profile); 
				popup("New contact found: "+profile.getField("profileid"));
				break;
			}

			case UI_REMOVE_CONTACT: {
				/* Remove the service from the list */
				ContactAdapter.remove((String) msg.obj);
				popup("Contact disconnected: "+(String) msg.obj);
				break;
			}


			case UI_LOG: {
				/* Add message to debug list */
				if (mDebug == null)
					mDebug = DebugAdapter.getAdapter(); 
				DebugAdapter.add((String)msg.obj, "i");
				DebugAdapter.update();
				break;
			}


			case UI_ERROR: {
				/* Display error string in popup */
				if (mDebug == null)
					mDebug = DebugAdapter.getAdapter(); 
				DebugAdapter.add((String)msg.obj, "e");
				Toast.makeText(mActivity, (String) msg.obj, Toast.LENGTH_SHORT).show();
				break;
			}

			case UI_STOP: {
				Log.d(TAG, "UI Shutting down");
				//## mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
				ContactAdapter.clear();
				DebugAdapter.clear();
				Toast.makeText(mActivity, "   Bye   ", Toast.LENGTH_SHORT).show();
				getLooper().quit();
				break;
			}

			default: {
				Toast.makeText(mActivity, "ERR: Unknown UI command", Toast.LENGTH_LONG).show();
				break;
			}
			}
		}
	}; //MainUIHandler

} //MainUI
