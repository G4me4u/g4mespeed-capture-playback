package com.g4mesoft.captureplayback.composition;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.stream.GSPlaybackStream;

class GSCompositionPlaybackStream extends GSPlaybackStream {

	public GSCompositionPlaybackStream(GSComposition composition) {
		super(composition.getBlockRegion());

		for (GSTrack track : composition.getTracks()) {
			GSSequence sequence = track.getSequence();
			
			for (GSTrackEntry entry : track.getEntries()) {
				for (GSChannel channel : sequence.getChannels())
					addChannelPlayback(channel, entry.getOffset());
			}
		}
	}
}
