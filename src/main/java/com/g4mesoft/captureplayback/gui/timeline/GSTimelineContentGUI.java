package com.g4mesoft.captureplayback.gui.timeline;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.access.GSIBufferBuilderAccess;
import com.g4mesoft.captureplayback.common.GSBlockEventTime;
import com.g4mesoft.captureplayback.timeline.GSETrackEntryType;
import com.g4mesoft.captureplayback.timeline.GSITimelineListener;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
import com.g4mesoft.gui.GSPanel;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.math.MatrixStack;

public class GSTimelineContentGUI extends GSPanel implements GSITimelineListener, GSITimelineModelViewListener {

	private static final int TEXT_COLOR = 0xFFFFFFFF;
	
	private static final int ENTRY_BORDER_THICKNESS = 2;

	private static final double MINIMUM_DRAG_DISTANCE = 5.0;

	private static final int DRAGGING_AREA_SIZE = 6;
	private static final int DRAGGING_PADDING = 2;
	private static final int DRAGGING_AREA_COLOR = 0x40FFFFFF;
	
	private final GSTimeline timeline;
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSTimelineModelView modelView;
	
	private final Rectangle tmpRenderRect;
	
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
	
	public GSTimelineContentGUI(GSTimeline timeline, GSExpandedColumnModel expandedColumnModel, GSTimelineModelView modelView) {
		this.timeline = timeline;
		this.expandedColumnModel = expandedColumnModel;
		this.modelView = modelView;
		
		tmpRenderRect = new Rectangle();
		
		draggingType = GSDraggingType.NOT_DRAGGING;
	}
	
	@Override
	protected void onAdded() {
		super.onAdded();

		timeline.addTimelineListener(this);
		modelView.addModelViewListener(this);
	}

	@Override
	protected void onRemoved() {
		super.onRemoved();
		
		timeline.removeTimelineListener(this);
		modelView.removeModelViewListener(this);
	}
	
	@Override
	protected void renderTranslated(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		super.renderTranslated(matrixStack, mouseX, mouseY, partialTicks);

		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		((GSIBufferBuilderAccess)buffer).pushClip(0, 0, width, height);
		
		renderColumns(matrixStack, mouseX, mouseY);
		renderTracks(matrixStack, mouseX, mouseY);
		
		if (editable)
			renderHoveredEdge(matrixStack, mouseX, mouseY);

		((GSIBufferBuilderAccess)buffer).popClip();
	}
	
	protected void renderColumns(MatrixStack matrixStack, int mouseX, int mouseY) {
		int columnStart = Math.max(0, modelView.getColumnIndexFromView(0));
		int columnEnd = modelView.getColumnIndexFromView(width - 1);

		int x0 = modelView.getColumnX(columnStart);
		for (int columnIndex = columnStart; columnIndex <= columnEnd; columnIndex++) {
			int x1 = x0 + modelView.getColumnWidth(columnIndex);
			renderColumn(matrixStack, mouseX, mouseY, columnIndex, x0, x1);
			x0 = x1;
		}
	}
	
