package com.g4mesoft.captureplayback.sequence;

import com.g4mesoft.captureplayback.common.GSCaptureStream;

class GSSequenceCaptureStream extends GSCaptureStream {
	
	public GSSequenceCaptureStream(GSSequence sequence) {
		super(sequence.getBlockRegion());
		
		for (GSChannel channel : sequence.getChannels()) {
			// TODO: fix issues when capturing a single channel in multiple positions
			addChannelCapture(channel, 0L);
		}
	}
}
