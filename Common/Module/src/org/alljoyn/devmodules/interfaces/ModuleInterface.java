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
package org.alljoyn.devmodules.interfaces;

import org.alljoyn.bus.BusObject;

/**
 * This interface defines the methods that must be implemented by a 'module'
 * These are mostly used to automate common functions such as advertising the 
 * service name, adding objects to the bus once established etc.
 *
 */
public interface ModuleInterface {
	
	public void RegisterSignalHandlers();
	
	public void InitAPI(AllJoynContainerInterface coreLogic);
	
	public void SetupSession(); //Initiates the module and uses SessionManager to bind a session and setup callbacks
	
	public String getServiceName(); //to register the service
	
	public BusObject getBusObject();
	
	public String getObjectPath();
	
	public String getAdvertisedName();
	
	public void shutdown();	//Cleanup
	
	public void foundAdvertisedName(String name, short transport, String namePrefix);
	
	public void lostAdvertisedName(String name, short transport, String namePrefix);
	
	//start stop, restart
}


