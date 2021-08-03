package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;

import com.g4mesoft.captureplayback.common.GSDeltaException;
import com.g4mesoft.captureplayback.composition.GSComposition;

import net.minecraft.network.PacketByteBuf;

public interface GSICompositionDelta {

	public void unapplyDelta(GSComposition composition) throws GSDeltaException;
	
	public void applyDelta(GSComposition composition) throws GSDeltaException;
	
	public void read(PacketByteBuf buf) throws IOException;

	public void write(PacketByteBuf buf) throws IOException;
	
}
