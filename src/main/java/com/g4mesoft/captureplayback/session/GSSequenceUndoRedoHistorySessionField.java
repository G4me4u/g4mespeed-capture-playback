package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.sequence.delta.GSISequenceUndoRedoListener;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceUndoRedoHistory;

public class GSSequenceUndoRedoHistorySessionField extends GSSessionField<GSSequenceUndoRedoHistory>
                                                   implements GSISequenceUndoRedoListener {

	private GSSession session;
	
	public GSSequenceUndoRedoHistorySessionField(GSSessionFieldType<GSSequenceUndoRedoHistory> type) {
		super(type);
	
		session = null;
	}

	@Override
	public void onAdded(GSSession session) {
		super.onAdded(session);

		this.session = session;
	}
	
	@Override
	public void set(GSSequenceUndoRedoHistory value) {
		if (this.value != null)
			this.value.removeUndoRedoListener(this);
		
		super.set(value);

		if (value != null)
			value.addUndoRedoListener(this);
	}

	@Override
	public void onHistoryChanged() {
		if (session != null)
			session.requestSync(type);
	}
}
