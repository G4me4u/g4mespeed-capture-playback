package com.g4mesoft.captureplayback.playlist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.util.GSMutableLinkedHashMap;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSPlaylist {

	private final UUID playlistUUID;
	private String name;
	
	private final GSTrigger trigger;
	private final GSMutableLinkedHashMap<UUID, GSPlaylistEntry> entries;
	
	private List<GSIPlaylistListener> listeners;
	
	public GSPlaylist(GSPlaylist other) {
		this(other.getPlaylistUUID(), other.getName(), new GSTrigger(other.getTrigger()));

		for (GSPlaylistEntry entry : other.getEntries())
			addEntryInternal(new GSPlaylistEntry(entry));
	}
	
	public GSPlaylist(UUID playlistUUID) {
		this(playlistUUID, "");
	}

	public GSPlaylist(UUID playlistUUID, String name) {
		this(playlistUUID, name, new GSTrigger(GSETriggerType.UNSPECIFIED, GSUnspecifiedPlaylistData.INSTANCE));
	}
	
	private GSPlaylist(UUID playlistUUID, String name, GSTrigger trigger) {
		if (playlistUUID == null)
			throw new IllegalArgumentException("playlistUUID is null!");
		if (name == null)
			throw new IllegalArgumentException("name is null!");
		
		this.playlistUUID = playlistUUID;
		this.name = name;
		
		this.trigger = trigger;
		entries = new GSMutableLinkedHashMap<>();
		
		// Lazily initialized when adding a listener
		listeners = null;
	}
	
	public void duplicateFrom(GSPlaylist other) {
		trigger.duplicateFrom(other.getTrigger());
		
		for (GSPlaylistEntry entry : other.getEntries()) {
			GSPlaylistEntry newEntry = addEntry(entry.getType(), entry.getData());
			// Note: newEntry#duplicateFrom is not required since the type
			// and data should already be set, but just in case.
			newEntry.duplicateFrom(entry);
		}
	}

	public UUID getPlaylistUUID() {
		return playlistUUID;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		// Note: deliberate null-pointer exception
		if (!name.equals(this.name)) {
			String oldName = this.name;
			this.name = name;
			
			dispatchPlaylistNameChanged(oldName);
		}
	}

	public GSTrigger getTrigger() {
		return trigger;
	}
	
	public GSPlaylistEntry getEntry(UUID entryUUID) {
		return entries.get(entryUUID);
	}
	
	public boolean hasEntryUUID(UUID entryUUID) {
		return entries.containsKey(entryUUID);
	}
	
	public Set<UUID> getEntryUUIDs() {
		return Collections.unmodifiableSet(entries.keySet());
	}
	
	public Collection<GSPlaylistEntry> getEntries() {
		return Collections.unmodifiableCollection(entries.values());
	}

	public GSPlaylistEntry addEntry(GSEPlaylistEntryType type) {
		return addEntry(type, GSUnspecifiedPlaylistData.INSTANCE);
	}

	public GSPlaylistEntry addEntry(GSEPlaylistEntryType type, GSIPlaylistData data) {
		return addEntry(GSUUIDUtil.randomUnique(this::hasEntryUUID), type, data);
	}
	
	public GSPlaylistEntry addEntry(UUID entryUUID, GSEPlaylistEntryType type, GSIPlaylistData data) {
		if (hasEntryUUID(entryUUID))
			throw new IllegalStateException("Duplicate entry UUID");
		
		GSPlaylistEntry entry = new GSPlaylistEntry(entryUUID, type, data);
		addEntryInternal(entry);
		
		GSPlaylistEntry prevEntry = getPreviousEntry(entryUUID);
		UUID prevUUID = (prevEntry == null) ? null : prevEntry.getEntryUUID();
		dispatchEntryAdded(entry, prevUUID);
		
		return entry;
	}
	
	private void addEntryInternal(GSPlaylistEntry entry) {
		entry.onAdded(this);
		
		entries.put(entry.getEntryUUID(), entry);
	}
	
	public boolean removeEntry(UUID entryUUID) {
		GSPlaylistEntry prevEntry = getPreviousEntry(entryUUID);
		GSPlaylistEntry entry = entries.remove(entryUUID);
		if (entry != null) {
			UUID prevUUID = (prevEntry == null) ? null : prevEntry.getEntryUUID();

			onEntryRemoved(entry, prevUUID);
			return true;
		}
		
		return false;
	}
	
	private void onEntryRemoved(GSPlaylistEntry entry, UUID oldPrevUUID) {
		dispatchEntryRemoved(entry, oldPrevUUID);
		// Ensure that changes to the entry are no longer
		// heard by the registered listeners.
		entry.onRemoved(this);
	}
	
	public void moveEntryBefore(UUID entryUUID, UUID newNextUUID) {
		GSPlaylistEntry prevEntry = getPreviousEntry(newNextUUID);
		moveEntryAfter(entryUUID, (prevEntry == null) ? null : prevEntry.getEntryUUID());
	}

	public void moveEntryAfter(UUID entryUUID, UUID newPrevUUID) {
		if (entryUUID != null) {
			Map.Entry<UUID, GSPlaylistEntry> prevEntry = entries.getPreviousEntry(entryUUID);
			UUID oldPrevUUID = (prevEntry == null) ? null : prevEntry.getKey();
	
			if (!entryUUID.equals(newPrevUUID)) {
				Map.Entry<UUID, GSPlaylistEntry> entry = entries.moveAfter(entryUUID, newPrevUUID);
				if (entry != null)
					dispatchEntryMoved(entry.getValue(), newPrevUUID, oldPrevUUID);
			}
		}
	}
	
	public GSPlaylistEntry getPreviousEntry(UUID entryUUID) {
		Map.Entry<UUID, GSPlaylistEntry> prevEntry = entries.getPreviousEntry(entryUUID);
		return (prevEntry == null) ? null : prevEntry.getValue();
	}

	public GSPlaylistEntry getNextEntry(UUID entryUUID) {
		Map.Entry<UUID, GSPlaylistEntry> nextEntry = entries.getNextEntry(entryUUID);
		return (nextEntry == null) ? null : nextEntry.getValue();
	}
	
	public void addPlaylistListener(GSIPlaylistListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		if (listeners == null)
			listeners = new ArrayList<>();
		listeners.add(listener);
	}

	public void removePlaylistListener(GSIPlaylistListener listener) {
		if (listeners != null)
			listeners.remove(listener);
	}
	
	/* Visible to allow events from playlist entries */
	Iterable<GSIPlaylistListener> getListeners() {
		return (listeners == null) ? Collections.emptyList() : listeners;
	}
	
	private void dispatchPlaylistNameChanged(String oldName) {
		for (GSIPlaylistListener listener : getListeners())
			listener.playlistNameChanged(oldName);
	}

	private void dispatchEntryAdded(GSPlaylistEntry entry, UUID prevUUID) {
		for (GSIPlaylistListener listener : getListeners())
			listener.entryAdded(entry, prevUUID);
	}

	private void dispatchEntryRemoved(GSPlaylistEntry entry, UUID oldPrevUUID) {
		for (GSIPlaylistListener listener : getListeners())
			listener.entryRemoved(entry, oldPrevUUID);
	}
	
	private void dispatchEntryMoved(GSPlaylistEntry entry, UUID newPrevUUID, UUID oldPrevUUID) {
		for (GSIPlaylistListener listener : getListeners())
			listener.entryMoved(entry, newPrevUUID, oldPrevUUID);
	}

	public static GSPlaylist read(GSDecodeBuffer buf) throws IOException {
		// Skip reserved byte
		buf.readByte();

		UUID playlistUUID = buf.readUUID();
		String name = buf.readString();
		GSTrigger trigger = GSTrigger.read(buf);
		
		GSPlaylist playlist = new GSPlaylist(playlistUUID, name, trigger);
		
		int entryCount = buf.readInt();
		while (entryCount-- != 0) {
			GSPlaylistEntry entry = GSPlaylistEntry.read(buf);
			if (playlist.hasEntryUUID(entry.getEntryUUID()))
				throw new IOException("Duplicate entry UUID.");
			playlist.addEntryInternal(entry);
		}
		
		return playlist;
	}

	public static void write(GSEncodeBuffer buf, GSPlaylist playlist) throws IOException {
		// Reserved for future use
		buf.writeByte((byte)0x00);
		
		buf.writeUUID(playlist.getPlaylistUUID());
		buf.writeString(playlist.getName());
		GSTrigger.write(buf, playlist.getTrigger());
		
		Collection<GSPlaylistEntry> entries = playlist.getEntries();
		buf.writeInt(entries.size());
		for (GSPlaylistEntry entry : entries)
			GSPlaylistEntry.write(buf, entry);
	}
}
