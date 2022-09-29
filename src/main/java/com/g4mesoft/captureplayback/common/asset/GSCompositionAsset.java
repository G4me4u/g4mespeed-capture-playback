package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import com.g4mesoft.captureplayback.composition.GSComposition;
import com.g4mesoft.captureplayback.composition.GSICompositionListener;
import com.g4mesoft.captureplayback.composition.GSTrack;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;

import net.minecraft.network.PacketByteBuf;

public class GSCompositionAsset extends GSAbstractAsset implements GSICompositionListener {

	private final GSComposition composition;

	public GSCompositionAsset(GSAssetInfo info) {
		this(new GSComposition(info.getAssetUUID(), info.getAssetName()));
	}

	public GSCompositionAsset(GSComposition composition) {
		super(GSEAssetType.COMPOSITION);
		
		this.composition = composition;
	}

	@Override
	protected void onAdded() {
		super.onAdded();
		
		composition.addCompositionListener(this);
	}

	@Override
	protected void onRemoved() {
		super.onRemoved();
		
		composition.removeCompositionListener(this);
	}
	
	public GSComposition getComposition() {
		return composition;
	}

	@Override
	public UUID getUUID() {
		return composition.getCompositionUUID();
	}
	
	@Override
	public String getName() {
		return composition.getName();
	}

	@Override
	public GSIPlaybackStream getPlaybackStream() {
		return composition.getPlaybackStream();
	}

	@Override
	public GSICaptureStream getCaptureStream() {
		return composition.getCaptureStream();
	}
	
	@Override
	public Iterator<UUID> getDerivedIterator() {
		// TODO: use sequence UUIDs once tracks hold multiple sequences.
		return composition.getTrackUUIDs().iterator();
	}

	@Override
	public GSAbstractAsset getDerivedAsset(UUID assetUUID) {
		GSTrack track = composition.getTrack(assetUUID);
		if (track != null)
			return new GSSequenceAsset(track.getSequence());
		return null;
	}
	
	@Override
	public void compositionNameChanged(String oldName) {
		dispatchNameChanged(composition.getName());
	}
	
	@Override
	public void trackAdded(GSTrack track) {
		dispatchDerivedAssetAdded(track.getTrackUUID());
	}

	@Override
	public void trackRemoved(GSTrack track) {
		dispatchDerivedAssetRemoved(track.getTrackUUID());
	}

	public static GSCompositionAsset read(PacketByteBuf buf) throws IOException {
		return new GSCompositionAsset(GSComposition.read(buf));
	}
	
	public static void write(PacketByteBuf buf, GSCompositionAsset asset) throws IOException {
		GSComposition.write(buf, asset.getComposition());
	}
}
