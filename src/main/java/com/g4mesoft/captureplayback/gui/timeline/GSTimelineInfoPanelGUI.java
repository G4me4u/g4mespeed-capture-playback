package com.g4mesoft.captureplayback.gui.timeline;

import com.g4mesoft.gui.GSPanel;

public class GSTimelineInfoPanelGUI extends GSPanel {

	private static final int INFO_TEXT_COLOR = 0xFFFFFFFF;
	
	private String infoText;
	
	@Override
	public void renderTranslated(int mouseX, int mouseY, float partialTicks) {
		super.renderTranslated(mouseX, mouseY, partialTicks);
		
		fill(0, 0, width, height, GSTimelineTrackHeaderGUI.TRACK_HEADER_COLOR);

		fill(width - 1, 0, width, height, GSTimelineColumnHeaderGUI.COLUMN_LINE_COLOR);
		fill(0, height - 1, width, height, GSTimelineTrackHeaderGUI.TRACK_SPACING_COLOR);
		
		if (infoText != null)
			drawCenteredString(font, infoText, width / 2, (height - font.fontHeight) / 2, INFO_TEXT_COLOR);
	}
	
	public void setInfoText(String infoText) {
		this.infoText = infoText;
	}
	
	public String getInfoText() {
		return infoText;
	}
}
