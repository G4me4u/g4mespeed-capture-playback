package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSLocation;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSPanelUtil;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.GSPopup;
import com.g4mesoft.panel.button.GSButton;
import com.g4mesoft.panel.scroll.GSScrollPanel;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public abstract class GSEditorPanel extends GSParentPanel {

	private static final int BACKGROUND_COLOR = 0xFF252526;
	
	private static final Text CANCEL_TEXT = new LiteralText("Cancel");
	private static final Text APPLY_TEXT = new LiteralText("Apply");
	private static final Text DONE_TEXT = new LiteralText("Done");
	
	private static final int BUTTON_MARGIN = 5;
	
	private final GSScrollPanel scrollPanel;
	private final GSPanel contentPanel;
	
	private final GSButton cancelButton;
	private final GSButton applyButton;
	private final GSButton doneButton;
	
	public GSEditorPanel() {
		contentPanel = new GSParentPanel();
		scrollPanel = new GSScrollPanel(contentPanel);
		
		cancelButton = new GSButton(CANCEL_TEXT);
		applyButton = new GSButton(APPLY_TEXT);
		doneButton = new GSButton(DONE_TEXT);
	
		cancelButton.addActionListener(this::close);
		applyButton.addActionListener(this::apply);
		doneButton.addActionListener(() -> {
			apply();
			close();
		});
		
		add(scrollPanel);
		
		add(cancelButton);
		add(applyButton);
		add(doneButton);
	}
	
	protected abstract void apply();

	protected void close() {
		GSPanel parent = getParent();
		if (parent instanceof GSPopup)
			((GSPopup)parent).hide();
	}
	
	@Override
	protected void layout() {
		GSDimension cbs = cancelButton.getPreferredSize();
		GSDimension abs = applyButton.getPreferredSize();
		GSDimension dbs = doneButton.getPreferredSize();
		
		int bh = Math.max(cbs.getHeight(), Math.max(abs.getHeight(), dbs.getHeight()));
		int ch = Math.max(0, height - bh - BUTTON_MARGIN * 2);
		
		scrollPanel.setBounds(0, 0, width, ch);
		
		int cbx = BUTTON_MARGIN;
		int dbx = width - dbs.getWidth() - BUTTON_MARGIN;
		int abx = dbx - abs.getWidth() - BUTTON_MARGIN;
		
		cancelButton.setBounds(cbx, ch + BUTTON_MARGIN, cbs.getWidth(), cbs.getHeight());
		applyButton.setBounds(abx, ch + BUTTON_MARGIN, abs.getWidth(), abs.getHeight());
		doneButton.setBounds(dbx, ch + BUTTON_MARGIN, dbs.getWidth(), dbs.getHeight());
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, BACKGROUND_COLOR);
		
		super.render(renderer);
	}
	
	@Override
	protected GSDimension calculatePreferredSize() {
		// TODO: change this?
		return new GSDimension(200, 200);
	}

	@Override
	protected GSDimension calculateMinimumSize() {
		return contentPanel.getMinimumSize();
	}
	
	public void show(GSPanel source) {
		GSPopup popup = new GSPopup(this);

		GSLocation location = GSPanelUtil.getViewLocation(source);
		GSDimension popupSize = popup.getPreferredSize();
		int px = location.getX() + (source.getWidth() - popupSize.getWidth()) / 2;
		int py = location.getY() + (source.getHeight() - popupSize.getHeight()) / 2;
		
		popup.show(source, px, py);
	}
	
	public GSPanel getContentPanel() {
		return contentPanel;
	}
}
