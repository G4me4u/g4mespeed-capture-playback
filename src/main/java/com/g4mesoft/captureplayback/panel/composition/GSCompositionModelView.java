package com.g4mesoft.captureplayback.panel.composition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;
import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.ui.panel.GSRectangle;
import com.g4mesoft.ui.util.GSMathUtil;

public class GSCompositionModelView implements GSICompositionListener, GSISequenceListener {

	private static final int TRACK_HEIGHT = 50;
	private static final int TRACK_SPACING =  1;
	
	private static final int MINIMUM_SEQUENCE_DURATION = 1;
	private static final double DEFAULT_GAMETICK_WIDTH = 8.0;
	private static final double MIN_GAMETICK_WIDTH = 0.01;
	private static final double MAX_GAMETICK_WIDTH = 75.0;
	
	private static final int MIN_TIME_INDICATOR_WIDTH = 75;
	public static final long TIME_SUB_INDICATOR_COUNT = 5;
	
	private final GSComposition model;
	
	private final Map<UUID, Integer> trackUUIDToIndex;
	private final Map<Integer, UUID> trackIndexToUUID;
	private final Map<UUID, Long> sequenceDurations;
	
	private long minimumGametickCount;

	private long timeIndicatorInterval;
	private double gametickWidth;
	
	private final List<GSIModelViewListener> listenters;
	
	public GSCompositionModelView(GSComposition model) {
		this.model = model;
		
		trackUUIDToIndex = new HashMap<>();
		trackIndexToUUID = new HashMap<>();
		sequenceDurations = new HashMap<>();
		
		minimumGametickCount = 0L;
		
		timeIndicatorInterval = 1L;
		gametickWidth = DEFAULT_GAMETICK_WIDTH;

		listenters = new ArrayList<>();
		
		calculateTimeIndicatorInterval();
	}
	
	/* ******************** MODEL-VIEW initialization ******************** */

	public void installListeners() {
		model.addCompositionListener(this);
		for (GSTrack track : model.getTracks())
			track.getSequence().addSequenceListener(this);
	}

	public void uninstallListeners() {
		model.removeCompositionListener(this);
		for (GSTrack track : model.getTracks())
			track.getSequence().removeSequenceListener(this);
	}
	
	public void updateModelView() {
		updateTrackIndexLookup();
		updateAllSequenceDurations();
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
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
		for (GSTrack track : model.getTracks())
			updateSequenceDuration(track.getSequence());
	}
	
	private void updateSequenceDuration(GSSequence sequence) {
		GSSignalTime latestTime = GSSignalTime.ZERO;
		
		for (GSChannel channel : sequence.getChannels()) {
			for (GSChannelEntry entry : channel.getEntries()) {
				if (entry.getEndTime().isAfter(latestTime))
					latestTime = entry.getEndTime();
			}
		}
		
		// Notice: +1 since (0) is inclusive.
		long duration = latestTime.getGametick() + 1;
		if (duration < MINIMUM_SEQUENCE_DURATION)
			duration = MINIMUM_SEQUENCE_DURATION;

		sequenceDurations.put(sequence.getSequenceUUID(), duration);
	}
	
	private void updateMinimumGametickCount() {
		minimumGametickCount = 0L;
		for (GSTrack track : model.getTracks()) {
			long duration = getSequenceDuration(track.getSequence());
			
			for (GSTrackEntry entry : track.getEntries()) {
				if (entry.getOffset() + duration > minimumGametickCount)
					minimumGametickCount = entry.getOffset() + duration;
			}
		}
	}

	/* ******************** MODEL-VIEW lookup ******************** */

	public long getSequenceDuration(GSSequence sequence) {
		return getSequenceDuration(sequence.getSequenceUUID());
	}

	public long getSequenceDuration(UUID sequenceUUID) {
		Long duration = sequenceDurations.get(sequenceUUID);
		return (duration != null) ? duration.longValue() : -1L;
	}

	public boolean isTrackAfter(GSTrack track, GSTrack otherTrack) {
		return isTrackAfter(track.getTrackUUID(), otherTrack.getTrackUUID());
	}
	
