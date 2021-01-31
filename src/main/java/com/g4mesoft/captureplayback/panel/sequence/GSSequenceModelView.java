package com.g4mesoft.captureplayback.panel.sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSPanelContext;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.renderer.GSIRenderer2D;
import com.g4mesoft.util.GSMathUtils;

public class GSSequenceModelView {

	private static final int MINIMUM_MICROTICKS = 2;
	private static final int EXTRA_MICROTICKS = 0;
	
	private static final int GAMETICK_COLUMN_WIDTH = 30;
	private static final int MULTI_COLUMN_INSETS = 10;
	private static final int MT_COLUMN_WIDTH = 20;
	private static final int MINIMUM_ENTRY_WIDTH = 15;
	
	private static final int ENTRY_HEIGHT = 8;
	private static final int CHANNEL_LABEL_PADDING = 2;
	private static final int DEFAULT_CHANNEL_SPACING = 1;
	
	private static final int MINIMUM_CHANNEL_HEIGHT = ENTRY_HEIGHT;
	
	private final GSSequence model;
	private final GSExpandedColumnModel expandedColumnModel;
	
	private int minimumColumnCount;
	
	private GSSignalTime modelStartTime;
	private GSSignalTime modelEndTime;
	
	private int lookupSize;
	private int[] durationLookup;
	private final Map<UUID, Integer> channelUUIDToIndex;
	private final Map<Integer, UUID> channelIndexToUUID;
	private final Map<UUID, Map<Integer, Integer>> multiCellLookup;
	
	private int xOffset;
	private int yOffset;
	private int channelHeight;
	private int channelSpacing;

	private final List<GSISequenceModelViewListener> listenters;
	
	public GSSequenceModelView(GSSequence model, GSExpandedColumnModel expandedColumnModel) {
		this.model = model;
		this.expandedColumnModel = expandedColumnModel;
		
		modelStartTime = modelEndTime = GSSignalTime.ZERO;
		
		lookupSize = 0;
		durationLookup = new int[0];
		channelUUIDToIndex = new HashMap<>();
		channelIndexToUUID = new HashMap<>();
		multiCellLookup = new HashMap<>();
		
		channelHeight = MINIMUM_CHANNEL_HEIGHT;
		channelSpacing = DEFAULT_CHANNEL_SPACING;
	
		listenters = new ArrayList<>();
	}
	
	/* ******************** MODEL-VIEW initialization ******************** */
	
	public void updateModelView() {
		GSIRenderer2D renderer = GSPanelContext.getRenderer();
		setChannelHeight(renderer.getTextHeight() + CHANNEL_LABEL_PADDING * 2);
		
		updateBoundLookup();
		updateDurationLookup();
		updateChannelIndexLookup();
		updateMultiCellLookup();
	}
	
	private void updateBoundLookup() {
		modelStartTime = GSSignalTime.INFINITY;
		modelEndTime = GSSignalTime.ZERO;
		
		for (GSChannel channel : model.getChannels()) {
			for (GSChannelEntry entry : channel.getEntries()) {
				if (modelStartTime.isAfter(entry.getStartTime()))
					modelStartTime = entry.getStartTime();
				if (modelEndTime.isBefore(entry.getEndTime()))
					modelEndTime = entry.getEndTime();
			}
		}
		
		if (modelStartTime.isAfter(modelEndTime))
			modelStartTime = GSSignalTime.ZERO;
		
		lookupSize = (int)(modelEndTime.getGametick() - modelStartTime.getGametick()) + 1;

		minimumColumnCount = getColumnIndex(modelEndTime) + 1;
	}
	
	private void updateDurationLookup() {
		if (lookupSize != durationLookup.length)
			durationLookup = new int[lookupSize];
		Arrays.fill(durationLookup, 1);
		
		for (GSChannel channel : model.getChannels()) {
			for (GSChannelEntry entry : channel.getEntries()) {
				updateGameTickDuration(entry.getStartTime());
				updateGameTickDuration(entry.getEndTime());
			}
		}
	}
	
	private void updateGameTickDuration(GSSignalTime time) {
		int lookupOffset = getLookupOffset(time);
		if (time.getMicrotick() >= durationLookup[lookupOffset])
			durationLookup[lookupOffset] = time.getMicrotick() + 1;
	}
	
