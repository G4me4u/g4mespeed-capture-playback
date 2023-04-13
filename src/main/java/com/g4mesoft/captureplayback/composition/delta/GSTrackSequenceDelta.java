package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSDeltaRegistries;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSTrackSequenceDelta extends GSTrackDelta {

	private GSIDelta<GSSequence> delta;

	public GSTrackSequenceDelta() {
	}
	
	public GSTrackSequenceDelta(UUID trackUUID, GSIDelta<GSSequence> delta) {
		super(trackUUID);
		
		this.delta = delta;
	}

	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		delta.unapply(getTrack(composition).getSequence());
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		delta.apply(getTrack(composition).getSequence());
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		delta = GSDeltaRegistries.SEQUENCE_DELTA_REGISTRY.read(buf);
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		GSDeltaRegistries.SEQUENCE_DELTA_REGISTRY.write(buf, delta);
	}
}
