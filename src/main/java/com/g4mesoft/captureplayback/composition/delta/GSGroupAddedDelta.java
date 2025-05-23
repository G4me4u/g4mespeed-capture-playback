package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrackGroup;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

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
	public void unapply(GSComposition composition) throws GSDeltaException {
		removeGroup(composition, groupName);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		addGroup(composition, groupName);
	}

	@Override
	public void read(GSDecodeBuffer buf) throws IOException {
		super.read(buf);
	
		groupName = buf.readString();
	}

	@Override
	public void write(GSEncodeBuffer buf) throws IOException {
		super.write(buf);
		
		buf.writeString(groupName);
	}
}
