package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionPanel;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.panel.GSPanel;

public class GSCompositionEditPanel extends GSAbstractEditPanel implements GSICompositionListener {

	private final GSComposition composition;
	
	private final GSCompositionPanel contentPanel;

	public GSCompositionEditPanel(GSSession session) {
		if (session.getType() != GSESessionType.COMPOSITION)
			throw new IllegalArgumentException("Session is not of type composition");
		
		this.composition = session.get(GSSession.COMPOSITION);

		contentPanel = new GSCompositionPanel(session);
		add(contentPanel);
		
		nameField.setText(composition.getName());
	}

	@Override
	public void onAdded(GSPanel parent) {
		super.onAdded(parent);

		contentPanel.requestFocus();
	}
	
	@Override
	protected void onShown() {
		super.onShown();

		composition.addCompositionListener(this);
	}

	@Override
	protected void onHidden() {
		super.onHidden();
		
		composition.removeCompositionListener(this);
	}

	@Override
	protected void layoutContent(int x, int y, int width, int height) {
		contentPanel.setBounds(x, y, width, height);
	}
	
	@Override
	protected void handleNameChanged(String name) {
		boolean visible = isVisible();
		try {
			if (visible)
				composition.removeCompositionListener(this);
			composition.setName(name);
		} finally {
			if (visible)
				composition.addCompositionListener(this);
		}
	}

	@Override
	public void compositionNameChanged(String oldName) {
		nameField.setText(composition.getName());
	}
}
