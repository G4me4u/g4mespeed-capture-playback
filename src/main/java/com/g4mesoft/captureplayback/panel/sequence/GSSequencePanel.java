package com.g4mesoft.captureplayback.panel.sequence;

import java.util.UUID;

import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.captureplayback.panel.GSScrollableContentPanel;
import com.g4mesoft.captureplayback.panel.composition.GSIChannelProvider;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.panel.scroll.GSIScrollListener;
import com.g4mesoft.panel.scroll.GSScrollBar;
import com.g4mesoft.renderer.GSIRenderer2D;
import com.g4mesoft.renderer.GSTexture;
import com.google.common.base.Objects;

import net.minecraft.util.Identifier;

public class GSSequencePanel extends GSScrollableContentPanel implements GSIModelViewListener, GSIMouseListener, GSIKeyListener {

	private static final Identifier ICONS_IDENTIFIER = new Identifier("g4mespeed/captureplayback/textures/icons.png");
	public static final GSTexture ICONS_SHEET = new GSTexture(ICONS_IDENTIFIER, 128, 128);
	
	private static final int CHANNEL_HEADER_WIDTH = 130;
	private static final int COLUMN_HEADER_HEIGHT = 30;
	
	private final GSSession session;
	private final GSSequence sequence;
	private final GSIChannelProvider channelProvider;
	
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSSequenceModelView modelView;
	
	private final GSSequenceContentPanel sequenceContent;
	private final GSChannelHeaderPanel channelHeader;
	private final GSSequenceColumnHeaderPanel columnHeader;
	
	private final GSSequenceInfoPanel infoPanel;
	private final GSSequenceButtonPanel buttonPanel;
	
	private int hoveredMouseX;
	private int hoveredMouseY;
	private int hoveredColumnIndex;
	private UUID hoveredChannelUUID;

	private UUID draggedChannelUUID;
	private int draggedChannelY;
	
	public GSSequencePanel(GSSession session, GSIChannelProvider channelProvider) {
		this.session = session;
		this.sequence = session.get(GSSession.S_SEQUENCE);
		this.channelProvider = channelProvider;

		expandedColumnModel = new GSExpandedColumnModel();
		modelView = new GSSequenceModelView(sequence, expandedColumnModel);
		modelView.addModelViewListener(this);
		
		sequenceContent = new GSSequenceContentPanel(sequence, expandedColumnModel, modelView, this);
		channelHeader = new GSChannelHeaderPanel(sequence, modelView, this);
		columnHeader = new GSSequenceColumnHeaderPanel(sequence, expandedColumnModel, modelView);
		
		infoPanel = new GSSequenceInfoPanel(session, sequence);
		buttonPanel = new GSSequenceButtonPanel();
		
		init();
	}
	
	@Override
	protected void init() {
		super.init();

		add(infoPanel);
		add(buttonPanel);
		
		addMouseEventListener(this);
		addKeyEventListener(this);
	}
	
	@Override
	public GSScrollBar createHorizontalScrollBar(GSIScrollListener listener) {
		return new GSSequencePreviewScrollBar(sequence, modelView, this, listener);
	}
	
	@Override
	protected GSPanel getContent() {
		return sequenceContent;
	}
	
	@Override
	protected GSPanel getColumnHeader() {
		return columnHeader;
	}

	@Override
	protected GSPanel getRowHeader() {
		return channelHeader;
	}
	
	@Override
	protected int getColumnHeaderHeight() {
		return COLUMN_HEADER_HEIGHT;
	}

	@Override
	protected int getRowHeaderWidth() {
		return CHANNEL_HEADER_WIDTH;
	}
	
	@Override
	public void onShown() {
		super.onShown();
		
		modelView.installListeners();
		modelView.updateModelView();
		
		setXOffset(session.get(GSSession.X_OFFSET));
		setYOffset(session.get(GSSession.Y_OFFSET));
		setOpacity(session.get(GSSession.OPACITY));
	}

	@Override
	public void onHidden() {
		super.onHidden();

		session.set(GSSession.X_OFFSET, getXOffset());
		session.set(GSSession.Y_OFFSET, getYOffset());
		session.set(GSSession.OPACITY, getOpacity());
		session.sync();
		
		modelView.uninstallListeners();
		
		setHoveredCell(-1, null);
	}

	@Override
	protected void layoutTopLeft(int x, int y, int width, int height) {
		infoPanel.setBounds(x, y, width, height);
	}
	
	@Override
	protected void layoutBottomLeft(int x, int y, int width, int height) {
		buttonPanel.setBounds(x, y, width, height);
	}
	
	@Override
	public void onBoundsChanged() {
		super.onBoundsChanged();

		if (isVisible())
			modelView.updateModelView();
	}
	
	@Override
	public void renderTranslucent(GSIRenderer2D renderer) {
		super.renderTranslucent(renderer);

		int sw = verticalScrollBar.getWidth();
		int cx = width - sw;

		// Top right corner
		renderer.fillRect(cx, 0, sw, COLUMN_HEADER_HEIGHT, GSSequenceColumnHeaderPanel.COLUMN_HEADER_COLOR);
	}
	
	@Override
	protected void onXOffsetChanged(float xOffset) {
		modelView.setXOffset(Math.round(xOffset));
	}

