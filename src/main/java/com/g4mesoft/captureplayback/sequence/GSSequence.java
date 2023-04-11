package com.g4mesoft.captureplayback.sequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.g4mesoft.captureplayback.stream.GSBlockRegion;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;
import com.g4mesoft.captureplayback.util.GSMutableLinkedHashMap;
import com.g4mesoft.captureplayback.util.GSUUIDUtil;
import com.g4mesoft.util.GSDecodeBuffer;
import com.g4mesoft.util.GSEncodeBuffer;

import net.minecraft.util.math.BlockPos;

public class GSSequence {

	private final UUID sequenceUUID;
	private String name;
	
	private final GSMutableLinkedHashMap<UUID, GSChannel> channels;
	private List<GSISequenceListener> listeners;

	public GSSequence(GSSequence other) {
		this(other.getSequenceUUID(), other.getName());
		
		for (GSChannel channel : other.getChannels())
			addChannelInternal(new GSChannel(channel));
	}
	
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
		
		channels = new GSMutableLinkedHashMap<>();
		// Lazily initialized when adding a listener
		listeners = null;
	}
	
	/**
	 * A version of {@link #set(GSSequence)} which does not change the
	 * name, and ensures that any group and track has unique UUIDs.
	 */
	public void duplicateFrom(GSSequence other) {
		if (!channels.isEmpty())
			throw new IllegalStateException("Expected an empty sequence");
		
		for (GSChannel channel : other.getChannels())
			addChannel(channel.getInfo()).duplicateFrom(channel);
	}
	
	public void set(GSSequence other) {
		clear();

		setName(other.getName());
		
		for (GSChannel channel : other.getChannels())
			addChannel(channel.getChannelUUID(), channel.getInfo()).set(channel);
	}
	
	private void clear() {
		Iterator<GSChannel> itr = channels.values().iterator();
		UUID prevUUID = null;
		while (itr.hasNext()) {
			GSChannel channel = itr.next();
			itr.remove();
			onChannelRemoved(channel, prevUUID);
			prevUUID = channel.getChannelUUID();
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
		
		GSChannel prevChannel = getPreviousChannel(channelUUID);
		UUID prevUUID = (prevChannel == null) ? null : prevChannel.getChannelUUID();
		dispatchChannelAdded(channel, prevUUID);
		
		return channel;
	}
	
	private void addChannelInternal(GSChannel channel) {
		channel.onAdded(this);
		
		channels.put(channel.getChannelUUID(), channel);
	}
	
	public boolean removeChannel(UUID channelUUID) {
		GSChannel prevChannel = getPreviousChannel(channelUUID);
		GSChannel channel = channels.remove(channelUUID);
		if (channel != null) {
			UUID prevUUID = (prevChannel == null) ? null : prevChannel.getChannelUUID();

			onChannelRemoved(channel, prevUUID);
			return true;
		}
		
		return false;
	}
	
	private void onChannelRemoved(GSChannel channel, UUID oldPrevUUID) {
		dispatchChannelRemoved(channel, oldPrevUUID);
		// Ensure that changes to the channel are no longer
		// heard by the registered listeners.
		channel.onRemoved(this);
	}
	
	public void moveChannelBefore(UUID channelUUID, UUID newNextUUID) {
		GSChannel prevChannel = getPreviousChannel(newNextUUID);
		moveChannelAfter(channelUUID, (prevChannel == null) ? null : prevChannel.getChannelUUID());
	}

	public void moveChannelAfter(UUID channelUUID, UUID newPrevUUID) {
		if (channelUUID != null) {
			Map.Entry<UUID, GSChannel> prevEntry = channels.getPreviousEntry(channelUUID);
			UUID oldPrevUUID = (prevEntry == null) ? null : prevEntry.getKey();
	
			if (!channelUUID.equals(newPrevUUID)) {
				Map.Entry<UUID, GSChannel> entry = channels.moveAfter(channelUUID, newPrevUUID);
				if (entry != null)
					dispatchChannelMoved(entry.getValue(), newPrevUUID, oldPrevUUID);
			}
		}
	}
	
	public GSChannel getPreviousChannel(UUID channelUUID) {
		Map.Entry<UUID, GSChannel> prevEntry = channels.getPreviousEntry(channelUUID);
		return (prevEntry == null) ? null : prevEntry.getValue();
	}

	public GSChannel getNextChannel(UUID channelUUID) {
		Map.Entry<UUID, GSChannel> nextEntry = channels.getNextEntry(channelUUID);
		return (nextEntry == null) ? null : nextEntry.getValue();
	}
	
	public UUID getSequenceUUID() {
		return sequenceUUID;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		// Note: deliberate null-pointer exception
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
		if (listener == null)
			throw new IllegalArgumentException("listener is null!");
		if (listeners == null)
			listeners = new ArrayList<>();
		listeners.add(listener);
	}

	public void removeSequenceListener(GSISequenceListener listener) {
		if (listeners != null)
			listeners.remove(listener);
	}
	
	/* Visible to allow events from the channels and entries */
	Iterable<GSISequenceListener> getListeners() {
		return (listeners == null) ? Collections.emptyList() : listeners;
	}
	
	private void dispatchSequenceNameChanged(String oldName) {
		for (GSISequenceListener listener : getListeners())
			listener.sequenceNameChanged(oldName);
	}

	private void dispatchChannelAdded(GSChannel channel, UUID prevUUID) {
		for (GSISequenceListener listener : getListeners())
			listener.channelAdded(channel, prevUUID);
	}

	private void dispatchChannelRemoved(GSChannel channel, UUID oldPrevUUID) {
		for (GSISequenceListener listener : getListeners())
			listener.channelRemoved(channel, oldPrevUUID);
	}
	
	private void dispatchChannelMoved(GSChannel channel, UUID newPrevUUID, UUID oldPrevUUID) {
		for (GSISequenceListener listener : getListeners())
			listener.channelMoved(channel, newPrevUUID, oldPrevUUID);
	}
	
	public static GSSequence read(GSDecodeBuffer buf) throws IOException {
		// Skip reserved byte
		buf.readByte();

		UUID sequenceUUID = buf.readUUID();
		String name = buf.readString();
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

	public static void write(GSEncodeBuffer buf, GSSequence sequence) throws IOException {
		// Reserved for future use
		buf.writeByte((byte)0x00);
		
		buf.writeUUID(sequence.getSequenceUUID());
		buf.writeString(sequence.getName());
		
		Collection<GSChannel> channels = sequence.getChannels();
		buf.writeInt(channels.size());
		for (GSChannel channel : channels)
			GSChannel.write(buf, channel);
	}
	
	public GSIPlaybackStream getPlaybackStream() {
		return new GSSequencePlaybackStream(this);
	}

	public GSICaptureStream getCaptureStream() {
		return new GSSequenceCaptureStream(this);
	}
	
	public GSBlockRegion getBlockRegion() {
		int x0 = Integer.MAX_VALUE;
		int y0 = Integer.MAX_VALUE;
		int z0 = Integer.MAX_VALUE;

		int x1 = Integer.MIN_VALUE;
		int y1 = Integer.MIN_VALUE;
		int z1 = Integer.MIN_VALUE;
		
		for (GSChannel channel : getChannels()) {
			for (BlockPos position : channel.getInfo().getPositions()) {
				if (position.getX() < x0)
					x0 = position.getX();
				if (position.getY() < y0)
					y0 = position.getY();
				if (position.getZ() < z0)
					z0 = position.getZ();
	
				if (position.getX() > x1)
					x1 = position.getX();
				if (position.getY() > y1)
					y1 = position.getY();
				if (position.getZ() > z1)
					z1 = position.getZ();
			}
		}
		
		return new GSBlockRegion(x0, y0, z0, x1, y1, z1);
	}
}
