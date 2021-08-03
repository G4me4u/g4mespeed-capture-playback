package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.composition.GSTrackEntry;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSTrackRemovedDelta extends GSTrackDelta {

	private String name;
	private int color;
	private UUID groupUUID;
	
	private GSSequence sequence;
	private GSTrackEntryRemovedDelta[] entryDeltas;

	public GSTrackRemovedDelta() {
	}

	public GSTrackRemovedDelta(GSTrack track) {
		this(track.getTrackUUID(), track.getName(), track.getColor(),
		     track.getGroupUUID(), track.getSequence(), track.getEntries());
	}
	
	public GSTrackRemovedDelta(UUID trackUUID, String name, int color, UUID groupUUID,
	                           GSSequence sequence, Collection<GSTrackEntry> entries) {
		
		super(trackUUID);
		
		this.name = name;
		this.color = color;
		this.groupUUID = groupUUID;
		this.sequence = sequence;
		
		entryDeltas = new GSTrackEntryRemovedDelta[entries.size()];
		
		int i = 0;
		for (GSTrackEntry entry : entries)
			entryDeltas[i++] = new GSTrackEntryRemovedDelta(entry);
	}
	
	@Override
	public void unapplyDelta(GSComposition composition) throws GSDeltaException {
		GSTrack track = addTrack(composition, name, color, groupUUID);
		
		track.getSequence().set(sequence);
		
		for (GSTrackEntryDelta entryDelta : entryDeltas)
			entryDelta.unapplyDelta(composition);
	}

	@Override
	public void applyDelta(GSComposition composition) throws GSDeltaException {
		removeTrack(composition, name, color, groupUUID, sequence.getChannels().size(), 
				getSequenceEntryCount(sequence), entryDeltas.length);
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		color = buf.readInt();
		groupUUID = buf.readUuid();
		
		sequence = GSSequence.read(buf);
		
		entryDeltas = new GSTrackEntryRemovedDelta[buf.readInt()];
		for (int i = 0; i < entryDeltas.length; i++)
			(entryDeltas[i] = new GSTrackEntryRemovedDelta()).read(buf);
	}
	
	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		buf.writeString(name);
		buf.writeInt(color);
		buf.writeUuid(groupUUID);
		
		GSSequence.write(buf, sequence);
		
		buf.writeInt(entryDeltas.length);
		for (GSTrackEntryRemovedDelta entryDelta : entryDeltas)
			entryDelta.write(buf);
	}
}
