package com.g4mesoft.captureplayback.sequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.util.PacketByteBuf;

public class GSSequence {

	private final UUID sequenceUUID;
	private String name;
	
	private final Map<UUID, GSChannel> channels;
	private final List<GSISequenceListener> listeners;

	public GSSequence(UUID sequenceUUID) {
		this(sequenceUUID, "");
	}

	public GSSequence(UUID sequenceUUID, String name) {
		if (sequenceUUID == null)
			throw new IllegalArgumentException("sequenceUUID is null");
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		this.sequenceUUID = sequenceUUID;
		this.name = name;
		
		channels = new LinkedHashMap<>();
		listeners = new ArrayList<>();
	}

	public void set(GSSequence other) {
		setName(other.getName());
		
		clear();
		
		for (GSChannel channel : other.getChannels()) {
			GSChannel channelCopy = new GSChannel(channel.getChannelUUID(), channel.getInfo());
			channelCopy.set(channel);
			addChannelInternal(channelCopy);
			
			dispatchChannelAdded(channelCopy);
		}
	}
	
	private void clear() {
		Iterator<GSChannel> itr = channels.values().iterator();
		while (itr.hasNext()) {
			GSChannel channel = itr.next();
			itr.remove();
			
			dispatchChannelRemoved(channel);
		}
	}
	
	public GSChannel addChannel(GSChannelInfo info) {
		return addChannel(GSUUIDUtil.randomUnique(this::hasChannelUUID), info);
	}
	
	public GSChannel addChannel(UUID channelUUID, GSChannelInfo info) {
		if (hasChannelUUID(channelUUID))
			throw new IllegalStateException("Duplicate channel UUID");
		
		GSChannel channel = new GSChannel(channelUUID, info);
		addChannelInternal(channel);
		
		dispatchChannelAdded(channel);
		
		return channel;
	}
	
	private void addChannelInternal(GSChannel channel) {
		channel.setParent(this);
		
		channels.put(channel.getChannelUUID(), channel);
	}
	
	public boolean removeChannel(UUID channelUUID) {
		GSChannel channel = channels.remove(channelUUID);
		if (channel != null) {
			dispatchChannelRemoved(channel);
			return true;
		}
		
		return false;
	}
	
	public UUID getSequenceUUID() {
		return sequenceUUID;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("name is null");
		
		if (!name.equals(this.name)) {
			String oldName = this.name;
			this.name = name;
			
			dispatchSequenceNameChanged(oldName);
		}
	}

	public GSChannel getChannel(UUID channelUUID) {
		return channels.get(channelUUID);
	}
	
	public boolean hasChannelUUID(UUID channelUUID) {
		return channels.containsKey(channelUUID);
	}
	
	public Set<UUID> getChannelUUIDs() {
		return Collections.unmodifiableSet(channels.keySet());
	}
	
	public Collection<GSChannel> getChannels() {
		return Collections.unmodifiableCollection(channels.values());
	}
	
	public void addSequenceListener(GSISequenceListener listener) {
		listeners.add(listener);
	}

	public void removeSequenceListener(GSISequenceListener listener) {
		listeners.remove(listener);
	}
	
	/* Visible to allow events from the tracks and entries */
	Iterable<GSISequenceListener> getListeners() {
		return listeners;
	}
	
	private void dispatchSequenceNameChanged(String oldName) {
		for (GSISequenceListener listener : listeners)
			listener.sequenceNameChanged(oldName);
	}

	private void dispatchChannelAdded(GSChannel track) {
		for (GSISequenceListener listener : listeners)
			listener.channelAdded(track);
	}

	private void dispatchChannelRemoved(GSChannel channel) {
		for (GSISequenceListener listener : listeners)
			listener.channelRemoved(channel);
	}
	
	public static GSSequence read(PacketByteBuf buf) throws IOException {
		// Skip reserved byte
		buf.readByte();

		UUID sequenceUUID = buf.readUuid();
		String name = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		GSSequence sequence = new GSSequence(sequenceUUID, name);

		int channelCount = buf.readInt();
		while (channelCount-- != 0) {
			GSChannel channel = GSChannel.read(buf);
			if (sequence.hasChannelUUID(channel.getChannelUUID()))
				throw new IOException("Duplicate channel UUID.");
			sequence.addChannelInternal(channel);
		}

		return sequence;
	}

	public static void write(PacketByteBuf buf, GSSequence sequence) throws IOException {
		// Reserved for future use
		buf.writeByte(0x00);
		
		buf.writeUuid(sequence.getSequenceUUID());
		buf.writeString(sequence.getName());
		
		Collection<GSChannel> channels = sequence.getChannels();
		buf.writeInt(channels.size());
		for (GSChannel channel : channels)
			GSChannel.write(buf, channel);
	}
}
