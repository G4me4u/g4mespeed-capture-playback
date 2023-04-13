package com.g4mesoft.captureplayback.common.asset;

import java.util.UUID;

public interface GSIAssetStorageListener {

	public void onAssetAdded(UUID assetUUID);

	public void onAssetRemoved(UUID assetUUID);
	
}
