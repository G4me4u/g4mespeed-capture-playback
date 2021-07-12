package com.g4mesoft.captureplayback.module;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.panel.GSEContentOpacity;

import net.minecraft.network.PacketByteBuf;

public class GSCompositionSession {

	private static final float DEFAULT_X_OFFSET = 0.0f;
	private static final float DEFAULT_Y_OFFSET = 0.0f;

	private static final GSEContentOpacity DEFAULT_OPACITY = GSEContentOpacity.FULLY_OPAQUE;
	
	private final UUID compositionUUID;

	/* Cached elements of the UI */
	private float xOffset;
	private float yOffset;
	
	private GSEContentOpacity opacity;
	
	public GSCompositionSession(UUID compositionUUID) {
		this.compositionUUID = compositionUUID;
		
		xOffset = DEFAULT_X_OFFSET;
		yOffset = DEFAULT_Y_OFFSET;

		opacity = DEFAULT_OPACITY;
	}
	
	public UUID getCompositionUUID() {
		return compositionUUID;
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

	public static GSCompositionSession read(PacketByteBuf buf) throws IOException {
		buf.readByte();
		
		UUID compositionUUID = buf.readUuid();
		
		GSCompositionSession session = new GSCompositionSession(compositionUUID);

		session.setXOffset(buf.readFloat());
		session.setYOffset(buf.readFloat());

		GSEContentOpacity opacity = GSEContentOpacity.fromIndex(buf.readInt());
		if (opacity == null)
			throw new IOException("Unknown opacity");
		session.setOpacity(opacity);
		
		return session;
	}
	
	public static void write(PacketByteBuf buf, GSCompositionSession session) throws IOException {
		/* Reserved for version control */
		buf.writeByte(0x00);
		
		buf.writeUuid(session.getCompositionUUID());
		
		buf.writeFloat(session.getXOffset());
		buf.writeFloat(session.getYOffset());
		
		buf.writeInt(session.getOpacity().getIndex());
	}
}
