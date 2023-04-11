package com.g4mesoft.captureplayback.common.asset;

public class GSAssetRef {

	private final GSAssetStorage storage;
	
	private GSAbstractAsset asset;
	private int cnt;
	
	/* Visible for GSAssetStorage */
	GSAssetRef(GSAssetStorage storage, GSAbstractAsset asset) {
		if (storage == null)
			throw new IllegalArgumentException("storage is null");
		if (asset == null)
			throw new IllegalArgumentException("asset is null");
		
		this.storage = storage;

		this.asset = asset;
		cnt = 1;
	}
	
	private void checkValid() {
		if (!valid())
			throw new IllegalStateException("Reference is no longer valid");
	}
	
	public GSAbstractAsset get() {
		checkValid();
		return asset;
	}

	public GSAssetRef retain() {
		return retain(1);
	}

	public GSAssetRef retain(int amount) {
		checkValid();
		if (amount <= 0)
			throw new IllegalArgumentException("amount must be positive!");
		cnt += amount;
		return this;
	}

	public void release() {
		release(1);
	}
	
	public void release(int amount) {
		checkValid();
		if (amount <= 0)
			throw new IllegalArgumentException("amount must be positive!");
		if (cnt < amount)
			throw new IllegalStateException("released too many times");
		cnt -= amount;
		// Unload the asset from the storage
		if (cnt == 0) {
			storage.unloadAsset(asset.getUUID());
			invalidate();
		}
	}

	/* Visible for GSAssetStorage */
	void invalidate() {
		asset = null;
	}
	
	public boolean valid() {
		return asset != null;
	}
	
	public int remaining() {
		return cnt;
	}
}
