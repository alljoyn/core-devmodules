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
package org.alljoyn.devmodules.filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.alljoyn.api.filetransfer.FileTransferAPIImpl;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.annotation.BusMethod;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.devmodules.interfaces.AllJoynContainerInterface;
import org.json.JSONObject;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class FileTransferObject implements FileTransferInterface, BusObject {

	private AllJoynContainerInterface alljoynContainer;
	private ProcessHandler handler = new ProcessHandler();
	
	private FileTransferInterface signalSender;
	
	private static final int MAX_BYTES = 114500;
	private static final int SEND_FILE = 0;
	
	public FileTransferObject(AllJoynContainerInterface alljoynContainer) {
		this.alljoynContainer = alljoynContainer;
	}
	
	public void getFile(String path) throws BusException {
		//This tells the service to start sending its signal with the file data if it has the file
		Message msg = handler.obtainMessage(SEND_FILE);
		Bundle data = new Bundle();
		data.putString("path", path);
		data.putString("requestor", alljoynContainer.getBusAttachment().getMessageContext().sender);
		msg.setData(data);
		handler.sendMessage(msg);
	}
	
	public void sendFile(String path) throws BusException {
		Message msg = handler.obtainMessage(SEND_FILE);
		Bundle data = new Bundle();
		data.putString("path", path);
		msg.setData(data);
		handler.sendMessage(msg);
	}
	
	private class ProcessHandler extends Handler {
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case SEND_FILE:
				try{
					//For now generate global signal emitter and store it
					//This will change when we change to single advertisement and sessionless signals
					String requestor = msg.getData().getString("requestor");

					if(signalSender == null) {	
						SignalEmitter se = new SignalEmitter(FileTransferObject.this, SignalEmitter.GlobalBroadcast.On);
						signalSender = se.getInterface(FileTransferInterface.class);
					}
					
					String filePath = (String)msg.getData().getString("path");
					System.out.println("filePath: "+filePath);
					String fileName = filePath.substring(filePath.lastIndexOf("/")+1);
					File outFile = new File(filePath);
					FileInputStream in = new FileInputStream(outFile);
					int length = (int) outFile.length();
					byte[] tempBytes;
					int numRead = 0;
					int numChunks = length / MAX_BYTES + (length % MAX_BYTES == 0 ? 0 : 1);
					for(int i = 0; i < numChunks; i++) {
						tempBytes = null;
						int offset = 0;
						numRead = 0;
						if(MAX_BYTES > length) {
							tempBytes = new byte[length];
						} else {
							tempBytes = new byte[MAX_BYTES];
						}
						while(offset < tempBytes.length && (numRead=in.read(tempBytes, 0, tempBytes.length-offset)) >= 0) {
							offset += numRead;
						}
						length -= MAX_BYTES;
						System.out.println("Sending the file: "+(i+1));
						signalSender.fileData(fileName, filePath, tempBytes);
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					in.close();
					signalSender.fileData(fileName, filePath, new byte[0]);
				} catch(Exception e) {
					e.printStackTrace();
				}
				break;
			}
		}
	}


	public void fileData(String fileName, String origPath, byte[] data) throws BusException {
		//Empty method since signal
	}

	public void offerFile(String filename, String path) throws BusException {
		//Empty method since signal		
	}
}
