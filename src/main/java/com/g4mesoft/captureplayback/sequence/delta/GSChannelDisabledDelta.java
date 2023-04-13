package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSChannelDisabledDelta extends GSChannelDelta {

	private boolean newDisabled;
	private boolean oldDisabled;

	public GSChannelDisabledDelta() {
	}
	
	public GSChannelDisabledDelta(UUID channelUUID, boolean newDisabled, boolean oldDisabled) {
		super(channelUUID);
		
		this.newDisabled = newDisabled;
		this.oldDisabled = oldDisabled;
	}

	private void setChannelDisabled(boolean newDisabled, boolean oldDisabled, GSSequence sequence) throws GSDeltaException {
		GSChannel channel = getChannel(sequence);
		checkChannelDisabled(channel, oldDisabled);
		channel.setDisabled(newDisabled);
	}
	
	@Override
	public void unapply(GSSequence sequence) throws GSDeltaException {
		setChannelDisabled(oldDisabled, newDisabled, sequence);
	}

	@Override
	public void apply(GSSequence sequence) throws GSDeltaException {
		setChannelDisabled(newDisabled, oldDisabled, sequence);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		newDisabled = buf.readBoolean();
		oldDisabled = buf.readBoolean();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		buf.writeBoolean(newDisabled);
		buf.writeBoolean(oldDisabled);
	}
}
