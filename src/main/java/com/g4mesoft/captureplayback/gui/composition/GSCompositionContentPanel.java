package com.g4mesoft.captureplayback.gui.composition;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.gui.GSPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionContentPanel extends GSPanel {

	private final GSComposition composition;
	
	public GSCompositionContentPanel(GSComposition composition) {
		this.composition = composition;
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);

		renderer.pushClip(0, 0, width, height);
		
		int cx = 90;
		int cw = width - cx;
		int cy = 12;
		int ch = height - cy;
		
		renderer.fillRect(0, 0, width, 11, 0xFF222222);
		renderer.fillRect(0, 11, width, height - 11, 0xFF333333);
		
		int n = 10;
		int s = cw / n;
		int x = cx;
		
		for (int i = 0; i < n; i++) {
			renderer.drawText(Integer.toString(i * 20), x + 3, 2, 0xFF777777);
			renderer.drawVLine(x, cy, cy + ch, 0xFF222222);
			
			int ss = s / 5;
			for (int j = 1; j <= 4; j++)
				renderer.drawDottedVLine(x + j * ss, cy, cy + ch, 3, 3, 0xFF222222);
			
			x += s;
		}
		
		for (int r = 0; r < 4; r++) {
			renderer.drawHLine(cx, width, cy + (r + 1) * 51 - 1, 0xFF222222);
		}
		
		performTest(renderer, cx, cy, s);
		
		renderer.popClip();
	}
	
	private void performTest(GSIRenderer2D renderer, int x, int y, int s) {
		renderer.fillRect(1, y, 88, 50, 0xFF444444);
		renderer.drawText("Track #1", 3, y + 2, renderer.brightenColor(0xFFBBEEAA), false);
		drawTest(renderer, "Sequence #1", x,         y, 2 * s, 50, 0xFFBBEEAA);
		drawTest(renderer, "Sequence #2", x + 2 * s, y, 2 * s, 50, 0xFFAABBCC);
		drawTest(renderer, "Sequence #3", x + 4 * s, y, 2 * s, 50, 0xFFEE22EE);
		
		renderer.fillRect(1, y + 51, 88, 50, 0xFF444444);
		renderer.drawText("Track #2", 3, y + 53, renderer.brightenColor(0xFFEE22EE), false);
		drawTest(renderer, "Sequence #3", x,         y + 51, 2 * s, 50, 0xFFEE22EE);
		drawTest(renderer, "Sequence #2", x + 2 * s, y + 51, 2 * s, 50, 0xFFAABBCC);
		
		renderer.fillRect(1, y + 102, 88, 50, 0xFF444444);
		renderer.drawText("Track #3", 3, y + 104, renderer.brightenColor(0xFFEE8822), false);
		drawTest(renderer, "Sequence #4", x,         y + 102, 3 * s, 50, 0xFFEE8822);
		drawTest(renderer, "Sequence #4", x + 3 * s, y + 102, 3 * s, 50, 0xFFEE8822);
		drawTest(renderer, "Sequence #4", x + 6 * s, y + 102, 3 * s, 50, 0xFFEE8822);
		
		renderer.fillRect(1, y + 153, 88, 50, 0xFF444444);
		renderer.drawText("Track #1", 3, y + 155, renderer.brightenColor(0xFFFFFFFF), false);
		drawTest(renderer, "Sequence #5", x + s, y + 153, 6 * s, 50, 0xFFFFFFFF);
	}
	
	private void drawTest(GSIRenderer2D renderer, String name, int x, int y, int w, int h, int color) {
		
		drawTest4(renderer, name, x, y, w, h, color);
	}
	
	private void drawTest1(GSIRenderer2D renderer, String name, int x, int y, int w, int h, int color) {
		int brightColor = renderer.brightenColor(color);
		int darkColor = renderer.darkenColor(color);
		int darkDarkColor = renderer.darkenColor(darkColor);
		
		renderer.fillRect(x, y, w, 10, darkColor);
		renderer.drawCenteredText(name, x + w / 2, y + (10 - renderer.getTextHeight() + 1) / 2, brightColor, false);
		renderer.drawRect(x, y + 10, w, h - 10, darkColor);
		renderer.fillRect(x + 1, y + 11, w - 2, h - 12, darkDarkColor & 0xA0FFFFFF);
		renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 2, 3, 5, 0xFF22EEEE);
		renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 4, 6, 8, 0xFF22EE22);
		renderer.drawDottedHLine(x + 5, x + w - 2, y + 10 + 6, 8, 9, 0xFFEE22EE);
		renderer.drawDottedHLine(x + 3, x + w - 2, y + 10 + 8, 1, 5, 0xFFEEEE22);
	}
	
	private void drawTest2(GSIRenderer2D renderer, String name, int x, int y, int w, int h, int color) {
		int brightColor = renderer.brightenColor(color);
		int darkColor = renderer.darkenColor(color);
		int darkDarkColor = renderer.darkenColor(darkColor);
		
		renderer.drawRect(x, y, w, h, darkColor);
		renderer.fillRect(x + 1, y + 1, w - 2, h - 2, darkDarkColor & 0xA0FFFFFF);
		renderer.fillRect(x, y, Math.round(renderer.getTextWidth(name)) + 4, 10, darkColor);
		renderer.drawText(name, x + 2, y + (10 - renderer.getTextHeight() + 1) / 2, brightColor, false);
		renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 2, 3, 5, 0xFF22EEEE);
		renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 4, 6, 8, 0xFF22EE22);
		renderer.drawDottedHLine(x + 5, x + w - 2, y + 10 + 6, 8, 9, 0xFFEE22EE);
		renderer.drawDottedHLine(x + 3, x + w - 2, y + 10 + 8, 1, 5, 0xFFEEEE22);
	}

	private void drawTest3(GSIRenderer2D renderer, String name, int x, int y, int w, int h, int color) {
		int brightColor = renderer.brightenColor(color);
		int darkColor = renderer.darkenColor(color);
		int darkDarkColor = renderer.darkenColor(darkColor);
		
		renderer.drawRect(x, y + 5, w, h - 5, darkColor);
		renderer.fillRect(x + 1, y + 6, w - 2, h - 7, darkDarkColor & 0xA0FFFFFF);
		renderer.fillRect(x, y, Math.round(renderer.getTextWidth(name)) + 4, 10, darkColor);
		renderer.drawText(name, x + 2, y + (10 - renderer.getTextHeight() + 1) / 2, brightColor, false);
		renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 2, 3, 5, 0xFF22EEEE);
		renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 4, 6, 8, 0xFF22EE22);
		renderer.drawDottedHLine(x + 5, x + w - 2, y + 10 + 6, 8, 9, 0xFFEE22EE);
		renderer.drawDottedHLine(x + 3, x + w - 2, y + 10 + 8, 1, 5, 0xFFEEEE22);
	}
	
	private void drawTest4(GSIRenderer2D renderer, String name, int x, int y, int w, int h, int color) {
		int brightColor = renderer.brightenColor(color);
		int darkColor = renderer.darkenColor(color);
		int darkDarkColor = renderer.darkenColor(darkColor);
		
		renderer.fillRect(x, y, w, 10, darkColor & 0xE0FFFFFF);
		renderer.drawText(renderer.trimString(name, w - 4), x + 2, y + (10 - renderer.getTextHeight() + 1) / 2, brightColor, false);
		//renderer.drawRect(x, y + 10, w, h - 10, darkColor);
		renderer.fillRect(x, y + 10, w, h - 10, darkDarkColor & 0x80FFFFFF);
		
		if (false) {
			renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 2, 3, 5, 0xFF22EEEE);
			renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 4, 6, 8, 0xFF22EE22);
			renderer.drawDottedHLine(x + 5, x + w - 2, y + 10 + 6, 8, 9, 0xFFEE22EE);
			renderer.drawDottedHLine(x + 3, x + w - 2, y + 10 + 8, 1, 5, 0xFFEEEE22);
		} else {
			renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 2, 3, 5, darkColor & 0xE0FFFFFF);
			renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 4, 6, 8, darkColor & 0xE0FFFFFF);
			renderer.drawDottedHLine(x + 5, x + w - 2, y + 10 + 6, 8, 9, darkColor & 0xE0FFFFFF);
			renderer.drawDottedHLine(x + 3, x + w - 2, y + 10 + 8, 1, 5, darkColor & 0xE0FFFFFF);
		}
	}
}
