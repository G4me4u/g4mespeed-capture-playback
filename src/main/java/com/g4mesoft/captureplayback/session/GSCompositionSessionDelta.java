package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSDeltaRegistries;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;

import net.minecraft.network.PacketByteBuf;

public class GSCompositionSessionDelta implements GSIDelta<GSSession> {

	private GSSessionFieldType<?> type;
	private GSIDelta<GSComposition> delta;
	
	public GSCompositionSessionDelta() {
	}
	
	public GSCompositionSessionDelta(GSSessionFieldType<GSComposition> type, GSIDelta<GSComposition> delta) {
		this.type = type;
		this.delta = delta;
	}
	
	@Override
	public void apply(GSSession session) throws GSDeltaException {
		GSSessionField<?> field = session.getField(type);
		if (!(field instanceof GSCompositionSessionField))
			throw new GSDeltaException("Field '" + type.getName() + "' is not a composition.");
		((GSCompositionSessionField)field).applyDelta(delta);
	}
	
	@Override
	public void unapply(GSSession session) throws GSDeltaException {
		throw new GSDeltaException("Unapply unsupported.");
	}

	@Override
	public void read(PacketByteBuf buf) throws IOException {
		type = GSSession.readFieldType(buf);
		delta = GSDeltaRegistries.COMPOSITION_DELTA_REGISTRY.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSSession.writeFieldType(buf, type);
		GSDeltaRegistries.COMPOSITION_DELTA_REGISTRY.write(buf, delta);
	}
}
