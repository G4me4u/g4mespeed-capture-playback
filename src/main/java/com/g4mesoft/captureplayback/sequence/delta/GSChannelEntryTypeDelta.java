package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSChannelEntryTypeDelta extends GSChannelEntryDelta {

	private GSEChannelEntryType newType;
	private GSEChannelEntryType oldType;

	public GSChannelEntryTypeDelta() {
	}
	
	public GSChannelEntryTypeDelta(UUID channelUUID, UUID entryUUID, GSEChannelEntryType newType, GSEChannelEntryType oldType) {
		super(channelUUID, entryUUID);
		
		this.newType = newType;
		this.oldType = oldType;
	}

	private void setEntryType(GSEChannelEntryType newType, GSEChannelEntryType oldType, 
			GSSequence sequence) throws GSDeltaException {
	
		GSChannelEntry entry = getEntry(sequence);
		checkEntryType(entry, oldType);
		entry.setType(newType);
	}
	
	@Override
	public void unapply(GSSequence sequence) throws GSDeltaException {
		setEntryType(oldType, newType, sequence);
	}

	@Override
	public void apply(GSSequence sequence) throws GSDeltaException {
		setEntryType(newType, oldType, sequence);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		newType = GSEChannelEntryType.fromIndex(buf.readInt());
		oldType = GSEChannelEntryType.fromIndex(buf.readInt());
		
		if (newType == null || oldType == null)
			throw new IOException("Invalid type index!");
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		buf.writeInt(newType.getIndex());
		buf.writeInt(oldType.getIndex());
	}
}
