package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDeltaListener;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaTransformer;

public class GSSequenceSessionField extends GSSessionField<GSSequence> implements GSISequenceDeltaListener {

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
	public void onSequenceDelta(GSISequenceDelta delta) {
		if (session != null)
			session.dispatchSessionDelta(new GSSequenceSessionDelta(type, delta));
	}

	public void applyDelta(GSISequenceDelta delta) throws GSDeltaException {
		if (value != null) {
			try {
				transformer.setEnabled(false);
				delta.applyDelta(value);
			} finally {
				transformer.setEnabled(true);
			}
		}
	}
}
