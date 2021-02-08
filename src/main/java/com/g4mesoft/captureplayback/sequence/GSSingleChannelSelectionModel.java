package com.g4mesoft.captureplayback.sequence;

import java.util.UUID;

public class GSSingleChannelSelectionModel implements GSIChannelSelectionModel {

	private UUID selectedChannelUUID;
	
	public GSSingleChannelSelectionModel() {
		selectedChannelUUID = null;
	}
	
	@Override
	public void setSelectedChannel(UUID channelUUID) {
		selectedChannelUUID = channelUUID;
	}

	@Override
	public boolean isChannelSelected(UUID channelUUID) {
		return (selectedChannelUUID == channelUUID);
	}
	
	@Override
	public boolean hasChannelSelection() {
		return (selectedChannelUUID != null);
	}
}
