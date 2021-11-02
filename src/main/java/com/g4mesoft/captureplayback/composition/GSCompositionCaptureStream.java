package com.g4mesoft.captureplayback.composition;

import com.g4mesoft.captureplayback.common.GSCaptureStream;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSSequence;

class GSCompositionCaptureStream extends GSCaptureStream {

	public GSCompositionCaptureStream(GSComposition composition) {
		super(composition.getBlockRegion());

		for (GSTrack track : composition.getTracks()) {
			GSSequence sequence = track.getSequence();
			
			for (GSTrackEntry entry : track.getEntries()) {
				// Calculate the offset from the initial event.
				for (GSChannel channel : sequence.getChannels())
					addChannelCapture(channel, entry.getOffset());
			}
		}
	}
}
