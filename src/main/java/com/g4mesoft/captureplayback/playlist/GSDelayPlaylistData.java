package com.g4mesoft.captureplayback.playlist;

import java.io.IOException;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSDelayPlaylistData implements GSIPlaylistData {

	private final int delay;
	
	public GSDelayPlaylistData(int delay) {
		if (delay < 0)
			throw new IllegalArgumentException("delay must be non-negative!");
		this.delay = delay;
	}
	
	public int getDelay() {
		return delay;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(delay);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof GSDelayPlaylistData) {
			GSDelayPlaylistData other = (GSDelayPlaylistData)obj;
			return delay == other.delay;
		}
		return false;
	}
	
	public static GSDelayPlaylistData read(GSDecodeBuffer buf) throws IOException {
		int delay = buf.readInt();
		return new GSDelayPlaylistData(delay);
	}

	public static void write(GSEncodeBuffer buf, GSDelayPlaylistData data) throws IOException {
		buf.writeInt(data.getDelay());
	}
}
