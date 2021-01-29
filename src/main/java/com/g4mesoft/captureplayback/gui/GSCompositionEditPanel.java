package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.panel.composition.GSCompositionPanel;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionEditPanel extends GSAbstractEditPanel {

	private static final int MIN_CONTENT_HEIGHT = 150;
	
	private final GSComposition composition;
	private final GSCompositionPanel contentPanel;
	
	public GSCompositionEditPanel(GSComposition composition) {
		this.composition = composition;

		contentPanel = new GSCompositionPanel(composition);
		add(contentPanel);
		
		nameField.setText(composition.getName());
	}

	@Override
	public void onAdded(GSPanel parent) {
		super.onAdded(parent);

		contentPanel.requestFocus();
	}
	
	@Override
	protected void handleNameChanged(String name) {
		composition.setName(name);
	}
	
	@Override
	protected void layoutContent(int x, int y, int width, int height) {
		int contentHeight = Math.min(Math.max(height / 2, MIN_CONTENT_HEIGHT), height);
		contentPanel.setBounds(x, y + height - contentHeight, width, contentHeight);
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, 0xDA0A0A0A);
		
		super.render(renderer);
	}
}
