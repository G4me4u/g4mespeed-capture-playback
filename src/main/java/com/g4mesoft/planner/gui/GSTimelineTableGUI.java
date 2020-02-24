package com.g4mesoft.planner.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.g4mesoft.core.GSCoreOverride;
import com.g4mesoft.gui.GSParentGUI;
import com.g4mesoft.planner.timeline.GSBlockEventTime;
import com.g4mesoft.planner.timeline.GSETimelineEntryType;
import com.g4mesoft.planner.timeline.GSTimeline;
import com.g4mesoft.planner.timeline.GSTimelineEntry;
import com.g4mesoft.planner.timeline.GSTimelineTable;

import net.minecraft.client.util.NarratorManager;
import net.minecraft.util.math.BlockPos;

public class GSTimelineTableGUI extends GSParentGUI {

	private static final int LABEL_COLUMN_COLOR = 0x40000000;
	private static final int COLUMN_HEADER_COLOR = LABEL_COLUMN_COLOR;

	private static final int LABEL_COLUMN_WIDTH = 100;
	private static final int COLUMN_HEADER_HEIGHT = 20;

	private static final int ROW_SPACING_COLOR = 0xFF444444;
	private static final int ROW_HOVER_COLOR = 0x804444AA;
	
	private static final int ROW_SPACING = 1;
	private static final int ROW_LABEL_PADDING = 2;
	private static final int ENTRY_HEIGHT = 8;
	private static final int GAMETIME_COLUMN_WIDTH = 30;
	
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int EXTRA_GAMETICKS = 3;
	
	private final GSTimelineTable table;
	private int[] gameTickDurations;
	
	private GSBlockEventTime startTime;
	private GSBlockEventTime endTime;
	
	protected GSTimelineTableGUI(GSTimelineTable table) {
		super(NarratorManager.EMPTY);
		
		this.table = table;
		
		initTable();
	}
	
	private void initTable() {
		List<GSTimeline> timelines = table.getTimelines();
		
		startTime = GSBlockEventTime.INFINITE;
		endTime = GSBlockEventTime.ZERO;
		
		for (GSTimeline timeline : timelines) {
			for (GSTimelineEntry entry : timeline.getEntries()) {
				if (startTime.isAfter(entry.getStartTime()))
					startTime = entry.getStartTime();
				if (endTime.isBefore(entry.getEndTime()))
					endTime = entry.getEndTime();
			}
		}
		
		if (startTime.isAfter(endTime))
			startTime = endTime = GSBlockEventTime.ZERO;
		
		endTime = new GSBlockEventTime(endTime.getGameTime() + EXTRA_GAMETICKS, endTime.getBlockEventDelay());
		
		int numGameTicks = (int)(endTime.getGameTime() - startTime.getGameTime()) + 1;
		gameTickDurations = new int[numGameTicks];
		Arrays.fill(gameTickDurations, 1);
		
		for (GSTimeline timeline : timelines) {
			for (GSTimelineEntry entry : timeline.getEntries()) {
				updateGameTickDuration(entry.getStartTime());
				updateGameTickDuration(entry.getEndTime());
			}
		}
	}
	
	private void updateGameTickDuration(GSBlockEventTime time) {
		int gameTimeOffset = getGameTimeOffset(time);
		if (time.getBlockEventDelay() > gameTickDurations[gameTimeOffset])
			gameTickDurations[gameTimeOffset] = time.getBlockEventDelay() + 1;
	}
	
	private int getGameTimeOffset(GSBlockEventTime time) {
		return (int)(time.getGameTime() - startTime.getGameTime());
	}

	@Override
	@GSCoreOverride
	public void init() {
		super.init();
	}

	@Override
	public void renderTranslated(int mouseX, int mouseY, float partialTicks) {
		super.renderTranslated(mouseX, mouseY, partialTicks);
	
		fill(0, COLUMN_HEADER_HEIGHT, LABEL_COLUMN_WIDTH, height, LABEL_COLUMN_COLOR);
		
		renderColumnHeaders();
		renderTimelines(mouseX, mouseY);
		renderHoveredInfo(mouseX, mouseY);
	}
	
	private int getRowHeight() {
		return Math.max(ROW_LABEL_PADDING * 2 + font.fontHeight, ENTRY_HEIGHT);
	}
	
	private void renderTimelines(int mouseX, int mouseY) {
		int rowHeight = getRowHeight();
		
		int rowEnd = modelToView(endTime);
		
		int y = COLUMN_HEADER_HEIGHT;
		for (GSTimeline timeline : table.getTimelines()) {
			renderTimeline(timeline, y, rowHeight, mouseX, mouseY);
			y += rowHeight;
			fill(0, y, rowEnd, y + ROW_SPACING, ROW_SPACING_COLOR);
			y += ROW_SPACING;
		}
	}
	
