package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

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
	public void unapplyDelta(GSComposition composition) throws GSDeltaException {
		setTrackName(composition, oldName, newName);
	}

	@Override
	public void applyDelta(GSComposition composition) throws GSDeltaException {
		setTrackName(composition, newName, oldName);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		newName = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		oldName = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeString(newName);
		buf.writeString(oldName);
	}
}
