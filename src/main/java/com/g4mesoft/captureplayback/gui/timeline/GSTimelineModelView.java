package com.g4mesoft.captureplayback.gui.timeline;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
import com.g4mesoft.util.GSMathUtils;

public class GSTimelineModelView {

	private static final int MINIMUM_MICROTICKS = 2;
	private static final int EXTRA_MICROTICKS = 0;
	
	private static final int GAMETICK_COLUMN_WIDTH = 30;
	private static final int MULTI_COLUMN_INSETS = 10;
	private static final int MT_COLUMN_WIDTH = 20;
	private static final int MINIMUM_ENTRY_WIDTH = 15;
	
	private static final int ENTRY_HEIGHT = 8;
	private static final int DEFAULT_TRACK_SPACING = 1;
	
	private static final int MINIMUM_TRACK_HEIGHT = ENTRY_HEIGHT;
	
	private final GSTimeline model;
	private final GSExpandedColumnModel expandedColumnModel;
	
	private int minimumNumColumns;
	
	private GSBlockEventTime modelStartTime;
	private GSBlockEventTime modelEndTime;
	
	private int lookupSize;
	private int[] durationLookup;
	private final Map<UUID, Integer> trackUUIDtoIndex;
	private final Map<Integer, UUID> trackIndexToUUID;
	private final Map<UUID, Map<Integer, Integer>> multiCellLookup;
	
	private int xOffset;
	private int yOffset;
	private int trackHeight;
	private int trackSpacing;

	private final List<GSITimelineModelViewListener> listenters;
	
	public GSTimelineModelView(GSTimeline model, GSExpandedColumnModel expandedColumnModel) {
		this.model = model;
		this.expandedColumnModel = expandedColumnModel;
		
		modelStartTime = modelEndTime = GSBlockEventTime.ZERO;
		
		lookupSize = 0;
		durationLookup = new int[0];
		trackUUIDtoIndex = new HashMap<UUID, Integer>();
		trackIndexToUUID = new HashMap<Integer, UUID>();
		multiCellLookup = new HashMap<UUID, Map<Integer,Integer>>();
		
		trackHeight = MINIMUM_TRACK_HEIGHT;
		trackSpacing = DEFAULT_TRACK_SPACING;
	
		listenters = new ArrayList<GSITimelineModelViewListener>();
	}
	
	/* ******************** MODEL-VIEW initialization ******************** */
	
	public void updateModelView() {
		updateBoundLookup();
		updateDurationLookup();
		updateTrackIndexLookup();
		updateMultiCellLookup();
	}
	
	private void updateBoundLookup() {
		modelStartTime = GSBlockEventTime.INFINITY;
		modelEndTime = GSBlockEventTime.ZERO;
		
		for (GSTrack track : model.getTracks()) {
			for (GSTrackEntry entry : track.getEntries()) {
				if (modelStartTime.isAfter(entry.getStartTime()))
					modelStartTime = entry.getStartTime();
				if (modelEndTime.isBefore(entry.getEndTime()))
					modelEndTime = entry.getEndTime();
			}
		}
		
		if (modelStartTime.isAfter(modelEndTime))
			modelStartTime = GSBlockEventTime.ZERO;
		
		lookupSize = (int)(modelEndTime.getGametick() - modelStartTime.getGametick()) + 1;

		minimumNumColumns = getColumnIndex(modelEndTime) + 1;
	}
	
	private void updateDurationLookup() {
		if (lookupSize != durationLookup.length)
			durationLookup = new int[lookupSize];
		Arrays.fill(durationLookup, 1);
		
		for (GSTrack track : model.getTracks()) {
			for (GSTrackEntry entry : track.getEntries()) {
				updateGameTickDuration(entry.getStartTime());
				updateGameTickDuration(entry.getEndTime());
			}
		}
	}
	
	private void updateGameTickDuration(GSBlockEventTime time) {
		int lookupOffset = getLookupOffset(time);
		if (time.getMicrotick() >= durationLookup[lookupOffset])
			durationLookup[lookupOffset] = time.getMicrotick() + 1;
	}
	
	private void updateTrackIndexLookup() {
		trackUUIDtoIndex.clear();
		trackIndexToUUID.clear();
		
		int trackIndex = 0;
		for (UUID trackUUID : model.getTrackUUIDs()) {
			trackUUIDtoIndex.put(trackUUID, trackIndex);
			trackIndexToUUID.put(trackIndex, trackUUID);
			trackIndex++;
		}
	}
	
