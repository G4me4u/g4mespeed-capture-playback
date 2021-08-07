package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.GSIDeltaListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaTransformer;

public class GSSequenceSessionField extends GSSessionField<GSSequence> implements GSIDeltaListener<GSSequence> {

	private GSSession session;
	
	private final GSSequenceDeltaTransformer transformer;
	
	public GSSequenceSessionField(GSSessionFieldType<GSSequence> type) {
		super(type);
		
		session = null;
		
		transformer = new GSSequenceDeltaTransformer();
		transformer.addDeltaListener(this);
	}
	
	@Override
	public void onAdded(GSSession session) {
		super.onAdded(session);

		this.session = session;
	}
	
	@Override
	public void set(GSSequence value) {
		if (this.value != null)
			transformer.uninstall(this.value);
		
		super.set(value);
		
		if (value != null)
			transformer.install(value);
	}

	@Override
	public void onDelta(GSIDelta<GSSequence> delta) {
		if (session != null) {
			session.dispatchSessionDelta(new GSSequenceSessionDelta(type, delta));

			if (session.getSide() == GSSessionSide.CLIENT_SIDE) {
				GSUndoRedoHistory history = session.get(GSSession.UNDO_REDO_HISTORY);
				if (history != null)
					history.addEntry(new GSSequenceUndoRedoEntry(delta));
			}
		}
	}

	public void applyDelta(GSIDelta<GSSequence> delta) throws GSDeltaException {
		if (value != null) {
			try {
				transformer.setEnabled(false);
				delta.apply(value);
			} finally {
				transformer.setEnabled(true);
			}
		}
	}
}
