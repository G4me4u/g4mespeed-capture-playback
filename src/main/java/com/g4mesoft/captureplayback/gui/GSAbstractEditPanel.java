package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.common.asset.GSAssetHandle;
import com.g4mesoft.captureplayback.panel.GSEContentOpacity;
import com.g4mesoft.captureplayback.session.GSESessionType;
import com.g4mesoft.captureplayback.session.GSSession;
import com.g4mesoft.core.client.GSClientController;
import com.g4mesoft.ui.panel.GSColoredIcon;
import com.g4mesoft.ui.panel.GSEAnchor;
import com.g4mesoft.ui.panel.GSECursorType;
import com.g4mesoft.ui.panel.GSEFill;
import com.g4mesoft.ui.panel.GSETextAlignment;
import com.g4mesoft.ui.panel.GSGridLayoutManager;
import com.g4mesoft.ui.panel.GSIActionListener;
import com.g4mesoft.ui.panel.GSIcon;
import com.g4mesoft.ui.panel.GSPanel;
import com.g4mesoft.ui.panel.GSPanelContext;
import com.g4mesoft.ui.panel.GSParentPanel;
import com.g4mesoft.ui.panel.button.GSButton;
import com.g4mesoft.ui.panel.dropdown.GSDropdown;
import com.g4mesoft.ui.panel.dropdown.GSDropdownAction;
import com.g4mesoft.ui.panel.dropdown.GSDropdownSubMenu;
import com.g4mesoft.ui.panel.event.GSFocusEvent;
import com.g4mesoft.ui.panel.event.GSIFocusEventListener;
import com.g4mesoft.ui.panel.event.GSIKeyListener;
import com.g4mesoft.ui.panel.event.GSIMouseListener;
import com.g4mesoft.ui.panel.event.GSKeyEvent;
import com.g4mesoft.ui.panel.event.GSMouseEvent;
import com.g4mesoft.ui.panel.field.GSTextField;
import com.g4mesoft.ui.panel.scroll.GSEScrollBarPolicy;
import com.g4mesoft.ui.panel.scroll.GSScrollBar;
import com.g4mesoft.ui.panel.scroll.GSScrollPanel;
import com.g4mesoft.ui.renderer.GSIRenderer2D;
import com.g4mesoft.ui.util.GSTextUtil;

import net.minecraft.text.Text;

public abstract class GSAbstractEditPanel extends GSParentPanel {

	private static final int TITLE_PREFERRED_WIDTH = 250;
	private static final int TITLE_MARGIN = 5;
	private static final int ASSET_UUID_RIGHT_MARGIN = 2;
	
	private static final int TITLE_SEPARATOR_COLOR = 0xFF444444;
	private static final int TITLE_BACKGROUND_COLOR = 0xFF171717;
	
	private static final GSIcon BACK_ICON = GSPanelContext.getIcon(60, 32, 9, 9);
	private static final GSIcon HOVERED_BACK_ICON = GSPanelContext.getIcon(69, 32, 9, 9);
	private static final GSIcon DISABLED_BACK_ICON = GSPanelContext.getIcon(78, 32, 9, 9);
	private static final Text BACK_TEXT = GSTextUtil.translatable("panel.edit.back");
	
	private static final GSIcon OPACITY_SELECTED_ICON = new GSColoredIcon(0xFFFFFFFF, 4, 4);
	private static final Text OPACITY_TEXT = GSTextUtil.translatable("panel.opacity");
	
	private static final int CONTENT_EVENT_HANDLER_PRIORITY = 100;
	
	protected final GSSession session;
	
	protected final GSButton backButton;
	protected final GSTextField nameField;
	protected final GSTextField handleField;
	protected final GSScrollPanel scrollPanel;
	
	private final GSContentEventHandler contentHandler;
	private final GSContentEventHandler columnHeaderHandler;
	private final GSContentEventHandler rowHeaderHandler;
	
	private GSEContentOpacity opacity;
	
