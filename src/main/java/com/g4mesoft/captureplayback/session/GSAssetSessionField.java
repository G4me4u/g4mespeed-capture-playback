package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSDeltaTransformer;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.common.GSIDeltaListener;

public abstract class GSAssetSessionField<T> extends GSSessionField<T> implements GSIDeltaListener<T> {

	protected GSSession session;
	protected final GSDeltaTransformer<T> transformer;
	
	public GSAssetSessionField(GSSessionFieldType<T> type, GSDeltaTransformer<T> transformer) {
		super(type);
		
		session = null;

		this.transformer = transformer;
		this.transformer.addDeltaListener(this);
	}
	
	@Override
	public void onAdded(GSSession session) {
		super.onAdded(session);

		this.session = session;
	}
	
	@Override
	public void set(T value) {
		if (this.value != null)
			transformer.uninstall(this.value);
		
		super.set(value);
		
		if (value != null)
			transformer.install(value);
	}

	@Override
	public abstract void onDelta(GSIDelta<T> delta);
	
	/* invoked from the appropriate session delta */
	public void applyDelta(GSIDelta<T> delta) throws GSDeltaException {
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
