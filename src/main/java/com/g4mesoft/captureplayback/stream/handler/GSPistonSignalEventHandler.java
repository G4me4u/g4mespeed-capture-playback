package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.common.GSESignalEdge;
import com.g4mesoft.captureplayback.stream.GSSignalEvent;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class GSPistonSignalEventHandler implements GSISignalEventHandler {

	@Override
	public void handle(BlockState state, GSSignalEvent event, GSISignalEventContext context) {
		Block block = state.getBlock();
		
		if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
			GSESignalEdge edge = event.getEdge();
			// Only send an event if piston is in the correct state. Specifically,
			// the piston should be extended when receiving an event for retraction.
			if (edge != GSESignalEdge.FALLING_EDGE || state.getValue(PistonBaseBlock.EXTENDED)) {
				int type = (edge == GSESignalEdge.RISING_EDGE) ? 0 : 1;
				int data = state.getValue(BlockStateProperties.FACING).get3DDataValue();
				
				context.dispatchBlockEvent(event.getPos(), block, type, data);
			}
		}
	}
}
