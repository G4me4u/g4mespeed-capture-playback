package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.module.GSCompositionSession;
import com.g4mesoft.captureplayback.module.client.GSCapturePlaybackClientModule;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionPanel;
import com.g4mesoft.panel.GSPanel;

public class GSCompositionEditPanel extends GSAbstractEditPanel implements GSICompositionListener {

	private final GSCompositionSession session;
	private final GSComposition composition;
	private final GSCapturePlaybackClientModule module;
	
	private final GSCompositionPanel contentPanel;

	public GSCompositionEditPanel(GSCapturePlaybackClientModule module, GSCompositionSession session, GSComposition composition) {
		this.session = session;
		this.composition = composition;
		this.module = module;

		contentPanel = new GSCompositionPanel(session, composition);
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
		
		module.onCompositionSessionChanged(session);
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
