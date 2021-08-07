package com.g4mesoft.captureplayback.composition.delta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaTransformer;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.GSIDeltaListener;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaTransformer;

public class GSCompositionDeltaTransformer extends GSDeltaTransformer<GSComposition> implements GSICompositionListener {

	private final Map<UUID, GSTrackSequenceDeltaListener> sequenceDeltaListeners;
	
	public GSCompositionDeltaTransformer() {
		sequenceDeltaListeners = new HashMap<>();
	}
	
	@Override
	public void install(GSComposition model) {
		super.install(model);
		
		for (GSTrack track : model.getTracks())
			installSequenceListener(track);
	
		model.addCompositionListener(this);
	}
	
	@Override
	public void uninstall(GSComposition model) {
		super.uninstall(model);
		
		model.removeCompositionListener(this);
		
		for (GSTrack track : model.getTracks())
			uninstallSequenceListener(track);
	}
	
	private void installSequenceListener(GSTrack track) {
		if (!sequenceDeltaListeners.containsKey(track.getTrackUUID())) {
			GSTrackSequenceDeltaListener listener = new GSTrackSequenceDeltaListener(track);
			listener.install();
			sequenceDeltaListeners.put(track.getTrackUUID(), listener);
		}
	}

	private void uninstallSequenceListener(GSTrack track) {
		GSTrackSequenceDeltaListener listener = sequenceDeltaListeners.remove(track.getTrackUUID());
		if (listener != null)
			listener.uninstall();
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (enabled != isEnabled()) {
			super.setEnabled(enabled);
			
			for (GSTrackSequenceDeltaListener listener : sequenceDeltaListeners.values())
				listener.setEnabled(enabled);
		}
	}
	
	@Override
	public void compositionNameChanged(String oldName) {
		dispatchDeltaEvent(new GSCompositionNameDelta(model.getName(), oldName));
	}

	@Override
	public void groupAdded(GSTrackGroup group) {
		dispatchDeltaEvent(new GSGroupAddedDelta(group));
	}

	@Override
	public void groupRemoved(GSTrackGroup group) {
		dispatchDeltaEvent(new GSGroupRemovedDelta(group));
	}
	
	@Override
	public void groupNameChanged(GSTrackGroup group, String oldName) {
		dispatchDeltaEvent(new GSGroupNameDelta(group.getGroupUUID(), group.getName(), oldName));
	}
	
	@Override
	public void trackAdded(GSTrack track) {
		dispatchDeltaEvent(new GSTrackAddedDelta(track));
		installSequenceListener(track);
	}

	@Override
	public void trackRemoved(GSTrack track) {
		dispatchDeltaEvent(new GSTrackRemovedDelta(track));
		uninstallSequenceListener(track);
	}

	@Override
	public void trackNameChanged(GSTrack track, String oldName) {
		dispatchDeltaEvent(new GSTrackNameDelta(track.getTrackUUID(), track.getName(), oldName));
	}

	@Override
	public void trackColorChanged(GSTrack track, int oldColor) {
		dispatchDeltaEvent(new GSTrackColorDelta(track.getTrackUUID(), track.getColor(), oldColor));
	}

	@Override
	public void trackGroupChanged(GSTrack track, UUID oldGroupUUID) {
		dispatchDeltaEvent(new GSTrackGroupDelta(track.getTrackUUID(), track.getGroupUUID(), oldGroupUUID));
	}
	
	@Override
	public void entryAdded(GSTrackEntry entry) {
		dispatchDeltaEvent(new GSTrackEntryAddedDelta(entry));
	}

	@Override
	public void entryRemoved(GSTrackEntry entry) {
		dispatchDeltaEvent(new GSTrackEntryRemovedDelta(entry));
	}

	@Override
	public void entryOffsetChanged(GSTrackEntry entry, long oldOffset) {
		dispatchDeltaEvent(new GSTrackEntryOffsetDelta(entry.getParent().getTrackUUID(), 
				entry.getEntryUUID(), entry.getOffset(), oldOffset));
	}
	
	private class GSTrackSequenceDeltaListener implements GSIDeltaListener<GSSequence> {

		private final UUID trackUUID;
		private final GSSequence sequence;
		
		private final GSSequenceDeltaTransformer transformer;
		
		public GSTrackSequenceDeltaListener(GSTrack track) {
			trackUUID = track.getTrackUUID();
			sequence = track.getSequence();
			
			transformer = new GSSequenceDeltaTransformer();
			transformer.addDeltaListener(this);
		}

		public void install() {
			transformer.install(sequence);
		}

		public void uninstall() {
			transformer.uninstall(sequence);
		}
		
		public void setEnabled(boolean enabled) {
			transformer.setEnabled(enabled);
		}
		
		@Override
		public void onDelta(GSIDelta<GSSequence> delta) {
			dispatchDeltaEvent(new GSTrackSequenceDelta(trackUUID, delta));
		}
	}
}
