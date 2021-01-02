package com.g4mesoft.captureplayback.gui.sequence;

import java.util.Locale;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.gui.GSPanel;
import com.g4mesoft.gui.event.GSEvent;
import com.g4mesoft.gui.event.GSIMouseListener;
import com.g4mesoft.gui.event.GSMouseEvent;
import com.g4mesoft.renderer.GSIRenderer2D;

public class GSSequenceColumnHeaderPanel extends GSPanel implements GSIMouseListener {

	public static final int COLUMN_HEADER_COLOR = 0x60000000;
	public static final int HEADER_TEXT_COLOR = 0xFFFFFFFF;
	public static final int DARK_HEADER_TEXT_COLOR = 0xFFB2B2B2;
	
	public static final int COLUMN_COLOR = 0xDA181818;
	public static final int DARK_COLUMN_COLOR = 0xDA0A0A0A;
	
	public static final int COLUMN_LINE_COLOR = 0x30B2B2B2;
	public static final int MT_COLUMN_LINE_COLOR = 0x30FEFEFE;
	
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

		renderer.drawHLine(0, width, height - 1, GSSequenceChannelHeaderPanel.CHANNEL_SPACING_COLOR);
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
		
		renderer.fillRect(cx, 0, cw, height, getColumnColor(columnIndex));

		int ty;
		int color = HEADER_TEXT_COLOR;
		
		if (expanded) {
			ty = (height / 2 - renderer.getTextHeight() + 1) / 2;
		} else {
			if (expandedColumnModel.hasExpandedColumn())
				color = DARK_HEADER_TEXT_COLOR;
			ty = (height - renderer.getTextHeight() + 1) / 2;
		}

		String title = getColumnTitle(columnIndex);
		renderer.drawCenteredText(title, cx + cw / 2, ty, color);
		
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

			String title = getMicrotickHeaderTitle(mt);
			renderer.drawCenteredText(title, x + w / 2, y, HEADER_TEXT_COLOR);
		
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
	
	private int getColumnColor(int columnIndex) {
		return (columnIndex & 0x1) != 0 ? DARK_COLUMN_COLOR : COLUMN_COLOR;
	}
	
	private String getColumnTitle(int columnIndex) {
		return String.format(Locale.ENGLISH, "%dgt", modelView.getColumnGametick(columnIndex));
	}

	private String getMicrotickHeaderTitle(int mt) {
		return String.format(Locale.ENGLISH, "%d", mt);
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
