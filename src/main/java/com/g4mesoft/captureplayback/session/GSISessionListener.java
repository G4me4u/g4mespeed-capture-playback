package com.g4mesoft.captureplayback.session;

import com.g4mesoft.captureplayback.common.GSIDelta;

public interface GSISessionListener {

	default public void onFieldChanged(GSSession session, GSSessionFieldType<?> type) {
	}

	default public void onSessionDeltas(GSSession session, GSIDelta<GSSession>[] deltas) {
	}
}
