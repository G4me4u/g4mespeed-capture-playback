package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.panel.scroll.GSIScrollListener;
import com.g4mesoft.panel.scroll.GSIScrollable;
import com.g4mesoft.panel.scroll.GSScrollBar;
import com.g4mesoft.renderer.GSIRenderer;
import com.g4mesoft.renderer.GSIRenderer2D;
import com.g4mesoft.renderer.GSTexture;

import net.minecraft.util.Identifier;

public class GSSequencePreviewScrollBar extends GSScrollBar {

	private static final Identifier TEXTURE_IDENTIFIER = new Identifier("g4mespeed/captureplayback/textures/scroll_bar_preview.png");
	private static final GSTexture SCROLL_BUTTON_TEXTURE = new GSTexture(TEXTURE_IDENTIFIER, 30, 54);
	
	private static final int PREVIEW_BACKGROUND = 0xFF171717;
	
	private static final int VERTICAL_BORDER_COLOR = 0xFF000000;
	
	private static final int DISABLED_KNOB_COLOR = 0x40333333;
	private static final int HOVERED_KNOB_COLOR = 0x40DDDDDD;
	private static final int KNOB_COLOR = 0x40888888;
	
	private static final int PREVIEW_CHANNEL_COUNT = 25;

	private static final int VERTICAL_BORDER_HEIGHT = 1;
	private static final int SCROLL_BUTTON_WIDTH = 10;
	private static final int SCROLL_BUTTON_HEIGHT = /* 27 */ PREVIEW_CHANNEL_COUNT + 2 * VERTICAL_BORDER_HEIGHT;
	
	private final GSSequence sequence;
	private final GSSequenceModelView modelView;
	
	private final GSRectangle tmpEntryRect;
	
	public GSSequencePreviewScrollBar(GSSequence sequence, GSSequenceModelView modelView, GSIScrollable parent, GSIScrollListener listener) {
		super(parent, listener);
		
		this.sequence = sequence;
		this.modelView = modelView;
	
		tmpEntryRect = new GSRectangle();

		setVertical(false);
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
		
		for (GSChannel channel : sequence.getChannels()) {
			if (isChannelVisible(channel, x, y, width, height))
				renderChannelPreview(renderer, channel, x, y, width, height);
		}
	}
	
	private boolean isChannelVisible(GSChannel channel, int x, int y, int width, int height) {
		int channelY = modelView.getChannelY(channel.getChannelUUID());
		if (channelY == -1)
			return false;
		
		int mappedChannelY = mapEntryY(channelY);
		return (mappedChannelY >= y && mappedChannelY < y + height);
	}
	
	private void renderChannelPreview(GSIRenderer2D renderer, GSChannel channel, int x, int y, int width, int height) {
		int color = GSIRenderer.darkenColor(channel.getInfo().getColor());
		
		for (GSChannelEntry entry : channel.getEntries()) {
			GSRectangle bounds = getMappedEntryBounds(entry);
			
			if (bounds != null && clampEntryBounds(bounds, x, y, width, height))
				renderer.fillRect(bounds.x, bounds.y, bounds.width, bounds.height, color);
		}
	}
	
	private boolean clampEntryBounds(GSRectangle bounds, int x, int y, int width, int height) {
		// Clamp left, top, right, bottom
		if (bounds.x < x) {
			bounds.width += bounds.x - x;
			bounds.x = x;
		}
		
		if (bounds.y < y) {
			bounds.height += bounds.y - y;
			bounds.y = y;
		}
		
		if (bounds.x + bounds.width > x + width)
			bounds.width -= (bounds.x + bounds.width) - (x + width);
		
		if (bounds.y + bounds.height > y + height)
			bounds.height -= (bounds.y + bounds.height) - (y + height);
		
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
	
	private GSRectangle getMappedEntryBounds(GSChannelEntry entry) {
		GSRectangle bounds = modelView.modelToView(entry, tmpEntryRect);
		return (bounds == null) ? null : mapEntryBounds(bounds);
	}
	
	private GSRectangle mapEntryBounds(GSRectangle bounds) {
		if (bounds.width <= 0 || bounds.height <= 0)
			return null;
		
		int x1 = bounds.x + bounds.width;
		int y1 = bounds.y + bounds.height;

		bounds.x = mapEntryX(bounds.x);
		bounds.y = mapEntryY(bounds.y);
		bounds.width  = Math.max(1, mapEntryX(x1) - bounds.x);
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
		
		y /= (modelView.getChannelHeight() + modelView.getChannelSpacing());

		// Ensure that we can see the top channel when vertical scroll
		// is zero and bottom channel when vertical scroll is maxScroll.
		int hiddenVerticalContent = parent.getContentHeight() - parent.getContentViewHeight();
		int hiddenChannelCount = sequence.getChannels().size() - PREVIEW_CHANNEL_COUNT;
		if (hiddenVerticalContent > 0 && hiddenChannelCount > 0)
			y += hiddenChannelCount * modelView.getYOffset() / hiddenVerticalContent;
		
		return VERTICAL_BORDER_HEIGHT + y;
	}
	
	@Override
	protected GSDimension calculatePreferredSize() {
		int w = Math.max(SCROLL_BUTTON_HEIGHT, getButtonWidth());
		int h = getMinimumNobSize() + getButtonHeight() * 2;
		return isVertical() ? new GSDimension(w, h) : new GSDimension(h, w);
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
	public void setVertical(boolean ignore) {
		// Only support horizontal
		super.setVertical(false);
	}
}
