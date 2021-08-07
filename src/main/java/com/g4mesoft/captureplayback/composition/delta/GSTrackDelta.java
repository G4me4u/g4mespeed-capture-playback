package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.sequence.GSChannel;
import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

public abstract class GSTrackDelta implements GSIDelta<GSComposition> {
	
	protected UUID trackUUID;

	protected GSTrackDelta() {
	}
	
	protected GSTrackDelta(UUID trackUUID) {
		this.trackUUID = trackUUID;
	}
	
	protected GSTrack getTrack(GSComposition composition) throws GSDeltaException {
		GSTrack track = composition.getTrack(trackUUID);
		if (track == null)
			throw new GSDeltaException("Expected track does not exist");
		return track;
	}
	
	protected void checkTrackName(GSTrack track, String expectedName) throws GSDeltaException {
		if (!track.getName().equals(expectedName))
			throw new GSDeltaException("Track does not have the expected name");
	}

	protected void checkTrackColor(GSTrack track, int expectedColor) throws GSDeltaException {
		if (track.getColor() != expectedColor)
			throw new GSDeltaException("Track does not have the expected color");
	}

	protected void checkGroup(GSTrack track, UUID expectedGroupUUID) throws GSDeltaException {
		if (!track.getGroupUUID().equals(expectedGroupUUID))
			throw new GSDeltaException("Track is not in the expected group");
	}
	
	protected void checkSequenceChannelCount(GSTrack track, int expectedCount) throws GSDeltaException {
		if (track.getSequence().getChannels().size() != expectedCount)
			throw new GSDeltaException("Track sequence does not have the expected channel count");
	}

	protected void checkSequenceEntryCount(GSTrack track, int expectedCount) throws GSDeltaException {
		if (getSequenceEntryCount(track.getSequence()) != expectedCount)
			throw new GSDeltaException("Track sequence does not have the expected entry count");
	}
	
	protected void checkTrackEntryCount(GSTrack track, int expectedCount) throws GSDeltaException {
		if (track.getEntries().size() != expectedCount)
			throw new GSDeltaException("Track does not have the expected entry count");
	}
	
	protected int getSequenceEntryCount(GSSequence sequence) {
		int count = 0;
		for (GSChannel channel : sequence.getChannels())
			count += channel.getEntries().size();
		return count;
	}
	
	protected void removeTrack(GSComposition composition, String name, int color, UUID groupUUID,
	                           int expectedSequenceChannelCount, int expectedSequenceEntryCount,
	                           int expectedEntryCount) throws GSDeltaException {
		
		GSTrack track = getTrack(composition);
		checkTrackName(track, name);
		checkTrackColor(track, color);
		checkGroup(track, groupUUID);
		checkSequenceChannelCount(track, expectedSequenceChannelCount);
		checkSequenceEntryCount(track, expectedSequenceEntryCount);
		checkTrackEntryCount(track, expectedEntryCount);
		composition.removeTrack(trackUUID);
	}
	
	protected GSTrack addTrack(GSComposition composition, String name, int color, UUID groupUUID) throws GSDeltaException {
		if (composition.hasTrackUUID(trackUUID))
			throw new GSDeltaException("Track already exists");
		if (!composition.hasGroupUUID(groupUUID))
			throw new GSDeltaException("Track group does not exist");
		
		return composition.addTrack(trackUUID, name, color, groupUUID);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		trackUUID = buf.readUuid();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeUuid(trackUUID);
	}
}
