package com.g4mesoft.captureplayback.composition;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.util.PacketByteBuf;

public class GSComposition {

	private final UUID compositionUUID;
	private String name;
	
	private final Map<UUID, GSSequence> sequences;
	private final Map<UUID, GSRocket> rockets;

	public GSComposition(UUID compositionUUID) {
		this(compositionUUID, "");
	}

	public GSComposition(UUID compositionUUID, String name) {
		if (compositionUUID == null)
			throw new IllegalArgumentException("compositionUUID is null");
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		this.compositionUUID = compositionUUID;
		this.name = name;
		
		sequences = new LinkedHashMap<>();
		rockets = new LinkedHashMap<>();
	}
	
	private void addSequenceInternal(GSSequence sequence) {
		sequences.put(sequence.getSequenceUUID(), sequence);
	}
	
	private void addRocketInternal(GSRocket rocket) {
		rocket.setParent(this);
		
		rockets.put(rocket.getRocketUUID(), rocket);
	}
	
	public UUID getCompositionUUID() {
		return compositionUUID;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		if (!name.equals(this.name))
			this.name = name;
	}
	
	public GSSequence getSequence(UUID sequenceUUID) {
		return sequences.get(sequenceUUID);
	}
	
	public boolean hasSequenceUUID(UUID sequenceUUID) {
		return sequences.containsKey(sequenceUUID);
	}

	public GSRocket getRocket(UUID rocketUUID) {
		return rockets.get(rocketUUID);
	}
	
	public boolean hasRocketUUID(UUID rocketUUID) {
		return rockets.containsKey(rocketUUID);
	}
	
	public Collection<GSSequence> getSequences() {
		return Collections.unmodifiableCollection(sequences.values());
	}

	public Set<UUID> getSequenceUUIDs() {
		return Collections.unmodifiableSet(sequences.keySet());
	}

	public Collection<GSRocket> getRockets() {
		return Collections.unmodifiableCollection(rockets.values());
	}
	
	public Set<UUID> getRocketUUIDs() {
		return Collections.unmodifiableSet(rockets.keySet());
	}
	
	public boolean isComplete() {
		for (GSRocket rocket : getRockets()) {
			if (!rocket.isComplete())
				return false;
		}
		
		return true;
	}

	public static GSComposition readComposition(PacketByteBuf buf) throws IOException {
		// Skip reserved byte
		buf.readByte();
		
		UUID compositionUUID = buf.readUuid();
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		GSComposition composition = new GSComposition(compositionUUID, name);

		int sequenceCount = buf.readInt();
		while (sequenceCount-- != 0) {
			GSSequence sequence = GSSequence.read(buf);
			if (composition.hasSequenceUUID(sequence.getSequenceUUID()))
				throw new IOException("Duplicate sequence UUID.");
			composition.addSequenceInternal(sequence);
		}

		int rocketCount = buf.readInt();
		while (rocketCount-- != 0) {
			GSRocket rocket = GSRocket.read(buf);
			if (composition.hasRocketUUID(rocket.getRocketUUID()))
				throw new IOException("Duplicate rocket UUID.");
			composition.addRocketInternal(rocket);
		}
		
		if (!composition.isComplete())
			throw new IOException("Composition is not complete!");
		
		return composition;
	}

	public static void write(PacketByteBuf buf, GSComposition composition) throws IOException {
		// Reserved byte for future use
		buf.writeByte(0x00);

		buf.writeUuid(composition.getCompositionUUID());
		buf.writeString(composition.getName());

		Collection<GSSequence> sequences = composition.getSequences();
		buf.writeInt(sequences.size());
		for (GSSequence sequence : sequences)
			GSSequence.write(buf, sequence);
		
		Collection<GSRocket> rockets = composition.getRockets();
		buf.writeInt(rockets.size());
		for (GSRocket rocket : rockets)
			GSRocket.write(buf, rocket);
	}
}
