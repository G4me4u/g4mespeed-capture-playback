package com.g4mesoft.captureplayback.panel.composition;

import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionColumnHeaderPanel extends GSPanel {

	public static final int BACKGROUND_COLOR = 0xFF222222;
	
	private final GSCompositionModelView modelView;
	
	public GSCompositionColumnHeaderPanel(GSCompositionModelView modelView) {
		this.modelView = modelView;
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);
		
		renderer.fillRect(0, 0, width, height - 1, BACKGROUND_COLOR);
		renderer.drawHLine(0, width, height - 1, GSCompositionContentPanel.BACKGROUND_COLOR);
		
		for (long gt = 0; gt < 120; gt += 10) {
			int x = modelView.getGametickX(gt);
			renderer.drawText(Long.toString(gt), x + 3, 2, 0xFF777777);
		}
	}
}
