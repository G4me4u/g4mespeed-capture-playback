package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.stream.GSSignalEvent;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class GSGeneralBlockSignalEventHandler implements GSISignalEventHandler {

	@Override
	public void handle(BlockState state, GSSignalEvent event, GSISignalEventContext context) {
		context.dispatchNeighborUpdate(event.getPos(), Blocks.AIR, Direction.UP);
	}
}