	private void updateChannelIndexLookup() {
		channelUUIDToIndex.clear();
		channelIndexToUUID.clear();
		
		int channelIndex = 0;
		for (UUID channelUUID : model.getChannelUUIDs()) {
			channelUUIDToIndex.put(channelUUID, channelIndex);
			channelIndexToUUID.put(channelIndex, channelUUID);
			channelIndex++;
		}
	}
	
	private void updateMultiCellLookup() {
		int[] columnEntryCount = new int[lookupSize];
		multiCellLookup.clear();
		
		for (GSChannel channel : model.getChannels()) {
			Arrays.fill(columnEntryCount, 0);

			for (GSChannelEntry entry : channel.getEntries()) {
				int startLookupOffset = getLookupOffset(entry.getStartTime());
				int endLookupOffset = getLookupOffset(entry.getEndTime());
			
				columnEntryCount[startLookupOffset]++;
				if (startLookupOffset != endLookupOffset)
					columnEntryCount[endLookupOffset]++;
			}
			
			Map<Integer, Integer> multiCellCount = new HashMap<>();
			for (int lookupIndex = 0; lookupIndex < lookupSize; lookupIndex++) {
				int entryCount = columnEntryCount[lookupIndex];
				if (entryCount > 1)
					multiCellCount.put(lookupIndex, entryCount);
			}
			
			multiCellLookup.put(channel.getChannelUUID(), multiCellCount);
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
	
	public int getMultiCellCount(UUID channelUUID, int columnIndex) {
		Map<Integer, Integer> multiCellCounts = multiCellLookup.get(channelUUID);
		if (multiCellCounts == null)
			return -1;

		int lookupOffset = getLookupOffset(columnIndex);
		if (lookupOffset == -1)
			return -1;
		
		Integer count = multiCellCounts.get(lookupOffset);
		return (count == null) ? -1 : count.intValue();
	}
	
	public boolean isMultiCell(UUID channelUUID, int columnIndex) {
		return getMultiCellCount(channelUUID, columnIndex) != -1;
	}
	
	public Iterator<GSMultiCellInfo> getMultiCellIterator(UUID channelUUID) {
		Map<Integer, Integer> multiCellCounts = multiCellLookup.get(channelUUID);
		if (multiCellCounts == null)
			return Collections.emptyIterator();
		return new GSMultiCellIterator(multiCellCounts.entrySet().iterator());
	}
	
	private int getLookupOffset(GSSignalTime time) {
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
	
	public UUID getNextChannelUUID(UUID channelUUID, boolean descending) {
		Integer channelIndex = channelUUIDToIndex.get(channelUUID);
		if (channelIndex == null)
			return null;
		
		int nextIndex = channelIndex;
		if (descending) {
			nextIndex--;
			
			if (nextIndex < 0)
				nextIndex = channelUUIDToIndex.size() - 1;
		} else {
			nextIndex++;
		
			if (nextIndex >= channelUUIDToIndex.size())
				nextIndex = 0;
		}
		
		return channelIndexToUUID.get(nextIndex);
	}
	
	/* ******************** MODEL TO VIEW methods ******************** */

	public GSRectangle modelToView(GSChannelEntry entry) {
		return modelToView(entry, null);
	}
	
	public GSRectangle modelToView(GSChannelEntry entry, GSRectangle dest) {
		int startColumnIndex = getColumnIndex(entry.getStartTime());
		int endColumnIndex = getColumnIndex(entry.getEndTime());

		// This should rarely or never happen
		if (startColumnIndex < 0)
			return null;
		
		UUID channelUUID = entry.getParent().getChannelUUID();
		
		boolean expanded = expandedColumnModel.isColumnExpanded(startColumnIndex);
		if (!expanded && startColumnIndex == endColumnIndex && isMultiCell(channelUUID, startColumnIndex)) {
			// The entry exists only in a single column and it is not
			// expanded. Since the column is also a multi-column we
			// should not render it. Return null.
			return null;
		}
		
		if (dest == null)
			dest = new GSRectangle();
		
		dest.x = getTimeViewX(channelUUID, entry.getStartTime(), false);
		dest.width = getTimeViewX(channelUUID, entry.getEndTime(), true) - dest.x;

		dest.y = getEntryY(channelUUID);
		dest.height = ENTRY_HEIGHT;
		
		if (dest.width < MINIMUM_ENTRY_WIDTH) {
			dest.x -= (MINIMUM_ENTRY_WIDTH - dest.width) / 2;
			dest.width = MINIMUM_ENTRY_WIDTH;
		}
		
		if (entry.getStartTime().isEqual(GSSignalTime.ZERO) && entry.getType() == GSEChannelEntryType.EVENT_END) {
			int x0 = getColumnX(0);
			dest.width += dest.x - x0;
			dest.x = x0;
		}
		
		return dest;
	}
	
	private int getTimeViewX(UUID channelUUID, GSSignalTime time, boolean endTime) {
		int columnIndex = getColumnIndex(time);
		
		int x = getColumnX(columnIndex);
		if (expandedColumnModel.isColumnExpanded(columnIndex)) {
			x += time.getMicrotick() * MT_COLUMN_WIDTH + MT_COLUMN_WIDTH / 2;
		} else if (isMultiCell(channelUUID, columnIndex)) {
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
			int trailingColumnCount = columnIndex - maxIndex - 1;
			if (trailingColumnCount > 0)
				columnOffset += trailingColumnCount * GAMETICK_COLUMN_WIDTH;
			
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
	
	public int getChannelY(UUID channelUUID) {
		Integer channelIndex = channelUUIDToIndex.get(channelUUID);
		if (channelIndex == null)
			return -1;
		
		return yOffset + channelIndex.intValue() * (channelHeight + channelSpacing);
	}
	
	public int getEntryY(UUID channelUUID) {
		return getChannelY(channelUUID) + (channelHeight - ENTRY_HEIGHT) / 2;
	}

	public int getColumnIndex(GSSignalTime time) {
		return getColumnIndex(time.getGametick());
	}
	
	public int getColumnIndex(long gametick) {
		return (int)gametick;
	}
	
	public int getMinimumWidth() {
		return getColumnOffset(minimumColumnCount);
	}

	public int getMinimumHeight() {
		return channelIndexToUUID.size() * (channelHeight + channelSpacing);
	}
	
	/* ******************** VIEW TO MODEL methods ******************** */
	
	public GSSignalTime viewToModel(int x, int y) {
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
			UUID channelUUID = getChannelUUIDFromView(y);
			int columnDuration = getColumnDuration(columnIndex);
			
			if (channelUUID != null && isMultiCell(channelUUID, columnIndex)) {
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
	
	public UUID getChannelUUIDFromView(int y) {
		if (y < yOffset)
			return null;
		
		int channelIndex = (y - yOffset) / (channelHeight + channelSpacing);
		return channelIndexToUUID.get(Integer.valueOf(channelIndex));
	}
	
	public GSSignalTime getDraggedTime(int x, int y) {
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
	
	public GSSignalTime getColumnTime(int columnIndex, int mt) {
		return new GSSignalTime(getColumnGametick(columnIndex), mt);
	}
	
	/* ******************** GETTER & SETTER methods ******************** */
	
	public int getChannelHeight() {
		return channelHeight;
	}

	public void setChannelHeight(int channelHeight) {
		if (channelHeight < MINIMUM_CHANNEL_HEIGHT)
			channelHeight = MINIMUM_CHANNEL_HEIGHT;
		
		if (channelHeight != this.channelHeight) {
			this.channelHeight = channelHeight;
			dispatchModelViewChangedEvent();
		}
	}
	
	public int getChannelSpacing() {
		return channelSpacing;
	}

	public void setChannelSpacing(int channelSpacing) {
		if (channelSpacing < 0)
			channelSpacing = 0;
		
		if (channelSpacing != this.channelSpacing) {
			this.channelSpacing = channelSpacing;
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
	
	public void addModelViewListener(GSISequenceModelViewListener listenter) {
		listenters.add(listenter);
	}

	public void removeModelViewListener(GSISequenceModelViewListener listenter) {
		listenters.remove(listenter);
	}
	
	private void dispatchModelViewChangedEvent() {
		listenters.forEach(GSISequenceModelViewListener::modelViewChanged);
	}
	
	private class GSMultiCellIterator implements Iterator<GSMultiCellInfo> {

		private final Iterator<Map.Entry<Integer, Integer>> countEntryIterator;
		
		public GSMultiCellIterator(Iterator<Map.Entry<Integer, Integer>> countEntryIterator) {
			this.countEntryIterator = countEntryIterator;
		}
		
		@Override
		public boolean hasNext() {
			return countEntryIterator.hasNext();
		}

		@Override
		public GSMultiCellInfo next() {
			Map.Entry<Integer, Integer> info = countEntryIterator.next();

			int columnIndex = getColumnIndexFromLookup(info.getKey());
			int count = info.getValue();
			
			return new GSMultiCellInfo(columnIndex, count);
		}
	}
}
