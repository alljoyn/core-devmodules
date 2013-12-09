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
package org.alljoyn.aroundme.RemoteMedia;


import java.io.File;
import java.util.List;


import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.MediaAdapter;
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaTypes;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;


// Simple debug display based on data held in the RemoteDebugListManager for the specified service
//public class AppSelectActivity extends ListActivity {
public class AppSelectActivity extends Activity {



	private static final String TAG = "AppSelectActivity";

	private  MediaAdapter mAdapter; 
	private  String       mTitle="";
	private  GridView     mGridview;
	private  Context      mContext;
	private  Uri          mSelectedUri;
	private  Intent       mIntent ;

	/* Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.grid);

		mContext = this ;
		mIntent = new Intent();

//		mTitle = "Select Application" ;
//		TextView   titleView;
//		titleView = (TextView)  findViewById(R.id.title);
//		titleView.setText(mTitle);


		// Set up list adapter for list display
		mAdapter = new MediaAdapter(getApplicationContext());
		mAdapter.setContext(this); 
		mAdapter.setDefaultIcon(R.drawable.ic_list_applications);
		mAdapter.clear();
		
		// Get the list of apps and populate adapter
		loadAppList();

		mGridview = (GridView) findViewById(R.id.gridview);
		mGridview.setAdapter(mAdapter);
		mGridview.setOnItemClickListener(mClickListener);
		

		// nothing else to do, ListAdapter handle update of display etc.
	}


	/* Called when the activity is exited. */
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

/****
	// Override Back key and set up result before returning
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		return;
	}
	****/


	/////////////////////////////////////////////////////////////////////////////////
	// Click Listener(s)
	/////////////////////////////////////////////////////////////////////////////////


	private OnItemClickListener mClickListener = new OnItemClickListener(){
		public void onItemClick(AdapterView parent, View v, int position, long id) 
		{   
			MediaIdentifier mi = mAdapter.get(position);
			String appfile = mi.localpath;
			//String appfile = mi.name; // need to specify package name, not actual file
			//mSelectedUri = Uri.parse("file://" + appfile);
			mSelectedUri = Uri.fromFile(new File(appfile));
			Log.v(TAG, "file selected: "+mSelectedUri+"("+mi.name+")");
			
			// set up the return value
			mIntent.setData(mSelectedUri); 
			setResult(RESULT_OK, mIntent) ;
			
			finish();
		}//onItemClick
	};



	/////////////////////////////////////////////////////////////////////////////////
	//  Load the data
	/////////////////////////////////////////////////////////////////////////////////

	private void loadAppList(){
		
		// Scan through the list of installed packages
		PackageManager pm = mContext.getPackageManager();
		List<PackageInfo> packs = pm.getInstalledPackages(0);
		for(int i=0;i<packs.size();i++) {
			PackageInfo p = packs.get(i);

			// ignore system packages
			if (p.versionName != null){
				String loc = p.applicationInfo.publicSourceDir;

				if (!loc.contains("/system")){

					MediaIdentifier mi = new MediaIdentifier();
					mi.title = p.applicationInfo.loadLabel(pm).toString();
					mi.name  = p.packageName;
					mi.path  = p.applicationInfo.publicSourceDir;
					mi.type  = "application/*";
					mi.size  = 0;
					mi.thumbpath = mi.name;
					mi.mediatype = MediaTypes.APP;
					mi.localpath = p.applicationInfo.publicSourceDir;

					Log.v(TAG, "Found: "+mi.path+"("+mi.name+", "+mi.title+", "+mi.thumbpath+")");

					mAdapter.add(mi);
				}
			}
		}  //for

	}
} // end of Activity
