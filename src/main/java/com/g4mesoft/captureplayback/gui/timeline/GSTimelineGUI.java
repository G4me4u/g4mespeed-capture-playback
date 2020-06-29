package com.g4mesoft.captureplayback.gui.timeline;

import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import com.g4mesoft.captureplayback.common.GSBlockEventTime;
import com.g4mesoft.captureplayback.gui.GSDarkScrollBar;
import com.g4mesoft.captureplayback.gui.GSITrackProvider;
import com.g4mesoft.captureplayback.timeline.GSITimelineListener;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.gui.scroll.GSIScrollListener;
import com.g4mesoft.gui.scroll.GSIScrollableViewport;
import com.g4mesoft.gui.scroll.GSScrollBar;

import net.minecraft.util.math.BlockPos;

public class GSTimelineGUI extends GSParentPanel implements GSIScrollableViewport, GSIScrollListener, GSITimelineListener, GSIExpandedColumnModelListener {

	private static final int TRACK_HEADER_WIDTH = 100;
	private static final int COLUMN_HEADER_HEIGHT = 30;
	
	private static final int CORNER_SQUARE_COLOR = 0xFF000000;
	
	private static final int TRACK_LABEL_PADDING = 2;
	
	private final GSTimeline timeline;
	private final GSITrackProvider trackProvider;
	
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSTimelineModelView modelView;
	
	private final GSTimelineContentGUI timelineContent;
	private final GSTimelineTrackHeaderGUI trackHeader;
	private final GSTimelineColumnHeaderGUI columnHeader;
	
	private final GSTimelineInfoPanelGUI infoPanel;
	
	private final GSScrollBar verticalScrollBar;
	private final GSScrollBar horizontalScrollBar;
	
	private boolean editable;
	
	private int minContentWidth;
	
	private int contentWidth;
	private int contentHeight;
	
	private UUID hoveredTrackUUID;
	
	public GSTimelineGUI(GSTimeline timeline, GSITrackProvider trackProvider) {
		this.timeline = timeline;
		this.trackProvider = trackProvider;

		expandedColumnModel = new GSExpandedColumnModel();
		modelView = new GSTimelineModelView(timeline, expandedColumnModel);
		
		timelineContent = new GSTimelineContentGUI(timeline, expandedColumnModel, modelView);
		trackHeader = new GSTimelineTrackHeaderGUI(timeline, modelView);
		columnHeader = new GSTimelineColumnHeaderGUI(timeline, expandedColumnModel, modelView);
		
		infoPanel = new GSTimelineInfoPanelGUI();
		
		verticalScrollBar = new GSDarkScrollBar(this, new GSIScrollListener() {
			@Override
			public void scrollChanged(double newScroll) {
				modelView.setYOffset((int)(-newScroll));
			}
		});
		
		horizontalScrollBar = new GSTimelinePreviewScrollBar(timeline, modelView, this, this);
	
		// Editable by default
		editable = true;
	}
	
	@Override
	protected void onAdded() {
		super.onAdded();
		
		timeline.addTimelineListener(this);
		expandedColumnModel.addModelListener(this);
	}

	@Override
	protected void onRemoved() {
		super.onRemoved();

		timeline.removeTimelineListener(this);
		expandedColumnModel.removeModelListener(this);
	}
	
	@Override
	public void init() {
		super.init();

		modelView.setTrackHeight(font.fontHeight + TRACK_LABEL_PADDING * 2);
		
		layoutPanels();
		initModelView();
	}
	
