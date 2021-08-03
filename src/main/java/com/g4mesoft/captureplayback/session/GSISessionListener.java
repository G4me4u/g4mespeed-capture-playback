package com.g4mesoft.captureplayback.session;

public interface GSISessionListener {

	default public void onFieldChanged(GSSession session, GSSessionFieldType<?> type) {
	}

	default public void onSessionDeltas(GSSession session, GSISessionDelta[] deltas) {
	}
}
