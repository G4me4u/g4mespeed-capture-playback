package com.g4mesoft.captureplayback.composition;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.common.GSPlaybackStream;
import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.util.math.BlockPos;

class GSCompositionPlaybackStream extends GSPlaybackStream {

	public GSCompositionPlaybackStream(GSComposition composition) {
		super(composition.getBlockRegion());

		for (GSTrack track : composition.getTracks()) {
			GSSequence sequence = track.getSequence();
			
			for (GSTrackEntry entry : track.getEntries()) {
				long offset = entry.getOffset();
				
				for (GSChannel channel : sequence.getChannels()) {
					for (BlockPos position : channel.getInfo().getPositions()) {
						for (GSChannelEntry channelEntry : channel.getEntries()) {
							GSEChannelEntryType type = channelEntry.getType();
							
							GSSignalTime startTime = channelEntry.getStartTime().offsetCopy(offset, 0);
							GSSignalTime endTime = channelEntry.getEndTime().offsetCopy(offset, 0);
							
							addEntry(position, startTime, GSESignalEdge.RISING_EDGE, !type.hasStartEvent());
							addEntry(position, endTime, GSESignalEdge.FALLING_EDGE, !type.hasEndEvent());
						}
					}
				}
			}
		}
	}
}
