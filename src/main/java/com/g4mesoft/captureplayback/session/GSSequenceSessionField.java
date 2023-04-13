package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaTransformer;

public class GSSequenceSessionField extends GSAssetSessionField<GSSequence> {

	public GSSequenceSessionField(GSSessionFieldType<GSSequence> type) {
		super(type, new GSSequenceDeltaTransformer());
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
}
