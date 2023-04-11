package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSAssetHistory implements GSIAssetHistory {

	private static final byte SUPPORTED_FILE_VERSION = 0x00;
	
	private final SortedSet<GSAssetInfo> infoSet;
	private final Map<UUID, GSAssetInfo> uuidToInfo;
	private final Map<GSAssetHandle, GSAssetInfo> handleToInfo;

	private final List<GSIAssetHistoryListener> listeners;
	
	public GSAssetHistory() {
		infoSet = new TreeSet<>();
		uuidToInfo = new HashMap<>();
		handleToInfo = new HashMap<>();
		
		listeners = new ArrayList<>();
	}
	
	public GSAssetHistory(GSIAssetHistory history) {
		this();
		
		// Asset info is mutable...
		for (GSAssetInfo info : history)
			addImpl(new GSAssetInfo(info));
	}
	
	@Override
	public void addListener(GSIAssetHistoryListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		listeners.add(listener);
	}

	@Override
	public void removeListener(GSIAssetHistoryListener listener) {
		listeners.remove(listener);
	}

	/* Note: when assetUUID is null the whole history changed. */
	private void dispatchHistoryChanged(UUID assetUUID) {
		for (GSIAssetHistoryListener listener : listeners)
			listener.onHistoryChanged(assetUUID);
	}

	@Override
	public boolean contains(UUID assetUUID) {
		return uuidToInfo.containsKey(assetUUID);
	}

	@Override
	public boolean containsHandle(GSAssetHandle handle) {
		return handleToInfo.containsKey(handle);
	}
	
	@Override
	public GSAssetInfo get(UUID assetUUID) {
		return uuidToInfo.get(assetUUID);
	}
	
	@Override
	public GSAssetInfo getFromHandle(GSAssetHandle handle) {
		return handleToInfo.get(handle);
	}

	@Override
	public void add(GSAssetInfo info) {
		addImpl(info);
		dispatchHistoryChanged(info.getAssetUUID());
	}
	
	@Override
	public void addAll(Iterable<GSAssetInfo> iterable) {
		for (GSAssetInfo info : iterable)
			addImpl(new GSAssetInfo(info));
		dispatchHistoryChanged(null);
	}
	
	private void addImpl(GSAssetInfo info) {
		GSAssetInfo oldInfo = get(info.getAssetUUID());
		if (oldInfo != null) {
			infoSet.remove(oldInfo);
			if (oldInfo.getHandle() != null)
				handleToInfo.remove(oldInfo.getHandle());
		}
		infoSet.add(info);
		uuidToInfo.put(info.getAssetUUID(), info);
		if (info.getHandle() != null) {
			if (handleToInfo.containsKey(info.getHandle()))
				throw new IllegalStateException("Duplicate asset handle");
			handleToInfo.put(info.getHandle(), info);
		}
	}
	
	@Override
	public GSAssetInfo remove(UUID assetUUID) {
		GSAssetInfo info = uuidToInfo.remove(assetUUID);
		if (info != null) {
			infoSet.remove(info);
			handleToInfo.remove(info.getHandle());
			dispatchHistoryChanged(info.getAssetUUID());
		}
		return info;
	}
	
	@Override
	public void set(GSIAssetHistory other) {
		clearImpl();
		addAll(other);
	}
	
	@Override
	public void clear() {
		clearImpl();
		dispatchHistoryChanged(null);
	}
	
	private void clearImpl() {
		infoSet.clear();
		uuidToInfo.clear();
		handleToInfo.clear();
	}

	public void setAssetName(UUID assetUUID, String name) {
		GSAssetInfo info = get(assetUUID);
		if (info != null && !name.equals(info.getAssetName())) {
			info.setAssetName(name);
			dispatchHistoryChanged(assetUUID);
		}
	}

	public void setLastModifiedTimestamp(UUID assetUUID, long timestamp) {
		GSAssetInfo info = get(assetUUID);
		if (info != null && timestamp != info.getLastModifiedTimestamp()) {
			// Last modified time is used for the sorting order. We have to
			// remove the info first such that it remains properly sorted.
			infoSet.remove(info);
			info.setLastModifiedTimestamp(timestamp);
			infoSet.add(info);
			dispatchHistoryChanged(assetUUID);
		}
	}

	public void setOwnerUUID(UUID assetUUID, UUID ownerUUID) {
		GSAssetInfo info = get(assetUUID);
		if (info != null && !ownerUUID.equals(info.getOwnerUUID())) {
			info.setOwnerUUID(ownerUUID);
			dispatchHistoryChanged(assetUUID);
		}
	}
	
	@Override
	public int size() {
		return infoSet.size();
	}
	
	@Override
	public boolean isEmpty() {
		return infoSet.isEmpty();
	}

	@Override
	public SortedSet<GSAssetInfo> asCollection() {
		return Collections.unmodifiableSortedSet(infoSet);
	}
	
	@Override
	public Iterator<GSAssetInfo> iterator() {
		return asCollection().iterator();
	}
	
	public static GSAssetHistory read(GSDecodeBuffer buf) throws IOException {
		// Reserved byte for file version
		byte fileVersion = buf.readByte();
		if (fileVersion != SUPPORTED_FILE_VERSION)
			throw new IOException("Unexpected file version " + Integer.toHexString(fileVersion));
		int infoCount = buf.readInt();
		if (infoCount < 0)
			throw new IOException("Corrupted history file");

		GSAssetHistory history = new GSAssetHistory();
		for (int i = 0; i < infoCount; i++)
			history.add(GSAssetInfo.read(buf));
		return history;
	}

	public static void write(GSEncodeBuffer buf, GSIAssetHistory history) throws IOException {
		buf.writeByte(SUPPORTED_FILE_VERSION);
		
		buf.writeInt(history.size());
		for (GSAssetInfo info : history)
			GSAssetInfo.write(buf, info);
	}
}