	@Override
	protected void onYOffsetChanged(float yOffset) {
		modelView.setYOffset(Math.round(yOffset));
	}
	
	@Override
	public float getIncrementalScrollX(int sign) {
		int leadingColumnIndex = modelView.getColumnIndexFromView(0);
		int alignedColumn = leadingColumnIndex + sign;
		if (leadingColumnIndex != -1 && alignedColumn >= 0)
			return sign * modelView.getColumnX(alignedColumn);
		
		return super.getIncrementalScrollX(sign);
	}

	@Override
	public float getIncrementalScrollY(int sign) {
		UUID leadingChannelUUID = modelView.getChannelUUIDFromView(0);
		if (leadingChannelUUID != null) {
			// Default incremental scroll is 2 channels
			int delta = 2 * (modelView.getChannelHeight() + modelView.getChannelSpacing());
			// Normalize scrolling to top channel
			return delta + sign * modelView.getChannelY(leadingChannelUUID);
		}
		
		return super.getIncrementalScrollY(sign);
	}
	
	@Override
	public void setEditable(boolean editable) {
		super.setEditable(editable);
		
		channelHeader.setEditable(editable);
		sequenceContent.setEditable(editable);
	}
	
	private void setHoveredCell(int columnIndex, UUID channelUUID) {
		if (columnIndex != hoveredColumnIndex || !Objects.equal(channelUUID, hoveredChannelUUID)) {
			hoveredChannelUUID = channelUUID;
			hoveredColumnIndex = columnIndex;
			
			sequenceContent.setHoveredCell(columnIndex, channelUUID);
			channelHeader.setHoveredChannelUUID(channelUUID);
			columnHeader.setHoveredColumn(columnIndex);
		}
	}
	
	public void editChannel(UUID channelUUID) {
		GSChannel channel = sequence.getChannel(channelUUID);
		if (channel != null)
			new GSChannelEditorPanel(channel).show(getParent());
	}
	
	public UUID getDraggedChannelUUID() {
		return draggedChannelUUID;
	}

	public void setDraggedChannelUUID(UUID draggedChannelUUID) {
		this.draggedChannelUUID = draggedChannelUUID;
		
		if (draggedChannelUUID != null)
			setHoveredCell(hoveredColumnIndex, draggedChannelUUID);
	}
	
	public int getDraggedChannelY() {
		return draggedChannelY;
	}

	public void setDraggedChannelY(int draggedChannelY) {
		this.draggedChannelY = draggedChannelY;
	}
	
	@Override
	public void modelViewChanged() {
		setXOffset(modelView.getXOffset());
		setYOffset(modelView.getYOffset());
		
		setContentSize(modelView.getMinimumWidth(), modelView.getMinimumHeight());
		
		updateHoveredCell();
	}
	
	@Override
	public void mouseMoved(GSMouseEvent event) {
		hoveredMouseX = event.getX() - sequenceContent.getX();
		hoveredMouseY = event.getY() - sequenceContent.getY();
		
		updateHoveredCell();
	}
	
	private void updateHoveredCell() {
			int columnIndex = modelView.getColumnIndexFromView(hoveredMouseX);
			
			UUID channelUUID;
			if (draggedChannelUUID != null) {
				channelUUID = draggedChannelUUID;
			} else {
				channelUUID = modelView.getChannelUUIDFromView(hoveredMouseY);
			}
			
			setHoveredCell(columnIndex, channelUUID);
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		switch (event.getKeyCode()) {
		case GSKeyEvent.KEY_T:
			if (event.isModifierHeld(GSEvent.MODIFIER_CONTROL)) {
				if (hoveredChannelUUID != null && sequence.removeChannel(hoveredChannelUUID))
					event.consume();
			} else {
				sequence.addChannel(channelProvider.createNextChannelInfo(sequence));
				event.consume();
			}
			break;
		case GSKeyEvent.KEY_E:
			if (!event.isRepeating()) {
				if (expandedColumnModel.hasExpandedColumn()) {
					expandedColumnModel.clearExpandedColumns();
				} else {
					expandedColumnModel.setExpandedColumnRange(0, Integer.MAX_VALUE);
				}
				event.consume();
			}
			break;
		case GSKeyEvent.KEY_Z:
			if (!event.isModifierHeld(GSKeyEvent.MODIFIER_ALT) &&
			     event.isModifierHeld(GSKeyEvent.MODIFIER_CONTROL)) {
				
				// Allow for redo with CTRL + SHIFT + Z
				if (event.isModifierHeld(GSKeyEvent.MODIFIER_SHIFT)) {
					session.get(GSSession.S_UNDO_REDO_HISTORY).redo(sequence);
				} else {
					session.get(GSSession.S_UNDO_REDO_HISTORY).undo(sequence);
				}
			}
			break;
		case GSKeyEvent.KEY_Y:
			if (!event.isAnyModifierHeld(GSKeyEvent.MODIFIER_ALT | GSKeyEvent.MODIFIER_SHIFT) &&
				event.isModifierHeld(GSKeyEvent.MODIFIER_CONTROL)) {
				
				session.get(GSSession.S_UNDO_REDO_HISTORY).redo(sequence);
			}
			break;
		}
	}
}
