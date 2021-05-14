package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;

public class GSNoteBlockSignalEventHandler implements GSISignalEventHandler {

	@Override
	public void handle(BlockState state, GSSignalEvent event, GSISignalEventContext context) {
		Block block = state.getBlock();
		
		if (block == Blocks.NOTE_BLOCK) {
			// Only perform the update if event is not shadow
			boolean rising = (event.getEdge() == GSESignalEdge.RISING_EDGE);
			BlockState newState = state.with(Properties.POWERED, rising);
			context.setState0(event.getPos(), newState, GSISignalEventContext.PROPAGATE_CHANGE | 
			                                            GSISignalEventContext.NOTIFY_LISTENERS);
			if (rising) {
				// Only send block events if the note block is powered
				// but was not powered prior to the signal (rising edge).
				context.dispatchBlockEvent(event.getPos(), block, 0, 0);
			}
		}
	}
}