	private void renderTimeline(GSTimeline timeline, int y, int rowHeight, int mouseX, int mouseY) {
		int color = (0xFF << 24) | timeline.getInfo().getColor();
		
		if (mouseX >= 0 && mouseX < width && mouseY >= y && mouseY < y + rowHeight + ROW_SPACING)
			fill(0, y, width, y + rowHeight, ROW_HOVER_COLOR);
		
		int entryY = y + (rowHeight - ENTRY_HEIGHT) / 2;
		for (GSTimelineEntry entry : timeline.getEntries())
			renderTimelineEntry(entry, entryY, entryY + ENTRY_HEIGHT, color, mouseX, mouseY);
		
		String name = trimText(timeline.getInfo().getName(), LABEL_COLUMN_WIDTH);
		int x = (LABEL_COLUMN_WIDTH - font.getStringWidth(name)) / 2;
		drawString(font, name, x, y + (rowHeight - font.fontHeight) / 2, color);
	}
	
	private void renderTimelineEntry(GSTimelineEntry entry, int y0, int y1, int color, int mouseX, int mouseY) {
		int x0 = modelToView(entry.getStartTime());
		int x1 = modelToView(entry.getEndTime());
		
		if (mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1)
			color = darkenColor(color);
		
		fill(x0, y0, x1, y1, darkenColor(color));
		
		int borderThickness = 2;
		if (entry.getType() != GSETimelineEntryType.EVENT_BEGINNING)
			x1 -= borderThickness;
		if (entry.getType() != GSETimelineEntryType.EVENT_END)
			x0 += borderThickness;
		
		fill(x0, y0 + borderThickness, x1, y1 - borderThickness, color);
	}
	
	private void renderColumnHeaders() {
		fill(0, 0, width, COLUMN_HEADER_HEIGHT, COLUMN_HEADER_COLOR);

		int gridColor = brightenColor(ROW_SPACING_COLOR);
		int x = modelToView(new GSBlockEventTime(startTime.getGameTime(), 0));

		for (int gt = 0; gt < gameTickDurations.length; gt++) {
			long gameTime = startTime.getGameTime() + gt;
			int nextX = modelToView(new GSBlockEventTime(gameTime + 1, 0));

			String title = String.format(Locale.ENGLISH, "%dgt", gameTime);
			drawCenteredString(font, title, (x + nextX) / 2, (COLUMN_HEADER_HEIGHT - font.fontHeight) / 2, TEXT_COLOR);
			
			fill(x, 0, x + 1, height, gridColor);

			x = nextX;
		}

		fill(x, 0, x + 1, height, gridColor);
		fill(0, COLUMN_HEADER_HEIGHT - 1, x, COLUMN_HEADER_HEIGHT, ROW_SPACING_COLOR);
	}
	
	private void renderHoveredInfo(int mouseX, int mouseY) {
		GSTimeline hoveredTimeline = getHoveredTimeline(mouseX, mouseY);
		if (hoveredTimeline != null) {
			String text;
			GSBlockEventTime mouseTime = viewToModel(mouseX);
			GSTimelineEntry hoveredEntry = hoveredTimeline.getEntryAt(mouseTime);
			if (hoveredEntry != null) {
				text = String.format(Locale.ENGLISH, "Duration: %dgt", hoveredEntry.getGameTimeDuration());
			} else {
				BlockPos pos = hoveredTimeline.getInfo().getPos();
				text = String.format(Locale.ENGLISH, "Pos: (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
			}

			renderHoveredInfoText(text);
		}
	}
	
	private GSTimeline getHoveredTimeline(int mouseX, int mouseY) {
		if (mouseX >= 0 && mouseX < width && mouseY >= COLUMN_HEADER_HEIGHT && mouseY < height) {
			int hoveredIndex = (mouseY - COLUMN_HEADER_HEIGHT) / (getRowHeight() + ROW_SPACING);
			List<GSTimeline> timelines = table.getTimelines();
			if (hoveredIndex >= 0 && hoveredIndex < timelines.size())
				return timelines.get(hoveredIndex);
		}
		
		return null;
	}
	
	private void renderHoveredInfoText(String text) {
		drawCenteredString(font, text, LABEL_COLUMN_WIDTH / 2, (COLUMN_HEADER_HEIGHT - font.fontHeight) / 2, TEXT_COLOR);
	}
	
	private int modelToView(GSBlockEventTime time) {
		int gameTimeOffset = getGameTimeOffset(time);
		if (gameTimeOffset < 0)
			return LABEL_COLUMN_WIDTH;
		
		int x = LABEL_COLUMN_WIDTH + gameTimeOffset * GAMETIME_COLUMN_WIDTH;
		
		// Probably test if some previous gametime was expanded.
		if (gameTimeOffset < gameTickDurations.length)
			x += GAMETIME_COLUMN_WIDTH * time.getBlockEventDelay() / gameTickDurations[gameTimeOffset];
		
		return x;
	}
	
	private GSBlockEventTime viewToModel(int x) {
		x -= LABEL_COLUMN_WIDTH;
		if (x < 0)
			return startTime;
		
		int gameTimeOffset = x / GAMETIME_COLUMN_WIDTH;
		if (gameTimeOffset >= gameTickDurations.length)
			return endTime;
		
		x %= GAMETIME_COLUMN_WIDTH;
		
		int blockEventDelay = x * gameTickDurations[gameTimeOffset] / GAMETIME_COLUMN_WIDTH;
		return new GSBlockEventTime(startTime.getGameTime() + gameTimeOffset, blockEventDelay);
	}
}
