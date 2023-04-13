package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

public class GSTrackAddedDelta extends GSTrackDelta {

	private String name;
	private int color;
	private UUID groupUUID;
	
	public GSTrackAddedDelta() {
	}

	public GSTrackAddedDelta(GSTrack track) {
		this(track.getTrackUUID(), track.getName(), track.getColor(), track.getGroupUUID());
	}
	
	public GSTrackAddedDelta(UUID trackUUID, String name, int color, UUID groupUUID) {
		super(trackUUID);
		
		this.name = name;
		this.color = color;
		this.groupUUID = groupUUID;
	}
	
	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		removeTrack(composition, name, color, groupUUID, 0, 0, 0);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		addTrack(composition, name, color, groupUUID);
	}
	
	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
		
		name = buf.readString();
		color = buf.readInt();
		groupUUID = buf.readUUID();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);

		buf.writeString(name);
		buf.writeInt(color);
		buf.writeUUID(groupUUID);
	}
}
