package com.g4mesoft.captureplayback.gui.timeline;

import com.g4mesoft.access.GSIBufferBuilderAccess;
import com.g4mesoft.captureplayback.gui.GSDarkScrollBar;
import com.g4mesoft.gui.GSIScrollListener;
import com.g4mesoft.gui.GSIScrollableViewport;
import com.g4mesoft.gui.GSScrollBar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

public class GSTimelineViewport implements GSIScrollableViewport, GSIScrollListener {

	private static final int CORNER_SQUARE_COLOR = 0xFF000000;
	
	private final GSScrollBar verticalScrollBar;
	private final GSScrollBar horizontalScrollBar;
	
	private int x;
	private int y;
	private int width;
	private int height;
	
	private int contentWidth;
	private int contentHeight;
	private int minContentWidth;
	private int minContentHeight;
	
	public GSTimelineViewport() {
		verticalScrollBar = new GSDarkScrollBar(true, this, null);
		horizontalScrollBar = new GSDarkScrollBar(false, this, this);
	}
	
	public void render(int mouseX, int mouseY, float partialTicks) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		double oldOffsetX = ((GSIBufferBuilderAccess)buffer).getOffsetX();
		double oldOffsetY = ((GSIBufferBuilderAccess)buffer).getOffsetY();
		double oldOffsetZ = ((GSIBufferBuilderAccess)buffer).getOffsetZ();
		
		buffer.setOffset(oldOffsetX + x, oldOffsetY + y, oldOffsetZ);
		
		if (verticalScrollBar.isEnabled())
			verticalScrollBar.render(mouseX - x, mouseY - y, partialTicks);
		horizontalScrollBar.render(mouseX - x, mouseY - y, partialTicks);

		buffer.setOffset(oldOffsetX, oldOffsetY, oldOffsetZ);

		int cx = x + width - GSScrollBar.SCROLL_BAR_WIDTH;
		int cy = y + height - GSScrollBar.SCROLL_BAR_WIDTH;
		DrawableHelper.fill(cx, cy, x + width, y + height, CORNER_SQUARE_COLOR);
	}
	
	public void setBounds(MinecraftClient client, int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;

		setMinimumContentSize(minContentWidth, minContentHeight);
		
		verticalScrollBar.init(client, 0, 0, 0, GSScrollBar.SCROLL_BAR_WIDTH);
		horizontalScrollBar.init(client, 0, GSScrollBar.SCROLL_BAR_WIDTH, 0, 0);
	}
	
	public void setMinimumContentSize(int minContentWidth, int minContentHeight) {
		this.minContentWidth = Math.max(width, minContentWidth);
		this.minContentHeight = minContentHeight + GSScrollBar.SCROLL_BAR_WIDTH;

		if (this.minContentWidth > contentWidth)
			contentWidth = this.minContentWidth;
		contentHeight = Math.max(height, this.minContentHeight);
		
		verticalScrollBar.setEnabled(this.minContentHeight >= height);
	}
	
	public int getXOffset() {
		return x - (int)horizontalScrollBar.getScrollOffset();
	}

	public int getYOffset() {
		return y - (int)verticalScrollBar.getScrollOffset();
	}
	
	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
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
	public void preScrollChanged(double newScroll) {
		contentWidth = Math.max(minContentWidth, (int)newScroll + width);
	}

	@Override
	public void scrollChanged(double newScroll) {
	}

	public void mouseScrolled(double mouseX, double mouseY, double scroll) {
		if (verticalScrollBar.isEnabled())
			verticalScrollBar.mouseScrolled(mouseX - x, mouseY - y, scroll);
		horizontalScrollBar.mouseScrolled(mouseX - x, mouseY - y, scroll);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (verticalScrollBar.isEnabled() && verticalScrollBar.mouseClicked(mouseX - x, mouseY - y, button))
			return true;
		if (horizontalScrollBar.mouseClicked(mouseX - x, mouseY - y, button))
			return true;
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (verticalScrollBar.isEnabled() && verticalScrollBar.mouseReleased(mouseX - x, mouseY - y, button))
			return true;
		if (horizontalScrollBar.mouseReleased(mouseX - x, mouseY - y, button))
			return true;
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (verticalScrollBar.isEnabled() && verticalScrollBar.mouseDragged(mouseX - x, mouseY - y, button, dragX, dragY))
			return true;
		if (horizontalScrollBar.mouseDragged(mouseX - x, mouseY - y, button, dragX, dragY))
			return true;
		return false;
	}
}
