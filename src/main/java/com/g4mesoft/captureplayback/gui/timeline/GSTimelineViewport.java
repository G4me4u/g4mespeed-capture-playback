package com.g4mesoft.captureplayback.gui.timeline;

import com.g4mesoft.access.GSIBufferBuilderAccess;
import com.g4mesoft.gui.GSIScrollListener;
import com.g4mesoft.gui.GSIScrollableViewport;
import com.g4mesoft.gui.GSScrollBar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;

public class GSTimelineViewport implements GSIScrollableViewport, GSIScrollListener {

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
		verticalScrollBar = new GSScrollBar(true, this, null);
		horizontalScrollBar = new GSScrollBar(false, this, this);
		
		verticalScrollBar.setDarkMode(true);
		horizontalScrollBar.setDarkMode(true);
	}
	
	public void render(int mouseX, int mouseY, float partialTicks) {
		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		GSIBufferBuilderAccess bufferAccess = (GSIBufferBuilderAccess)buffer;
		double oldOffsetX = bufferAccess.getOffsetX();
		double oldOffsetY = bufferAccess.getOffsetY();
		double oldOffsetZ = bufferAccess.getOffsetZ();
		
		buffer.setOffset(oldOffsetX + x, oldOffsetY + y, oldOffsetZ);
		verticalScrollBar.render(mouseX - x, mouseY - y, partialTicks);
		horizontalScrollBar.render(mouseX - x, mouseY - y, partialTicks);
		buffer.setOffset(oldOffsetX, oldOffsetY, oldOffsetZ);
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
		this.minContentHeight = minContentHeight;

		if (this.minContentWidth > contentWidth)
			contentWidth = this.minContentWidth;
		contentHeight = Math.max(height, minContentHeight);
		
		verticalScrollBar.setEnabled(minContentHeight >= height);
	}
	
	public int getX() {
		return x - (int)horizontalScrollBar.getScrollOffset();
	}

	public int getY() {
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
		verticalScrollBar.mouseScrolled(mouseX - x, mouseY - y, scroll);
		horizontalScrollBar.mouseScrolled(mouseX - x, mouseY - y, scroll);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (verticalScrollBar.mouseClicked(mouseX - x, mouseY - y, button))
			return true;
		if (horizontalScrollBar.mouseClicked(mouseX - x, mouseY - y, button))
			return true;
		return false;
	}

	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (verticalScrollBar.mouseReleased(mouseX - x, mouseY - y, button))
			return true;
		if (horizontalScrollBar.mouseReleased(mouseX - x, mouseY - y, button))
			return true;
		return false;
	}

	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (verticalScrollBar.mouseDragged(mouseX - x, mouseY - y, button, dragX, dragY))
			return true;
		if (horizontalScrollBar.mouseDragged(mouseX - x, mouseY - y, button, dragX, dragY))
			return true;
		return false;
	}
}
