package com.g4mesoft.captureplayback.stream.handler;

import com.g4mesoft.captureplayback.stream.GSSignalEvent;

import net.minecraft.block.BlockState;

public interface GSISignalEventHandler {

	public void handle(BlockState state, GSSignalEvent event, GSISignalEventContext context);
	
}
