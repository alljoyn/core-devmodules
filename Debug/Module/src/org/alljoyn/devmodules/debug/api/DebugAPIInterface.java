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
package org.alljoyn.devmodules.debug.api;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.*;

@BusInterface(name = "org.alljoyn.api.devmodules.debug")
public interface DebugAPIInterface {
////////////////////////////////////////////////
	// Control of Local Service 
	////////////////////////////////////////////////
	
	// Check status
	@BusMethod
    public boolean IsEnabled() throws BusException;

	// Enable Logging
	@BusMethod
    public void Enable() throws BusException;

	// Disable Logging
	@BusMethod
    public void Disable() throws BusException;

	// Get current filter string (OS-Specific)
	@BusMethod
    public String GetFilter() throws BusException;

	// Set filter string (OS-Specific)
	@BusMethod
    public void SetFilter(String filter) throws BusException;
	
	
	
	////////////////////////////////////////////////
	// Interaction with Remote Debug Services
	////////////////////////////////////////////////
	
	// Connect to a particular service (just use prefix for all services)
	@BusMethod
	public void Connect (String Device, String service) throws BusException;
	
	// Disconnect from a particular service
	@BusMethod
	public void Disconnect (String Device, String service) throws BusException;

	// List connected services
	@BusMethod
	public String[] GetServiceList () throws BusException;
	
	// Get the list of messages received from a particular service
	@BusMethod
	public DebugMessageDescriptor[] GetMessageList (String service) throws BusException;
}
