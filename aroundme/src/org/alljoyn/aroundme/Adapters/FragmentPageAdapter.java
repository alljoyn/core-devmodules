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
package org.alljoyn.aroundme.Adapters;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.alljoyn.devmodules.common.Utilities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

public class FragmentPageAdapter extends FragmentStatePagerAdapter {

	private static final String       TAG = "FragmentPageAdapter";

	private HashMap<String,FragmentData>  mFragmentList=null;
	private ArrayList<String>             mFragmentIndex=null;
	private FragmentManager               mFragmentMgr=null;
	private Context                       mContext=null;
	private boolean                       mSort=false;

	/**
	 * Class to hold data associated with a fragment
	 */
	private class FragmentData{
		public String   title="";
		public String   classname="";
		public Bundle   args=null;
		public Fragment fragment=null;
	}

	/**
	 * Default constructor. Pages are NOT sorted by default
	 * @param fm The FragmentManager instance associated with this display
	 */
	public FragmentPageAdapter(FragmentManager fm) {
		super(fm);
		mFragmentMgr = fm;
		if (mFragmentList==null) { mFragmentList  = new HashMap<String,FragmentData>(); }
		if (mFragmentIndex==null){ mFragmentIndex = new ArrayList<String>(); }
		mFragmentList.clear();
		mFragmentIndex.clear();
		mSort = false; // do not sort by default
	}
	
	@Override
	public void notifyDataSetChanged() {
		for (int i = 0; i < mFragmentList.size(); i++){
			String key = mFragmentIndex.get(i);
			if (mFragmentList.get(key).fragment==null){
				if (mContext != null){
					mFragmentList.get(key).fragment = Fragment.instantiate(mContext, 
							mFragmentList.get(key).classname, 
							mFragmentList.get(key).args);
					// work around bug in nested fragments
					mFragmentMgr.beginTransaction().commitAllowingStateLoss();
				} else {
					Log.e(TAG, "getItem() Error: context is null!");
				}
			}
		}
		super.notifyDataSetChanged();
	}

	/**
	 * Constructor that allows you to specify whether the pages are sorted or not
	 * @param fm The FragmentManager instance associated with this display
	 * @param sort If true, the pages will be sorted alphabetically by title
	 */
	public FragmentPageAdapter(FragmentManager fm, boolean sort) {
		super(fm);
		mFragmentMgr = fm;
		if (mFragmentList==null) { mFragmentList  = new HashMap<String,FragmentData>(); }
		if (mFragmentIndex==null){ mFragmentIndex = new ArrayList<String>(); }
		mFragmentList.clear();
		mFragmentIndex.clear();
		mSort = sort; // set sort option
	}

	@Override
	/**
	 * return the item at a particular 'position'
	 * Note that this will instantiate the appropriate fragment if not already done
	 * (Android will sometimes destroy fragments that are not currently displayed)
	 */
	public Fragment getItem(int position) {
		Fragment fragment = null;
		if (position<=mFragmentList.size()){
			String key = mFragmentIndex.get(position);
			if (mFragmentList.get(key).fragment==null){
				if (mContext != null){
					mFragmentList.get(key).fragment = Fragment.instantiate(mContext, 
							mFragmentList.get(key).classname, 
							mFragmentList.get(key).args);
					// work around bug in nested fragments
					mFragmentMgr.beginTransaction().commitAllowingStateLoss();
				} else {
					Log.e(TAG, "getItem() Error: context is null!");
				}
			}
			fragment = mFragmentList.get(key).fragment;
		}
		return fragment;
	}

	@Override
	public int getCount() {
		int ret = 0;
		synchronized(mFragmentList) {
			ret = mFragmentList.size();
		}
		return ret;
	}

	@Override
	public CharSequence getPageTitle (int position){
		CharSequence title="";
		if (position<=mFragmentList.size()){
			title = mFragmentList.get(mFragmentIndex.get(position)).title;
		}
		return title;
	}

	// sort of a hack to force update of views
	public int getItemPosition(Object item) {
		return POSITION_NONE;
	}

	/**
	 * Returns the position of the key in the index (note: this changes)
	 * @param key The key to lookup
	 * @return The position in the index. -1 if not found
	 */
	public int getPosition (String key){
		int i = -1;
		if (mFragmentIndex.contains(key)){
			i = mFragmentIndex.indexOf(key);
		}
		return i;
	}


	/**
	 * Provides the context to use for managing fragments
	 * @param context The context to use for managing fragments
	 */
	public void setContext (Context context){
		mContext=context;
	}

	/**
	 * Checks whether the adapter already contains the fragment or not
	 * @param key the key identifying the fragment
	 * @return true if already present, otherwise false
	 */
	public boolean contains (String key){
		return mFragmentIndex.contains(key) ? true : false ;
	}

	/**
	 * Add a fragment to the list, with associated key
	 * @param key The key used to identify the fragment
	 * @param fragment The fragment for the supplied key
	 */
	public synchronized void add (String key, String title, String classname, Bundle args){
		add(key, title, classname, args, false);
	}
	
	public synchronized void add (String key, String title, String classname, Bundle args, boolean notifyDataSetChanged){
		synchronized(mFragmentList) {
			if (!mFragmentIndex.contains(key)){
				mFragmentIndex.add(key);
				if (mSort) Collections.sort(mFragmentIndex);
				FragmentData fd = new FragmentData();
				fd.title = title;
				fd.classname = classname;
				fd.args = args;
				mFragmentList.put(key, fd);
				Log.v(TAG, "add fragment: "+key+" ("+title+")");
				if (args!=null) Log.v(TAG, "Args: "+args.toString());
				if(notifyDataSetChanged)
					notifyDataSetChanged();
			} else {
				Log.w(TAG, "add("+key+") Fragment already present, ignoring");
			}
		}
	}

	/**
	 * Remove a fragment from the list
	 * @param key the key associated with the fragment
	 */
	public synchronized void remove (String key){
		synchronized(mFragmentList) {
			if (mFragmentIndex.contains(key)){
				mFragmentIndex.remove(key);
				if (mSort) Collections.sort(mFragmentIndex);
				mFragmentList.remove(key);
				Log.v(TAG, "remove fragment: "+key);
			} else {
				Log.w(TAG, "remove("+key+") Fragment not present, ignoring");
			}
		}
	}
} // FragmentPageAdapter
