package com.g4mesoft.captureplayback.panel.sequence;

import java.util.Iterator;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSColoredIcon;
import com.g4mesoft.panel.GSIcon;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.panel.dropdown.GSDropdown;
import com.g4mesoft.panel.dropdown.GSDropdownAction;
import com.g4mesoft.panel.dropdown.GSDropdownSubMenu;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.renderer.GSIRenderer;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class GSSequenceContentPanel extends GSPanel implements GSISequenceListener, GSIModelViewListener,
                                                               GSIMouseListener {

	private static final int TEXT_COLOR = 0xFFFFFFFF;
	
//	public static final int COLUMN_COLOR = 0xDA181818;
//	public static final int DARK_COLUMN_COLOR = 0xDA0A0A0A;
	public static final int COLUMN_COLOR = 0xFF2E2E2E;
	public static final int DARK_COLUMN_COLOR = 0xFF2A2A2A;

	public static final int CHANNEL_SPACING_COLOR = 0xFF202020;
	
	private static final int ENTRY_BORDER_THICKNESS = 2;

	private static final int MINIMUM_DRAG_DISTANCE = 5;

	private static final int DRAGGING_AREA_SIZE = 6;
	private static final int DRAGGING_PADDING = 2;
	private static final int DRAGGING_AREA_COLOR = 0x40FFFFFF;
	
	private static final int DOTTED_LINE_LENGTH  = GSSequenceColumnHeaderPanel.DOTTED_LINE_LENGTH;
	private static final int DOTTED_LINE_SPACING = GSSequenceColumnHeaderPanel.DOTTED_LINE_SPACING;
	private static final int DOTTED_LINE_COLOR   = GSSequenceColumnHeaderPanel.DOTTED_LINE_COLOR;
	
	private static final GSIcon TYPE_SELECTED_ICON = new GSColoredIcon(0xFFFFFFFF, 4, 4);
	private static final Text CREATE_ENTRY_TEXT = new TranslatableText("panel.sequencecontent.createentry");
	private static final Text ENTRY_TYPE_TEXT   = new TranslatableText("panel.sequencecontent.entrytype");
	private static final Text DELETE_ENTRY_TEXT = new TranslatableText("panel.sequencecontent.deleteentry");
	
	private final GSSequence sequence;
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSSequenceModelView modelView;
	private final GSSequencePanel sequencePanel;
	
	private final GSRectangle tmpRenderRect;
	
	private int currentMouseX;
	private int currentMouseY;
	private UUID hoveredChannelUUID;
	private GSChannelEntry hoveredEntry;
	
	private int clickedMouseX;
	private int clickedMouseY;
	private GSSignalTime clickedMouseTime;
	
	private GSChannelEntry draggingEntry;
	private GSEDraggingType draggingType;
	private GSSignalTime draggingStartTime;
	private GSSignalTime draggingEndTime;
	
	private boolean editable;
	
	public GSSequenceContentPanel(GSSequence sequence, GSExpandedColumnModel expandedColumnModel,
	                              GSSequenceModelView modelView, GSSequencePanel sequencePanel) {
		
		this.sequence = sequence;
		this.expandedColumnModel = expandedColumnModel;
		this.modelView = modelView;
		this.sequencePanel = sequencePanel;
		
		tmpRenderRect = new GSRectangle();
		
		draggingType = GSEDraggingType.NOT_DRAGGING;
		
		addMouseEventListener(this);
		
		// Editable by default
		editable = true;
	}
	
	@Override
	public void onShown() {
		super.onShown();
		
		sequence.addSequenceListener(this);
		modelView.addModelViewListener(this);
	}

	@Override
	public void onHidden() {
		super.onHidden();
		
		sequence.removeSequenceListener(this);
		modelView.removeModelViewListener(this);
		
		stopDragging();
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);

		renderer.pushClip(0, 0, width, height);
		
		renderColumns(renderer);
		renderChannels(renderer);
		
		if (editable) {
			GSChannel draggedChannel = sequence.getChannel(sequencePanel.getDraggedChannelUUID());
			if (draggedChannel != null) {
				renderDraggedChannel(renderer, draggedChannel);
			} else {
				renderHoveredEdge(renderer);
			}
		}

		renderer.popClip();
	}

	protected void renderColumns(GSIRenderer2D renderer) {
		renderColumns(renderer, 0, height);
	}
	
	protected void renderColumns(GSIRenderer2D renderer, int y, int height) {
		int columnStart = Math.max(0, modelView.getColumnIndexFromView(0));
		int columnEnd = modelView.getColumnIndexFromView(width - 1);

		int x = modelView.getColumnX(columnStart);
		for (int columnIndex = columnStart; columnIndex <= columnEnd; columnIndex++) {
			int cw = modelView.getColumnWidth(columnIndex);
			renderColumn(renderer, columnIndex, x, y, cw, height);
			x += cw;
		}
	}
	
	protected void renderColumn(GSIRenderer2D renderer, int columnIndex, int cx, int y, int cw, int height) {
		renderer.fillRect(cx, y, cw, height, getColumnColor(columnIndex));
		renderer.drawVLine(cx + cw - 1, y, y + height, GSSequenceColumnHeaderPanel.COLUMN_LINE_COLOR);
		
		if (expandedColumnModel.isColumnExpanded(columnIndex)) {
			int offset = modelView.getYOffset() % (DOTTED_LINE_LENGTH + DOTTED_LINE_SPACING);
			int ly = y + DOTTED_LINE_SPACING / 2 + offset;

			int duration = modelView.getColumnDuration(columnIndex);
			for (int mt = 1; mt < duration; mt++) {
				int lx = modelView.getMicrotickColumnX(columnIndex, mt) - 1;
				renderer.drawDottedVLine(lx, ly, y + height, DOTTED_LINE_LENGTH, DOTTED_LINE_SPACING, DOTTED_LINE_COLOR);
			}
		}
	}
	
	protected int getColumnColor(int columnIndex) {
		return ((columnIndex & 0x1) != 0) ? DARK_COLUMN_COLOR : COLUMN_COLOR;
	}
	
	protected void renderChannels(GSIRenderer2D renderer) {
		for (GSChannel channel : sequence.getChannels()) {
			int cy = modelView.getChannelY(channel.getChannelUUID());
			if (cy + modelView.getChannelHeight() > 0 && cy < height) {
				if (channel.getChannelUUID().equals(sequencePanel.getDraggedChannelUUID())) {
					int h = modelView.getChannelHeight() + modelView.getChannelSpacing();
					renderer.fillRect(0, cy, width, h, COLUMN_COLOR);
				} else {
					renderChannel(renderer, channel, cy);
				}
			}
		}
	}

	protected void renderDraggedChannel(GSIRenderer2D renderer, GSChannel draggedChannel) {
		int cy = sequencePanel.getDraggedChannelY();
		renderColumns(renderer, cy, modelView.getChannelHeight());
		renderChannel(renderer, draggedChannel, cy);
	}
	
	protected void renderChannel(GSIRenderer2D renderer, GSChannel channel, int cy) {
		int ch = modelView.getChannelHeight();
		
		if (channel.getChannelUUID().equals(hoveredChannelUUID))
			renderer.fillRect(0, cy, width, ch, GSChannelHeaderPanel.CHANNEL_HOVER_COLOR);

		int entryOffsetY = cy - modelView.getChannelY(channel.getChannelUUID());
		for (GSChannelEntry entry : channel.getEntries())
			renderChannelEntry(renderer, entry, channel.getInfo().getColor(), entryOffsetY);
		renderMultiCells(renderer, channel.getChannelUUID(), cy);
		
		renderer.fillRect(0, cy + ch, width, modelView.getChannelSpacing(), CHANNEL_SPACING_COLOR);
	}
	
	protected void renderChannelEntry(GSIRenderer2D renderer, GSChannelEntry entry, int color, int entryOffsetY) {
		GSRectangle rect = modelView.modelToView(entry, tmpRenderRect);
		
		if (rect != null) {
			rect.y += entryOffsetY;
			
			if (draggingEntry == entry || hoveredEntry == entry)
				color = GSIRenderer.darkenColor(color);
			
			renderer.fillRect(rect.x, rect.y, rect.width, rect.height, GSIRenderer.darkenColor(color));
			
			if (entry.getType().hasEndEvent())
				rect.width -= ENTRY_BORDER_THICKNESS;
			if (entry.getType().hasStartEvent()) {
				rect.x += ENTRY_BORDER_THICKNESS;
				rect.width -= ENTRY_BORDER_THICKNESS;
			}
			
			rect.y += ENTRY_BORDER_THICKNESS;
			rect.height -= 2 * ENTRY_BORDER_THICKNESS;
			
			renderer.fillRect(rect.x, rect.y, rect.width, rect.height, color);
		}
	}
	
	protected void renderMultiCells(GSIRenderer2D renderer, UUID channelUUID, int cy) {
		Iterator<GSMultiCellInfo> itr = modelView.getMultiCellIterator(channelUUID);
		while (itr.hasNext()) {
			GSMultiCellInfo multiCellInfo = itr.next();
			if (!expandedColumnModel.isColumnExpanded(multiCellInfo.getColumnIndex()))
				renderMultiCell(renderer, cy, multiCellInfo);
		}
	}
	
	protected void renderMultiCell(GSIRenderer2D renderer, int cy, GSMultiCellInfo multiCellInfo) {
		String infoText = formatMultiCellInfo(multiCellInfo);
		
		int columnIndex = multiCellInfo.getColumnIndex();
		int xc = modelView.getColumnX(columnIndex) + modelView.getColumnWidth(columnIndex) / 2;
		int ty = cy + (modelView.getChannelHeight() - renderer.getTextAscent()) / 2;
		renderer.drawCenteredText(infoText, xc, ty, TEXT_COLOR);
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
	
	private void renderResizeArea(GSIRenderer2D renderer, GSChannelEntry entry, GSEResizeArea resizeArea) {
		GSRectangle rect = modelView.modelToView(entry, tmpRenderRect);
		
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
	public void populateRightClickMenu(GSDropdown dropdown, int x, int y) {
		UUID channelUUID = hoveredChannelUUID;
		GSChannelEntry entry = hoveredEntry;
		
		dropdown.addItem(new GSDropdownAction(CREATE_ENTRY_TEXT, () -> {
			GSSignalTime time = modelView.viewToModel(x, y);
			GSChannel channel = sequence.getChannel(channelUUID);
			
			if (time != null && channel != null)
				channel.tryAddEntry(time, time.offsetCopy(1L, 0));
		}));
		dropdown.separate();
		GSDropdown entryTypeMenu = new GSDropdown();
		if (entry != null) {
			for (GSEChannelEntryType type : GSEChannelEntryType.TYPES) {
				GSIcon icon = (entry.getType() == type) ? TYPE_SELECTED_ICON : null;
				Text text = new TranslatableText(type.getName());
				entryTypeMenu.addItem(new GSDropdownAction(icon, text, () -> {
					entry.setType(type);
				}));
			}
		}
		GSDropdownSubMenu entryType;
		dropdown.addItem(entryType = new GSDropdownSubMenu(ENTRY_TYPE_TEXT, entryTypeMenu));
		GSDropdownAction deleteEntry;
		dropdown.addItem(deleteEntry = new GSDropdownAction(DELETE_ENTRY_TEXT, () ->  {
			removeEntry(channelUUID, entry);
		}));
		
		entryType.setEnabled(entry != null);
		deleteEntry.setEnabled(entry != null);
	}
	
	@Override
	public void mouseMoved(GSMouseEvent event) {
		currentMouseX = event.getX();
		currentMouseY = event.getY();
		
		updateHoveredEntry();
	}
	
	private void updateHoveredEntry() {
		hoveredEntry = (hoveredChannelUUID != null) ? 
				getEntryAt(hoveredChannelUUID, currentMouseX, currentMouseY) : null;
	}
	
	private GSChannelEntry getEntryAt(UUID channelUUID, int x, int y) {
		GSChannel hoveredChannel = sequence.getChannel(channelUUID);
		GSSignalTime hoveredTime = modelView.viewToModel(x, y);
		
		if (hoveredChannel != null && hoveredTime != null) {
			int columnIndex = modelView.getColumnIndex(hoveredTime);
			
			boolean precise = shouldUsePreciseHovering(channelUUID, columnIndex);
			GSChannelEntry entry = hoveredChannel.getEntryAt(hoveredTime, precise);

			if (entry != null) {
				GSRectangle rect = modelView.modelToView(entry);
			
				if (rect != null && rect.contains(x, y))
					return entry;
			}
		}
		
		return null;
	}
	
	private boolean shouldUsePreciseHovering(UUID channelUUID, int columnIndex) {
		if (expandedColumnModel.isColumnExpanded(columnIndex))
			return true;
		if (modelView.isMultiCell(channelUUID, columnIndex))
			return true;
		return false;
	}
	
	@Override
	public void mousePressed(GSMouseEvent event) {
		if (event.getButton() == GSMouseEvent.BUTTON_LEFT) {
			clickedMouseX = event.getX();
			clickedMouseY = event.getY();
			clickedMouseTime = modelView.viewToModel(event.getX(), event.getY());

			if (editable && hoveredEntry != null) {
				if (event.isModifierHeld(GSMouseEvent.MODIFIER_CONTROL)) {
					if (draggingType == GSEDraggingType.NOT_DRAGGING) {
						removeEntry(hoveredChannelUUID, hoveredEntry);
						event.consume();
					}
				} else {
					GSEResizeArea resizeArea = getHoveredResizeArea(hoveredEntry, event.getX(), event.getY());
					
					if (startDragging(hoveredEntry, resizeAreaToDraggingType(resizeArea)))
						event.consume();
				}
			}
		}
	}

	private boolean startDragging(GSChannelEntry entry, GSEDraggingType type) {
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
	
	private boolean isDraggingAllowed(GSChannelEntry entry, GSEDraggingType type) {
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
	
	private void removeEntry(UUID channelUUID, GSChannelEntry entry) {
		GSChannel channel = sequence.getChannel(channelUUID);
		if (channel == entry.getParent())
			channel.removeEntry(entry.getEntryUUID());
	}
	
	private GSEResizeArea getHoveredResizeArea(GSChannelEntry entry, int mouseX, int mouseY) {
		GSRectangle r = modelView.modelToView(entry);
		
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
			if (draggingType != GSEDraggingType.NOT_DRAGGING) {
				stopDragging();
				event.consume();
			}

			if (currentMouseX == clickedMouseX && currentMouseY == clickedMouseY && hoveredEntry != null) {
				GSEResizeArea resizeArea = getHoveredResizeArea(hoveredEntry, event.getX(), event.getY());
				
				if (resizeArea != null) {
					toggleEntryEdge(hoveredEntry, resizeArea);
					event.consume();
				}
			}
		}
	}
	
	private void toggleEntryEdge(GSChannelEntry entry, GSEResizeArea resizeArea) {
		if (resizeArea == GSEResizeArea.HOVERING_START) {
			entry.setType(entry.getType().toggleStart());
		} else {
			entry.setType(entry.getType().toggleEnd());
		}
	}

	@Override
	public void mouseDragged(GSMouseEvent event) {
		if (editable && event.getButton() == GSMouseEvent.BUTTON_LEFT) {
			if (draggingEntry != null) {
				if (dragSelectedEntry(event.getX(), event.getY()))
					event.consume();
			} else if (hoveredChannelUUID != null && hoveredEntry == null) {
				int dx = event.getX() - clickedMouseX;
				int dy = event.getY() - clickedMouseY;

				if (dx * dx + dy * dy > MINIMUM_DRAG_DISTANCE * MINIMUM_DRAG_DISTANCE) {
					GSEDraggingType draggingType = (dx > 0) ? GSEDraggingType.RESIZING_END :
					                                          GSEDraggingType.RESIZING_START;
					
					if (addChannelEntry(draggingType, event.getX(), event.getY()))
						event.consume();
				}
			}
		}
	}
	
	private boolean dragSelectedEntry(int mouseX, int mouseY) {
		GSSignalTime draggedTime = modelView.getDraggedTime(mouseX, mouseY);
		
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

	private boolean moveDraggedEntry(GSSignalTime mouseTime) {
		long dgt = 0L;
		int dmt = 0;
		
		if (expandedColumnModel.hasExpandedColumn()) {
			dmt = mouseTime.getMicrotick() - clickedMouseTime.getMicrotick();
		} else {
			dgt = mouseTime.getGametick() - clickedMouseTime.getGametick();
		}

		if (draggingStartTime.getGametick() + dgt < 0L || draggingStartTime.getMicrotick() + dmt < 0)
			return false;
		if (draggingEndTime.getGametick() + dgt < 0L || draggingEndTime.getMicrotick() + dmt < 0)
			return false;
		
		GSSignalTime startTime = draggingStartTime.offsetCopy(dgt, dmt);
		GSSignalTime endTime = draggingEndTime.offsetCopy(dgt, dmt);

		return moveEntry(draggingEntry, startTime, endTime);
	}
	
	private boolean isValidDraggedTime(GSSignalTime currentTime, GSSignalTime mouseTime) {
		int c0 = modelView.getColumnIndex(currentTime);
		int c1 = modelView.getColumnIndex(mouseTime);
		return isColumnModifiable(c1) && isColumnModifiable(c0);
	}
	
	private boolean changeDraggedStart(GSSignalTime mouseTime) {
		if (isValidDraggedTime(draggingEntry.getStartTime(), mouseTime)) {
			GSSignalTime startTime = offsetDraggedTime(draggingEntry.getStartTime(), mouseTime);
			return moveEntry(draggingEntry, startTime, draggingEntry.getEndTime());
		}
		
		return false;
	}

	private boolean changeDraggedEnd(GSSignalTime mouseTime) {
		if (isValidDraggedTime(draggingEntry.getEndTime(), mouseTime)) {
			GSSignalTime endTime = offsetDraggedTime(draggingEntry.getEndTime(), mouseTime);
			return moveEntry(draggingEntry, draggingEntry.getStartTime(), endTime);
		}
		
		return false;
	}
	
	private GSSignalTime offsetDraggedTime(GSSignalTime t0, GSSignalTime t1) {
		return expandedColumnModel.hasExpandedColumn() ? t1 : new GSSignalTime(t1.getGametick(), t0.getMicrotick());
	}
	
	private boolean moveEntry(GSChannelEntry entry, GSSignalTime startTime, GSSignalTime endTime) {
		if (!canMoveEntry(startTime, endTime, draggingEntry))
			return false;
		
		draggingEntry.setTimespan(startTime, endTime);
		
		return true;
	}
	
	private boolean canMoveEntry(GSSignalTime startTime, GSSignalTime endTime, GSChannelEntry entry) {
		// The model has not changed. No reason to move it.
		if (entry.getStartTime().isEqual(startTime) && entry.getEndTime().isEqual(endTime))
			return false;
		
		// Ensure we do not move entries out of the sequence.
		if (startTime.getGametick() < modelView.getColumnGametick(0))
			return false;
		if (startTime.getMicrotick() < 0)
			return false;
		
		// Ensure the entry is a model-valid format.
		if (endTime.isBefore(startTime))
			return false;
		
		// Lastly ensure we do not overlap other entries.
		GSChannel channel = entry.getParent();
		
		return !channel.isOverlappingEntries(startTime, endTime, entry);
	}

	private boolean addChannelEntry(GSEDraggingType draggingType, int mouseX, int mouseY) {
		GSChannel hoveredChannel = sequence.getChannel(hoveredChannelUUID);
		
		if (hoveredChannel != null) {
			GSSignalTime t0 = modelView.getDraggedTime(clickedMouseX, clickedMouseY);
			GSSignalTime t1 = modelView.getDraggedTime(mouseX, mouseY);
	
			if (t0 != null && t1 != null) {
				if (t0.isAfter(t1)) {
					GSSignalTime tmp = t0;
					t0 = t1;
					t1 = tmp;
				}
			
				if (expandedColumnModel.hasExpandedColumn()) {
					int c0 = modelView.getColumnIndex(t0);
					int c1 = modelView.getColumnIndex(t1);
					if (c0 != c1 || !isColumnModifiable(c0))
						return false;
				} else {
					t0 = new GSSignalTime(t0.getGametick(), 0);
					t1 = new GSSignalTime(t1.getGametick(), 0);
				}
				
				GSChannelEntry entry = hoveredChannel.tryAddEntry(t0, t1);
				
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
	public void channelRemoved(GSChannel channel, UUID oldPrevUUID) {
		UUID channelUUID = channel.getChannelUUID();
		
		if (draggingEntry != null) {
			GSChannel draggingChannel = draggingEntry.getParent();
			
			if (channelUUID.equals(draggingChannel.getChannelUUID()))
				stopDragging();
		}
	}

	@Override
	public void entryRemoved(GSChannelEntry entry) {
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
	
	public UUID getHoveredChannelUUID() {
		return hoveredChannelUUID;
	}
	
	void setHoveredCell(int columnIndex, UUID channelUUID) {
		hoveredChannelUUID = channelUUID;
		
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
