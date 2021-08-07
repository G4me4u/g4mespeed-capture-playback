package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.GSIDeltaListener;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.delta.GSCompositionDeltaTransformer;

public class GSCompositionSessionField extends GSSessionField<GSComposition> implements GSIDeltaListener<GSComposition> {

	private GSSession session;
	
	private final GSCompositionDeltaTransformer transformer;
	
	public GSCompositionSessionField(GSSessionFieldType<GSComposition> type) {
		super(type);
		
		session = null;
		
		transformer = new GSCompositionDeltaTransformer();
		transformer.addDeltaListener(this);
	}
	
	@Override
	public void onAdded(GSSession session) {
		super.onAdded(session);

		this.session = session;
	}
	
	@Override
	public void set(GSComposition value) {
		if (this.value != null)
			transformer.uninstall(this.value);
		
		super.set(value);
		
		if (value != null)
			transformer.install(value);
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

	public void applyDelta(GSIDelta<GSComposition> delta) throws GSDeltaException {
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
