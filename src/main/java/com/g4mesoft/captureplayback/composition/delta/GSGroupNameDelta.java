package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

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

	private void setGroupName(GSComposition composition, String newName, String oldName) throws GSDeltaException {
		GSTrackGroup group = getGroup(composition);
		checkGroupName(group, oldName);
		group.setName(newName);
	}
	
	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		setGroupName(composition, oldName, newName);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		setGroupName(composition, newName, oldName);
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
