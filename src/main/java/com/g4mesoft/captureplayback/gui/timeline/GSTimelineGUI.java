package com.g4mesoft.captureplayback.gui.timeline;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.access.GSIBufferBuilderAccess;
import com.g4mesoft.captureplayback.gui.GSITrackProvider;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.captureplayback.timeline.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSITimelineListener;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
import com.g4mesoft.core.GSCoreOverride;
import com.g4mesoft.gui.GSClipRect;
import com.g4mesoft.gui.GSPanel;

import net.minecraft.client.render.Tessellator;
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
	private final GSCapturePlaybackModule module;
	
	private final GSTimelineViewport timelineViewport;
	private final GSTimelineModelView modelView;
	private final Rectangle tmpRenderRect;
	
	private int rowHeight;
	private int expandedColumnIndex;

	private double currentMouseX;
	private double currentMouseY;
	private GSBlockEventTime hoveredTime;
	private GSTrack hoveredTrack;
	private GSTrackEntry hoveredEntry;
	
	private double clickedMouseX;
	private double clickedMouseY;
	private GSBlockEventTime clickedMouseTime;
	
	private GSDraggingType draggingType;
	private GSBlockEventTime draggedStartTime;
	private GSBlockEventTime draggedEndTime;
	private boolean draggedEntryChanged;

	private GSTrack selectedTrack;
	private GSTrackEntry selectedEntry;
	private boolean toggleSelection;
	
	private boolean editable;
	
	public GSTimelineGUI(GSTimeline timeline, GSITrackProvider trackProvider, GSCapturePlaybackModule module) {
		this.timeline = timeline;
		this.trackProvider = trackProvider;
		this.module = module;
		
		timelineViewport = new GSTimelineViewport();
		modelView = new GSTimelineModelView(timeline, this, timelineViewport);
		tmpRenderRect = new Rectangle();
		
		expandedColumnIndex = -1;
		draggingType = GSDraggingType.NOT_DRAGGING;
	}
	
	@Override
	public void init() {
		super.init();
		
		rowHeight = ROW_LABEL_PADDING * 2 + font.fontHeight;

		timelineViewport.setBounds(client, LABEL_COLUMN_WIDTH, COLUMN_HEADER_HEIGHT,
				width - LABEL_COLUMN_WIDTH, height - COLUMN_HEADER_HEIGHT);
		
		initModelView();
	}
	
	@Override
	protected void onAdded() {
		super.onAdded();
		
		timeline.addTimelineListener(this);
	}

	@Override
	protected void onRemoved() {
		super.onRemoved();

		timeline.removeTimelineListener(this);
	}
	
	private void initModelView() {
		modelView.updateModelView();

		timelineViewport.setMinimumContentSize(modelView.getMinimumWidth(), modelView.getMinimumHeight());
	
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
		
		timelineViewport.render(mouseX, mouseY, partialTicks);
	}
	
	private void renderColumnHeaders(int mouseX, int mouseY) {
		fill(LABEL_COLUMN_WIDTH, 0, width, COLUMN_HEADER_HEIGHT, COLUMN_HEADER_COLOR);
		
		GSIBufferBuilderAccess buffer = (GSIBufferBuilderAccess)Tessellator.getInstance().getBuffer();
		GSClipRect oldClip = buffer.getClip();
		buffer.setClip(LABEL_COLUMN_WIDTH, 0.0f, width, height);
		
		int x0 = timelineViewport.getX();

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
	
		buffer.setClip(oldClip);
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
		for (Map.Entry<UUID, GSTrack> trackEntry : timeline.getTrackEntries()) {
			UUID trackUUID = trackEntry.getKey();
			GSTrack track = trackEntry.getValue();
			
			int y = modelView.getTrackY(trackUUID);
			boolean trackHovered = (track == hoveredTrack);
			renderTrack(track, trackUUID, y, trackHovered);
			renderTrackLabel(track, trackUUID, y, trackHovered);
			y += rowHeight;
		}
	}
	
	private void renderTrack(GSTrack track, UUID trackUUID, int y, boolean trackHovered) {
		GSIBufferBuilderAccess buffer = (GSIBufferBuilderAccess)Tessellator.getInstance().getBuffer();
		GSClipRect oldClip = buffer.getClip();
		buffer.setClip(LABEL_COLUMN_WIDTH, COLUMN_HEADER_HEIGHT, width, height);

		if (trackHovered)
			fill(LABEL_COLUMN_WIDTH, y, width, y + rowHeight, ROW_HOVER_COLOR);

		for (GSTrackEntry entry : track.getEntries())
			renderTrackEntry(trackUUID, entry, getTrackColor(track));
		renderMultiCells(trackUUID, y);
		
		buffer.setClip(oldClip);
	}

	private void renderTrackLabel(GSTrack track, UUID trackUUID, int y, boolean trackHovered) {
		GSIBufferBuilderAccess buffer = (GSIBufferBuilderAccess)Tessellator.getInstance().getBuffer();
		GSClipRect oldClip = buffer.getClip();
		buffer.setClip(0, COLUMN_HEADER_HEIGHT, width, height);

		int y1 = y + rowHeight;
		
		if (trackHovered)
			fill(0, y, LABEL_COLUMN_WIDTH, y1, ROW_HOVER_COLOR);
		
		String name = trimText(track.getInfo().getName(), LABEL_COLUMN_WIDTH);
		int x = (LABEL_COLUMN_WIDTH - font.getStringWidth(name)) / 2;
		drawString(font, name, x, y + (rowHeight - font.fontHeight) / 2, getTrackColor(track));

		fill(0, y1, width, y1 + TRACK_SEPARATOR_HEIGHT, ROW_SPACING_COLOR);
		
		buffer.setClip(oldClip);
	}
	
	private int getTrackColor(GSTrack track) {
		return (0xFF << 24) | track.getInfo().getColor();
	}
	
	private void renderTrackEntry(UUID trackUUID, GSTrackEntry entry, int color) {
		if (selectedEntry == entry || hoveredEntry == entry)
			color = darkenColor(color);

		Rectangle rect = modelView.modelToView(trackUUID, entry, tmpRenderRect);
		
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
	
	private void renderMultiCells(UUID trackUUID, int y) {
		Iterator<GSMultiCellInfo> itr = modelView.getMultiCellIterator(trackUUID);
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
				GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrack.getTrackUUID(), hoveredEntry, mouseX, mouseY);
				if (resizeArea != null) {
					hoveredRect = modelView.modelToView(hoveredTrack.getTrackUUID(), hoveredEntry, tmpRenderRect);
					endTime = (resizeArea == GSResizeArea.HOVERING_END);
				}
			}
			break;
		case DRAGGING:
			break;
		case RESIZING_END:
			endTime = true;
		case RESIZING_START:
			hoveredRect = modelView.modelToView(selectedTrack.getTrackUUID(), selectedEntry, tmpRenderRect);
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
		rect.y = modelView.getTrackEndY() + ADD_TRACK_BUTTON_MARGIN;
		rect.height = font.fontHeight + ADD_TRACK_BUTTON_PADDING * 2;
		
		return rect;
	}
	
	@Override
	public void mouseMovedTranslated(double mouseX, double mouseY) {
		super.mouseMovedTranslated(mouseX, mouseY);
		
		int mx = (int)mouseX;
		int my = (int)mouseY;
		UUID hoveredTrackUUID = modelView.getTrackUUIDFromView(my);
		if (hoveredTrackUUID != null) {
			hoveredTrack = timeline.getTrack(hoveredTrackUUID);
			
			hoveredTime = modelView.viewToModel(mx, my);
			if (hoveredTime != null && hoveredTrack != null) {
				int columnIndex = modelView.getColumnIndex(hoveredTime);
				boolean multiCell = modelView.isMultiCell(hoveredTrackUUID, columnIndex);
				hoveredEntry = hoveredTrack.getEntryAt(hoveredTime, (columnIndex == expandedColumnIndex) || multiCell);
				
				if (hoveredEntry != null) {
					Rectangle rect = modelView.modelToView(hoveredTrackUUID, hoveredEntry);
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
	
	private boolean expandHoveredColumn(double mouseX, double mouseY, boolean allowToggle) {
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
	protected boolean mouseScrolledTranslated(double mouseX, double mouseY, double scroll) {
		timelineViewport.mouseScrolled(mouseX, mouseY, scroll);
		return super.mouseScrolledTranslated(mouseX, mouseY, scroll);
	}
	
	@Override
	public boolean mouseClickedTranslated(double mouseX, double mouseY, int button) {
		if (timelineViewport.mouseClicked(mouseX, mouseY, button))
			return true;
		
		if (button == GLFW.GLFW_MOUSE_BUTTON_1 && mouseY >= 0.0) {
			clickedMouseX = mouseX;
			clickedMouseY = mouseY;
			clickedMouseTime = hoveredTime;
			
			if (mouseY < COLUMN_HEADER_HEIGHT) {
				if (expandHoveredColumn(mouseX, mouseY, true))
					return true;
			} else if (hoveredEntry != null) {
				updateSelectedEntry();
				
				if (editable) {
					GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrack.getTrackUUID(), hoveredEntry, (int)mouseX, (int)mouseY);
	
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
					timeline.addTrack(trackProvider.createNewTrackInfo(timeline));
					return true;
				}
			} else {
				// In case we are adding a new entry, we should return
				// true to ensure we receive the mouse dragging events.
				return true;
			}
		} else if (editable && button == GLFW.GLFW_MOUSE_BUTTON_2 && hoveredEntry != null) {
			GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrack.getTrackUUID(), hoveredEntry, (int)mouseX, (int)mouseY);
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
	
	private GSResizeArea getHoveredResizeArea(UUID trackUUID, GSTrackEntry entry, int mouseX, int mouseY) {
		Rectangle r = modelView.modelToView(trackUUID, entry);
		
		if (r == null || !r.contains(mouseX, mouseY))
			return null;
		
		if (mouseX < r.x + DRAGGING_AREA_SIZE) {
			if (isColumnModifiable(modelView.getColumnIndex(entry.getStartTime())))
				return GSResizeArea.HOVERING_START;
		} else if (mouseX >= r.x + r.width - DRAGGING_AREA_SIZE) {
			if (isColumnModifiable(modelView.getColumnIndex(entry.getEndTime())))
				return GSResizeArea.HOVERING_END;
		}
		
		return null;
	}
	
	private void updateSelectedEntry() {
		if (hoveredEntry != selectedEntry) {
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

		selectedTrack = null;
	}
	
	@Override
	public boolean mouseReleasedTranslated(double mouseX, double mouseY, int button) {
		if (timelineViewport.mouseReleased(mouseX, mouseY, button))
			return true;
		
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
		if (timelineViewport.mouseDragged(mouseX, mouseY, button, dragX, dragY))
			return true;
		
		if (editable && button == GLFW.GLFW_MOUSE_BUTTON_1) {
			if (selectedTrack != null && selectedEntry != null) {
				return dragSelectedEntry(mouseX, mouseY);
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
	
	private boolean dragSelectedEntry(double mouseX, double mouseY) {
		GSBlockEventTime draggedTime = modelView.getDraggedTime((int)mouseX, (int)mouseY);
		
		boolean changed = false;

		if (draggedTime != null) {
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
			case NOT_DRAGGING:
			default:
				break;
			}
		}
		
		if (changed) {
			draggedEntryChanged = true;
			return true;
		}

		return false;
	}

	private boolean moveDraggedEntry(GSBlockEventTime mouseTime) {
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
		
		return moveEntry(selectedTrack, selectedEntry, startTime, endTime);
	}
	
	private boolean isValidDraggedTime(GSBlockEventTime currentTime, GSBlockEventTime mouseTime) {
		int c0 = modelView.getColumnIndex(currentTime);
		int c1 = modelView.getColumnIndex(mouseTime);
		return isColumnModifiable(c1) && isColumnModifiable(c0);
	}
	
	private boolean changeDraggedStart(GSBlockEventTime mouseTime) {
		if (isValidDraggedTime(selectedEntry.getStartTime(), mouseTime)) {
			GSBlockEventTime startTime = offsetDraggedTime(selectedEntry.getStartTime(), mouseTime);
			return moveEntry(selectedTrack, selectedEntry, startTime, selectedEntry.getEndTime());
		}
		
		return false;
	}

	private boolean changeDraggedEnd(GSBlockEventTime mouseTime) {
		if (isValidDraggedTime(selectedEntry.getEndTime(), mouseTime)) {
			GSBlockEventTime endTime = offsetDraggedTime(selectedEntry.getEndTime(), mouseTime);
			return moveEntry(selectedTrack, selectedEntry, selectedEntry.getStartTime(), endTime);
		}
		
		return false;
	}
	
	private GSBlockEventTime offsetDraggedTime(GSBlockEventTime t0, GSBlockEventTime t1) {
		return (expandedColumnIndex != -1) ? t1 : new GSBlockEventTime(t1.getGametick(), t0.getMicrotick());
	}
	
	private boolean moveEntry(GSTrack track, GSTrackEntry entry, GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (!canMoveEntry(startTime, endTime, selectedTrack, selectedEntry))
			return false;
		
		selectedEntry.setTimespan(startTime, endTime);
		
		return true;
	}
	
	private boolean canMoveEntry(GSBlockEventTime startTime, GSBlockEventTime endTime, GSTrack track, GSTrackEntry entry) {
		// The model has not changed. No reason to move it.
		if (entry.getStartTime().isEqual(startTime) && entry.getEndTime().isEqual(endTime))
			return false;
		
		// Ensure we do not move entries out of the timeline.
		if (startTime.getGametick() < modelView.getColumnGametick(0))
			return false;
		if (startTime.getMicrotick() < 0)
			return false;
		
		// Ensure the entry is a model-valid format.
		if (endTime.isBefore(startTime))
			return false;
		
		// Lastly ensure we do not overlap other entries.
		return !track.isOverlappingEntries(startTime, endTime, entry);
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
				int c0 = modelView.getColumnIndex(t0);
				int c1 = modelView.getColumnIndex(t1);
				if (c0 != c1 || !isColumnModifiable(c0))
					return;
			} else {
				t0 = new GSBlockEventTime(t0.getGametick(), 0);
				t1 = new GSBlockEventTime(t1.getGametick(), 0);
			}
			
			GSTrackEntry entry = hoveredTrack.tryAddEntry(t0, t1);
			if (entry != null) {
				hoveredEntry = entry;
				updateSelectedEntry();
	
				this.draggingType = draggingType;
				draggedStartTime = hoveredEntry.getStartTime();
				draggedEndTime = hoveredEntry.getEndTime();
				draggedEntryChanged = true;
			}
		}
	}
	
	@Override
	@GSCoreOverride
	public boolean keyPressed(int key, int scancode, int mods) {
		if (key == module.getCollapseTabKey().getGLFWKeyCode() && expandedColumnIndex != -1) {
			expandedColumnIndex = -1;
			return true;
		} else if (key == module.getExpandHoveredTabKey().getGLFWKeyCode()) {
			if (expandHoveredColumn(currentMouseX, currentMouseY, false))
				return true;
		}
		
		return super.keyPressed(key, scancode, mods);
	}
	
	private boolean isColumnModifiable(int columnIndex) {
		return (expandedColumnIndex == -1 || expandedColumnIndex == columnIndex);
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
	public void trackAdded(GSTrack track) {
		initModelView();
	}

	@Override
	public void trackRemoved(GSTrack track) {
		UUID trackUUID = track.getTrackUUID();
		if (hoveredTrack != null && trackUUID.equals(hoveredTrack.getTrackUUID())) {
			hoveredTrack = null;
			hoveredEntry = null;
		}
		
		if (selectedTrack != null && trackUUID.equals(selectedTrack.getTrackUUID()))
			unselectEntries();
		
		initModelView();
	}

	@Override
	public void entryAdded(GSTrackEntry entry) {
		initModelView();
	}

	@Override
	public void entryRemoved(GSTrackEntry entry) {
		UUID entryUUID = entry.getEntryUUID();
		if (hoveredEntry != null && entryUUID.equals(hoveredEntry.getEntryUUID()))
			hoveredEntry = null;
		if (selectedEntry != null && entryUUID.equals(selectedEntry.getEntryUUID()))
			unselectEntries();
		
		initModelView();
	}

	@Override
	public void entryTimeChanged(GSTrackEntry entry, GSBlockEventTime oldStart, GSBlockEventTime oldEnd) {
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
