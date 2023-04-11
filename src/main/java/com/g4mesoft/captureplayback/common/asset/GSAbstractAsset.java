package com.g4mesoft.captureplayback.common.asset;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;

public abstract class GSAbstractAsset {

	private final GSEAssetType type;
	private boolean added;
	
	private final List<GSIAssetListener> listeners;
	
	protected GSAbstractAsset(GSEAssetType type) {
		this.type = type;
		added = false;
		
		listeners = new ArrayList<>();
	}
	
	protected abstract void duplicateFrom(GSAbstractAsset other);
	
	protected void onAdded() {
		if (added)
			throw new IllegalStateException("Already added");
		added = true;
	}

	protected void onRemoved() {
		if (!added)
			throw new IllegalStateException("Already removed");
		added = false;
	}
	
	void addListener(GSIAssetListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		listeners.add(listener);
	}

	void removeListener(GSIAssetListener listener) {
		listeners.remove(listener);
	}
	
	protected void dispatchNameChanged(String name) {
		for (GSIAssetListener listener : listeners)
			listener.onNameChanged(name);
	}

	protected void dispatchDerivedAssetAdded(UUID derivedUUID) {
		for (GSIAssetListener listener : listeners)
			listener.onDerivedAssetAdded(derivedUUID);
	}

	protected void dispatchDerivedAssetRemoved(UUID derivedUUID) {
		for (GSIAssetListener listener : listeners)
			listener.onDerivedAssetRemoved(derivedUUID);
	}

	public final GSEAssetType getType() {
		return type;
	}
	
	public boolean isAdded() {
		return added;
	}
	
	public abstract UUID getUUID();
	
	public abstract String getName();

	public abstract GSIPlaybackStream getPlaybackStream();

	public abstract GSICaptureStream getCaptureStream();

	public abstract Iterator<UUID> getDerivedIterator();
	
	public abstract GSAbstractAsset getDerivedAsset(UUID assetUUID);
	
}
