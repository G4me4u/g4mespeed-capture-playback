package com.g4mesoft.captureplayback.panel.sequence;

import com.g4mesoft.captureplayback.module.GSSequenceSession;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.panel.GSDimension;
import com.g4mesoft.panel.GSECursorType;
import com.g4mesoft.panel.GSIcon;
import com.g4mesoft.panel.GSLocation;
import com.g4mesoft.panel.GSPanelContext;
import com.g4mesoft.panel.GSPanelUtil;
import com.g4mesoft.panel.GSParentPanel;
import com.g4mesoft.panel.GSPopup;
import com.g4mesoft.panel.button.GSButton;
import com.g4mesoft.panel.dropdown.GSDropdown;
import com.g4mesoft.panel.dropdown.GSDropdownAction;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.text.LiteralText;

public class GSSequenceInfoPanel extends GSParentPanel {

	private static final GSIcon DOWN_ARROW_ICON = GSPanelContext.getIcon(30, 62, 10, 10);
	private static final GSIcon HOVERED_DOWN_ARROW_ICON = GSPanelContext.getIcon(40, 62, 10, 10);
	private static final GSIcon DISABLED_DOWN_ARROW_ICON = GSPanelContext.getIcon(50, 62, 10, 10);
	
	private final GSSequenceSession session;
	private final GSButton channelAction;
	
	public GSSequenceInfoPanel(GSSequenceSession session) {
		this.session = session;
		
		channelAction = new GSButton(DOWN_ARROW_ICON, "Edit Channel");
		channelAction.setHoveredIcon(HOVERED_DOWN_ARROW_ICON);
		channelAction.setDisabledIcon(DISABLED_DOWN_ARROW_ICON);
		channelAction.setCursor(GSECursorType.HAND);
		channelAction.setBackgroundColor(0x00000000);
		channelAction.setHoveredBackgroundColor(0x00000000);
		channelAction.setDisabledBackgroundColor(0x00000000);
		channelAction.setBorderWidth(0);
		channelAction.setHorizontalMargin(1);
		channelAction.setIconSpacing(4);
		channelAction.setClickSound(null);
		
		channelAction.addActionListener(this::onChannelAction);
		
		add(channelAction);
	}
	
	@Override
	protected void onBoundsChanged() {
		super.onBoundsChanged();

		GSDimension actionSize = channelAction.getPreferredSize();
		channelAction.setBounds(0, height - actionSize.getHeight(),
				actionSize.getWidth(), actionSize.getHeight());
	}
	
	@Override
	public void render(GSIRenderer2D renderer) {
		renderer.fillRect(0, 0, width, height, GSChannelHeaderPanel.CHANNEL_HEADER_COLOR);

		renderer.drawVLine(width - 1, 0, height, GSSequenceColumnHeaderPanel.COLUMN_LINE_COLOR);
		renderer.drawHLine(0, width, height - 1, GSSequenceContentPanel.CHANNEL_SPACING_COLOR);

		super.render(renderer);
	}
	
	private void onChannelAction() {
		GSDropdown dropdown = new GSDropdown();
		
		dropdown.addItem(new GSDropdownAction(new LiteralText("Test #1"), () -> {
			GSChannel channel = session.getSelectedChannel();
			
			if (channel != null) {
				GSChannelEditorPanel channelEditor = new GSChannelEditorPanel(channel);
				channelEditor.show(getParent());
			}
		}));
		dropdown.addItem(new GSDropdownAction(new LiteralText("Test #2"), null));
		dropdown.addItem(new GSDropdownAction(new LiteralText("Test #3"), null));
		
		GSLocation viewLocation = GSPanelUtil.getViewLocation(channelAction);
		int px = viewLocation.getX() + 15;
		int py = viewLocation.getY() + channelAction.getHeight() - channelAction.getVerticalMargin();

		GSPopup popup = new GSPopup(dropdown);
		popup.show(channelAction, px, py);
	}
}
