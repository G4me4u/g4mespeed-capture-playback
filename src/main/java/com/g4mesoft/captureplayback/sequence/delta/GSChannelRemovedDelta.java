package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;

import net.minecraft.util.PacketByteBuf;

public class GSChannelRemovedDelta extends GSChannelDelta {

	private GSChannelInfo info;
	private boolean disabled;
	private GSEntryRemovedDelta[] entryDeltas;

	public GSChannelRemovedDelta() {
	}

	public GSChannelRemovedDelta(GSChannel channel) {
		this(channel.getChannelUUID(), channel.getInfo(), channel.isDisabled(), channel.getEntries());
	}
	
	public GSChannelRemovedDelta(UUID channelUUID, GSChannelInfo info, boolean disabled, Collection<GSChannelEntry> entries) {
		super(channelUUID);
		
		this.info = info;
		this.disabled = disabled;
		
		entryDeltas = new GSEntryRemovedDelta[entries.size()];

		int i = 0;
		for (GSChannelEntry entry : entries)
			entryDeltas[i++] = new GSEntryRemovedDelta(entry);
	}
	
	@Override
	public void unapplyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		GSChannel channel = addChannel(sequence, info);
		
		channel.setDisabled(disabled);
		
		for (GSEntryDelta entryDelta : entryDeltas)
			entryDelta.unapplyDelta(sequence);
	}

	@Override
	public void applyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		removeChannel(sequence, info, disabled, entryDeltas.length);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		info = GSChannelInfo.read(buf);
		disabled = buf.readBoolean();
		
		entryDeltas = new GSEntryRemovedDelta[buf.readInt()];
		for (int i = 0; i < entryDeltas.length; i++)
			(entryDeltas[i] = new GSEntryRemovedDelta()).read(buf);
	}
	
	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);
	
		GSChannelInfo.write(buf, info);
		buf.writeBoolean(disabled);
		
		buf.writeInt(entryDeltas.length);
		for (GSEntryRemovedDelta entryDelta : entryDeltas)
			entryDelta.write(buf);
	}
}
