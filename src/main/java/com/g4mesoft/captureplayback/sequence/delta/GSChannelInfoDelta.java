package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSChannelInfoDelta extends GSChannelDelta {

	private GSChannelInfo newInfo;
	private GSChannelInfo oldInfo;

	public GSChannelInfoDelta() {
	}

	public GSChannelInfoDelta(UUID channelUUID, GSChannelInfo newInfo, GSChannelInfo oldInfo) {
		super(channelUUID);
		
		this.newInfo = newInfo;
		this.oldInfo = oldInfo;
	}
	
	private void setChannelInfo(GSChannelInfo newInfo, GSChannelInfo oldInfo, GSSequence sequence) throws GSDeltaException {
		GSChannel channel = getChannel(sequence);
		checkChannelInfo(channel, oldInfo);
		channel.setInfo(newInfo);
	}
	
	@Override
	public void unapply(GSSequence sequence) throws GSDeltaException {
		setChannelInfo(oldInfo, newInfo, sequence);
	}

	@Override
	public void apply(GSSequence sequence) throws GSDeltaException {
		setChannelInfo(newInfo, oldInfo, sequence);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		newInfo = GSChannelInfo.read(buf);
		oldInfo = GSChannelInfo.read(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		GSChannelInfo.write(buf, newInfo);
		GSChannelInfo.write(buf, oldInfo);
	}
}
