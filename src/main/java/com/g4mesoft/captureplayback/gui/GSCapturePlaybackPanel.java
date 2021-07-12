package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.module.GSCompositionSession;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.gui.GSMainGUI;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.legend.GSButtonPanel;

public class GSCapturePlaybackPanel extends GSParentPanel {

	private static final int TOP_MARGIN = 5;

	private GSButtonPanel editCompositionButton;

	public GSCapturePlaybackPanel(GSCapturePlaybackClientModule module) {
		editCompositionButton = new GSButtonPanel("Edit Composition", () -> {
			GSMainGUI mainGUI = GSClientController.getInstance().getMainGUI();
			
			GSCompositionSession session = module.getCompositionSession();
			GSComposition composition = module.getSessionComposition();
			
			if (session != null && composition != null)
				mainGUI.setContent(new GSCompositionEditPanel(module, session, composition));
		});
		
		add(editCompositionButton);
	}
	
	@Override
	protected void layout() {
		editCompositionButton.setPreferredBounds(width / 2 - 45, TOP_MARGIN, 90);
	}
}
