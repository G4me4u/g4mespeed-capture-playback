package com.g4mesoft.captureplayback.gui.sequence;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSSequenceButtonPanel extends GSParentPanel {

	private static final int BACKGROUND_COLOR = 0xFF171717;
	
	public GSSequenceButtonPanel(GSSequence sequence) {
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 1, width, height, BACKGROUND_COLOR);
		renderer.drawHLine(0, width, 0, 0xFF000000);
		
		super.render(renderer);
	}
}
