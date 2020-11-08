package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class GSNoteBlockSignalEventHandler implements GSISignalEventHandler {

	@Override
	public void handle(BlockState state, GSSignalEvent event, GSISignalEventContext context) {
		// Only send block actions if the note block is powered
		// but was not powered prior to the signal (rising edge).
		if (event.getEdge() == GSESignalEdge.RISING_EDGE) {
			Block block = state.getBlock();
			
			if (block == Blocks.NOTE_BLOCK)
				context.dispatchBlockAction(event.getPos(), block, 0, 0);
		}
	}
}