	public boolean isTrackAfter(UUID trackUUID, UUID otherTrackUUID) {
		Integer i0 = trackUUIDToIndex.get(trackUUID);
		Integer i1 = trackUUIDToIndex.get(otherTrackUUID);
	
		if (i0 == null || i1 == null)
			return (i0 == null);
		
		return (i0 > i1);
	}


	public GSTrack getNextTrack(GSTrack track, boolean descending) {
		return getNextTrack(track, descending, true);
	}

	public UUID getNextTrack(UUID trackUUID, boolean descending) {
		return getNextTrack(trackUUID, descending, true);
	}

	public GSTrack getNextTrack(GSTrack track, boolean descending, boolean cycle) {
		UUID nextTrackUUID = getNextTrack(track.getTrackUUID(), descending, cycle);
		return (nextTrackUUID == null) ? null : model.getTrack(nextTrackUUID);
	}
	
	public UUID getNextTrack(UUID trackUUID, boolean descending, boolean cycle) {
		Integer trackIndex = trackUUIDToIndex.get(trackUUID);
		if (trackIndex == null)
			return null;
		
		int nextIndex = trackIndex;
		if (descending) {
			nextIndex--;
			
			if (nextIndex < 0) {
				if (!cycle)
					return null;
				nextIndex = trackUUIDToIndex.size() - 1;
			}
		} else {
			nextIndex++;
		
			if (nextIndex >= trackUUIDToIndex.size()) {
				if (!cycle)
					return null;
				nextIndex = 0;
			}
		}
		
		return trackIndexToUUID.get(nextIndex);
	}
	
	/* ******************** MODEL TO VIEW methods ******************** */

	public GSRectangle modelToView(GSTrackEntry entry) {
		return modelToView(entry, null);
	}
	
