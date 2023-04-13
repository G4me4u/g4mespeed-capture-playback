package com.g4mesoft.captureplayback.panel.sequence;

import static com.g4mesoft.captureplayback.gui.GSCapturePlaybackPanel.translatable;

import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSEPopupPlacement;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSPanel;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.GSPopup;
import com.g4mesoft.ui.panel.button.GSButton;
import com.g4mesoft.ui.panel.event.GSKeyButtonStroke;
import com.g4mesoft.ui.panel.event.GSKeyEvent;
import com.g4mesoft.ui.panel.field.GSTextLabel;
import com.g4mesoft.ui.renderer.GSIRenderer2D;

import net.minecraft.text.Text;

public abstract class GSEditorPanel extends GSParentPanel {

	private static final int BACKGROUND_COLOR = 0xFF252526;
	
	private static final Text DONE_TEXT   = translatable("done");
	private static final Text APPLY_TEXT  = translatable("apply");
	private static final Text CANCEL_TEXT = translatable("cancel");
	
	private static final int OUTER_MARGIN  = 10;
	private static final int TITLE_MARGIN  = 10;
	private static final int BUTTON_MARGIN = 5;
	
	private final GSParentPanel contentPanel;
	
	private final GSTextLabel titleLabel;
	private final GSButton doneButton;
	private final GSButton applyButton;
	private final GSButton cancelButton;
	
	public GSEditorPanel() {
		contentPanel = new GSParentPanel();
		
		titleLabel = new GSTextLabel((Text)null);
		doneButton = new GSButton(DONE_TEXT);
		applyButton = new GSButton(APPLY_TEXT);
		cancelButton = new GSButton(CANCEL_TEXT);
	
		initLayout();
		initEventListeners();
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		titleLabel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.GRID_WIDTH, 3)
			.set(GSGridLayoutManager.TOP_MARGIN, TITLE_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, TITLE_MARGIN);
		add(titleLabel);
		contentPanel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 1)
			.set(GSGridLayoutManager.GRID_WIDTH, 3)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH)
			.set(GSGridLayoutManager.LEFT_MARGIN, OUTER_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, OUTER_MARGIN);
		add(contentPanel);
		doneButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.LEFT_MARGIN, OUTER_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, OUTER_MARGIN);
		add(doneButton);
		applyButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.LEFT_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, OUTER_MARGIN);
		add(applyButton);
		cancelButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 2)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.EAST)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.LEFT_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, OUTER_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, OUTER_MARGIN);
		add(cancelButton);
		
		setProperty(PREFERRED_HEIGHT, 200);
	}
	
	private void initEventListeners() {
		doneButton.addActionListener(this::applyAndClose);
		applyButton.addActionListener(this::apply);
		cancelButton.addActionListener(this::close);
		putButtonStroke(new GSKeyButtonStroke(GSKeyEvent.KEY_ENTER), this::applyAndClose);
		putButtonStroke(new GSKeyButtonStroke(GSKeyEvent.KEY_ESCAPE), this::close);
	}
	
	private void applyAndClose() {
		apply();
		close();
	}
	
	protected abstract void apply();

	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, BACKGROUND_COLOR);
		
		super.render(renderer);
	}
	
	public void show(GSPanel source) {
		GSPopup popup = new GSPopup(this, true);
		popup.setHiddenOnFocusLost(false);
		popup.setSourceFocusedOnHide(source != null);
		popup.show(source, 0, 0, GSEPopupPlacement.CENTER);
	}
	
	protected void close() {
		GSPanel parent = getParent();
		if (parent instanceof GSPopup)
			((GSPopup)parent).hide();
	}
	
	protected void setTitle(Text title) {
		titleLabel.setText(title);
	}
	
	public GSPanel getContentPanel() {
		return contentPanel;
	}
}
