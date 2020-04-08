package com.g4mesoft.planner.gui.timeline;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.core.GSCoreOverride;
import com.g4mesoft.gui.GSPanel;
import com.g4mesoft.planner.gui.GSITrackProvider;
import com.g4mesoft.planner.module.GSPlannerModule;
import com.g4mesoft.planner.timeline.GSBlockEventTime;
import com.g4mesoft.planner.timeline.GSETrackEntryType;
import com.g4mesoft.planner.timeline.GSITimelineListener;
import com.g4mesoft.planner.timeline.GSTimeline;
import com.g4mesoft.planner.timeline.GSTrack;
import com.g4mesoft.planner.timeline.GSTrackEntry;

import net.minecraft.util.math.BlockPos;

public class GSTimelineGUI extends GSPanel implements GSITimelineListener {

	private static final int LABEL_COLUMN_COLOR = 0x80000000;
	private static final int COLUMN_HEADER_COLOR = 0x60000000;
	private static final int COLUMN_COLOR = 0x60000000;
	private static final int DARK_COLUMN_COLOR = 0x60202020;
	private static final int ROW_HOVER_COLOR = 0x30FFFFFF;

	private static final int ROW_SPACING_COLOR = 0xFF444444;
	private static final int COLUMN_LINE_COLOR = 0x30B2B2B2;

	private static final int TEXT_COLOR = 0xFFFFFFFF;
	
	private static final int LABEL_COLUMN_WIDTH = 100;
	private static final int COLUMN_HEADER_HEIGHT = 30;
	private static final int ROW_LABEL_PADDING = 2;

	private static final int TRACK_SEPARATOR_HEIGHT = 1;
	private static final int ENTRY_BORDER_THICKNESS = 2;
	private static final int DOTTED_LINE_LENGTH = 4;
	private static final int DOTTED_LINE_SPACING = 3;

	
	/* Add track button constants */
	private static final String ADD_TRACK_BUTTON_TEXT = "+ Add Track";
	private static final int ADD_TRACK_BUTTON_MARGIN = 2;
	private static final int ADD_TRACK_BUTTON_PADDING = 3;
	private static final int ADD_TRACK_BUTTON_COLOR = 0xFF222222;

	private static final double MINIMUM_DRAG_DISTANCE = 10.0;

	/* Dragging area constants */
	private static final int DRAGGING_AREA_SIZE = 6;
	private static final int DRAGGING_PADDING = 2;
	private static final int DRAGGING_AREA_COLOR = 0x40FFFFFF;

	private final GSTimeline timeline;
	private final GSITrackProvider trackProvider;
	private final GSPlannerModule plannerModule;
	
	private final GSTimelineModelView modelView;
	private final Rectangle tmpRenderRect;
	
	private int rowHeight;
	private int expandedColumnIndex;
	
	private GSBlockEventTime hoveredTime;
	private int hoveredTrackIndex;
	private GSTrack hoveredTrack;
	private GSTrackEntry hoveredEntry;
	
	private int selectedTrackIndex;
	private GSTrack selectedTrack;
	private GSTrackEntry selectedEntry;
	private boolean toggleSelection;

	private double clickedMouseX;
	private double clickedMouseY;
	private GSBlockEventTime clickedMouseTime;
	
	private GSDraggingType draggingType;
	private GSBlockEventTime draggedStartTime;
	private GSBlockEventTime draggedEndTime;
	private boolean draggedEntryChanged;
	
	private double currentMouseX;
	private double currentMouseY;
	
	private boolean editable;
	
	public GSTimelineGUI(GSTimeline timeline, GSITrackProvider trackProvider, GSPlannerModule plannerModule) {
		this.timeline = timeline;
		this.trackProvider = trackProvider;
		this.plannerModule = plannerModule;
		
		modelView = new GSTimelineModelView(timeline, this);
		tmpRenderRect = new Rectangle();
		
		expandedColumnIndex = -1;
		draggingType = GSDraggingType.NOT_DRAGGING;
		
		timeline.addTimelineListener(this);
	}
	
	@Override
	public void init() {
		super.init();
		
		rowHeight = ROW_LABEL_PADDING * 2 + font.fontHeight;

		initModelView();
	}
	
