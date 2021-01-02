package com.g4mesoft.captureplayback.gui.edit;

import com.g4mesoft.captureplayback.gui.timeline.GSTimelinePanel;
import com.g4mesoft.captureplayback.timeline.GSTimeline;

public class GSTimelineEditPanel extends GSAbstractEditPanel {

	private final GSTimeline timeline;
	
	public GSTimelineEditPanel(GSTimeline timeline) {
		super(new GSTimelinePanel(timeline, new GSDefaultTrackProvider()));
	
		this.timeline = timeline;
		
		nameField.setText(timeline.getName());
	}
	
	@Override
	protected void handleNameChanged(String name) {
		timeline.setName(name);
	}
}
