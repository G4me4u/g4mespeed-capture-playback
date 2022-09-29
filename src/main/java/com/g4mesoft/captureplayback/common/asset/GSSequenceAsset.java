package com.g4mesoft.captureplayback.common.asset;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import com.g4mesoft.captureplayback.sequence.GSISequenceListener;
import com.g4mesoft.captureplayback.sequence.GSSequence;
import com.g4mesoft.captureplayback.stream.GSICaptureStream;
import com.g4mesoft.captureplayback.stream.GSIPlaybackStream;

import net.minecraft.network.PacketByteBuf;

public class GSSequenceAsset extends GSAbstractAsset implements GSISequenceListener {

	private final GSSequence sequence;
	
	public GSSequenceAsset(GSAssetInfo info) {
		this(new GSSequence(info.getAssetUUID(), info.getAssetName()));
	}
	
	public GSSequenceAsset(GSSequence sequence) {
		super(GSEAssetType.SEQUENCE);
		
		this.sequence = sequence;
	}
	
	@Override
	protected void onAdded() {
		super.onAdded();
		
		sequence.addSequenceListener(this);
	}

	@Override
	protected void onRemoved() {
		super.onRemoved();
		
		sequence.removeSequenceListener(this);
	}
	
	public GSSequence getSequence() {
		return sequence;
	}
	
	@Override
	public UUID getUUID() {
		return sequence.getSequenceUUID();
	}
	
	@Override
	public String getName() {
		return sequence.getName();
	}
	
	@Override
	public GSIPlaybackStream getPlaybackStream() {
		return sequence.getPlaybackStream();
	}

	@Override
	public GSICaptureStream getCaptureStream() {
		return sequence.getCaptureStream();
	}
	
	@Override
	public Iterator<UUID> getDerivedIterator() {
		return Collections.emptyIterator();
	}

	@Override
	public GSAbstractAsset getDerivedAsset(UUID assetUUID) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void sequenceNameChanged(String oldName) {
		dispatchNameChanged(sequence.getName());
	}

	public static GSSequenceAsset read(PacketByteBuf buf) throws IOException {
		return new GSSequenceAsset(GSSequence.read(buf));
	}
	
	public static void write(PacketByteBuf buf, GSSequenceAsset asset) throws IOException {
		GSSequence.write(buf, asset.getSequence());
	}
}
