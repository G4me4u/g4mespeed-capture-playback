package com.g4mesoft.captureplayback.panel.composition;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSRectangle;

public class GSCompositionModelView implements GSICompositionListener, GSISequenceListener {

	private static final int TRACK_HEIGHT = 50;
	private static final int TRACK_SPACING =  1;
	
	private static final int MINIMUM_SEQUENCE_DURATION = 1;
	
	private final GSComposition model;
	
	private final Map<UUID, Integer> trackUUIDToIndex;
	private final Map<Integer, UUID> trackIndexToUUID;
	private final Map<UUID, Long> sequenceDurations;
	
	private int xOffset;
	private int yOffset;

	public GSCompositionModelView(GSComposition model) {
		this.model = model;
		
		trackUUIDToIndex = new HashMap<>();
		trackIndexToUUID = new HashMap<>();
		sequenceDurations = new HashMap<>();
	
		yOffset = 0;
	}
	
	/* ******************** MODEL-VIEW initialization ******************** */

	public void installListeners() {
		model.addCompositionListener(this);
		for (GSSequence sequence : model.getSequences())
			sequence.addSequenceListener(this);
	}

	public void uninstallListeners() {
		model.addCompositionListener(this);
		for (GSSequence sequence : model.getSequences())
			sequence.removeSequenceListener(this);
	}
	
	public void updateModelView() {
		updateTrackIndexLookup();
		updateAllSequenceDurations();
	}
	
	private void updateTrackIndexLookup() {
		trackUUIDToIndex.clear();
		trackIndexToUUID.clear();
		
		int trackIndex = 0;
		for (UUID trackUUID : model.getTrackUUIDs()) {
			trackUUIDToIndex.put(trackUUID, trackIndex);
			trackIndexToUUID.put(trackIndex, trackUUID);
			trackIndex++;
		}
	}
	
	private void updateAllSequenceDurations() {
		for (GSSequence sequence : model.getSequences())
			updateSequenceDuration(sequence);
	}
	
	private void updateSequenceDuration(GSSequence sequence) {
		GSSignalTime earliestTime = GSSignalTime.INFINITY;
		GSSignalTime latestTime = GSSignalTime.ZERO;
		
		for (GSChannel channel : sequence.getChannels()) {
			for (GSChannelEntry entry : channel.getEntries()) {
				if (entry.getStartTime().isBefore(earliestTime))
					earliestTime = entry.getStartTime();
				if (entry.getEndTime().isAfter(latestTime))
					latestTime = entry.getEndTime();
			}
		}
		
		long duration = latestTime.getGametick() - earliestTime.getGametick();
		if (duration < MINIMUM_SEQUENCE_DURATION)
			duration = MINIMUM_SEQUENCE_DURATION;
		
		sequenceDurations.put(sequence.getSequenceUUID(), duration);
	}

	/* ******************** MODEL-VIEW lookup ******************** */

	private long getSequenceDuration(UUID sequenceUUID) {
		Long duration = sequenceDurations.get(sequenceUUID);
		return (duration != null) ? duration.longValue() : -1L;
	}
	
	/* ******************** MODEL TO VIEW methods ******************** */

	public GSRectangle viewToModel(GSTrackEntry entry) {
		return viewToModel(entry, null);
	}
	
	public GSRectangle viewToModel(GSTrackEntry entry, GSRectangle dest) {
		long duration = getSequenceDuration(entry.getSequenceUUID());

		if (duration == -1L)
			return null;

		if (dest == null)
			dest = new GSRectangle();
		
		dest.x = getGametickX(entry.getOffset());
		dest.width = getGametickX(duration + entry.getOffset()) - dest.x;

		dest.y = getTrackY(entry.getParent().getTrackUUID());
		dest.height = getTrackHeight();
		
		return dest;
	}
	
	public int getGametickX(long offset) {
		// TODO: allow for zooming etc.
		return (int)offset * 8;
	}
	
	public int getTrackY(UUID trackUUID) {
		Integer trackIndex = trackUUIDToIndex.get(trackUUID);
		if (trackIndex == null)
			return -1;
		
		return yOffset + trackIndex.intValue() * (TRACK_HEIGHT + TRACK_SPACING);
	}

	/* ******************** VIEW TO MODEL methods ******************** */

	/* ******************** GETTER & SETTER methods ******************** */
	
	public int getTrackHeight() {
		return TRACK_HEIGHT;
	}

	public int getTrackSpacing() {
		return TRACK_SPACING;
	}
	
	public int getXOffset() {
		return xOffset;
	}

	public void setXOffset(int xOffset) {
		if (xOffset != this.xOffset) {
			this.xOffset = xOffset;
			dispatchModelViewChangedEvent();
		}
	}

	public int getYOffset() {
		return yOffset;
	}

	public void setYOffset(int yOffset) {
		if (yOffset != this.yOffset) {
			this.yOffset = yOffset;
			dispatchModelViewChangedEvent();
		}
	}
	
	private void dispatchModelViewChangedEvent() {
		// TODO: implement some listener methods
	}
	
	/* ******************** INHERITED LISTENER methods ******************** */
	
	@Override
	public void sequenceAdded(GSSequence sequence) {
		updateSequenceDuration(sequence);
		
		sequence.addSequenceListener(this);
	}
	
	@Override
	public void sequenceRemoved(GSSequence sequence) {
		sequenceDurations.remove(sequence.getSequenceUUID());

		sequence.removeSequenceListener(this);
	}

	@Override
	public void trackAdded(GSTrack track) {
		updateTrackIndexLookup();
	}

	@Override
	public void trackRemoved(GSTrack track) {
		updateTrackIndexLookup();
	}
	
	public void channelAdded(GSChannel channel) {
		updateSequenceDuration(channel.getParent());
	}

	public void channelRemoved(GSChannel channel) {
		updateSequenceDuration(channel.getParent());
	}
	
	public void entryAdded(GSChannelEntry entry) {
		updateSequenceDuration(entry.getParent().getParent());
	}

	public void entryRemoved(GSChannelEntry entry) {
		updateSequenceDuration(entry.getParent().getParent());
	}

	public void entryTimeChanged(GSChannelEntry entry, GSSignalTime oldStart, GSSignalTime oldEnd) {
		updateSequenceDuration(entry.getParent().getParent());
	}
}
