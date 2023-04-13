package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.delta.GSCompositionDeltaTransformer;

public class GSCompositionSessionField extends GSAssetSessionField<GSComposition> {

	public GSCompositionSessionField(GSSessionFieldType<GSComposition> type) {
		super(type, new GSCompositionDeltaTransformer());
	}
	
	@Override
	public void onDelta(GSIDelta<GSComposition> delta) {
		if (session != null) {
			session.dispatchSessionDelta(new GSCompositionSessionDelta(type, delta));

			if (session.getSide() == GSSessionSide.CLIENT_SIDE) {
				GSUndoRedoHistory history = session.get(GSSession.UNDO_REDO_HISTORY);
				if (history != null)
					history.addEntry(new GSCompositionUndoRedoEntry(delta));
			}
		}
	}
}
