package com.g4mesoft.captureplayback.composition.delta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDeltaListener;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaTransformer;

public class GSCompositionDeltaTransformer implements GSICompositionListener {

	private final List<GSICompositionDeltaListener> listeners;
	
	private final Map<UUID, GSTrackSequenceDeltaListener> sequenceDeltaListeners;
	
	private GSComposition composition;
	private boolean enabled;
	
	public GSCompositionDeltaTransformer() {
		listeners = new ArrayList<>();
		
		sequenceDeltaListeners = new HashMap<>();
	
		composition = null;
		enabled = true;
	}
	
	public void addDeltaListener(GSICompositionDeltaListener listener) {
		listeners.add(listener);
	}

	public void removeDeltaListener(GSICompositionDeltaListener listener) {
		listeners.remove(listener);
	}
	
	public void install(GSComposition composition) {
		if (this.composition != null)
			throw new IllegalStateException("Already installed");
		
		this.composition = composition;

		for (GSTrack track : composition.getTracks())
			installSequenceListener(track);
	
		composition.addCompositionListener(this);
	}
	
	public void uninstall(GSComposition composition) {
		if (this.composition == null)
			throw new IllegalStateException("Not installed");
		if (this.composition != composition)
			throw new IllegalStateException("Composition is not the one that is installed");
		
		this.composition.removeCompositionListener(this);
		
		for (GSTrack track : this.composition.getTracks())
			uninstallSequenceListener(track);
			
		this.composition = null;
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
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		if (enabled != this.enabled) {
			this.enabled = enabled;
			
			for (GSTrackSequenceDeltaListener listener : sequenceDeltaListeners.values())
				listener.setEnabled(enabled);
		}
	}
	
	@Override
	public void compositionNameChanged(String oldName) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSCompositionNameDelta(composition.getName(), oldName));
	}

	@Override
	public void groupAdded(GSTrackGroup group) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSGroupAddedDelta(group));
	}

	@Override
	public void groupRemoved(GSTrackGroup group) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSGroupRemovedDelta(group));
	}
	
	@Override
	public void groupNameChanged(GSTrackGroup group, String oldName) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSGroupNameDelta(group.getGroupUUID(), group.getName(), oldName));
	}
	
	@Override
	public void trackAdded(GSTrack track) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSTrackAddedDelta(track));
		
		installSequenceListener(track);
	}

	@Override
	public void trackRemoved(GSTrack track) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSTrackRemovedDelta(track));

		uninstallSequenceListener(track);
	}

	@Override
	public void trackNameChanged(GSTrack track, String oldName) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSTrackNameDelta(track.getTrackUUID(), track.getName(), oldName));
	}

	@Override
	public void trackColorChanged(GSTrack track, int oldColor) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSTrackColorDelta(track.getTrackUUID(), track.getColor(), oldColor));
	}

	@Override
	public void trackGroupChanged(GSTrack track, UUID oldGroupUUID) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSTrackGroupDelta(track.getTrackUUID(), track.getGroupUUID(), oldGroupUUID));
	}
	
	@Override
	public void entryAdded(GSTrackEntry entry) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSTrackEntryAddedDelta(entry));
	}

	@Override
	public void entryRemoved(GSTrackEntry entry) {
		if (enabled)
			dispatchCompositionDeltaEvent(new GSTrackEntryRemovedDelta(entry));
	}

	@Override
	public void entryOffsetChanged(GSTrackEntry entry, long oldOffset) {
		if (enabled) {
			dispatchCompositionDeltaEvent(new GSTrackEntryOffsetDelta(entry.getParent().getTrackUUID(), 
					entry.getEntryUUID(), entry.getOffset(), oldOffset));
		}
	}
	
	private void dispatchCompositionDeltaEvent(GSICompositionDelta delta) {
		for (GSICompositionDeltaListener listener : listeners)
			listener.onCompositionDelta(delta);
	}
	
	private class GSTrackSequenceDeltaListener implements GSISequenceDeltaListener {

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
		public void onSequenceDelta(GSISequenceDelta delta) {
			dispatchCompositionDeltaEvent(new GSTrackSequenceDelta(trackUUID, delta));
		}
	}
}
