package com.g4mesoft.captureplayback.gui.timeline;

import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.gui.GSDarkScrollBar;
import com.g4mesoft.captureplayback.gui.GSITrackProvider;
import com.g4mesoft.captureplayback.timeline.GSITimelineListener;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
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

public class GSTimelinePanel extends GSParentPanel implements GSIScrollableViewport, GSIScrollListener, 
                                                              GSITimelineListener, GSIExpandedColumnModelListener,
                                                              GSIMouseListener, GSIKeyListener {

	private static final int TRACK_HEADER_WIDTH = 100;
	private static final int COLUMN_HEADER_HEIGHT = 30;
	
	private static final int CORNER_SQUARE_COLOR = 0xFF000000;
	
	private final GSTimeline timeline;
	private final GSITrackProvider trackProvider;
	
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSTimelineModelView modelView;
	
	private final GSTimelineContentPanel timelineContent;
	private final GSTimelineTrackHeaderPanel trackHeader;
	private final GSTimelineColumnHeaderPanel columnHeader;
	
	private final GSTimelineInfoPanel infoPanel;
	
	private final GSScrollBar verticalScrollBar;
	private final GSScrollBar horizontalScrollBar;
	
	private boolean editable;
	
	private int minContentWidth;
	
	private int contentWidth;
	private int contentHeight;
	
	private int hoveredMouseY;
	private UUID hoveredTrackUUID;
	
	public GSTimelinePanel(GSTimeline timeline, GSITrackProvider trackProvider) {
		this.timeline = timeline;
		this.trackProvider = trackProvider;

		expandedColumnModel = new GSExpandedColumnModel();
		modelView = new GSTimelineModelView(timeline, expandedColumnModel);
		
		timelineContent = new GSTimelineContentPanel(timeline, expandedColumnModel, modelView);
		trackHeader = new GSTimelineTrackHeaderPanel(timeline, modelView);
		columnHeader = new GSTimelineColumnHeaderPanel(timeline, expandedColumnModel, modelView);
		
		infoPanel = new GSTimelineInfoPanel(timeline);
		
		verticalScrollBar = new GSDarkScrollBar(this, new GSIScrollListener() {
			@Override
			public void scrollChanged(float newScroll) {
				modelView.setYOffset((int)(-newScroll));
			}
		});
		
		horizontalScrollBar = new GSTimelinePreviewScrollBar(timeline, modelView, this, this);
	
		// Editable by default
		editable = true;
		
		add(timelineContent);
		add(trackHeader);
		add(columnHeader);
		add(infoPanel);
	
		add(verticalScrollBar);
		add(horizontalScrollBar);
		
		addMouseEventListener(this);
		addKeyEventListener(this);
	}
	
	@Override
	public void onShown() {
		super.onShown();
		
		timeline.addTimelineListener(this);
		expandedColumnModel.addModelListener(this);
		
		initModelView();
		updateHoveredTrack();
	}

	@Override
	public void onHidden() {
		super.onHidden();

		timeline.removeTimelineListener(this);
		expandedColumnModel.removeModelListener(this);
	
		setHoveredTrackUUID(null);
	}
	
	@Override
	public void onBoundsChanged() {
		super.onBoundsChanged();

		layoutPanels();
		
		if (isVisible())
			initModelView();
	}
	
	private void layoutPanels() {
		int cw = Math.max(1, width - TRACK_HEADER_WIDTH - verticalScrollBar.getPreferredScrollBarWidth());
		int ch = Math.max(1, height - COLUMN_HEADER_HEIGHT - horizontalScrollBar.getPreferredScrollBarWidth());

		timelineContent.setBounds(TRACK_HEADER_WIDTH, COLUMN_HEADER_HEIGHT, cw, ch);
		trackHeader.setBounds(0, COLUMN_HEADER_HEIGHT, TRACK_HEADER_WIDTH, ch);
		columnHeader.setBounds(TRACK_HEADER_WIDTH, 0, cw, COLUMN_HEADER_HEIGHT);
		
		infoPanel.setBounds(0, 0, TRACK_HEADER_WIDTH, COLUMN_HEADER_HEIGHT);

		verticalScrollBar.initVerticalRight(width, COLUMN_HEADER_HEIGHT, ch);
		horizontalScrollBar.initHorizontalBottom(TRACK_HEADER_WIDTH, height, cw);
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
		
		renderer.fillRect(cx, 0, sw, COLUMN_HEADER_HEIGHT, CORNER_SQUARE_COLOR);
		renderer.fillRect(cx, cy, sw, sh, CORNER_SQUARE_COLOR);
		
		renderer.fillRect(cx, 0, sw, COLUMN_HEADER_HEIGHT, GSTimelineColumnHeaderPanel.COLUMN_HEADER_COLOR);
		renderer.fillRect(0, cy, TRACK_HEADER_WIDTH, sh, GSTimelineTrackHeaderPanel.TRACK_HEADER_COLOR);
	}
	
	@Override
	public void mouseMoved(GSMouseEvent event) {
		hoveredMouseY = event.getY() - timelineContent.getY();
		
		updateHoveredTrack();
	}
	
	private void updateHoveredTrack() {
		setHoveredTrackUUID(modelView.getTrackUUIDFromView(hoveredMouseY));
	}
	
	@Override
	public void keyPressed(GSKeyEvent event) {
		if (event.getKeyCode() == GSKeyEvent.KEY_T) {
			if (event.isModifierHeld(GSEvent.MODIFIER_CONTROL)) {
				if (hoveredTrackUUID != null && timeline.removeTrack(hoveredTrackUUID))
					event.consume();
			} else {
				timeline.addTrack(trackProvider.createNewTrackInfo(timeline));
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
		return timelineContent.getWidth();
	}

	@Override
	public int getContentViewHeight() {
		return timelineContent.getHeight();
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
		UUID leadingTrackUUID = modelView.getTrackUUIDFromView(0);
		if (leadingTrackUUID != null) {
			// Default incremental scroll is 2 tracks
			int delta = 2 * (modelView.getTrackHeight() + modelView.getTrackSpacing());
			// Normalize scrolling to top track
			return delta + sign * modelView.getTrackY(leadingTrackUUID);
		}
		
		return GSIScrollableViewport.super.getIncrementalScrollY(sign);
	}
	
	@Override
	public void trackAdded(GSTrack track) {
		initModelView();
		updateHoveredTrack();
	}

	@Override
	public void trackRemoved(GSTrack track) {
		initModelView();
		updateHoveredTrack();
	}

	@Override
	public void entryAdded(GSTrackEntry entry) {
		initModelView();
	}

	@Override
	public void entryRemoved(GSTrackEntry entry) {
		initModelView();
	}

	@Override
	public void entryTimeChanged(GSTrackEntry entry, GSSignalTime oldStart, GSSignalTime oldEnd) {
		initModelView();
	}

	@Override
	public void onExpandedColumnChanged(int minExpandedColumnIndex, int maxExpandedColumnIndex) {
		updateContentSize();
	}
	
	public UUID getHoveredTrackUUID() {
		return hoveredTrackUUID;
	}
	
	private void setHoveredTrackUUID(UUID hoveredTrackUUID) {
		if (!Objects.equal(hoveredTrackUUID, this.hoveredTrackUUID)) {
			this.hoveredTrackUUID = hoveredTrackUUID;
			
			timelineContent.setHoveredTrackUUID(hoveredTrackUUID);
			trackHeader.setHoveredTrackUUID(hoveredTrackUUID);
			infoPanel.setHoveredTrackUUID(hoveredTrackUUID);
		}
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
		
		trackHeader.setEditable(editable);
		timelineContent.setEditable(editable);
	}
}
