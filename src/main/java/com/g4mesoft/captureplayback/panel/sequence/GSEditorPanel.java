package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSEAnchor;
import com.g4mesoft.panel.GSEFill;
import com.g4mesoft.panel.GSGridLayoutManager;
import com.g4mesoft.panel.GSLocation;
import com.g4mesoft.panel.GSPanel;
import com.g4mesoft.panel.GSPanelContext;
import com.g4mesoft.panel.GSPanelUtil;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.GSPopup;
import com.g4mesoft.panel.button.GSButton;
import com.g4mesoft.panel.field.GSTextLabel;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.text.Text;

public abstract class GSEditorPanel extends GSParentPanel {

	private static final int BACKGROUND_COLOR = 0xFF252526;
	
	private static final Text CANCEL_TEXT = Text.literal("Cancel");
	private static final Text APPLY_TEXT = Text.literal("Apply");
	private static final Text DONE_TEXT = Text.literal("Done");
	
	private static final int OUTER_MARGIN  = 10;
	private static final int TITLE_MARGIN  = 10;
	private static final int BUTTON_MARGIN = 5;
	
	private final GSParentPanel contentPanel;
	
	private final GSTextLabel titleLabel;
	private final GSButton cancelButton;
	private final GSButton applyButton;
	private final GSButton doneButton;
	
	public GSEditorPanel() {
		contentPanel = new GSParentPanel();
		
		titleLabel = new GSTextLabel((Text)null);
		cancelButton = new GSButton(CANCEL_TEXT);
		applyButton = new GSButton(APPLY_TEXT);
		doneButton = new GSButton(DONE_TEXT);
	
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
		cancelButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.LEFT_MARGIN, OUTER_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, OUTER_MARGIN);
		add(cancelButton);
		applyButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 1)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.LEFT_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, OUTER_MARGIN);
		add(applyButton);
		doneButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 2)
			.set(GSGridLayoutManager.GRID_Y, 2)
			.set(GSGridLayoutManager.TOP_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.LEFT_MARGIN, BUTTON_MARGIN)
			.set(GSGridLayoutManager.BOTTOM_MARGIN, OUTER_MARGIN)
			.set(GSGridLayoutManager.RIGHT_MARGIN, OUTER_MARGIN);
		add(doneButton);
		
		setProperty(PREFERRED_HEIGHT, 200);
	}
	
	private void initEventListeners() {
		cancelButton.addActionListener(this::close);
		applyButton.addActionListener(this::apply);
		doneButton.addActionListener(() -> {
			apply();
			close();
		});
	}
	
	protected abstract void apply();

	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, BACKGROUND_COLOR);
		
		super.render(renderer);
	}
	
	public void show(GSPanel source) {
		GSPopup popup = new GSPopup(this);
		
		// Show popup centered relative to source, or root panel (if source is null).
		GSPanel relative = (source != null) ? source : GSPanelContext.getRootPanel();
		GSLocation location = GSPanelUtil.getViewLocation(relative);
		GSDimension popupSize = popup.getProperty(PREFERRED_SIZE);
		int cx = location.getX() + (relative.getWidth()  - popupSize.getWidth()) / 2;
		int cy = location.getY() + (relative.getHeight() - popupSize.getHeight()) / 2;
		
		popup.show(source, cx, cy);
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
