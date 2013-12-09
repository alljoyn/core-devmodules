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



import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.alljoyn.aroundme.R;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.ifaces.DBusProxyObj;
import org.alljoyn.bus.ifaces.Introspectable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import android.app.Activity;
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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
/*
 * Simple  application that will display the details of a running Service
 */
public class AJObject extends Activity {
	static {
		System.loadLibrary("alljoyn_java");
	}


	private static final String TAG = "AJObject";

	// Display variables
	private InterfaceAdapter     mIfAdapter;
	private ObjectAdapter        mObjAdapter;
	private TextView             mServiceView ;
	private TextView             mObjectView ;
	private TextView             mOwnerView ;
	private Menu                 mMenu;

	private ListView             lvInterfaces ;
	private ListView             lvObjects ;

	private BusHandler           mBusHandler;

	private String               mServiceName ;
	private String               mObjectName ;
	private String               mOwnerName ;

	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// layout the UI
		setContentView(R.layout.ajobject);

		// get the title views
		mServiceView = (TextView) findViewById(R.id.serviceText);
		mObjectView  = (TextView) findViewById(R.id.objectText);
		mOwnerView   = (TextView) findViewById(R.id.ownerText);

		//get the name of the service, owner and object
		Intent myIntent = getIntent();
		mServiceName = myIntent.getStringExtra(BrowserConstants.BROWSER_PREFIX+".service");
		mObjectName  = myIntent.getStringExtra(BrowserConstants.BROWSER_PREFIX+".object");
		mOwnerName   = myIntent.getStringExtra(BrowserConstants.BROWSER_PREFIX+".owner");

		//HACK: bug in introspection (can't introspect root object), convert to known object
		//String tmp = mObjectName;
		//mObjectName = convertRootObject(tmp);

		// set up display titles
		mServiceView.setText(mServiceName);
		mObjectView.setText(mObjectName);
		mOwnerView.setText(mOwnerName);

		//set up up scrolling lists (Interfaces and Objects)
		mIfAdapter = new InterfaceAdapter(this);
		lvInterfaces = (ListView) findViewById(R.id.interfaceList);
		lvInterfaces.setAdapter(mIfAdapter);