	protected void renderColumn(MatrixStack matrixStack, int mouseX, int mouseY, int columnIndex, int x0, int x1) {
		fill(matrixStack, x0, 0, x1, height, getColumnColor(columnIndex));
	
		if (mouseX >= x0 && mouseX < x1) {
			fill(matrixStack, x0, 0, x0 + 1, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
			fill(matrixStack, x1, 0, x1 - 1, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
		}
		
		if (expandedColumnModel.isColumnExpanded(columnIndex)) {
			int duration = modelView.getColumnDuration(columnIndex);

			for (int mt = 1; mt < duration; mt++) {
				int x = modelView.getMicrotickColumnX(columnIndex, mt);
				int y = GSTimelineColumnHeaderGUI.DOTTED_LINE_SPACING / 2;
			
				drawVerticalDottedLine(matrixStack, x, y, height, GSTimelineColumnHeaderGUI.DOTTED_LINE_LENGTH, 
						GSTimelineColumnHeaderGUI.DOTTED_LINE_SPACING, GSTimelineColumnHeaderGUI.MT_COLUMN_LINE_COLOR);
			}
		}
	}
	
	protected int getColumnColor(int columnIndex) {
		return ((columnIndex & 0x1) != 0) ? GSTimelineColumnHeaderGUI.DARK_COLUMN_COLOR : 
		                                    GSTimelineColumnHeaderGUI.COLUMN_COLOR;
	}
	
	protected void renderTracks(MatrixStack matrixStack, int mouseX, int mouseY) {
		for (Map.Entry<UUID, GSTrack> trackEntry : timeline.getTrackEntries()) {
			UUID trackUUID = trackEntry.getKey();
			GSTrack track = trackEntry.getValue();
			
			int y = modelView.getTrackY(trackUUID);
			if (y + modelView.getTrackHeight() > 0 && y < height)
				renderTrack(matrixStack, track, trackUUID, y, (track == hoveredTrack));
			y += modelView.getTrackHeight();
		}
	}
	
	protected void renderTrack(MatrixStack matrixStack, GSTrack track, UUID trackUUID, int y, boolean trackHovered) {
		int y1 = y + modelView.getTrackHeight();
		
		if (trackHovered)
			fill(matrixStack, 0, y, width, y1, GSTimelineTrackHeaderGUI.TRACK_HOVER_COLOR);

		for (GSTrackEntry entry : track.getEntries())
			renderTrackEntry(matrixStack, trackUUID, entry, getTrackColor(track));
		renderMultiCells(matrixStack, trackUUID, y);
		
		fill(matrixStack, 0, y1, width, y1 + modelView.getTrackSpacing(), GSTimelineTrackHeaderGUI.TRACK_SPACING_COLOR);
	}
	
	protected int getTrackColor(GSTrack track) {
		return (0xFF << 24) | track.getInfo().getColor();
	}
	
	protected void renderTrackEntry(MatrixStack matrixStack, UUID trackUUID, GSTrackEntry entry, int color) {
		if (selectedEntry == entry || hoveredEntry == entry)
			color = darkenColor(color);

		Rectangle rect = modelView.modelToView(trackUUID, entry, tmpRenderRect);
		
		if (rect != null) {
			int x1 = rect.x + rect.width;
			int y1 = rect.y + rect.height;

			fill(matrixStack, rect.x, rect.y, x1, y1, darkenColor(color));
			
			int x0 = rect.x;
			if (entry.getType() != GSETrackEntryType.EVENT_START)
				x1 -= ENTRY_BORDER_THICKNESS;
			if (entry.getType() != GSETrackEntryType.EVENT_END)
				x0 += ENTRY_BORDER_THICKNESS;
			
			fill(matrixStack, x0, rect.y + ENTRY_BORDER_THICKNESS, x1, y1 - ENTRY_BORDER_THICKNESS, color);
		}
	}
	
	protected void renderMultiCells(MatrixStack matrixStack, UUID trackUUID, int y) {
		Iterator<GSMultiCellInfo> itr = modelView.getMultiCellIterator(trackUUID);
		while (itr.hasNext()) {
			GSMultiCellInfo multiCellInfo = itr.next();
			if (!expandedColumnModel.isColumnExpanded(multiCellInfo.getColumnIndex()))
				renderMultiCell(matrixStack, trackUUID, y, multiCellInfo);
		}
	}
	
	protected void renderMultiCell(MatrixStack matrixStack, UUID trackUUID, int y, GSMultiCellInfo multiCellInfo) {
		String infoText = formatMultiCellInfo(multiCellInfo);
		
		int columnIndex = multiCellInfo.getColumnIndex();
		int xc = modelView.getColumnX(columnIndex) + modelView.getColumnWidth(columnIndex) / 2;
		int ty = y + (modelView.getTrackHeight() - textRenderer.fontHeight) / 2;
		drawCenteredString(matrixStack, textRenderer, infoText, xc, ty, TEXT_COLOR);
	}

	protected String formatMultiCellInfo(GSMultiCellInfo multiCellInfo) {
		if (multiCellInfo.getCount() > 9)
			return "+";
		return Integer.toString(multiCellInfo.getCount());
	}
	
	protected void renderHoveredEdge(MatrixStack matrixStack, int mouseX, int mouseY) {
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
			fill(matrixStack, x0, y0, x1, y1, DRAGGING_AREA_COLOR);
		}
	}
	
	@Override
	public void onMouseMovedGS(double mouseX, double mouseY) {
		super.onMouseMovedGS(mouseX, mouseY);
		
		currentMouseX = mouseX;
		currentMouseY = mouseY;
		
		updateHoveredEntry();
	}
	
	private void updateHoveredEntry() {
		UUID hoveredTrackUUID = modelView.getTrackUUIDFromView((int)currentMouseY);
		if (hoveredTrackUUID != null) {
			hoveredTrack = timeline.getTrack(hoveredTrackUUID);
			
			hoveredTime = modelView.viewToModel((int)currentMouseX, (int)currentMouseY);
			if (hoveredTime != null && hoveredTrack != null) {
				int columnIndex = modelView.getColumnIndex(hoveredTime);
				boolean multiCell = modelView.isMultiCell(hoveredTrackUUID, columnIndex);
				boolean mtPrecision = expandedColumnModel.isColumnExpanded(columnIndex) || multiCell;
				hoveredEntry = hoveredTrack.getEntryAt(hoveredTime, mtPrecision);
				
				if (hoveredEntry != null) {
					Rectangle rect = modelView.modelToView(hoveredTrackUUID, hoveredEntry);
					if (rect == null || !rect.contains(currentMouseX, currentMouseY))
						hoveredEntry = null;
				}
			}
		} else {
			hoveredTime = null;
			hoveredTrack = null;
			hoveredEntry = null;
		}
	}
	
	@Override
	public boolean onMouseClickedGS(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1 && mouseY >= 0.0) {
			clickedMouseX = mouseX;
			clickedMouseY = mouseY;
			clickedMouseTime = hoveredTime;
			
			if (hoveredEntry != null) {
				updateSelectedEntry();
				
				if (prepareDragging(mouseX, mouseY))
					return true;
			} else {
				// In case we are adding a new entry, we should return
				// true to ensure we receive the mouse dragging events.
				return true;
			}
		} else if (editable && button == GLFW.GLFW_MOUSE_BUTTON_2 && hoveredEntry != null) {
			GSResizeArea resizeArea = getHoveredResizeArea(hoveredTrack.getTrackUUID(), hoveredEntry, (int)mouseX, (int)mouseY);
			if (resizeArea != null) {
				toggleEntryEdge(hoveredEntry, resizeAreaToEntryType(resizeArea));
			} else if (draggingType == GSDraggingType.NOT_DRAGGING) {
				removeEntry(hoveredTrack, hoveredEntry);
			}
		}
		
		return super.onMouseClickedGS(mouseX, mouseY, button);
	}

