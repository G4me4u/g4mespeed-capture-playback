package com.g4mesoft.planner.gui;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.core.GSCoreOverride;
import com.g4mesoft.gui.GSParentGUI;
import com.g4mesoft.planner.module.GSPlannerModule;
import com.g4mesoft.planner.timeline.GSBlockEventTime;
import com.g4mesoft.planner.timeline.GSETrackEntryType;
import com.g4mesoft.planner.timeline.GSITimelineListener;
import com.g4mesoft.planner.timeline.GSTimeline;
import com.g4mesoft.planner.timeline.GSTrack;
import com.g4mesoft.planner.timeline.GSTrackEntry;

import net.minecraft.client.util.NarratorManager;
import net.minecraft.util.math.BlockPos;

public class GSTimelineGUI extends GSParentGUI implements GSITimelineListener {

	private static final int LABEL_COLUMN_COLOR = 0x80000000;
	private static final int COLUMN_HEADER_COLOR = 0x60000000;
	private static final int COLUMN_COLOR = 0x60000000;
	private static final int DARK_COLUMN_COLOR = 0x60202020;
	private static final int ROW_HOVER_COLOR = 0x30FFFFFF;

	private static final int ROW_SPACING_COLOR = 0xFF444444;
	private static final int COLUMN_LINE_COLOR = darkenColor(ROW_HOVER_COLOR);

	private static final int TEXT_COLOR = 0xFFFFFFFF;
	
	private static final int LABEL_COLUMN_WIDTH = 100;
	private static final int GAMETIME_COLUMN_WIDTH = 30;
	private static final int MULTI_COLUMN_WIDTH = 10;
	private static final int MT_COLUMN_WIDTH = 20;
	private static final int MINIMUM_ENTRY_WIDTH = 15;
	
	private static final int COLUMN_HEADER_HEIGHT = 30;
	private static final int ROW_LABEL_PADDING = 2;
	private static final int ENTRY_HEIGHT = 8;
	
	/* Add track button constants */
	private static final String ADD_TRACK_BUTTON_TEXT = "+ Add Track";
	private static final int ADD_TRACK_BUTTON_MARGIN = 2;
	private static final int ADD_TRACK_BUTTON_PADDING = 3;
	private static final int ADD_TRACK_BUTTON_COLOR = 0xFF222222;

	private static final double MINIMUM_DRAG_DISTANCE = 10.0;
	
	private static final int ROW_SPACING = 1;
	private static final int ENTRY_BORDER_THICKNESS = 2;

	private static final int DOTTED_LINE_LENGTH = 4;

	/* Dragging area constants */
	private static final int DRAGGING_AREA_SIZE = 6;
	private static final int DRAGGING_PADDING = 2;
	private static final int DRAGGING_AREA_COLOR = 0x40FFFFFF;

	/* Extra ticks that should be added to the timeline */
	private static final int EXTRA_GAMETICKS = 1;
	private static final int EXTRA_MICROTICKS = 1;

	/* Constants related to the dragging flag */
	private static final int NOT_DRAGGING = 0;
	private static final int NOT_RESIZING = 1;
	private static final int RESIZING_START = 2;
	private static final int RESIZING_END = 3;
	
	private final GSTimeline timeline;
	private final GSITrackProvider trackProvider;
	private final GSPlannerModule plannerModule;
	
	private int[] gameTickDurations;
	private int rowHeight;
	
	private GSBlockEventTime startTime;
	private GSBlockEventTime endTime;
	
	private int expandedColumnIndex;
	
	private int hoveredTrackIndex;
	private GSTrack hoveredTrack;
	private GSTrackEntry hoveredEntry;
	
	private int selectedTrackIndex;
	private GSTrack selectedTrack;
	private GSTrackEntry selectedEntry;
	private boolean toggleSelection;

	private double clickedMouseX;
	private double clickedMouseY;
	
	private int draggingFlag;
	private GSBlockEventTime draggedStartTime;
	private GSBlockEventTime draggedEndTime;
	private boolean draggedEntryChanged;
	
	private final Map<Integer, Map<Integer, Integer>> trackMultiCellCounts;
	
	private double currentMouseX;
	private double currentMouseY;
	
