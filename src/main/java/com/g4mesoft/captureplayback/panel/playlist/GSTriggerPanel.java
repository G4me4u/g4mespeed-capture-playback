package com.g4mesoft.captureplayback.panel.playlist;

import com.g4mesoft.captureplayback.playlist.GSETriggerType;
import com.g4mesoft.captureplayback.playlist.GSTrigger;

public class GSTriggerPanel extends GSAbstractPlaylistEntryPanel<GSETriggerType> {

	public GSTriggerPanel(GSTrigger trigger) {
		super(trigger, GSETriggerType.TYPES);
	}
}