	private void updateMultiCellLookup() {
		int[] columnEntryCount = new int[lookupSize];
		multiCellLookup.clear();
		
		for (Map.Entry<UUID, GSTrack> trackEntry : model.getTrackEntries()) {
			Arrays.fill(columnEntryCount, 0);

			GSTrack track = trackEntry.getValue();
			UUID trackUUID = trackEntry.getKey();
			
			for (GSTrackEntry entry : track.getEntries()) {
				int startLookupOffset = getLookupOffset(entry.getStartTime());
				int endLookupOffset = getLookupOffset(entry.getEndTime());
			
				columnEntryCount[startLookupOffset]++;
				if (startLookupOffset != endLookupOffset)
					columnEntryCount[endLookupOffset]++;
			}
			
			Map<Integer, Integer> multiCellCount = new HashMap<Integer, Integer>();
			for (int lookupIndex = 0; lookupIndex < lookupSize; lookupIndex++) {
				int entryCount = columnEntryCount[lookupIndex];
				if (entryCount > 1)
					multiCellCount.put(lookupIndex, entryCount);
			}
			
			multiCellLookup.put(trackUUID, multiCellCount);
		}
	}
	
	/* ******************** MODEL-VIEW lookup ******************** */
	
	public int getColumnDuration(int columnIndex) {
		int lookupOffset = getLookupOffset(columnIndex);
		int duration = MINIMUM_MICROTICKS;
		if (lookupOffset != -1 && durationLookup[lookupOffset] > duration)
			duration = durationLookup[lookupOffset];
		return duration + EXTRA_MICROTICKS;
	}
	
	public int getMultiCellCount(UUID trackUUID, int columnIndex) {
		Map<Integer, Integer> multiCellCounts = multiCellLookup.get(trackUUID);
		if (multiCellCounts == null)
			return -1;

		int lookupOffset = getLookupOffset(columnIndex);
		if (lookupOffset == -1)
			return -1;
		
		Integer count = multiCellCounts.get(lookupOffset);
		return (count == null) ? -1 : count.intValue();
	}
	
	public boolean isMultiCell(UUID trackUUID, int columnIndex) {
		return getMultiCellCount(trackUUID, columnIndex) != -1;
	}
	
	public Iterator<GSMultiCellInfo> getMultiCellIterator(UUID trackUUID) {
		Map<Integer, Integer> multiCellCounts = multiCellLookup.get(trackUUID);
		if (multiCellCounts == null)
			return Collections.emptyIterator();
		return new GSMultiCellIterator(multiCellCounts.entrySet().iterator());
	}
	
	private int getLookupOffset(GSBlockEventTime time) {
		return getLookupOffset(getColumnIndex(time));
	}
	
	private int getLookupOffset(int columnIndex) {
		int lookupOffset = (int)(getColumnGametick(columnIndex) - modelStartTime.getGametick());
		if (lookupOffset < 0 || lookupOffset >= lookupSize)
			return -1;
		return lookupOffset;
	}
	
	private int getColumnIndexFromLookup(int lookupOffset) {
		return getColumnIndex(lookupOffset + modelStartTime.getGametick());
	}
	
	public UUID getNextTrackUUID(UUID trackUUID, boolean descending) {
		Integer trackIndex = trackUUIDtoIndex.get(trackUUID);
		if (trackIndex == null)
			return null;
		
		int nextIndex = trackIndex;
		if (descending) {
			nextIndex--;
			
			if (nextIndex < 0)
				nextIndex = trackUUIDtoIndex.size() - 1;
		} else {
			nextIndex++;
		
			if (nextIndex >= trackUUIDtoIndex.size())
				nextIndex = 0;
		}
		
		return trackIndexToUUID.get(nextIndex);
	}
	
	/* ******************** MODEL TO VIEW methods ******************** */

	public Rectangle modelToView(UUID trackUUID, GSTrackEntry entry) {
		return modelToView(trackUUID, entry, null);
	}
	
	public Rectangle modelToView(UUID trackUUID, GSTrackEntry entry, Rectangle dest) {
		int startColumnIndex = getColumnIndex(entry.getStartTime());
		int endColumnIndex = getColumnIndex(entry.getEndTime());

		// This should rarely or never happen
		if (startColumnIndex < 0)
			return null;
		
		boolean expanded = expandedColumnModel.isColumnExpanded(startColumnIndex);
		if (!expanded && startColumnIndex == endColumnIndex && isMultiCell(trackUUID, startColumnIndex)) {
			// The entry exists only in a single column and it is not
			// expanded. Since the column is also a multi-column we
			// should not render it. Return null.
			return null;
		}
		
		if (dest == null)
			dest = new Rectangle();
		
		dest.y = getEntryY(trackUUID);
		dest.height = ENTRY_HEIGHT;
		
		dest.x = getTimeViewX(trackUUID, entry.getStartTime(), false);
		dest.width = getTimeViewX(trackUUID, entry.getEndTime(), true) - dest.x;
		
		if (dest.width < MINIMUM_ENTRY_WIDTH) {
			dest.x -= (MINIMUM_ENTRY_WIDTH - dest.width) / 2;
			dest.width = MINIMUM_ENTRY_WIDTH;
		}
		
		if (entry.getStartTime().isEqual(GSBlockEventTime.ZERO) && entry.getType() == GSETrackEntryType.EVENT_END) {
			int x0 = getColumnX(0);
			dest.width += dest.x - x0;
			dest.x = x0;
		}
		
		return dest;
	}
	
