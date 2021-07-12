package com.g4mesoft.captureplayback.panel.composition;

import java.util.Iterator;

import com.g4mesoft.captureplayback.composition.GSTrackEntry;

public interface GSITrackEntrySelectionModel extends Iterable<GSTrackEntry> {

	public void select(GSTrackEntry entry);

	public void unselect(GSTrackEntry entry);

	public void unselectAll();
	
	public boolean isSelected(GSTrackEntry entry);

	public boolean hasSelection();
	
	@Override
	public Iterator<GSTrackEntry> iterator();
	
}
