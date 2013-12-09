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
package org.alljoyn.whiteboard.activity;

import java.util.ArrayList;

public class DrawingLines {

	public static final float CANVAS_FLOAT_WIDTH = 1.0f;	
	public static final float CANVAS_FLOAT_HEIGHT = 1.0f;	
	public static final float REVIEW_SCALE = 0.25f;	
	public static final int PART_COUNT = 4;

	private ArrayList <ArrayList <Point> > mListOfLines;

	transient private int scaleWidth = 320;
	transient private int displayYOffset = 0;

	public DrawingLines() {
		ArrayList <Point> initCurve = new ArrayList<Point>();
		mListOfLines = new ArrayList<ArrayList <Point> >();
		mListOfLines.add(initCurve);
	}

	public ArrayList<ArrayList <Point> > getDraw(){ 
		return mListOfLines;
	}

	public void setDisplaySize(int x, int y){
		displayYOffset = (int) ((y - (scaleWidth * CANVAS_FLOAT_HEIGHT)) / 2);
	}

	public int getCanvasHeight(){		
		return (int) (scaleWidth  * CANVAS_FLOAT_HEIGHT);
	}

	public void addCurvePoint(int x, int y){
		Point p = new Point();
		p.set(x, y);
		if(mListOfLines.size() <= 0){
			ArrayList <Point> initCurve = new ArrayList<Point>();
			mListOfLines.add(initCurve);
		}
		mListOfLines.get(mListOfLines.size()-1).add(p);
	}

	public void addCurvePoint(Point p){
		mListOfLines.get(mListOfLines.size()-1).add(p);
	}

	public void createNewCurve(){
		ArrayList <Point> initCurve = new ArrayList<Point>();
		mListOfLines.add(initCurve);
	}

	public void addCurve(ArrayList<Point> ap){
		mListOfLines.add(ap);
	}

	public void removeLastCurve(){
		if(mListOfLines.size() > 0){
			mListOfLines.remove(mListOfLines.size() - 1);
		}
	}

	public class Point{
		/**
		 * 
		 */
		private float x;
		private float y;

		public Point(){

		}

		public void set(int x, int y){
			this.x  = x / (scaleWidth *  CANVAS_FLOAT_WIDTH) ;
			this.y =  (y - displayYOffset) / (scaleWidth * CANVAS_FLOAT_HEIGHT);
		}
		public int rx(int layer){
			int offset = (int)((1.0f - 1.0f*REVIEW_SCALE) / 2.0f * scaleWidth * CANVAS_FLOAT_WIDTH);
			return (int) ((x) * scaleWidth * CANVAS_FLOAT_WIDTH * REVIEW_SCALE) + offset;
		}

		public int ry(int layer){
			int offset = (int)((1.0f - 4.0f*REVIEW_SCALE + (2.0f*displayYOffset/scaleWidth) ) / 2.0f * scaleWidth * CANVAS_FLOAT_HEIGHT);
			return (int) ((y + layer) * scaleWidth * CANVAS_FLOAT_HEIGHT * REVIEW_SCALE) + offset ;
		}
		public int x(){
			return (int) (x * scaleWidth * CANVAS_FLOAT_WIDTH);
		}

		public int y(){
			return (int) (y * scaleWidth * CANVAS_FLOAT_HEIGHT) + displayYOffset;
		}
		public int topPartY(){
			return (int) ((y - 1)  * scaleWidth * CANVAS_FLOAT_HEIGHT) + displayYOffset;
		}
		public float rawX(){
			return x;
		}

		public float rawY(){
			return y ;
		}
	}

	public void setScaleWidth(int w) {
		scaleWidth = w;
	};

	public void clear()
	{
		mListOfLines = new ArrayList<ArrayList <Point> >();
	}
}
