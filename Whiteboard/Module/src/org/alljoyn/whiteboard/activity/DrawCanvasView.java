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
import java.util.HashMap;

import org.alljoyn.devmodules.common.WhiteboardLineInfo;
import org.alljoyn.whiteboard.api.WhiteboardAPI;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;

public class DrawCanvasView extends View {
	
	private String groupId = "";
	
	private HashMap<String, Integer> colorMap;
	private int currentColor;
		
	private DrawingLines drawingLines;
    public DrawingLines getDrawingLines() {
		return drawingLines;
	}

	public void setDrawingLines(DrawingLines drawingLines) {
		this.drawingLines = drawingLines;
	}

	private Bitmap mBitmap;
    private Canvas mCanvas;
    private final Rect mRect = new Rect();
    private final Paint mPaint;
    private float mCurX = -1;
    private float mCurY = -1;
	
	private DrawHandler drawHandler;

    public DrawCanvasView(Context c) {
        super(c);
        setFocusable(true);
        colorMap = new HashMap<String, Integer>();
        currentColor = 0;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setARGB(0xff, 0, 0, 0);
        
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        
        drawHandler = new DrawHandler();
        
        drawingLines = new DrawingLines();
    } 
    
    public DrawHandler getDrawHandler()
    {
    	return drawHandler;
    }
    
    public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

    @Override 
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int curW = mBitmap != null ? mBitmap.getWidth() : 0;
        int curH = mBitmap != null ? mBitmap.getHeight() : 0;
        if (curW >= w && curH >= h) {
            return;
        }

        if (curW < w) curW = w;
        if (curH < h) curH = h;

        Bitmap newBitmap = Bitmap.createBitmap(curW, curH, Bitmap.Config.RGB_565);
        Canvas newCanvas = new Canvas();
        newCanvas.setBitmap(newBitmap);
        if (mBitmap != null) {
            newCanvas.drawBitmap(mBitmap, 0, 0, null);
        }

        mBitmap = newBitmap;
        mCanvas = newCanvas;
        
    	int lineWidth = (mCanvas.getWidth() / 320);
        
        mPaint.setStrokeWidth(lineWidth);
        
        if(drawingLines != null){
        	drawingLines.setScaleWidth(w); 
        	drawingLines.setDisplaySize(w, h);
        }
        
