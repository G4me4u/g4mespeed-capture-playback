package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.access.GSIServerLevelAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GSServerLevelSignalEventContext implements GSISignalEventContext {

	private final ServerLevel level;

	public GSServerLevelSignalEventContext(ServerLevel level) {
		this.level = level;
	}
	
	@Override
	public boolean dispatchBlockEvent(BlockPos pos, Block block, int type, int data) {
		return ((GSIServerLevelAccess)level).gcp_dispatchBlockEvent(pos, block, type, data);
	}

	@Override
	public void dispatchNeighborUpdate(BlockPos pos, Block fromBlock, Direction fromDir) {
		((GSIServerLevelAccess)level).gcp_dispatchNeighborUpdate(pos, fromBlock, fromDir);
	}

	@Override
	public boolean setState(BlockPos pos, BlockState state, int flags) {
		return ((GSIServerLevelAccess)level).gcp_setState(pos, state, flags);
	}
}
