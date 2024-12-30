package com.g4mesoft.captureplayback.playlist;

public abstract class GSAbstractPlaylistEntry<T extends GSIPlaylistEntryType> {

	private T type;
	private GSIPlaylistData data;
	
	private GSPlaylist parent;
	
	public GSAbstractPlaylistEntry(T type, GSIPlaylistData data) {
		set(type, data);
	}

	public GSPlaylist getParent() {
		return parent;
	}

	void onAdded(GSPlaylist parent) {
		if (this.parent != null)
			throw new IllegalStateException("Entry already has a parent");
		
		this.parent = parent;
	}
	
	void onRemoved(GSPlaylist parent) {
		if (this.parent != parent)
			throw new IllegalStateException("Entry does not have specified parent");
		
		this.parent = null;
	}
	
	void duplicateFrom(GSAbstractPlaylistEntry<T> other) {
		set(other.getType(), other.getData());
	}
	
	public T getType() {
		return type;
	}
	
	public GSIPlaylistData getData() {
		return data;
	}

	public void set(T type) {
		set(type, GSUnspecifiedPlaylistData.INSTANCE);
	}
	
	public void set(T type, GSIPlaylistData data) {
		if (type == null)
			throw new IllegalArgumentException("type is null!");
		if (data == null || data.getClass() != type.getDataClazz())
			throw new IllegalArgumentException("mismatching data type!");

		if (type != this.type || !data.equals(this.data)) {
			T oldType = this.type;
			GSIPlaylistData oldData = this.data;
			this.type = type;
			this.data = data;
			dispatchDataChanged(oldType, oldData);
		}
	}
	
	protected abstract void dispatchDataChanged(T oldType, GSIPlaylistData oldData);

}
