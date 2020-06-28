package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.gui.edit.GSEditTimelineGUI;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.gui.GSParentPanel;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;

public class GSCapturePlaybackGUI extends GSParentPanel {

	private static final int TOP_MARGIN = 5;

	private final GSCapturePlaybackModule module;
	
	public GSCapturePlaybackGUI(GSCapturePlaybackModule module) {
		this.module = module;
	}
	
	@Override
	public void init() {
		super.init();
		
		addWidget(new ButtonWidget(width / 2 - 45, TOP_MARGIN, 90, 20, new LiteralText("Edit Timeline"), (button) -> {
			client.openScreen(new GSEditTimelineGUI(module.getActiveTimeline(), module));
		}));
	}
}
