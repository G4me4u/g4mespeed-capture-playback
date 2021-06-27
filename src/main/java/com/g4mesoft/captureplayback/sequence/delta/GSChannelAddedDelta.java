package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

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
		
		this.prevUUID = prevUUID;
		this.info = info;
	}
	
	@Override
	public void unapplyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		removeChannel(sequence, prevUUID, info, GSChannel.DEFAULT_DISABLED, 0);
	}

	@Override
	public void applyDelta(GSSequence sequence) throws GSSequenceDeltaException {
		addChannel(sequence, prevUUID, info);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		info = GSChannelInfo.read(buf);
		prevUUID = buf.readBoolean() ? buf.readUuid() : null;
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSChannelInfo.write(buf, info);

		buf.writeBoolean(prevUUID != null);
		if (prevUUID != null)
			buf.writeUuid(prevUUID);
	}
}
