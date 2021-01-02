package com.g4mesoft.captureplayback.gui;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.sequence.GSChannelInfo;

public interface GSIChannelProvider {

	public GSChannelInfo createNextChannelInfo(GSSequence sequence);
	
}