	public GSRectangle modelToView(GSTrackEntry entry, GSRectangle dest) {
		long duration = getSequenceDuration(entry.getParent().getSequence());

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
	
	public int getGametickX(long gametick) {
		return getGametickExactX((double)gametick);
	}

	public int getGametickExactX(double gt) {
		return (int)Math.round(gt * gametickWidth);
	}
	
	public int getGametickWidth(long gametick) {
		return getGametickX(gametick + 1L) - getGametickX(gametick);
	}

	public int getTrackY(GSTrack track) {
		return getTrackY(track.getTrackUUID());
	}
	
	public int getTrackY(UUID trackUUID) {
		Integer trackIndex = trackUUIDToIndex.get(trackUUID);
		if (trackIndex == null)
			return -1;
		
		return trackIndex * (TRACK_HEIGHT + TRACK_SPACING);
	}

	public int getMinimumWidth() {
		return getGametickX(minimumGametickCount);
	}

	public int getMinimumHeight() {
		return trackUUIDToIndex.size() * (TRACK_HEIGHT + TRACK_SPACING);
	}
	
	/* ******************** VIEW TO MODEL methods ******************** */

	public GSTrackEntry getEntryAt(int x, int y) {
		GSTrack track = getTrackFromY(y);
		
		if (track != null) {
			long duration = getSequenceDuration(track.getSequence());
			long gt = getGametickFromX(x);
			
			for (GSTrackEntry entry : track.getEntries()) {
				if (entry.getOffset() <= gt && entry.getOffset() + duration > gt)
					return entry;
			}
		}
		
		return null;
	}
	
	public long getGametickFromX(int x) {
		return getGametickFromExact(getGametickExactFromX(x));
	}

	public double getGametickExactFromX(int x) {
		return (double)x / gametickWidth;
	}
	
	public long getGametickFromExact(double gt) {
		return (long)Math.floor(gt);
	}

	public GSTrack getTrackFromY(int y) {
		if (y < 0)
			return null;
		
		int trackIndex = y / (TRACK_HEIGHT + TRACK_SPACING);
		UUID trackUUID = trackIndexToUUID.get(trackIndex);
		
		return (trackUUID == null) ? null : model.getTrack(trackUUID);
	}
	
	/* ******************** GETTER & SETTER methods ******************** */
	
	public int getTrackHeight() {
		return TRACK_HEIGHT;
	}

	public int getTrackSpacing() {
		return TRACK_SPACING;
	}
	
	public double getGametickWidth() {
		return gametickWidth;
	}

	public void setGametickWidth(double gametickWidth) {
		if (gametickWidth != this.gametickWidth) {
			this.gametickWidth = gametickWidth;
			calculateTimeIndicatorInterval();
			dispatchModelViewChangedEvent();
		}
	}

	public void multiplyZoom(double multiplier) {
		// Ensure that width is within decent bounds
		gametickWidth = GSMathUtil.clamp(gametickWidth * multiplier, 
				MIN_GAMETICK_WIDTH, MAX_GAMETICK_WIDTH);
		calculateTimeIndicatorInterval();
		dispatchModelViewChangedEvent();
	}

	public long getTimeIndicatorInterval() {
		return timeIndicatorInterval;
	}
	
	private void calculateTimeIndicatorInterval() {
		double minInterval = MIN_TIME_INDICATOR_WIDTH / gametickWidth;
		// Fix interval to nice intervals such as 10 20 30 or similar.
		if (minInterval <= 0L) {
			timeIndicatorInterval = 1L;
		} else if (minInterval < 10) {
			timeIndicatorInterval = (minInterval < TIME_SUB_INDICATOR_COUNT) ?
					TIME_SUB_INDICATOR_COUNT : 10L;
		} else {
			// Calculate the 10^e exponent for the minimum interval.
			double exponent = Math.floor(Math.log10(minInterval));
			double magnitude = Math.pow(10.0, exponent);
			double coefficient = Math.ceil(minInterval / magnitude);
			timeIndicatorInterval = Math.round(coefficient * magnitude);
		}
	}
	
	public long getTimeIndicatorFromX(int x) {
		return Math.floorDiv(getGametickFromX(x), timeIndicatorInterval) * timeIndicatorInterval;
	}
	
	/* ******************** LISTENER & EVENT methods ******************** */
	
	public void addModelViewListener(GSIModelViewListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		listenters.add(listener);
	}

	public void removeModelViewListener(GSIModelViewListener listener) {
		listenters.remove(listener);
	}
	
	private void dispatchModelViewChangedEvent() {
		listenters.forEach(GSIModelViewListener::modelViewChanged);
	}
	
	/* ******************** INHERITED LISTENER methods ******************** */
	
	@Override
	public void trackAdded(GSTrack track) {
		updateTrackIndexLookup();
		updateSequenceDuration(track.getSequence());
		updateMinimumGametickCount();
		track.getSequence().addSequenceListener(this);
		dispatchModelViewChangedEvent();
	}

	@Override
	public void trackRemoved(GSTrack track) {
		track.getSequence().removeSequenceListener(this);
		sequenceDurations.remove(track.getSequence().getSequenceUUID());
		updateTrackIndexLookup();
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
	}

	@Override
	public void trackGroupChanged(GSTrack track, UUID oldGroupUUID) {
		updateTrackIndexLookup();
		dispatchModelViewChangedEvent();
	}
	
	@Override
	public void entryRemoved(GSTrackEntry entry) {
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
	}

	@Override
	public void entryAdded(GSTrackEntry entry) {
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
	}
	
	@Override
	public void entryOffsetChanged(GSTrackEntry entry, long oldOffset) {
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
	}
	
	@Override
	public void channelAdded(GSChannel channel, UUID prevUUID) {
		updateSequenceDuration(channel.getParent());
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
	}

	@Override
	public void channelRemoved(GSChannel channel, UUID oldPrevUUID) {
		updateSequenceDuration(channel.getParent());
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
	}
	
	@Override
	public void entryAdded(GSChannelEntry entry) {
		updateSequenceDuration(entry.getParent().getParent());
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
	}

	@Override
	public void entryRemoved(GSChannelEntry entry) {
		updateSequenceDuration(entry.getParent().getParent());
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
	}

	@Override
	public void entryTimeChanged(GSChannelEntry entry, GSSignalTime oldStart, GSSignalTime oldEnd) {
		updateSequenceDuration(entry.getParent().getParent());
		updateMinimumGametickCount();
		dispatchModelViewChangedEvent();
	}
}
