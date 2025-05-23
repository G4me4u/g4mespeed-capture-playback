package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.renderer.GSIRenderer2D;

public class GSSequenceButtonPanel extends GSParentPanel {

	private static final int BACKGROUND_COLOR = 0xFF171717;
	
	public GSSequenceButtonPanel() {
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 1, width, height, BACKGROUND_COLOR);
		renderer.drawHLine(0, width, 0, 0xFF060606);
		
		super.render(renderer);
	}
}
