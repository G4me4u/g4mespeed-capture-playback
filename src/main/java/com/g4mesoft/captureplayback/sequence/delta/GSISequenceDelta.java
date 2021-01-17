package com.g4mesoft.captureplayback.sequence.delta;

import java.io.IOException;

import com.g4mesoft.captureplayback.sequence.GSSequence;

import net.minecraft.network.PacketByteBuf;

public interface GSISequenceDelta {

	public void unapplyDelta(GSSequence sequence) throws GSSequenceDeltaException;
	
	public void applyDelta(GSSequence sequence) throws GSSequenceDeltaException;
	
	public void read(PacketByteBuf buf) throws IOException;

	public void write(PacketByteBuf buf) throws IOException;
	
}
