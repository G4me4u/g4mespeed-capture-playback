package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;

public class GSPistonSignalEventHandler implements GSISignalEventHandler {

	@Override
	public void handle(BlockState state, GSSignalEvent event, GSISignalEventContext context) {
		Block block = state.getBlock();

		if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
			int type = (event.getEdge() == GSESignalEdge.RISING_EDGE) ? 0 : 1;
			int data = state.get(Properties.FACING).getId();
			
			context.dispatchBlockEvent(event.getPos(), block, type, data);
		}
	}
}
