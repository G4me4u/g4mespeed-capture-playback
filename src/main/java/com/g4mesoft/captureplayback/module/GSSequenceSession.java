package com.g4mesoft.captureplayback.module;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.panel.GSEContentOpacity;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;

import net.minecraft.network.PacketByteBuf;

public class GSSequenceSession {

	private static final float DEFAULT_X_OFFSET = 0.0f;
	private static final float DEFAULT_Y_OFFSET = 0.0f;
	private static final GSEContentOpacity DEFAULT_OPACITY = GSEContentOpacity.FULLY_OPAQUE;
	private static final UUID DEFAULT_SELECTED_CHANNEL = null;
	
	private final UUID compositionUUID;
	private final UUID trackUUID;
	private final UUID sequenceUUID;
	
	/* Cached elements of the UI */
	private float xOffset;
	private float yOffset;
	
	private GSEContentOpacity opacity;
	
	private UUID selectedChannelUUID;
	
	private final GSSequenceUndoRedoHistory undoRedoHistory;

	public GSSequenceSession(UUID compositionUUID, UUID trackUUID, UUID sequenceUUID) {
		this(compositionUUID, trackUUID, sequenceUUID, new GSSequenceUndoRedoHistory());
	}

	private GSSequenceSession(UUID compositionUUID, UUID trackUUID, UUID sequenceUUID, GSSequenceUndoRedoHistory undoRedoHistory) {
		this.compositionUUID = compositionUUID;
		this.trackUUID = trackUUID;
		this.sequenceUUID = sequenceUUID;
		
		xOffset = DEFAULT_X_OFFSET;
		yOffset = DEFAULT_Y_OFFSET;
		
		opacity = DEFAULT_OPACITY;
	
		selectedChannelUUID = DEFAULT_SELECTED_CHANNEL;
	
		this.undoRedoHistory = undoRedoHistory;
	}
	
	public void setRelevantRepeatedFields(GSSequenceSession other) {
		xOffset = other.getXOffset();
		yOffset = other.getYOffset();
		
		opacity = other.getOpacity();
	}
	
	public UUID getCompositionUUID() {
		return compositionUUID;
	}

	public UUID getTrackUUID() {
		return trackUUID;
	}
	
	public UUID getSequenceUUID() {
		return sequenceUUID;
	}
	
	public float getXOffset() {
		return xOffset;
	}

	public void setXOffset(float xOffset) {
		this.xOffset = xOffset;
	}

	public float getYOffset() {
		return yOffset;
	}

	public void setYOffset(float yOffset) {
		this.yOffset = yOffset;
	}
	
	public GSEContentOpacity getOpacity() {
		return opacity;
	}

	public void setOpacity(GSEContentOpacity opacity) {
		if (opacity == null)
			throw new IllegalArgumentException("opacity is null!");
		
		this.opacity = opacity;
	}
	
	public UUID getSelectedChannelUUID() {
		return selectedChannelUUID;
	}

	public void setSelectedChannelUUID(UUID channelUUID) {
		selectedChannelUUID = channelUUID;
	}

	public boolean isSelected(UUID channelUUID) {
		return channelUUID.equals(selectedChannelUUID);
	}
	
	public GSSequenceUndoRedoHistory getUndoRedoHistory() {
		return undoRedoHistory;
	}
	
	public void trackSequenceDelta(GSISequenceDelta delta) {
		undoRedoHistory.trackSequenceDelta(delta);
	}
	
	public static GSSequenceSession read(PacketByteBuf buf) throws IOException {
		buf.readByte();
		
		UUID compositionUUID = buf.readUuid();
		UUID trackUUID = buf.readUuid();
		UUID sequenceUUID = buf.readUuid();
		GSSequenceUndoRedoHistory undoRedoHistory = GSSequenceUndoRedoHistory.read(buf);
		
		GSSequenceSession session = new GSSequenceSession(compositionUUID, trackUUID, sequenceUUID, undoRedoHistory);

		session.setXOffset(buf.readFloat());
		session.setYOffset(buf.readFloat());

		GSEContentOpacity opacity = GSEContentOpacity.fromIndex(buf.readInt());
		if (opacity == null)
			throw new IOException("Unknown opacity");
		session.setOpacity(opacity);

		if (buf.readBoolean()) {
			session.setSelectedChannelUUID(buf.readUuid());
		} else {
			session.setSelectedChannelUUID(null);
		}
		
		return session;
	}
	
	public static void write(PacketByteBuf buf, GSSequenceSession session) throws IOException {
		/* Reserved for version control */
		buf.writeByte(0x00);
		
		buf.writeUuid(session.getCompositionUUID());
		buf.writeUuid(session.getTrackUUID());
		buf.writeUuid(session.getSequenceUUID());
		GSSequenceUndoRedoHistory.write(buf, session.getUndoRedoHistory());
		
		buf.writeFloat(session.getXOffset());
		buf.writeFloat(session.getYOffset());
		
		buf.writeInt(session.getOpacity().getIndex());
		
		UUID selectedChannelUUID = session.getSelectedChannelUUID();
		if (selectedChannelUUID != null) {
			buf.writeBoolean(true);
			buf.writeUuid(selectedChannelUUID);
		} else {
			buf.writeBoolean(false);
		}
	}
}
