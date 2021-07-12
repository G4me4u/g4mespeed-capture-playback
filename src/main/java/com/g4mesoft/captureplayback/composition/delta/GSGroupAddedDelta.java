package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSGroupAddedDelta extends GSGroupDelta {

	private String groupName;
	
	public GSGroupAddedDelta() {
	}

	public GSGroupAddedDelta(GSTrackGroup group) {
		this(group.getGroupUUID(), group.getName());
	}

	public GSGroupAddedDelta(UUID groupUUID, String groupName) {
		super(groupUUID);
		
		this.groupName = groupName;
	}
	
	@Override
	public void unapplyDelta(GSComposition composition) throws GSCompositionDeltaException {
		removeGroup(composition, groupName);
	}

	@Override
	public void applyDelta(GSComposition composition) throws GSCompositionDeltaException {
		addGroup(composition, groupName);
	}

	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
	
		groupName = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);
		
		buf.writeString(groupName);
	}
}
