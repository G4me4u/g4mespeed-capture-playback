package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSTrackColorDelta extends GSTrackDelta {

	private int newColor;
	private int oldColor;

	public GSTrackColorDelta() {
	}
	
	public GSTrackColorDelta(UUID trackUUID, int newColor, int oldColor) {
		super(trackUUID);
		
		this.newColor = newColor;
		this.oldColor = oldColor;
	}

	private void setTrackColor(GSComposition composition, int newColor, int oldColor) throws GSDeltaException {
		GSTrack track = getTrack(composition);
		checkTrackColor(track, oldColor);
		track.setColor(newColor);
	}
	
	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		setTrackColor(composition, oldColor, newColor);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		setTrackColor(composition, newColor, oldColor);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		newColor = buf.readInt();
		oldColor = buf.readInt();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		buf.writeInt(newColor);
		buf.writeInt(oldColor);
	}
}