	protected GSTimelineGUI(GSTimeline timeline, GSITrackProvider trackProvider, GSPlannerModule plannerModule) {
		super(NarratorManager.EMPTY);
		
		this.timeline = timeline;
		this.trackProvider = trackProvider;
		this.plannerModule = plannerModule;
		
		trackMultiCellCounts = new HashMap<Integer, Map<Integer,Integer>>();
		
		initTimeline();
	}
	
	private void initTimeline() {
		updateGameTickTimes();
		expandedColumnIndex = -1;

		this.timeline.addTimelineListener(this);
	}
	
	@Override
	@GSCoreOverride
	public void init() {
		super.init();
		
		rowHeight = Math.max(ROW_LABEL_PADDING * 2 + font.fontHeight, ENTRY_HEIGHT);
	}
	
	private void updateGameTickTimes() {
		List<GSTrack> tracks = timeline.getTracks();
		
		startTime = GSBlockEventTime.ZERO;
		endTime = GSBlockEventTime.ZERO;
		
		for (GSTrack track : tracks) {
			for (GSTrackEntry entry : track.getEntries()) {
				if (endTime.isBefore(entry.getEndTime()))
					endTime = entry.getEndTime();
			}
		}
		
		endTime = new GSBlockEventTime(endTime.getGameTime() + EXTRA_GAMETICKS, endTime.getBlockEventDelay());
		
		int numGameTicks = (int)(endTime.getGameTime() - startTime.getGameTime()) + 1;
		gameTickDurations = new int[numGameTicks];
		Arrays.fill(gameTickDurations, 1);
		
		int[] columnEntryCount = new int[numGameTicks];
		trackMultiCellCounts.clear();
		
		for (int trackIndex = 0; trackIndex < tracks.size(); trackIndex++) {
			Arrays.fill(columnEntryCount, 0);
			
			GSTrack track = tracks.get(trackIndex);
			for (GSTrackEntry entry : track.getEntries()) {
				updateGameTickDuration(entry.getStartTime());
				updateGameTickDuration(entry.getEndTime());

				int startOffset = getGameTimeOffset(entry.getStartTime());
				int endOffset = getGameTimeOffset(entry.getEndTime());
			
				columnEntryCount[startOffset]++;
				if (startOffset != endOffset)
					columnEntryCount[endOffset]++;
			}
			
			Map<Integer, Integer> multiCellCount = new HashMap<Integer, Integer>();
			for (int gameTimeOffset = 0; gameTimeOffset < numGameTicks; gameTimeOffset++) {
				int entryCount = columnEntryCount[gameTimeOffset];
				if (entryCount > 1)
					multiCellCount.put(gameTimeOffset, entryCount);
			}
			
			trackMultiCellCounts.put(trackIndex, multiCellCount);
		}

		for (int i = 0; i < numGameTicks; i++)
			gameTickDurations[i] += EXTRA_MICROTICKS;
		
		if (expandedColumnIndex >= numGameTicks)
			expandedColumnIndex = -1;
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
	
		fill(0, 0, LABEL_COLUMN_WIDTH, height, LABEL_COLUMN_COLOR);
		fill(LABEL_COLUMN_WIDTH, 0, LABEL_COLUMN_WIDTH + 1, height, COLUMN_LINE_COLOR);
		
		renderColumnHeaders(mouseX, mouseY);
		renderTracks(mouseX, mouseY);
		renderHoveredInfo(mouseX, mouseY);
		renderHoveredEdge(mouseX, mouseY);
	
		renderAddTrackButton(mouseX, mouseY);
	}
	
