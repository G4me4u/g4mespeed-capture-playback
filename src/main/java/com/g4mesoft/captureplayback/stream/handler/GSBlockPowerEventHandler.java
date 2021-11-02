package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.stream.GSSignalEvent;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

public class GSBlockPowerEventHandler implements GSISignalEventHandler {

	@Override
	public void handle(BlockState state, GSSignalEvent event, GSISignalEventContext context) {
		context.dispatchNeighborUpdate(event.getPos(), state, Direction.UP);
	}
}
