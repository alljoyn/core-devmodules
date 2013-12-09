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
package org.alljoyn.aroundme.Transactions;




import java.io.File;

import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.ApprovalAdapter;
import org.alljoyn.aroundme.Adapters.TransactionAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.devmodules.api.mediaquery.MediaQueryAPI;
import org.alljoyn.devmodules.api.mediaquery.MediaQueryListener;
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.common.MediaUtilities;
import org.alljoyn.devmodules.common.TransactionListDescriptor;
import org.alljoyn.storage.MediaTransactionCache;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;




/**
 * Fragment to list the received transactions (files), and allow user to accept or reject.
 * If accepted, the files are copied to their expected location on the SD card and registered with the 
 * Android framework so that they become visible in the normal media tools
 * 
 * @author pprice
 *
 */

public class TransactionsReceivedFragment extends ListFragment {



	private static final String TAG = "TransactionsReceivedFragment";

	private ApprovalAdapter           mAdapter; 

	private static Button             acceptButton;
	private static Button             rejectButton;
	private static ListView           mListView ;

	private static Context            mContext;
	private static Activity           mActivity;

	private boolean                   mReady = false;

	private View                      mDisplayView = null;


	/* Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity().getBaseContext();
		mActivity = getActivity();
		mReady = true;

	}



	// Called when Fragment is attached to a View on the display
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		try{
			if (mReady){
				// Layout the overall screen
				mDisplayView = inflater.inflate(R.layout.approvallist, container, false);


				// Load the transaction list and set up the adapter
				mAdapter = new ApprovalAdapter(mContext); 
				mAdapter.setTransactionType(MediaTransactionCache.RECEIVED);

				// Set up list for multiple selection
				mListView = (ListView) mDisplayView.findViewById(android.R.id.list);

				//mListView = getListView();// doesn't work for fragments
				mListView.setItemsCanFocus(false);
				mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
				mListView.setAdapter(mAdapter);

				// create the buttons (Note: cannot register listeners here)
				acceptButton = (Button) mDisplayView.findViewById(R.id.acceptButton);
				rejectButton = (Button) mDisplayView.findViewById(R.id.rejectButton);

			} else {
				Log.e(TAG, "onCreateView() Not ready...");
			}
		} catch (Exception e){
			Log.e(TAG, "onCreateView() exception: "+e.toString());
			e.printStackTrace();
		}
		return mDisplayView;
	} //onCreateView


	// cannot set click listeners in onCreateView, have to do it in onViewCreated instead
	// Warning: do not use the View parameter if using the compatibility library
	@Override
	public void onViewCreated (View view, Bundle savedInstanceState){

		// Set up click listeners for Approve/Reject Buttons
		// Note: can't seem to set this up in a separate variable, have to create dynamically
		// because this routine is called before normal initialisation takes place
		
		acceptButton.setOnClickListener (mAcceptListener);
		
		rejectButton.setOnClickListener (mRejectListener);

		// Listen for incoming files
		MediaQueryAPI.RegisterListener(mMQListener);


	}
	
	/* Called when the activity is exited. */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}


	////////////////////////////////////////////////////////
	// Listeners
	////////////////////////////////////////////////////////

	// Handler for Accept button
	private OnClickListener mAcceptListener = new OnClickListener() {
		public void onClick(View v) {

			try{
				// Register any checked items
				for (int i=0; i<mAdapter.getCount(); i++){
					if (mAdapter.isChecked(i)){

						MediaIdentifier mi = new MediaIdentifier();
						mi = mAdapter.get(i);

						// register media and move to approved list
						String oldpath = mAdapter.getPath(i);

						// Move the file to a more 'standard' directory and remove the prefix
						String newname = mi.name;
						// remove path prefix (if any)
						if (newname.contains("/")){
							newname = newname.substring(newname.lastIndexOf("/")+1);
						}
						// remove userid prefix (if any)
						if (newname.contains("_")){
							newname = newname.substring(newname.indexOf("_")+1);
						}

						// get new location basd on media type
						String newloc = MediaUtilities.getStorageLocation(mi.mediatype);

						// modify the fields to reflect the new location and name
						// if userid is not in the string, then it's not a filepath, so leave it alone
						if (mi.title.contains(mi.userid)){
							mi.title = newname;
						}
						if (mi.name.contains(mi.userid)){
							mi.name = newname;
						}
						mi.localpath = newloc + "/" + newname;


						// move the file
						File f1 = new File(oldpath);
						File f2 = new File(mi.localpath);
						f1.renameTo(f2);
						Log.v(TAG, "Accept() Moved file from: "+oldpath+" to:"+mi.localpath);

						// Register the media file with the system, so that it appears in standard browsing tools (e.g. Photo Gallery)

						MediaUtilities.registerMedia(mContext, mi);

						// Move the item to the approved list
						MediaTransactionCache.remove(MediaTransactionCache.RECEIVED, oldpath);
						MediaTransactionCache.saveMediaTransaction(MediaTransactionCache.APPROVED, mi.localpath, mi);

					}// if checked
				}//for

				// Remove selected items
				mAdapter.removeCheckedItems();

			} catch (Exception e){
				Log.e(TAG, "Error processing Accepted files: "+e.toString());
			}
		}
	};


	// Handler for Reject button
	private OnClickListener mRejectListener = new OnClickListener() {
		public void onClick(View v) {
			try{
				// Scan through list and process checked items
				for (int i=0; i<mAdapter.getCount(); i++){
					if (mAdapter.isChecked(i)){
						String path = mAdapter.getPath(i);
						Log.d(TAG, "Reject item: "+path);

						// move to rejected list
						MediaIdentifier mi = new MediaIdentifier();
						mi = mAdapter.get(i);
						MediaTransactionCache.remove(MediaTransactionCache.RECEIVED, path);
						MediaTransactionCache.saveMediaTransaction(MediaTransactionCache.REJECTED, path, mi);

						// delete media
						File f = new File(path);
						if (f.exists()){
							f.delete();
						}
					}
				}//for

				// Remove selected items
				mAdapter.removeCheckedItems();

			} catch (Exception e){
				Log.e(TAG, "Error processing Rejected files: "+e.toString());
			}

		}
	};

	
	// Listener for MediaQuery events
	private MediaQueryListener mMQListener = new MediaQueryListener() {
		@Override
		public void onMediaQueryServiceAvailable(String service) { }

		@Override
		public void onMediaQueryServiceLost(String service) { }

		@Override
		public void onQueryComplete(String service, String mtype) { }

		@Override
		public void onItemAvailable(String service, MediaIdentifier item) { }

		@Override
		public void onTransferComplete(String service, String path, String mtype, String localpath) {
			// Reset the list
			mActivity.runOnUiThread(new Runnable(){
				public void run(){
					Log.v(TAG, "File transfer detected, updating list");
					mAdapter.setTransactionType(MediaTransactionCache.RECEIVED);
					mAdapter.notifyDataSetChanged();
					Toast.makeText(mContext, "File received, pending approval", Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public void onTransferError(int transaction, String service, String mtype, String path) { }

	};
	
} // end of Activity
