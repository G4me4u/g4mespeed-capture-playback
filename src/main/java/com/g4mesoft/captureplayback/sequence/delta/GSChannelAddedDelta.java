package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSChannelAddedDelta extends GSChannelDelta {

	private UUID prevUUID;
	private GSChannelInfo info;

	public GSChannelAddedDelta() {
	}

	public GSChannelAddedDelta(GSChannel channel, UUID prevUUID) {
		this(channel.getChannelUUID(), prevUUID, channel.getInfo());
	}
	
	public GSChannelAddedDelta(UUID channelUUID, UUID prevUUID, GSChannelInfo info) {
		super(channelUUID);
		
		if (info == null)
			throw new IllegalArgumentException("info is null");
		
		this.prevUUID = prevUUID;
		this.info = info;
	}
	
	@Override
	public void unapply(GSSequence sequence) throws GSDeltaException {
		removeChannel(sequence, prevUUID, info, GSChannel.DEFAULT_DISABLED, 0);
	}

	@Override
	public void apply(GSSequence sequence) throws GSDeltaException {
		addChannel(sequence, prevUUID, info);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		info = GSChannelInfo.read(buf);
		prevUUID = buf.readBoolean() ? buf.readUUID() : null;
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		GSChannelInfo.write(buf, info);

		buf.writeBoolean(prevUUID != null);
		if (prevUUID != null)
			buf.writeUUID(prevUUID);
	}
}
