package com.g4mesoft.captureplayback.sequence.delta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;

public class GSSequenceDeltaTransformer implements GSISequenceListener {

	private final List<GSISequenceDeltaListener> listeners;
	
	private GSSequence sequence;
	private boolean enabled;
	
	public GSSequenceDeltaTransformer() {
		listeners = new ArrayList<>();
	
		sequence = null;
		enabled = true;
	}
	
	public void addDeltaListener(GSISequenceDeltaListener listener) {
		listeners.add(listener);
	}

	public void removeDeltaListener(GSISequenceDeltaListener listener) {
		listeners.remove(listener);
	}
	
	public void install(GSSequence sequence) {
		if (this.sequence != null)
			throw new IllegalStateException("Already installed");
		
		this.sequence = sequence;
	
		sequence.addSequenceListener(this);
	}
	
	public void uninstall(GSSequence sequence) {
		if (this.sequence == null)
			throw new IllegalStateException("Not installed");
		if (this.sequence != sequence)
			throw new IllegalStateException("Sequence is not the one that is installed");
		
		this.sequence.removeSequenceListener(this);
		
		this.sequence = null;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public void sequenceNameChanged(String oldName) {
		if (enabled)
			dispatchSequenceDeltaEvent(new GSSequenceNameDelta(sequence.getName(), oldName));
	}
	
	@Override
	public void channelAdded(GSChannel channel, UUID prevUUID) {
		if (enabled)
			dispatchSequenceDeltaEvent(new GSChannelAddedDelta(channel, prevUUID));
	}

	@Override
	public void channelRemoved(GSChannel channel, UUID oldPrevUUID) {
		if (enabled)
			dispatchSequenceDeltaEvent(new GSChannelRemovedDelta(channel, oldPrevUUID));
	}
	
	@Override
	public void channelMoved(GSChannel channel, UUID newPrevUUID, UUID oldPrevUUID) {
		if (enabled)
			dispatchSequenceDeltaEvent(new GSChannelMovedDelta(channel.getChannelUUID(), newPrevUUID, oldPrevUUID));
	}
	
	@Override
	public void channelInfoChanged(GSChannel channel, GSChannelInfo oldInfo) {
		if (enabled)
			dispatchSequenceDeltaEvent(new GSChannelInfoDelta(channel.getChannelUUID(), channel.getInfo(), oldInfo));
	}

	@Override
	public void channelDisabledChanged(GSChannel channel, boolean oldDisabled) {
		if (enabled)
			dispatchSequenceDeltaEvent(new GSChannelDisabledDelta(channel.getChannelUUID(), channel.isDisabled(), oldDisabled));
	}

	@Override
	public void entryAdded(GSChannelEntry entry) {
		if (enabled)
			dispatchSequenceDeltaEvent(new GSEntryAddedDelta(entry));
	}

	@Override
	public void entryRemoved(GSChannelEntry entry) {
		if (enabled)
			dispatchSequenceDeltaEvent(new GSEntryRemovedDelta(entry));
	}

	@Override
	public void entryTimeChanged(GSChannelEntry entry, GSSignalTime oldStart, GSSignalTime oldEnd) {
		if (enabled) {
			dispatchSequenceDeltaEvent(new GSEntryTimeDelta(entry.getParent().getChannelUUID(),
					entry.getEntryUUID(), entry.getStartTime(), entry.getEndTime(), oldStart, oldEnd));
		}
	}

	@Override
	public void entryTypeChanged(GSChannelEntry entry, GSEChannelEntryType oldType) {
		if (enabled) {
			dispatchSequenceDeltaEvent(new GSEntryTypeDelta(entry.getParent().getChannelUUID(),
					entry.getEntryUUID(), entry.getType(), oldType));
		}
	}
	
	private void dispatchSequenceDeltaEvent(GSISequenceDelta delta) {
		for (GSISequenceDeltaListener listener : listeners)
			listener.onSequenceDelta(delta);
	}
}
