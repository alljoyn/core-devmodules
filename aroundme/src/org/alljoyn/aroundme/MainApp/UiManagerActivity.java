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


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Chat.ChatPagerFragment;
import org.alljoyn.aroundme.Debug.DebugFunctionsFragment;
import org.alljoyn.aroundme.Groups.GroupListFragment;
import org.alljoyn.aroundme.Peers.NearbyUsersPagerFragment;
import org.alljoyn.aroundme.Peers.PeerDetailsFragment;
import org.alljoyn.aroundme.Peers.PeerFunctionsMasterFragment;
import org.alljoyn.aroundme.RemoteMedia.RemoteDebugFragment;
import org.alljoyn.aroundme.Transactions.TransactionsPagerFragment;
import org.alljoyn.aroundme.Whiteboard.WhiteboardPagerFragment;
import org.alljoyn.profileloader.MyProfileData;
import org.alljoyn.devmodules.APICoreImpl;


import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;



/*
 * Activity to start the header fragment and main display and handle navigation.
 * Note that there two ways to navigate that are both handled here (hence the apparent duplication): 
 *   - select the function from a drop down list in the action bar
 *   - issuing intents from within Activities and fragments
 */
public class UiManagerActivity extends FragmentActivity implements TaskControlInterface {


	private static final String TAG = "UiManagerActivity";

