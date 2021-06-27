package com.g4mesoft.captureplayback.module;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDeltaListener;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaException;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaTransformer;

public class GSSequenceUndoRedoHistory implements GSISequenceDeltaListener {

	private static final int MAX_HISTORY_SIZE = 100000;
	private static final long MAX_CHAINED_INTERVAL = 250L;
	
	private final Deque<GSEntry> undoHistory;
	private final Deque<GSEntry> redoHistory;
	
	private final GSSequenceDeltaTransformer transformer;
	private boolean tracking;

	private List<GSISequenceUndoRedoListener> listeners;
	
	private GSSequence sequence;
	
	public GSSequenceUndoRedoHistory() {
		undoHistory = new LinkedList<GSEntry>();
		redoHistory = new LinkedList<GSEntry>();
	
		transformer = new GSSequenceDeltaTransformer();
		tracking = false;
		
		listeners = new ArrayList<GSISequenceUndoRedoListener>(1);
		
		sequence = null;
	}

	public void install(GSSequence sequence) {
		this.sequence = sequence;
		
		transformer.install(sequence);
		transformer.addDeltaListener(this);

		startTracking();
	}

	public void uninstall(GSSequence sequence) {
		stopTracking();

		transformer.uninstall(sequence);
		transformer.removeDeltaListener(this);
	
		sequence = null;
	}
	
	public void stopTracking() {
		transformer.setEnabled(false);
		tracking = false;
	}

	public void startTracking() {
		transformer.setEnabled(true);
		tracking = true;
	}
	
	public void addUndoRedoListener(GSISequenceUndoRedoListener listener) {
		listeners.add(listener);
	}

	public void removeUndoRedoListener(GSISequenceUndoRedoListener listener) {
		listeners.remove(listener);
	}
	
	private void invokeHistoryChangedEvent() {
		listeners.forEach(GSISequenceUndoRedoListener::onHistoryChanged);
	}
	
	public boolean undo() {
		return applyHistory(undoHistory, redoHistory, this::undoEntry);
	}

	private void undoEntry(GSEntry entry) throws GSSequenceDeltaException {
		entry.delta.unapplyDelta(sequence);
	}

	public boolean redo() {
		return applyHistory(redoHistory, undoHistory, this::redoEntry);
	}
	
	private void redoEntry(GSEntry entry) throws GSSequenceDeltaException {
		entry.delta.applyDelta(sequence);
	}

	public boolean applyHistory(Deque<GSEntry> srcHistory, Deque<GSEntry> dstHistory, GSAction action) {
		boolean success = true;
		
		if (!srcHistory.isEmpty()) {
			boolean wasTracking = tracking;
			
			GSEntry entry;
			do {
				entry = srcHistory.getLast();

				try {
					stopTracking();
					action.apply(entry);
				} catch (GSSequenceDeltaException e) {
					// Unable to apply, break until it is possible.
					success = false;
					break;
				} finally {
					if (wasTracking)
						startTracking();
				}
				
				dstHistory.addLast(srcHistory.removeLast());
			} while (!srcHistory.isEmpty() && srcHistory.getLast().isChained(entry));

			invokeHistoryChangedEvent();
		}
		
		return success;
	}
	
	public boolean hasUndoHistory() {
		return !undoHistory.isEmpty();
	}

	public boolean hasRedoHistory() {
		return !redoHistory.isEmpty();
	}
	
	@Override
	public void onSequenceDelta(GSISequenceDelta delta) {
		if (redoHistory.size() != 0)
			redoHistory.clear();
		
		undoHistory.addLast(new GSEntry(delta, System.currentTimeMillis()));

		// Ensure that the size of the history in limited to MAX_HISTORY_SIZE.
		if (undoHistory.size() > MAX_HISTORY_SIZE)
			undoHistory.removeFirst();
		
		invokeHistoryChangedEvent();
	}
	
	private static class GSEntry {

		private final GSISequenceDelta delta;
		private final long timestampMillis;
		
		public GSEntry(GSISequenceDelta delta, long timestampMillis) {
			this.delta = delta;
			this.timestampMillis = timestampMillis;
		}
		
		public boolean isChained(GSEntry other) {
			return Math.abs(other.timestampMillis - timestampMillis) <= MAX_CHAINED_INTERVAL;
		}
	}
	
	private static interface GSAction {
		
		public void apply(GSEntry entry) throws GSSequenceDeltaException;
		
	}
}
