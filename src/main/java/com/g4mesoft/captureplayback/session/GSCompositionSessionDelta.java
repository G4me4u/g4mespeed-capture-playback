package com.g4mesoft.captureplayback.session;

import java.io.IOException;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.delta.GSICompositionDelta;

import net.minecraft.network.PacketByteBuf;

public class GSCompositionSessionDelta implements GSISessionDelta {

	private GSSessionFieldType<?> type;
	private GSICompositionDelta delta;
	
	public GSCompositionSessionDelta() {
	}
	
	public GSCompositionSessionDelta(GSSessionFieldType<?> type, GSICompositionDelta delta) {
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
	public void read(PacketByteBuf buf) throws IOException {
		type = GSSession.readFieldType(buf);
		
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		
		delta = extension.getCompositionDeltaRegistry().createNewElement(buf.readInt());
		if (delta == null)
			throw new IOException("Invalid delta ID");
		delta.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		GSSession.writeFieldType(buf, type);
		
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		buf.writeInt(extension.getCompositionDeltaRegistry().getIdentifier(delta));
		delta.write(buf);
	}

	@Override
	public GSSessionFieldType<?> getType() {
		return type;
	}
}
