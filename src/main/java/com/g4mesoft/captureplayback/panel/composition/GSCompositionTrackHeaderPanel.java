package com.g4mesoft.captureplayback.panel.composition;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionTrackHeaderPanel extends GSPanel {
	
	private static final int TRACK_LABEL_COLOR = 0xFF444444;
	
	private final GSComposition composition;
	private final GSCompositionModelView modelView;
	
	public GSCompositionTrackHeaderPanel(GSComposition composition, GSCompositionModelView modelView) {
		this.composition = composition;
		this.modelView = modelView;
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);

		renderer.fillRect(0, 0, width, height, GSCompositionContentPanel.BACKGROUND_COLOR);

		renderHeaders(renderer);
	}

	private void renderHeaders(GSIRenderer2D renderer) {
		int y = 0;
		for (GSTrack track : composition.getTracks()) {
			renderHeader(renderer, track, 1, y, width - 1);
			y += modelView.getTrackHeight() + modelView.getTrackSpacing();
		}
	}
	
	private void renderHeader(GSIRenderer2D renderer, GSTrack track, int x, int y, int width) {
		renderer.fillRect(x, y, width, modelView.getTrackHeight(), TRACK_LABEL_COLOR);
		renderer.drawText(track.getName(), x + 2, y + 2, renderer.brightenColor(track.getColor()), false);
	}
}
