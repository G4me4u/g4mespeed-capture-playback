package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;

import net.minecraft.util.PacketByteBuf;

public class GSEntryTypeDelta extends GSEntryDelta {

	private GSEChannelEntryType newType;
	private GSEChannelEntryType oldType;

	public GSEntryTypeDelta() {
	}
	
	public GSEntryTypeDelta(UUID channelUUID, UUID entryUUID, GSEChannelEntryType newType, GSEChannelEntryType oldType) {
		super(channelUUID, entryUUID);
		
		this.newType = newType;
		this.oldType = oldType;
	}

	private void setEntryType(GSEChannelEntryType newType, GSEChannelEntryType oldType, 
			GSSequence sequence) throws GSSequenceDeltaException {
	
		GSChannelEntry entry = getEntry(sequence);
		checkEntryType(entry, oldType);
		entry.setType(newType);
	}
	
	@Override
	public void unapplyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		setEntryType(oldType, newType, sequence);
	}

	@Override
	public void applyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		setEntryType(newType, oldType, sequence);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		newType = GSEChannelEntryType.fromIndex(buf.readInt());
		oldType = GSEChannelEntryType.fromIndex(buf.readInt());
		
		if (newType == null || oldType == null)
			throw new IOException("Invalid type index!");
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeInt(newType.getIndex());
		buf.writeInt(oldType.getIndex());
	}
}
