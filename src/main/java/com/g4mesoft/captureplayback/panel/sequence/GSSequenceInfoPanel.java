package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSSequenceInfoPanel extends GSPanel {

	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);
		
		renderer.fillRect(0, 0, width, height, GSChannelHeaderPanel.CHANNEL_HEADER_COLOR);

		renderer.drawVLine(width - 1, 0, height, GSSequenceColumnHeaderPanel.COLUMN_LINE_COLOR);
		renderer.drawHLine(0, width, height - 1, GSSequenceContentPanel.CHANNEL_SPACING_COLOR);
	}
}
