package com.g4mesoft.captureplayback.panel.composition;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionPanel extends GSParentPanel implements GSICompositionListener {

	private static final int TRACK_HEADER_WIDTH = 90;
	private static final int COLUMN_HEADER_HEIGHT = 12;
	
	private final GSComposition composition;
	
	private final GSCompositionModelView modelView;
	private final GSCompositionContentPanel content;
	private final GSCompositionColumnHeaderPanel columnHeader;
	private final GSCompositionTrackHeaderPanel trackHeader;
	
	public GSCompositionPanel(GSComposition composition) {
		this.composition = composition;
		
		modelView = new GSCompositionModelView(composition);
		content = new GSCompositionContentPanel(composition, modelView);
		columnHeader = new GSCompositionColumnHeaderPanel(modelView);
		trackHeader = new GSCompositionTrackHeaderPanel(composition, modelView);
	
		add(content);
		add(columnHeader);
		add(trackHeader);
	}
	
	@Override
	protected void onShown() {
		super.onShown();
		
		composition.addCompositionListener(this);
		
		modelView.installListeners();
		modelView.updateModelView();
	}
	
	@Override
	protected void onHidden() {
		super.onHidden();

		composition.removeCompositionListener(this);

		modelView.uninstallListeners();
	}

	@Override
	protected void onBoundsChanged() {
		super.onBoundsChanged();
	
		layoutPanels();
		
		if (isVisible())
			modelView.updateModelView();
	}
	
	private void layoutPanels() {
		int cw = Math.max(width - TRACK_HEADER_WIDTH, 0);
		int ch = Math.max(height - COLUMN_HEADER_HEIGHT, 0);
		
		content.setBounds(TRACK_HEADER_WIDTH, COLUMN_HEADER_HEIGHT, cw, ch);
		columnHeader.setBounds(TRACK_HEADER_WIDTH, 0, cw, COLUMN_HEADER_HEIGHT);
		trackHeader.setBounds(0, COLUMN_HEADER_HEIGHT, TRACK_HEADER_WIDTH, ch);
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);

		// Top left corner
		renderer.fillRect(0, 0, TRACK_HEADER_WIDTH, COLUMN_HEADER_HEIGHT - 1, 
				GSCompositionColumnHeaderPanel.BACKGROUND_COLOR);
		renderer.drawHLine(0, TRACK_HEADER_WIDTH, COLUMN_HEADER_HEIGHT - 1, 
				GSCompositionContentPanel.BACKGROUND_COLOR);
	}
}
