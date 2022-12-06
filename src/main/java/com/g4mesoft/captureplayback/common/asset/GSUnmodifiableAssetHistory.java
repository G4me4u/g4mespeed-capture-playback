package com.g4mesoft.captureplayback.common.asset;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.UUID;

public class GSUnmodifiableAssetHistory implements GSIAssetHistory {

	private final GSAssetHistory history;
	
	public GSUnmodifiableAssetHistory(GSAssetHistory history) {
		if (history == null)
			throw new IllegalArgumentException("history is null");
		this.history = history;
	}
	
	@Override
	public void addListener(GSIAssetHistoryListener listener) {
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
	public GSAssetInfo get(UUID assetUUID) {
		return history.get(assetUUID);
	}
	
	@Override
	public void add(GSAssetInfo info) {
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
	public SortedSet<GSAssetInfo> getInfoSet() {
		// Already unmodifiable in GSAssetHistory.
		return history.getInfoSet();
	}
	
	@Override
	public Iterator<GSAssetInfo> iterator() {
		return getInfoSet().iterator();
	}
}