	private static HeaderFragment  mHeaderFragment;
	private static Fragment        mDisplayFragment;
	private static FragmentManager mFragmentManager;
	private static Menu            mMenu;
	private static boolean         mSetup = false;
	private static ActionBar       mActionBar = null;
	private static Context         mContext;
	private static ArrayList<Integer> mNavigationStack = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		if (!mSetup){
			mSetup = true;

			setContentView(R.layout.main_layout);

			mActionBar = getActionBar();
			mContext = getBaseContext();
			mFragmentManager = getSupportFragmentManager();

			mNavigationStack = new ArrayList<Integer>();

			// check that framing is set up and create the display sections
			if (findViewById(R.id.display_frame) != null) {
				setupActionList();
				initHeader();
				initDisplay();	
			} else {
				Log.e(TAG, "onCreate() Error: display frame not found");
				finish();
			}
		}
	} // onCreate


	@Override
	protected void onDestroy() {
		super.onDestroy();
		finish();
		try {
			APICoreImpl.StopAllJoynServices(this);
		} catch (Exception e1) {
			Log.e(TAG, "Error starting Framework Services: "+e1.toString());
		}
	} //onDestroy



	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.remove("android:support:fragments");
		invokeFragmentManagerNoteStateNotSaved(); 
	}

	// Fragments 'leak' state using the sherlock library, so this is a workaround of sorts
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void invokeFragmentManagerNoteStateNotSaved() {
		/**
		 * For post-Honeycomb devices
		 * */
		if (Build.VERSION.SDK_INT < 11) {
			return;
		}
		try {
			Class cls = getClass();
			do {
				cls = cls.getSuperclass();
			} while (!"Activity".equals(cls.getSimpleName()));
			Field fragmentMgrField = cls.getDeclaredField("mFragments");
			fragmentMgrField.setAccessible(true);

			Object fragmentMgr = fragmentMgrField.get(this);
			cls = fragmentMgr.getClass();

			Method noteStateNotSavedMethod = cls.getDeclaredMethod("noteStateNotSaved", new Class[] {});
			noteStateNotSavedMethod.invoke(fragmentMgr, new Object[] {});
			Log.d(TAG, "DLOutState() Successful call for noteStateNotSaved!!!");
		} catch (Exception ex) {
			Log.e("DLOutState", "Exception on FM.noteStateNotSaved", ex);
		}
	}
	///////////////////////////////////////////////////////////////
	// Dropdown Navigation Handling
	///////////////////////////////////////////////////////////////

	// The list of available (main) functions is accessible through a dropdown list in the Action Bar
	// Make sure the String and the constants match in order!
	// NOTE that list includes only those fragments that are 'standalone', i.e. do not operate
	// on a specific user or supply information to a main fragment

	class MenuEntry{
		int     index;
		int     actionId;
		String  actionText;

		MenuEntry(int index, int actionId, String actionText){
			this.index = index;
			this.actionId = actionId;
			this.actionText = actionText;
		}
	}

	private MenuEntry[] mActionList = { 
			new MenuEntry(0, TaskControlInterface.Functions.HOME,            "MAIN PAGE"),
			new MenuEntry(1, TaskControlInterface.Functions.NEARBY_USERS,    "NEARBY USERS"),
			new MenuEntry(2, TaskControlInterface.Functions.MANAGE_GROUPS,   "MANAGE GROUPS"),
			new MenuEntry(3, TaskControlInterface.Functions.DEBUG_FUNCTIONS, "DEBUG"),
			new MenuEntry(4, TaskControlInterface.Functions.TRANSACTIONS,    "TRANSACTIONS"),
			new MenuEntry(5, TaskControlInterface.Functions.CHAT,            "CHAT/IM"),
			new MenuEntry(6, TaskControlInterface.Functions.NOTIFICATIONS,   "NOTIFICATIONS"),
			new MenuEntry(7, TaskControlInterface.Functions.WHITEBOARD,      "WHITEBOARD")
	};


	/**
	 *  routine to set up navigation via a drop-down list on the Action Bar
	 */
	private static String[] mSpinnerList = null;
	private static ArrayAdapter<String> mSpinnerAdapter=null;

	private void setupActionList(){

		// populate the spinner for the dropdown navigation menu
		mSpinnerList = new String[mActionList.length];
		Log.v(TAG, "setupActionList() Adding "+mActionList.length+" items to dropdown menu");
		for (int i=0; i<mActionList.length; i++){
			mSpinnerList[i] = mActionList[i].actionText;
		}
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		mSpinnerAdapter = new ArrayAdapter<String>(mContext, 
				android.R.layout.simple_spinner_dropdown_item, 
				mSpinnerList);
		mActionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationCallback);
	}


	/**
	 *  the callback for when an item is selected from the dropdown list
	 */
	private ActionBar.OnNavigationListener mNavigationCallback = new ActionBar.OnNavigationListener(){
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			boolean result = true;
			try{
				if ((itemPosition>=0) && (itemPosition<mActionList.length)){
					Log.v(TAG, "Navigation Action: "+mActionList[itemPosition].actionText+" ("+itemPosition+")");

					startFunction(mActionList[itemPosition].actionId, null);
				}
			} catch (Exception e){
				Log.e(TAG, "onNavigationItemSelected("+itemPosition+") exception: "+e.toString());
				e.printStackTrace();
			}
			return result;
		}
	};



	/**
	 * Builds an Intent to launch an activity
	 * @param activity the postfix identifier of the Intent to issue
	 */
	void launchActivity(String activity) {
		// Launch a Service-specific Activity, setting the service name as a parameter
		Intent intent = new Intent();
		String service = MyProfileData.getSvcName();
		intent.setAction(AppConstants.INTENT_PREFIX + "." + activity);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".service", service);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".details.name",    service);
		try {
			startActivity(intent);
		} catch (Throwable t){
			Toast.makeText(mContext, 
					"Error starting " + activity + " Activity for service: " + service, 
					Toast.LENGTH_SHORT).show();
		}
	}


	/**
	 * Builds an Intent to launch an activity, with the group name as parameter
	 * @param activity the postfix identifier of the Intent to issue
	 * @param group the name of the group to process
	 */
	void launchGroupActivity(String activity, String group) {
		// Launch a Service-specific Activity, setting the group name as a parameter
		Intent intent = new Intent();
		intent.setAction(AppConstants.INTENT_PREFIX + "." + activity);
		intent.putExtra(AppConstants.INTENT_PREFIX + ".group", group);
		try {
			startActivity(intent);
		} catch (Exception e){
			Toast.makeText(this, 
					"Error starting " + activity + " Activity for group: " + group, 
					Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Launch error: "+e.toString());
		}

	}

	///////////////////////////////////////////////////////////////
	// Default key/Menu handling
	///////////////////////////////////////////////////////////////


	@Override
	public void onBackPressed() {
		// If not the main page then pop the displayed fragment
		//int count = mFragmentManager.getBackStackEntryCount();
		int count = mNavigationStack.size();
		if (count > 1){
			mActionBar.setSelectedNavigationItem(mNavigationStack.get(count-2));
			mNavigationStack.remove(count-1);
			Log.v(TAG, "Stack: "+mNavigationStack.toString());
		}
		return;
	}

	
	/* Menu Options setup and processing */
	/* Called when the menu button is pressed. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.simplemenu, menu);
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

		case R.id.quit:{
			APICoreImpl.StopAllJoynServices(this); // for debug, remove later
			onDestroy();
			finish();
			return true;
		}
		default:{
			return super.onOptionsItemSelected(item);
		}
		}
	}//onOptionsItemSelected

	////////////////////////////////////////////////////////////////////
	// Fragment handling and utilities
	////////////////////////////////////////////////////////////////////

	/**
	 * Utility to launch the supplied fragment in the display frame
	 * @param fragment The fragment to start
	 * @param bundle Arguments to pass to the fragment. Set to null if not arguments are needed
	 */
	private void startFragment(Fragment fragment, Bundle bundle){
		try{
			mDisplayFragment = fragment;
			if (bundle!=null){
				fragment.setArguments(bundle);
			}
			FragmentTransaction transaction = mFragmentManager.beginTransaction();
			transaction.replace(R.id.display_frame, fragment);
			//transaction.addToBackStack(null);
			transaction.setTransition(FragmentTransaction.TRANSIT_ENTER_MASK);
			transaction.commitAllowingStateLoss();
		} catch (Exception e){
			Log.e(TAG, "Exception starting fragment: "+e.toString());
		}
	}

	/**
	 * Initialise the Header Fragment
	 */
	private void initHeader(){
		mHeaderFragment = new HeaderFragment();
		getSupportFragmentManager().beginTransaction().replace(R.id.header_frame, mHeaderFragment).commit();
	}



	/**
	 * Initialise the Main Display Fragment
	 */
	private void initDisplay(){

		startFunction(TaskControlInterface.Functions.HOME, null);	
	}

	/**
	 * Display the list of nearby users
	 */
	private void showNearbyUsers(){
		startFunction(TaskControlInterface.Functions.NEARBY_USERS, null);	
	}


	/**
	 * Returns a Fragment instance corresponding to logical "id" of the function
	 * @param id the identifier (int) associated with the function
	 * @return an instance of the corresponding fragment type, or null if unknown or not handled (yet)
	 */
	private Fragment getFragmentById (int id){

		Fragment fragment = null;
		try{

			// Start the appropriate fragment based on the event
			switch (id){
			case TaskControlInterface.Functions.HOME: {
				fragment = new FrontPageFragment();
				break;
			}
			case TaskControlInterface.Functions.ABOUT: {
				fragment = null; //TODO
				break;
			}
			case TaskControlInterface.Functions.SETTINGS: {
				fragment = null; //TODO
				break;
			}
			case TaskControlInterface.Functions.NEARBY_USERS: {
				fragment = new NearbyUsersPagerFragment();
				break;
			}
			case TaskControlInterface.Functions.USER_DETAILS: {
				fragment = new PeerDetailsFragment();
				break;
			}
			case TaskControlInterface.Functions.USER_SPECIFIC_FUNCTIONS: {
				fragment = new PeerFunctionsMasterFragment();
				break;
			}
			case TaskControlInterface.Functions.DEBUG_FUNCTIONS: {
				fragment = new DebugFunctionsFragment();
				break;
			}

			case TaskControlInterface.Functions.TRANSACTIONS: {
				fragment = new TransactionsPagerFragment();
				break;
			}
			case TaskControlInterface.Functions.MANAGE_GROUPS: {
				fragment = new GroupListFragment();
				break;
			}

			case TaskControlInterface.Functions.CHAT: {
				//fragment = new org.alljoyn.chat.activity.UIFragment();
				fragment = new ChatPagerFragment();
				break;
			}
			case TaskControlInterface.Functions.NOTIFICATIONS: {
				fragment = new org.alljoyn.notify.activity.UIFragment();
				break;
			}
			case TaskControlInterface.Functions.WHITEBOARD: {
				//fragment = new org.alljoyn.whiteboard.activity.UIFragment();
				fragment = new WhiteboardPagerFragment();;
				break;
			}
			default:{
				Log.e(TAG, "Unknown/unhandled function type: "+id);
				break;
			}
			}
		} catch (Exception e){
			Log.e (TAG, "Exception in getFragmentById("+id+"): "+e.toString());
		}			
		return fragment;
	}

	/**
	 * The interface provided for fragments to call into
	 * Mainly used to initiate other fragments or pass data around
	 */
	TaskControlInterface mTaskControlInterface; // need an instance
	@Override
	public void startFunction(int function, Bundle args) {
		try{
			int index = 0;
			Fragment fragment = getFragmentById(function);

			if (fragment != null){
				
				mDisplayFragment = fragment;
				startFragment (mDisplayFragment, args);

				//scan through list, don't assume index matches function id
				boolean found = false;
				for (int i=0; (i<mActionList.length)&&(!found); i++){
					if (mActionList[i].actionId==function){
						found = true;
						index = i;
					}
				}
				if (found){
					mActionBar.setSelectedNavigationItem(index);
					int count = mNavigationStack.size();
					// Update stack unless this function is already at the top
					if ((count==0) || (mNavigationStack.get(count-1)!=index)){
						mNavigationStack.add(index);
					}
					Log.v(TAG, "startFunction() Stack: "+mNavigationStack.toString());
				} else{
					Log.w(TAG, "startFunction(). Function not found: "+function);
				}
			} else {
				Log.w(TAG, "No fragment to handle requested function ("+function+")");
			}
		} catch (Exception e){
			Log.e (TAG, "startFunction("+function+") exception: "+e.toString());
		}
	}


} // UiManagerActivity
