package com.g4mesoft.captureplayback.stream.handler;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public interface GSISignalEventContext {

	public boolean dispatchBlockAction(BlockPos pos, Block block, int type, int data);

}
