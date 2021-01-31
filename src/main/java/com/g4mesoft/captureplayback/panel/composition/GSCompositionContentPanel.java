package com.g4mesoft.captureplayback.panel.composition;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionContentPanel extends GSPanel {

	public static final int BACKGROUND_COLOR    = 0xFF333333;
	public static final int TRACK_SPACING_COLOR = 0xFF222222;
	
	private final GSComposition composition;
	private final GSCompositionModelView modelView;
	
	public GSCompositionContentPanel(GSComposition composition, GSCompositionModelView modelView) {
		this.composition = composition;
		this.modelView = modelView;
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);

		renderer.pushClip(0, 0, width, height);
		
		renderer.fillRect(0, 0, width, height, BACKGROUND_COLOR);
		
		int s = 10;
		for (long gt = 0L; gt < 120; gt += s) {
			int x = modelView.getGametickX(gt);
			renderer.drawVLine(x, 0, height, 0xFF222222);
			
			int ss = s / 5;
			for (int j = ss; j < s; j += ss) {
				int xx = modelView.getGametickX(gt + j);
				renderer.drawDottedVLine(xx, 0, height, 3, 3, 0xFF222222);
			}
		}
		
		for (GSTrack track : composition.getTracks())
			renderTrack(renderer, track);
		
		renderer.popClip();
	}
	
	private void renderTrack(GSIRenderer2D renderer, GSTrack track) {
		for (GSTrackEntry entry : track.getEntries())
			drawTest(renderer, entry, track.getColor());
		
		int sy = modelView.getTrackY(track.getTrackUUID()) + modelView.getTrackHeight();
		renderer.fillRect(0, sy, width, modelView.getTrackSpacing(), TRACK_SPACING_COLOR);
	}
	
	/*
	private void performTest(GSIRenderer2D renderer, int x, int y, int s) {
		drawTest(renderer, "Sequence #1", x,         y, 2 * s, 50, 0xFFBBEEAA);
		drawTest(renderer, "Sequence #3", x + 4 * s, y, 2 * s, 50, 0xFFEE22EE);
		
		drawTest(renderer, "Sequence #3", x,         y + 51, 2 * s, 50, 0xFFEE22EE);
		drawTest(renderer, "Sequence #2", x + 2 * s, y + 51, 2 * s, 50, 0xFFAABBCC);
		
		drawTest(renderer, "Sequence #4", x,         y + 102, 3 * s, 50, 0xFFEE8822);
		drawTest(renderer, "Sequence #4", x + 3 * s, y + 102, 3 * s, 50, 0xFFEE8822);
		drawTest(renderer, "Sequence #4", x + 6 * s, y + 102, 3 * s, 50, 0xFFEE8822);
		
		drawTest(renderer, "Sequence #5", x + s, y + 153, 6 * s, 50, 0xFFFFFFFF);
	}*/
	
	private void drawTest(GSIRenderer2D renderer, GSTrackEntry entry, int color) {
		GSSequence sequence = composition.getSequence(entry.getSequenceUUID());
		GSRectangle bounds = modelView.viewToModel(entry);
		
		if (sequence != null && bounds != null) {
			int brightColor = renderer.brightenColor(color);
			int darkColor = renderer.darkenColor(color);
			int darkDarkColor = renderer.darkenColor(darkColor);
			
			int x = bounds.x;
			int y = bounds.y;
			int w = bounds.width;
			int h = bounds.height;
			String name = renderer.trimString(sequence.getName(), w - 4);
			
			renderer.fillRect(x, y, w, 10, darkColor & 0xE0FFFFFF);
			renderer.drawText(name, x + 2, y + (10 - renderer.getTextHeight() + 1) / 2, brightColor, false);
			renderer.fillRect(x, y + 10, w, h - 10, darkDarkColor & 0x80FFFFFF);
	
			renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 2, 3, 5, darkColor & 0xE0FFFFFF);
			renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 4, 6, 8, darkColor & 0xE0FFFFFF);
			renderer.drawDottedHLine(x + 5, x + w - 2, y + 10 + 6, 8, 9, darkColor & 0xE0FFFFFF);
			renderer.drawDottedHLine(x + 3, x + w - 2, y + 10 + 8, 1, 5, darkColor & 0xE0FFFFFF);
		}
	}
}
