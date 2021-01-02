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

import net.minecraft.util.PacketByteBuf;

public class GSChannel {

	public static final boolean DEFAULT_DISABLED = false;

	private final UUID channelUUID;
	private GSChannelInfo info;
	private boolean disabled;

	private final Map<UUID, GSChannelEntry> entries;

	private GSSequence parent;

	public GSChannel(UUID channelUUID, GSChannelInfo info) {
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

	void setParent(GSSequence parent) {
		if (this.parent != null)
			throw new IllegalStateException("Channel already has a parent");
		this.parent = parent;
	}
	
	public void set(GSChannel other) {
		setInfo(other.getInfo());
		setDisabled(other.isDisabled());

		clear();
		
		for (GSChannelEntry entry : other.getEntries()) {
			GSChannelEntry entryCopy = new GSChannelEntry(entry.getEntryUUID());
			entryCopy.set(entry);
			addEntrySilent(entryCopy);
			dispatchEntryAdded(entryCopy);
		}
	}
	
	private void clear() {
		Iterator<GSChannelEntry> itr = entries.values().iterator();
		while (itr.hasNext()) {
			GSChannelEntry entry = itr.next();
			itr.remove();
			
			dispatchEntryRemoved(entry);
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
		addEntrySilent(entry);
		
		dispatchEntryAdded(entry);
		
		return entry;
	}
	
	private void addEntrySilent(GSChannelEntry entry) {
		entry.setParent(this);
		
		entries.put(entry.getEntryUUID(), entry);
	}
	
	public boolean removeEntry(GSChannelEntry entry) {
		return removeEntry(entry.getEntryUUID());
	}
	
	public boolean removeEntry(UUID entryUUID) {
		GSChannelEntry entry = entries.remove(entryUUID);
		if (entry != null) {
			dispatchEntryRemoved(entry);
			return true;
		}
		
		return false;
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
	
	public static GSChannel read(PacketByteBuf buf) throws IOException {
		UUID channelUUID = buf.readUuid();
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
			channel.addEntrySilent(entry);
		}
		
		return channel;
	}

	public static void write(PacketByteBuf buf, GSChannel channel) throws IOException {
		buf.writeUuid(channel.getChannelUUID());
		GSChannelInfo.write(buf, channel.getInfo());
		
		buf.writeBoolean(channel.isDisabled());
		
		Collection<GSChannelEntry> entries = channel.getEntries();
		buf.writeInt(entries.size());
		for (GSChannelEntry entry : entries)
			GSChannelEntry.write(buf, entry);
	}
}
