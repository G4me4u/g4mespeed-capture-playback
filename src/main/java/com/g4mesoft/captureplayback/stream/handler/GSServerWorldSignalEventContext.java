package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class GSServerWorldSignalEventContext implements GSISignalEventContext {

	private final ServerWorld world;

	public GSServerWorldSignalEventContext(ServerWorld world) {
		this.world = world;
	}
	
	@Override
	public boolean dispatchBlockEvent(BlockPos pos, Block block, int type, int data) {
		return ((GSIServerWorldAccess)world).gcp_dispatchBlockEvent(pos, block, type, data);
	}

	@Override
	public void dispatchNeighborUpdate(BlockPos pos, Block fromBlock, Direction fromDir) {
		((GSIServerWorldAccess)world).gcp_dispatchNeighborUpdate(pos, fromBlock, fromDir);
	}

	@Override
	public boolean setState(BlockPos pos, BlockState state, int flags) {
		return ((GSIServerWorldAccess)world).gcp_setState(pos, state, flags);
	}
}
