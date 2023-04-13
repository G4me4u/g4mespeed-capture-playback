package com.g4mesoft.captureplayback.common.asset;

import java.util.UUID;

public interface GSIPlayerCache {

	public void addListener(GSIPlayerCacheListener listener);

	public void removeListener(GSIPlayerCacheListener listener);
	
	public GSPlayerCacheEntry get(UUID playerUUID);

	public Iterable<UUID> getPlayers();
	
}
