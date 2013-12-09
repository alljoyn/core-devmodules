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

package org.alljoyn.profileloader;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.alljoyn.devmodules.api.profilemanager.ProfileManagerAPI;
import org.alljoyn.devmodules.common.ProfileDescriptor;
import org.alljoyn.devmodules.common.Utilities;
import org.alljoyn.storage.ProfileCache;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


/*
 * Simple  application that will either just retiree the stored URI or launch the contact picker
 * an save the results that it returns
 */
public class ProfileLoaderActivity extends Activity {

	private Menu                 mMenu;

	private static final String TAG = "Profile";

	private static final String profileIntro = 
		"\nTo use this app, you need to first identify a contact that will be used as your identity." + 
		"It does not have to really be you, it can be any contact. The name, number, photo etc." +
		"will be taken from the contact entry.\n" + 
		"Click OK to display the list of contacts on your device.\n\n"+
		"Select the one you want to use as a Profile\n";

	SharedPreferences   mSettings ;
	Uri                 mURI ;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);



		//get the flag that indicates processing to be done
		//valid values are: create, modify, load
		// create: nothing set, get user to select contact, save and display
		// modify: already set, allow user to change
		// load:   just load data and populate internal structures, do not display anything

		Intent myIntent = getIntent();
		String action = myIntent.getStringExtra("action");

		if ((action==null)){
			Log.e(TAG, "Error: Missing parameter to PROFILE Activity");
			finish();
		}

		if((action.equals("load"))){

			//just load data without any UI and exit this activity
			String uriStr = null;
			try {
				uriStr = ProfileManagerAPI.GetMyProfile().getField("contactid");
			} catch (Exception e) {
				e.printStackTrace();
			}

			Uri uri = Uri.parse(uriStr);
			finish();
		} else{

			// popup a quick set of instructions

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(profileIntro)
			.setCancelable(false)
			.setIcon(R.drawable.icon)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					//Start the contact picker activity
					try {
						// start Activity to choose a contact
						// return is handled in onActivityResult
						Intent intent = new Intent (Intent.ACTION_PICK, Contacts.CONTENT_URI);
						startActivityForResult (intent, 0);
					} catch (Exception e){
						Log.e(TAG, "Error getting profile");
						finish();
					}
				} //onClick
			});
			AlertDialog alert = builder.create();
			alert.show();

		}


	} // onCreate


	@Override
	protected void onDestroy() {
		super.onDestroy();
		finish();
	} //onDestroy


	//  this method is called when the contact picker activity returns
	@Override  
	public void onActivityResult(int reqCode, int resultCode, Intent intent) {  
		super.onActivityResult(reqCode, resultCode, intent);  

		Uri contactURI ;


		if (resultCode == Activity.RESULT_OK) {  
			// Contact picker returns the URI, so populate from that

			contactURI = intent.getData();
			try {
				ProfileManagerAPI.GetMyProfile().setField("contactid", contactURI.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
			ProfileCache.saveContactId(contactURI.toString());
			Utilities.logMessage(TAG, "myURI saved: "+ contactURI.toString());
		} else {
			Log.e (TAG, "Activity returned with error code: "+resultCode);
		}
		
		// Exit activity
		finish();


	} //onActivityResult



	/* Menu Options setup and processing */
	/* Called when the menu button is pressed. */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.profilemenu, menu);
		this.mMenu = menu;
		return true;
	}


} // ProfileLoader
