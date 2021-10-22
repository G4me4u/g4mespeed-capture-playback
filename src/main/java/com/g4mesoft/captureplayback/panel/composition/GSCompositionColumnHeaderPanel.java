package com.g4mesoft.captureplayback.panel.composition;

import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.panel.scroll.GSIScrollable;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionColumnHeaderPanel extends GSPanel implements GSIScrollable, GSIModelViewListener {

	private static final int COLUMN_HEADER_PREFERRED_HEIGHT = 12;
	public static final int BACKGROUND_COLOR = 0xFF222222;
	public static final int TIME_INDICATOR_COLOR = 0xFF777777;
	
	private final GSCompositionModelView modelView;
	
	public GSCompositionColumnHeaderPanel(GSCompositionModelView modelView) {
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
		renderTimeIndicators(renderer, bounds);
		
	}
	
	private void renderBackground(GSIRenderer2D renderer, GSRectangle bounds) {
		renderer.fillRect(bounds.x, bounds.y, bounds.width, bounds.height, BACKGROUND_COLOR);
	}
	
	private void renderTimeIndicators(GSIRenderer2D renderer, GSRectangle bounds) {
		long interval = modelView.getTimeIndicatorInterval();

		long gt = modelView.getTimeIndicatorFromX(bounds.x);
		int x = modelView.getGametickX(gt);
		
		while (x - bounds.x < bounds.width) {
			renderer.drawTextNoStyle(Long.toString(gt), x + 3, 2, TIME_INDICATOR_COLOR);
			gt += interval;
			x = modelView.getGametickX(gt);
		}
	}
	
	@Override
	protected GSDimension calculatePreferredSize() {
		return new GSDimension(modelView.getMinimumWidth(), COLUMN_HEADER_PREFERRED_HEIGHT);
	}
	
	@Override
	public boolean isScrollableWidthFixed() {
		return true;
	}
	
	@Override
	public void modelViewChanged() {
		// Preferred size might have changed.
		invalidate();
		// Request layout from parent (GSViewport)
		invalidateParent();
	}
}
