package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;

import net.minecraft.network.PacketByteBuf;

public abstract class GSGroupDelta implements GSIDelta<GSComposition> {

	private UUID groupUUID;

	public GSGroupDelta() {
	}
	
	public GSGroupDelta(UUID groupUUID) {
		this.groupUUID = groupUUID;
	}
	
	protected GSTrackGroup getGroup(GSComposition composition) throws GSDeltaException {
		GSTrackGroup group = composition.getGroup(groupUUID);
		if (group == null)
			throw new GSDeltaException("Expected group does not exist");
		return group;
	}
	
	protected void checkGroupName(GSTrackGroup group, String expectedName) throws GSDeltaException {
		if (!Objects.equals(expectedName, group.getName()))
			throw new GSDeltaException("Group does not have the expected name");
	}

	protected void removeGroup(GSComposition composition, String groupName) throws GSDeltaException {
		GSTrackGroup group = getGroup(composition);
		checkGroupName(group, groupName);
		composition.removeGroup(groupUUID);
	}
	
	protected void addGroup(GSComposition composition, String groupName) throws GSDeltaException {
		if (composition.hasGroupUUID(groupUUID))
			throw new GSDeltaException("Group already exists");
		
		composition.addGroup(groupUUID, groupName);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		groupUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeUuid(groupUUID);
	}
}
