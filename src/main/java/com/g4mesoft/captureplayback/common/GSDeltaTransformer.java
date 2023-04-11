package com.g4mesoft.captureplayback.common;

import java.util.ArrayList;
import java.util.List;

public abstract class GSDeltaTransformer<M> {

	protected M model;
	
	private final List<GSIDeltaListener<M>> listeners;
	private boolean enabled;
	
	public GSDeltaTransformer() {
		model = null;
	
		listeners = new ArrayList<>();
		enabled = true;
	}
	
	public void install(M model) {
		if (this.model != null)
			throw new IllegalStateException("Already installed");
		
		this.model = model;
	}
	
	public void uninstall(M model) {
		if (this.model == null)
			throw new IllegalStateException("Not installed");
		if (this.model != model)
			throw new IllegalStateException("Model is not the one that is installed");
		
		this.model = null;
	}
	
	public void addDeltaListener(GSIDeltaListener<M> listener) {
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		listeners.add(listener);
	}

	public void removeDeltaListener(GSIDeltaListener<M> listener) {
		listeners.remove(listener);
	}
	
	protected void dispatchDeltaEvent(GSIDelta<M> delta) {
		if (isEnabled()) {
			for (GSIDeltaListener<M> listener : listeners)
				listener.onDelta(delta);
		}
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
