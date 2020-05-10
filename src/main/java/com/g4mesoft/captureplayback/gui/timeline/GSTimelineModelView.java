package com.g4mesoft.captureplayback.gui.timeline;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.timeline.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;

public class GSTimelineModelView {

	private static final int EXTRA_MICROTICKS = 1;
	
	private static final int GAMETICK_COLUMN_WIDTH = 30;
	private static final int MULTI_COLUMN_INSETS = 10;
	private static final int MT_COLUMN_WIDTH = 20;
	private static final int MINIMUM_ENTRY_WIDTH = 15;
	
	private static final int ENTRY_HEIGHT = 8;
	private static final int ROW_SPACING = 1;
	
	private final GSTimeline model;
	private final GSTimelineGUI timelineGUI;
	private final GSTimelineViewport viewport;
	
	private int minimumNumColumns;
	
	private GSBlockEventTime modelStartTime;
	private GSBlockEventTime modelEndTime;
	
	private int lookupSize;
	private int[] durationLookup;
	private final Map<UUID, Integer> trackUUIDtoIndex;
	private final Map<Integer, UUID> trackIndexToUUID;
	private final Map<UUID, Map<Integer, Integer>> multiCellLookup;
	
	public GSTimelineModelView(GSTimeline model, GSTimelineGUI timelineGUI, GSTimelineViewport viewport) {
		this.model = model;
		this.timelineGUI = timelineGUI;
		this.viewport = viewport;
		
		modelStartTime = modelEndTime = GSBlockEventTime.ZERO;
		
		lookupSize = 0;
		durationLookup = new int[0];
		trackUUIDtoIndex = new HashMap<UUID, Integer>();
		trackIndexToUUID = new HashMap<Integer, UUID>();
		multiCellLookup = new HashMap<UUID, Map<Integer,Integer>>();
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
		if (lookupOffset == -1)
			return 1 + EXTRA_MICROTICKS;
		return durationLookup[lookupOffset] + EXTRA_MICROTICKS;
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
	
	/* ******************** MODEL TO VIEW methods ******************** */

	public Rectangle modelToView(UUID trackUUID, GSTrackEntry entry) {
		return modelToView(trackUUID, entry, null);
	}
	
	public Rectangle modelToView(UUID trackUUID, GSTrackEntry entry, Rectangle dest) {
		int startColumnIndex = getColumnIndex(entry.getStartTime());
		int endColumnIndex = getColumnIndex(entry.getEndTime());
		if (startColumnIndex < 0 || endColumnIndex >= getNumColumns())
			return null;
		
		if (startColumnIndex != timelineGUI.getExpandedColumnIndex() && 
				startColumnIndex == endColumnIndex && isMultiCell(trackUUID, startColumnIndex)) {

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
		if (columnIndex == timelineGUI.getExpandedColumnIndex()) {
			x += time.getMicrotick() * MT_COLUMN_WIDTH;

			x += MT_COLUMN_WIDTH / 2;
		} else if (isMultiCell(trackUUID, columnIndex)) {
			x += endTime ? MULTI_COLUMN_INSETS : (GAMETICK_COLUMN_WIDTH - MULTI_COLUMN_INSETS);
		} else {
			x += GAMETICK_COLUMN_WIDTH / 2;
		}
		
		return x;
	}
	
	public int getColumnX(int columnIndex) {
		int cx = viewport.getX() + columnIndex * GAMETICK_COLUMN_WIDTH;
		if (timelineGUI.getExpandedColumnIndex() != -1 && columnIndex > timelineGUI.getExpandedColumnIndex())
			cx += getColumnDuration(timelineGUI.getExpandedColumnIndex()) * MT_COLUMN_WIDTH - GAMETICK_COLUMN_WIDTH;
		return cx;
	}
	
	public int getColumnWidth(int columnIndex) {
		if (timelineGUI.getExpandedColumnIndex() == columnIndex) 
			return getColumnDuration(timelineGUI.getExpandedColumnIndex()) * MT_COLUMN_WIDTH;
		return GAMETICK_COLUMN_WIDTH;
	}

	public int getMicrotickColumnX(int columnIndex, int mt) {
		return getColumnX(columnIndex) + MT_COLUMN_WIDTH * mt;
	}

	public int getMicrotickColumnWidth(int columnIndex, int mt) {
		return MT_COLUMN_WIDTH;
	}
	
	public int getTrackEndY() {
		return viewport.getY() + trackIndexToUUID.size() * (timelineGUI.getRowHeight() + ROW_SPACING);
	}
	
	public int getTrackY(UUID trackUUID) {
		Integer trackIndex = trackUUIDtoIndex.get(trackUUID);
		if (trackIndex == null)
			return -1;
		
		return viewport.getY() + trackIndex.intValue() * (timelineGUI.getRowHeight() + ROW_SPACING);
	}
	
	public int getEntryY(UUID trackUUID) {
		return getTrackY(trackUUID) + (timelineGUI.getRowHeight() - ENTRY_HEIGHT) / 2;
	}

	public int getColumnIndex(GSBlockEventTime time) {
		return getColumnIndex(time.getGametick());
	}
	
	public int getColumnIndex(long gametick) {
		return (int)gametick;
	}
	
	public int getMinimumWidth() {
		int lastColumnIndex = minimumNumColumns - 1;
		return getColumnX(lastColumnIndex) + getColumnWidth(lastColumnIndex);
	}

	public int getMinimumHeight() {
		return trackIndexToUUID.size() * (timelineGUI.getRowHeight() + ROW_SPACING);
	}
	
	/* ******************** VIEW TO MODEL methods ******************** */
	
	public GSBlockEventTime viewToModel(int x, int y) {
		int columnIndex = getColumnIndexFromView(x);
		if (columnIndex == -1)
			return null;
		
		int mt = 0;
		if (columnIndex == timelineGUI.getExpandedColumnIndex()) {
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
	
	private int getColumnIndexFromView(int x) {
		if (x < viewport.getX())
			return -1;
		
		int columnIndex = (x - viewport.getX()) / GAMETICK_COLUMN_WIDTH;
		if (timelineGUI.getExpandedColumnIndex() != -1 && columnIndex >= timelineGUI.getExpandedColumnIndex()) {
			columnIndex = timelineGUI.getExpandedColumnIndex();

			int offset = x;
			offset -= getColumnX(timelineGUI.getExpandedColumnIndex());
			offset -= getColumnWidth(timelineGUI.getExpandedColumnIndex());
			
			if (offset > 0)
				columnIndex += 1 + offset / GAMETICK_COLUMN_WIDTH;
		}
		
		if (columnIndex >= getNumColumns())
			return -1;
		
		return columnIndex;
	}
	
	public UUID getTrackUUIDFromView(int y) {
		if (y >= 0 && y < viewport.getHeight()) {
			int trackIndex = (y - viewport.getY()) / (timelineGUI.getRowHeight() + ROW_SPACING);
			if (trackIndexToUUID.containsKey(trackIndex))
				return trackIndexToUUID.get(trackIndex);
		}
		
		return null;
	}
	
	public GSBlockEventTime getDraggedTime(int x, int y) {
		if (timelineGUI.getExpandedColumnIndex() != -1) {
			int columnOffset = x - getColumnX(timelineGUI.getExpandedColumnIndex());
			if (columnOffset < 0)
				return null;
			return getColumnTime(timelineGUI.getExpandedColumnIndex(), columnOffset / MT_COLUMN_WIDTH);
		}
		
		return viewToModel(x, y);
	}
	
	public long getColumnGametick(int columnIndex) {
		return (long)columnIndex;
	}
	
	public GSBlockEventTime getColumnTime(int columnIndex, int mt) {
		return new GSBlockEventTime(getColumnGametick(columnIndex), mt);
	}
	
	public int getNumColumns() {
		int numColumns = (viewport.getContentWidth() + GAMETICK_COLUMN_WIDTH - 1) / GAMETICK_COLUMN_WIDTH;
		return (numColumns > minimumNumColumns) ? numColumns : minimumNumColumns;
	}
	
	private static class GSMultiCellIterator implements Iterator<GSMultiCellInfo> {

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
			multiCellInfo.setColumnIndex(info.getKey());
			multiCellInfo.setCount(info.getValue());
			return multiCellInfo;
		}
	}
}
