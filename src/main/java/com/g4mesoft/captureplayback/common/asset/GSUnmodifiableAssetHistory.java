package com.g4mesoft.captureplayback.common.asset;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

public class GSUnmodifiableAssetHistory implements GSIAssetHistory {

	private final GSIAssetHistory history;
	
	public GSUnmodifiableAssetHistory(GSIAssetHistory history) {
		if (history == null)
			throw new IllegalArgumentException("history is null");
		this.history = history;
	}
	
	@Override
	public void addListener(GSIAssetHistoryListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		history.addListener(listener);
	}

	@Override
	public void removeListener(GSIAssetHistoryListener listener) {
		history.removeListener(listener);
	}

	@Override
	public boolean contains(UUID assetUUID) {
		return history.contains(assetUUID);
	}
	
	@Override
	public boolean containsHandle(GSAssetHandle handle) {
		return history.containsHandle(handle);
	}

	@Override
	public GSAssetInfo get(UUID assetUUID) {
		return history.get(assetUUID);
	}
	
	@Override
	public GSAssetInfo getFromHandle(GSAssetHandle handle) {
		return history.getFromHandle(handle);
	}
	
	@Override
	public void add(GSAssetInfo info) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void addAll(Iterable<GSAssetInfo> iterable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public GSAssetInfo remove(UUID assetUUID) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void set(GSIAssetHistory other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int size() {
		return history.size();
	}

	@Override
	public boolean isEmpty() {
		return history.isEmpty();
	}

	@Override
	public Collection<GSAssetInfo> asCollection() {
		// Already unmodifiable in GSAssetHistory.
		return history.asCollection();
	}
	
	@Override
	public Iterator<GSAssetInfo> iterator() {
		// Already unmodifiable in GSAssetHistory.
		return history.iterator();
	}
}
