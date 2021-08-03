package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;

import net.minecraft.network.PacketByteBuf;

public class GSFieldSessionDelta implements GSISessionDelta {

	private GSSessionFieldType<?> type;
	private Object value;

	public GSFieldSessionDelta() {
	}

	public <T> GSFieldSessionDelta(GSSessionFieldPair<T> pair) {
		this(pair.getType(), pair.getValue());
	}
	
	public <T> GSFieldSessionDelta(GSSessionFieldType<T> type, T value) {
		this.type = type;
		this.value = value;
	}
	
	@Override
	public void apply(GSSession session) throws GSDeltaException {
		applyUnchecked(session);
	}
	
	@SuppressWarnings("unchecked")
	public <T> void applyUnchecked(GSSession session) throws GSDeltaException {
		// We are guaranteed to have the same types, given that
		// GSSession.readField keeps its type-related promises.
		session.set((GSSessionFieldType<T>)type, (T)value);
	}

	@Override
	public void read(PacketByteBuf buf) throws IOException {
		GSSessionFieldPair<?> pair = GSSession.readFieldPair(buf);
		type = pair.getType();
		value = pair.getValue();
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSSession.writeFieldPair(buf, new GSSessionFieldPair<>(type, value));
	}
	
	@Override
	public GSSessionFieldType<?> getType() {
		return type;
	}
}
