package com.g4mesoft.captureplayback.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.GSIDeltaListener;
import com.g4mesoft.registry.GSSupplierRegistry;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSUndoRedoHistory {

	private static final int MAX_HISTORY_SIZE = 10000;
	
	private static final GSSupplierRegistry<Integer, GSIUndoRedoEntry> ENTRY_REGISTRY;
	
	static {
		ENTRY_REGISTRY = new GSSupplierRegistry<>();
		ENTRY_REGISTRY.register(0, GSCompositionUndoRedoEntry.class, GSCompositionUndoRedoEntry::new);
		ENTRY_REGISTRY.register(1, GSSequenceUndoRedoEntry.class, GSSequenceUndoRedoEntry::new);
	}
	
	private final Deque<GSIUndoRedoEntry> undoHistory;
	private final Deque<GSIUndoRedoEntry> redoHistory;

	private GSSession session;
	private boolean tracking;
	private boolean applyingDelta;
	
	private final List<GSIUndoRedoListener> historyListeners;
	private final List<GSIDeltaListener<GSUndoRedoHistory>> deltaListeners;

	public GSUndoRedoHistory() {
		this(new LinkedList<>(), new LinkedList<>());
	}
	
	private GSUndoRedoHistory(Deque<GSIUndoRedoEntry> undoHistory, Deque<GSIUndoRedoEntry> redoHistory) {
		this.undoHistory = undoHistory;
		this.redoHistory = redoHistory;
	
		session = null;
		tracking = true;
		applyingDelta = false;

		historyListeners = new ArrayList<>(1);
		deltaListeners = new ArrayList<>(1);
	}

	/* Visible for GSUndoRedoHistorySessionField */
	void onAdded(GSSession session) {
		this.session = session;
	}

	/* Visible for GSUndoRedoHistorySessionField */
	void onRemoved(GSSession session) {
		this.session = null;
	}
	
	public void undo() {
		if (session != null)
			applyHistory(undoHistory, redoHistory, true);
	}

	public void redo() {
		if (session != null)
			applyHistory(redoHistory, undoHistory, false);
	}
	
	private void applyHistory(Deque<GSIUndoRedoEntry> srcHistory, Deque<GSIUndoRedoEntry> dstHistory, boolean undo) {
		if (!srcHistory.isEmpty()) {
			int count = 0;

			GSIUndoRedoEntry entry;
			do {
				entry = srcHistory.peekLast();

				try {
					tracking = false;
					if (undo) {
						entry.undo(session);
					} else {
						entry.redo(session);
					}
				} catch (GSDeltaException e) {
					// Unable to apply, break until it is possible.
					break;
				} finally {
					tracking = true;
				}
				
				dstHistory.addLast(srcHistory.removeLast());
				count++;
			} while (!srcHistory.isEmpty() && srcHistory.getLast().isChained(entry));

			if (count != 0) {
				dispatchHistoryChanged();
				dispatchHistoryDelta(new GSMoveUndoRedoHistoryDelta(undo, count));
			}
		}
	}
	
	public boolean hasUndoHistory() {
		return !undoHistory.isEmpty();
	}

	public boolean hasRedoHistory() {
		return !redoHistory.isEmpty();
	}
	
	public void addUndoRedoListener(GSIUndoRedoListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		historyListeners.add(listener);
	}

	public void removeUndoRedoListener(GSIUndoRedoListener listener) {
		historyListeners.remove(listener);
	}
	
	private void dispatchHistoryChanged() {
		for (GSIUndoRedoListener listener : historyListeners)
			listener.onHistoryChanged();
	}
	
	/* Visible for GSUndoRedoHistorySessionField */
	void addDeltaListener(GSIDeltaListener<GSUndoRedoHistory> listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		deltaListeners.add(listener);
	}

	/* Visible for GSUndoRedoHistorySessionField */
	void removeDeltaListener(GSIDeltaListener<GSUndoRedoHistory> listener) {
		deltaListeners.remove(listener);
	}
	
	private void dispatchHistoryDelta(GSIDelta<GSUndoRedoHistory> delta) {
		if (!applyingDelta) {
			for (GSIDeltaListener<GSUndoRedoHistory> listener : deltaListeners)
				listener.onDelta(delta);
		}
	}

	/* Visible for GSUndoRedoHistorySessionField */
	void applyDelta(GSIDelta<GSUndoRedoHistory> delta) throws GSDeltaException {
		applyingDelta = true;
		try {
			delta.apply(this);
		} finally {
			applyingDelta = false;
		}
	}
	
	/* Visible for GSTrackUndoRedoHistoryDelta and sub-classes of GSSessionField */
	void addEntry(GSIUndoRedoEntry entry) {
		if (tracking) {
			if (!redoHistory.isEmpty())
				redoHistory.clear();
			
			undoHistory.addLast(entry);
			
			// Ensure that the size of the history in limited to MAX_HISTORY_SIZE.
			if (undoHistory.size() > MAX_HISTORY_SIZE)
				undoHistory.removeFirst();
			
			dispatchHistoryChanged();
			dispatchHistoryDelta(new GSTrackUndoRedoHistoryDelta(entry));
		}
	}

	/* Visible for GSUndoRedoHistoryDelta */
	int moveEntries(boolean moveToRedo, int count) {
		Deque<GSIUndoRedoEntry> srcHistory = moveToRedo ? undoHistory : redoHistory;
		Deque<GSIUndoRedoEntry> dstHistory = moveToRedo ? redoHistory : undoHistory;
		
		int i;
		for (i = 0; i < count && !srcHistory.isEmpty(); i++)
			dstHistory.addLast(srcHistory.removeLast());
		
		return i;
	}

	public static GSUndoRedoHistory read(GSDecodeBuffer buf) throws IOException {
		Deque<GSIUndoRedoEntry> undoHistory = new LinkedList<>();
		Deque<GSIUndoRedoEntry> redoHistory = new LinkedList<>();
		
		int undoCount = buf.readInt();
		while (undoCount-- != 0)
			undoHistory.addLast(readEntry(buf));
		int redoCount = buf.readInt();
		while (redoCount-- != 0)
			redoHistory.addLast(readEntry(buf));
		
		return new GSUndoRedoHistory(undoHistory, redoHistory);
	}
	
	public static void write(GSEncodeBuffer buf, GSUndoRedoHistory history) throws IOException {
		buf.writeInt(history.undoHistory.size());
		for (GSIUndoRedoEntry entry : history.undoHistory)
			writeEntry(buf, entry);
		
		buf.writeInt(history.redoHistory.size());
		for (GSIUndoRedoEntry entry : history.redoHistory)
			writeEntry(buf, entry);
	}
	
	/* Visible for GSTrackUndoRedoHistoryDelta */
	static GSIUndoRedoEntry readEntry(GSDecodeBuffer buf) throws IOException {
		int identifier = (int)buf.readUnsignedByte();
		GSIUndoRedoEntry entry = ENTRY_REGISTRY.createNewElement(identifier);
		if (entry == null)
			throw new IOException("Unknown entry identifier: " + identifier);
		entry.read(buf);
		return entry;
	}
	
	/* Visible for GSTrackUndoRedoHistoryDelta */
	static void writeEntry(GSEncodeBuffer buf, GSIUndoRedoEntry entry) throws IOException {
		buf.writeUnsignedByte(ENTRY_REGISTRY.getIdentifier(entry).shortValue());
		entry.write(buf);
	}
}
