package com.g4mesoft.captureplayback.gui.timeline;

import java.awt.Rectangle;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrack;
import com.g4mesoft.captureplayback.timeline.GSTrackEntry;
import com.g4mesoft.gui.renderer.GSIRenderer2D;
import com.g4mesoft.gui.renderer.GSTexture;
import com.g4mesoft.gui.scroll.GSIScrollListener;
import com.g4mesoft.gui.scroll.GSIScrollableViewport;
import com.g4mesoft.gui.scroll.GSScrollBar;

import net.minecraft.util.Identifier;

public class GSTimelinePreviewScrollBar extends GSScrollBar {

	private static final Identifier TEXTURE_IDENTIFIER = new Identifier("g4mespeed/captureplayback/textures/scroll_bar_preview.png");
	private static final GSTexture SCROLL_BUTTON_TEXTURE = new GSTexture(TEXTURE_IDENTIFIER, 30, 54);
	
	private static final int PREVIEW_BACKGROUND = 0xFF171717;
	
	private static final int VERTICAL_BORDER_COLOR = 0xFF000000;
	
	private static final int DISABLED_KNOB_COLOR = 0x40333333;
	private static final int HOVERED_KNOB_COLOR = 0x40DDDDDD;
	private static final int KNOB_COLOR = 0x40888888;
	
	private static final int PREVIEW_TRACK_COUNT = 25;

	private static final int VERTICAL_BORDER_HEIGHT = 1;
	private static final int SCROLL_BUTTON_WIDTH = 10;
	private static final int SCROLL_BUTTON_HEIGHT = /* 27 */ PREVIEW_TRACK_COUNT + 2 * VERTICAL_BORDER_HEIGHT;
	
	private final GSTimeline timeline;
	private final GSTimelineModelView modelView;
	
	private final Rectangle tmpEntryRect;
	
	public GSTimelinePreviewScrollBar(GSTimeline timeline, GSTimelineModelView modelView, GSIScrollableViewport parent, GSIScrollListener listener) {
		super(parent, listener);
		
		this.timeline = timeline;
		this.modelView = modelView;
	
		tmpEntryRect = new Rectangle();
	}
	
	@Override
	public void initVerticalLeft(int xl, int yt, int height) {
		throw new IllegalStateException("Vertical scroll bar not supported");
	}
	
	@Override
	public void initVerticalRight(int xr, int yt, int height) {
		throw new IllegalStateException("Vertical scroll bar not supported");
	}

	@Override
	protected int getScrollButtonSpriteX(boolean left, boolean hovered) {
		return isEnabled() ? (hovered ? SCROLL_BUTTON_WIDTH : 0) : (2 * SCROLL_BUTTON_WIDTH);
	}

	@Override
	protected int getScrollButtonSpriteY(boolean left, boolean hovered) {
		return left ? 0 : SCROLL_BUTTON_HEIGHT;
	}
	
	@Override
	protected GSTexture getScrollButtonTexture() {
		return SCROLL_BUTTON_TEXTURE;
	}
	
	@Override
	protected void drawKnobArea(GSIRenderer2D renderer) {
		renderPreview(renderer, SCROLL_BUTTON_WIDTH, 0, width - 2 * SCROLL_BUTTON_WIDTH, height);
	}
	
	private void renderPreview(GSIRenderer2D renderer, int x, int y, int width, int height) {
		renderer.fillRect(x, y, width, height, VERTICAL_BORDER_COLOR);

		y += VERTICAL_BORDER_HEIGHT;
		height -= 2 * VERTICAL_BORDER_HEIGHT;
		
		renderer.fillRect(x, y, width, height, PREVIEW_BACKGROUND);
		
		for (GSTrack track : timeline.getTracks()) {
			if (isTrackVisible(track, x, y, width, height))
				renderTrackPreview(renderer, track, x, y, width, height);
		}
	}
	