	private void initModelView() {
		modelView.initModelView(LABEL_COLUMN_WIDTH, COLUMN_HEADER_HEIGHT,
				width - LABEL_COLUMN_WIDTH, height - COLUMN_HEADER_HEIGHT);
	
		// Ensure that we are not expanding an invalid column.
		if (expandedColumnIndex >= modelView.getNumColumns())
			expandedColumnIndex = -1;
	}
	
	@Override
	public void renderTranslated(int mouseX, int mouseY, float partialTicks) {
		super.renderTranslated(mouseX, mouseY, partialTicks);
	
		fill(0, 0, LABEL_COLUMN_WIDTH, height, LABEL_COLUMN_COLOR);
		fill(LABEL_COLUMN_WIDTH, 0, LABEL_COLUMN_WIDTH + 1, height, COLUMN_LINE_COLOR);
		
		renderColumnHeaders(mouseX, mouseY);
		renderTracks(mouseX, mouseY);
		renderHoveredInfo(mouseX, mouseY);
	
		if (editable) {
			renderHoveredEdge(mouseX, mouseY);
			renderAddTrackButton(mouseX, mouseY);
		}
	}
	
	private void renderColumnHeaders(int mouseX, int mouseY) {
		fill(LABEL_COLUMN_WIDTH, 0, width, COLUMN_HEADER_HEIGHT, COLUMN_HEADER_COLOR);
		
		int x0 = LABEL_COLUMN_WIDTH;

		int numColumns = modelView.getNumColumns();
		for (int columnIndex = 0; columnIndex < numColumns; columnIndex++) {
			int x1 = x0 + modelView.getColumnWidth(columnIndex);

			int y;
			int color = TEXT_COLOR;
			if (expandedColumnIndex != columnIndex) {
				if (expandedColumnIndex != -1)
					color = darkenColor(TEXT_COLOR);
				y = (COLUMN_HEADER_HEIGHT - font.fontHeight) / 2;
			} else {
				y = (COLUMN_HEADER_HEIGHT / 2 - font.fontHeight) / 2;
			}

			fill(x0, 0, x1, height, (columnIndex & 0x1) != 0 ? COLUMN_COLOR : DARK_COLUMN_COLOR);
			drawCenteredString(font, getColumnTitle(columnIndex), (x0 + x1) / 2, y, color);
			
			if (mouseX >= x0 && mouseX < x1) {
				fill(x0, 0, x0 + 1, height, COLUMN_LINE_COLOR);
				fill(x1 - 1, 0, x1, height, COLUMN_LINE_COLOR);
			}
			
			x0 = x1;
		}

		if (expandedColumnIndex != -1)
			renderExpandedColumnHeader(expandedColumnIndex);
		
		fill(0, COLUMN_HEADER_HEIGHT - 1, Math.min(width, x0), COLUMN_HEADER_HEIGHT, ROW_SPACING_COLOR);
	}
	
	private void renderExpandedColumnHeader(int expandedColumnIndex) {
		int mtGridColor = brightenColor(COLUMN_LINE_COLOR);
		
		int duration = modelView.getColumnDuration(expandedColumnIndex);
		int y = COLUMN_HEADER_HEIGHT * 3 / 4 - font.fontHeight / 2;
		for (int mt = 0; mt < duration; mt++) {
			String title = (mt == duration - 1) ? "-" : Integer.toString(mt);
			int x = modelView.getMicrotickColumnX(expandedColumnIndex, mt);
			int w = modelView.getMicrotickColumnWidth(expandedColumnIndex, mt);
			drawCenteredString(font, title, x + w / 2, y, TEXT_COLOR);
		
			if (mt != 0)
				drawDottedLine(x, (COLUMN_HEADER_HEIGHT + DOTTED_LINE_LENGTH) / 2, height, mtGridColor);
		}
		
		int ys = COLUMN_HEADER_HEIGHT / 2;
		int x0 = modelView.getColumnX(expandedColumnIndex);
		int x1 = x0 + modelView.getColumnWidth(expandedColumnIndex);
		fill(x0, ys - 1, x1, ys, mtGridColor);
	}
	
