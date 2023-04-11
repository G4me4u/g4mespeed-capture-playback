package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSTrackNameDelta extends GSTrackDelta {

	private String newName;
	private String oldName;

	public GSTrackNameDelta() {
	}
	
	public GSTrackNameDelta(UUID trackUUID, String newName, String oldName) {
		super(trackUUID);
		
		this.newName = newName;
		this.oldName = oldName;
	}

	private void setTrackName(GSComposition composition, String newName, String oldName) throws GSDeltaException {
		GSTrack track = getTrack(composition);
		checkTrackName(track, oldName);
		track.setName(newName);
	}
	
	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		setTrackName(composition, oldName, newName);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		setTrackName(composition, newName, oldName);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		newName = buf.readString();
		oldName = buf.readString();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		buf.writeString(newName);
		buf.writeString(oldName);
	}
}
