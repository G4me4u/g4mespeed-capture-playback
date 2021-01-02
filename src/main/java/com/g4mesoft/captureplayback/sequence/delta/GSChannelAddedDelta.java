package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;

import net.minecraft.util.PacketByteBuf;

public class GSChannelAddedDelta extends GSChannelDelta {

	private GSChannelInfo info;

	public GSChannelAddedDelta() {
	}

	public GSChannelAddedDelta(GSChannel channel) {
		this(channel.getChannelUUID(), channel.getInfo());
	}
	
	public GSChannelAddedDelta(UUID channelUUID, GSChannelInfo info) {
		super(channelUUID);
		
		this.info = info;
	}
	
	@Override
	public void unapplyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		removeChannel(sequence, info, GSChannel.DEFAULT_DISABLED, 0);
	}

	@Override
	public void applyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		addChannel(sequence, info);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		info = GSChannelInfo.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSChannelInfo.write(buf, info);
	}
}
