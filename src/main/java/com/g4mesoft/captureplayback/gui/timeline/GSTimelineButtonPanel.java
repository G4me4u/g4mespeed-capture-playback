package com.g4mesoft.captureplayback.gui.timeline;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSTimelineButtonPanel extends GSParentPanel {

	private static final int BACKGROUND_COLOR = 0xFF171717;
	
	public GSTimelineButtonPanel(GSTimeline timeline) {
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 1, width, height, BACKGROUND_COLOR);
		renderer.drawHLine(0, width, 0, 0xFF000000);
		
		super.render(renderer);
	}
}
