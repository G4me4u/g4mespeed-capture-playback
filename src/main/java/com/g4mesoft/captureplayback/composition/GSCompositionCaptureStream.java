package com.g4mesoft.captureplayback.composition;

import com.g4mesoft.captureplayback.common.GSCaptureStream;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSSequence;

class GSCompositionCaptureStream extends GSCaptureStream {

	public GSCompositionCaptureStream(GSComposition composition) {
		super(composition.getBlockRegion());

		for (GSTrack track : composition.getTracks()) {
			long smallestOffset = Long.MAX_VALUE;
			for (GSTrackEntry entry : track.getEntries()) {
				if (entry.getOffset() < smallestOffset)
					smallestOffset = entry.getOffset();
			}
			
			if (smallestOffset != Long.MAX_VALUE) {
				GSSequence sequence = track.getSequence();
				for (GSChannel channel : sequence.getChannels())
					addChannelCapture(channel, smallestOffset);
			}
		}
	}
}
