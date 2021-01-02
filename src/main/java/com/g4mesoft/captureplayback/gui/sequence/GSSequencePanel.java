package com.g4mesoft.captureplayback.gui.sequence;

import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.gui.GSDarkScrollBar;
import com.g4mesoft.captureplayback.gui.GSIChannelProvider;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.gui.event.GSEvent;
import com.g4mesoft.gui.event.GSIKeyListener;
import com.g4mesoft.gui.event.GSIMouseListener;
import com.g4mesoft.gui.event.GSKeyEvent;
import com.g4mesoft.gui.event.GSMouseEvent;
import com.g4mesoft.gui.scroll.GSIScrollListener;
import com.g4mesoft.gui.scroll.GSIScrollableViewport;
import com.g4mesoft.gui.scroll.GSScrollBar;
import com.g4mesoft.renderer.GSIRenderer2D;
import com.google.common.base.Objects;

public class GSSequencePanel extends GSParentPanel implements GSIScrollableViewport, GSIScrollListener, 
                                                              GSISequenceListener, GSIExpandedColumnModelListener,
                                                              GSIMouseListener, GSIKeyListener {

	private static final int CHANNEL_HEADER_WIDTH = 100;
	private static final int COLUMN_HEADER_HEIGHT = 30;
	
	private static final int CORNER_SQUARE_COLOR = 0xFF000000;
	
	private final GSSequence sequence;
	private final GSIChannelProvider channelProvider;
	
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSSequenceModelView modelView;
	
	private final GSSequenceContentPanel sequenceContent;
	private final GSSequenceChannelHeaderPanel channelHeader;
	private final GSSequenceColumnHeaderPanel columnHeader;
	
	private final GSSequenceInfoPanel infoPanel;
	private final GSSequenceButtonPanel buttonPanel;
	
	private final GSScrollBar verticalScrollBar;
	private final GSScrollBar horizontalScrollBar;
	
	private boolean editable;
	
	private int minContentWidth;
	
	private int contentWidth;
	private int contentHeight;
	
	private int hoveredMouseY;
	private UUID hoveredChannelUUID;
	
	public GSSequencePanel(GSSequence sequence, GSIChannelProvider channelProvider) {
		this.sequence = sequence;
		this.channelProvider = channelProvider;

		expandedColumnModel = new GSExpandedColumnModel();
		modelView = new GSSequenceModelView(sequence, expandedColumnModel);
		
		sequenceContent = new GSSequenceContentPanel(sequence, expandedColumnModel, modelView);
		channelHeader = new GSSequenceChannelHeaderPanel(sequence, modelView);
		columnHeader = new GSSequenceColumnHeaderPanel(sequence, expandedColumnModel, modelView);
		
		infoPanel = new GSSequenceInfoPanel(sequence);
		buttonPanel = new GSSequenceButtonPanel(sequence);
		
		verticalScrollBar = new GSDarkScrollBar(this, new GSIScrollListener() {
			@Override
			public void scrollChanged(float newScroll) {
				modelView.setYOffset((int)(-newScroll));
			}
		});
		
		horizontalScrollBar = new GSSequencePreviewScrollBar(sequence, modelView, this, this);
	
		// Editable by default
		editable = true;
		
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
		updateHoveredChannel();
	}

	@Override
	public void onHidden() {
		super.onHidden();

		sequence.removeSequenceListener(this);
		expandedColumnModel.removeModelListener(this);
	
		setHoveredChannelUUID(null);
	}
	
	@Override
	public void onBoundsChanged() {
		super.onBoundsChanged();

		layoutPanels();
		
		if (isVisible())
			initModelView();
	}
	
	private void layoutPanels() {
		int sw = verticalScrollBar.getPreferredScrollBarWidth();
		int sh = horizontalScrollBar.getPreferredScrollBarWidth();
		int cw = Math.max(1, width - CHANNEL_HEADER_WIDTH - sw);
		int ch = Math.max(1, height - COLUMN_HEADER_HEIGHT - sh);

		sequenceContent.setBounds(CHANNEL_HEADER_WIDTH, COLUMN_HEADER_HEIGHT, cw, ch);
		channelHeader.setBounds(0, COLUMN_HEADER_HEIGHT, CHANNEL_HEADER_WIDTH, ch);
		columnHeader.setBounds(CHANNEL_HEADER_WIDTH, 0, cw, COLUMN_HEADER_HEIGHT);
		
		infoPanel.setBounds(0, 0, CHANNEL_HEADER_WIDTH, COLUMN_HEADER_HEIGHT);
		buttonPanel.setBounds(0, height - sh, CHANNEL_HEADER_WIDTH, sh);

		verticalScrollBar.initVerticalRight(width, COLUMN_HEADER_HEIGHT, ch);
		horizontalScrollBar.initHorizontalBottom(CHANNEL_HEADER_WIDTH, height, cw);
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
		super.render(renderer);

		int sw = verticalScrollBar.getWidth();
		int sh = horizontalScrollBar.getHeight();
		int cx = width - sw;
		int cy = height - sh;
		
		// Top right corner
		renderer.fillRect(cx, 0, sw, COLUMN_HEADER_HEIGHT, CORNER_SQUARE_COLOR);
		// Bottom right corner
		renderer.fillRect(cx, cy, sw, sh, CORNER_SQUARE_COLOR);
	}
	
	@Override
	public void mouseMoved(GSMouseEvent event) {
		hoveredMouseY = event.getY() - sequenceContent.getY();
		
		updateHoveredChannel();
	}
	
	private void updateHoveredChannel() {
		setHoveredChannelUUID(modelView.getChannelUUIDFromView(hoveredMouseY));
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		if (event.getKeyCode() == GSKeyEvent.KEY_T) {
			if (event.isModifierHeld(GSEvent.MODIFIER_CONTROL)) {
				if (hoveredChannelUUID != null && sequence.removeChannel(hoveredChannelUUID))
					event.consume();
			} else {
				sequence.addChannel(channelProvider.createNextChannelInfo(sequence));
				event.consume();
			}
		} else if (!event.isRepeating() && event.getKeyCode() == GSKeyEvent.KEY_E) {
			if (expandedColumnModel.hasExpandedColumn()) {
				expandedColumnModel.clearExpandedColumns();
			} else {
				expandedColumnModel.setExpandedColumnRange(0, Integer.MAX_VALUE);
			}
			
			event.consume();
		}
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
		
		return GSIScrollableViewport.super.getIncrementalScrollX(sign);
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
		
		return GSIScrollableViewport.super.getIncrementalScrollY(sign);
	}
	
	@Override
	public void channelAdded(GSChannel track) {
		initModelView();
		updateHoveredChannel();
	}

	@Override
	public void channelRemoved(GSChannel track) {
		initModelView();
		updateHoveredChannel();
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
	}
	
	public UUID getHoveredTrackUUID() {
		return hoveredChannelUUID;
	}
	
	private void setHoveredChannelUUID(UUID hoveredTrackUUID) {
		if (!Objects.equal(hoveredTrackUUID, this.hoveredChannelUUID)) {
			this.hoveredChannelUUID = hoveredTrackUUID;
			
			sequenceContent.setHoveredChannelUUID(hoveredTrackUUID);
			channelHeader.setHoveredChannelUUID(hoveredTrackUUID);
			infoPanel.setHoveredChannelUUID(hoveredTrackUUID);
		}
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
		
		channelHeader.setEditable(editable);
		sequenceContent.setEditable(editable);
	}
}
