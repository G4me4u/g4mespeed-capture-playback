package com.g4mesoft.captureplayback.common.asset;

import java.util.UUID;

public interface GSIPlayerCacheListener {

	public void onEntryAdded(UUID playerUUID);
	
	public void onEntryRemoved(UUID playerUUID);
	
}
