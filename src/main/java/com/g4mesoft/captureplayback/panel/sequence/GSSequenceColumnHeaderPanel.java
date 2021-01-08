package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSSequenceColumnHeaderPanel extends GSPanel implements GSIMouseListener {

	public static final int COLUMN_HEADER_COLOR = 0xFF202020;
	public static final int HEADER_TEXT_COLOR = 0xFFFFFFFF;
	public static final int DARK_HEADER_TEXT_COLOR = 0xFFB2B2B2;
	
	public static final int COLUMN_LINE_COLOR = 0xFF202020;
	public static final int MT_COLUMN_LINE_COLOR = 0xFF404040;
	
	public static final int DOTTED_LINE_LENGTH = 4;
	public static final int DOTTED_LINE_SPACING = 3;
	
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSSequenceModelView modelView;
	
	public GSSequenceColumnHeaderPanel(GSSequence sequence, GSExpandedColumnModel expandedColumnModel, GSSequenceModelView modelView) {
		this.expandedColumnModel = expandedColumnModel;
		this.modelView = modelView;
		
		addMouseEventListener(this);
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		super.render(renderer);
	
		renderer.fillRect(0, 0, width, height, COLUMN_HEADER_COLOR);
		
		renderColumnHeaders(renderer);

		renderer.drawHLine(0, width, height - 1, GSSequenceContentPanel.CHANNEL_SPACING_COLOR);
	}
	
	private void renderColumnHeaders(GSIRenderer2D renderer) {
		renderer.pushClip(0, 0, width, height);
		
		int columnStart = Math.max(0, modelView.getColumnIndexFromView(0));
		int columnEnd = modelView.getColumnIndexFromView(width - 1);

		int cx = modelView.getColumnX(columnStart);
		for (int columnIndex = columnStart; columnIndex <= columnEnd; columnIndex++) {
			int cw = modelView.getColumnWidth(columnIndex);
			renderColumnHeader(renderer, columnIndex, cx, cw);
			cx += cw;
		}

		renderer.popClip();
	}
	
	private void renderColumnHeader(GSIRenderer2D renderer, int columnIndex, int cx, int cw) {
		boolean expanded = expandedColumnModel.isColumnExpanded(columnIndex);
		
		int ty = (height / 2 - renderer.getTextHeight() + 1) / 2;
		int color = HEADER_TEXT_COLOR;
		
		if (!expanded && expandedColumnModel.hasExpandedColumn())
			color = DARK_HEADER_TEXT_COLOR;

		long gametick = modelView.getColumnGametick(columnIndex);
		renderer.drawText(Long.toString(gametick), cx + 2, ty, color, false);
		
		if (renderer.getMouseX() >= cx && renderer.getMouseX() < cx + cw) {
			renderer.drawVLine(cx, 0, height, COLUMN_LINE_COLOR);
			renderer.drawVLine(cx + cw - 1, 0, height, COLUMN_LINE_COLOR);
		}
		
		if (expanded)
			renderMicrotickLabels(renderer, columnIndex);
	}
	
	private void renderMicrotickLabels(GSIRenderer2D renderer, int expandedColumnIndex) {
		int duration = modelView.getColumnDuration(expandedColumnIndex);
		int y = height * 3 / 4 - renderer.getTextHeight() / 2;
		
		for (int mt = 0; mt < duration; mt++) {
			int x = modelView.getMicrotickColumnX(expandedColumnIndex, mt);
			int w = modelView.getMicrotickColumnWidth(expandedColumnIndex, mt);

			renderer.drawCenteredText(Integer.toString(mt), x + w / 2, y, HEADER_TEXT_COLOR);
		
			if (mt != 0) {
				int ly = height / 2 + GSSequenceColumnHeaderPanel.DOTTED_LINE_SPACING / 2;
				renderer.drawDottedVLine(x, ly, height, DOTTED_LINE_LENGTH, 
						DOTTED_LINE_SPACING, MT_COLUMN_LINE_COLOR);
			}
		}
		
		int ex = modelView.getColumnX(expandedColumnIndex);
		int ew = modelView.getColumnWidth(expandedColumnIndex);
		renderer.fillRect(ex, height / 2 - 1, ew, 1, MT_COLUMN_LINE_COLOR);
	}
	
	@Override
	public void mousePressed(GSMouseEvent event) {
		if (event.getButton() == GSMouseEvent.BUTTON_LEFT) {
			int hoveredColumn = modelView.getColumnIndexFromView(event.getX());
			if (hoveredColumn != -1) {
				if (event.isModifierHeld(GSEvent.MODIFIER_SHIFT)) {
					expandedColumnModel.includeExpandedColumn(hoveredColumn);
				} else {
					expandedColumnModel.toggleExpandedColumn(hoveredColumn);
				}
				
				event.consume();
			}
		}
	}
}
