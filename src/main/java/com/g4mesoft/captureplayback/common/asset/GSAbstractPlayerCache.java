package com.g4mesoft.captureplayback.common.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class GSAbstractPlayerCache implements GSIPlayerCache {

	private final List<GSIPlayerCacheListener> listeners;
	
	public GSAbstractPlayerCache() {
		listeners = new ArrayList<>();
	}
	
	@Override
	public void addListener(GSIPlayerCacheListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null");
		listeners.add(listener);
	}

	@Override
	public void removeListener(GSIPlayerCacheListener listener) {
		listeners.remove(listener);
	}
	
	protected void dispatchEntryAdded(UUID playerUUID) {
		for (GSIPlayerCacheListener listener : listeners)
			listener.onEntryAdded(playerUUID);
	}

	protected void dispatchEntryRemoved(UUID playerUUID) {
		for (GSIPlayerCacheListener listener : listeners)
			listener.onEntryRemoved(playerUUID);
	}
}
