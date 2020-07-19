package com.g4mesoft.captureplayback.gui.timeline;

import com.g4mesoft.gui.GSPanel;
import com.g4mesoft.gui.renderer.GSIRenderer2D;

public class GSTimelineInfoPanelGUI extends GSPanel {

	private static final int INFO_TEXT_COLOR = 0xFFFFFFFF;
	
	private String infoText;
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);
		
		renderer.fillRect(0, 0, width, height, GSTimelineTrackHeaderGUI.TRACK_HEADER_COLOR);

		renderer.drawVLine(width - 1, 0, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
		renderer.drawHLine(0, width, height - 1, GSTimelineTrackHeaderGUI.TRACK_SPACING_COLOR);
		
		if (infoText != null) {
			int ty = (height - renderer.getFontHeight() + 1) / 2;
			renderer.drawCenteredString(infoText, width / 2, ty, INFO_TEXT_COLOR);
		}
	}
	
	public void setInfoText(String infoText) {
		this.infoText = infoText;
	}
	
	public String getInfoText() {
		return infoText;
	}
}
