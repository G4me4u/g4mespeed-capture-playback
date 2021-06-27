package com.g4mesoft.captureplayback.sequence;

import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;

public interface GSISequenceListener {

	default public void sequenceNameChanged(String oldName) {
	}
	
	default public void channelAdded(GSChannel channel, UUID prevUUID) {
	}

	default public void channelRemoved(GSChannel channel, UUID oldPrevUUID) {
	}

	default public void channelMoved(GSChannel channel, UUID newPrevUUID, UUID oldPrevUUID) {
	}
	
	default public void channelInfoChanged(GSChannel channel, GSChannelInfo oldInfo) {
	}

	default public void channelDisabledChanged(GSChannel channel, boolean oldDisabled) {
	}

	default public void entryAdded(GSChannelEntry entry) {
	}

	default public void entryRemoved(GSChannelEntry entry) {
	}

	default public void entryTimeChanged(GSChannelEntry entry, GSSignalTime oldStart, GSSignalTime oldEnd) {
	}

	default public void entryTypeChanged(GSChannelEntry entry, GSEChannelEntryType oldType) {
	}
}
