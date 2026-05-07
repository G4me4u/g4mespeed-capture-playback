package com.g4mesoft.captureplayback.module.server;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

public interface GSISessionStatusListener {

	public void sessionStarted(ServerPlayer player, UUID assetUUID);

	public void sessionStopped(ServerPlayer player, UUID assetUUID);
	
}
