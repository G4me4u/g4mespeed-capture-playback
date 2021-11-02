package com.g4mesoft.captureplayback.sequence;

import com.g4mesoft.captureplayback.common.GSPlaybackStream;

class GSSequencePlaybackStream extends GSPlaybackStream {

	public GSSequencePlaybackStream(GSSequence sequence) {
		super(sequence.getBlockRegion());

		for (GSChannel channel : sequence.getChannels())
			addChannelPlayback(channel, 0L);
	}
}
