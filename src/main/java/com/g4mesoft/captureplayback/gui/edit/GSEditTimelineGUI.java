package com.g4mesoft.captureplayback.gui.edit;

import com.g4mesoft.captureplayback.gui.timeline.GSTimelineGUI;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.core.GSCoreOverride;
import com.g4mesoft.gui.GSScreen;

public class GSEditTimelineGUI extends GSScreen {

	private final GSTimeline timeline;
	private final GSTimelineGUI timelineGUI;

	public GSEditTimelineGUI(GSTimeline timeline, GSCapturePlaybackModule module) {
		this.timeline = timeline;
		
		timelineGUI = new GSTimelineGUI(timeline, new DefaultTrackProvider());
		timelineGUI.setEditable(true);
	}

	@Override
	@GSCoreOverride
	public void init() {
		super.init();

		timelineGUI.initBounds(minecraft, 0, 0, width, height);
		addPanel(timelineGUI);

		setFocused(timelineGUI);
	}

	@Override
	@GSCoreOverride
	public void render(int mouseX, int mouseY, float partialTicks) {
		renderBackground();

		super.render(mouseX, mouseY, partialTicks);
	}

	@Override
	@GSCoreOverride
	public boolean shouldCloseOnEsc() {
		return true;
	}
}
