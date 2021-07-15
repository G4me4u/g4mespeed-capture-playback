package com.g4mesoft.captureplayback.panel;

import com.g4mesoft.panel.GSColoredIcon;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSIcon;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.dropdown.GSDropdown;
import com.g4mesoft.panel.dropdown.GSDropdownAction;
import com.g4mesoft.panel.dropdown.GSDropdownSubMenu;
import com.g4mesoft.panel.scroll.GSIScrollListener;
import com.g4mesoft.panel.scroll.GSIScrollable;
import com.g4mesoft.panel.scroll.GSScrollBar;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public abstract class GSScrollableContentPanel extends GSParentPanel implements GSIScrollable {

	private static final int BOTTOM_RIGHT_CORNER_COLOR = 0xFF000000;
	
	private static final GSIcon OPACITY_SELECTED_ICON = new GSColoredIcon(0xFFFFFFFF, 4, 4);
	private static final Text OPACITY_TEXT = new TranslatableText("panel.opacity");
	
	protected GSScrollBar verticalScrollBar;
	protected GSScrollBar horizontalScrollBar;
	
	protected int contentWidth;
	protected int contentHeight;
	protected int minContentWidth;
	
	protected boolean editable;
	
	private GSEContentOpacity opacity;
	
	public GSScrollableContentPanel() {
		// Editable by default
		editable = true;
		
		// Fully opaque by default
		opacity = GSEContentOpacity.FULLY_OPAQUE;
	}
	
	protected void init() {
		verticalScrollBar = createVerticalScrollBar((newScroll) -> {
			onYOffsetChanged(-newScroll);
		});
		
		horizontalScrollBar = createHorizontalScrollBar(new GSIScrollListener() {
			@Override
			public void preScrollChanged(float newScroll) {
				contentWidth = Math.max(minContentWidth, (int)newScroll + getContentViewWidth());
			}

			@Override
			public void scrollChanged(float newScroll) {
				onXOffsetChanged(-newScroll);
			}
		});
		
		verticalScrollBar.setVertical(true);
		horizontalScrollBar.setVertical(false);
		
		add(getContent());
		add(getColumnHeader());
		add(getRowHeader());
		
		add(verticalScrollBar);
		add(horizontalScrollBar);
	}
	
	protected GSScrollBar createVerticalScrollBar(GSIScrollListener listener) {
		return new GSScrollBar(this, listener);
	}

	protected GSScrollBar createHorizontalScrollBar(GSIScrollListener listener) {
		return new GSScrollBar(this, listener);
	}
	
	protected abstract GSPanel getContent();

	protected abstract GSPanel getColumnHeader();

	protected abstract GSPanel getRowHeader();

	protected abstract int getColumnHeaderHeight();
	
	protected abstract int getRowHeaderWidth();

	@Override
	protected void onShown() {
		super.onShown();
		
		getContent().requestFocus();
	}
	
	@Override
	protected void layout() {
		super.layout();
		
		int chh = getColumnHeaderHeight();
		int rhw = getRowHeaderWidth();
	
		GSDimension vs = verticalScrollBar.getPreferredSize();
		GSDimension hs = horizontalScrollBar.getPreferredSize();
		int cw = Math.max(1, width - rhw - vs.getWidth());
		int ch = Math.max(1, height - chh - hs.getHeight());

		getContent().setBounds(rhw, chh, cw, ch);
		getColumnHeader().setBounds(rhw, 0, cw, chh);
		getRowHeader().setBounds(0, chh, rhw, ch);
		
		int sx = width - vs.getWidth();
		int sy = height - hs.getHeight();
		
		layoutTopLeft(0, 0, rhw, chh);
		layoutBottomLeft(0, sy, rhw, hs.getHeight());
		layoutTopRight(sx, 0, vs.getWidth(), chh);
		layoutBottomRight(0, sy, vs.getWidth(), hs.getHeight());
		
		verticalScrollBar.setBounds(sx, chh, vs.getWidth(), ch);
		horizontalScrollBar.setBounds(rhw, sy, cw, hs.getHeight());
	}
	
	protected void layoutTopLeft(int x, int y, int width, int height) {
	}

	protected void layoutBottomLeft(int x, int y, int width, int height) {
	}

	protected void layoutTopRight(int x, int y, int width, int height) {
	}

	protected void layoutBottomRight(int x, int y, int width, int height) {
	}
	
	@Override
	public final void render(GSIRenderer2D renderer) {
		float oldOpacity = renderer.getOpacity();
		renderer.setOpacity(opacity.getOpacity());
		renderTranslucent(renderer);
		renderer.setOpacity(oldOpacity);
	}
	
	public void renderTranslucent(GSIRenderer2D renderer) {
		super.render(renderer);

		int sw = verticalScrollBar.getWidth();
		int sh = horizontalScrollBar.getHeight();
		int cx = width - sw;
		int cy = height - sh;

		// Bottom right corner
		renderer.fillRect(cx, cy, sw, sh, BOTTOM_RIGHT_CORNER_COLOR);		
	}
	
	@Override
	public void populateRightClickMenu(GSDropdown dropdown, int x, int y) {
		GSDropdown opacityMenu = new GSDropdown();
		for (GSEContentOpacity opacity : GSEContentOpacity.OPACITIES) {
			GSIcon icon = (this.opacity == opacity) ? OPACITY_SELECTED_ICON : null;
			Text text = new TranslatableText(opacity.getName());
			opacityMenu.addItem(new GSDropdownAction(icon, text, () -> {
				setOpacity(opacity);
			}));
		}
		dropdown.addItem(new GSDropdownSubMenu(OPACITY_TEXT, opacityMenu));
	}
	
	protected float getXOffset() {
		return -horizontalScrollBar.getScrollOffset();
	}
	
	protected void setXOffset(float xOffset) {
		horizontalScrollBar.setScrollOffset(-xOffset);
	}

	protected float getYOffset() {
		return -verticalScrollBar.getScrollOffset();
	}

	protected void setYOffset(float yOffset) {
		verticalScrollBar.setScrollOffset(-yOffset);
	}
	
	protected abstract void onXOffsetChanged(float xOffset);
	
	protected abstract void onYOffsetChanged(float yOffset);
	
	public int getMinContentWidth() {
		return minContentWidth;
	}

	public void setMinContentWidth(int minContentWidth) {
		this.minContentWidth = minContentWidth;
		
		if (minContentWidth > contentWidth)
			contentWidth = minContentWidth;
	}
	
	public void setContentSize(int contentWidth, int contentHeight) {
		setMinContentWidth(Math.max(getContentViewWidth(), contentWidth));
		
		if (contentHeight > getContentViewHeight()) {
			this.contentHeight = contentHeight;
			verticalScrollBar.setEnabled(true);
		} else {
			this.contentHeight = getContentViewHeight();
			verticalScrollBar.setEnabled(false);
		}
		
		// Updating the scroll will ensure it is within bounds.
		setXOffset(getXOffset());
		setYOffset(getYOffset());
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public GSEContentOpacity getOpacity() {
		return opacity;
	}
	
	public void setOpacity(GSEContentOpacity opacity) {
		if (opacity == null)
			throw new IllegalArgumentException("opacity is null");
		this.opacity = opacity;
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
		return getContent().getWidth();
	}

	@Override
	public int getContentViewHeight() {
		return getContent().getHeight();
	}
}