	private boolean editable;
	
	public GSAbstractEditPanel(GSSession session, GSESessionType type) {
		if (session.getType() != type)
			throw new IllegalArgumentException("Session is not of type '" + type.getName() + "'");
		
		this.session = session;
		
		backButton = new GSButton(BACK_ICON, BACK_TEXT);
		backButton.setHoveredIcon(HOVERED_BACK_ICON);
		backButton.setDisabledIcon(DISABLED_BACK_ICON);
		backButton.setBackgroundColor(0);
		backButton.setHoveredBackgroundColor(0);
		backButton.setDisabledBackgroundColor(0);
		backButton.setBorderWidth(0);
		backButton.setCursor(GSECursorType.HAND);
		
		nameField = new GSTextField();
		nameField.setBackgroundColor(0x00000000);
		nameField.setTextAlignment(GSETextAlignment.CENTER);
		nameField.setBorderWidth(0);
		nameField.setVerticalMargin(TITLE_MARGIN);
		nameField.setHorizontalMargin(0);
		nameField.setFocusLostOnConfirm(true);

		handleField = new GSTextField();
		GSAssetHandle handle = session.get(GSSession.ASSET_HANDLE);
		if (handle != null)
			handleField.setText(handle.toString());
		handleField.setEditable(false);
		handleField.setBackgroundColor(0x00000000);
		handleField.setTextAlignment(GSETextAlignment.RIGHT);
		handleField.setBorderWidth(0);
		handleField.setVerticalMargin(TITLE_MARGIN);
		handleField.setHorizontalMargin(0);
		
		scrollPanel = new GSScrollPanel();
		scrollPanel.setHorizontalScrollBarPolicy(GSEScrollBarPolicy.SCROLLBAR_ALWAYS);
		scrollPanel.setVerticalScrollBarPolicy(GSEScrollBarPolicy.SCROLLBAR_AS_NEEDED);
		
		contentHandler = new GSContentEventHandler(true, true);
		columnHeaderHandler = new GSContentEventHandler(true, false);
		rowHeaderHandler = new GSContentEventHandler(false, true);
		
		setContentOpacity(GSEContentOpacity.FULLY_OPAQUE);
		onHorizontalScrollBarChanged(scrollPanel.getHorizontalScrollBar());
		
		initLayout();
		initEventListeners();
		
		editable = false;
	}
	
