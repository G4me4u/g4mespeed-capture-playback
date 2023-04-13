package com.g4mesoft.captureplayback.sequence;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSChannel {

	public static final boolean DEFAULT_DISABLED = false;

	private final UUID channelUUID;
	private GSChannelInfo info;
	private boolean disabled;

	private final Map<UUID, GSChannelEntry> entries;

	private GSSequence parent;

	GSChannel(GSChannel other) {
		this(other.getChannelUUID(), other.getInfo());
		
		disabled = other.isDisabled();
		
		for (GSChannelEntry entry : other.getEntries())
			addEntryInternal(new GSChannelEntry(entry));
	}
	
	GSChannel(UUID channelUUID, GSChannelInfo info) {
		if (channelUUID == null)
			throw new IllegalArgumentException("Channel UUID must not be null!");
		if (info == null)
			throw new IllegalArgumentException("Info must not be null!");

		this.channelUUID = channelUUID;
		this.info = info;
		disabled = DEFAULT_DISABLED;
		
		entries = new LinkedHashMap<>();

		parent = null;
	}
	
	public GSSequence getParent() {
		return parent;
	}

	void onAdded(GSSequence parent) {
		if (this.parent != null)
			throw new IllegalStateException("Channel already has a parent");
		
		this.parent = parent;
	}
	
	void onRemoved(GSSequence parent) {
		if (this.parent != parent)
			throw new IllegalStateException("Channel does not have specified parent");
		
		this.parent = null;
	}
	
	void duplicateFrom(GSChannel other) {
		clear();

		setInfo(other.getInfo());
		setDisabled(other.isDisabled());
		
		for (GSChannelEntry entry : other.getEntries()) {
			UUID entryUUID = GSUUIDUtil.randomUnique(this::hasEntryUUID);
			GSChannelEntry entryCopy = new GSChannelEntry(entryUUID,
					entry.getStartTime(), entry.getEndTime());
		
			addEntryInternal(entryCopy);
			dispatchEntryAdded(entryCopy);
			entryCopy.set(entry);
		}
	}
	
	void set(GSChannel other) {
		clear();

		setInfo(other.getInfo());
		setDisabled(other.isDisabled());
		
		for (GSChannelEntry entry : other.getEntries()) {
			GSChannelEntry entryCopy = new GSChannelEntry(entry.getEntryUUID(),
					entry.getStartTime(), entry.getEndTime());
		
			addEntryInternal(entryCopy);
			dispatchEntryAdded(entryCopy);
			entryCopy.set(entry);
		}
	}
	
	private void clear() {
		Iterator<GSChannelEntry> itr = entries.values().iterator();
		while (itr.hasNext()) {
			GSChannelEntry entry = itr.next();
			itr.remove();
			onEntryRemoved(entry);
		}
	}
	
	public GSChannelEntry tryAddEntry(GSSignalTime startTime, GSSignalTime endTime) {
		return tryAddEntry(GSUUIDUtil.randomUnique(this::hasEntryUUID), startTime, endTime);
	}
	
	public GSChannelEntry tryAddEntry(UUID entryUUID, GSSignalTime startTime, GSSignalTime endTime) {
		if (entryUUID == null || hasEntryUUID(entryUUID))
			return null;
		if (isOverlappingEntries(startTime, endTime, null))
			return null;
		
		GSChannelEntry entry = new GSChannelEntry(entryUUID, startTime, endTime);
		addEntryInternal(entry);
		
		dispatchEntryAdded(entry);
		
		return entry;
	}
	
	private void addEntryInternal(GSChannelEntry entry) {
		entry.onAdded(this);
		
		entries.put(entry.getEntryUUID(), entry);
	}
	
	public boolean removeEntry(UUID entryUUID) {
		GSChannelEntry entry = entries.remove(entryUUID);
		if (entry != null) {
			onEntryRemoved(entry);
			return true;
		}
		
		return false;
	}
	
	private void onEntryRemoved(GSChannelEntry entry) {
		dispatchEntryRemoved(entry);
		// Ensure that events are no longer heard by
		// the registered listeners.
		entry.onRemoved(this);
	}
	
	public boolean isOverlappingEntries(GSSignalTime startTime, GSSignalTime endTime, GSChannelEntry ignoreEntry) {
		if (startTime.isAfter(endTime))
			return false;
		
		for (GSChannelEntry other : entries.values()) {
			if (other != ignoreEntry && other.isOverlapping(startTime, endTime))
				return true;
		}
		return false;
	}
	
	public GSChannelEntry getEntryAt(GSSignalTime time, boolean preciseSearch) {
		for (GSChannelEntry entry : entries.values()) {
			if (entry.containsTimestamp(time, preciseSearch))
				return entry;
		}
		
		return null;
	}

	public void setInfo(GSChannelInfo info) {
		if (info == null)
			throw new NullPointerException("Info must not be null!");
		
		GSChannelInfo oldInfo = this.info;
		if (!oldInfo.equals(info)) {
			this.info = info;
			
			dispatchChannelInfoChanged(this, oldInfo);
		}
	}
	
	public GSChannelInfo getInfo() {
		return info;
	}
	
	public void setDisabled(boolean disabled) {
		boolean oldDisabled = this.disabled;
		if (oldDisabled != disabled) {
			this.disabled = disabled;
			
			dispatchChannelDisabledChanged(this, oldDisabled);
		}
	}
	
	public boolean isDisabled() {
		return disabled;
	}

	public UUID getChannelUUID() {
		return channelUUID;
	}
	
	public GSChannelEntry getEntry(UUID entryUUID) {
		return entries.get(entryUUID);
	}
	
	public boolean hasEntryUUID(UUID entryUUID) {
		return entries.containsKey(entryUUID);
	}
	
	public Set<UUID> getEntryUUIDs() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	public Collection<GSChannelEntry> getEntries() {
		return Collections.unmodifiableCollection(entries.values());
	}
	
	private void dispatchChannelInfoChanged(GSChannel channel, GSChannelInfo oldInfo) {
		if (parent != null) {
			for (GSISequenceListener listener : parent.getListeners())
				listener.channelInfoChanged(channel, oldInfo);
		}
	}

	private void dispatchChannelDisabledChanged(GSChannel channel, boolean oldDisabled) {
		if (parent != null) {
			for (GSISequenceListener listener : parent.getListeners())
				listener.channelDisabledChanged(channel, oldDisabled);
		}
	}
	
	private void dispatchEntryAdded(GSChannelEntry entry) {
		if (parent != null) {
			for (GSISequenceListener listener : parent.getListeners())
				listener.entryAdded(entry);
		}
	}
	
	private void dispatchEntryRemoved(GSChannelEntry entry) {
		if (parent != null) {
			for (GSISequenceListener listener : parent.getListeners())
				listener.entryRemoved(entry);
		}
	}
	
	public static GSChannel read(GSDecodeBuffer buf) throws IOException {
		UUID channelUUID = buf.readUUID();
		GSChannelInfo info = GSChannelInfo.read(buf);
		GSChannel channel = new GSChannel(channelUUID, info);

		channel.setDisabled(buf.readBoolean());
		
		int entryCount = buf.readInt();
		while (entryCount-- != 0) {
			GSChannelEntry entry = GSChannelEntry.read(buf);
			if (channel.hasEntryUUID(entry.getEntryUUID()))
				throw new IOException("Duplicate entry UUID");
			if (channel.isOverlappingEntries(entry.getStartTime(), entry.getEndTime(), null))
				throw new IOException("Overlapping entry");
			channel.addEntryInternal(entry);
		}
		
		return channel;
	}

	public static void write(GSEncodeBuffer buf, GSChannel channel) throws IOException {
		buf.writeUUID(channel.getChannelUUID());
		GSChannelInfo.write(buf, channel.getInfo());
		
		buf.writeBoolean(channel.isDisabled());
		
		Collection<GSChannelEntry> entries = channel.getEntries();
		buf.writeInt(entries.size());
		for (GSChannelEntry entry : entries)
			GSChannelEntry.write(buf, entry);
	}
}
