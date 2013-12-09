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
package org.alljoyn.devmodules.api.mediaplayer;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.annotation.*;

import org.alljoyn.devmodules.common.SongInfo;

@BusInterface(name = "org.alljoyn.api.devmodules.mediaplayer")
public interface MediaPlayerConnectorInterface {
	@BusMethod
    void play() throws BusException;

	@BusMethod
    void stop() throws BusException;
	
	@BusMethod
    void pause() throws BusException;
	
	@BusMethod
    void nextTrack() throws BusException;
	
	@BusMethod
    void prevTrack() throws BusException;

	@BusMethod
    void seekTo(int pos) throws BusException;
	
	@BusMethod
    void addTrack(SongInfo si) throws BusException;
	
	@BusMethod
    void removeTrack(SongInfo si) throws BusException;
	
	@BusMethod
    void moveTrackTo(SongInfo si, int loc) throws BusException;
	
	@BusMethod
	void startStreaming(SongInfo si) throws BusException;
}
