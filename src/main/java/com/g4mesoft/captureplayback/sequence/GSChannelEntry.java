package com.g4mesoft.captureplayback.sequence;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;

import net.minecraft.util.PacketByteBuf;

public final class GSChannelEntry {

	public static final GSEChannelEntryType DEFAULT_ENTRY_TYPE = GSEChannelEntryType.EVENT_BOTH;
	public static final GSSignalTime DEFAULT_TIME = new GSSignalTime(0L, 0);
	
	private final UUID entryUUID;
	
	private GSSignalTime startTime;
	private GSSignalTime endTime;
	
	private GSEChannelEntryType type;

	private GSChannel owner;

	GSChannelEntry(UUID entryUUID) {
		this(entryUUID, DEFAULT_TIME, DEFAULT_TIME);
	}

	GSChannelEntry(UUID entryUUID, GSSignalTime startTime, GSSignalTime endTime) {
		if (entryUUID == null)
			throw new NullPointerException("entryUUID is null");
		
		this.entryUUID = entryUUID;
		
		this.startTime = startTime;
		this.endTime = endTime;
		
		type = DEFAULT_ENTRY_TYPE;

		owner = null;
		
		validateTimespan(startTime, endTime);
	}

	public GSChannel getParent() {
		return owner;
	}
	
	void setParent(GSChannel parent) {
		if (this.owner != null)
			throw new IllegalStateException("Entry already has a parent");
		this.owner = parent;
	}
	
	public void set(GSChannelEntry other) {
		setTimespan(other.getStartTime(), other.getEndTime());
		setType(other.getType());
	}
	
	private void validateTimespan(GSSignalTime startTime, GSSignalTime endTime) {
		if (startTime.isAfter(endTime))
			throw new IllegalArgumentException("Start time is after end time!");
		if (owner != null && owner.isOverlappingEntries(startTime, endTime, this))
			throw new IllegalArgumentException("Timespan is overlapping other channel entries!");
	}
	
	public void setTimespan(GSSignalTime startTime, GSSignalTime endTime) {
		GSSignalTime oldStartTime = this.startTime;
		GSSignalTime oldEndTime = this.endTime;
		if (!oldStartTime.isEqual(startTime) || !oldEndTime.isEqual(endTime)) {
			validateTimespan(startTime, endTime);

			this.startTime = startTime;
			this.endTime = endTime;
			
			dispatchEntryTimeChanged(this, oldStartTime, oldEndTime);
		}
	}
	
	public long getGametickDuration() {
		return endTime.getGametick() - startTime.getGametick();
	}

	public void setStartTime(GSSignalTime startTime) {
		if (startTime.isAfter(endTime))
			throw new IllegalArgumentException("Start time is after current end time!");
		setTimespan(startTime, this.endTime);
	}
	
	public GSSignalTime getStartTime() {
		return startTime;
	}

	public void setEndTime(GSSignalTime endTime) {
		if (endTime.isBefore(startTime))
			throw new IllegalArgumentException("End time is before current start time!");
		setTimespan(this.startTime, endTime);
	}
	
	public boolean isOverlapping(GSSignalTime startTime, GSSignalTime endTime) {
		return !startTime.isAfter(this.endTime) && !endTime.isBefore(this.startTime);
	}
	
	public boolean containsTimestamp(GSSignalTime time, boolean includeBlockEventDelay) {
		if (includeBlockEventDelay)
			return !startTime.isAfter(time) && !endTime.isBefore(time);
		
		return time.getGametick() >= startTime.getGametick() &&
		       time.getGametick() <= endTime.getGametick();
	}
	
	public GSSignalTime getEndTime() {
		return endTime;
	}
	
	public void setType(GSEChannelEntryType type) {
		if (type == null)
			throw new NullPointerException();
		
		GSEChannelEntryType oldType = this.type;
		if (type != oldType) {
			this.type = type;
			
			dispatchEntryTypeChanged(this, oldType);
		}
	}
	
	public GSEChannelEntryType getType() {
		return type;
	}
	
	public UUID getEntryUUID() {
		return entryUUID;
	}

	private void dispatchEntryTimeChanged(GSChannelEntry entry, GSSignalTime oldStart, GSSignalTime oldEnd) {
		if (owner != null && owner.getParent() != null) {
			for (GSISequenceListener listener : owner.getParent().getListeners())
				listener.entryTimeChanged(entry, oldStart, oldEnd);
		}
	}
	
	private void dispatchEntryTypeChanged(GSChannelEntry entry, GSEChannelEntryType oldType) {
		if (owner != null && owner.getParent() != null) {
			for (GSISequenceListener listener : owner.getParent().getListeners())
				listener.entryTypeChanged(entry, oldType);
		}
	}
	
	public static GSChannelEntry read(PacketByteBuf buf) throws IOException {
		GSChannelEntry entry = new GSChannelEntry(buf.readUuid());

		GSSignalTime startTime = GSSignalTime.read(buf);
		GSSignalTime endTime = GSSignalTime.read(buf);
		entry.setTimespan(startTime, endTime);
		
		GSEChannelEntryType type = GSEChannelEntryType.fromIndex(buf.readInt());
		if (type == null)
			throw new IOException("Invalid entry type");
		entry.setType(type);
		
		return entry;
	}

	public static void write(PacketByteBuf buf, GSChannelEntry entry) throws IOException {
		buf.writeUuid(entry.getEntryUUID());
		
		GSSignalTime.write(buf, entry.getStartTime());
		GSSignalTime.write(buf, entry.getEndTime());

		buf.writeInt(entry.getType().getIndex());
	}
}
