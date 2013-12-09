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
package org.alljoyn.aroundme.Debug.ServiceBrowser;


/*
 * Simple "Browser for listing available services
 * It works by watching for signals indicating the availability/removal of services
 * and uses Introspection to find the objects within the services.
 * A separate Activity is launched to show the details of a particular object
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.alljoyn.aroundme.R;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.bus.ifaces.Introspectable;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class AllJoynBrowser extends ListActivity {
	static {
		System.loadLibrary("alljoyn_java"); // Load the AllJoyn Java binding (JNI)
	}

	// List of UI commands
	private static final int UI_ADD_SERVICE   = 1;             /* Display string */
	private static final int UI_REMOVE_SERVICE  = 2;           /* Warning text (or different color)    */
	private static final int UI_ERROR  = 3;                    /* error popup    */


	// Colour values
	private static final int UI_COLOUR_SERVICE    = 0xFF0900E5 ;
	private static final int UI_COLOUR_ERROR      = 0xFFFF0000 ;


	private static final String TAG = "AJBrowser";


	private TextAdapter          mAdapter; 
	private BusHandler           mBusHandler;
	private Menu                 mMenu;


	/*
	 * UI Message Queue - updates to the UI should be done by sending a message to this loop
	 * using mUIHandler.sendMessage()
	 */
	public Handler mUIHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String stxt;
			String [] args = new String [2];
			args[0]="?";
			args[1]="?";

			switch (msg.what) {
			case UI_ADD_SERVICE: {
				//string is formatted as "service+owner", so just split the string
				stxt = (String)msg.obj;

				args = stxt.split("\\|");

				mAdapter.add(args[0], args[1]); 
				break;
			}

			case UI_REMOVE_SERVICE: {
				/* Remove the service from the list */
				mAdapter.remove((String) msg.obj);
				break;
			}


			case UI_ERROR: {
				/* Display error string in popup */
				Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
				break;
			}

			default: {
				Toast.makeText(getApplicationContext(), "ERR: Unkown UI command", Toast.LENGTH_LONG).show();
				break;
			}
			}
		}
	};

	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ajbrowser);


		// Set up list adapter for scrolling text output
		mAdapter = new TextAdapter(this); 
		setListAdapter(mAdapter); 

		/* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
		HandlerThread busThread = new HandlerThread("BusHandler");
		busThread.start();
		mBusHandler = new BusHandler(busThread.getLooper());

		/* Initialise the bus handler processing */
		mBusHandler.sendEmptyMessage(BusHandler.INIT);


	}

	/* Called when the activity is exited. */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Use the mBusHandler to disconnect from the bus. Failing to to this could result in memory leaks
		mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
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


	/* Menu Options setup and processing */
	/* Called when the menu button is pressed. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		this.mMenu = menu;
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true; //must return true for options menu to display
	}

	/* Called when a menu item is selected */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* Handle item selection
		 */
		switch (item.getItemId()) {

		case R.id.quit:
			mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
			onDestroy();
			finish();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/* When clicked, launch the selected activity */

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) 
	{    

		// Launch the selected Activity (if entry is not null)
		String sname = mAdapter.getName(position);
		if (null!=sname){
			String oname = mAdapter.getOwner(position);
			Intent myIntent = new Intent();
			myIntent.setAction(BrowserConstants.BROWSER_PREFIX+".OBJECT");
			myIntent.putExtra(BrowserConstants.BROWSER_PREFIX+".service", sname); 
			myIntent.putExtra(BrowserConstants.BROWSER_PREFIX+".owner", oname); 
			myIntent.putExtra(BrowserConstants.BROWSER_PREFIX+".object", "/"); // start at root
			try {
				startActivity(myIntent);
			} catch (Throwable t){
				Toast.makeText(this, "Error starting Activity for "+sname, Toast.LENGTH_SHORT).show();

			}
		}
		else {
			Toast.makeText(this, "Undefined Service", Toast.LENGTH_SHORT).show();

		}
	}

	// List Management

	// convenience class for service parameters
	class ServiceHolder {
		public String name ;
		public String owner ;

		public ServiceHolder (String n, String o){
			this.name = n ;
			this.owner = o ;
		}
	}  


	//Adapter to deal with formatted strings
	public class TextAdapter extends BaseAdapter { 

		//	Lists of the service and owner names
		private ArrayList<String> mServiceList = new ArrayList<String>(); 
		private ArrayList<String> mOwnerList = new ArrayList<String>(); 

		// Context of the current View
		private Context mContext; 


		public TextAdapter(Context c) { 
			mContext = c; 
		} 

		public int getCount() { 
			return mServiceList.size(); 
		} 

		public Object getItem(int position) { 
			return position; 
		} 

		public long getItemId(int position) { 
			return position; 
		} 

		public String getName(int position){
			return mServiceList.get(position);
		}

		public String getOwner(int position){
			return mOwnerList.get(position);
		}

		// add a new item
		public void add (ServiceHolder sh) {
			mServiceList.add(sh.name);
			mOwnerList.add(sh.owner);
		}

		// convenience function to add name and owner
		public void add (String n, String o) {
			int index = mServiceList.indexOf(n); // check to see if it already exists
			if (index < 0) { //not found, so add it
				mServiceList.add(n);
				mOwnerList.add(o);
				this.notifyDataSetChanged(); // force re-display of list
			} else {
				// Owner can be updated, so if service is found, just update owner
				mOwnerList.set(index, o);        // replace the owner name
				//Toast.makeText(getApplicationContext(), "Duplicate entry ignored: "+n, Toast.LENGTH_LONG).show();
			}
		}

		public void addName (String n){
			int index = mServiceList.indexOf(n); // check to see if it already exists
			if (index < 0) {                     // not found, so add it
				mServiceList.add(n);
				mOwnerList.add("?");             // owner is not always known, so put in temp value
				this.notifyDataSetChanged();     // force re-display of list
			} else {
				// just log and ignore
				Toast.makeText(getApplicationContext(), "Duplicate entry ignored: "+n, Toast.LENGTH_LONG).show();
			}

		}

		public void updateOwner (String n, String o){
			int index = mServiceList.indexOf(n); // check to see if it already exists
			if (index >= 0) { 
				mOwnerList.set(index, o);        // replace the owner name
				this.notifyDataSetChanged();     // force re-display of list
			} else {
				// just log and ignore
				Toast.makeText(getApplicationContext(), "Entry not found: "+n, Toast.LENGTH_LONG).show();
			}

		}

		public void remove (String name){
			int index ;
			index = mServiceList.indexOf(name); // find the entry
			if (index >= 0){
				mServiceList.remove(index);
				mOwnerList.remove(index);
				this.notifyDataSetChanged(); // force re-display of list
			} else {
				//entry not found
				Toast.makeText(getApplicationContext(), "Entry not found: "+name, Toast.LENGTH_LONG).show();
			}

		}

		// return the View to be displayed
		public View getView(int position, View convertView, ViewGroup parent) { 

			TextView tv ;

			if  (convertView == null) {
				tv = new TextView(mContext); 
			} else {
				tv = (TextView)convertView;
			}

			tv.setText(mServiceList.get(position)); // just displaying name for now
			//tv.setTextColor(UI_COLOUR_SERVICE);//hack until inflate code is working

			return tv; 
		} 

	} // TextAdapter

	/*
	 * ===============================================================================
	 * ALLJOYN-SPECIFIC PROCESSING
	 * ===============================================================================
	 */

	/*--------------------------------------------------------------------------------
	 * Handler for bus-specific processing. Most activities are started/stopped via 
	 * messages from the UI
	 */
	public class BusHandler extends Handler {


		/* Messages handled by the message loop */
		public static final int  INIT = 0 ;
		public static final int  CONNECT_WITH_REMOTE_BUS = 1;
		public static final int  DISCONNECT = 2;
		public static final int  INTROSPECT = 3 ;

		//AllJoyn specific elements
		private BusAttachment mBus;
		private boolean       mIsConnected;
		private boolean       mIsStoppingDiscovery;


		private List<String>  mBusAddressList;

		DBusProxyObj          dbusProxy;
		String                mOwner = "?";


		public BusHandler(Looper looper) {
			super(looper);


			mBusAddressList = new LinkedList<String>();
			mIsStoppingDiscovery = false;
		}


		/* The main processing logic. This method is called when a message is received by the Handler */
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case (INIT): {


				/* set up the main bus objects and connects to the local daemon */

				//Create new BusAttachment
				//String name = getClass().getName();
				String name = "org.alljoyn.samples.ajbrowser";
				Log.d(TAG, "Attaching to bus: "+name);
				mBus = new BusAttachment(name, BusAttachment.RemoteMessage.Receive);              


				// The DBusProxyObj creates a way to make calls to methods built into the DBus standard
				dbusProxy = mBus.getDBusProxyObj();

				// save the local owner ID
				mOwner = mBus.getUniqueName();
				if (mOwner.length()==0)mOwner="?";

				//connect the BusAttachment to the daemon.
				Status status = mBus.connect();
				logStatus("BusAttachment.connect()", status);
				if (status != Status.OK) {
					showError("Error connecting to bus. Is daemon/service running?!");
					return;
				}			

				mBus.useOSLogging(true);
				mBus.setDebugLevel("ALLJOYN_JAVA", 7);

				// define the Listener callback for handling bus events
				mBus.registerBusListener (new BusListener() {
					
					public void foundAdvertisedName(String name, short transport, String namePrefix) {
						Message msg ;

						if (mOwner.length() == 0) mOwner = "?";

						msg = obtainMessage(UI_ADD_SERVICE, name+"|"+mOwner); // Don't know owner, so send dummy entry
						mUIHandler.sendMessage(msg);

						Log.i(TAG, String.format("foundAdvertisedName activated for: "+name));
						msg = obtainMessage(CONNECT_WITH_REMOTE_BUS);
						sendMessage(msg);
					}

					// This handler is called when a LostAdvertisedName signal is received, indicating that an instance
					// of the service has gone away
					public void lostAdvertisedName(String name, short transport, String namePrefix) {
						if (!name.startsWith(":")){
							// ":" indicates an "owning" connection, which are not displayed, so don't
							// bother removing
							Message msg = obtainMessage(UI_REMOVE_SERVICE, name);
							mUIHandler.sendMessage(msg);
						}
					}
				});

				/*
				 * This is registers anything annotated with the @BusSignalHandler within this class
				 */
				status = mBus.registerSignalHandlers(this);
				logStatus("BusAttachment.registerSignalHandlers()", status);
				if (status != Status.OK) {
					showError("Error registering signal handler: " + status.toString());
					return;
				}

				// Get the list of current services and add to the display
				try {
					String[] svcList = dbusProxy.ListNames();
					if (svcList.length>0) {
						Message m ;
						for (int i=0; i<svcList.length; i++){
							if (!svcList[i].startsWith(":")){
								//TODO: look up owner
								String owner = dbusProxy.GetNameOwner(svcList[i]);
								if (owner.length()==0){
									owner = "?";
								}
								m = obtainMessage(UI_ADD_SERVICE, svcList[i]+"|"+owner);
								mUIHandler.sendMessage(m);
							}
						}

					}	

				} catch  (BusException ex) {
					logException("BusException getting service list", ex);
				}


				/*
				 * Initiate discovery for services. Note that I use the prefix "org.", 
				 * so any service starting with that string will be detected. This includes any of our samples.
				 * If found the bus will send out a "FoundName" signal, so we must register a signal handler 
				 * (outside the handleMessage() method) for the 'FoundName' signal to know about any remote buses. 
				 */

				try {
					//AllJoynProxyObj.FindNameResult findNameResult = alljoynProxy.FindName(NAME_PREFIX);
					// for now, hacking it and looking for any service that begins with "org."
					//AllJoynProxyObj.FindNameResult findNameResult = alljoynProxy.FindName("org.");
					status = mBus.findAdvertisedName("org.");
					logStatus("BusAttachment.findAdvertisedName()", status);
					if (status != Status.OK) {
						showError("Error find name: " + status.toString());
						return;
					}

				} catch  (Exception ex) {
					logException("BusException while trying to Find service", ex);
				}
				break;
			}

			/*
			 * When the 'FoundName' signal is received it will send the address of the remote bus 
			 * to this BusHandler case. The AllJoyn connect method is used to make a P2P connection between
			 * two separate buses. 
			 */
			case (CONNECT_WITH_REMOTE_BUS): {
				/*#### not needed for 2.0
				 */
				break;
			}
			/*
			 * Disconnect the local bus from all of the other buses that have been found. 
			 * Stop looking for the NAME_PREFIX
			 * Stop the local bus from advertising its own well known name so no other 
			 * buses will try and connect with the local bus.
			 * Remove the wellKnownName from the local bus.
			 */

			case (DISCONNECT): {
				mIsStoppingDiscovery = true;

				// disconnect from any peers
				try {

					mBusAddressList.clear();               
					mIsStoppingDiscovery = false;

					mBus.unregisterSignalHandlers(this);

					Status status = mBus.cancelFindAdvertisedName("org.");
					logStatus("mBus.CancelFindAdvertisedName()", status, Status.OK);

				} catch (Exception ex) {
					logException("BusException while trying to stop advertising", ex);
				}

				getLooper().quit();
				break;
			}

			/*
			 * Introspect the supplied service
			 * For now, just print out the results to the log
			 */
			case (INTROSPECT): {
				// This is really just for debug - log the Introspection data for a named service
				ProxyBusObject proxyObj;
				Introspectable introspectIf;
				String svcName = (String) msg.obj;
				String objName = "/";
				proxyObj =  mBus.getProxyBusObject(svcName, objName,0, new Class<?>[] { Introspectable.class });
				Log.i(TAG, String.format("Introspection data for: " + svcName + objName)) ;

				introspectIf = proxyObj.getInterface(Introspectable.class);

				try {
					String intData = introspectIf.Introspect();
					Log.i(TAG, String.format(intData)) ;
				} catch (BusException ex) {
					logException("BusException while trying to Introspect "+svcName+"\n", ex);
				}
			}

			default:
				break;
			}
		}

		public boolean usingDiscovery(){
			return this.mIsConnected;
		}

		/*
		 * SIGNAL HANDLERS
		 * The @BussignalHandler annotation is used to identify this as a signal listener.  When 
		 * BusAttachment.registerSignalHandlers(Object) is called all methods in the specified 
		 * Object that contain the @BusSignalHandler annotation will be called when the specified 
		 * signal comes from the specified interface.  
		 */

		/***
		BusListener mBusListener = new BusListener() {

			public void foundAdvertisedName(String name, short transport, String namePrefix) {
				Message msg ;

				if (mOwner.length() == 0) mOwner = "?";

				msg = obtainMessage(UI_ADD_SERVICE, name+"|"+mOwner); // Don't know owner, so send dummy entry
				mUIHandler.sendMessage(msg);

				Log.i(TAG, String.format("foundAdvertisedName activated for: "+name));
				msg = obtainMessage(CONNECT_WITH_REMOTE_BUS);
				sendMessage(msg);
			}

			// This handler is called when a LostAdvertisedName signal is received, indicating that an instance
			// of the service has gone away
			public void lostAdvertisedName(String name, short transport, String namePrefix) {
				if (!name.startsWith(":")){
					// ":" indicates an "owning" connection, which are not displayed, so don't
					// bother removing
					Message msg = obtainMessage(UI_REMOVE_SERVICE, name);
					mUIHandler.sendMessage(msg);
				}
			}
		};
		
		***/


	} // end of BusHandler


	/* UTILITIES */
	/*
	 * Utilities to display text on main screen
	 */


	private void showError(String msg) {
		Message tmsg = mUIHandler.obtainMessage(UI_ERROR);
		tmsg.obj = msg ;
		mUIHandler.sendMessage(tmsg);
	}

	private void logStatus(String msg, Status status) {
		logStatus(msg, status, Status.OK);
	}

	/*
	 * print the status or result to the Android log. If the result is the expected
	 * result only print it to the log.  Otherwise print it to the error log and
	 * Sent a Toast to the users screen. 
	 */
	private void logStatus(String msg, Object status, Object passStatus) {
		String log = String.format("%s: %s", msg, status);
		if (status == passStatus) {
			Log.i(TAG, log);
		} else {
			//Toast.makeText(this, log, Toast.LENGTH_LONG).show();
			showError ("ERR: " + log);
			Log.e(TAG, log);
		}
	}

	/*
	 * When an exception is thrown use this to Toast the name of the exception 
	 * and send a log of the exception to the Android log.
	 */
	private void logException(String msg, Exception ex) {
		String log = String.format("%s: %s", msg, ex);
		showError ("ERR: " + log);
		Log.e(TAG, log, ex);
	}


} // end of Activity