	private boolean prepareDragging(double mouseX, double mouseY) {
		if (!editable || hoveredTrack == null || hoveredEntry == null)
			return false;

		UUID trackUUID = hoveredTrack.getTrackUUID();
		GSResizeArea resizeArea = getHoveredResizeArea(trackUUID, hoveredEntry, (int)mouseX, (int)mouseY);

		GSDraggingType type = resizeAreaToDraggingType(resizeArea);
		if (isDraggingAllowed(hoveredEntry, type)) {
			draggingType = type;
			draggedStartTime = hoveredEntry.getStartTime();
			draggedEndTime = hoveredEntry.getEndTime();
			return true;
		}
		
		return false;
	}
	
	private boolean isDraggingAllowed(GSTrackEntry entry, GSDraggingType type) {
		if (type != GSDraggingType.DRAGGING || !expandedColumnModel.hasExpandedColumn())
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
	
	private GSDraggingType resizeAreaToDraggingType(GSResizeArea resizeArea) {
		if (resizeArea == GSResizeArea.HOVERING_START)
			return GSDraggingType.RESIZING_START;
		if (resizeArea == GSResizeArea.HOVERING_END)
			return GSDraggingType.RESIZING_END;
		return GSDraggingType.DRAGGING;
	}
	
	private GSETrackEntryType resizeAreaToEntryType(GSResizeArea resizeArea) {
		if (resizeArea == GSResizeArea.HOVERING_START)
			return GSETrackEntryType.EVENT_END;
		return GSETrackEntryType.EVENT_START;
	}
	
	private void toggleEntryEdge(GSTrackEntry hoveredEntry2, GSETrackEntryType type) {
		hoveredEntry.setType((type == hoveredEntry.getType()) ? GSETrackEntryType.EVENT_BOTH : type);
	}
	
	private void removeEntry(GSTrack track, GSTrackEntry entry) {
		if (track == entry.getOwnerTrack())
			track.removeEntry(entry);
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
	public boolean onMouseReleasedGS(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_1 && mouseY > 0) {
			if (draggedEntryChanged || toggleSelection)
				unselectEntries();
			toggleSelection = (selectedEntry != null);
		}
		
		draggingType = GSDraggingType.NOT_DRAGGING;
		draggedEntryChanged = false;
		
		return super.onMouseReleasedGS(mouseX, mouseY, button);
	}

	@Override
	public boolean onMouseDraggedGS(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (editable && button == GLFW.GLFW_MOUSE_BUTTON_1) {
			if (selectedTrack != null && selectedEntry != null) {
				return dragSelectedEntry(mouseX, mouseY);
			} else if (hoveredTrack != null && hoveredEntry == null) {
				double dx = mouseX - clickedMouseX;
				double dy = mouseY - clickedMouseY;

				if (dx * dx + dy * dy > MINIMUM_DRAG_DISTANCE * MINIMUM_DRAG_DISTANCE) {
					GSDraggingType draggingType = (dx > 0) ? GSDraggingType.RESIZING_END :
					                                         GSDraggingType.RESIZING_START;
					addTrackEntry(mouseX, mouseY, draggingType);
					return true;
				}
			}
		}
		
		return super.onMouseDraggedGS(mouseX, mouseY, button, dragX, dragY);
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
		
		if (expandedColumnModel.hasExpandedColumn()) {
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
		return expandedColumnModel.hasExpandedColumn() ? t1 : new GSBlockEventTime(t1.getGametick(), t0.getMicrotick());
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
		
			if (expandedColumnModel.hasExpandedColumn()) {
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
	
	private boolean isColumnModifiable(int columnIndex) {
		if (expandedColumnModel.hasExpandedColumn())
			return expandedColumnModel.isColumnExpanded(columnIndex);
		return true;
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
	public void trackRemoved(GSTrack track) {
		UUID trackUUID = track.getTrackUUID();
		if (hoveredTrack != null && trackUUID.equals(hoveredTrack.getTrackUUID())) {
			hoveredTrack = null;
			hoveredEntry = null;
		}
		
		if (selectedTrack != null && trackUUID.equals(selectedTrack.getTrackUUID()))
			unselectEntries();
	}

	@Override
	public void entryRemoved(GSTrackEntry entry) {
		UUID entryUUID = entry.getEntryUUID();
		if (hoveredEntry != null && entryUUID.equals(hoveredEntry.getEntryUUID()))
			hoveredEntry = null;
		if (selectedEntry != null && entryUUID.equals(selectedEntry.getEntryUUID()))
			unselectEntries();
	}
	
	@Override
	public void modelViewChanged() {
		updateHoveredEntry();
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
