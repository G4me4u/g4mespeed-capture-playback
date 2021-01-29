package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.panel.GSClosableParentPanel;
import com.g4mesoft.panel.event.GSFocusEvent;
import com.g4mesoft.panel.event.GSIFocusEventListener;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.legend.GSButtonPanel;
import com.g4mesoft.panel.text.GSETextAlignment;
import com.g4mesoft.panel.text.GSTextField;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public abstract class GSAbstractEditPanel extends GSClosableParentPanel {

	private static final int TITLE_HEIGHT = 25;
	private static final int MAXIMUM_TITLE_WIDTH = 250;
	private static final int BACK_BUTTON_WIDTH = 45;
	private static final int TITLE_MARGIN = 5;
	
	private static final int TITLE_SEPARATOR_COLOR = 0xFF444444;
	private static final int TITLE_BACKGROUND_COLOR = 0xFF171717;
	
	private static final Text BACK_TEXT = new LiteralText("< BACK");
	
	protected final GSButtonPanel backButton;
	protected final GSTextField nameField;

	public GSAbstractEditPanel() {
		backButton = new GSButtonPanel(BACK_TEXT, this::close);
		
		nameField = new GSTextField();
		nameField.setBackgroundColor(0x00000000);
		nameField.setTextAlignment(GSETextAlignment.CENTER);
		nameField.setBorderWidth(0);
		nameField.setVerticalMargin(TITLE_MARGIN);
		nameField.setHorizontalMargin(0);
		
		nameField.addFocusEventListener(new GSIFocusEventListener() {
			@Override
			public void focusLost(GSFocusEvent event) {
				handleNameChanged(nameField.getText());
				nameField.getCaret().setCaretLocation(0);
				event.consume();
			}
		});
		
		nameField.addKeyEventListener(new GSIKeyListener() {
			@Override
			public void keyPressed(GSKeyEvent event) {
				if (!event.isRepeating() && event.getKeyCode() == GSKeyEvent.KEY_ENTER) {
					handleNameChanged(nameField.getText());
					event.consume();
				}
			}
		});
		
		add(backButton);
		add(nameField);
	}
	
	protected abstract void handleNameChanged(String name);
	
	@Override
	public void onBoundsChanged() {
		layoutContent(0, TITLE_HEIGHT, width, Math.max(height - TITLE_HEIGHT, 0));
		
		backButton.setPreferredBounds(0, (TITLE_HEIGHT - GSButtonPanel.BUTTON_HEIGHT) / 2, BACK_BUTTON_WIDTH);
		nameField.setBounds((width - MAXIMUM_TITLE_WIDTH) / 2, 0, MAXIMUM_TITLE_WIDTH, TITLE_HEIGHT);
	}
	
	protected abstract void layoutContent(int x, int y, int width, int height);

	@Override
	public void render(GSIRenderer2D renderer) {
		renderTitle(renderer);

		super.render(renderer);
	}
	
	private void renderTitle(GSIRenderer2D renderer) {
		renderer.drawHLine(0, width, TITLE_HEIGHT - 1, TITLE_SEPARATOR_COLOR);
		renderer.fillRect(0, 0, width, TITLE_HEIGHT - 1, TITLE_BACKGROUND_COLOR);
	}
}
