package com.g4mesoft.captureplayback.sequence;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.common.GSPlaybackStream;

import net.minecraft.util.math.BlockPos;

class GSSequencePlaybackStream extends GSPlaybackStream {

	public GSSequencePlaybackStream(GSSequence sequence) {
		super(sequence.getBlockRegion());

		for (GSChannel channel : sequence.getChannels()) {
			for (BlockPos position : channel.getInfo().getPositions()) {
				for (GSChannelEntry entry : channel.getEntries()) {
					GSEChannelEntryType type = entry.getType();
					
					addEntry(position, entry.getStartTime(), GSESignalEdge.RISING_EDGE, !type.hasStartEvent());
					addEntry(position, entry.getEndTime(), GSESignalEdge.FALLING_EDGE, !type.hasEndEvent());
				}
			}
		}
	}
}
