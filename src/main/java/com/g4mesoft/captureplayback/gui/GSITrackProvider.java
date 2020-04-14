package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.timeline.GSTimeline;
import com.g4mesoft.captureplayback.timeline.GSTrackInfo;

public interface GSITrackProvider {

	public GSTrackInfo createNewTrackInfo(GSTimeline timeline);
	
}
