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
package org.alljoyn.devmodules.common;

import org.alljoyn.bus.annotation.Position;

public final class SongInfo {
	@Position(0)
	public String songPath;
	@Position(1)
	public String songName;
	@Position(2)
	public String artist;
	@Position(3)
	public String album;
	@Position(4)
	public String artPath;
	@Position(5)
	public int songId;
	@Position(6)
	public int albumId;
	@Position(7)
	public String fileName;
	@Position(8)
	public String busId;
	
	public SongInfo() {
		this.songId = -1;
		this.songPath = "ERROR";
		this.songName = "ERROR";
		this.artist = "ERROR";
		this.album = "ERROR";
		this.albumId = -1;
		this.artPath = "";
		this.busId = "ERROR";
		this.fileName = "ERROR";
	}
	
	public SongInfo(int songId, String songPath, String songName, String artist, String album, int albumId, String artPath, String fileName)
	{
		this.songId = songId;
		this.songPath = songPath;
		this.songName = songName;
		this.artist = artist;
		this.album = album;
		this.albumId = albumId;
		this.artPath = artPath;
		this.fileName = fileName;
		this.busId = "ERROR";
	}
	
	public String toString() {
		return "SongInfo: "
			+ songId + ", "
			+ songPath + ", "
			+ songName + ", "
			+ artist + ", "
			+ album + ", "
			+ albumId + ", "
			+ artPath + ", "
			+ fileName + ", "
			+ busId;
	}
}
