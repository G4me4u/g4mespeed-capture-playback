package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSGroupNameDelta extends GSGroupDelta {

	private String newName;
	private String oldName;

	public GSGroupNameDelta() {
	}
	
	public GSGroupNameDelta(UUID groupUUID, String newName, String oldName) {
		super(groupUUID);
		
		this.newName = newName;
		this.oldName = oldName;
	}

	private void setGroupName(GSComposition composition, String newName, String oldName) throws GSCompositionDeltaException {
		GSTrackGroup group = getGroup(composition);
		checkGroupName(group, oldName);
		group.setName(newName);
	}
	
	@Override
	public void unapplyDelta(GSComposition composition) throws GSCompositionDeltaException {
		setGroupName(composition, oldName, newName);
	}

	@Override
	public void applyDelta(GSComposition composition) throws GSCompositionDeltaException {
		setGroupName(composition, newName, oldName);
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
