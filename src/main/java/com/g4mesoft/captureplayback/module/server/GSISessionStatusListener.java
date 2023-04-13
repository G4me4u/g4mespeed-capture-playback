package com.g4mesoft.captureplayback.module.server;

import java.util.UUID;

import net.minecraft.server.network.ServerPlayerEntity;

public interface GSISessionStatusListener {

	public void sessionStarted(ServerPlayerEntity player, UUID assetUUID);

	public void sessionStopped(ServerPlayerEntity player, UUID assetUUID);
	
}
