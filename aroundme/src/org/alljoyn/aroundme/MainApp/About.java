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





import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;



/*
 * Simple  application that will display the About dialog
 * Not really much to do except setup up the UI and handle the Quit menu option
 */
public class About extends Activity {

	private Menu                 mMenu;

	private static final String TAG = "About";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);

		// layout the UI
		setContentView(R.layout.about);
		
	} // onCreate

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		finish();
	} //onDestroy



} // About