	private String getColumnTitle(int columnIndex) {
		return Long.toString(modelView.getColumnGametick(columnIndex)) + "gt";
	}
	
	private void drawDottedLine(int x, int y0, int y1, int color) {
		int n = (y1 - y0) / (DOTTED_LINE_LENGTH + DOTTED_LINE_SPACING);
		
		for (int yl = 0; yl <= n; yl++) {
			int yl0 = y0 + yl * (DOTTED_LINE_LENGTH + DOTTED_LINE_SPACING);
			int yl1 = Math.min(yl0 + DOTTED_LINE_LENGTH, y1);
			fill(x, yl0, x + 1, yl1, color);
		}
	}
	
	private void renderTracks(int mouseX, int mouseY) {
		List<GSTrack> tracks = timeline.getTracks();
		for (int trackIndex = 0; trackIndex < tracks.size(); trackIndex++) {
			int y = modelView.getTrackY(trackIndex);
			
			renderTrack(tracks.get(trackIndex), trackIndex, y, (trackIndex == hoveredTrackIndex));
			y += rowHeight;
			
			fill(0, y, width, y + TRACK_SEPARATOR_HEIGHT, ROW_SPACING_COLOR);
		}
	}
	
	private void renderTrack(GSTrack track, int trackIndex, int y, boolean trackHovered) {
		int color = (0xFF << 24) | track.getInfo().getColor();
		
		if (trackHovered)
			fill(0, y, width, y + rowHeight, ROW_HOVER_COLOR);
		
		for (GSTrackEntry entry : track.getEntries())
			renderTrackEntry(trackIndex, entry, color);
		
		renderMultiCells(trackIndex, y);
		
		String name = trimText(track.getInfo().getName(), LABEL_COLUMN_WIDTH);
		int x = (LABEL_COLUMN_WIDTH - font.getStringWidth(name)) / 2;
		drawString(font, name, x, y + (rowHeight - font.fontHeight) / 2, color);
	}
	
	private void renderTrackEntry(int trackIndex, GSTrackEntry entry, int color) {
		if (selectedEntry == entry || hoveredEntry == entry)
			color = darkenColor(color);

		Rectangle rect = modelView.modelToView(trackIndex, entry, tmpRenderRect);
		
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
		Iterator<GSMultiCellInfo> itr = modelView.getMultiCellIterator(trackIndex);
		while (itr.hasNext()) {
			GSMultiCellInfo multiCellInfo = itr.next();
			
			int columnIndex = multiCellInfo.getColumnIndex();
			if (columnIndex != expandedColumnIndex) {
				int count = multiCellInfo.getCount();

				String infoText = (count > 9) ? "+" : Integer.toString(count);
				int xc = modelView.getColumnX(columnIndex) + modelView.getColumnWidth(columnIndex) / 2;
				drawCenteredString(font, infoText, xc, y + (rowHeight - font.fontHeight) / 2, TEXT_COLOR);
			}
		}
	}
	
