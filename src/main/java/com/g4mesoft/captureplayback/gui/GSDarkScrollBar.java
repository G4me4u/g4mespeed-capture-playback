package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.gui.GSIScrollListener;
import com.g4mesoft.gui.GSIScrollableViewport;
import com.g4mesoft.gui.GSScrollBar;

import net.minecraft.util.Identifier;

public class GSDarkScrollBar extends GSScrollBar {

	private static final Identifier DARK_TEXTURE = new Identifier("g4mespeed/captureplayback/textures/scroll_bar_dark.png");
	
	private static final int DARK_KNOB_AREA_COLOR = 0xFF171717;
	private static final int DARK_DISABLED_KNOB_AREA_COLOR = 0xFF000000;
	
	private static final int DARK_KNOB_COLOR = 0xFF4D4D4D;
	private static final int DARK_HOVERED_KNOB_COLOR = 0xFF7A7A7A;
	private static final int DARK_DISABLED_KNOB_COLOR = 0xFF2B2A2B;
	
	public GSDarkScrollBar(boolean vertical, GSIScrollableViewport parent, GSIScrollListener listener) {
		super(vertical, parent, listener);
	}

	@Override
	protected Identifier getScrollButtonTexture() {
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
