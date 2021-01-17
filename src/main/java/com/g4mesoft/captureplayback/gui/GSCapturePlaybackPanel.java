package com.g4mesoft.captureplayback.gui;

import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.panel.GSPanelContext;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.legend.GSButtonPanel;

public class GSCapturePlaybackPanel extends GSParentPanel {

	private static final int TOP_MARGIN = 5;

	private GSButtonPanel editCompositionButton;
	private GSButtonPanel editSequenceButton;

	public GSCapturePlaybackPanel(GSCapturePlaybackModule module) {
		editCompositionButton = new GSButtonPanel("Edit Composition", () -> {
			GSPanelContext.setContent(new GSCompositionEditPanel(new GSComposition(UUID.randomUUID(), "Test")));
		});
		editSequenceButton = new GSButtonPanel("Edit Sequence", () -> {
			GSPanelContext.setContent(new GSSequenceEditPanel(module.getActiveSequence()));
		});
		
		add(editCompositionButton);
		add(editSequenceButton);
	}
	
	@Override
	protected void onBoundsChanged() {
		super.onBoundsChanged();
		
		editCompositionButton.setPreferredBounds(width / 2 - 95, TOP_MARGIN, 90);
		editSequenceButton.setPreferredBounds(width / 2 + 5, TOP_MARGIN, 90);
	}
}
