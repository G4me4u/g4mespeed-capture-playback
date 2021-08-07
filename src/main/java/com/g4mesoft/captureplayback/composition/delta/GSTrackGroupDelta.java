package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;

import net.minecraft.network.PacketByteBuf;

public class GSTrackGroupDelta extends GSTrackDelta {

	private UUID newGroupUUID;
	private UUID oldGroupUUID;

	public GSTrackGroupDelta() {
	}
	
	public GSTrackGroupDelta(UUID trackUUID, UUID newGroupUUID, UUID oldGroupUUID) {
		super(trackUUID);
		
		this.newGroupUUID = newGroupUUID;
		this.oldGroupUUID = oldGroupUUID;
	}

	private void setTrackGroup(GSComposition composition, UUID newGroupUUID, UUID oldGroupUUID) throws GSDeltaException {
		GSTrack track = getTrack(composition);
		checkGroup(track, oldGroupUUID);
		track.setGroupUUID(newGroupUUID);
	}
	
	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		setTrackGroup(composition, oldGroupUUID, newGroupUUID);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		setTrackGroup(composition, newGroupUUID, oldGroupUUID);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		newGroupUUID = buf.readUuid();
		oldGroupUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeUuid(newGroupUUID);
		buf.writeUuid(oldGroupUUID);
	}
}
