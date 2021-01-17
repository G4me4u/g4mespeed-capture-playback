package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionPanel;

public class GSCompositionEditPanel extends GSAbstractEditPanel {

	private final GSComposition composition;
	
	public GSCompositionEditPanel(GSComposition composition) {
		super(new GSCompositionPanel(composition));

		this.composition = composition;

		nameField.setText(composition.getName());
	}

	@Override
	protected void handleNameChanged(String name) {
		composition.setName(name);
	}
}
