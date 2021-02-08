package com.g4mesoft.captureplayback.sequence;

import java.util.UUID;

public interface GSIChannelSelectionModel {

	public void setSelectedChannel(UUID channelUUID);

	public boolean isChannelSelected(UUID channelUUID);
	
	public boolean hasChannelSelection();
	
}