	private void layoutPanels() {
		int cw = Math.max(1, width - TRACK_HEADER_WIDTH - verticalScrollBar.getPreferredScrollBarWidth());
		int ch = Math.max(1, height - COLUMN_HEADER_HEIGHT - horizontalScrollBar.getPreferredScrollBarWidth());

		timelineContent.initBounds(client, TRACK_HEADER_WIDTH, COLUMN_HEADER_HEIGHT, cw, ch);
		trackHeader.initBounds(client, 0, COLUMN_HEADER_HEIGHT, TRACK_HEADER_WIDTH, ch);
		columnHeader.initBounds(client, TRACK_HEADER_WIDTH, 0, cw, COLUMN_HEADER_HEIGHT);
		
		addPanel(timelineContent);
		addPanel(trackHeader);
		addPanel(columnHeader);
	
		infoPanel.initBounds(client, 0, 0, TRACK_HEADER_WIDTH, COLUMN_HEADER_HEIGHT);
		addPanel(infoPanel);

		verticalScrollBar.initVerticalRight(client, width, COLUMN_HEADER_HEIGHT, ch);
		horizontalScrollBar.initHorizontalBottom(client, TRACK_HEADER_WIDTH, height, cw);
		
		addPanel(verticalScrollBar);
		addPanel(horizontalScrollBar);
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
	protected void renderTranslated(int mouseX, int mouseY, float partialTick) {
		super.renderTranslated(mouseX, mouseY, partialTick);

		int cx = width - verticalScrollBar.getWidth();
		int cy = height - horizontalScrollBar.getHeight();
		
		fill(cx, cy, width, height, CORNER_SQUARE_COLOR);
		
		fill(cx, 0, width, COLUMN_HEADER_HEIGHT, GSTimelineColumnHeaderGUI.COLUMN_HEADER_COLOR);
		fill(0, cy, TRACK_HEADER_WIDTH, height, GSTimelineTrackHeaderGUI.TRACK_HEADER_COLOR);
	}
	
	/*
	private static final String ADD_TRACK_BUTTON_TEXT = "+ Add Track";
	private static final int ADD_TRACK_BUTTON_MARGIN = 2;
	private static final int ADD_TRACK_BUTTON_PADDING = 3;
	private static final int ADD_TRACK_BUTTON_COLOR = 0xFF222222;
	
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
	*/
	
	@Override
	public boolean onKeyPressedGS(int key, int scancode, int mods) {
		if (key == GLFW.GLFW_KEY_T) {
			if ((mods & GLFW.GLFW_MOD_CONTROL) != 0) {
				if (hoveredTrackUUID != null && timeline.removeTrack(hoveredTrackUUID))
					return true;
			} else {
				timeline.addTrack(trackProvider.createNewTrackInfo(timeline));
				return true;
			}
		} else if (key == GLFW.GLFW_KEY_E) {
			if (expandedColumnModel.hasExpandedColumn()) {
				expandedColumnModel.clearExpandedColumns();
			} else {
				expandedColumnModel.setExpandedColumnRange(0, Integer.MAX_VALUE);
			}
			
			return true;
		}
		
		return super.onKeyPressedGS(key, scancode, mods);
	}
	
	@Override
	public void onMouseMovedGS(double mouseX, double mouseY) {
		hoveredTrackUUID = modelView.getTrackUUIDFromView((int)mouseY - timelineContent.getY());
		
		if (hoveredTrackUUID != null) {
			GSTrack hoveredTrack = timeline.getTrack(hoveredTrackUUID);
			if (hoveredTrack != null) {
				BlockPos pos = hoveredTrack.getInfo().getPos();
				infoPanel.setInfoText(formatTrackPosition(pos));
			}
		} else {
			infoPanel.setInfoText(null);
		}
		
		super.onMouseMovedGS(mouseX, mouseY);
	}
	
	private String formatTrackPosition(BlockPos pos) {
		return String.format("(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
	}
	
	@Override
	public void preScrollChanged(double newScroll) {
		contentWidth = Math.max(minContentWidth, (int)newScroll + getContentViewWidth());
	}
	
	@Override
	public void scrollChanged(double newScroll) {
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
	public double getIncrementalScrollX(int sign) {
		int leadingColumnIndex = modelView.getColumnIndexFromView(0);
		int alignedColumn = leadingColumnIndex + sign;
		if (leadingColumnIndex != -1 && alignedColumn >= 0)
			return sign * modelView.getColumnX(alignedColumn);
		
		return GSIScrollableViewport.super.getIncrementalScrollX(sign);
	}

	@Override
	public double getIncrementalScrollY(int sign) {
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
	}

	@Override
	public void trackRemoved(GSTrack track) {
		initModelView();
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
	public void entryTimeChanged(GSTrackEntry entry, GSBlockEventTime oldStart, GSBlockEventTime oldEnd) {
		initModelView();
	}

	@Override
	public void onExpandedColumnChanged(int minExpandedColumnIndex, int maxExpandedColumnIndex) {
		updateContentSize();
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
		
		timelineContent.setEditable(editable);
	}
}
