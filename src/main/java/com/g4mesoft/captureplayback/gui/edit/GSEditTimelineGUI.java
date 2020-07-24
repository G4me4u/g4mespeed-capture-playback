package com.g4mesoft.captureplayback.gui.edit;

import com.g4mesoft.captureplayback.gui.timeline.GSTimelineGUI;
import com.g4mesoft.captureplayback.module.GSCapturePlaybackModule;
import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.gui.GSBasePanel;
import com.g4mesoft.gui.GSIElement;
import com.g4mesoft.gui.renderer.GSIRenderer2D;

public class GSEditTimelineGUI extends GSBasePanel {

	private final GSTimelineGUI timelineGUI;

	public GSEditTimelineGUI(GSTimeline timeline, GSCapturePlaybackModule module) {
		timelineGUI = new GSTimelineGUI(timeline, new DefaultTrackProvider());
		timelineGUI.setEditable(true);

		add(timelineGUI);
	}

	@Override
	public void onAdded(GSIElement parent) {
		super.onAdded(parent);

		timelineGUI.requestFocus();
	}
	
	@Override
	public void onBoundsChanged() {
		timelineGUI.setBounds(0, 0, width, height);
	}

	@Override
	public void render(GSIRenderer2D renderer) {
		renderBackground(renderer);

		super.render(renderer);
	}
}
