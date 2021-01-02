package com.g4mesoft.captureplayback.gui;

import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.gui.edit.GSCompositionEditPanel;
import com.g4mesoft.captureplayback.gui.edit.GSTimelineEditPanel;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.gui.GSElementContext;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.gui.action.GSButtonPanel;

public class GSCapturePlaybackPanel extends GSParentPanel {

	private static final int TOP_MARGIN = 5;

	private GSButtonPanel editCompositionButton;
	private GSButtonPanel editTimelineButton;

	public GSCapturePlaybackPanel(GSCapturePlaybackModule module) {
		editCompositionButton = new GSButtonPanel("Edit Composition", () -> {
			GSElementContext.setContent(new GSCompositionEditPanel(new GSComposition(UUID.randomUUID(), "Test")));
		});
		editTimelineButton = new GSButtonPanel("Edit Timeline", () -> {
			GSElementContext.setContent(new GSTimelineEditPanel(module.getActiveTimeline()));
		});
		
		add(editCompositionButton);
		add(editTimelineButton);
	}
	
	@Override
	protected void onBoundsChanged() {
		super.onBoundsChanged();
		
		editCompositionButton.setPreferredBounds(width / 2 - 95, TOP_MARGIN, 90);
		editTimelineButton.setPreferredBounds(width / 2 + 5, TOP_MARGIN, 90);
	}
}
