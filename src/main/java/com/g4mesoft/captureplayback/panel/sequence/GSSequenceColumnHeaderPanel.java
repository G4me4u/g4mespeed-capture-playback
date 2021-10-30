package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.captureplayback.panel.GSIModelViewListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.panel.dropdown.GSDropdown;
import com.g4mesoft.panel.dropdown.GSDropdownAction;
import com.g4mesoft.panel.event.GSEvent;
import com.g4mesoft.panel.event.GSIMouseListener;
import com.g4mesoft.panel.event.GSMouseEvent;
import com.g4mesoft.panel.scroll.GSIScrollable;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class GSSequenceColumnHeaderPanel extends GSPanel implements GSIScrollable, GSIModelViewListener,
                                                                    GSIMouseListener {

	public static final int COLUMN_HEADER_COLOR    = 0xFF202020;
	public static final int HEADER_TEXT_COLOR      = 0xFFE0E0E0;
	public static final int DARK_HEADER_TEXT_COLOR = 0xFFA0A0A0;
	
	public static final int COLUMN_LINE_COLOR = 0xFF202020;
	public static final int DOTTED_LINE_COLOR = 0xFF404040;
	
	public static final int DOTTED_LINE_LENGTH  = 4;
	public static final int DOTTED_LINE_SPACING = 3;
	
	private static final int COLUMN_TITLE_LEFT_MARGIN = 2;
	
	private static final int SHADOW_WIDTH = 10;
	private static final int SHADOW_START_COLOR = COLUMN_HEADER_COLOR;
	private static final int SHADOW_END_COLOR = SHADOW_START_COLOR & 0x00FFFFFF;
	
	private static final Text EXPAND_TEXT       = new TranslatableText("panel.sequencecolumnheader.expand");
	private static final Text COLLAPSE_TEXT     = new TranslatableText("panel.sequencecolumnheader.collapse");
	private static final Text EXPAND_ALL_TEXT   = new TranslatableText("panel.sequencecolumnheader.expandall");
	private static final Text COLLAPSE_ALL_TEXT = new TranslatableText("panel.sequencecolumnheader.collapseall");
	
	private static final int COLUMN_HEADER_PREFERRED_HEIGHT = 30;
	
	private final GSExpandedColumnModel expandedColumnModel;
	private final GSSequenceModelView modelView;
	
	private int hoveredColumnIndex;
	
	public GSSequenceColumnHeaderPanel(GSSequence sequence, GSSequenceModelView modelView) {
		this.expandedColumnModel = modelView.getExpandedColumnModel();
		this.modelView = modelView;
		
		addMouseEventListener(this);
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
	
		GSRectangle bounds = renderer.getClipBounds().intersection(0, 0, width, height);
		
		renderBackground(renderer, bounds);
		renderColumnHeaders(renderer, bounds);

		// Bottom separator
		renderer.drawHLine(bounds.x, bounds.x + bounds.width, height - 1, GSSequencePanel.CHANNEL_SPACING_COLOR);
		// Shadow for fading out columns to the right.
		renderer.fillHGradient(bounds.x + bounds.width - SHADOW_WIDTH, 0,
				SHADOW_WIDTH, height, SHADOW_END_COLOR, SHADOW_START_COLOR);
	}

	private void renderBackground(GSIRenderer2D renderer, GSRectangle bounds) {
		renderer.fillRect(bounds.x, bounds.y, bounds.width, bounds.height, COLUMN_HEADER_COLOR);
	}
	
	private void renderColumnHeaders(GSIRenderer2D renderer, GSRectangle bounds) {
		int columnIndex = Math.max(0, modelView.getColumnIndexFromX(bounds.x));
		int cx = modelView.getColumnX(columnIndex);
		while (cx < bounds.x + bounds.width) {
			int cw = modelView.getColumnWidth(columnIndex);
			renderColumnHeader(renderer, columnIndex, cx, cw);
			cx += cw;
			columnIndex++;
		}
	}
	
	private void renderColumnHeader(GSIRenderer2D renderer, int columnIndex, int cx, int cw) {
		boolean expanded = expandedColumnModel.isColumnExpanded(columnIndex);
		
		int color = HEADER_TEXT_COLOR;
		if (!expanded && expandedColumnModel.hasExpandedColumn())
			color = DARK_HEADER_TEXT_COLOR;

		String title = Long.toString(modelView.getColumnGametick(columnIndex));
		int ty = (height / 2 - renderer.getTextHeight() + 1) / 2;
		renderer.drawTextNoStyle(title, cx + COLUMN_TITLE_LEFT_MARGIN, ty, color, false);
		
		if (columnIndex == hoveredColumnIndex) {
			renderer.drawVLine(cx - 1, 0, height, COLUMN_LINE_COLOR);
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
				renderer.drawDottedVLine(x - 1, ly, height, DOTTED_LINE_LENGTH, 
						DOTTED_LINE_SPACING, DOTTED_LINE_COLOR);
			}
		}
		
		int ex = modelView.getColumnX(expandedColumnIndex);
		int ew = modelView.getColumnWidth(expandedColumnIndex);
		renderer.fillRect(ex, height / 2 - 1, ew, 1, DOTTED_LINE_COLOR);
	}
	
	@Override
	protected GSDimension calculatePreferredSize() {
		return new GSDimension(modelView.getMinimumWidth(), COLUMN_HEADER_PREFERRED_HEIGHT);
	}
	
	@Override
	public void populateRightClickMenu(GSDropdown dropdown, int x, int y) {
		int hoveredColumn = modelView.getColumnIndexFromX(x);
		if (hoveredColumn != -1) {
			dropdown.addItem(new GSDropdownAction(EXPAND_TEXT, () -> {
				expandedColumnModel.setExpandedColumn(hoveredColumn);
			}));
			GSDropdownAction collapseAction;
			dropdown.addItem(collapseAction = new GSDropdownAction(COLLAPSE_TEXT, () -> {
				expandedColumnModel.toggleExpandedColumn(hoveredColumn);
			}));
			dropdown.separate();
			dropdown.addItem(new GSDropdownAction(EXPAND_ALL_TEXT, () -> {
				expandedColumnModel.setExpandedColumnRange(0, Integer.MAX_VALUE);
			}));
			dropdown.addItem(new GSDropdownAction(COLLAPSE_ALL_TEXT, () -> {
				expandedColumnModel.clearExpandedColumns();
			}));
			
			collapseAction.setEnabled(expandedColumnModel.isColumnExpanded(hoveredColumn));
		}
	}
	
	@Override
	public boolean isScrollableWidthFilled() {
		return true;
	}
	
	@Override
	public void modelViewChanged() {
		invalidate();
	}
	
	@Override
	public void mousePressed(GSMouseEvent event) {
		if (event.getButton() == GSMouseEvent.BUTTON_LEFT) {
			int hoveredColumn = modelView.getColumnIndexFromX(event.getX());
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
	
	public void setHoveredColumn(int columnIndex) {
		hoveredColumnIndex = columnIndex;
	}
}
