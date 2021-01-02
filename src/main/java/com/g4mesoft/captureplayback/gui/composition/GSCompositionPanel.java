package com.g4mesoft.captureplayback.gui.composition;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.gui.GSParentPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSCompositionPanel extends GSParentPanel {

	private final GSComposition composition;
	
	public GSCompositionPanel(GSComposition composition) {
		this.composition = composition;
	}

	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, 0xDA0A0A0A);
		
		int x = 50;
		int y = 20;
		
		drawTest(renderer, "Timeline #1", x, y, 100, 50, 0xFFBBEEAA);
		drawTest(renderer, "Timeline #2", x + 101, y, 100, 50, 0xFFAABBCC);
		drawTest(renderer, "Timeline #3", x + 222, y, 100, 50, 0xFFEE22EE);
		
		drawTest(renderer, "Timeline #3", x, y + 51, 100, 50, 0xFFEE22EE);
		drawTest(renderer, "Timeline #2", x + 101, y + 51, 100, 50, 0xFFAABBCC);

		drawTest(renderer, "Timeline #4", x, y + 102, 150, 50, 0xFFEE8822);
		drawTest(renderer, "Timeline #4", x + 151, y + 102, 150, 50, 0xFFEE8822);
		drawTest(renderer, "Timeline #4", x + 302, y + 102, 150, 50, 0xFFEE8822);
		
		drawTest(renderer, "Timeline #5", x + 50, y + 153, 300, 50, 0xFFFFFFFF);

		super.render(renderer);
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
		renderer.fillRect(x + 1, y + 11, w - 2, h - 12, darkDarkColor & 0x80FFFFFF);
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
		renderer.fillRect(x + 1, y + 1, w - 2, h - 2, darkDarkColor & 0x80FFFFFF);
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
		renderer.fillRect(x + 1, y + 6, w - 2, h - 7, darkDarkColor & 0x80FFFFFF);
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
		
		renderer.fillRect(x, y, w, 10, darkColor);
		renderer.drawText(name, x + 2, y + (10 - renderer.getTextHeight() + 1) / 2, brightColor, false);
		renderer.drawRect(x, y + 10, w, h - 10, darkColor);
		renderer.fillRect(x + 1, y + 11, w - 2, h - 12, darkDarkColor & 0x80FFFFFF);
		renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 2, 3, 5, 0xFF22EEEE);
		renderer.drawDottedHLine(x + 2, x + w - 2, y + 10 + 4, 6, 8, 0xFF22EE22);
		renderer.drawDottedHLine(x + 5, x + w - 2, y + 10 + 6, 8, 9, 0xFFEE22EE);
		renderer.drawDottedHLine(x + 3, x + w - 2, y + 10 + 8, 1, 5, 0xFFEEEE22);
	}
}
