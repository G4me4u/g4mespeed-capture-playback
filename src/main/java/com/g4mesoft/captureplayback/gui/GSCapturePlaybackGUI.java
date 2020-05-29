package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.gui.edit.GSEditTimelineGUI;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.gui.GSParentPanel;

import net.minecraft.client.gui.widget.ButtonWidget;

public class GSCapturePlaybackGUI extends GSParentPanel {

	private static final int TOP_MARGIN = 5;
	private static final int POSITION_GUI_HEIGHT = 16;

	private final GSCapturePlaybackModule module;
	
	private final GSPositionGUI positionGUI;
	
	public GSCapturePlaybackGUI(GSCapturePlaybackModule module) {
		this.module = module;
		
		positionGUI = new GSPositionGUI();
	}
	
	@Override
	public void init() {
		super.init();
		
		int y = TOP_MARGIN;
		positionGUI.initBounds(client, 0, y, width / 2, POSITION_GUI_HEIGHT);
		addPanel(positionGUI);
		y += POSITION_GUI_HEIGHT + 5;

		addWidget(new ButtonWidget(width / 2 - 45, y, 90, 20, "Edit timeline", (button) -> {
			client.openScreen(new GSEditTimelineGUI(module.getActiveTimeline(), module));
		}));
	}
}