	private void renderColumnHeaders(int mouseX, int mouseY) {
		fill(LABEL_COLUMN_WIDTH, 0, getRowEndX(), COLUMN_HEADER_HEIGHT, COLUMN_HEADER_COLOR);
		
		int x0 = LABEL_COLUMN_WIDTH;
		for (int gt = 0; gt < gameTickDurations.length; gt++) {
			int x1 = x0;
			if (gt == expandedColumnIndex) {
				x1 += gameTickDurations[gt] * MT_COLUMN_WIDTH;
			} else {
				x1 += GAMETIME_COLUMN_WIDTH;
			}

			int y;
			int color = TEXT_COLOR;
			if (expandedColumnIndex != gt) {
				if (expandedColumnIndex != -1)
					color = darkenColor(TEXT_COLOR);
				y = (COLUMN_HEADER_HEIGHT - font.fontHeight) / 2;
			} else {
				y = (COLUMN_HEADER_HEIGHT / 2 - font.fontHeight) / 2;
			}

			fill(x0, 0, x1, height, (gt & 0x1) != 0 ? COLUMN_COLOR : DARK_COLUMN_COLOR);
			drawCenteredString(font, getColumnTitle(gt), (x0 + x1) / 2, y, color);
			
			if (mouseX >= x0 && mouseX < x1) {
				fill(x0, 0, x0 + 1, height, COLUMN_LINE_COLOR);
				fill(x1 - 1, 0, x1, height, COLUMN_LINE_COLOR);
			}
			
			x0 = x1;
		}

		if (expandedColumnIndex != -1) {
			int mtGridColor = brightenColor(COLUMN_LINE_COLOR);
			
			int gameTickDuration = gameTickDurations[expandedColumnIndex];
			int xe = LABEL_COLUMN_WIDTH + expandedColumnIndex * GAMETIME_COLUMN_WIDTH;
			int ye = COLUMN_HEADER_HEIGHT * 3 / 4 - font.fontHeight / 2;
			for (int mt = 0; mt < gameTickDuration; mt++) {
				String title = (mt == gameTickDuration - 1) ? "-" : Integer.toString(mt);
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
	
	private int getRowEndX() {
		int endOffset = getGameTimeOffset(endTime);
		return getColumnX(endOffset) + getColumnWidth(endOffset);
	}

	private String getColumnTitle(int gameTimeOffset) {
		return Long.toString(startTime.getGameTime() + gameTimeOffset) + "gt";
	}
	
	private void drawDottedLine(int x, int y0, int y1, int color) {
		int n = (y1 - y0) / DOTTED_LINE_LENGTH / 2;
		
		for (int yl = 0; yl <= n; yl++) {
			int yl0 = y0 + 2 * yl * DOTTED_LINE_LENGTH;
			int yl1 = Math.min(yl0 + DOTTED_LINE_LENGTH, y1);
			fill(x, yl0, x + 1, yl1, color);
		}
	}
	
	private void renderTracks(int mouseX, int mouseY) {
		int rowEnd = getRowEndX();
		
		List<GSTrack> tracks = timeline.getTracks();
		for (int trackIndex = 0; trackIndex < tracks.size(); trackIndex++) {
			int y = getTrackY(trackIndex);
			
			renderTrack(tracks.get(trackIndex), trackIndex, y, mouseX, mouseY);
			y += rowHeight;
			
			fill(0, y, rowEnd, y + ROW_SPACING, ROW_SPACING_COLOR);
		}
	}
	
	private void renderTrack(GSTrack track, int timelineIndex, int y, int mouseX, int mouseY) {
		int color = (0xFF << 24) | track.getInfo().getColor();
		
		if (mouseX >= 0 && mouseX < width && mouseY >= y && mouseY < y + rowHeight + ROW_SPACING)
			fill(0, y, width, y + rowHeight, ROW_HOVER_COLOR);
		
		for (GSTrackEntry entry : track.getEntries())
			renderTrackEntry(timelineIndex, entry, color);
		
		renderMultiCells(timelineIndex, y);
		
		String name = trimText(track.getInfo().getName(), LABEL_COLUMN_WIDTH);
		int x = (LABEL_COLUMN_WIDTH - font.getStringWidth(name)) / 2;
		drawString(font, name, x, y + (rowHeight - font.fontHeight) / 2, color);
	}
	
	private void renderTrackEntry(int trackIndex, GSTrackEntry entry, int color) {
		if (selectedEntry == entry || hoveredEntry == entry)
			color = darkenColor(color);

		Rectangle rect = modelToView(trackIndex, entry);
		
		if (rect != null) {
			int x1 = rect.x + rect.width;
			int y1 = rect.y + rect.height;

			fill(rect.x, rect.y, x1, y1, darkenColor(color));
			
			int x0 = rect.x;
			if (entry.getType() != GSETrackEntryType.EVENT_START)
				x1 -= ENTRY_BORDER_THICKNESS;
			if (entry.getType() != GSETrackEntryType.EVENT_END)
				x0 += ENTRY_BORDER_THICKNESS;
			
			fill(x0, rect.y + ENTRY_BORDER_THICKNESS, x1, y1 - ENTRY_BORDER_THICKNESS, color);
		}
	}
	
	private void renderMultiCells(int trackIndex, int y) {
		Map<Integer, Integer> multiCellCounts = trackMultiCellCounts.get(trackIndex);
		if (multiCellCounts == null)
			return;
		
		for (Map.Entry<Integer, Integer> multiCellInfo : multiCellCounts.entrySet()) {
			int gameTimeOffset = multiCellInfo.getKey();
			if (gameTimeOffset != expandedColumnIndex) {
				int count = multiCellInfo.getValue();

				String infoText = (count > 9) ? "+" : Integer.toString(count);
				int xc = getColumnX(gameTimeOffset) + getColumnWidth(gameTimeOffset) / 2;
				drawCenteredString(font, infoText, xc, y + (rowHeight - font.fontHeight) / 2, TEXT_COLOR);
			}
		}
	}
	
	private void renderHoveredInfo(int mouseX, int mouseY) {
		String text = null;
		if (hoveredEntry != null) {
			text = String.format(Locale.ENGLISH, "Duration: %dgt", hoveredEntry.getGameTimeDuration());
		} else if (hoveredTrack != null) {
			BlockPos pos = hoveredTrack.getInfo().getPos();
			text = String.format(Locale.ENGLISH, "Pos: (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
		}

		if (text != null)
			drawCenteredString(font, text, LABEL_COLUMN_WIDTH / 2, (COLUMN_HEADER_HEIGHT - font.fontHeight) / 2, TEXT_COLOR);
	}
	
	private void renderHoveredEdge(int mouseX, int mouseY) {
		if (hoveredEntry == null && draggingFlag != RESIZING_START && draggingFlag != RESIZING_END)
			return;
		
		Rectangle hoveredRect = null;
		boolean endTime = false;
		if (draggingFlag != NOT_DRAGGING) {
			switch (draggingFlag) {
			case RESIZING_END:
				endTime = true;
			case RESIZING_START:
				hoveredRect = modelToView(selectedTrackIndex, selectedEntry);
				break;
			}
		} else {
			GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrackIndex, hoveredEntry, mouseX, mouseY);
			if (resizeArea != null) {
				hoveredRect = modelToView(hoveredTrackIndex, hoveredEntry);
				endTime = (resizeArea == GSResizeArea.HOVERING_END);
			}
		}
		
		if (hoveredRect != null) {
			int x0;
			if (endTime) {
				x0 = hoveredRect.x + hoveredRect.width - DRAGGING_AREA_SIZE;
			} else {
				x0 = hoveredRect.x - DRAGGING_PADDING;
			}
			
			int y0 = hoveredRect.y - DRAGGING_PADDING;
			int y1 = hoveredRect.y + hoveredRect.height + DRAGGING_PADDING;
			int x1 = x0 + DRAGGING_AREA_SIZE + DRAGGING_PADDING;
			fill(x0, y0, x1, y1, DRAGGING_AREA_COLOR);
		}
	}

	private int getTrackIndexFromView(int mouseX, int mouseY) {
		if (mouseX >= 0 && mouseX < width && mouseY >= COLUMN_HEADER_HEIGHT && mouseY < height) {
			int hoveredIndex = (mouseY - COLUMN_HEADER_HEIGHT) / (rowHeight + ROW_SPACING);
			List<GSTrack> tracks = timeline.getTracks();
			if (hoveredIndex >= 0 && hoveredIndex < tracks.size())
				return hoveredIndex;
		}
		
		return -1;
	}
	
	private void renderAddTrackButton(int mouseX, int mouseY) {
		Rectangle rect = getAddTrackButtonBounds();
		
		int color = ADD_TRACK_BUTTON_COLOR;
		if (rect.contains(mouseX, mouseY))
			color = brightenColor(brightenColor(color));
		
		int x1 = rect.x + rect.width;
		int y1 = rect.y + rect.height;
		
		fill(rect.x, rect.y, x1, y1, brightenColor(color));
		fill(rect.x + 1, rect.y + 1, x1 - 1, y1 - 1, darkenColor(color));
		
		int xt = rect.x + rect.width / 2;
		int yt = rect.y + (rect.height - font.fontHeight) / 2;
		drawCenteredString(font, ADD_TRACK_BUTTON_TEXT, xt, yt, TEXT_COLOR);
	}

	private Rectangle getAddTrackButtonBounds() {
		int textWidth = font.getStringWidth(ADD_TRACK_BUTTON_TEXT);

		Rectangle rect = new Rectangle();
		rect.x = (LABEL_COLUMN_WIDTH - textWidth) / 2 - ADD_TRACK_BUTTON_PADDING;
		rect.width = textWidth + ADD_TRACK_BUTTON_PADDING * 2;
		rect.y = getTrackY(timeline.getTracks().size()) + ADD_TRACK_BUTTON_MARGIN;
		rect.height = font.fontHeight + ADD_TRACK_BUTTON_PADDING * 2;
		
		return rect;
	}
	
	@Override
	public void mouseMovedTranslated(double mouseX, double mouseY) {
		super.mouseMovedTranslated(mouseX, mouseY);
		
		int mx = (int)mouseX;
		int my = (int)mouseY;
		hoveredTrackIndex = getTrackIndexFromView(mx, my);
		if (hoveredTrackIndex != -1) {
			hoveredTrack = timeline.getTracks().get(hoveredTrackIndex);
			
			GSBlockEventTime time = viewToModel(mx, my);
			if (time != null && hoveredTrack != null) {
				boolean multiCell = isMultiCell(hoveredTrackIndex, getGameTimeOffset(time));
				hoveredEntry = hoveredTrack.getEntryAt(time, (time.getGameTime() == expandedColumnIndex) || multiCell);
				
				if (hoveredEntry != null) {
					Rectangle rect = modelToView(hoveredTrackIndex, hoveredEntry);
					if (rect == null || !rect.contains(mouseX, mouseY))
						hoveredEntry = null;
				}
			}
		} else {
			hoveredTrack = null;
			hoveredEntry = null;
		}
		
		currentMouseX = mouseX;
		currentMouseY = mouseY;
	}
	
	private boolean expandHoveredTab(double mouseX, double mouseY, boolean allowToggle) {
		GSBlockEventTime time = viewToModel((int)mouseX, (int)mouseY);
		if (time != null) {
			int gameTimeOffset = (int)(time.getGameTime() - startTime.getGameTime());
			if (gameTimeOffset == expandedColumnIndex && allowToggle) {
				expandedColumnIndex = -1;
			} else {
				expandedColumnIndex = gameTimeOffset;
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean mouseClickedTranslated(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1 && mouseY >= 0.0) {
			clickedMouseX = mouseX;
			clickedMouseY = mouseY;
			
			if (mouseY < COLUMN_HEADER_HEIGHT) {
				if (expandHoveredTab(mouseX, mouseY, true))
					return true;
			} else if (hoveredEntry != null) {
				updateSelectedEntry();
				
				GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrackIndex, hoveredEntry, (int)mouseX, (int)mouseY);

				int newDraggingFlag;
				if (resizeArea != null) {
					newDraggingFlag = (resizeArea == GSResizeArea.HOVERING_START) ? RESIZING_START : RESIZING_END;
				} else {
					newDraggingFlag = NOT_RESIZING;
				}
				
				if (expandedColumnIndex == -1 || newDraggingFlag != NOT_RESIZING || hoveredEntry.getGameTimeDuration() == 0L) {
					draggingFlag = newDraggingFlag;
					draggedStartTime = hoveredEntry.getStartTime();
					draggedEndTime = hoveredEntry.getEndTime();
				}
			} else if (mouseX < LABEL_COLUMN_WIDTH && getAddTrackButtonBounds().contains(mouseX, mouseY)) {
				timeline.addTrack(trackProvider.createNewTrackInfo());
			}

			return true;
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_2 && mouseY >= COLUMN_HEADER_HEIGHT) {
			if (hoveredEntry != null) {
				GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrackIndex, hoveredEntry, (int)mouseX, (int)mouseY);
				if (resizeArea != null) {
					GSETrackEntryType newType = (resizeArea == GSResizeArea.HOVERING_START) ? 
							GSETrackEntryType.EVENT_END : GSETrackEntryType.EVENT_START;
					hoveredEntry.setType((newType == hoveredEntry.getType()) ? GSETrackEntryType.EVENT_BOTH : newType);
				} else if (draggingFlag == NOT_DRAGGING) {
					hoveredTrack.removeEntry(hoveredEntry);
				}
			}
		}
		
		return super.mouseClickedTranslated(mouseX, mouseY, button);
	}
	
	private void updateSelectedEntry() {
		if (hoveredEntry != selectedEntry) {
			selectedTrackIndex = hoveredTrackIndex;
			selectedTrack = hoveredTrack;
			selectedEntry = hoveredEntry;
			
			toggleSelection = false;
		}
	}
	
	private void unselectEntries() {
		selectedTrackIndex = -1;
		selectedTrack = null;
		selectedEntry = null;
	}
	
	@Override
	public boolean mouseReleasedTranslated(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1 && mouseY > COLUMN_HEADER_HEIGHT) {
			if (draggedEntryChanged || toggleSelection)
				unselectEntries();
			toggleSelection = (selectedEntry != null);
		}
		
		draggingFlag = NOT_DRAGGING;
		draggedEntryChanged = false;
		
		return super.mouseReleasedTranslated(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDraggedTranslated(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
			if (selectedTrack != null && selectedEntry != null) {
				if (draggingFlag != NOT_DRAGGING) {
					boolean changed;
					switch (draggingFlag) {
					case NOT_RESIZING:
						changed = moveDraggedEntry((int)(mouseX - clickedMouseX));
						break;
					case RESIZING_START:
						changed = changeDraggedStart(getDraggedTime(mouseX, mouseY));
						break;
					case RESIZING_END:
						changed = changeDraggedEnd(getDraggedTime(mouseX, mouseY));
						break;
					
					default:
						changed = false;
						break;
					}
						
					if (changed)
						draggedEntryChanged = true;
	
					return true;
				}
			} else if (hoveredTrack != null && hoveredEntry == null) {
				double dx = mouseX - clickedMouseX;
				double dy = mouseY - clickedMouseY;

				if (dx * dx + dy * dy > MINIMUM_DRAG_DISTANCE) {
					GSBlockEventTime t0 = getDraggedTime(clickedMouseX, clickedMouseY);
					GSBlockEventTime t1 = getDraggedTime(mouseX, mouseY);
				
					if (t0 != null && t1 != null) {
						int draggingFlag = RESIZING_END;
						if (t0.isAfter(t1)) {
							GSBlockEventTime tmp = t0;
							t0 = t1;
							t1 = tmp;
							
							draggingFlag = RESIZING_START;
						}
						
						if (expandedColumnIndex != -1) {
							if (t0.getGameTime() != t1.getGameTime() || t0.getGameTime() != expandedColumnIndex)
								t0 = t1 = null;
						} else {
							t0 = new GSBlockEventTime(t0.getGameTime(), 0);
							t1 = new GSBlockEventTime(t1.getGameTime(), 0);
						}
						
						if (t0 != null && t1 != null) {
							GSTrackEntry entry = hoveredTrack.tryAddEntry(t0, t1);
							if (entry != null) {
								hoveredEntry = entry;
								updateSelectedEntry();
	
								this.draggingFlag = draggingFlag;
								draggedStartTime = hoveredEntry.getStartTime();
								draggedEndTime = hoveredEntry.getEndTime();
							}
						}
					}
				}
			}
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
			if (selectedEntry.getStartTime().isEqual(startTime))
				return false;
			
			if (!selectedTrack.isOverlappingEntries(startTime, endTime, selectedEntry)) {
				selectedEntry.setTimespan(startTime, endTime);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean isValidDraggedTime(GSBlockEventTime time, GSBlockEventTime currentTime) {
		if (time == null)
			return false;
		
		if (expandedColumnIndex != -1)
			return time.getGameTime() == currentTime.getGameTime() && !time.isEqual(currentTime);
		return time.getGameTime() != currentTime.getGameTime();
	}
	
	private boolean changeDraggedStart(GSBlockEventTime time) {
		if (isValidDraggedTime(time, selectedEntry.getStartTime())) {
			GSBlockEventTime newTime = offsetDraggedTime(selectedEntry.getStartTime(), time);
			GSBlockEventTime endTime = selectedEntry.getEndTime();
			if (!newTime.isAfter(endTime) && !selectedTrack.isOverlappingEntries(newTime, endTime, selectedEntry))
				selectedEntry.setStartTime(newTime);
			return true;
		}
		
		return false;
	}

	private boolean changeDraggedEnd(GSBlockEventTime time) {
		if (isValidDraggedTime(time, selectedEntry.getEndTime())) {
			GSBlockEventTime newTime = offsetDraggedTime(selectedEntry.getEndTime(), time);
			GSBlockEventTime startTime = selectedEntry.getStartTime();
			if (!newTime.isBefore(startTime) && !selectedTrack.isOverlappingEntries(startTime, newTime, selectedEntry))
				selectedEntry.setEndTime(newTime);
			return true;
		}
		
		return false;
	}

	private GSBlockEventTime offsetDraggedTime(GSBlockEventTime t0, GSBlockEventTime t1) {
		return (expandedColumnIndex != -1) ? t1 : new GSBlockEventTime(t1.getGameTime(), t0.getBlockEventDelay());
	}
	
	private GSResizeArea getHoveredResizeArea(int trackIndex, GSTrackEntry entry, int mouseX, int mouseY) {
		Rectangle r = modelToView(trackIndex, entry);
		if (r == null)
			return null;
		
		GSResizeArea resizeArea = null;
		if (mouseX >= r.x && mouseX < r.x + DRAGGING_AREA_SIZE) {
			if (expandedColumnIndex == -1 || expandedColumnIndex == entry.getStartTime().getGameTime())
				resizeArea = GSResizeArea.HOVERING_START;
		} else if (mouseX < r.x + r.width && mouseX >= r.x + r.width - DRAGGING_AREA_SIZE) {
			if (expandedColumnIndex == -1 || expandedColumnIndex == entry.getEndTime().getGameTime())
				resizeArea = GSResizeArea.HOVERING_END;
		}
		
		return (mouseY >= r.y && mouseY < r.y + r.height) ? resizeArea : null;
	}
	
	@Override
	@GSCoreOverride
	public boolean keyPressed(int key, int scancode, int mods) {
		if (key == plannerModule.getCollapseTabKey().getGLFWKeyCode() && expandedColumnIndex != -1) {
			expandedColumnIndex = -1;
			return true;
		} else if (key == plannerModule.getExpandHoveredTabKey().getGLFWKeyCode()) {
			if (expandHoveredTab(currentMouseX, currentMouseY, false))
				return true;
		}
		
		return super.keyPressed(key, scancode, mods);
	}

	private int getMultiCellCount(int trackIndex, int gameTimeOffset) {
		Map<Integer, Integer> multiCellCounts = trackMultiCellCounts.get(trackIndex);
		if (multiCellCounts == null)
			return -1;

		Integer count = multiCellCounts.get(gameTimeOffset);
		return (count == null) ? -1 : count.intValue();
	}
	
	private boolean isMultiCell(int trackIndex, int gameTimeOffset) {
		return getMultiCellCount(trackIndex, gameTimeOffset) != -1;
	}
	
	private Rectangle modelToView(int trackIndex, GSTrackEntry entry) {
		int startOffset = getGameTimeOffset(entry.getStartTime());
		int endOffset = getGameTimeOffset(entry.getEndTime());
		if (startOffset < 0 || endOffset >= gameTickDurations.length)
			return null;
		
		// Check if we are even supposed to be rendering
		// the current entry.
		if (startOffset == endOffset && expandedColumnIndex != startOffset && isMultiCell(trackIndex, startOffset))
			return null;
			
		Rectangle rect = new Rectangle();
		rect.y = getEntryY(trackIndex);
		rect.height = ENTRY_HEIGHT;
		
		rect.x = getTimeViewX(trackIndex, entry.getStartTime(), false);
		rect.width = getTimeViewX(trackIndex, entry.getEndTime(), true) - rect.x;
		
		if (rect.width < MINIMUM_ENTRY_WIDTH) {
			rect.x -= (MINIMUM_ENTRY_WIDTH - rect.width) / 2;
			rect.width = MINIMUM_ENTRY_WIDTH;
		}
		
		if (entry.getStartTime().isEqual(GSBlockEventTime.ZERO) && entry.getType() == GSETrackEntryType.EVENT_END) {
			int x0 = getColumnX(0);
			rect.width += rect.x - x0;
			rect.x = x0;
		}
		
		return rect;
	}
	
	private int getTimeViewX(int trackIndex, GSBlockEventTime time, boolean endTime) {
		int gameTimeOffset = getGameTimeOffset(time);
		
		int x = getColumnX(gameTimeOffset);
		if (gameTimeOffset == expandedColumnIndex) {
			x += time.getBlockEventDelay() * MT_COLUMN_WIDTH;

			x += MT_COLUMN_WIDTH / 2;
		} else if (isMultiCell(trackIndex, gameTimeOffset)) {
			x += endTime ? MULTI_COLUMN_WIDTH : (GAMETIME_COLUMN_WIDTH - MULTI_COLUMN_WIDTH);
		} else {
			x += GAMETIME_COLUMN_WIDTH / 2;
		}
		
		return x;
	}
	
	private GSBlockEventTime getDraggedTime(double mouseX, double mouseY) {
		if (expandedColumnIndex != -1) {
			int columnOffset = (int)(mouseX - getColumnX(expandedColumnIndex));
			if (columnOffset < 0)
				return null;
			
			int mt = columnOffset / MT_COLUMN_WIDTH;
			return new GSBlockEventTime(startTime.getGameTime() + expandedColumnIndex, mt);
		}
		
		return viewToModel((int)mouseX, (int)mouseY);
	}
	
	private GSBlockEventTime viewToModel(int x, int y) {
		int gameTimeOffset = getGametickOffsetFromView(x);
		if (gameTimeOffset == -1)
			return null;
		
		int mt = 0;
		if (gameTimeOffset == expandedColumnIndex) {
			mt = (x - getColumnX(gameTimeOffset)) / MT_COLUMN_WIDTH;
		} else {
			int trackIndex = getTrackIndexFromView(x, y);
			int gameTickDuration = gameTickDurations[gameTimeOffset];
			
			if (trackIndex != -1 && isMultiCell(trackIndex, gameTimeOffset)) {
				int columnOffset = x - getColumnX(gameTimeOffset);
				mt = (columnOffset > GAMETIME_COLUMN_WIDTH - MULTI_COLUMN_WIDTH) ? gameTickDuration : 
					(columnOffset < MULTI_COLUMN_WIDTH) ? 0 : gameTickDuration / 2;
			} else {
				mt = gameTickDuration / 2;
			}
		}
		
		if (mt < 0)
			return null;

		return new GSBlockEventTime(startTime.getGameTime() + gameTimeOffset, mt);
	}
	
	private int getGametickOffsetFromView(int mouseX) {
		if (mouseX < getColumnX(0))
			return -1;
		
		int gameTimeOffset = (mouseX - getColumnX(0)) / GAMETIME_COLUMN_WIDTH;
		if (expandedColumnIndex != -1 && gameTimeOffset >= expandedColumnIndex) {
			gameTimeOffset = expandedColumnIndex;

			int offset = mouseX - getColumnX(expandedColumnIndex) - getColumnWidth(expandedColumnIndex);
			if (offset > 0)
				gameTimeOffset += 1 + offset / GAMETIME_COLUMN_WIDTH;
		}
		
		if (gameTimeOffset >= gameTickDurations.length)
			return -1;
		
		return gameTimeOffset;
	}
	
	private int getColumnX(int gameTimeOffset) {
		int x = LABEL_COLUMN_WIDTH + gameTimeOffset * GAMETIME_COLUMN_WIDTH;
		if (expandedColumnIndex != -1 && gameTimeOffset > expandedColumnIndex)
			x += gameTickDurations[expandedColumnIndex] * MT_COLUMN_WIDTH - GAMETIME_COLUMN_WIDTH;
		return x;
	}
	
	private int getColumnWidth(int gameTimeOffset) {
		return (expandedColumnIndex != gameTimeOffset) ? GAMETIME_COLUMN_WIDTH :
			gameTickDurations[expandedColumnIndex] * MT_COLUMN_WIDTH;
	}
	
	private int getTrackY(int trackIndex) {
		return COLUMN_HEADER_HEIGHT + trackIndex * (rowHeight + ROW_SPACING);
	}
	
	private int getEntryY(int trackIndex) {
		return getTrackY(trackIndex) + (rowHeight - ENTRY_HEIGHT) / 2;
	}

	@Override
	public void timelinePropertyChanged(int property) {
	}

	@Override
	public void trackAdded(GSTrack track) {
	}

	@Override
	public void trackPropertyChanged(GSTrack track, int property) {
	}

	@Override
	public void entryAdded(GSTrack track, GSTrackEntry entry) {
		updateGameTickTimes();
	}

	@Override
	public void entryRemoved(GSTrack track, GSTrackEntry entry) {
		if (entry == hoveredEntry)
			hoveredEntry = null;
		if (entry == selectedEntry)
			unselectEntries();
		
		updateGameTickTimes();
	}

	@Override
	public void entryPropertyChanged(GSTrack track, GSTrackEntry entry, int property) {
		if (property == GSTrackEntry.PROPERTY_TIMESPAN)
			updateGameTickTimes();
	}
	
	private enum GSResizeArea {

		HOVERING_START, HOVERING_END;
	
	}
}
