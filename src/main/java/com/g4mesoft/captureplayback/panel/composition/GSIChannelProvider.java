package com.g4mesoft.captureplayback.panel.composition;

import com.g4mesoft.captureplayback.sequence.GSChannelInfo;
import com.g4mesoft.captureplayback.sequence.GSSequence;

public interface GSIChannelProvider {

	public GSChannelInfo createChannelInfo(GSSequence sequence);
	
}
