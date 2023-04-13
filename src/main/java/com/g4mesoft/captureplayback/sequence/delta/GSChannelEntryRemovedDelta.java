package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSChannelEntryRemovedDelta extends GSChannelEntryDelta {

	private GSSignalTime startTime;
	private GSSignalTime endTime;
	private GSEChannelEntryType type;
	
	public GSChannelEntryRemovedDelta() {
	}

	public GSChannelEntryRemovedDelta(GSChannelEntry entry) {
		this(entry.getParent().getChannelUUID(), entry.getEntryUUID(),
				entry.getStartTime(), entry.getEndTime(), entry.getType());
	}
	
	public GSChannelEntryRemovedDelta(UUID channelUUID, UUID entryUUID, GSSignalTime startTime,
			GSSignalTime endTime, GSEChannelEntryType type) {
		
		super(channelUUID, entryUUID);
		
		this.startTime = startTime;
		this.endTime = endTime;
		this.type = type;
	}

	@Override
	public void unapply(GSSequence sequence) throws GSDeltaException {
		GSChannelEntry entry = addEntry(sequence, startTime, endTime);
		
		entry.setType(type);
	}

	@Override
	public void apply(GSSequence sequence) throws GSDeltaException {
		removeEntry(sequence, startTime, endTime, type);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		startTime = GSSignalTime.read(buf);
		endTime = GSSignalTime.read(buf);
		
		type = GSEChannelEntryType.fromIndex(buf.readInt());
		if (type == null)
			throw new IOException("Type index invalid");
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		GSSignalTime.write(buf, startTime);
		GSSignalTime.write(buf, endTime);
		
		buf.writeInt(type.getIndex());
	}
}
