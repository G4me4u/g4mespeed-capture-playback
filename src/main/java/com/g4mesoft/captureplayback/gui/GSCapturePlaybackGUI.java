package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.gui.edit.GSEditTimelineGUI;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.gui.GSElementContext;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.gui.action.GSButtonPanel;

public class GSCapturePlaybackGUI extends GSParentPanel {

	private static final int TOP_MARGIN = 5;

	private GSButtonPanel editButton;

	public GSCapturePlaybackGUI(GSCapturePlaybackModule module) {
		editButton = new GSButtonPanel("Edit Timeline", true, () -> {
			GSElementContext.setContent(new GSEditTimelineGUI(module.getActiveTimeline(), module));
		});
		
		add(editButton);
	}
	
	@Override
	protected void onBoundsChanged() {
		super.onBoundsChanged();
		
		editButton.setPreferredBounds(width / 2 - 45, TOP_MARGIN, 90);
	}
}
