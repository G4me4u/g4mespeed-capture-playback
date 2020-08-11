package com.g4mesoft.captureplayback.gui.timeline;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSITimelineListener;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
import com.g4mesoft.gui.GSPanel;
import com.g4mesoft.gui.event.GSIMouseListener;
import com.g4mesoft.gui.event.GSMouseEvent;
import com.g4mesoft.gui.renderer.GSIRenderer2D;

public class GSTimelineContentGUI extends GSPanel implements GSITimelineListener, GSITimelineModelViewListener,
                                                             GSIMouseListener {

	private static final int TEXT_COLOR = 0xFFFFFFFF;
	
	private static final int ENTRY_BORDER_THICKNESS = 2;

	private static final int MINIMUM_DRAG_DISTANCE = 5;

	private static final int DRAGGING_AREA_SIZE = 6;
	private static final int DRAGGING_PADDING = 2;
	private static final int DRAGGING_AREA_COLOR = 0x40FFFFFF;
	
	private final GSTimeline timeline;
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSTimelineModelView modelView;
	
	private final Rectangle tmpRenderRect;
	
	private int currentMouseX;
	private int currentMouseY;
	private UUID hoveredTrackUUID;
	private GSTrackEntry hoveredEntry;
	
	private int clickedMouseX;
	private int clickedMouseY;
	private GSBlockEventTime clickedMouseTime;
	
	private GSTrackEntry draggingEntry;
	private GSEDraggingType draggingType;
	private GSBlockEventTime draggingStartTime;
	private GSBlockEventTime draggingEndTime;
	
	private boolean editable;
	
	public GSTimelineContentGUI(GSTimeline timeline, GSExpandedColumnModel expandedColumnModel, GSTimelineModelView modelView) {
		this.timeline = timeline;
		this.expandedColumnModel = expandedColumnModel;
		this.modelView = modelView;
		
		tmpRenderRect = new Rectangle();
		
		draggingType = GSEDraggingType.NOT_DRAGGING;
		
		addMouseEventListener(this);
	}
	
	@Override
	public void onShown() {
		super.onShown();
		
		timeline.addTimelineListener(this);
		modelView.addModelViewListener(this);
	}

	@Override
	public void onHidden() {
		super.onHidden();
		
		timeline.removeTimelineListener(this);
		modelView.removeModelViewListener(this);
		
		stopDragging();
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);

		renderer.pushClip(0, 0, width, height);
		
		renderColumns(renderer);
		renderTracks(renderer);
		
		if (editable)
			renderHoveredEdge(renderer);

		renderer.popClip();
	}
	
	protected void renderColumns(GSIRenderer2D renderer) {
		int columnStart = Math.max(0, modelView.getColumnIndexFromView(0));
		int columnEnd = modelView.getColumnIndexFromView(width - 1);

		int x = modelView.getColumnX(columnStart);
		for (int columnIndex = columnStart; columnIndex <= columnEnd; columnIndex++) {
			int cw = modelView.getColumnWidth(columnIndex);
			renderColumn(renderer, columnIndex, x, cw);
			x += cw;
		}
	}
	
	protected void renderColumn(GSIRenderer2D renderer, int columnIndex, int cx, int cw) {
		renderer.fillRect(cx, 0, cw, height, getColumnColor(columnIndex));
	
		if (renderer.getMouseX() >= cx && renderer.getMouseX() < cx + cw) {
			renderer.drawVLine(cx, 0, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
			renderer.drawVLine(cx + cw - 1, 0, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
		}
		
		if (expandedColumnModel.isColumnExpanded(columnIndex)) {
			int duration = modelView.getColumnDuration(columnIndex);

			for (int mt = 1; mt < duration; mt++) {
				int x = modelView.getMicrotickColumnX(columnIndex, mt);
				int y = GSTimelineColumnHeaderGUI.DOTTED_LINE_SPACING / 2;
			
				renderer.drawDottedVLine(x, y, height, GSTimelineColumnHeaderGUI.DOTTED_LINE_LENGTH, 
						GSTimelineColumnHeaderGUI.DOTTED_LINE_SPACING, GSTimelineColumnHeaderGUI.MT_COLUMN_LINE_COLOR);
			}
		}
	}
	
	protected int getColumnColor(int columnIndex) {
		return ((columnIndex & 0x1) != 0) ? GSTimelineColumnHeaderGUI.DARK_COLUMN_COLOR : 
		                                    GSTimelineColumnHeaderGUI.COLUMN_COLOR;
	}
	
	protected void renderTracks(GSIRenderer2D renderer) {
		for (GSTrack track : timeline.getTracks()) {
			int ty = modelView.getTrackY(track.getTrackUUID());
			
			if (ty + modelView.getTrackHeight() > 0 && ty < height)
				renderTrack(renderer, track, ty);
		}
	}
	
	protected void renderTrack(GSIRenderer2D renderer, GSTrack track, int ty) {
		int th = modelView.getTrackHeight();
		
		if (track.getTrackUUID().equals(hoveredTrackUUID))
			renderer.fillRect(0, ty, width, th, GSTimelineTrackHeaderGUI.TRACK_HOVER_COLOR);

		for (GSTrackEntry entry : track.getEntries())
			renderTrackEntry(renderer, entry, getTrackColor(track));
		renderMultiCells(renderer, track.getTrackUUID(), ty);
		
		renderer.fillRect(0, ty + th, width, modelView.getTrackSpacing(), 
				GSTimelineTrackHeaderGUI.TRACK_SPACING_COLOR);
	}
	
	protected int getTrackColor(GSTrack track) {
		return (0xFF << 24) | track.getInfo().getColor();
	}
	
	protected void renderTrackEntry(GSIRenderer2D renderer, GSTrackEntry entry, int color) {
		if (draggingEntry == entry || hoveredEntry == entry)
			color = renderer.darkenColor(color);

		Rectangle rect = modelView.modelToView(entry, tmpRenderRect);
		
		if (rect != null) {
			renderer.fillRect(rect.x, rect.y, rect.width, rect.height, renderer.darkenColor(color));
			
			if (entry.getType() != GSETrackEntryType.EVENT_START)
				rect.width -= ENTRY_BORDER_THICKNESS;

			if (entry.getType() != GSETrackEntryType.EVENT_END) {
				rect.x += ENTRY_BORDER_THICKNESS;
				rect.width -= ENTRY_BORDER_THICKNESS;
			}
			
			rect.y += ENTRY_BORDER_THICKNESS;
			rect.height -= 2 * ENTRY_BORDER_THICKNESS;
			
			renderer.fillRect(rect.x, rect.y, rect.width, rect.height, color);
		}
	}
	
	protected void renderMultiCells(GSIRenderer2D renderer, UUID trackUUID, int y) {
		Iterator<GSMultiCellInfo> itr = modelView.getMultiCellIterator(trackUUID);
		while (itr.hasNext()) {
			GSMultiCellInfo multiCellInfo = itr.next();
			if (!expandedColumnModel.isColumnExpanded(multiCellInfo.getColumnIndex()))
				renderMultiCell(renderer, y, multiCellInfo);
		}
	}
	
	protected void renderMultiCell(GSIRenderer2D renderer, int y, GSMultiCellInfo multiCellInfo) {
		String infoText = formatMultiCellInfo(multiCellInfo);
		
		int columnIndex = multiCellInfo.getColumnIndex();
		int xc = modelView.getColumnX(columnIndex) + modelView.getColumnWidth(columnIndex) / 2;
		int ty = y + (modelView.getTrackHeight() - renderer.getFontHeight() + 1) / 2;
		renderer.drawCenteredString(infoText, xc, ty, TEXT_COLOR);
	}

	protected String formatMultiCellInfo(GSMultiCellInfo multiCellInfo) {
		if (multiCellInfo.getCount() > 9)
			return "+";
		return Integer.toString(multiCellInfo.getCount());
	}
	
	protected void renderHoveredEdge(GSIRenderer2D renderer) {
		switch (draggingType) {
		case NOT_DRAGGING:
			if (hoveredEntry != null) {
				int mouseX = renderer.getMouseX();
				int mouseY = renderer.getMouseY();
				
				GSEResizeArea resizeArea = getHoveredResizeArea(hoveredEntry, mouseX, mouseY);
				if (resizeArea != null)
					renderResizeArea(renderer, hoveredEntry, resizeArea);
			}
			break;
		case RESIZING_START:
			renderResizeArea(renderer, draggingEntry, GSEResizeArea.HOVERING_START);
			break;
		case RESIZING_END:
			renderResizeArea(renderer, draggingEntry, GSEResizeArea.HOVERING_END);
			break;
		case DRAGGING:
		default:
			break;
		}
	}
	
	private void renderResizeArea(GSIRenderer2D renderer, GSTrackEntry entry, GSEResizeArea resizeArea) {
		Rectangle rect = modelView.modelToView(entry, tmpRenderRect);
		
		if (rect != null) {
			if (resizeArea == GSEResizeArea.HOVERING_END) {
				rect.x += rect.width - DRAGGING_AREA_SIZE;
			} else {
				rect.x -= DRAGGING_PADDING;
			}
			
			rect.y -= DRAGGING_PADDING;

			rect.width = DRAGGING_AREA_SIZE + DRAGGING_PADDING;
			rect.height += 2 * DRAGGING_PADDING;
			
			renderer.fillRect(rect.x, rect.y, rect.width, rect.height, DRAGGING_AREA_COLOR);
		}
	}
	
	@Override
	public void mouseMoved(GSMouseEvent event) {
		currentMouseX = event.getX();
		currentMouseY = event.getY();
		
		updateHoveredEntry();
	}
	
	private void updateHoveredEntry() {
		GSTrackEntry hoveredEntry = null;
		
		if (hoveredTrackUUID != null) {
			GSTrack hoveredTrack = timeline.getTrack(hoveredTrackUUID);
			GSBlockEventTime hoveredTime = modelView.viewToModel(currentMouseX, currentMouseY);
			
			if (hoveredTrack != null && hoveredTime != null) {
				int columnIndex = modelView.getColumnIndex(hoveredTime);
				
				boolean precise = shouldUsePreciseHovering(hoveredTrackUUID, columnIndex);
				hoveredEntry = hoveredTrack.getEntryAt(hoveredTime, precise);

				if (hoveredEntry != null) {
					Rectangle rect = modelView.modelToView(hoveredEntry);
				
					if (rect == null || !rect.contains(currentMouseX, currentMouseY))
						hoveredEntry = null;
				}
			}
		}
		
		this.hoveredEntry = hoveredEntry;
	}
	
	private boolean shouldUsePreciseHovering(UUID trackUUID, int columnIndex) {
		if (expandedColumnModel.isColumnExpanded(columnIndex))
			return true;
		if (modelView.isMultiCell(trackUUID, columnIndex))
			return true;
		return false;
	}
	
	@Override
	public void mousePressed(GSMouseEvent event) {
		switch (event.getButton()) {
		case GSMouseEvent.BUTTON_LEFT:
			clickedMouseX = event.getX();
			clickedMouseY = event.getY();
			clickedMouseTime = modelView.viewToModel(event.getX(), event.getY());
			
			if (editable && hoveredEntry != null) {
				GSEResizeArea resizeArea = getHoveredResizeArea(hoveredEntry, event.getX(), event.getY());
				
				if (startDragging(hoveredEntry, resizeAreaToDraggingType(resizeArea)))
					event.consume();
			}
			
			break;
		case GSMouseEvent.BUTTON_RIGHT:
			if (editable && hoveredEntry != null) {
				GSEResizeArea resizeArea = getHoveredResizeArea(hoveredEntry, event.getX(), event.getY());
				
				if (resizeArea != null) {
					toggleEntryEdge(hoveredEntry, resizeAreaToEntryType(resizeArea));
					event.consume();
				} else if (draggingType == GSEDraggingType.NOT_DRAGGING) {
					removeEntry(hoveredTrackUUID, hoveredEntry);
					event.consume();
				}
			}
			break;
		}
	}

	private boolean startDragging(GSTrackEntry entry, GSEDraggingType type) {
		if (isDraggingAllowed(entry, type)) {
			draggingEntry = entry;
			draggingType = type;
			draggingStartTime = entry.getStartTime();
			draggingEndTime = entry.getEndTime();
		
			return true;
		}
		
		return false;
	}
	
	private void stopDragging() {
		if (draggingType != GSEDraggingType.NOT_DRAGGING) {
			draggingEntry = null;
			draggingType = GSEDraggingType.NOT_DRAGGING;
			draggingStartTime = null;
			draggingEndTime = null;
		}
	}
	
	private boolean isDraggingAllowed(GSTrackEntry entry, GSEDraggingType type) {
		if (type != GSEDraggingType.DRAGGING || !expandedColumnModel.hasExpandedColumn())
			return true;
	
		int startColumn = modelView.getColumnIndex(entry.getStartTime());
		int endColumn = modelView.getColumnIndex(entry.getEndTime());
		if (startColumn == endColumn)
			return true;
		
		// Do not allow dragging between columns.
		if (expandedColumnModel.isColumnExpanded(startColumn))
			return false;
		if (expandedColumnModel.isColumnExpanded(endColumn))
			return false;
		
		return true;
	}
	
	private GSEDraggingType resizeAreaToDraggingType(GSEResizeArea resizeArea) {
		if (resizeArea == GSEResizeArea.HOVERING_START)
			return GSEDraggingType.RESIZING_START;
		if (resizeArea == GSEResizeArea.HOVERING_END)
			return GSEDraggingType.RESIZING_END;
		return GSEDraggingType.DRAGGING;
	}
	
	private GSETrackEntryType resizeAreaToEntryType(GSEResizeArea resizeArea) {
		if (resizeArea == GSEResizeArea.HOVERING_START)
			return GSETrackEntryType.EVENT_END;
		return GSETrackEntryType.EVENT_START;
	}
	
	private void toggleEntryEdge(GSTrackEntry entry, GSETrackEntryType type) {
		entry.setType((type == entry.getType()) ? GSETrackEntryType.EVENT_BOTH : type);
	}
	
	private void removeEntry(UUID trackUUID, GSTrackEntry entry) {
		GSTrack track = timeline.getTrack(trackUUID);
		if (track == entry.getOwnerTrack())
			track.removeEntry(entry);
	}
	
	private GSEResizeArea getHoveredResizeArea(GSTrackEntry entry, int mouseX, int mouseY) {
		Rectangle r = modelView.modelToView(entry);
		
		if (r == null || !r.contains(mouseX, mouseY))
			return null;
		
		if (mouseX < r.x + DRAGGING_AREA_SIZE) {
			if (isColumnModifiable(modelView.getColumnIndex(entry.getStartTime())))
				return GSEResizeArea.HOVERING_START;
		} else if (mouseX >= r.x + r.width - DRAGGING_AREA_SIZE) {
			if (isColumnModifiable(modelView.getColumnIndex(entry.getEndTime())))
				return GSEResizeArea.HOVERING_END;
		}
		
		return null;
	}
	
	@Override
	public void mouseReleased(GSMouseEvent event) {
		if (event.getButton() == GSMouseEvent.BUTTON_LEFT) {
			stopDragging();
			event.consume();
		}
	}

	@Override
	public void mouseDragged(GSMouseEvent event) {
		if (editable && event.getButton() == GSMouseEvent.BUTTON_LEFT) {
			if (draggingEntry != null) {
				if (dragSelectedEntry(event.getX(), event.getY()))
					event.consume();
			} else if (hoveredTrackUUID != null && hoveredEntry == null) {
				int dx = event.getX() - clickedMouseX;
				int dy = event.getY() - clickedMouseY;

				if (dx * dx + dy * dy > MINIMUM_DRAG_DISTANCE * MINIMUM_DRAG_DISTANCE) {
					GSEDraggingType draggingType = (dx > 0) ? GSEDraggingType.RESIZING_END :
					                                          GSEDraggingType.RESIZING_START;
					
					if (addTrackEntry(draggingType, event.getX(), event.getY()))
						event.consume();
				}
			}
		}
	}
	
	private boolean dragSelectedEntry(int mouseX, int mouseY) {
		GSBlockEventTime draggedTime = modelView.getDraggedTime(mouseX, mouseY);
		
		if (draggedTime != null) {
			switch (draggingType) {
			case DRAGGING:
				return moveDraggedEntry(draggedTime);
			case RESIZING_START:
				return changeDraggedStart(draggedTime);
			case RESIZING_END:
				return changeDraggedEnd(draggedTime);
			case NOT_DRAGGING:
			default:
				break;
			}
		}
		
		return false;
	}

	private boolean moveDraggedEntry(GSBlockEventTime mouseTime) {
		GSBlockEventTime startTime = draggingStartTime;
		GSBlockEventTime endTime = draggingEndTime;
		
		if (expandedColumnModel.hasExpandedColumn()) {
			int dmt = mouseTime.getMicrotick() - clickedMouseTime.getMicrotick();
			
			if (startTime.getMicrotick() + dmt < 0 || endTime.getMicrotick() + dmt < 0)
				return false;
			
			startTime = startTime.offsetCopy(0, dmt);
			endTime = endTime.offsetCopy(0, dmt);
		} else {
			long dgt = mouseTime.getGametick() - clickedMouseTime.getGametick();
			
			if (startTime.getGametick() + dgt < 0L || endTime.getGametick() + dgt < 0L)
				return false;

			startTime = startTime.offsetCopy(dgt, 0);
			endTime = endTime.offsetCopy(dgt, 0);
		}

		return moveEntry(draggingEntry, startTime, endTime);
	}
	
	private boolean isValidDraggedTime(GSBlockEventTime currentTime, GSBlockEventTime mouseTime) {
		int c0 = modelView.getColumnIndex(currentTime);
		int c1 = modelView.getColumnIndex(mouseTime);
		return isColumnModifiable(c1) && isColumnModifiable(c0);
	}
	
	private boolean changeDraggedStart(GSBlockEventTime mouseTime) {
		if (isValidDraggedTime(draggingEntry.getStartTime(), mouseTime)) {
			GSBlockEventTime startTime = offsetDraggedTime(draggingEntry.getStartTime(), mouseTime);
			return moveEntry(draggingEntry, startTime, draggingEntry.getEndTime());
		}
		
		return false;
	}

	private boolean changeDraggedEnd(GSBlockEventTime mouseTime) {
		if (isValidDraggedTime(draggingEntry.getEndTime(), mouseTime)) {
			GSBlockEventTime endTime = offsetDraggedTime(draggingEntry.getEndTime(), mouseTime);
			return moveEntry(draggingEntry, draggingEntry.getStartTime(), endTime);
		}
		
		return false;
	}
	
	private GSBlockEventTime offsetDraggedTime(GSBlockEventTime t0, GSBlockEventTime t1) {
		return expandedColumnModel.hasExpandedColumn() ? t1 : new GSBlockEventTime(t1.getGametick(), t0.getMicrotick());
	}
	
	private boolean moveEntry(GSTrackEntry entry, GSBlockEventTime startTime, GSBlockEventTime endTime) {
		if (!canMoveEntry(startTime, endTime, draggingEntry))
			return false;
		
		draggingEntry.setTimespan(startTime, endTime);
		
		return true;
	}
	
	private boolean canMoveEntry(GSBlockEventTime startTime, GSBlockEventTime endTime, GSTrackEntry entry) {
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
		GSTrack track = entry.getOwnerTrack();
		
		return !track.isOverlappingEntries(startTime, endTime, entry);
	}

	private boolean addTrackEntry(GSEDraggingType draggingType, int mouseX, int mouseY) {
		GSTrack hoveredTrack = timeline.getTrack(hoveredTrackUUID);
		
		if (hoveredTrack != null) {
			GSBlockEventTime t0 = modelView.getDraggedTime(clickedMouseX, clickedMouseY);
			GSBlockEventTime t1 = modelView.getDraggedTime(mouseX, mouseY);
	
			if (t0 != null && t1 != null) {
				if (t0.isAfter(t1)) {
					GSBlockEventTime tmp = t0;
					t0 = t1;
					t1 = tmp;
				}
			
				if (expandedColumnModel.hasExpandedColumn()) {
					int c0 = modelView.getColumnIndex(t0);
					int c1 = modelView.getColumnIndex(t1);
					if (c0 != c1 || !isColumnModifiable(c0))
						return false;
				} else {
					t0 = new GSBlockEventTime(t0.getGametick(), 0);
					t1 = new GSBlockEventTime(t1.getGametick(), 0);
				}
				
				GSTrackEntry entry = hoveredTrack.tryAddEntry(t0, t1);
				
				if (entry != null) {
					hoveredEntry = entry;
					return startDragging(entry, draggingType);
				}
			}
		}
		
		return false;
	}
	
	private boolean isColumnModifiable(int columnIndex) {
		if (expandedColumnModel.hasExpandedColumn())
			return expandedColumnModel.isColumnExpanded(columnIndex);
		return true;
	}

	@Override
	public void trackRemoved(GSTrack track) {
		UUID trackUUID = track.getTrackUUID();
		
		if (draggingEntry != null) {
			GSTrack draggingTrack = draggingEntry.getOwnerTrack();
			
			if (trackUUID.equals(draggingTrack.getTrackUUID()))
				stopDragging();
		}
	}

	@Override
	public void entryRemoved(GSTrackEntry entry) {
		UUID entryUUID = entry.getEntryUUID();

		if (hoveredEntry != null && entryUUID.equals(hoveredEntry.getEntryUUID()))
			hoveredEntry = null;
		
		if (draggingEntry != null && entryUUID.equals(draggingEntry.getEntryUUID()))
			stopDragging();
	}
	
	@Override
	public void modelViewChanged() {
		updateHoveredEntry();
	}
	
	public UUID getHoveredTrackUUID() {
		return hoveredTrackUUID;
	}
	
	void setHoveredTrackUUID(UUID hoveredTrackUUID) {
		this.hoveredTrackUUID = hoveredTrackUUID;
		
		updateHoveredEntry();
	}
	
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
		
		if (!editable)
			draggingType = GSEDraggingType.NOT_DRAGGING;
	}
	
	private enum GSEResizeArea {

		HOVERING_START, HOVERING_END;
	
	}
	
	private enum GSEDraggingType {
		
		NOT_DRAGGING,
		DRAGGING,
		RESIZING_START,
		RESIZING_END;
		
	}
}
