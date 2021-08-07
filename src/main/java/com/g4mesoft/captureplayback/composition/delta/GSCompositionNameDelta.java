package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.Objects;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.common.GSIDelta;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.util.GSBufferUtil;

import net.minecraft.network.PacketByteBuf;

public class GSCompositionNameDelta implements GSIDelta<GSComposition> {

	private String newName;
	private String oldName;

	public GSCompositionNameDelta() {
	}
	
	public GSCompositionNameDelta(String newName, String oldName) {
		this.newName = newName;
		this.oldName = oldName;
	}

	private void setName(String newName, String oldName, GSComposition composition) throws GSDeltaException {
		if (!Objects.equals(oldName, composition.getName()))
			throw new GSDeltaException("Composition does not have the expected name");
		composition.setName(newName);
	}
	
	@Override
	public void unapply(GSComposition composition) throws GSDeltaException {
		setName(oldName, newName, composition);
	}

	@Override
	public void apply(GSComposition composition) throws GSDeltaException {
		setName(newName, oldName, composition);
	}

	@Override
	public void read(PacketByteBuf buf) throws IOException {
		newName = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
		oldName = buf.readString(GSBufferUtil.MAX_STRING_LENGTH);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		buf.writeString(newName);
		buf.writeString(oldName);
	}
}
