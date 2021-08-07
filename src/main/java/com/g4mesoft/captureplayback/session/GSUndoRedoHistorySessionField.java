package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.GSIDeltaListener;

public class GSUndoRedoHistorySessionField extends GSSessionField<GSUndoRedoHistory>
                                           implements GSIDeltaListener<GSUndoRedoHistory> {

	private GSSession session;
	
	public GSUndoRedoHistorySessionField(GSSessionFieldType<GSUndoRedoHistory> type) {
		super(type);
	
		session = null;
	}

	@Override
	public void onAdded(GSSession session) {
		super.onAdded(session);

		this.session = session;
		
		if (value != null)
			value.onAdded(session);
	}
	
	@Override
	public void set(GSUndoRedoHistory value) {
		if (this.value != null) {
			if (session != null)
				this.value.onRemoved(session);
			this.value.removeDeltaListener(this);
		}
		
		super.set(value);

		if (value != null) {
			if (session != null)
				value.onAdded(session);
			value.addDeltaListener(this);
		}
	}

	@Override
	public void onDelta(GSIDelta<GSUndoRedoHistory> delta) {
		if (session != null)
			session.dispatchSessionDelta(new GSUndoRedoHistorySessionDelta(type, delta));
	}

	public void applyDelta(GSIDelta<GSUndoRedoHistory> delta) throws GSDeltaException {
		if (value != null)
			value.applyDelta(delta);
	}
}
