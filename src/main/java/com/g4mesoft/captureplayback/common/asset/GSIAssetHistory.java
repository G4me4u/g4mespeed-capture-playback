package com.g4mesoft.captureplayback.common.asset;

import java.util.SortedSet;
import java.util.UUID;

public interface GSIAssetHistory extends Iterable<GSAssetInfo> {

	public void addListener(GSIAssetHistoryListener listener);
	
	public void removeListener(GSIAssetHistoryListener listener);

	public boolean contains(UUID assetUUID);

	public GSAssetInfo get(UUID assetUUID);
	
	public void add(GSAssetInfo info);

	public GSAssetInfo remove(UUID assetUUID);

	public void set(GSIAssetHistory other);
	
	public void clear();
	
	public int size();

	public boolean isEmpty();

	public SortedSet<GSAssetInfo> getInfoSet();
	
}
