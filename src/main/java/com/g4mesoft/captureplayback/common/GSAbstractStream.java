package com.g4mesoft.captureplayback.common;

import java.util.ArrayList;
import java.util.List;

import com.g4mesoft.captureplayback.stream.GSIStream;
import com.g4mesoft.captureplayback.stream.GSIStreamCloseListener;

public abstract class GSAbstractStream implements GSIStream {

	private final List<GSIStreamCloseListener> closeListeners;
	
	public GSAbstractStream() {
		closeListeners = new ArrayList<>();
	}
	
	@Override
	public void addCloseListener(GSIStreamCloseListener listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null");
		closeListeners.add(listener);
	}

	@Override
	public void removeCloseListener(GSIStreamCloseListener listener) {
		closeListeners.remove(listener);
	}
	
	protected void dispatchCloseEvent() {
		closeListeners.forEach(GSIStreamCloseListener::onClose);
	}
}
