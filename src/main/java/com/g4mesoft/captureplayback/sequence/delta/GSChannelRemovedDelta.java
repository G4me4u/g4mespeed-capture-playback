package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

public class GSChannelRemovedDelta extends GSChannelDelta {

	private UUID prevUUID;
	private GSChannelInfo info;
	private boolean disabled;
	private GSChannelEntryRemovedDelta[] entryDeltas;

	public GSChannelRemovedDelta() {
	}

	public GSChannelRemovedDelta(GSChannel channel, UUID prevUUID) {
		this(channel.getChannelUUID(), prevUUID, channel.getInfo(), channel.isDisabled(), channel.getEntries());
	}
	
	public GSChannelRemovedDelta(UUID channelUUID, UUID prevUUID, GSChannelInfo info,
	                             boolean disabled, Collection<GSChannelEntry> entries) {
		
		super(channelUUID);
		
		this.prevUUID = prevUUID;
		this.info = info;
		this.disabled = disabled;
		
		entryDeltas = new GSChannelEntryRemovedDelta[entries.size()];
		
		int i = 0;
		for (GSChannelEntry entry : entries)
			entryDeltas[i++] = new GSChannelEntryRemovedDelta(entry);
	}
	
	@Override
	public void unapply(GSSequence sequence) throws GSDeltaException {
		GSChannel channel = addChannel(sequence, prevUUID, info);
		
		channel.setDisabled(disabled);
		
		for (GSChannelEntryDelta entryDelta : entryDeltas)
			entryDelta.unapply(sequence);
	}

	@Override
	public void apply(GSSequence sequence) throws GSDeltaException {
		removeChannel(sequence, prevUUID, info, disabled, entryDeltas.length);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		info = GSChannelInfo.read(buf);
		disabled = buf.readBoolean();
		
		entryDeltas = new GSChannelEntryRemovedDelta[buf.readInt()];
		for (int i = 0; i < entryDeltas.length; i++)
			(entryDeltas[i] = new GSChannelEntryRemovedDelta()).read(buf);
		
		prevUUID = buf.readBoolean() ? buf.readUuid() : null;
	}
	
	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);
	
		GSChannelInfo.write(buf, info);
		buf.writeBoolean(disabled);
		
		buf.writeInt(entryDeltas.length);
		for (GSChannelEntryRemovedDelta entryDelta : entryDeltas)
			entryDelta.write(buf);
		
		buf.writeBoolean(prevUUID != null);
		if (prevUUID != null)
			buf.writeUuid(prevUUID);
	}
}
