package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.gui.edit.GSEditTimelineGUI;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.gui.text.GSTextField;

import net.minecraft.client.gui.widget.ButtonWidget;

public class GSCapturePlaybackGUI extends GSParentPanel {

	private static final int TOP_MARGIN = 5;

	private final GSCapturePlaybackModule module;

	private final GSTextField textField;
	
	public GSCapturePlaybackGUI(GSCapturePlaybackModule module) {
		this.module = module;
		
		textField = new GSTextField("Testing... :)");
	}
	
	@Override
	public void init() {
		super.init();
		
		addWidget(new ButtonWidget(width / 2 - 45, TOP_MARGIN, 90, 20, "Edit timeline", (button) -> {
			client.openScreen(new GSEditTimelineGUI(module.getActiveTimeline(), module));
		}));
		
		textField.initBounds(client, width / 2 - 100, TOP_MARGIN + 30, 200, font.fontHeight + textField.getBorderWidth() * 2);
		addPanel(textField);
	}
}
