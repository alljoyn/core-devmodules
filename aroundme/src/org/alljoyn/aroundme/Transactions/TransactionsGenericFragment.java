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




import org.alljoyn.aroundme.R;
import org.alljoyn.aroundme.Adapters.TransactionAdapter;
import org.alljoyn.aroundme.MainApp.AppConstants;
import org.alljoyn.devmodules.common.TransactionListDescriptor;
import org.alljoyn.storage.MediaTransactionCache;
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




//Activity to list generic transactions. Type of list is passed in the Intent

public class TransactionsGenericFragment extends ListFragment {



	private static final String TAG = "TransactionsGenericActivity";

	private TransactionAdapter        mAdapter; 
	private TransactionListDescriptor mTransactionList;

	private ListView                  mListView ;
	private Context                   mContext;
	private String                    mTransType;
	private Button                    mClearButton;
	private TextView                  mTextView;
	private String                    mTitle;
	private boolean                   mReady = false;

	private View                      mDisplayView = null;


	/* Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = getActivity();

		// Process the arguments
		Bundle args = getArguments();

		if (args!=null){
			Log.v(TAG, "Args: "+args.toString());
			
			// handle the ProfileID (unique id) name and look up associated data
			if (args.containsKey(AppConstants.TRANSACTION)) {
				mTransType = args.getString(AppConstants.TRANSACTION);
				mReady = true;
			} else {
				Log.e(TAG, "No Transaction Type specified!!! Quitting...");
				mReady = false;
				getActivity().getSupportFragmentManager().popBackStack();
			}

		} else {
			Log.e(TAG, "No args provided! Quitting...");
			mReady = false;
			getActivity().getSupportFragmentManager().popBackStack();
		}
	}



	// Called when Fragment is attached to a View on the display
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		if (mReady){
			// Layout the overall screen
			mDisplayView = inflater.inflate(R.layout.translist, container, false);


			// Set up Title
			mTitle = "Files: " + mTransType;
			mTextView = (TextView)mDisplayView.findViewById(R.id.serviceTitle);
			mTextView.setText(mTitle);
			
			// Set up Clear button
			mClearButton = (Button) mDisplayView.findViewById(R.id.clearButton);
			mClearButton.setOnClickListener (mClearListener);

			// Load the transaction list and set up the adapter
			mAdapter = new TransactionAdapter(mContext); 
			mAdapter.setTransactionType(mTransType);
			setListAdapter(mAdapter); 

			// That's it, ListView handles scrolling etc.

		} else {
			Log.e(TAG, "onCreateView() Not ready...");
		}
		return mDisplayView;
	} //onCreateView


	/* Called when the activity is exited. */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}


	// Handler for Clear button
	private OnClickListener mClearListener = new OnClickListener() {
		public void onClick(View v) {
			try{
				Log.d(TAG, "Clearing items from "+mTransType+" list");
				// Scan through list and process checked items
				for (int i=0; i<mAdapter.getCount(); i++){
					String path = mAdapter.getPath(i);
					Log.d(TAG, "Reject item: "+path);
					mAdapter.remove(i);
					
					// delete transaction
					MediaTransactionCache.remove(mTransType, path);

				}//for

			} catch (Exception e){
				Log.e(TAG, "Error processing Rejected files: "+e.toString());
			}

		}
	};

} // end of Activity