	private void renderHoveredInfo(int mouseX, int mouseY) {
		String text = null;
		if (hoveredEntry != null) {
			text = String.format(Locale.ENGLISH, "Duration: %dgt", hoveredEntry.getGametickDuration());
		} else if (hoveredTrack != null) {
			BlockPos pos = hoveredTrack.getInfo().getPos();
			text = String.format(Locale.ENGLISH, "Pos: (%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
		}

		if (text != null)
			drawCenteredString(font, text, LABEL_COLUMN_WIDTH / 2, (COLUMN_HEADER_HEIGHT - font.fontHeight) / 2, TEXT_COLOR);
	}
	
	private void renderHoveredEdge(int mouseX, int mouseY) {
		Rectangle hoveredRect = null;
		boolean endTime = false;
		
		switch (draggingType) {
		case NOT_DRAGGING:
			if (hoveredEntry != null) {
				GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrackIndex, hoveredEntry, mouseX, mouseY);
				if (resizeArea != null) {
					hoveredRect = modelView.modelToView(hoveredTrackIndex, hoveredEntry, tmpRenderRect);
					endTime = (resizeArea == GSResizeArea.HOVERING_END);
				}
			}
			break;
		case DRAGGING:
			break;
		case RESIZING_END:
			endTime = true;
		case RESIZING_START:
			hoveredRect = modelView.modelToView(selectedTrackIndex, selectedEntry, tmpRenderRect);
			break;
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
		rect.y = modelView.getTrackY(timeline.getTracks().size()) + ADD_TRACK_BUTTON_MARGIN;
		rect.height = font.fontHeight + ADD_TRACK_BUTTON_PADDING * 2;
		
		return rect;
	}
	
	@Override
	public void mouseMovedTranslated(double mouseX, double mouseY) {
		super.mouseMovedTranslated(mouseX, mouseY);
		
		int mx = (int)mouseX;
		int my = (int)mouseY;
		hoveredTrackIndex = modelView.getTrackIndexFromView(my);
		if (hoveredTrackIndex != -1) {
			hoveredTrack = timeline.getTracks().get(hoveredTrackIndex);
			
			hoveredTime = modelView.viewToModel(mx, my);
			if (hoveredTime != null && hoveredTrack != null) {
				int columnIndex = modelView.getColumnIndex(hoveredTime);
				boolean multiCell = modelView.isMultiCell(hoveredTrackIndex, columnIndex);
				hoveredEntry = hoveredTrack.getEntryAt(hoveredTime, (columnIndex == expandedColumnIndex) || multiCell);
				
				if (hoveredEntry != null) {
					Rectangle rect = modelView.modelToView(hoveredTrackIndex, hoveredEntry);
					if (rect == null || !rect.contains(mouseX, mouseY))
						hoveredEntry = null;
				}
			}
		} else {
			hoveredTime = null;
			hoveredTrack = null;
			hoveredEntry = null;
		}
		
		currentMouseX = mouseX;
		currentMouseY = mouseY;
	}
	
	private boolean expandHoveredTab(double mouseX, double mouseY, boolean allowToggle) {
		GSBlockEventTime time = modelView.viewToModel((int)mouseX, (int)mouseY);
		if (time != null) {
			int columnIndex = modelView.getColumnIndex(time);
			if (columnIndex == expandedColumnIndex && allowToggle) {
				expandedColumnIndex = -1;
			} else {
				expandedColumnIndex = columnIndex;
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
			clickedMouseTime = hoveredTime;
			
			if (mouseY < COLUMN_HEADER_HEIGHT) {
				if (expandHoveredTab(mouseX, mouseY, true))
					return true;
			} else if (hoveredEntry != null) {
				updateSelectedEntry();
				
				if (editable) {
					GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrackIndex, hoveredEntry, (int)mouseX, (int)mouseY);
	
					GSDraggingType type;
					if (resizeArea != null) {
						type = (resizeArea == GSResizeArea.HOVERING_START) ? 
								GSDraggingType.RESIZING_START : GSDraggingType.RESIZING_END;
					} else {
						type = GSDraggingType.DRAGGING;
					}
					
					long dgt = hoveredEntry.getGametickDuration();
					if (expandedColumnIndex == -1 || type != GSDraggingType.DRAGGING || dgt == 0L) {
						draggingType = type;
						draggedStartTime = hoveredEntry.getStartTime();
						draggedEndTime = hoveredEntry.getEndTime();
						return true;
					}
				}
			} else if (mouseX < LABEL_COLUMN_WIDTH) {
				if (editable && getAddTrackButtonBounds().contains(mouseX, mouseY)) {
					timeline.addTrack(trackProvider.createNewTrackInfo());
					return true;
				}
			} else {
				// In case we are adding a new entry, we should return
				// true to ensure we receive the mouse dragging events.
				return true;
			}
		} else if (editable && button == GLFW.GLFW_MOUSE_BUTTON_2 && hoveredEntry != null) {
			GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrackIndex, hoveredEntry, (int)mouseX, (int)mouseY);
			if (resizeArea != null) {
				GSETrackEntryType newType = (resizeArea == GSResizeArea.HOVERING_START) ? 
						GSETrackEntryType.EVENT_END : GSETrackEntryType.EVENT_START;
				hoveredEntry.setType((newType == hoveredEntry.getType()) ? GSETrackEntryType.EVENT_BOTH : newType);
			} else if (draggingType == GSDraggingType.NOT_DRAGGING) {
				hoveredTrack.removeEntry(hoveredEntry);
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
		if (selectedEntry != null) {
			selectedEntry = null;
			draggingType = GSDraggingType.NOT_DRAGGING;
		}

		selectedTrackIndex = -1;
		selectedTrack = null;
	}
	
	@Override
	public boolean mouseReleasedTranslated(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1 && mouseY > COLUMN_HEADER_HEIGHT) {
			if (draggedEntryChanged || toggleSelection)
				unselectEntries();
			toggleSelection = (selectedEntry != null);
		}
		
		draggingType = GSDraggingType.NOT_DRAGGING;
		draggedEntryChanged = false;
		
		return super.mouseReleasedTranslated(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDraggedTranslated(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (editable && button == GLFW.GLFW_MOUSE_BUTTON_1) {
			if (selectedTrack != null && selectedEntry != null) {
				if (draggingType != GSDraggingType.NOT_DRAGGING) {
					dragSelectedEntry(mouseX, mouseY);
					return true;
				}
			} else if (hoveredTrack != null && hoveredEntry == null) {
				double dx = mouseX - clickedMouseX;
				double dy = mouseY - clickedMouseY;

				if (dx * dx + dy * dy > MINIMUM_DRAG_DISTANCE) {
					GSDraggingType draggingType = (dx > 0) ? GSDraggingType.RESIZING_END :
					                                         GSDraggingType.RESIZING_START;
					addTrackEntry(mouseX, mouseY, draggingType);
					return true;
				}
			}
		}
		
		return super.mouseDraggedTranslated(mouseX, mouseY, button, dragX, dragY);
	}
	
	private void dragSelectedEntry(double mouseX, double mouseY) {
		GSBlockEventTime draggedTime = modelView.getDraggedTime((int)mouseX, (int)mouseY);
		
		boolean changed;
		switch (draggingType) {
		case DRAGGING:
			changed = moveDraggedEntry(draggedTime);
			break;
		case RESIZING_START:
			changed = changeDraggedStart(draggedTime);
			break;
		case RESIZING_END:
			changed = changeDraggedEnd(draggedTime);
			break;
		
		default:
			changed = false;
			break;
		}
			
		if (changed)
			draggedEntryChanged = true;
	}

	private void addTrackEntry(double mouseX, double mouseY, GSDraggingType draggingType) {
		GSBlockEventTime t0 = modelView.getDraggedTime((int)clickedMouseX, (int)clickedMouseY);
		GSBlockEventTime t1 = modelView.getDraggedTime((int)mouseX, (int)mouseY);
	
		if (t0 != null && t1 != null) {
			if (t0.isAfter(t1)) {
				GSBlockEventTime tmp = t0;
				t0 = t1;
				t1 = tmp;
			}
			
			if (expandedColumnIndex != -1) {
				if (t0.getGametick() != t1.getGametick() || t0.getGametick() != expandedColumnIndex)
					t0 = t1 = null;
			} else {
				t0 = new GSBlockEventTime(t0.getGametick(), 0);
				t1 = new GSBlockEventTime(t1.getGametick(), 0);
			}
			
			if (t0 != null && t1 != null) {
				GSTrackEntry entry = hoveredTrack.tryAddEntry(t0, t1);
				if (entry != null) {
					hoveredEntry = entry;
					updateSelectedEntry();

					this.draggingType = draggingType;
					draggedStartTime = hoveredEntry.getStartTime();
					draggedEndTime = hoveredEntry.getEndTime();
				}
			}
		}
	}
	
	private boolean moveDraggedEntry(GSBlockEventTime mouseTime) {
		if (mouseTime == null)
			return false;
		
		GSBlockEventTime startTime = draggedStartTime;
		GSBlockEventTime endTime = draggedEndTime;
		
		if (expandedColumnIndex != -1) {
			int deltaMicroticks = mouseTime.getMicrotick() - clickedMouseTime.getMicrotick();
			startTime = startTime.offsetCopy(0, deltaMicroticks);
			endTime = endTime.offsetCopy(0, deltaMicroticks);
		} else {
			long deltaGameticks = mouseTime.getGametick() - clickedMouseTime.getGametick();
			startTime = startTime.offsetCopy(deltaGameticks, 0);
			endTime = endTime.offsetCopy(deltaGameticks, 0);
		}
		
		if (startTime.getGametick() >= 0 && startTime.getMicrotick() >= 0) {
			if (selectedEntry.getStartTime().isEqual(startTime))
				return false;
			
			if (!selectedTrack.isOverlappingEntries(startTime, endTime, selectedEntry)) {
				selectedEntry.setTimespan(startTime, endTime);
				return true;
			}
		}
		
		return false;
	}
	
	private boolean isValidDraggedTime(GSBlockEventTime mouseTime, GSBlockEventTime currentTime) {
		if (mouseTime == null)
			return false;
		
		if (expandedColumnIndex != -1)
			return mouseTime.getGametick() == currentTime.getGametick() && !mouseTime.isEqual(currentTime);
		return mouseTime.getGametick() != currentTime.getGametick();
	}
	
	private boolean changeDraggedStart(GSBlockEventTime mouseTime) {
		if (isValidDraggedTime(mouseTime, selectedEntry.getStartTime())) {
			GSBlockEventTime newTime = offsetDraggedTime(selectedEntry.getStartTime(), mouseTime);
			GSBlockEventTime endTime = selectedEntry.getEndTime();
			if (!newTime.isAfter(endTime) && !selectedTrack.isOverlappingEntries(newTime, endTime, selectedEntry))
				selectedEntry.setStartTime(newTime);
			return true;
		}
		
		return false;
	}

	private boolean changeDraggedEnd(GSBlockEventTime mouseTime) {
		if (isValidDraggedTime(mouseTime, selectedEntry.getEndTime())) {
			GSBlockEventTime newTime = offsetDraggedTime(selectedEntry.getEndTime(), mouseTime);
			GSBlockEventTime startTime = selectedEntry.getStartTime();
			if (!newTime.isBefore(startTime) && !selectedTrack.isOverlappingEntries(startTime, newTime, selectedEntry))
				selectedEntry.setEndTime(newTime);
			return true;
		}
		
		return false;
	}

	private GSBlockEventTime offsetDraggedTime(GSBlockEventTime t0, GSBlockEventTime t1) {
		return (expandedColumnIndex != -1) ? t1 : new GSBlockEventTime(t1.getGametick(), t0.getMicrotick());
	}
	
	private GSResizeArea getHoveredResizeArea(int trackIndex, GSTrackEntry entry, int mouseX, int mouseY) {
		Rectangle r = modelView.modelToView(trackIndex, entry);
		if (r == null)
			return null;
		
		GSResizeArea resizeArea = null;
		if (mouseX >= r.x && mouseX < r.x + DRAGGING_AREA_SIZE) {
			if (expandedColumnIndex == -1 || expandedColumnIndex == entry.getStartTime().getGametick())
				resizeArea = GSResizeArea.HOVERING_START;
		} else if (mouseX < r.x + r.width && mouseX >= r.x + r.width - DRAGGING_AREA_SIZE) {
			if (expandedColumnIndex == -1 || expandedColumnIndex == entry.getEndTime().getGametick())
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

	public int getRowHeight() {
		return rowHeight;
	}
	
	public int getExpandedColumnIndex() {
		return expandedColumnIndex;
	}

	public void setExpandedColumnIndex(int expandedColumnIndex) {
		this.expandedColumnIndex = expandedColumnIndex;
	}
	
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
		
		if (!editable)
			draggingType = GSDraggingType.NOT_DRAGGING;
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
		initModelView();
	}

	@Override
	public void entryRemoved(GSTrack track, GSTrackEntry entry) {
		if (entry == hoveredEntry)
			hoveredEntry = null;
		if (entry == selectedEntry)
			unselectEntries();
		
		initModelView();
	}

	@Override
	public void entryPropertyChanged(GSTrack track, GSTrackEntry entry, int property) {
		if (property == GSTrackEntry.PROPERTY_TIMESPAN)
			initModelView();
	}
	
	private enum GSResizeArea {

		HOVERING_START, HOVERING_END;
	
	}
	
	private enum GSDraggingType {
		
		NOT_DRAGGING,
		DRAGGING,
		RESIZING_START,
		RESIZING_END;
		
	}
}
