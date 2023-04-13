package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel;
import com.g4mesoft.captureplayback.session.GSIUndoRedoListener;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.captureplayback.session.GSUndoRedoHistory;
import com.g4mesoft.ui.panel.GSDimension;
import com.g4mesoft.ui.panel.GSECursorType;
import com.g4mesoft.ui.panel.GSEIconAlignment;
import com.g4mesoft.ui.panel.GSIcon;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.GSTexturedIcon;
import com.g4mesoft.ui.panel.button.GSButton;
import com.g4mesoft.ui.renderer.GSIRenderer2D;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class GSSequenceInfoPanel extends GSParentPanel implements GSIUndoRedoListener {

	private static final GSIcon UNDO_ICON          = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion(0, 27, 9, 9));
	private static final GSIcon HOVERED_UNDO_ICON  = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion(0, 36, 9, 9));
	private static final GSIcon DISABLED_UNDO_ICON = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion(0, 45, 9, 9));
	private static final GSIcon REDO_ICON          = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion(9, 27, 9, 9));
	private static final GSIcon HOVERED_REDO_ICON  = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion(9, 36, 9, 9));
	private static final GSIcon DISABLED_REDO_ICON = new GSTexturedIcon(GSCapturePlaybackPanel.ICONS_SHEET.getRegion(9, 45, 9, 9));
	
	private static final Text UNDO_TEXT = new TranslatableText("panel.edit.undo");
	private static final Text REDO_TEXT = new TranslatableText("panel.edit.redo");
	
	private static final int BUTTON_MARGIN = 2;
	
	private final GSUndoRedoHistory history;
	
	private final GSButton undoButton;
	private final GSButton redoButton;
	
	public GSSequenceInfoPanel(GSSession session) {
		history = session.get(GSSession.UNDO_REDO_HISTORY);
		
		undoButton = new GSButton(UNDO_ICON, UNDO_TEXT);
		undoButton.setHoveredIcon(HOVERED_UNDO_ICON);
		undoButton.setDisabledIcon(DISABLED_UNDO_ICON);
		undoButton.setIconAlignment(GSEIconAlignment.LEFT);
		undoButton.setCursor(GSECursorType.HAND);
		undoButton.setBackgroundColor(0);
		undoButton.setHoveredBackgroundColor(0);
		undoButton.setDisabledBackgroundColor(0);
		undoButton.setBorderWidth(0);
		undoButton.addActionListener(history::undo);
		
		redoButton = new GSButton(REDO_ICON, REDO_TEXT);
		redoButton.setHoveredIcon(HOVERED_REDO_ICON);
		redoButton.setDisabledIcon(DISABLED_REDO_ICON);
		redoButton.setIconAlignment(GSEIconAlignment.LEFT);
		redoButton.setCursor(GSECursorType.HAND);
		redoButton.setBackgroundColor(0);
		redoButton.setHoveredBackgroundColor(0);
		redoButton.setDisabledBackgroundColor(0);
		redoButton.setBorderWidth(0);
		redoButton.addActionListener(history::redo);
		
		add(undoButton);
		add(redoButton);
	}
	
	@Override
	protected void layout() {
		GSDimension undoPrefS = undoButton.getProperty(PREFERRED_SIZE);
		GSDimension redoPrefS = redoButton.getProperty(PREFERRED_SIZE);

		int bx = BUTTON_MARGIN;
		int by = Math.min(height - undoPrefS.getHeight(), height - redoPrefS.getHeight()) - BUTTON_MARGIN;
		undoButton.setBounds(bx, by, undoPrefS.getWidth(), undoPrefS.getHeight());
		bx += undoPrefS.getWidth() + BUTTON_MARGIN;
		redoButton.setBounds(bx, by, redoPrefS.getWidth(), redoPrefS.getHeight());
	}
	
	@Override
	protected void onShown() {
		super.onShown();
		
		history.addUndoRedoListener(this);
		
		onHistoryChanged();
	}

	@Override
	protected void onHidden() {
		super.onHidden();
		
		history.removeUndoRedoListener(this);
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, GSChannelHeaderPanel.CHANNEL_HEADER_COLOR);

		renderer.drawVLine(width - 1, 0, height, GSSequenceColumnHeaderPanel.COLUMN_LINE_COLOR);
		renderer.drawHLine(0, width, height - 1, GSSequencePanel.CHANNEL_SPACING_COLOR);

		super.render(renderer);
	}

	@Override
	public void onHistoryChanged() {
		undoButton.setEnabled(history.hasUndoHistory());
		redoButton.setEnabled(history.hasRedoHistory());
	}
}
