package com.g4mesoft.captureplayback.panel.composition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;

public class GSTrackEntrySelectionModel implements GSITrackEntrySelectionModel {

	private final GSComposition composition;
	
	private final Map<UUID, Set<UUID>> trackSelections;

	public GSTrackEntrySelectionModel(GSComposition composition) {
		this.composition = composition;
		
		trackSelections = new HashMap<>();
	}

	@Override
	public void select(GSTrackEntry entry) {
		GSTrack track = entry.getParent();
		if (track != null) {
			Set<UUID> selections = trackSelections.get(track.getTrackUUID());
			if (selections == null) {
				selections = new HashSet<>();
				trackSelections.put(track.getTrackUUID(), selections);
			}

			selections.add(entry.getEntryUUID());
		}
	}

	@Override
	public void unselect(GSTrackEntry entry) {
		GSTrack track = entry.getParent();
		if (track != null) {
			Set<UUID> selections = trackSelections.get(track.getTrackUUID());
			if (selections != null) {
				selections.remove(entry.getEntryUUID());
				
				if (selections.isEmpty())
					trackSelections.remove(track.getTrackUUID());
			}
		}
	}

	@Override
	public void unselectAll() {
		trackSelections.clear();
	}

	@Override
	public boolean isSelected(GSTrackEntry entry) {
		GSTrack track = entry.getParent();
		if (track != null) {
			Set<UUID> selections = trackSelections.get(track.getTrackUUID());
			return (selections != null && selections.contains(entry.getEntryUUID()));
		}

		return false;
	}

	@Override
	public boolean hasSelection() {
		return !trackSelections.isEmpty();
	}
	
	@Override
	public Iterator<GSTrackEntry> iterator() {
		return new GSSelectionIterator();
	}
	
	final class GSSelectionIterator implements Iterator<GSTrackEntry> {
		
		private final Iterator<Map.Entry<UUID, Set<UUID>>> trackIterator;
		
		private GSTrack currentTrack;
		private Iterator<UUID> entryIterator;
		private GSTrackEntry nextEntry;
		
		public GSSelectionIterator() {
			trackIterator = trackSelections.entrySet().iterator();
			
			currentTrack = null;
			entryIterator = null;
		}
		
		@Override
		public boolean hasNext() {
			return findNextEntry() != null;
		}
		
		@Override
		public GSTrackEntry next() {
			GSTrackEntry entry = findNextEntry();
			if (entry == null)
				throw new NoSuchElementException();
			
			// Reset cached entry
			nextEntry = null;
			
			return entry;
		}
		
		private GSTrackEntry findNextEntry() {
			while (nextEntry == null) {
				if (currentTrack == null) {
					if (trackIterator.hasNext()) {
						Map.Entry<UUID, Set<UUID>> entry = trackIterator.next();
						currentTrack = composition.getTrack(entry.getKey());
						entryIterator = entry.getValue().iterator();
					}
	
					if (currentTrack == null)
						return null;
				}

				UUID entryUUID = entryIterator.next();
				nextEntry = currentTrack.getEntry(entryUUID);

				if (!entryIterator.hasNext()) {
					currentTrack = null;
					entryIterator = null;
				}
			}

			return nextEntry;
		}
	}
}
