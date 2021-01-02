package com.g4mesoft.captureplayback.composition;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.util.PacketByteBuf;

public class GSRocket {

	private final UUID rocketUUID;
	private String name;
	
	private final Map<UUID, GSRocketEntry> entries;
	
	private GSComposition parent;
	
	public GSRocket(UUID rocketUUID, String name) {
		if (rocketUUID == null)
			throw new IllegalArgumentException("rocketUUID is null");
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		this.rocketUUID = rocketUUID;
		this.name = name;
		
		entries = new LinkedHashMap<>();
		
		parent = null;
	}
	
	public GSComposition getParent() {
		return parent;
	}

	void setParent(GSComposition parent) {
		if (this.parent != null)
			throw new IllegalStateException("Rocket already has a parent");
		this.parent = parent;
	}
	
	public UUID getRocketUUID() {
		return rocketUUID;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		this.name = name;
	}
	
	public GSRocketEntry getEntry(UUID entryUUID) {
		return entries.get(entryUUID);
	}
	
	public boolean hasEntryUUID(UUID entryUUID) {
		return entries.containsKey(entryUUID);
	}
	
	public Set<UUID> getEntryUUIDs() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	public Collection<GSRocketEntry> getEntries() {
		return Collections.unmodifiableCollection(entries.values());
	}
	
	public boolean isComplete() {
		if (parent == null)
			return false;
		
		for (GSRocketEntry entry : getEntries()) {
			if (!parent.hasSequenceUUID(entry.getSequenceUUID()))
				return false;
		}
		
		return true;
	}
	
	public static GSRocket read(PacketByteBuf buf) throws IOException {
		UUID rocketUUID = buf.readUuid();
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		GSRocket rocket = new GSRocket(rocketUUID, name);
		
		return rocket;
	}

	public static void write(PacketByteBuf buf, GSRocket rocket) throws IOException {
		buf.writeUuid(rocket.getRocketUUID());
		buf.writeString(rocket.getName());
	}
}
