package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public abstract class GSChannelDelta implements GSIDelta<GSSequence> {

	protected UUID channelUUID;

	protected GSChannelDelta() {
	}
	
	protected GSChannelDelta(UUID channelUUID) {
		this.channelUUID = channelUUID;
	}
	
	protected GSChannel getChannel(GSSequence sequence) throws GSDeltaException {
		GSChannel channel = sequence.getChannel(channelUUID);
		if (channel == null)
			throw new GSDeltaException("Expected channel does not exist");
		return channel;
	}
	
	protected void checkPreviousChannel(GSChannel channel, UUID expectedPrevUUID) throws GSDeltaException {
		GSChannel prevChannel = channel.getParent().getPreviousChannel(channelUUID);
		UUID prevUUID = (prevChannel == null) ? null : prevChannel.getChannelUUID();
		if (!Objects.equals(prevUUID, expectedPrevUUID))
			throw new GSDeltaException("Channel does not have the expected previous channel");
	}

	protected void checkChannelInfo(GSChannel channel, GSChannelInfo info) throws GSDeltaException {
		if (!channel.getInfo().equals(info))
			throw new GSDeltaException("Channel does not have the expected info");
	}
	
	protected void checkChannelDisabled(GSChannel channel, boolean disabled) throws GSDeltaException {
		if (channel == null || channel.isDisabled() != disabled)
			throw new GSDeltaException("Channel does not have the expected disabled state");
	}
	
	protected void checkChannelEntryCount(GSChannel channel, int expectedCount) throws GSDeltaException {
		if (channel.getEntries().size() != expectedCount)
			throw new GSDeltaException("Channel does not have the expected entry count");
	}
	
	protected void removeChannel(GSSequence sequence, UUID prevUUID, GSChannelInfo info,
			boolean expectedDisabled, int expectedEntryCount) throws GSDeltaException {
		
		GSChannel channel = getChannel(sequence);
		checkPreviousChannel(channel, prevUUID);
		checkChannelInfo(channel, info);
		checkChannelDisabled(channel, expectedDisabled);
		checkChannelEntryCount(channel, expectedEntryCount);
		sequence.removeChannel(channelUUID);
	}
	
	protected GSChannel addChannel(GSSequence sequence, UUID prevUUID, GSChannelInfo info) throws GSDeltaException {
		if (sequence.hasChannelUUID(channelUUID))
			throw new GSDeltaException("Channel already exists");
		
		try {
			GSChannel channel = sequence.addChannel(channelUUID, info);
			sequence.moveChannelAfter(channelUUID, prevUUID);
			return channel;
		} catch (Throwable t) {
			sequence.removeChannel(channelUUID);
			throw new GSDeltaException("Failed to add channel", t);
		}
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		channelUUID = buf.readUUID();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		buf.writeUUID(channelUUID);
	}
}
