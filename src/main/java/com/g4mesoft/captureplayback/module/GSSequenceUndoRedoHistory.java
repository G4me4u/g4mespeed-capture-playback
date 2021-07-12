package com.g4mesoft.captureplayback.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaException;

import net.minecraft.network.PacketByteBuf;

public class GSSequenceUndoRedoHistory {

	private static final int MAX_HISTORY_SIZE = 100000;
	private static final long MAX_CHAINED_INTERVAL = 250L;
	
	private final Deque<GSEntry> undoHistory;
	private final Deque<GSEntry> redoHistory;

	private boolean tracking;
	
	private List<GSISequenceUndoRedoListener> listeners;

	public GSSequenceUndoRedoHistory() {
		this(new LinkedList<>(), new LinkedList<>());
	}
	
	private GSSequenceUndoRedoHistory(Deque<GSEntry> undoHistory, Deque<GSEntry> redoHistory) {
		this.undoHistory = undoHistory;
		this.redoHistory = redoHistory;
	
		tracking = true;
		
		listeners = new ArrayList<GSISequenceUndoRedoListener>(1);
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
	
	public boolean undo(GSSequence sequence) {
		return applyHistory(undoHistory, redoHistory, entry -> {
			entry.delta.unapplyDelta(sequence);
		});
	}

	public boolean redo(GSSequence sequence) {
		return applyHistory(redoHistory, undoHistory, entry -> {
			entry.delta.applyDelta(sequence);
		});
	}
	
	public boolean applyHistory(Deque<GSEntry> srcHistory, Deque<GSEntry> dstHistory, GSAction action) {
		boolean success = true;
		
		if (!srcHistory.isEmpty()) {
			GSEntry entry;
			do {
				entry = srcHistory.getLast();

				try {
					tracking = false;
					action.apply(entry);
				} catch (GSSequenceDeltaException e) {
					// Unable to apply, break until it is possible.
					success = false;
					break;
				} finally {
					tracking = true;
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

	/* Visible for GSSequenceSession only! */
	void trackSequenceDelta(GSISequenceDelta delta) {
		if (tracking) {
			if (redoHistory.size() != 0)
				redoHistory.clear();
			
			undoHistory.addLast(new GSEntry(delta, System.currentTimeMillis()));
	
			// Ensure that the size of the history in limited to MAX_HISTORY_SIZE.
			if (undoHistory.size() > MAX_HISTORY_SIZE)
				undoHistory.removeFirst();
			
			invokeHistoryChangedEvent();
		}
	}

	public static GSSequenceUndoRedoHistory read(PacketByteBuf buf) throws IOException {
		Deque<GSEntry> undoHistory = new LinkedList<>();
		Deque<GSEntry> redoHistory = new LinkedList<>();
		
		int undoCount = buf.readInt();
		while (undoCount-- != 0)
			undoHistory.addLast(GSEntry.read(buf));
		int redoCount = buf.readInt();
		while (redoCount-- != 0)
			redoHistory.addLast(GSEntry.read(buf));
		
		return new GSSequenceUndoRedoHistory(undoHistory, redoHistory);
	}

	public static void write(PacketByteBuf buf, GSSequenceUndoRedoHistory history) throws IOException {
		buf.writeInt(history.undoHistory.size());
		for (GSEntry entry : history.undoHistory)
			GSEntry.write(buf, entry);
		
		buf.writeInt(history.redoHistory.size());
		for (GSEntry entry : history.redoHistory)
			GSEntry.write(buf, entry);
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
		
		public static GSEntry read(PacketByteBuf buf) throws IOException {
			GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
			
			GSISequenceDelta delta = extension.getSequenceDeltaRegistry().createNewElement(buf.readInt());
			if (delta == null)
				throw new IOException("Invalid delta ID");
			delta.read(buf);
			
			long timestampMillis = buf.readLong();
			
			return new GSEntry(delta, timestampMillis);
		}

		public static void write(PacketByteBuf buf, GSEntry entry) throws IOException {
			GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
			
			buf.writeInt(extension.getSequenceDeltaRegistry().getIdentifier(entry.delta));
			entry.delta.write(buf);
			
			buf.writeLong(entry.timestampMillis);
		}
	}
	
	private static interface GSAction {
		
		public void apply(GSEntry entry) throws GSSequenceDeltaException;
		
	}
}
