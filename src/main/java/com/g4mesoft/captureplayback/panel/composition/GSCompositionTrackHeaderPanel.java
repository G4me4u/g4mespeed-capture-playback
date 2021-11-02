package com.g4mesoft.captureplayback.panel.composition;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.panel.scroll.GSIScrollable;
import com.g4mesoft.renderer.GSIRenderer;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionTrackHeaderPanel extends GSPanel implements GSIScrollable, GSIModelViewListener {
	
	private static final int TRACK_HEADER_PREFERRED_WIDTH = 90;
	private static final int TRACK_LABEL_COLOR = 0xFF444444;
	
	private final GSComposition composition;
	private final GSCompositionModelView modelView;
	
	public GSCompositionTrackHeaderPanel(GSComposition composition, GSCompositionModelView modelView) {
		this.composition = composition;
		this.modelView = modelView;
	}
	
	@Override
	protected void onShown() {
		super.onShown();

		modelView.addModelViewListener(this);
	}

	@Override
	protected void onHidden() {
		super.onHidden();
		
		modelView.removeModelViewListener(this);
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);
		
		GSRectangle bounds = renderer.getClipBounds()
				.intersection(0, 0, width, height);

		renderBackground(renderer, bounds);
		renderHeaders(renderer, bounds);
	}
	
	private void renderBackground(GSIRenderer2D renderer, GSRectangle bounds) {
		renderer.fillRect(bounds.x, bounds.y, bounds.width, bounds.height, GSCompositionPanel.BACKGROUND_COLOR);
	}

	private void renderHeaders(GSIRenderer2D renderer, GSRectangle bounds) {
		for (GSTrack track : composition.getTracks())
			renderHeader(renderer, track, 1, width - 1, bounds);
	}
	
	private void renderHeader(GSIRenderer2D renderer, GSTrack track, int x, int width, GSRectangle bounds) {
		int y = modelView.getTrackY(track.getTrackUUID());
		if (y + modelView.getTrackHeight() >= bounds.y && y - bounds.y < bounds.height) {
			renderer.fillRect(x, y, width, modelView.getTrackHeight(), TRACK_LABEL_COLOR);
			renderer.drawTextNoStyle(track.getName(), x + 2, y + 2, GSIRenderer.brightenColor(track.getColor()), false);
		}
	}
	
	@Override
	protected GSDimension calculatePreferredSize() {
		return new GSDimension(TRACK_HEADER_PREFERRED_WIDTH, modelView.getMinimumHeight());
	}
	
	public void setEditable(boolean editable) {
		// TODO: implement track header actions.
	}
	
	@Override
	public boolean isScrollableHeightFilled() {
		return true;
	}
	
	@Override
	public void modelViewChanged() {
		invalidate();
	}
}
