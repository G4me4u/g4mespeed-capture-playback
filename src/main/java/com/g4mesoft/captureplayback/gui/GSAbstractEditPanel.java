package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.core.client.GSControllerClient;
import com.g4mesoft.panel.GSClosableParentPanel;
import com.g4mesoft.panel.GSECursorType;
import com.g4mesoft.panel.GSETextAlignment;
import com.g4mesoft.panel.GSIActionListener;
import com.g4mesoft.panel.GSIcon;
import com.g4mesoft.panel.GSPanelContext;
import com.g4mesoft.panel.GSRectangle;
import com.g4mesoft.panel.GSTexturedIcon;
import com.g4mesoft.panel.button.GSButton;
import com.g4mesoft.panel.event.GSFocusEvent;
import com.g4mesoft.panel.event.GSIFocusEventListener;
import com.g4mesoft.panel.event.GSIKeyListener;
import com.g4mesoft.panel.event.GSKeyEvent;
import com.g4mesoft.panel.field.GSTextField;
import com.g4mesoft.panel.legend.GSButtonPanel;
import com.g4mesoft.renderer.GSIRenderer2D;
import com.g4mesoft.renderer.GSTexture;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public abstract class GSAbstractEditPanel extends GSClosableParentPanel {

	private static final Identifier BACKARROW_TEXTURE_IDENTIFIER = new Identifier("g4mespeed/captureplayback/textures/backarrow.png");
	private static final GSTexture BACKARROW_TEXTURE = new GSTexture(BACKARROW_TEXTURE_IDENTIFIER, 9, 9);
	
	private static final int TITLE_HEIGHT = 25;
	private static final int MAXIMUM_TITLE_WIDTH = 250;
	private static final int TITLE_MARGIN = 5;
	
	private static final int TITLE_SEPARATOR_COLOR = 0xFF444444;
	private static final int TITLE_BACKGROUND_COLOR = 0xFF171717;
	
	private static final GSIcon BACK_ICON = new GSTexturedIcon(BACKARROW_TEXTURE);
	private static final Text BACK_TEXT = new TranslatableText("panel.edit.back");
	
	protected final GSButton backButton;
	protected final GSTextField nameField;

	public GSAbstractEditPanel() {
		backButton = new GSButton(BACK_ICON, BACK_TEXT);
		backButton.setBackgroundColor(0x00000000);
		backButton.setHoveredBackgroundColor(0x00000000);
		backButton.setBorderWidth(0);
		backButton.setCursor(GSECursorType.HAND);
		backButton.addActionListener(new GSIActionListener() {
			@Override
			public void actionPerformed() {
				GSPanelContext.setContent(GSControllerClient.getInstance().getTabbedGUI());
			}
		});
		
		nameField = new GSTextField();
		nameField.setBackgroundColor(0x00000000);
		nameField.setTextAlignment(GSETextAlignment.CENTER);
		nameField.setBorderWidth(0);
		nameField.setVerticalMargin(TITLE_MARGIN);
		nameField.setHorizontalMargin(0);
		
		nameField.addFocusEventListener(new GSIFocusEventListener() {
			@Override
			public void focusLost(GSFocusEvent event) {
				if (!nameField.hasPopupVisible()) {
					handleNameChanged(nameField.getText());
					nameField.getCaret().setCaretLocation(0);
					event.consume();
				}
			}
		});
		
		nameField.addKeyEventListener(new GSIKeyListener() {
			@Override
			public void keyPressed(GSKeyEvent event) {
				if (!event.isRepeating()) {
					switch (event.getKeyCode()) {
					case GSKeyEvent.KEY_ENTER:
						handleNameChanged(nameField.getText());
					case GSKeyEvent.KEY_ESCAPE:
						nameField.unfocus();
						event.consume();
					default:
						break;
					}
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
		
		backButton.setBounds(new GSRectangle(0, (TITLE_HEIGHT - GSButtonPanel.BUTTON_HEIGHT) / 2, backButton.getPreferredSize()));
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