	private int getTimeViewX(UUID trackUUID, GSBlockEventTime time, boolean endTime) {
		int columnIndex = getColumnIndex(time);
		
		int x = getColumnX(columnIndex);
		if (expandedColumnModel.isColumnExpanded(columnIndex)) {
			x += time.getMicrotick() * MT_COLUMN_WIDTH + MT_COLUMN_WIDTH / 2;
		} else if (isMultiCell(trackUUID, columnIndex)) {
			x += endTime ? MULTI_COLUMN_INSETS : (GAMETICK_COLUMN_WIDTH - MULTI_COLUMN_INSETS);
		} else {
			x += GAMETICK_COLUMN_WIDTH / 2;
		}
		
		return x;
	}
	
	public int getColumnX(int columnIndex) {
		return xOffset + getColumnOffset(columnIndex);
	}
	
	private int getColumnOffset(int columnIndex) {
		if (expandedColumnModel.hasExpandedColumn() && columnIndex >= expandedColumnModel.getMinColumnIndex()) {
			int minIndex = expandedColumnModel.getMinColumnIndex();
			int maxIndex = expandedColumnModel.getMaxColumnIndex();

			int columnOffset = minIndex * GAMETICK_COLUMN_WIDTH;
			for (int i = minIndex; i <= maxIndex && i < columnIndex; i++)
				columnOffset += getColumnDuration(i) * MT_COLUMN_WIDTH;
			
			// We should not include the columnIndex itself.
			int numTrailingColumns = columnIndex - maxIndex - 1;
			if (numTrailingColumns > 0)
				columnOffset += numTrailingColumns * GAMETICK_COLUMN_WIDTH;
			
			return columnOffset;
		}
		
		return columnIndex * GAMETICK_COLUMN_WIDTH;
	}
	
	public int getColumnWidth(int columnIndex) {
		if (expandedColumnModel.isColumnExpanded(columnIndex)) 
			return getColumnDuration(columnIndex) * MT_COLUMN_WIDTH;
		return GAMETICK_COLUMN_WIDTH;
	}

	public int getMicrotickColumnX(int columnIndex, int mt) {
		return getColumnX(columnIndex) + MT_COLUMN_WIDTH * mt;
	}

	public int getMicrotickColumnWidth(int columnIndex, int mt) {
		return MT_COLUMN_WIDTH;
	}
	
	public int getTrackY(UUID trackUUID) {
		Integer trackIndex = trackUUIDtoIndex.get(trackUUID);
		if (trackIndex == null)
			return -1;
		
		return yOffset + trackIndex.intValue() * (trackHeight + trackSpacing);
	}
	
	public int getEntryY(UUID trackUUID) {
		return getTrackY(trackUUID) + (trackHeight - ENTRY_HEIGHT) / 2;
	}

	public int getColumnIndex(GSBlockEventTime time) {
		return getColumnIndex(time.getGametick());
	}
	
	public int getColumnIndex(long gametick) {
		return (int)gametick;
	}
	
	public int getMinimumWidth() {
		return getColumnOffset(minimumNumColumns);
	}

	public int getMinimumHeight() {
		return trackIndexToUUID.size() * (trackHeight + trackSpacing);
	}
	
	/* ******************** VIEW TO MODEL methods ******************** */
	
	public GSBlockEventTime viewToModel(int x, int y) {
		int columnIndex = getColumnIndexFromView(x);
		if (columnIndex == -1)
			return null;
		
		int mt = 0;
		if (expandedColumnModel.isColumnExpanded(columnIndex)) {
			int columnX = getColumnX(columnIndex);
			if (x < columnX)
				return null;

			mt = (x - columnX) / MT_COLUMN_WIDTH;
		} else {
			UUID trackUUID = getTrackUUIDFromView(y);
			int columnDuration = getColumnDuration(columnIndex);
			
			if (trackUUID != null && isMultiCell(trackUUID, columnIndex)) {
				int columnOffset = x - getColumnX(columnIndex);
				mt = (columnOffset > GAMETICK_COLUMN_WIDTH - MULTI_COLUMN_INSETS) ? columnDuration : 
						(columnOffset < MULTI_COLUMN_INSETS) ? 0 : columnDuration / 2;
			} else {
				mt = columnDuration / 2;
			}
		}

		return getColumnTime(columnIndex, mt);
	}
	
