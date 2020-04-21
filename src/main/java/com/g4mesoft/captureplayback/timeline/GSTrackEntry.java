package com.g4mesoft.captureplayback.timeline;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.util.PacketByteBuf;

public final class GSTrackEntry {

	public static final GSETrackEntryType DEFAULT_ENTRY_TYPE = GSETrackEntryType.EVENT_BOTH;
	public static final GSBlockEventTime DEFAULT_TIME = new GSBlockEventTime(0L, 0);
	
	private final UUID entryUUID;
	
	private GSBlockEventTime startTime;
	private GSBlockEventTime endTime;
	
	private GSETrackEntryType type;

	private GSTrack owner;

	GSTrackEntry(UUID entryUUID) {
		this(entryUUID, DEFAULT_TIME, DEFAULT_TIME);
	}

	GSTrackEntry(UUID entryUUID, GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (entryUUID == null)
			throw new NullPointerException("entryUUID is null");
		
		this.entryUUID = entryUUID;
		
		this.startTime = startTime;
		this.endTime = endTime;
		
		type = DEFAULT_ENTRY_TYPE;

		owner = null;
		
		validateTimespan(startTime, endTime);
	}
	
	public void setOwnerTrack(GSTrack owner) {
		if (this.owner != null)
			throw new IllegalStateException("Entry already has an owner");
		this.owner = owner;
	}
	
	public GSTrack getOwnerTrack() {
		return owner;
	}
	
	public void set(GSTrackEntry other) {
		setTimespan(other.getStartTime(), other.getEndTime());
		setType(other.getType());
	}
	
	private void validateTimespan(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (startTime.isAfter(endTime))
			throw new IllegalArgumentException("Start time is after end time!");
		if (owner != null && owner.isOverlappingEntries(startTime, endTime, this))
			throw new IllegalArgumentException("Timespan is overlapping other track entries!");
	}
	
	public void setTimespan(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		GSBlockEventTime oldStartTime = this.startTime;
		GSBlockEventTime oldEndTime = this.endTime;
		if (!oldStartTime.isEqual(startTime) || !oldEndTime.isEqual(endTime)) {
			validateTimespan(startTime, endTime);

			this.startTime = startTime;
			this.endTime = endTime;
			
			if (owner != null)
				owner.onEntryTimeChanged(this, oldStartTime, oldEndTime);
		}
	}
	
	public long getGametickDuration() {
		return endTime.getGametick() - startTime.getGametick();
	}

	public void setStartTime(GSBlockEventTime startTime) {
		if (startTime.isAfter(endTime))
			throw new IllegalArgumentException("Start time is after current end time!");
		setTimespan(startTime, this.endTime);
	}
	
	public GSBlockEventTime getStartTime() {
		return startTime;
	}

	public void setEndTime(GSBlockEventTime endTime) {
		if (endTime.isBefore(startTime))
			throw new IllegalArgumentException("End time is before current start time!");
		setTimespan(this.startTime, endTime);
	}
	
	public boolean isOverlapping(GSBlockEventTime startTime, GSBlockEventTime endTime) {
		return !startTime.isAfter(this.endTime) && !endTime.isBefore(this.startTime);
	}
	
	public boolean containsTimestamp(GSBlockEventTime time, boolean includeBlockEventDelay) {
		if (includeBlockEventDelay)
			return !startTime.isAfter(time) && !endTime.isBefore(time);
		
		return time.getGametick() >= startTime.getGametick() &&
		       time.getGametick() <= endTime.getGametick();
	}
	
	public GSBlockEventTime getEndTime() {
		return endTime;
	}
	
	public void setType(GSETrackEntryType type) {
		if (type == null)
			throw new NullPointerException();
		
		GSETrackEntryType oldType = this.type;
		if (type != oldType) {
			this.type = type;
			
			if (owner != null)
				owner.onEntryTypeChanged(this, oldType);
		}
	}
	
	public GSETrackEntryType getType() {
		return type;
	}
	
	public UUID getEntryUUID() {
		return entryUUID;
	}

	public static GSTrackEntry read(PacketByteBuf buf) throws IOException {
		GSTrackEntry entry = new GSTrackEntry(buf.readUuid());

		GSBlockEventTime startTime = GSBlockEventTime.read(buf);
		GSBlockEventTime endTime = GSBlockEventTime.read(buf);
		entry.setTimespan(startTime, endTime);
		
		GSETrackEntryType type = GSETrackEntryType.fromIndex(buf.readInt());
		if (type == null)
			throw new IOException("Invalid entry type");
		entry.setType(type);
		
		return entry;
	}

	public static void write(PacketByteBuf buf, GSTrackEntry entry) throws IOException {
		buf.writeUuid(entry.getEntryUUID());
		
		GSBlockEventTime.write(buf, entry.getStartTime());
		GSBlockEventTime.write(buf, entry.getEndTime());

		buf.writeInt(entry.getType().getIndex());
	}
}
