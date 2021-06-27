package com.g4mesoft.captureplayback.panel.sequence;

import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.module.GSSequenceSession;
import com.g4mesoft.captureplayback.panel.composition.GSIChannelProvider;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSColoredIcon;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSIcon;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.dropdown.GSDropdown;
import com.g4mesoft.panel.dropdown.GSDropdownAction;
import com.g4mesoft.panel.dropdown.GSDropdownSubMenu;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.panel.scroll.GSIScrollListener;
import com.g4mesoft.panel.scroll.GSIScrollable;
import com.g4mesoft.panel.scroll.GSScrollBar;
import com.g4mesoft.renderer.GSIRenderer2D;
import com.g4mesoft.renderer.GSTexture;
import com.google.common.base.Objects;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class GSSequencePanel extends GSParentPanel implements GSIScrollable, GSIScrollListener, 
                                                              GSISequenceListener, GSIExpandedColumnModelListener,
                                                              GSIMouseListener, GSIKeyListener {

	private static final Identifier ICONS_IDENTIFIER = new Identifier("g4mespeed/captureplayback/textures/icons.png");
	public static final GSTexture ICONS_SHEET = new GSTexture(ICONS_IDENTIFIER, 128, 128);
	
	private static final int CHANNEL_HEADER_WIDTH = 130;
	private static final int COLUMN_HEADER_HEIGHT = 30;
	
	private static final int BOTTOM_RIGHT_CORNER_COLOR = 0xFF000000;
	
	private static final GSIcon OPACITY_SELECTED_ICON = new GSColoredIcon(0xFFFFFFFF, 4, 4);
	private static final Text OPACITY_TEXT = new TranslatableText("panel.sequence.opacity");
	
	private final GSSequenceSession session;
	private final GSSequence sequence;
	private final GSIChannelProvider channelProvider;
	
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSSequenceModelView modelView;
	
	private final GSSequenceContentPanel sequenceContent;
	private final GSChannelHeaderPanel channelHeader;
	private final GSSequenceColumnHeaderPanel columnHeader;
	
	private final GSSequenceInfoPanel infoPanel;
	private final GSSequenceButtonPanel buttonPanel;
	
	private final GSScrollBar verticalScrollBar;
	private final GSScrollBar horizontalScrollBar;
	
	private boolean editable;
	
	private int minContentWidth;
	
	private int contentWidth;
	private int contentHeight;
	
	private int hoveredMouseX;
	private int hoveredMouseY;
	private int hoveredColumnIndex;
	private UUID hoveredChannelUUID;
	
	private GSESequenceOpacity opacity;

	private UUID draggedChannelUUID;
	private int draggedChannelY;
	
	public GSSequencePanel(GSSequenceSession session, GSIChannelProvider channelProvider) {
		this.session = session;
		this.sequence = session.getActiveSequence();
		this.channelProvider = channelProvider;

		expandedColumnModel = new GSExpandedColumnModel();
		modelView = new GSSequenceModelView(sequence, expandedColumnModel);
		
		sequenceContent = new GSSequenceContentPanel(sequence, expandedColumnModel, modelView, this);
		channelHeader = new GSChannelHeaderPanel(sequence, modelView, this);
		columnHeader = new GSSequenceColumnHeaderPanel(sequence, expandedColumnModel, modelView);
		
		infoPanel = new GSSequenceInfoPanel(session);
		buttonPanel = new GSSequenceButtonPanel();
		
		verticalScrollBar = new GSScrollBar(this, (newScroll) -> {
			modelView.setYOffset((int)(-newScroll));
		});
		horizontalScrollBar = new GSSequencePreviewScrollBar(sequence, modelView, this, this);
		
		verticalScrollBar.setVertical(true);
		horizontalScrollBar.setVertical(false);
	
		// Editable by default
		editable = true;
		// Fully opaque by default
		opacity = GSESequenceOpacity.FULLY_OPAQUE;
		
		add(sequenceContent);
		add(channelHeader);
		add(columnHeader);
		add(infoPanel);
		add(buttonPanel);
		
		add(verticalScrollBar);
		add(horizontalScrollBar);
		
		addMouseEventListener(this);
		addKeyEventListener(this);
	}
	
	@Override
	public void onShown() {
		super.onShown();
		
		sequence.addSequenceListener(this);
		expandedColumnModel.addModelListener(this);
		
		initModelView();
		updateHoveredCell();
		
		horizontalScrollBar.setScrollOffset(session.getXOffset());
		verticalScrollBar.setScrollOffset(session.getYOffset());
		setOpacity(session.getOpacity());
	}

	@Override
	public void onHidden() {
		super.onHidden();

		session.setXOffset(horizontalScrollBar.getScrollOffset());
		session.setYOffset(verticalScrollBar.getScrollOffset());
		session.setOpacity(getOpacity());
		
		sequence.removeSequenceListener(this);
		expandedColumnModel.removeModelListener(this);
	
		setHoveredCell(-1, null);
	}
	
	@Override
	public void onBoundsChanged() {
		super.onBoundsChanged();

		layoutPanels();
		
		if (isVisible())
			initModelView();
	}
	
	private void layoutPanels() {
		GSDimension vs = verticalScrollBar.getPreferredSize();
		GSDimension hs = horizontalScrollBar.getPreferredSize();
		int cw = Math.max(1, width - CHANNEL_HEADER_WIDTH - vs.getWidth());
		int ch = Math.max(1, height - COLUMN_HEADER_HEIGHT - hs.getHeight());

		sequenceContent.setBounds(CHANNEL_HEADER_WIDTH, COLUMN_HEADER_HEIGHT, cw, ch);
		channelHeader.setBounds(0, COLUMN_HEADER_HEIGHT, CHANNEL_HEADER_WIDTH, ch);
		columnHeader.setBounds(CHANNEL_HEADER_WIDTH, 0, cw, COLUMN_HEADER_HEIGHT);
		
		infoPanel.setBounds(0, 0, CHANNEL_HEADER_WIDTH, COLUMN_HEADER_HEIGHT);
		buttonPanel.setBounds(0, height - hs.getHeight(), CHANNEL_HEADER_WIDTH, hs.getHeight());

		verticalScrollBar.setBounds(width - vs.getWidth(), COLUMN_HEADER_HEIGHT, vs.getWidth(), ch);
		horizontalScrollBar.setBounds(CHANNEL_HEADER_WIDTH, height - hs.getHeight(), cw, hs.getHeight());
	}

	public void initModelView() {
		modelView.updateModelView();
		
		updateContentSize();
	}
	
	public void updateContentSize() {
		int newContentWidth = modelView.getMinimumWidth();
		int newContentHeight = modelView.getMinimumHeight();

		minContentWidth = Math.max(getContentViewWidth(), newContentWidth);
		if (minContentWidth > contentWidth)
			contentWidth = minContentWidth;
		
		if (newContentHeight > getContentViewHeight()) {
			contentHeight = newContentHeight;
			verticalScrollBar.setEnabled(true);
		} else {
			contentHeight = getContentViewHeight();
			verticalScrollBar.setEnabled(false);
		}
		
		// Updating the scroll will ensure it is within bounds.
		horizontalScrollBar.setScrollOffset(horizontalScrollBar.getScrollOffset());
		verticalScrollBar.setScrollOffset(verticalScrollBar.getScrollOffset());
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		float oldOpacity = renderer.getOpacity();
		renderer.setOpacity(opacity.getOpacity());
		
		super.render(renderer);

		int sw = verticalScrollBar.getWidth();
		int sh = horizontalScrollBar.getHeight();
		int cx = width - sw;
		int cy = height - sh;

		// Top right corner
		renderer.fillRect(cx, 0, sw, COLUMN_HEADER_HEIGHT, GSSequenceColumnHeaderPanel.COLUMN_HEADER_COLOR);
		// Bottom right corner
		renderer.fillRect(cx, cy, sw, sh, BOTTOM_RIGHT_CORNER_COLOR);

		renderer.setOpacity(oldOpacity);
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
					session.getUndoRedoHistory().redo();
				} else {
					session.getUndoRedoHistory().undo();
				}
			}
			break;
		case GSKeyEvent.KEY_Y:
			if (!event.isAnyModifierHeld(GSKeyEvent.MODIFIER_ALT | GSKeyEvent.MODIFIER_SHIFT) &&
				event.isModifierHeld(GSKeyEvent.MODIFIER_CONTROL)) {
				
				session.getUndoRedoHistory().redo();
			}
			break;
		}
	}
	
	@Override
	public void createRightClickMenu(GSDropdown dropdown, int x, int y) {
		dropdown.addItemSeparator();
		GSDropdown opacityMenu = new GSDropdown();
		for (GSESequenceOpacity opacity : GSESequenceOpacity.OPACITIES) {
			GSIcon icon = (this.opacity == opacity) ? OPACITY_SELECTED_ICON : null;
			Text text = new TranslatableText(opacity.getName());
			opacityMenu.addItem(new GSDropdownAction(icon, text, () -> {
				setOpacity(opacity);
			}));
		}
		dropdown.addItem(new GSDropdownSubMenu(OPACITY_TEXT, opacityMenu));
		
		super.createRightClickMenu(dropdown, x, y);
	}
	
	@Override
	public void preScrollChanged(float newScroll) {
		contentWidth = Math.max(minContentWidth, (int)newScroll + getContentViewWidth());
	}
	
	@Override
	public void scrollChanged(float newScroll) {
		modelView.setXOffset((int)(-newScroll));
	}

	@Override
	public int getContentWidth() {
		return contentWidth;
	}

	@Override
	public int getContentHeight() {
		return contentHeight;
	}
	
	@Override
	public int getContentViewWidth() {
		return sequenceContent.getWidth();
	}

	@Override
	public int getContentViewHeight() {
		return sequenceContent.getHeight();
	}
	
	@Override
	public float getIncrementalScrollX(int sign) {
		int leadingColumnIndex = modelView.getColumnIndexFromView(0);
		int alignedColumn = leadingColumnIndex + sign;
		if (leadingColumnIndex != -1 && alignedColumn >= 0)
			return sign * modelView.getColumnX(alignedColumn);
		
		return GSIScrollable.super.getIncrementalScrollX(sign);
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
		
		return GSIScrollable.super.getIncrementalScrollY(sign);
	}
	
	@Override
	public void channelAdded(GSChannel channel, UUID prevUUID) {
		initModelView();
		updateHoveredCell();
	}

	@Override
	public void channelRemoved(GSChannel channel, UUID oldPrevUUID) {
		initModelView();
		updateHoveredCell();
	}
	
	@Override
	public void channelMoved(GSChannel channel, UUID newPrevUUID, UUID oldPrevUUID) {
		modelView.updateChannelIndexLookup();
		updateHoveredCell();
	}

	@Override
	public void entryAdded(GSChannelEntry entry) {
		initModelView();
	}

	@Override
	public void entryRemoved(GSChannelEntry entry) {
		initModelView();
	}

	@Override
	public void entryTimeChanged(GSChannelEntry entry, GSSignalTime oldStart, GSSignalTime oldEnd) {
		initModelView();
	}

	@Override
	public void onExpandedColumnChanged(int minExpandedColumnIndex, int maxExpandedColumnIndex) {
		updateContentSize();
		updateHoveredCell();
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
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
		
		channelHeader.setEditable(editable);
		sequenceContent.setEditable(editable);
	}
	
	public GSESequenceOpacity getOpacity() {
		return opacity;
	}
	
	public void setOpacity(GSESequenceOpacity opacity) {
		if (opacity == null)
			throw new IllegalArgumentException("opacity is null");
		this.opacity = opacity;
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
}
