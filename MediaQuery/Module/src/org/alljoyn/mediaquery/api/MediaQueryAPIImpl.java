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
package org.alljoyn.mediaquery.api;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.annotation.BusSignal;
import org.alljoyn.devmodules.common.MediaIdentifier;
import org.alljoyn.devmodules.interfaces.ModuleAPIInterface;

public class MediaQueryAPIImpl implements ModuleAPIInterface {
	//callback interface so that specific callback can be invoked
	//Signal emitter - so we have multiple apps that receive callback data
	
	class MediaQueryCallbackObject implements MediaQueryCallbackInterface, BusObject {
		public void onMediaQueryServiceAvailable(String service) { }
		
	    public void onMediaQueryServiceLost(String service) { }
	    
	    // called when a transaction completes
		// @param transaction The name of the service from which the media was obtained
		// @param service     The transaction ID of the query
		// @param mtype       The media type ("photo", "music" etc. - see MediaQueryConstants)
		public void onQueryComplete(String service, String mtype) { }
		
		// called when an item from a query is available
		// @param transaction The name of the service from which the media was obtained
		// @param service     The transaction ID of the query
		// @param mtype       The media type ("photo", "music" etc. - see MediaQueryConstants)
		// @param item        The identifier/descriptor for the media item
		public void onItemAvailable(String service, MediaIdentifier item) { }
		
		public void onTransferComplete(String service, String path, String mtype, String localpath) { }
		
		public void onTransferError(int transaction, String service, String mtype, String path) { }

		public void CallbackJSON(int transactionId, String module,
				String jsonCallbackData) {
			
		}

		public void CallbackData(int transactionId, String module,
				byte[] rawData, int totalParts, int partNumber) {
			
		}
	}
	
	private MediaQueryCallbackObject mediaQueryCallbackObject = new MediaQueryCallbackObject();
	public static MediaQueryCallbackInterface mediaQueryCallback; //look into possibly just folding this in with the regular impl so I don't have to static var this and link it with the ChatImpl
	
	public MediaQueryAPIImpl() {
	}

	@Override
	public void connectCallbackObject(int sessionId, String joiner) {
		SignalEmitter emitter = new SignalEmitter(mediaQueryCallbackObject, sessionId, SignalEmitter.GlobalBroadcast.Off);
		mediaQueryCallback = emitter.getInterface(MediaQueryCallbackInterface.class);
	}

	@Override
	public BusObject getBusObject() {
		return new MediaQueryAPIObject();
	}

	@Override
	public String getBusObjectPath() {
		return MediaQueryAPIObject.OBJECT_PATH;
	}

	@Override
	public BusObject getCallbackBusObject() {
		// TODO Auto-generated method stub
		return mediaQueryCallbackObject;
	}

	@Override
	public String getCallbackBusObjectPath() {
		// TODO Auto-generated method stub
		return mediaQueryCallbackObject.OBJECT_PATH;
	}
	
}