	public int getColumnIndexFromView(int x) {
		if (x < xOffset)
			return -1;
		
		int columnIndex = (x - xOffset) / GAMETICK_COLUMN_WIDTH;
		if (expandedColumnModel.hasExpandedColumn() && columnIndex >= expandedColumnModel.getMinColumnIndex()) {
			columnIndex = expandedColumnModel.getMinColumnIndex();
			int maxIndex = expandedColumnModel.getMaxColumnIndex();

			int offset = x - getColumnX(columnIndex);
			for ( ; columnIndex <= maxIndex; columnIndex++) {
				offset -= getColumnWidth(columnIndex);
				if (offset < 0)
					return columnIndex;
			}
			
			return columnIndex + offset / GAMETICK_COLUMN_WIDTH;
		}
		
		return columnIndex;
	}
	
	public UUID getTrackUUIDFromView(int y) {
		if (y < yOffset)
			return null;
		
		int trackIndex = (y - yOffset) / (trackHeight + trackSpacing);
		return trackIndexToUUID.get(Integer.valueOf(trackIndex));
	}
	
	public GSBlockEventTime getDraggedTime(int x, int y) {
		if (expandedColumnModel.isSingleExpandedColumn()) {
			int minIndex = expandedColumnModel.getMinColumnIndex();
			int maxIndex = expandedColumnModel.getMaxColumnIndex();
			int columnIndex = GSMathUtils.clamp(getColumnIndexFromView(x), minIndex, maxIndex);
			
			int columnOffset = x - getColumnX(columnIndex);
			if (columnOffset < 0)
				return null;
			return getColumnTime(columnIndex, columnOffset / MT_COLUMN_WIDTH);
		}
		
		return viewToModel(x, y);
	}
	
	public long getColumnGametick(int columnIndex) {
		return (long)columnIndex;
	}
	
	public GSBlockEventTime getColumnTime(int columnIndex, int mt) {
		return new GSBlockEventTime(getColumnGametick(columnIndex), mt);
	}
	
	/* ******************** GETTER & SETTER methods ******************** */
	
	public int getTrackHeight() {
		return trackHeight;
	}

	public void setTrackHeight(int trackHeight) {
		if (trackHeight < MINIMUM_TRACK_HEIGHT)
			trackHeight = MINIMUM_TRACK_HEIGHT;
		
		if (trackHeight != this.trackHeight) {
			this.trackHeight = trackHeight;
			dispatchModelViewChangedEvent();
		}
	}
	
	public int getTrackSpacing() {
		return trackSpacing;
	}

	public void setTrackSpacing(int trackSpacing) {
		if (trackSpacing < 0)
			trackSpacing = 0;
		
		if (trackSpacing != this.trackSpacing) {
			this.trackSpacing = trackSpacing;
			dispatchModelViewChangedEvent();
		}
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
	
	/* ******************** LISTENER & EVENT methods ******************** */
	
	public void addModelViewListener(GSITimelineModelViewListener listenter) {
		listenters.add(listenter);
	}

	public void removeModelViewListener(GSITimelineModelViewListener listenter) {
		listenters.remove(listenter);
	}
	
	private void dispatchModelViewChangedEvent() {
		listenters.forEach(GSITimelineModelViewListener::modelViewChanged);
	}
	
	private class GSMultiCellIterator implements Iterator<GSMultiCellInfo> {

		private final Iterator<Map.Entry<Integer, Integer>> countEntryIterator;
		private final GSMultiCellInfo multiCellInfo;
		
		public GSMultiCellIterator(Iterator<Map.Entry<Integer, Integer>> countEntryIterator) {
			this.countEntryIterator = countEntryIterator;
			multiCellInfo = new GSMultiCellInfo();
		}
		
		@Override
		public boolean hasNext() {
			return countEntryIterator.hasNext();
		}

		@Override
		public GSMultiCellInfo next() {
			if (!countEntryIterator.hasNext())
				throw new IllegalStateException("Iterator has no next element.");
			
			Map.Entry<Integer, Integer> info = countEntryIterator.next();
			multiCellInfo.setColumnIndex(getColumnIndexFromLookup(info.getKey()));
			multiCellInfo.setCount(info.getValue());
			return multiCellInfo;
		}
	}
}
