package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSGroupRemovedDelta extends GSGroupDelta {

	private String groupName;
	
	public GSGroupRemovedDelta() {
	}

	public GSGroupRemovedDelta(GSTrackGroup group) {
		this(group.getGroupUUID(), group.getName());
	}
	
	public GSGroupRemovedDelta(UUID groupUUID, String groupName) {
		super(groupUUID);
		
		this.groupName = groupName;
	}
	
	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		addGroup(composition, groupName);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		removeGroup(composition, groupName);
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
