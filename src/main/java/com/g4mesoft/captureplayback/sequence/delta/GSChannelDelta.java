package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

public abstract class GSChannelDelta implements GSISequenceDelta {

	protected UUID channelUUID;

	protected GSChannelDelta() {
	}
	
	protected GSChannelDelta(UUID channelUUID) {
		this.channelUUID = channelUUID;
	}
	
	protected GSChannel getChannel(GSSequence sequence) throws GSSequenceDeltaException {
		GSChannel channel = sequence.getChannel(channelUUID);
		if (channel == null)
			throw new GSSequenceDeltaException("Expected channel does not exist");
		return channel;
	}
	
	protected void checkPreviousChannel(GSChannel channel, UUID expectedPrevUUID) throws GSSequenceDeltaException {
		GSChannel prevChannel = channel.getParent().getPreviousChannel(channelUUID);
		UUID prevUUID = (prevChannel == null) ? null : prevChannel.getChannelUUID();
		if (!Objects.equals(prevUUID, expectedPrevUUID))
			throw new GSSequenceDeltaException("Channel does not have the expected previous channel");
	}

	protected void checkChannelInfo(GSChannel channel, GSChannelInfo info) throws GSSequenceDeltaException {
		if (!channel.getInfo().equals(info))
			throw new GSSequenceDeltaException("Channel does not have the expected info");
	}
	
	protected void checkChannelDisabled(GSChannel channel, boolean disabled) throws GSSequenceDeltaException {
		if (channel == null || channel.isDisabled() != disabled)
			throw new GSSequenceDeltaException("Channel does not have the expected disabled state");
	}
	
	protected void checkChannelEntryCount(GSChannel channel, int expectedCount) throws GSSequenceDeltaException {
		if (channel.getEntries().size() != expectedCount)
			throw new GSSequenceDeltaException("Channel does not have the expected entry count");
	}
	
	protected void removeChannel(GSSequence sequence, UUID prevUUID, GSChannelInfo info,
			boolean expectedDisabled, int expectedEntryCount) throws GSSequenceDeltaException {
		
		GSChannel channel = getChannel(sequence);
		checkPreviousChannel(channel, prevUUID);
		checkChannelInfo(channel, info);
		checkChannelDisabled(channel, expectedDisabled);
		checkChannelEntryCount(channel, expectedEntryCount);
		sequence.removeChannel(channelUUID);
	}
	
	protected GSChannel addChannel(GSSequence sequence, UUID prevUUID, GSChannelInfo info) throws GSSequenceDeltaException {
		if (sequence.hasChannelUUID(channelUUID))
			throw new GSSequenceDeltaException("Channel already exists");
		
		GSChannel channel = sequence.addChannel(channelUUID, info);
		sequence.moveChannelAfter(channelUUID, prevUUID);
		return channel;
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		channelUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeUuid(channelUUID);
	}
}
