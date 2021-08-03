package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.delta.GSCompositionDeltaTransformer;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDelta;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDeltaListener;

public class GSCompositionSessionField extends GSSessionField<GSComposition> implements GSICompositionDeltaListener {

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
	public void onCompositionDelta(GSICompositionDelta delta) {
		if (session != null)
			session.dispatchSessionDelta(new GSCompositionSessionDelta(type, delta));
	}

	public void applyDelta(GSICompositionDelta delta) throws GSDeltaException {
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
