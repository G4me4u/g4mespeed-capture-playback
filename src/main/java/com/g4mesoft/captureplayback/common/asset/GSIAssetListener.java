package com.g4mesoft.captureplayback.common.asset;

import java.util.UUID;

public interface GSIAssetListener {

	public void onNameChanged(String name);

	public void onDerivedAssetAdded(UUID assetUUID);

	public void onDerivedAssetRemoved(UUID assetUUID);
	
}