	private boolean isTrackVisible(GSTrack track, int x, int y, int width, int height) {
		int trackY = modelView.getTrackY(track.getTrackUUID());
		if (trackY == -1)
			return false;
		
		int mappedTrackY = mapEntryY(trackY);
		return (mappedTrackY >= y && mappedTrackY < y + height);
	}
	
	private void renderTrackPreview(GSIRenderer2D renderer, GSTrack track, int x0, int y0, int x1, int y1) {
		int color = renderer.darkenColor(getTrackColor(track));
		
		for (GSTrackEntry entry : track.getEntries()) {
			Rectangle bounds = getMappedEntryBounds(entry);
			
			if (bounds != null && clampEntryBounds(bounds, x0, y0, x1, y1))
				renderer.fillRect(bounds.x, bounds.y, bounds.width, bounds.height, color);
		}
	}
	
	private int getTrackColor(GSTrack track) {
		return (0xFF << 24) | track.getInfo().getColor();
	}
	
	private boolean clampEntryBounds(Rectangle bounds, int x0, int y0, int x1, int y1) {
		// Clamp left, top, right, bottom
		if (bounds.x < x0) {
			bounds.width += bounds.x - x0;
			bounds.x = x0;
		}
		
		if (bounds.y < y0) {
			bounds.height += bounds.y;
			bounds.y = y0;
		}
		
		if (bounds.x + bounds.width > x1)
			bounds.width -= x1 - (bounds.x + bounds.width);
		
		if (bounds.y + bounds.height > y1)
			bounds.height -= y1 - (bounds.y + bounds.height);
		
		return (bounds.width > 0 && bounds.height > 0);
	}
	
	@Override
	protected int getKnobColor(boolean hovered) {
		if (!isEnabled())
			return DISABLED_KNOB_COLOR;
		if (isScrollDragActive() || hovered)
			return HOVERED_KNOB_COLOR;
		return KNOB_COLOR;
	}
	
	private Rectangle getMappedEntryBounds(GSTrackEntry entry) {
		Rectangle bounds = modelView.modelToView(entry, tmpEntryRect);
		return (bounds == null) ? null : mapEntryBounds(bounds);
	}
	
	private Rectangle mapEntryBounds(Rectangle bounds) {
		if (bounds.width <= 0 || bounds.height <= 0)
			return null;
		
		int x1 = bounds.x + bounds.width;
		bounds.x = mapEntryX(bounds.x);
		bounds.width  = mapEntryX(x1) - bounds.x;

		if (bounds.width <= 0)
			return null;

		int y1 = bounds.y + bounds.height;
		bounds.y = mapEntryY(bounds.y);
		bounds.height = Math.max(1, mapEntryY(y1) - bounds.y);
		
		return bounds;
	}
	
	private int mapEntryX(int x) {
		// Translate into static view
		x -= modelView.getXOffset();
	
		x = x * getKnobAreaSize() / getContentSize();
		return SCROLL_BUTTON_WIDTH + x;
	}

	private int mapEntryY(int y) {
		// Translate into static view
		y -= modelView.getYOffset();
		
		y /= (modelView.getTrackHeight() + modelView.getTrackSpacing());

		// Ensure that we can see the top track when vertical scroll
		// is zero and bottom track when vertical scroll is maxScroll.
		int hiddenVerticalContent = parent.getContentHeight() - parent.getContentViewHeight();
		int hiddenTrackCount = timeline.getTracks().size() - PREVIEW_TRACK_COUNT;
		if (hiddenVerticalContent > 0 && hiddenTrackCount > 0)
			y += hiddenTrackCount * modelView.getYOffset() / hiddenVerticalContent;
		
		return VERTICAL_BORDER_HEIGHT + y;
	}
	
	@Override
	protected int getButtonWidth() {
		// Note that this is opposite since we only support the
		// horizontal scroll bar.
		return SCROLL_BUTTON_HEIGHT;
	}

	@Override
	protected int getButtonHeight() {
		return SCROLL_BUTTON_WIDTH;
	}
	
	@Override
	public int getPreferredScrollBarWidth() {
		return SCROLL_BUTTON_HEIGHT;
	}
}
