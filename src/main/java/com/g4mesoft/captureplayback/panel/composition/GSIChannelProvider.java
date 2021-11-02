package com.g4mesoft.captureplayback.panel.composition;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;

public interface GSIChannelProvider {

	public GSChannelInfo createChannelInfo(GSSequence sequence);
	
}