		lvInterfaces.setOnItemClickListener(new ListView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//start Activity to display interface details
				Intent myIntent = new Intent();
				myIntent.setAction(BrowserConstants.BROWSER_PREFIX+".INTERFACE");
				myIntent.putExtra(BrowserConstants.BROWSER_PREFIX+".name", mIfAdapter.getName(position)); 
				myIntent.putExtra(BrowserConstants.BROWSER_PREFIX+".description", mIfAdapter.getDetails(position)); 
				try {
					startActivity(myIntent);
				} catch (Throwable t){
					Toast.makeText(getApplicationContext(), 
							"Error starting Activity for "+mIfAdapter.getName(position), 
							Toast.LENGTH_SHORT).show();

				}

			}
		});



		mObjAdapter = new ObjectAdapter(this);
		lvObjects = (ListView) findViewById(R.id.objectList);
		lvObjects.setAdapter(mObjAdapter);

		lvObjects.setOnItemClickListener(new ListView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// Launch the selected Activity (if entry is not null)
				String sname = mServiceName;
				String oname ;
				if (null!=sname){
					if ("/".equals(mObjectName)){ // currently at root object
						oname = mObjectName + mObjAdapter.getName(position);
					} else {
						oname = mObjectName + "/" + mObjAdapter.getName(position);
					}
					Intent myIntent = new Intent();
					myIntent.setAction(BrowserConstants.BROWSER_PREFIX+".OBJECT");
					myIntent.putExtra(BrowserConstants.BROWSER_PREFIX+".service", sname); 
					myIntent.putExtra(BrowserConstants.BROWSER_PREFIX+".owner", mOwnerName); 
					myIntent.putExtra(BrowserConstants.BROWSER_PREFIX+".object", oname); 
					try {
						startActivity(myIntent);
					} catch (Throwable t){
						Toast.makeText(getApplicationContext(), "Error starting Activity for "+oname, Toast.LENGTH_SHORT).show();
					}

				}
				else {
					Toast.makeText(getApplicationContext(), "Undefined Service", Toast.LENGTH_SHORT).show();

				}
			}
		});



		/* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
		HandlerThread busThread = new HandlerThread("BusHandler");
		busThread.start();
		mBusHandler = new BusHandler(busThread.getLooper());

		/* Initialise the bus handler processing */
		mBusHandler.sendEmptyMessage(BusHandler.INIT);

	}//onCreate

	/* Called when the activity is exited. */
	@Override
	protected void onDestroy() {
		super.onDestroy();
	} //onDestroy


	/* Menu Options setup and processing */
	/* Called when the menu button is pressed. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		this.mMenu = menu;
		return true;
	} //onCreateOptionsMenu

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true; //must return true for options menu to display
	} //onPrepareOptionsMenu

	/* Called when a menu item is selected */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* Handle item selection
		 */
		switch (item.getItemId()) {

		case R.id.quit:
			onDestroy();
			finish();
			return true;

			// TODO: Add help option
		default:
			return super.onOptionsItemSelected(item);
		}
	} // onOptionsItemSelected



	//------------------------------------
	//Adapter to deal with list of Interfaces (behaves differently than object list)
	//------------------------------------

	public class InterfaceAdapter extends BaseAdapter { 

		//	Lists of the service and owner names

		private ArrayList<String> mInterfaceList = new ArrayList<String>();
		private ArrayList<String> mInterfaceDetails = new ArrayList<String>();

		// Context of the current View
		private Context mContext; 

		public InterfaceAdapter(Context c) { 
			mContext = c; 
		} 

		public int getCount() { 
			return mInterfaceList.size(); 
		} 

		public Object getItem(int position) { 
			return position; 
		} 

		public long getItemId(int position) { 
			return position; 
		} 

		public String getName(int position){
			return mInterfaceList.get(position);
		}

		public String getDetails(int position){
			return mInterfaceDetails.get(position);
		}

		// convenience function to add name and details
		public void add (String n, String d) {
			int index = mInterfaceList.indexOf(n); // check to see if it already exists
			if (index < 0) { //not found, so add it
				mInterfaceList.add(n);
				if (d==null)d="?";
				mInterfaceDetails.add(d);
				this.notifyDataSetChanged(); // force re-display of list
			} else {

				//Toast.makeText(getApplicationContext(), "Duplicate entry ignored: "+n, Toast.LENGTH_LONG).show();
			}
		}

		public void add (String n){
			int index = mInterfaceList.indexOf(n); // check to see if it already exists
			if (index < 0) {                     // not found, so add it
				mInterfaceList.add(n);
				mInterfaceDetails.add("?");             // owner is not always known, so put in temp value
				this.notifyDataSetChanged();     // force re-display of list
			} else {
				// just log and ignore
				Toast.makeText(getApplicationContext(), "Duplicate entry ignored: "+n, Toast.LENGTH_LONG).show();
			}

		}


		public void remove (String name){
			int index ;
			index = mInterfaceList.indexOf(name); // find the entry
			if (index >= 0){
				mInterfaceList.remove(index);
				mInterfaceDetails.remove(index);
				this.notifyDataSetChanged(); // force re-display of list
			} else {
				//entry not found
				Toast.makeText(getApplicationContext(), "Entry not found: "+name, Toast.LENGTH_LONG).show();
			}

		}

		public void clear(){
			mInterfaceList.clear();
			mInterfaceDetails.clear();
			this.notifyDataSetChanged();
		}


		// return the View to be displayed
		public View getView(int position, View convertView, ViewGroup parent) { 

			TextView tv ;
			if  (convertView == null) {
				tv = new TextView(mContext); 
			} else {
				tv = (TextView)convertView;
			}

			tv.setText(mInterfaceList.get(position)); // just displaying name for now
			//tv.setTextColor(UI_COLOUR_SERVICE);

			return tv; 
		} 

	} // InterfaceAdapter


	//------------------------------------
	//Adapter to deal with list of Objects
	//------------------------------------
	public class ObjectAdapter extends BaseAdapter { 

		//	Lists of the service and owner names

		private ArrayList<String> mObjectList = new ArrayList<String>();

		// Context of the current View
		private Context mContext; 


		public ObjectAdapter(Context c) { 
			mContext = c; 
		} 

		public int getCount() { 
			return mObjectList.size(); 
		} 

		public Object getItem(int position) { 
			return position; 
		} 

		public long getItemId(int position) { 
			return position; 
		} 

		public String getName(int position){
			return mObjectList.get(position);
		}

		public String getDetails(int position){
			return mObjectList.get(position);
		}

		public void add (String n){
			int index = mObjectList.indexOf(n); // check to see if it already exists
			if (index < 0) {                     // not found, so add it
				mObjectList.add(n);
				this.notifyDataSetChanged();     // force re-display of list
			} else {
				// ignore, not unusual to see entries added multiple times
				//Toast.makeText(getApplicationContext(), "Duplicate entry ignored: "+n, Toast.LENGTH_LONG).show();
			}

		}


		public void remove (String name){
			int index ;
			index = mObjectList.indexOf(name); // find the entry
			if (index >= 0){
				mObjectList.remove(index);
				this.notifyDataSetChanged(); // force re-display of list
			} else {
				//entry not found
				Toast.makeText(getApplicationContext(), "Entry not found: "+name, Toast.LENGTH_LONG).show();
			}

		}

		public void clear(){
			mObjectList.clear();
			this.notifyDataSetChanged();
		}

		// return the View to be displayed
		public View getView(int position, View convertView, ViewGroup parent) { 

			TextView tv ;
			if  (convertView == null) {
				tv = new TextView(mContext); 
			} else {
				tv = (TextView)convertView;
			}

			tv.setText(mObjectList.get(position)); // just displaying name for now
			//tv.setTextColor(UI_COLOUR_SERVICE);

			return tv; 
		} 

	} // ObjectAdapter

	//----------------------------------------------------------------------------
	// UI COMMAND HANDLING
	//----------------------------------------------------------------------------

	// List of UI commands
	private static final int UI_INIT = 1;
	private static final int UI_ADD_INTERFACE = 2;
	private static final int UI_ADD_OBJECT = 3;
	private static final int UI_ERROR  = 4;


	/*
	 * UI Message Queue - updates to the UI should be done by sending a message to this loop
	 * using mUIHandler.sendMessage()
	 */
	public Handler mUIHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case UI_INIT: {

				// set up display titles
				mServiceView.setText(mServiceName);
				mObjectView.setText(mObjectName);
				mOwnerView.setText(mOwnerName);
				mIfAdapter.clear();
				mObjAdapter.clear();
				mBusHandler.sendEmptyMessage(BusHandler.INIT);
				break;
			}
			case UI_ADD_INTERFACE: {
				//string is formatted as "interface|description", so just split the string
				String stxt = (String)msg.obj;
				String[] tmp = stxt.split("\\|");
				mIfAdapter.add(tmp[0], tmp[1]); 
				break;
			}

			case UI_ADD_OBJECT: {
				mObjAdapter.add((String)msg.obj); 
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

	//HACK: convert root object into known root based on service name
	//work around for bug stopping multiple introspections
	private String convertRootObject (String obj){
		String s;
		s = obj;
		if ("/".equals(s)){
			//TODO: define map of possible known services and root objects and loop through
			// for now, just map chat service
			if (s.startsWith("org.alljoyn.bus.samples.chat")){
				s = "/chatService";
			}
		}
		return s;
	} //convertRootObject



	//----------------------------------------------------------------------------
	// BUS PROCESSING
	//----------------------------------------------------------------------------

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
		private String        mBusAddress;

		DBusProxyObj          dbusProxy;

		// constant for the DBUS Introspection DTD text
		private final static String DOC_TYPE = "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\"\n\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">";

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
				//mBus = new BusAttachment(getClass().getName(), BusAttachment.RemoteMessage.Receive);   
				String cname = getApplicationContext().toString();
				String pname = getApplication().getPackageName();
				Random ran = new Random();
				int ranInt = ran.nextInt(1000);
				String rname = Integer.toString(ranInt);


				//mBus = new BusAttachment(pname+rname, BusAttachment.RemoteMessage.Receive);              
				mBus = new BusAttachment(BrowserConstants.BROWSER_PREFIX+".ajbrowser", BusAttachment.RemoteMessage.Receive);              

				// The DBusProxyObj creates a way to make calls to methods built into the DBus standard
				dbusProxy = mBus.getDBusProxyObj();

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
				mBus.registerBusListener (mBusListener);

				/*
				 * This is registers anything annotated with the @BusSignalHandler within this class
				 */
				status = mBus.registerSignalHandlers(this);
				logStatus("BusAttachment.registerSignalHandlers()", status);
				if (status != Status.OK) {
					showError("Error registering signal handler: " + status.toString());
					return;
				}

				/*
				 * Initiate discovery for services. 
				 * If found the bus will send out a "FoundName" signal 
				 */

				try {

					status = mBus.findAdvertisedName(mServiceName);
					logStatus("findAdvertisedName()", status, Status.OK);

				} catch  (Exception ex) {
					logException("BusException while trying to Find service", ex);
				}


				//HACK: shouldn't have to do this
				// Initiate introspection of service
				Message m = obtainMessage(INTROSPECT, mServiceName);
				sendMessage(m);

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

					Status status = mBus.cancelFindAdvertisedName(mServiceName);
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
				// introspect the current service/object and parse the data to get
				// the Interface and Object descriptions
				ProxyBusObject proxyObj;
				Introspectable introspectIf;

				try {
					if (!mServiceName.equals((String)msg.obj)) {
						showError ("Expected: \""+mServiceName+"\"\nGot: \""+(String)msg.obj+"\"");
					}
					proxyObj =  mBus.getProxyBusObject(mServiceName, mObjectName, 0,
							new Class<?>[] { Introspectable.class });

					if (proxyObj == null){
						showError("Null ProxyObject returned");
					}

					String objpath = proxyObj.getObjPath();
					if (!mObjectName.equals(objpath)){
						showError("**Unexpected Path: "+objpath);
					}

					Log.i(TAG, String.format("Introspection data: (" + mServiceName + "," + mObjectName+")")) ;

					introspectIf = proxyObj.getInterface(Introspectable.class);
					String intData = introspectIf.Introspect();
					Log.i(TAG, String.format(intData)) ;

					//TEMP HACK, need to update AJParser to encapsulate this

					/** Parse the service Introspection data and return a descriptive text string **/


					String nodeName ;


					AJParser parser = new AJParser(intData);

					// scan through the elements

					NodeList children = parser.getChildNodes();
					for (int i=0;i<children.getLength();i++) {
						Node node = children.item(i);
						if (Node.ELEMENT_NODE != node.getNodeType()) {
							continue; // skip anything that isn't an ELEMENT
						}

						nodeName = node.getNodeName();
						if ("interface".equals(nodeName)) {
							String iName = parser.parseNameAttr(node);
							String iDetails = parser.parseInterface(node, " ");
							Message m ;
							m = obtainMessage(UI_ADD_INTERFACE, iName+"|"+iDetails); // Don't know details, so send dummy entry
							mUIHandler.sendMessage(m);
						}
						else if ("node".equals(nodeName)) {

							String oName = parser.parseNameAttr(node);
							Message m ;
							m = obtainMessage(UI_ADD_OBJECT, oName); // Don't know details, so send dummy entry
							mUIHandler.sendMessage(m);
						}

					} // end for


				} catch (Throwable t) { // generic catch for any exceptions. Really should be more specific!
					showError(String.format("Exception during Introspect\n"+t.toString())) ;
				}
				break;
			} //end INTROSPECT

			default:
				break;
			}
		} //handleMessage

		public boolean usingDiscovery(){
			return this.mIsConnected;
		} //usingDiscovery

		
		
		
		
		/*
		 * SIGNAL HANDLERS
		 */

		BusListener mBusListener = new BusListener() {

			public void foundAdvertisedName(String name, short transport, String namePrefix) {
				Message msg ;


				Log.i(TAG, String.format("foundAdvertisedName activated for: "+name));
				msg = obtainMessage(CONNECT_WITH_REMOTE_BUS);
				sendMessage(msg);
			}

			// This handler is called when a LostAdvertisedName signal is received, indicating that an instance
			// of the service has gone away
			public void lostAdvertisedName(String name, short transport, String namePrefix) {
				Log.i(TAG, String.format("lostAdvertisedName activated for: "+name));
			}
		};


		/* 
		 * DISPLAY/LOGGING UTILITIES 
		 */

		private void showError(String msg) {
			Log.e(TAG, msg);
			Message tmsg = mUIHandler.obtainMessage(UI_ERROR);
			tmsg.obj = msg ;
			mUIHandler.sendMessage(tmsg);
		} //showError

		private void logStatus(String msg, Status status) {
			logStatus(msg, status, Status.OK);
		} //logStatus

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
		} //logStatus

		/*
		 * When an exception is thrown use this to Toast the name of the exception 
		 * and send a log of the exception to the Android log.
		 */
		private void logException(String msg, Exception ex) {
			String log = String.format("%s: %s", msg, ex);
			showError ("ERR: " + log);
			Log.e(TAG, log, ex);
		} //logException

	} //BusHandler


} //AJObject
