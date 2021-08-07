package com.g4mesoft.captureplayback.sequence.delta;

import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaTransformer;
import com.g4mesoft.captureplayback.common.GSSignalTime;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSChannelEntry;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSEChannelEntryType;
import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;

public class GSSequenceDeltaTransformer extends GSDeltaTransformer<GSSequence> implements GSISequenceListener {

	@Override
	public void install(GSSequence model) {
		super.install(model);
		
		model.addSequenceListener(this);
	}
	
	public void uninstall(GSSequence model) {
		super.uninstall(model);
		
		model.removeSequenceListener(this);
	}
	
	@Override
	public void sequenceNameChanged(String oldName) {
		dispatchDeltaEvent(new GSSequenceNameDelta(model.getName(), oldName));
	}
	
	@Override
	public void channelAdded(GSChannel channel, UUID prevUUID) {
		dispatchDeltaEvent(new GSChannelAddedDelta(channel, prevUUID));
	}

	@Override
	public void channelRemoved(GSChannel channel, UUID oldPrevUUID) {
		dispatchDeltaEvent(new GSChannelRemovedDelta(channel, oldPrevUUID));
	}
	
	@Override
	public void channelMoved(GSChannel channel, UUID newPrevUUID, UUID oldPrevUUID) {
		dispatchDeltaEvent(new GSChannelMovedDelta(channel.getChannelUUID(), newPrevUUID, oldPrevUUID));
	}
	
	@Override
	public void channelInfoChanged(GSChannel channel, GSChannelInfo oldInfo) {
		dispatchDeltaEvent(new GSChannelInfoDelta(channel.getChannelUUID(), channel.getInfo(), oldInfo));
	}

	@Override
	public void channelDisabledChanged(GSChannel channel, boolean oldDisabled) {
		dispatchDeltaEvent(new GSChannelDisabledDelta(channel.getChannelUUID(), channel.isDisabled(), oldDisabled));
	}

	@Override
	public void entryAdded(GSChannelEntry entry) {
		dispatchDeltaEvent(new GSChannelEntryAddedDelta(entry));
	}

	@Override
	public void entryRemoved(GSChannelEntry entry) {
		dispatchDeltaEvent(new GSChannelEntryRemovedDelta(entry));
	}

	@Override
	public void entryTimeChanged(GSChannelEntry entry, GSSignalTime oldStart, GSSignalTime oldEnd) {
		dispatchDeltaEvent(new GSChannelEntryTimeDelta(entry.getParent().getChannelUUID(),
				entry.getEntryUUID(), entry.getStartTime(), entry.getEndTime(), oldStart, oldEnd));
	}

	@Override
	public void entryTypeChanged(GSChannelEntry entry, GSEChannelEntryType oldType) {
		dispatchDeltaEvent(new GSChannelEntryTypeDelta(entry.getParent().getChannelUUID(),
				entry.getEntryUUID(), entry.getType(), oldType));
	}
}
