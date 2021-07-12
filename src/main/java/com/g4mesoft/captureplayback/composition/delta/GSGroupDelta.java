package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;

import net.minecraft.network.PacketByteBuf;

public abstract class GSGroupDelta implements GSICompositionDelta {

	private UUID groupUUID;

	public GSGroupDelta() {
	}
	
	public GSGroupDelta(UUID groupUUID) {
		this.groupUUID = groupUUID;
	}
	
	protected GSTrackGroup getGroup(GSComposition composition) throws GSCompositionDeltaException {
		GSTrackGroup group = composition.getGroup(groupUUID);
		if (group == null)
			throw new GSCompositionDeltaException("Expected group does not exist");
		return group;
	}
	
	protected void checkGroupName(GSTrackGroup group, String expectedName) throws GSCompositionDeltaException {
		if (!Objects.equals(expectedName, group.getName()))
			throw new GSCompositionDeltaException("Group does not have the expected name");
	}

	protected void removeGroup(GSComposition composition, String groupName) throws GSCompositionDeltaException {
		GSTrackGroup group = getGroup(composition);
		checkGroupName(group, groupName);
		composition.removeGroup(groupUUID);
	}
	
	protected void addGroup(GSComposition composition, String groupName) throws GSCompositionDeltaException {
		if (composition.hasGroupUUID(groupUUID))
			throw new GSCompositionDeltaException("Group already exists");
		
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
