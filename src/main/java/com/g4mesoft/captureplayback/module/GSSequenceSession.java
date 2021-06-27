package com.g4mesoft.captureplayback.module;

import java.util.UUID;

import com.g4mesoft.captureplayback.panel.sequence.GSESequenceOpacity;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSSequence;

public class GSSequenceSession {

	private final GSSequence activeSequence;
	
	/* Cached elements of the UI */
	private float xOffset;
	private float yOffset;
	private GSESequenceOpacity opacity;
	
	private UUID selectedChannelUUID;
	
	private final GSSequenceUndoRedoHistory undoRedoHistory;

	public GSSequenceSession(GSSequence activeSequence) {
		this.activeSequence = activeSequence;
		
		xOffset = yOffset = 0.0f;
		opacity = GSESequenceOpacity.OPACITY_90;
	
		selectedChannelUUID = null;
		
		undoRedoHistory = new GSSequenceUndoRedoHistory();
		undoRedoHistory.install(activeSequence);
	}
	
	public GSSequence getActiveSequence() {
		return activeSequence;
	}
	
	public float getXOffset() {
		return xOffset;
	}

	public void setXOffset(float xOffset) {
		this.xOffset = xOffset;
	}

	public float getYOffset() {
		return yOffset;
	}

	public void setYOffset(float yOffset) {
		this.yOffset = yOffset;
	}
	
	public GSESequenceOpacity getOpacity() {
		return opacity;
	}

	public void setOpacity(GSESequenceOpacity opacity) {
		this.opacity = opacity;
	}
	
	public UUID getSelectedChannelUUID() {
		return selectedChannelUUID;
	}

	public GSChannel getSelectedChannel() {
		return activeSequence.getChannel(selectedChannelUUID);
	}

	public void setSelectedChannelUUID(UUID channelUUID) {
		selectedChannelUUID = channelUUID;
	}

	public boolean isSelected(UUID channelUUID) {
		return channelUUID.equals(selectedChannelUUID);
	}
	
	public GSSequenceUndoRedoHistory getUndoRedoHistory() {
		return undoRedoHistory;
	}
}
