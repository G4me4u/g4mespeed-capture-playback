package com.g4mesoft.planner.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

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
	private static final int COLUMN_HEADER_COLOR = 0x80000000;
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	private static final int LABEL_COLUMN_WIDTH = 100;
	private static final int COLUMN_HEADER_HEIGHT = 30;

	private static final int ROW_SPACING_COLOR = 0xFF444444;
	private static final int ROW_HOVER_COLOR = 0x80666666;
	
	private static final int DOTTED_LINE_LENGTH = 4;
	
	private static final int ROW_SPACING = 1;
	private static final int ENTRY_BORDER_THICKNESS = 2;
	
	private static final int ROW_LABEL_PADDING = 2;
	private static final int ENTRY_HEIGHT = 8;
	private static final int GAMETIME_COLUMN_WIDTH = 30;
	private static final int MT_COLUMN_WIDTH = 20;
	
	private static final int EXTRA_GAMETICKS = 1;
	private static final int EXTRA_MICROTICKS = 1;
	
	private static final int DRAGGING_AREA_SIZE = 5;
	private static final int DRAGGING_PADDING = 2;
	private static final int DRAGGING_AREA_COLOR = 0x40FFFFFF;
	
	private static final int NOT_RESIZING = 0;
	private static final int RESIZING_START = 1;
	private static final int RESIZING_END = 2;
	
	private final GSTimelineTable table;
	private int[] gameTickDurations;
	
	private GSBlockEventTime startTime;
	private GSBlockEventTime endTime;
	
	private int expandedColumnIndex;
	
	private GSTimeline draggedTimeline;
	private GSTimelineEntry draggedEntry;
	private int draggingFlag;
	
	private GSBlockEventTime draggedStartTime;
	private GSBlockEventTime draggedEndTime;
	private double mouseClickedX;
	
	protected GSTimelineTableGUI(GSTimelineTable table) {
		super(NarratorManager.EMPTY);
		
		this.table = table;
		
		initTable();
	}
	
	private void initTable() {
		updateGameTickTimes();
		
		expandedColumnIndex = -1;
		
		draggedTimeline = null;
		draggedEntry = null;
		draggingFlag = NOT_RESIZING;
	}
	
	private void updateGameTickTimes() {
		List<GSTimeline> timelines = table.getTimelines();
		
		startTime = GSBlockEventTime.ZERO;
		endTime = GSBlockEventTime.ZERO;
		
		for (GSTimeline timeline : timelines) {
			for (GSTimelineEntry entry : timeline.getEntries()) {
				if (endTime.isBefore(entry.getEndTime()))
					endTime = entry.getEndTime();
			}
		}
		
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

		for (int i = 0; i < numGameTicks; i++)
			gameTickDurations[i] += EXTRA_MICROTICKS;
	}
	
	private void updateGameTickDuration(GSBlockEventTime time) {
		int gameTimeOffset = getGameTimeOffset(time);
		if (time.getBlockEventDelay() >= gameTickDurations[gameTimeOffset])
			gameTickDurations[gameTimeOffset] = time.getBlockEventDelay() + 1;
	}
	
	private int getGameTimeOffset(GSBlockEventTime time) {
		return (int)(time.getGameTime() - startTime.getGameTime());
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
		
		int rowEnd = modelToView(endTime, true);
		
		List<GSTimeline> timelines = table.getTimelines();
		for (int timelineIndex = 0; timelineIndex < timelines.size(); timelineIndex++) {
			int y = getTimelineY(timelineIndex);
			renderTimeline(timelines.get(timelineIndex), y, rowHeight, mouseX, mouseY);
			y += rowHeight;
			fill(0, y, rowEnd, y + ROW_SPACING, ROW_SPACING_COLOR);
		}
	}
	
	private int getTimelineY(int timelineIndex) {
		return COLUMN_HEADER_HEIGHT + timelineIndex * (getRowHeight() + ROW_SPACING);
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
		int x0 = modelToView(entry.getStartTime(), false);
		int x1 = modelToView(entry.getEndTime(), true);
		
		if (mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1)
			color = darkenColor(color);
		
		fill(x0, y0, x1, y1, darkenColor(color));
		
		if (entry.getType() != GSETimelineEntryType.EVENT_BEGINNING)
			x1 -= ENTRY_BORDER_THICKNESS;
		if (entry.getType() != GSETimelineEntryType.EVENT_END)
			x0 += ENTRY_BORDER_THICKNESS;
		
		fill(x0, y0 + ENTRY_BORDER_THICKNESS, x1, y1 - ENTRY_BORDER_THICKNESS, color);
	}
	
	private void renderColumnHeaders() {
		fill(0, 0, width, COLUMN_HEADER_HEIGHT, COLUMN_HEADER_COLOR);

		int gridColor = brightenColor(ROW_SPACING_COLOR);
		
		int x0 = LABEL_COLUMN_WIDTH;
		for (int gt = 0; gt < gameTickDurations.length; gt++) {
			int x1 = x0;
			if (gt == expandedColumnIndex) {
				x1 += gameTickDurations[gt] * MT_COLUMN_WIDTH;
			} else {
				x1 += GAMETIME_COLUMN_WIDTH;
			}

			int color = TEXT_COLOR;
			int y;
			if (expandedColumnIndex != gt) {
				if (expandedColumnIndex != -1)
					color = darkenColor(TEXT_COLOR);
				y = (COLUMN_HEADER_HEIGHT - font.fontHeight) / 2;
			} else {
				y = (COLUMN_HEADER_HEIGHT / 2 - font.fontHeight) / 2;
			}
			
			long gameTime = startTime.getGameTime() + gt;
			String title = String.format(Locale.ENGLISH, "%dgt", gameTime);
			drawCenteredString(font, title, (x0 + x1) / 2, y, color);
			
			fill(x0, 0, x0 + 1, height, gridColor);

			x0 = x1;
		}
		
		fill(x0, 0, x0 + 1, height, gridColor);

		if (expandedColumnIndex != -1) {
			int mtGridColor = brightenColor(gridColor);
			
			int gameTickDuration = gameTickDurations[expandedColumnIndex];
			int xe = LABEL_COLUMN_WIDTH + expandedColumnIndex * GAMETIME_COLUMN_WIDTH;
			int ye = COLUMN_HEADER_HEIGHT * 3 / 4 - font.fontHeight / 2;
			for (int mt = 0; mt < gameTickDuration; mt++) {
				String title = (mt == gameTickDuration - 1) ? "-" : String.format(Locale.ENGLISH, "%d", mt);
				int xx = xe + mt * MT_COLUMN_WIDTH;
				drawCenteredString(font, title, xx + MT_COLUMN_WIDTH / 2, ye, TEXT_COLOR);
			
				if (mt != 0)
					drawDottedLine(xx, COLUMN_HEADER_HEIGHT / 2, height, mtGridColor);
			}
			
			int ys = COLUMN_HEADER_HEIGHT / 2;
			fill(xe, ys - 1, xe + gameTickDuration * MT_COLUMN_WIDTH, ys, mtGridColor);
		}
		
		fill(0, COLUMN_HEADER_HEIGHT - 1, Math.min(width, x0), COLUMN_HEADER_HEIGHT, ROW_SPACING_COLOR);
	}
	
	private void drawDottedLine(int x, int y0, int y1, int color) {
		int n = (y1 - y0) / DOTTED_LINE_LENGTH / 2;
		
		for (int yl = 0; yl <= n; yl++) {
			int yl0 = y0 + 2 * yl * DOTTED_LINE_LENGTH;
			int yl1 = Math.min(yl0 + DOTTED_LINE_LENGTH, y1);
			fill(x, yl0, x + 1, yl1, color);
		}
	}
	
	private void renderHoveredInfo(int mouseX, int mouseY) {
		int hoveredTimelineIndex = getHoveredTimelineIndex(mouseX, mouseY);
		if (hoveredTimelineIndex != -1) {
			GSTimeline hoveredTimeline = table.getTimelines().get(hoveredTimelineIndex);
			
			String text = null;
			GSBlockEventTime mouseTime = viewToModel(mouseX);
			if (mouseTime != null) {
				int gameTickOffset = getGametickOffsetFromView(mouseX);
				
				if (gameTickOffset != -1) {
					GSTimelineEntry hoveredEntry = hoveredTimeline.getEntryAt(mouseTime, (gameTickOffset == expandedColumnIndex));
					if (hoveredEntry != null) {
						text = String.format(Locale.ENGLISH, "Duration: %dgt", hoveredEntry.getGameTimeDuration());

						renderHoveredEdge(hoveredEntry, hoveredTimelineIndex, mouseX, mouseY);
					}
				}
			}
			
			if (text == null) {
				BlockPos pos = hoveredTimeline.getInfo().getPos();
				text = String.format(Locale.ENGLISH, "Pos: (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
			}

			renderHoveredInfoText(text);
		}
	}
	
	private void renderHoveredEdge(GSTimelineEntry hoveredEntry, int hoveredTimelineIndex, int mouseX, int mouseY) {
		int x = -1;
		if (hoveringStartResizeArea(hoveredEntry, mouseX) || (hoveredEntry == draggedEntry && draggingFlag == RESIZING_START)) {
			x = modelToView(hoveredEntry.getStartTime(), false);
		} else if (hoveringEndResizeArea(hoveredEntry, mouseX) || (hoveredEntry == draggedEntry && draggingFlag == RESIZING_END)) {
			x = modelToView(hoveredEntry.getEndTime(), true) - DRAGGING_AREA_SIZE;
		}
		
		if (x >= 0) {
			int y0 = getTimelineY(hoveredTimelineIndex) + (getRowHeight() - ENTRY_HEIGHT) / 2 - DRAGGING_PADDING;
			int y1 = y0 + ENTRY_HEIGHT + DRAGGING_PADDING * 2;
			fill(x - DRAGGING_PADDING, y0, x + DRAGGING_AREA_SIZE + DRAGGING_PADDING, y1, DRAGGING_AREA_COLOR);
		}
	}

	private GSTimeline getHoveredTimeline(int mouseX, int mouseY) {
		int index = getHoveredTimelineIndex(mouseX, mouseY);
		return (index == -1) ? null : table.getTimelines().get(index);
	}
	
	private int getHoveredTimelineIndex(int mouseX, int mouseY) {
		if (mouseX >= 0 && mouseX < width && mouseY >= COLUMN_HEADER_HEIGHT && mouseY < height) {
			int hoveredIndex = (mouseY - COLUMN_HEADER_HEIGHT) / (getRowHeight() + ROW_SPACING);
			List<GSTimeline> timelines = table.getTimelines();
			if (hoveredIndex >= 0 && hoveredIndex < timelines.size())
				return hoveredIndex;
		}
		
		return -1;
	}
	
	private void renderHoveredInfoText(String text) {
		drawCenteredString(font, text, LABEL_COLUMN_WIDTH / 2, (COLUMN_HEADER_HEIGHT - font.fontHeight) / 2, TEXT_COLOR);
	}
	
	@Override
	public boolean mouseClickedTranslated(double mouseX, double mouseY, int button) {
		int mx = (int)mouseX;
		int my = (int)mouseY;
		
		if (button == GLFW.GLFW_MOUSE_BUTTON_1 && mouseY >= 0.0) {
			GSBlockEventTime time = viewToModel(mx);
			if (time != null) {
				if (mouseY < COLUMN_HEADER_HEIGHT) {
					int gameTimeOffset = (int)(time.getGameTime() - startTime.getGameTime());
					expandedColumnIndex = (gameTimeOffset == expandedColumnIndex) ? -1 : gameTimeOffset;
					return true;
				} else {
					GSTimeline clickedTimeline = getHoveredTimeline(mx, my);
					if (clickedTimeline != null && (expandedColumnIndex == -1 || expandedColumnIndex == time.getGameTime())) {
						GSTimelineEntry clickedEntry = clickedTimeline.getEntryAt(time, (expandedColumnIndex != -1));
						if (clickedEntry != null) {

							if (hoveringStartResizeArea(clickedEntry, mouseX)) {
								draggingFlag = RESIZING_START;
							} else if (hoveringEndResizeArea(clickedEntry, mouseX)) {
								draggingFlag = RESIZING_END;
							} else {
								draggingFlag = NOT_RESIZING;
							}

							if (expandedColumnIndex == -1 || draggingFlag != NOT_RESIZING || clickedEntry.getGameTimeDuration() == 0L) {
								draggedTimeline = clickedTimeline;
								draggedEntry = clickedEntry;
								
								draggedStartTime = clickedEntry.getStartTime();
								draggedEndTime = clickedEntry.getEndTime();
								mouseClickedX = mouseX;
							}
							
							return true;
						}
					}
				}
			}
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_2 && mouseY >= COLUMN_HEADER_HEIGHT) {
			GSTimeline clickedTimeline = getHoveredTimeline(mx, my);
			if (clickedTimeline != null) {
				GSTimelineEntry clickedEntry = clickedTimeline.getEntryAt(viewToModel(mx), (expandedColumnIndex != -1));
				if (clickedEntry != null) {
					GSETimelineEntryType newType = null;
					if (hoveringStartResizeArea(clickedEntry, mouseX)) {
						newType = GSETimelineEntryType.EVENT_END;
					} else if (hoveringEndResizeArea(clickedEntry, mouseX)) {
						newType = GSETimelineEntryType.EVENT_BEGINNING;
					}
					
					if (newType != null)
						clickedEntry.setType((newType == clickedEntry.getType()) ? GSETimelineEntryType.EVENT_BOTH : newType);
				}
			}
		}
		
		return super.mouseClickedTranslated(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleasedTranslated(double mouseX, double mouseY, int button) {
		draggedTimeline = null;
		draggedEntry = null;
		draggingFlag = NOT_RESIZING;
		return super.mouseReleasedTranslated(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDraggedTranslated(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
			if (draggedTimeline != null && draggedEntry != null) {
				boolean changed = false;
				switch (draggingFlag) {
				case NOT_RESIZING:
					changed = moveDraggedEntry((int)(mouseX - mouseClickedX));
					break;
				case RESIZING_START:
					changed = changeDraggedStart(viewToModel((int)mouseX));
					break;
				case RESIZING_END:
					changed = changeDraggedEnd(viewToModel((int)mouseX));
					break;
				}
					
				if (changed)
					updateGameTickTimes();
		}
				
			return true;
		}
		
		return super.mouseDraggedTranslated(mouseX, mouseY, button, dragX, dragY);
	}

	private boolean moveDraggedEntry(int deltaX) {
		boolean expanded = (expandedColumnIndex != -1);
		
		int columnWidth = expanded ? MT_COLUMN_WIDTH : GAMETIME_COLUMN_WIDTH;
		int deltaTicks = deltaX / columnWidth;
		
		GSBlockEventTime startTime = draggedStartTime;
		GSBlockEventTime endTime = draggedEndTime;
		
		if (expanded) {
			startTime = startTime.offsetCopy(0, deltaTicks);
			endTime = endTime.offsetCopy(0, deltaTicks);
		} else {
			startTime = startTime.offsetCopy(deltaTicks, 0);
			endTime = endTime.offsetCopy(deltaTicks, 0);
		}
		
		if (startTime.getGameTime() >= 0 && startTime.getBlockEventDelay() >= 0) {
			GSTimelineEntry overlappingStart = draggedTimeline.getEntryAt(startTime, expanded);
			GSTimelineEntry overlappingEnd = draggedTimeline.getEntryAt(endTime, expanded);
		
			if ((overlappingStart == null || overlappingStart == draggedEntry) && 
			    (overlappingEnd == null || overlappingEnd == draggedEntry)) {
				
				draggedEntry.setTimespan(startTime, endTime);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean isValidDraggedTime(GSBlockEventTime time, GSBlockEventTime currentTime) {
		return time != null && (expandedColumnIndex == -1 || time.getGameTime() == currentTime.getGameTime());
	}
	
	private boolean changeDraggedStart(GSBlockEventTime time) {
		if (isValidDraggedTime(time, draggedEntry.getStartTime())) {
			GSBlockEventTime newTime = offsetDraggedTime(draggedEntry.getStartTime(), time);
			GSBlockEventTime endTime = draggedEntry.getEndTime();
			if (!newTime.isAfter(endTime) && !draggedTimeline.isOverlappingEntries(newTime, endTime, draggedEntry))
				draggedEntry.setStartTime(newTime);
			return true;
		}
		
		return false;
	}

	private boolean changeDraggedEnd(GSBlockEventTime time) {
		if (isValidDraggedTime(time, draggedEntry.getEndTime())) {
			GSBlockEventTime newTime = offsetDraggedTime(draggedEntry.getEndTime(), time);
			GSBlockEventTime startTime = draggedEntry.getStartTime();
			if (!newTime.isBefore(startTime) && !draggedTimeline.isOverlappingEntries(startTime, newTime, draggedEntry))
				draggedEntry.setEndTime(newTime);
			return true;
		}
		
		return false;
	}

	private GSBlockEventTime offsetDraggedTime(GSBlockEventTime t0, GSBlockEventTime t1) {
		return (expandedColumnIndex != -1) ? t1 : new GSBlockEventTime(t1.getGameTime(), t0.getBlockEventDelay());
	}
	
	private boolean hoveringStartResizeArea(GSTimelineEntry entry, double mouseX) {
		GSBlockEventTime time = viewToModel((int)mouseX);
		if (time == null || (expandedColumnIndex != -1 && expandedColumnIndex != time.getGameTime()))
			return false;

		GSBlockEventTime startTime = entry.getStartTime();
		if (expandedColumnIndex != -1 && startTime.getBlockEventDelay() != time.getBlockEventDelay())
			return false;
		if (startTime.getGameTime() != time.getGameTime())
			return false;
			
		return (mouseX < modelToView(startTime, false) + DRAGGING_AREA_SIZE);
	}

	private boolean hoveringEndResizeArea(GSTimelineEntry entry, double mouseX) {
		GSBlockEventTime time = viewToModel((int)mouseX);
		if (time == null || (expandedColumnIndex != -1 && expandedColumnIndex != time.getGameTime()))
			return false;

		GSBlockEventTime endTime = entry.getEndTime();
		if (expandedColumnIndex != -1 && endTime.getBlockEventDelay() != time.getBlockEventDelay())
			return false;
		if (endTime.getGameTime() != time.getGameTime())
			return false;
			
		return (mouseX > modelToView(endTime, true) - DRAGGING_AREA_SIZE);
	}

	private int modelToView(GSBlockEventTime time, boolean endTime) {
		int gameTimeOffset = getGameTimeOffset(time);
		if (gameTimeOffset < 0 || gameTimeOffset >= gameTickDurations.length)
			return -1;
		
		int x = LABEL_COLUMN_WIDTH;
		int mtOffset = 0; 
		if (gameTimeOffset == expandedColumnIndex) {
			mtOffset = time.getBlockEventDelay();
			if (endTime)
				mtOffset++;
		} else {
			if (expandedColumnIndex != -1 && gameTimeOffset > expandedColumnIndex) {
				mtOffset = gameTickDurations[expandedColumnIndex];
				gameTimeOffset--;
			}
			
			if (endTime)
				gameTimeOffset++;
		}

		x += gameTimeOffset * GAMETIME_COLUMN_WIDTH;
		x += mtOffset * MT_COLUMN_WIDTH;
		
		return x;
	}
	
	private int getGametickOffsetFromView(int mouseX) {
		mouseX -= LABEL_COLUMN_WIDTH;
		if (mouseX < 0)
			return -1;
		
		int gameTimeOffset = mouseX / GAMETIME_COLUMN_WIDTH;
		if (expandedColumnIndex != -1 && gameTimeOffset >= expandedColumnIndex) {
			mouseX -= expandedColumnIndex * GAMETIME_COLUMN_WIDTH;
			gameTimeOffset = expandedColumnIndex;

			mouseX -= gameTickDurations[expandedColumnIndex] * MT_COLUMN_WIDTH;
			if (mouseX > 0)
				gameTimeOffset += 1 + mouseX / GAMETIME_COLUMN_WIDTH;
		}
		
		if (gameTimeOffset >= gameTickDurations.length)
			return -1;
		
		return gameTimeOffset;
	}
	
	private GSBlockEventTime viewToModel(int x) {
		int gameTimeOffset = getGametickOffsetFromView(x);
		if (gameTimeOffset == -1)
			return null;
		
		int mt = 0;
		if (gameTimeOffset == expandedColumnIndex) {
			x -= LABEL_COLUMN_WIDTH;
			x -= expandedColumnIndex * GAMETIME_COLUMN_WIDTH;
			mt = x / MT_COLUMN_WIDTH;
		}
		
		if (mt < 0)
			return null;

		return new GSBlockEventTime(startTime.getGameTime() + gameTimeOffset, mt);
	}
}
