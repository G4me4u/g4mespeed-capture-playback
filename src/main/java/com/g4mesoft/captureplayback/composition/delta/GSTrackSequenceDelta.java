package com.g4mesoft.captureplayback.composition.delta;

import java.io.IOException;
import java.util.UUID;

import com.g4mesoft.captureplayback.CapturePlaybackMod;
import com.g4mesoft.captureplayback.GSCapturePlaybackExtension;
import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.sequence.delta.GSISequenceDelta;
import com.g4mesoft.captureplayback.sequence.delta.GSSequenceDeltaException;

import net.minecraft.network.PacketByteBuf;

public class GSTrackSequenceDelta extends GSTrackDelta {

	private GSISequenceDelta delta;

	public GSTrackSequenceDelta() {
	}
	
	public GSTrackSequenceDelta(UUID trackUUID, GSISequenceDelta delta) {
		super(trackUUID);
		
		this.delta = delta;
	}

	@Override
	public void unapplyDelta(GSComposition composition) throws GSCompositionDeltaException {
		try {
			delta.unapplyDelta(getTrack(composition).getSequence());
		} catch (GSSequenceDeltaException e) {
			throw new GSCompositionDeltaException(e);
		}
	}

	@Override
	public void applyDelta(GSComposition composition) throws GSCompositionDeltaException {
		try {
			delta.applyDelta(getTrack(composition).getSequence());
		} catch (GSSequenceDeltaException e) {
			throw new GSCompositionDeltaException(e);
		}
	}
	
	@Override
	public void read(PacketByteBuf buf) throws IOException {
		super.read(buf);
		
		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		
		delta = extension.getSequenceDeltaRegistry().createNewElement(buf.readInt());
		if (delta == null)
			throw new IOException("Invalid delta ID");
		delta.read(buf);
	}

	@Override
	public void write(PacketByteBuf buf) throws IOException {
		super.write(buf);

		GSCapturePlaybackExtension extension = CapturePlaybackMod.getInstance().getExtension();
		
		buf.writeInt(extension.getSequenceDeltaRegistry().getIdentifier(delta));
		delta.write(buf);
	}
}
