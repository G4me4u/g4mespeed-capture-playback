package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.gui.renderer.GSTexture;
import com.g4mesoft.gui.scroll.GSIScrollListener;
import com.g4mesoft.gui.scroll.GSIScrollableViewport;
import com.g4mesoft.gui.scroll.GSScrollBar;

import net.minecraft.util.Identifier;

public class GSDarkScrollBar extends GSScrollBar {

	private static final Identifier DARK_TEXTURE_IDENTIFIER = new Identifier("g4mespeed/captureplayback/textures/scroll_bar_dark.png");
	private static final GSTexture DARK_TEXTURE = new GSTexture(DARK_TEXTURE_IDENTIFIER, 30, 40);
	
	private static final int DARK_KNOB_AREA_COLOR = 0xFF171717;
	private static final int DARK_DISABLED_KNOB_AREA_COLOR = 0xFF000000;
	
	private static final int DARK_KNOB_COLOR = 0xFF4D4D4D;
	private static final int DARK_HOVERED_KNOB_COLOR = 0xFF7A7A7A;
	private static final int DARK_DISABLED_KNOB_COLOR = 0xFF2B2A2B;
	
	public GSDarkScrollBar(GSIScrollableViewport parent, GSIScrollListener listener) {
		super(parent, listener);
	}

	@Override
	protected GSTexture getScrollButtonTexture() {
		return DARK_TEXTURE;
	}
	
	@Override
	protected int getKnobAreaColor() {
		if (!isEnabled())
			return DARK_DISABLED_KNOB_AREA_COLOR;
		return DARK_KNOB_AREA_COLOR;
	}
	
	@Override
	protected int getKnobColor(boolean hovered) {
		if (!isEnabled())
			return DARK_DISABLED_KNOB_COLOR;
		if (isScrollDragActive() || hovered)
			return DARK_HOVERED_KNOB_COLOR;
		return DARK_KNOB_COLOR;
	}
}