        clear();
    }

    @Override 
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }

    @Override 
    public boolean onTouchEvent(MotionEvent event) {
    	int viewWidth = getWidth();
    	int viewHeight = getHeight();
        float prevX = -1.0f;
        float prevY = -1.0f;
    	int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {

            mCurX = event.getX(0);
            mCurY = event.getY(0);
           
            if(prevX >= 0 && prevY > 0 && mCurX >= 0 && mCurY > 0)
            {
            	drawLine(Color.BLACK, prevX, prevY, mCurX, mCurY, event.getPressure(0), 4);
            	WhiteboardLineInfo line = new WhiteboardLineInfo();
            	line.x1 = prevX / ((float)viewWidth);
            	line.y1 = prevY / ((float)viewHeight);
            	line.x2 = mCurX / ((float)viewWidth);
            	line.y2 = mCurY / ((float)viewHeight);
            	line.pressure = event.getPressure(0);
            	line.width = 4;
            	line.action = action;
            	drawHandler.sendMessage(drawHandler.obtainMessage(DrawHandler.SEND_DRAW_EVENT, line));
//            	try {
//					WhiteboardAPI.Draw(groupId, "", line);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
            }
        	mCurX = -1;
        	mCurY = -1;
        	
        }
        if (action == MotionEvent.ACTION_DOWN){
        	drawingLines.createNewCurve();
        	WhiteboardLineInfo line = new WhiteboardLineInfo();
        	line.x1 = 0;
        	line.y1 = 0;
        	line.x2 = 0;
        	line.y2 = 0;
        	line.pressure = 0;
        	line.width = 0;
        	line.action = action;
        	drawHandler.sendMessage(drawHandler.obtainMessage(DrawHandler.SEND_DRAW_EVENT, line));
//        	try {
//				WhiteboardAPI.Draw(groupId, "", line);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
        }
        if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
            int P = event.getPointerCount();

            P = 1;
           
            for (int j = 0; j < P; j++) {	
            	prevX = mCurX;
            	prevY = mCurY;

            	mCurX = event.getX(j);
                mCurY = event.getY(j);

                if(prevX >= 0 && prevY > 0 && mCurX >= 0 && mCurY > 0)
                {
                	drawLine(Color.BLACK, prevX, prevY, mCurX, mCurY, event.getPressure(j), 4);
                	WhiteboardLineInfo line = new WhiteboardLineInfo();
                	line.x1 = prevX / ((float)viewWidth);
                	line.y1 = prevY / ((float)viewHeight);
                	line.x2 = mCurX / ((float)viewWidth);
                	line.y2 = mCurY / ((float)viewHeight);
                	line.pressure = event.getPressure(j);
                	line.width = 4;
                	line.action = action;
                	drawHandler.sendMessage(drawHandler.obtainMessage(DrawHandler.SEND_DRAW_EVENT, line));
//                	try {
//    					WhiteboardAPI.Draw(groupId, "", line);
//    				} catch (Exception e) {
//    					e.printStackTrace();
//    				}
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
    	return true;
    }
        
    public void clear() {
    	if (mCanvas != null) {  		
    		
    		drawingLines.setScaleWidth(mCanvas.getWidth()); 
    		drawingLines.setDisplaySize(mCanvas.getWidth(), mCanvas.getHeight());
        	
        	Drawable bg = this.getBackground();
        	if(bg != null) {
        		bg.setBounds(mCanvas.getClipBounds());
        		bg.draw(mCanvas);
        	} else {
        		mPaint.setARGB(0xff, 0xff, 0xff, 0xff);
        		mCanvas.drawPaint(mPaint);
        	}
        	redrawDrawingLines();
            invalidate();
        }
    	System.out.println("CLEAR!!");
    }
    
    public void drawLine(WhiteboardLineInfo lineInfo) {
    	drawHandler.sendMessage(drawHandler.obtainMessage(DrawHandler.DRAW, lineInfo));
    }
    
    public void doClearLines() {
    	drawHandler.sendMessage(drawHandler.obtainMessage(DrawHandler.CLEAR));
    }
    
    public void drawLine(int color, float x1, float y1, float x2, float y2, float pressure, float width) {
        if (width < 1) width = 1;

        if (mBitmap != null) {      	
        	drawingLines.addCurvePoint((int)x2, (int)y2);

        	pressure = 0.9f;
            int pressureLevel = (int)(128 + pressure * 128);
            mPaint.setARGB(pressureLevel, Color.red(color), Color.green(color), Color.blue(color));
            mCanvas.drawLine(x1, y1, x2, y2, mPaint);
            mRect.set((int)Math.min(x1, x2), (int)Math.min(y1, y2), (int)Math.max(x1, x2) + 1, (int)Math.max(y1, y2) + 1);
            invalidate(mRect);
        }
        if(x1 > 1000)
        	try{
        		throw new Exception("ha");
        	}catch(Exception e) {
        		e.printStackTrace();
        	}
    }
    
    public void redrawDrawingLines(){
        int prevX = -1;
	     int prevY = -1;
	     
	     ArrayList<ArrayList <DrawingLines.Point> > points = drawingLines.getDraw();
	     for(ArrayList <DrawingLines.Point> ap : points){
	    	 if(ap.size() > 0){
	    	 prevX = ap.get(0).x();
	    	 prevY = ap.get(0).y();
	    	 for(DrawingLines.Point p : ap){
	    		 if(prevX >= 0 && prevY > 0 && p.x() >= 0 && p.y() > 0){ 
	    			 if(mCanvas!= null)
	    				 mCanvas.drawLine(prevX, prevY, p.x(), p.y(), mPaint);
	    		 }
	    		 prevX = p.x();
	    		 prevY = p.y();
	    	 }
	    	 }
	     }
	     if(points != null){
	    	 for(ArrayList <DrawingLines.Point> ap : points){
	    	 	if(ap.size() > 0){
	    		prevX = ap.get(0).x();
	    	 	prevY = ap.get(0).topPartY();
	    	 	for(DrawingLines.Point p : ap){
	    		 	if(prevX >= 0 && prevY > 0 && p.x() >= 0 && p.topPartY() > 0){ 
	    			 	if(mCanvas!= null)
	    			 		mCanvas.drawLine(prevX, prevY, p.x(), p.topPartY(), mPaint);
	    		 	}
	    		 	prevX = p.x();
	    		 	prevY = p.topPartY();
	    	 	}
	    	 	}
	     	}
	     }
    }
    
    private int getNextColor()
    {
    	currentColor++;
    	if(currentColor == 1)
    	{
    		return Color.BLUE;
    	}
    	if(currentColor == 2)
    	{
    		return Color.GREEN;
    	}
    	if(currentColor == 3)
    	{
    		return Color.RED;
    	}
    	if(currentColor == 4)
    	{
    		return Color.YELLOW;
    	}
    	if(currentColor == 5)
    	{
    		return Color.CYAN;
    	}
    	if(currentColor == 6)
    	{
    		currentColor = 0;
    		return Color.MAGENTA;
    	}
    	
    	return Color.BLACK;
    }
    
    public void clearDrawingLines(){
    	drawingLines.clear();
    	clear(); 	
    }
    public void newDrawingLines(){
    	drawingLines = new DrawingLines();
    	clear();
    }
    public void redoDrawingLines(){
    	drawingLines.removeLastCurve();
    	clear();
    }
    
    public class DrawHandler extends Handler
    {
    	public static final int DRAW = 0;
    	public static final int CLEAR = 1;
    	public static final int SEND_DRAW_EVENT = 2;
    	
    	public void handleMessage(Message msg)
    	{
    		switch(msg.what)
    		{
    			case DRAW: handleDrawSignal((WhiteboardLineInfo)msg.obj); break;
    			case CLEAR: clearDrawingLines(); break;
    			case SEND_DRAW_EVENT:
    				try {
    					WhiteboardAPI.Draw(groupId, "", (WhiteboardLineInfo)msg.obj);
    				} catch (Exception e) {
    					e.printStackTrace();
    				}
    				break;
    			default: break;
    		}
    	}
    	private void handleDrawSignal(WhiteboardLineInfo line)
    	{
    		if(!colorMap.containsKey(line.sender))
        	{
        		colorMap.put(line.sender, getNextColor());
        	}
        	if(line.action == MotionEvent.ACTION_DOWN)
        	{
        		drawingLines.createNewCurve();
        	}    	
        	else if(line.action != MotionEvent.ACTION_CANCEL)
        	{
        		int viewWidth = getWidth();
        		int viewHeight = getHeight();
        		if(line.x1 < 1) { //Scale to the UI
	        		line.x1 = (float)viewWidth * line.x1;
	        		line.y1 = (float)viewHeight * line.y1;
	        		line.x2 = (float)viewWidth * line.x2;
	        		line.y2 = (float)viewHeight * line.y2;
        		}
        		drawLine(colorMap.get(line.sender), (float)line.x1, (float)line.y1, (float)line.x2, (float)line.y2, (float)line.pressure, (float)line.width);
        		
        	}
    	}
    }
 
}
