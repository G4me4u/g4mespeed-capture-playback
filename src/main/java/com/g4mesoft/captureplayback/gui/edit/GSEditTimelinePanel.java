package com.g4mesoft.captureplayback.gui.edit;

import com.g4mesoft.captureplayback.gui.timeline.GSTimelinePanel;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.gui.GSBasePanel;
import com.g4mesoft.gui.GSElementContext;
import com.g4mesoft.gui.GSIElement;
import com.g4mesoft.gui.action.GSButtonPanel;
import com.g4mesoft.gui.text.GSTextField;
import com.g4mesoft.gui.text.GSTextLabel;
import com.g4mesoft.renderer.GSIRenderer2D;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class GSEditTimelinePanel extends GSBasePanel {

	private static final int TITLE_HEIGHT = 30;
	private static final int MAXIMUM_TITLE_WIDTH = 250;
	private static final int BACK_BUTTON_WIDTH = 45;
	private static final int TITLE_MARGIN = 5;
	
	private static final int TITLE_SEPARATOR_COLOR = 0xFF444444;
	private static final int TITLE_BACKGROUND_COLOR = 0xFF171717;
	
	private static final String TIMELINE_LABEL_TEXT = "Timeline - ";
	private static final Text BACK_TEXT = new LiteralText("< BACK");
	
	private final GSTimelinePanel timelineGUI;
	private final GSButtonPanel backButton;
	private final GSTextLabel titleLabel;
	private final GSTextField titleField;

	public GSEditTimelinePanel(GSTimeline timeline, GSCapturePlaybackModule module) {
		timelineGUI = new GSTimelinePanel(timeline, new GSDefaultTrackProvider());
		timelineGUI.setEditable(true);
		
		backButton = new GSButtonPanel(BACK_TEXT, () -> {
			GSElementContext.setContent(null);
		});
		
		titleLabel = new GSTextLabel(new TranslatableText(TIMELINE_LABEL_TEXT));
		titleField = new GSTextField(timeline.getName());
		
		add(timelineGUI);
		add(backButton);
		add(titleLabel);
		add(titleField);
	}

	@Override
	public void onAdded(GSIElement parent) {
		super.onAdded(parent);

		timelineGUI.requestFocus();
	}
	
	@Override
	public void onBoundsChanged() {
		timelineGUI.setBounds(0, TITLE_HEIGHT, width, height - TITLE_HEIGHT);
		
		backButton.setPreferredBounds(0, (TITLE_HEIGHT - GSButtonPanel.BUTTON_HEIGHT) / 2, BACK_BUTTON_WIDTH);
		titleLabel.setPreferredBounds(backButton.x + backButton.width + TITLE_MARGIN, 0, TITLE_HEIGHT);
		titleField.setPreferredBounds(titleLabel.x + titleLabel.width + TITLE_MARGIN, 0, MAXIMUM_TITLE_WIDTH);
	}

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
