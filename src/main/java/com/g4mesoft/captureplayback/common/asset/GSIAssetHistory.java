package com.g4mesoft.captureplayback.common.asset;

import java.util.Collection;
import java.util.UUID;

public interface GSIAssetHistory extends Iterable<GSAssetInfo> {

	public void addListener(GSIAssetHistoryListener listener);
	
	public void removeListener(GSIAssetHistoryListener listener);

	public boolean contains(UUID assetUUID);

	public boolean containsHandle(GSAssetHandle handle);

	public GSAssetInfo get(UUID assetUUID);

	public GSAssetInfo getFromHandle(GSAssetHandle handle);
	
	public void add(GSAssetInfo info);

	public void addAll(Iterable<GSAssetInfo> iterable);
	
	public GSAssetInfo remove(UUID assetUUID);

	public void set(GSIAssetHistory other);
	
	public void clear();
	
	public int size();

	public boolean isEmpty();
	
	public Collection<GSAssetInfo> asCollection();
	
}
