package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class GSTntSignalEventHandler implements GSISignalEventHandler {

	@Override
	public void handle(BlockState state, GSSignalEvent event, GSISignalEventContext context) {
		if (event.getEdge() == GSESignalEdge.RISING_EDGE) {
			// Only send updates to TNT on the rising edge.
			context.dispatchNeighborUpdate(event.getPos(), Blocks.AIR, Direction.UP);
		}
	}
}
