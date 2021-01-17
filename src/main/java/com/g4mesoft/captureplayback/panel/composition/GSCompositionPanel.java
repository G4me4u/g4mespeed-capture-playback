package com.g4mesoft.captureplayback.panel.composition;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionPanel extends GSParentPanel {

	private static final int MIN_CONTENT_HEIGHT = 150;
	
	private final GSComposition composition;
	
	private final GSCompositionContentPanel compositionContent;
	
	public GSCompositionPanel(GSComposition composition) {
		this.composition = composition;
		
		compositionContent = new GSCompositionContentPanel(composition);
	
		add(compositionContent);
	}

	@Override
	protected void onBoundsChanged() {
		super.onBoundsChanged();
	
		int contentHeight = Math.min(Math.max(height / 2, MIN_CONTENT_HEIGHT), height);
		compositionContent.setBounds(0, height - contentHeight, width, contentHeight);
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, 0xDA0A0A0A);

		super.render(renderer);
	}
}