	private void initLayout() {
		setLayoutManager(new GSGridLayoutManager());
		
		backButton.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.WEST);
		add(backButton);
		nameField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.GRID_WIDTH, 3)
			.set(GSGridLayoutManager.WEIGHT_X, 1.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.CENTER)
			.set(GSGridLayoutManager.PREFERRED_WIDTH, TITLE_PREFERRED_WIDTH);
		add(nameField);
		handleField.getLayout()
			.set(GSGridLayoutManager.GRID_X, 2)
			.set(GSGridLayoutManager.GRID_Y, 0)
			.set(GSGridLayoutManager.WEIGHT_X, 0.0f)
			.set(GSGridLayoutManager.ANCHOR, GSEAnchor.EAST)
			.set(GSGridLayoutManager.RIGHT_MARGIN, ASSET_UUID_RIGHT_MARGIN);
		add(handleField);
		scrollPanel.getLayout()
			.set(GSGridLayoutManager.GRID_X, 0)
			.set(GSGridLayoutManager.GRID_Y, 1)
			.set(GSGridLayoutManager.GRID_WIDTH, 3)
			.set(GSGridLayoutManager.WEIGHT_Y, 1.0f)
			.set(GSGridLayoutManager.FILL, GSEFill.BOTH);
		add(scrollPanel);
	}
	
	private void initEventListeners() {
		backButton.addActionListener(new GSIActionListener() {
			@Override
			public void actionPerformed() {
				GSClientController.getInstance().getPrimaryGUI().back();
			}
		});
		
		nameField.addActionListener(() -> {
			onNameChanged(nameField.getText());
		});
		
		// Handle undo/redo events
		addKeyEventListener(new GSIKeyListener() {
			@Override
			public void keyPressed(GSKeyEvent event) {
				switch (event.getKeyCode()) {
				case GSKeyEvent.KEY_Z:
					if (isEditable()) {
						if (!event.isModifierHeld(GSKeyEvent.MODIFIER_ALT) &&
						     event.isModifierHeld(GSKeyEvent.MODIFIER_CONTROL)) {
							
							// Allow for redo with CTRL + SHIFT + Z
							if (event.isModifierHeld(GSKeyEvent.MODIFIER_SHIFT)) {
								session.get(GSSession.UNDO_REDO_HISTORY).redo();
							} else {
								session.get(GSSession.UNDO_REDO_HISTORY).undo();
							}
						}
					}
					break;
				case GSKeyEvent.KEY_Y:
					if (isEditable()) {
						if (!event.isAnyModifierHeld(GSKeyEvent.MODIFIER_ALT | GSKeyEvent.MODIFIER_SHIFT) &&
							event.isModifierHeld(GSKeyEvent.MODIFIER_CONTROL)) {
							
							session.get(GSSession.UNDO_REDO_HISTORY).redo();
						}
					}
					break;
				}
			}
		});
	}
	
	protected abstract void onNameChanged(String name);
	
	protected void setContent(GSPanel content) {
		GSPanel oldContent = scrollPanel.getContent();
		if (oldContent != null)
			contentHandler.uninstall(oldContent);

		scrollPanel.setContent(content);
		
		if (content != null)
			contentHandler.install(content);
	}

	protected void setColumnHeader(GSPanel columnHeader) {
		GSPanel oldColumnHeader = scrollPanel.getColumnHeader();
		if (oldColumnHeader != null)
			columnHeaderHandler.uninstall(oldColumnHeader);

		scrollPanel.setColumnHeader(columnHeader);
		
		if (columnHeader != null)
			columnHeaderHandler.install(columnHeader);
	}

	protected void setRowHeader(GSPanel rowHeader) {
		GSPanel oldRowHeader = scrollPanel.getRowHeader();
		if (oldRowHeader != null)
			rowHeaderHandler.uninstall(oldRowHeader);
		
		scrollPanel.setRowHeader(rowHeader);

		if (rowHeader != null)
			rowHeaderHandler.install(rowHeader);
	}
	
	protected void setHorizontalScrollBar(GSScrollBar scrollBar) {
		scrollPanel.setHorizontalScrollBar(scrollBar);
		onHorizontalScrollBarChanged(scrollBar);
	}
	
	private void onHorizontalScrollBarChanged(GSScrollBar scrollBar) {
		scrollBar.setModel(new GSUnlimitedScrollBarModel());
	}

	@Override
	protected void onShown() {
		super.onShown();
		retrieveSessionFields();
		
		GSPanel content = scrollPanel.getContent();
		if (content != null)
			content.requestFocus();
	}
	
	@Override
	protected void onHidden() {
		super.onHidden();
		offerSessionFields();
		session.sync();
	}

	protected void retrieveSessionFields() {
		setContentOpacity(session.get(GSSession.OPACITY));
		setXOffset(session.get(GSSession.X_OFFSET));
		setYOffset(session.get(GSSession.Y_OFFSET));
	}

	protected void offerSessionFields() {
		session.set(GSSession.OPACITY, getContentOpacity());
		session.set(GSSession.X_OFFSET, getXOffset());
		session.set(GSSession.Y_OFFSET, getYOffset());
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderTitleBackground(renderer);

		super.render(renderer);
	}
	
	private void renderTitleBackground(GSIRenderer2D renderer) {
		renderer.drawHLine(0, width, scrollPanel.getY() - 1, TITLE_SEPARATOR_COLOR);
		renderer.fillRect(0, 0, width, scrollPanel.getY() - 1, TITLE_BACKGROUND_COLOR);
	}

	@Override
	public void populateRightClickMenu(GSDropdown dropdown, int x, int y) {
		GSDropdown opacityMenu = new GSDropdown();
		for (GSEContentOpacity opacity : GSEContentOpacity.OPACITIES) {
			GSIcon icon = (this.opacity == opacity) ? OPACITY_SELECTED_ICON : null;
			Text text = GSTextUtil.translatable(opacity.getName());
			opacityMenu.addItem(new GSDropdownAction(icon, text, () -> {
				setContentOpacity(opacity);
			}));
		}
		dropdown.addItem(new GSDropdownSubMenu(OPACITY_TEXT, opacityMenu));
	}
	
	public GSEContentOpacity getContentOpacity() {
		return opacity;
	}

	public void setContentOpacity(GSEContentOpacity opacity) {
		if (opacity == null)
			throw new IllegalArgumentException("opacity is null");
		
		this.opacity = opacity;
		scrollPanel.setOpacity(opacity.getOpacity());
	}
	
	public float getXOffset() {
		return scrollPanel.getHorizontalScrollBar().getScroll();
	}

	public void setXOffset(float xOffset) {
		scrollPanel.getHorizontalScrollBar().setScroll(xOffset);
	}

	public float getYOffset() {
		return scrollPanel.getVerticalScrollBar().getScroll();
	}
	
	public void setYOffset(float yOffset) {
		scrollPanel.getVerticalScrollBar().setScroll(yOffset);
	}
	
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	private class GSContentEventHandler implements GSIMouseListener, GSIKeyListener, GSIFocusEventListener {
		
		private final boolean draggableX;
		private final boolean draggableY;
		
		private boolean draggingModifier;
		private boolean draggingContent;
		
		public GSContentEventHandler(boolean draggableX, boolean draggableY) {
			this.draggableX = draggableX;
			this.draggableY = draggableY;
		
			draggingModifier = false;
			draggingContent = false;
		}
		
		public void install(GSPanel content) {
			content.addMouseEventListener(this, CONTENT_EVENT_HANDLER_PRIORITY);
			content.addKeyEventListener(this, CONTENT_EVENT_HANDLER_PRIORITY);
			content.addFocusEventListener(this);
		}

		public void uninstall(GSPanel content) {
			content.removeMouseEventListener(this);
			content.removeKeyEventListener(this);
			content.removeFocusEventListener(this);
		}
		
		@Override
		public void mousePressed(GSMouseEvent event) {
			if (!draggingContent && (event.getButton() == GSMouseEvent.BUTTON_MIDDLE || 
					(draggingModifier && event.getButton() == GSMouseEvent.BUTTON_LEFT))) {
				draggingContent = true;
				event.consume();
			}
		}

		@Override
		public void mouseReleased(GSMouseEvent event) {
			draggingContent = false;
		}
		
		@Override
		public void mouseDragged(GSMouseEvent event) {
			if (draggingContent) {
				if (draggableX) {
					GSScrollBar hsb = scrollPanel.getHorizontalScrollBar();
					hsb.setScroll(hsb.getScroll() - event.getDragX());
				}

				if (draggableY) {
					GSScrollBar vsb = scrollPanel.getVerticalScrollBar();
					vsb.setScroll(vsb.getScroll() - event.getDragY());
				}

				event.consume();
			}
		}
		
		@Override
		public void keyPressed(GSKeyEvent event) {
			if (event.getKeyCode() == GSKeyEvent.KEY_SPACE) {
				draggingModifier = true;
				event.consume();
			}
		}

		@Override
		public void keyReleased(GSKeyEvent event) {
			if (event.getKeyCode() == GSKeyEvent.KEY_SPACE) {
				draggingModifier = false;
				event.consume();
			}
		}
		
		@Override
		public void focusLost(GSFocusEvent event) {
			draggingModifier = false;
			draggingContent = false;
		}
	}
}
