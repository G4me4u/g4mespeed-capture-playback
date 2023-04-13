package com.g4mesoft.captureplayback.sequence;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSChannelEntryTime {

	private final GSSignalTime time;
	private final int subordering;
	
	public GSChannelEntryTime(GSSignalTime time, int subordering) {
		this.time = time;
		this.subordering = subordering;
	}
	
	public GSChannelEntryTime offsetCopy(long gtOffset, int mtOffset) {
		return new GSChannelEntryTime(time.offsetCopy(gtOffset, mtOffset), subordering);
	}
	
	public GSSignalTime getTime() {
		return time;
	}
	
	public int getSubordering() {
		return subordering;
	}

	@Override
	public int hashCode() {
		return time.hashCode() + 31 * subordering;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof GSChannelEntryTime))
			return false;

		GSChannelEntryTime entryTime = (GSChannelEntryTime)other;
		if (!time.isEqual(entryTime.time))
			return false;
		if (subordering != entryTime.subordering)
			return false;
		
		return true;
	}
	
	public static GSChannelEntryTime read(GSDecodeBuffer buf) throws IOException {
		GSSignalTime time = GSSignalTime.read(buf);
		int subordering = buf.readInt();
		
		if (subordering < 0)
			throw new IOException("Invalid sub-ordering!");
		
		return new GSChannelEntryTime(time, subordering);
	}

	public static void write(GSEncodeBuffer buf, GSChannelEntryTime entryTime) throws IOException {
		GSSignalTime.write(buf, entryTime.time);
		buf.writeInt(entryTime.subordering);
	}
}
