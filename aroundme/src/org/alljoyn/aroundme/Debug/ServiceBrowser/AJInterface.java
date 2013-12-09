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


import org.alljoyn.aroundme.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


/*
 * Simple  application that will display the details of an Interface
 */
public class AJInterface extends Activity {


	private TextView mTitleView ;
	private TextView mDescriptionView ;
	private String   mInterfaceName ;
	private String   mDescription ;
    private ArrayAdapter<String> mListViewAdapter;

	
	
	private static final String TAG = "AJInterface";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);

		// layout the UI
		setContentView(R.layout.ajinterface);

		//TODO: split up interface description into methods, signals and properties, display separately
		
		// get the title views
		mTitleView        = (TextView) findViewById(R.id.interfaceTitle);
		mDescriptionView  = (TextView) findViewById(R.id.interfaceDescription);
	
		//get the name and description of the interface
		Intent myIntent = getIntent();
		mInterfaceName = myIntent.getStringExtra(BrowserConstants.BROWSER_PREFIX+".name");
		mDescription   = myIntent.getStringExtra(BrowserConstants.BROWSER_PREFIX+".description");
		
		//set the text views to the retrieved items
		mTitleView.setText(mInterfaceName);
		mDescriptionView.setVerticalScrollBarEnabled(true);
		mDescriptionView.setText(mDescription);
		
	} // onCreate

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		finish();
	} //onDestroy

} // AJInterface
