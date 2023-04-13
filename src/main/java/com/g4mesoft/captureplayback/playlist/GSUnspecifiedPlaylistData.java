package com.g4mesoft.captureplayback.playlist;

import java.io.IOException;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSUnspecifiedPlaylistData implements GSIPlaylistData {

	public static final GSUnspecifiedPlaylistData INSTANCE = new GSUnspecifiedPlaylistData();
	
	public static GSUnspecifiedPlaylistData read(GSDecodeBuffer buf) throws IOException {
		return INSTANCE;
	}

	public static void write(GSEncodeBuffer buf, GSUnspecifiedPlaylistData data) throws IOException {
	}
}
